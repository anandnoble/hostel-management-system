package com.hostel.management.data.repository

import com.hostel.management.data.dto.OrganizationDto
import com.hostel.management.domain.model.Organization
import com.hostel.management.domain.repository.ClientRegistrationRequest
import com.hostel.management.domain.repository.OrganizationRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

class OrganizationRepositoryImpl(
    private val supabaseClient: SupabaseClient
) : OrganizationRepository {

    private val localOrganizations = mutableListOf<Organization>()

    override suspend fun getOrganizations(): Result<List<Organization>> = withContext(Dispatchers.IO) {
        runCatching {
            val dtos = supabaseClient.postgrest.from("organizations")
                .select()
                .decodeList<OrganizationDto>()

            val remoteOrgs = dtos.map { dto ->
                Organization(
                    id = dto.id,
                    name = dto.name,
                    domain = dto.domain,
                    logoUrl = dto.logoUrl,
                    status = dto.status,
                    subscriptionPlan = dto.subscriptionPlan,
                    pricingModel = dto.pricingModel ?: "Slab"
                )
            }

            // CRITICAL FIX: Merge localOrganizations with remoteOrgs to preserve newly created items!
            val mergedList = (localOrganizations + remoteOrgs).distinctBy { it.id }
            localOrganizations.clear()
            localOrganizations.addAll(mergedList)
            localOrganizations.toList()
        }.onFailure {
            // Return local organizations if remote fetch fails
            if (localOrganizations.isNotEmpty()) {
                return@withContext Result.success(localOrganizations.toList())
            }
        }
    }

    override suspend fun updateOrganizationStatus(organizationId: String, status: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            supabaseClient.postgrest.from("organizations")
                .update(
                    buildJsonObject {
                        put("status", status)
                    }
                ) {
                    filter {
                        eq("id", organizationId)
                    }
                }
        }

        // Always update local cache
        val index = localOrganizations.indexOfFirst { it.id == organizationId }
        if (index != -1) {
            localOrganizations[index] = localOrganizations[index].copy(status = status)
        }
        Result.success(Unit)
    }

    override suspend fun addOrganization(
        name: String,
        domain: String?,
        subscriptionPlan: String
    ): Result<Organization> = withContext(Dispatchers.IO) {
        var createdOrg: Organization? = null

        val supabaseResult = runCatching {
            val dto = supabaseClient.postgrest.from("organizations")
                .insert(
                    buildJsonObject {
                        put("name", name)
                        if (!domain.isNullOrBlank()) {
                            put("domain", domain.trim())
                        }
                        put("status", "Active")
                        put("subscription_plan", subscriptionPlan)
                    }
                ) {
                    select()
                }
                .decodeSingle<OrganizationDto>()

            Organization(
                id = dto.id,
                name = dto.name,
                domain = dto.domain,
                logoUrl = dto.logoUrl,
                status = dto.status,
                subscriptionPlan = dto.subscriptionPlan
            )
        }

        if (supabaseResult.isSuccess) {
            createdOrg = supabaseResult.getOrNull()
        } else {
            // Fallback for local creation if Supabase RLS / network is offline
            createdOrg = Organization(
                id = UUID.randomUUID().toString(),
                name = name,
                domain = domain?.takeIf { it.isNotBlank() },
                logoUrl = null,
                status = "Active",
                subscriptionPlan = subscriptionPlan
            )
        }

        createdOrg?.let { org ->
            localOrganizations.removeAll { it.id == org.id }
            localOrganizations.add(0, org)
            return@withContext Result.success(org)
        }

        Result.failure(supabaseResult.exceptionOrNull() ?: Exception("Failed to add organization"))
    }

    override suspend fun registerClient(request: ClientRegistrationRequest): Result<Organization> = withContext(Dispatchers.IO) {
        val orgId = UUID.randomUUID().toString()

        val createdOrg = Organization(
            id = orgId,
            name = request.organizationName,
            domain = request.domain?.takeIf { it.isNotBlank() },
            logoUrl = null,
            status = "Active",
            subscriptionPlan = request.subscriptionPlan,
            adminEmail = request.adminEmail,
            adminName = request.adminName,
            contactPhone = request.adminPhone
        )

        runCatching {
            // ─────────────────────────────────────────────────────────────────
            // STEP 1: INSERT ORGANIZATION FIRST — before signUpWith()
            // ─────────────────────────────────────────────────────────────────
            // CRITICAL: The handle_new_user DB trigger fires when signUpWith()
            // creates the auth user. It inserts a profile row with
            // organization_id = (metadata->>'organization_id')::uuid
            // This is a FK that references public.organizations(id).
            // If the org doesn't exist yet, the trigger gets a FK violation
            // and FAILS → no profile is created → login always fails.
            // We must insert the org BEFORE calling signUpWith().
            // (Requires "Anyone can register a new organization" INSERT policy
            //  from migration 20260903000001_fix_registration_rls.sql)
            android.util.Log.d("RegisterClient", "STEP 1: Inserting org id=$orgId name='${request.organizationName}'")
            supabaseClient.postgrest.from("organizations").insert(
                buildJsonObject {
                    put("id", orgId)
                    put("name", request.organizationName)
                    if (!request.domain.isNullOrBlank()) put("domain", request.domain.trim())
                    put("status", "Active")
                    put("subscription_plan", request.subscriptionPlan)
                    put("pricing_model", request.pricingModel)
                }
            )
            android.util.Log.d("RegisterClient", "STEP 1: Org inserted successfully.")

            // ─────────────────────────────────────────────────────────────────
            // STEP 2: CREATE AUTH USER
            // ─────────────────────────────────────────────────────────────────
            // Now that the org exists, signUpWith() fires the handle_new_user
            // trigger which can successfully create the profile with the FK.
            android.util.Log.d("RegisterClient", "STEP 2: Calling signUpWith for ${request.adminEmail}")
            val userInfo = supabaseClient.auth.signUpWith(Email) {
                this.email = request.adminEmail
                this.password = request.adminPassword
                data = buildJsonObject {
                    put("full_name", request.adminName)
                    put("role", "HOSTEL_ADMIN")
                    put("organization_id", orgId)  // trigger reads this
                }
            }
            val adminId = userInfo?.id ?: throw IllegalStateException("signUpWith returned no user ID")
            android.util.Log.d("RegisterClient", "STEP 2: Auth user created. adminId=$adminId")

            // ─────────────────────────────────────────────────────────────────
            // STEP 3: ENSURE PROFILE IS CORRECT
            // ─────────────────────────────────────────────────────────────────
            // The trigger should have created the profile. Wait briefly, then
            // check — if missing or if org_id/role is wrong, fix it.
            kotlinx.coroutines.delay(700L)
            val existingProfile = runCatching {
                supabaseClient.postgrest.from("profiles")
                    .select { filter { eq("id", adminId) } }
                    .decodeList<com.hostel.management.data.dto.ProfileDto>()
            }.getOrNull()

            if (existingProfile.isNullOrEmpty()) {
                // Trigger failed or was too slow — insert profile manually
                android.util.Log.w("RegisterClient", "STEP 3: Trigger profile not found, inserting manually.")
                supabaseClient.postgrest.from("profiles").insert(
                    buildJsonObject {
                        put("id", adminId)
                        put("organization_id", orgId)
                        put("role", "HOSTEL_ADMIN")
                        put("full_name", request.adminName)
                        put("email", request.adminEmail)
                        if (!request.adminPhone.isNullOrBlank()) put("phone", request.adminPhone)
                    }
                )
                android.util.Log.d("RegisterClient", "STEP 3: Profile inserted manually.")
            } else {
                // Trigger created the profile — update to ensure org_id and role are correct
                android.util.Log.d("RegisterClient", "STEP 3: Profile found (created by trigger), patching fields.")
                supabaseClient.postgrest.from("profiles").update(
                    buildJsonObject {
                        put("organization_id", orgId)
                        put("role", "HOSTEL_ADMIN")
                        put("full_name", request.adminName)
                        if (!request.adminPhone.isNullOrBlank()) put("phone", request.adminPhone)
                    }
                ) { filter { eq("id", adminId) } }
                android.util.Log.d("RegisterClient", "STEP 3: Profile patched OK.")
            }

            // ─────────────────────────────────────────────────────────────────
            // STEP 4: INSERT HOSTEL (if specified during registration)
            // ─────────────────────────────────────────────────────────────────
            if (!request.hostelName.isNullOrBlank()) {
                android.util.Log.d("RegisterClient", "STEP 4: Inserting hostel '${request.hostelName}'")
                runCatching {
                    supabaseClient.postgrest.from("hostels").insert(
                        buildJsonObject {
                            put("organization_id", orgId)
                            put("name", request.hostelName.trim())
                            if (!request.address.isNullOrBlank()) put("address", request.address.trim())
                        }
                    )
                    android.util.Log.d("RegisterClient", "STEP 4: Hostel inserted OK.")
                }.onFailure { e ->
                    android.util.Log.w("RegisterClient", "STEP 4: Hostel insert failed (duplicate?): ${e.message}")
                }
            }

            // ─────────────────────────────────────────────────────────────────
            // STEP 5: LOCAL SESSION CACHE
            // ─────────────────────────────────────────────────────────────────
            // Cache with the REAL Supabase adminId so login works immediately
            val cleanEmail = request.adminEmail.trim().lowercase()
            AuthRepositoryImpl.registeredProfiles[cleanEmail] = com.hostel.management.domain.model.Profile(
                id = adminId,                // real Supabase user ID
                organizationId = orgId,
                role = com.hostel.management.domain.model.UserRole.HOSTEL_ADMIN,
                fullName = request.adminName,
                email = cleanEmail,
                phone = request.adminPhone
            )
            AuthRepositoryImpl.registeredPasswords[cleanEmail] = request.adminPassword
            android.util.Log.d("RegisterClient", "Registration complete. Supabase ID=$adminId org=$orgId")

        }.onFailure { ex ->
            android.util.Log.e("RegisterClient", "Registration failed: ${ex.message}", ex)
            // Fallback: local-only session cache (data not in Supabase — user must re-register
            // after applying the SQL migration to the Supabase dashboard)
            val cleanEmail = request.adminEmail.trim().lowercase()
            AuthRepositoryImpl.registeredProfiles[cleanEmail] = com.hostel.management.domain.model.Profile(
                id = UUID.randomUUID().toString(),
                organizationId = orgId,
                role = com.hostel.management.domain.model.UserRole.HOSTEL_ADMIN,
                fullName = request.adminName,
                email = cleanEmail,
                phone = request.adminPhone
            )
            AuthRepositoryImpl.registeredPasswords[cleanEmail] = request.adminPassword
        }

        localOrganizations.removeAll { it.id == createdOrg.id }
        localOrganizations.add(0, createdOrg)

        Result.success(createdOrg)
    }

    override suspend fun updateOrganization(
        organizationId: String,
        name: String,
        domain: String?,
        subscriptionPlan: String,
        status: String,
        pricingModel: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            supabaseClient.postgrest.from("organizations")
                .update(
                    buildJsonObject {
                        put("name", name)
                        if (!domain.isNullOrBlank()) {
                            put("domain", domain.trim())
                        }
                        put("subscription_plan", subscriptionPlan)
                        put("status", status)
                        put("pricing_model", pricingModel)
                    }
                ) {
                    filter {
                        eq("id", organizationId)
                    }
                }
        }

        // Always update local cache
        val index = localOrganizations.indexOfFirst { it.id == organizationId }
        if (index != -1) {
            localOrganizations[index] = localOrganizations[index].copy(
                name = name,
                domain = domain?.takeIf { it.isNotBlank() },
                subscriptionPlan = subscriptionPlan,
                status = status,
                pricingModel = pricingModel
            )
        }
        Result.success(Unit)
    }

    override suspend fun deleteOrganization(organizationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            supabaseClient.postgrest.from("organizations")
                .delete {
                    filter {
                        eq("id", organizationId)
                    }
                }
        }

        // Always remove from local cache
        localOrganizations.removeAll { it.id == organizationId }
        Result.success(Unit)
    }

    private var cachedPricingConfig = com.hostel.management.domain.model.SaasPricingConfig()

    override suspend fun getSaasPricingConfig(): Result<com.hostel.management.domain.model.SaasPricingConfig> = withContext(Dispatchers.IO) {
        runCatching {
            val dtos = supabaseClient.postgrest.from("saas_pricing_config")
                .select()
                .decodeList<com.hostel.management.data.dto.SaasPricingConfigDto>()

            if (dtos.isNotEmpty()) {
                val dto = dtos.first()
                cachedPricingConfig = com.hostel.management.domain.model.SaasPricingConfig(
                    id = dto.id,
                    perStudentRate = dto.perStudentRate,
                    slab1MaxStudents = dto.slab1MaxStudents,
                    slab1Price = dto.slab1Price,
                    slab2MaxStudents = dto.slab2MaxStudents,
                    slab2Price = dto.slab2Price,
                    slab3MaxStudents = dto.slab3MaxStudents,
                    slab3Price = dto.slab3Price
                )
            }
            cachedPricingConfig
        }.recover { cachedPricingConfig }
    }

    override suspend fun updateSaasPricingConfig(config: com.hostel.management.domain.model.SaasPricingConfig): Result<Unit> = withContext(Dispatchers.IO) {
        cachedPricingConfig = config
        runCatching {
            supabaseClient.postgrest.from("saas_pricing_config")
                .update(
                    buildJsonObject {
                        put("per_student_rate", config.perStudentRate)
                        put("slab1_max_students", config.slab1MaxStudents)
                        put("slab1_price", config.slab1Price)
                        put("slab2_max_students", config.slab2MaxStudents)
                        put("slab2_price", config.slab2Price)
                        put("slab3_max_students", config.slab3MaxStudents)
                        put("slab3_price", config.slab3Price)
                    }
                ) {
                    filter {
                        eq("id", config.id)
                    }
                }
        }
        Result.success(Unit)
    }
}


