package com.hostel.management.domain.repository

import com.hostel.management.domain.model.Organization

data class ClientRegistrationRequest(
    val organizationName: String,
    val domain: String?,
    val subscriptionPlan: String,
    val adminName: String,
    val adminEmail: String,
    val adminPhone: String?,
    val adminPassword: String,
    val hostelName: String?,
    val address: String?,
    val pricingModel: String = "Slab"
)

interface OrganizationRepository {
    suspend fun getOrganizations(): Result<List<Organization>>
    suspend fun updateOrganizationStatus(organizationId: String, status: String): Result<Unit>
    suspend fun addOrganization(name: String, domain: String?, subscriptionPlan: String): Result<Organization>
    suspend fun registerClient(request: ClientRegistrationRequest): Result<Organization>
    suspend fun updateOrganization(organizationId: String, name: String, domain: String?, subscriptionPlan: String, status: String, pricingModel: String = "Slab"): Result<Unit>
    suspend fun deleteOrganization(organizationId: String): Result<Unit>

    // SaaS Global Pricing Configuration
    suspend fun getSaasPricingConfig(): Result<com.hostel.management.domain.model.SaasPricingConfig>
    suspend fun updateSaasPricingConfig(config: com.hostel.management.domain.model.SaasPricingConfig): Result<Unit>
}


