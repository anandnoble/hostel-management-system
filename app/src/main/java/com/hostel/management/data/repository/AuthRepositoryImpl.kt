package com.hostel.management.data.repository

import com.hostel.management.data.dto.ProfileDto
import com.hostel.management.domain.model.Profile
import com.hostel.management.domain.model.UserRole
import com.hostel.management.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class AuthRepositoryImpl(
    private val supabaseClient: SupabaseClient
) : AuthRepository {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val _sessionProfile = MutableStateFlow<Profile?>(null)

    companion object {
        val registeredProfiles = ConcurrentHashMap<String, Profile>()
        val registeredPasswords = ConcurrentHashMap<String, String>()

        init {
            // Seed demo accounts with known passwords
            val demoAccounts = listOf(
                Triple("saas@admin.com", "password123", UserRole.SUPER_ADMIN to "SaaS Super Admin"),
                Triple("admin@hostel.com", "password123", UserRole.HOSTEL_ADMIN to "Hostel Administrator"),
                Triple("student@hostel.com", "password123", UserRole.STUDENT to "Alex Johnson"),
                Triple("accountant@hostel.com", "password123", UserRole.ACCOUNTANT to "Chief Accountant"),
                Triple("maintenance@hostel.com", "password123", UserRole.MAINTENANCE_STAFF to "Maintenance Manager")
            )

            demoAccounts.forEach { (email, pass, roleAndName) ->
                val (role, name) = roleAndName
                val cleanEmail = email.trim().lowercase()
                registeredProfiles[cleanEmail] = Profile(
                    id = UUID.randomUUID().toString(),
                    organizationId = "org_default",
                    role = role,
                    fullName = name,
                    email = cleanEmail
                )
                registeredPasswords[cleanEmail] = pass
            }
        }
    }

    init {
        scope.launch {
            supabaseClient.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val userId = status.session.user?.id
                        if (userId != null && _sessionProfile.value == null) {
                            // Retry up to 3 times with a short delay to avoid
                            // RLS race conditions where auth.uid() isn't yet available
                            var attempt = 0
                            while (attempt < 3) {
                                val result = fetchProfile(userId)
                                if (result.isSuccess) {
                                    _sessionProfile.value = result.getOrNull()
                                    break
                                }
                                attempt++
                                if (attempt < 3) delay(500L)
                            }
                        }
                    }
                    is SessionStatus.NotAuthenticated -> {
                        // Supabase session ended — clear the local profile too
                        _sessionProfile.value = null
                    }
                    else -> {
                        // LoadingFromStorage, NetworkError — don't touch the profile
                    }
                }
            }
        }
    }

    private suspend fun fetchProfile(userId: String): Result<Profile> = runCatching {
        android.util.Log.d("AuthDebug", "Querying profiles table for id=$userId")
        // Use decodeList + firstOrNull instead of decodeSingle, which throws on empty result.
        val dto = supabaseClient.postgrest.from("profiles")
            .select { filter { eq("id", userId) } }
            .decodeList<ProfileDto>()
            .firstOrNull()
            ?: throw IllegalStateException("No profile row found for userId=$userId")

        android.util.Log.d("AuthDebug", "Profile fetched: id=${dto.id} orgId=${dto.organizationId} role=${dto.role}")
        Profile(
            id = dto.id,
            organizationId = dto.organizationId,
            role = UserRole.valueOf(dto.role),
            fullName = dto.fullName,
            email = dto.email,
            phone = dto.phone,
            avatarUrl = dto.avatarUrl
        )
    }

    override suspend fun login(email: String, password: String): Result<Profile> = runCatching {
        val cleanEmail = email.trim().lowercase()

        // 1. Attempt remote Supabase authentication
        val authResult = runCatching {
            supabaseClient.auth.signInWith(Email) {
                this.email = cleanEmail
                this.password = password
            }
        }

        if (authResult.isSuccess) {
            // Auth succeeded — now fetch the profile (with retry for RLS race condition)
            val session = supabaseClient.auth.currentSessionOrNull()
            val userId = session?.user?.id

            if (userId != null) {
                var profileResult: Result<Profile>? = null
                repeat(5) { attempt ->
                    if (profileResult?.isSuccess != true) {
                        if (attempt > 0) kotlinx.coroutines.delay(600L)
                        android.util.Log.d("AuthDebug", "Profile fetch attempt ${attempt + 1} for userId=$userId")
                        profileResult = fetchProfile(userId)
                    }
                }
                if (profileResult?.isSuccess == true) {
                    val profile = profileResult!!.getOrThrow()
                    _sessionProfile.value = profile
                    return@runCatching profile
                } else {
                    // Profile row missing — build a minimal profile from session metadata.
                    // IMPORTANT: Extract organization_id from JWT metadata so RLS works correctly.
                    // Without organizationId, current_user_org_id() returns NULL and getHostels() returns [].
                    android.util.Log.e("AuthDebug", "Profile fetch failed: ${profileResult?.exceptionOrNull()?.message}")
                    val user = session.user
                    val metaOrgId = user?.userMetadata?.get("organization_id")
                        ?.toString()?.trim('"')
                        ?.takeIf { it.isNotBlank() && it != "null" }
                    val metaName = user?.userMetadata?.get("full_name")
                        ?.toString()?.trim('"')
                        ?.takeIf { it.isNotBlank() && it != "null" }

                    android.util.Log.d("AuthDebug", "Fallback: orgId from JWT meta = $metaOrgId")

                    // Attempt to INSERT the missing profile row so RLS functions work.
                    // This recovers from the case where the handle_new_user trigger failed.
                    if (metaOrgId != null) {
                        runCatching {
                            supabaseClient.postgrest.from("profiles").insert(
                                buildJsonObject {
                                    put("id", userId)
                                    put("organization_id", metaOrgId)
                                    put("role", "HOSTEL_ADMIN")
                                    put("full_name", metaName ?: cleanEmail.substringBefore("@"))
                                    put("email", cleanEmail)
                                }
                            )
                            android.util.Log.d("AuthDebug", "Fallback: inserted missing profile row into Supabase.")
                        }.onFailure { e ->
                            android.util.Log.w("AuthDebug", "Fallback profile insert failed (may already exist): ${e.message}")
                        }
                        // Retry fetch once after recovery insert
                        val retryResult = fetchProfile(userId)
                        if (retryResult.isSuccess) {
                            val profile = retryResult.getOrThrow()
                            _sessionProfile.value = profile
                            return@runCatching profile
                        }
                    }

                    val fallbackProfile = Profile(
                        id = userId,
                        organizationId = metaOrgId,
                        role = UserRole.HOSTEL_ADMIN,
                        fullName = metaName ?: cleanEmail.substringBefore("@"),
                        email = cleanEmail
                    )
                    _sessionProfile.value = fallbackProfile
                    return@runCatching fallbackProfile
                }
            } else {
                throw IllegalStateException("Auth succeeded but no session user ID found")
            }
        }

        // Log the remote auth error before checking local fallback
        authResult.exceptionOrNull()?.let { ex ->
            android.util.Log.e("AuthDebug", "Remote auth failed [${ex::class.simpleName}]: ${ex.message}")
            android.util.Log.e("AuthDebug", "Cause: ${ex.cause?.message}")
        }

        // 2. Check local registered accounts (demo accounts)
        val localProfile = registeredProfiles[cleanEmail]
        val registeredPass = registeredPasswords[cleanEmail]

        if (localProfile != null) {
            if (registeredPass == null || registeredPass != password) {
                throw IllegalArgumentException("Incorrect password for $cleanEmail")
            }
            _sessionProfile.value = localProfile
            return@runCatching localProfile
        }

        // 3. Re-throw the original Supabase error if it exists (shows real reason),
        //    otherwise throw a generic credentials error
        val remoteEx = authResult.exceptionOrNull()
        if (remoteEx != null) throw remoteEx
        throw IllegalArgumentException("Incorrect email or password. Please check your credentials.")
    }

    override suspend fun logout(): Result<Unit> = runCatching {
        runCatching { supabaseClient.auth.signOut() }
        _sessionProfile.value = null
    }

    override suspend fun getCurrentProfile(): Result<Profile?> = runCatching {
        if (_sessionProfile.value != null) {
            return@runCatching _sessionProfile.value
        }
        val session = supabaseClient.auth.currentSessionOrNull()
        if (session != null) {
            val userId = session.user?.id ?: return@runCatching null
            val profile = fetchProfile(userId).getOrThrow()
            _sessionProfile.value = profile
            profile
        } else {
            null
        }
    }

    override fun getSessionFlow(): Flow<Profile?> {
        return _sessionProfile.asStateFlow()
    }
}
