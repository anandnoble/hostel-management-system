package com.hostel.management.presentation.saas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hostel.management.domain.model.Organization
import com.hostel.management.domain.repository.ClientRegistrationRequest
import com.hostel.management.domain.repository.OrganizationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ClientRegistrationSummary(
    val organization: Organization,
    val adminName: String,
    val adminEmail: String,
    val adminPassword: String,
    val hostelName: String
)

class OrganizationViewModel(
    private val repository: OrganizationRepository
) : ViewModel() {

    private val _organizations = MutableStateFlow<List<Organization>>(emptyList())
    val organizations: StateFlow<List<Organization>> = _organizations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _registeredCredentials = MutableStateFlow<ClientRegistrationSummary?>(null)
    val registeredCredentials: StateFlow<ClientRegistrationSummary?> = _registeredCredentials.asStateFlow()

    fun clearError() {
        _error.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    fun clearRegisteredCredentials() {
        _registeredCredentials.value = null
    }

    fun fetchOrganizations() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            repository.getOrganizations().fold(
                onSuccess = { orgs ->
                    _organizations.value = orgs
                },
                onFailure = { e ->
                    _error.value = e.message ?: "Failed to fetch organizations"
                }
            )
            
            _isLoading.value = false
        }
    }

    fun updateStatus(organizationId: String, newStatus: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.updateOrganizationStatus(organizationId, newStatus).fold(
                onSuccess = {
                    _successMessage.value = "Status updated to $newStatus"
                    fetchOrganizations()
                },
                onFailure = { e ->
                    _error.value = e.message ?: "Failed to update status"
                }
            )
            _isLoading.value = false
        }
    }

    fun addOrganization(name: String, domain: String?, subscriptionPlan: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.addOrganization(name, domain, subscriptionPlan).fold(
                onSuccess = { org ->
                    _successMessage.value = "Organization '${org.name}' created successfully!"
                    fetchOrganizations()
                },
                onFailure = { e ->
                    _error.value = e.message ?: "Failed to add organization"
                }
            )
            _isLoading.value = false
        }
    }

    fun registerClient(request: ClientRegistrationRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.registerClient(request).fold(
                onSuccess = { org ->
                    _successMessage.value = "Client '${org.name}' registered successfully!"
                    _registeredCredentials.value = ClientRegistrationSummary(
                        organization = org,
                        adminName = request.adminName,
                        adminEmail = request.adminEmail,
                        adminPassword = request.adminPassword,
                        hostelName = request.hostelName?.ifBlank { "Default Hostel" } ?: "Default Hostel"
                    )
                    fetchOrganizations()
                },
                onFailure = { e ->
                    _error.value = e.message ?: "Failed to register client"
                }
            )
            _isLoading.value = false
        }
    }

    private val _saasPricingConfig = MutableStateFlow(com.hostel.management.domain.model.SaasPricingConfig())
    val saasPricingConfig: StateFlow<com.hostel.management.domain.model.SaasPricingConfig> = _saasPricingConfig.asStateFlow()

    fun fetchSaasPricingConfig() {
        viewModelScope.launch {
            repository.getSaasPricingConfig().onSuccess { cfg ->
                _saasPricingConfig.value = cfg
            }
        }
    }

    fun updateSaasPricingConfig(config: com.hostel.management.domain.model.SaasPricingConfig) {
        viewModelScope.launch {
            repository.updateSaasPricingConfig(config).onSuccess {
                _saasPricingConfig.value = config
                _successMessage.value = "SaaS Global Pricing Configuration updated!"
            }
        }
    }

    fun updateOrganization(
        organizationId: String,
        name: String,
        domain: String?,
        subscriptionPlan: String,
        status: String,
        pricingModel: String = "Slab"
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.updateOrganization(organizationId, name, domain, subscriptionPlan, status, pricingModel).fold(
                onSuccess = {
                    _successMessage.value = "Organization details updated successfully!"
                    fetchOrganizations()
                },
                onFailure = { e ->
                    _error.value = e.message ?: "Failed to update organization"
                }
            )
            _isLoading.value = false
        }
    }

    fun deleteOrganization(organizationId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.deleteOrganization(organizationId).fold(
                onSuccess = {
                    _successMessage.value = "Organization deleted successfully!"
                    fetchOrganizations()
                },
                onFailure = { e ->
                    _error.value = e.message ?: "Failed to delete organization"
                }
            )
            _isLoading.value = false
        }
    }
}


