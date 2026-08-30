package com.hostel.management.presentation.complaint

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hostel.management.domain.model.Complaint
import com.hostel.management.domain.model.ComplaintComment
import com.hostel.management.domain.model.Profile
import com.hostel.management.domain.repository.ComplaintRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ComplaintViewModel(
    private val complaintRepository: ComplaintRepository
) : ViewModel() {

    private val _complaints = MutableStateFlow<List<Complaint>>(emptyList())
    val complaints: StateFlow<List<Complaint>> = _complaints.asStateFlow()

    private val _assignedComplaints = MutableStateFlow<List<Complaint>>(emptyList())
    val assignedComplaints: StateFlow<List<Complaint>> = _assignedComplaints.asStateFlow()

    private val _comments = MutableStateFlow<List<ComplaintComment>>(emptyList())
    val comments: StateFlow<List<ComplaintComment>> = _comments.asStateFlow()

    private val _staffList = MutableStateFlow<List<Profile>>(emptyList())
    val staffList: StateFlow<List<Profile>> = _staffList.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadComplaints(studentId: String? = null, status: String? = null, category: String? = null) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            complaintRepository.getComplaints(studentId, status, category)
                .onSuccess { list ->
                    _complaints.value = list
                    _loading.value = false
                }
                .onFailure { err ->
                    _error.value = err.localizedMessage ?: "Failed to load complaints"
                    _loading.value = false
                }
        }
    }

    fun loadAssignedComplaints(staffId: String) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            complaintRepository.getAssignedComplaints(staffId)
                .onSuccess { list ->
                    _assignedComplaints.value = list
                    _loading.value = false
                }
                .onFailure { err ->
                    _error.value = err.localizedMessage ?: "Failed to load assigned complaints"
                    _loading.value = false
                }
        }
    }

    fun createComplaint(
        category: String,
        title: String,
        description: String,
        priority: String,
        imageBytes: ByteArray?,
        fileExtension: String?,
        onSuccess: () -> Unit
    ) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            complaintRepository.createComplaint(category, title, description, priority, imageBytes, fileExtension)
                .onSuccess {
                    _loading.value = false
                    onSuccess()
                }
                .onFailure { err ->
                    _error.value = err.localizedMessage ?: "Failed to submit complaint"
                    _loading.value = false
                }
        }
    }

    fun updateStatus(complaintId: String, status: String, onSuccess: () -> Unit) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            complaintRepository.updateComplaintStatus(complaintId, status)
                .onSuccess {
                    _loading.value = false
                    onSuccess()
                }
                .onFailure { err ->
                    _error.value = err.localizedMessage ?: "Failed to update complaint status"
                    _loading.value = false
                }
        }
    }

    fun assignComplaint(complaintId: String, staffId: String, notes: String?, onSuccess: () -> Unit) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            complaintRepository.assignComplaint(complaintId, staffId, notes)
                .onSuccess {
                    _loading.value = false
                    onSuccess()
                }
                .onFailure { err ->
                    _error.value = err.localizedMessage ?: "Failed to assign complaint"
                    _loading.value = false
                }
        }
    }

    fun loadComments(complaintId: String) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            complaintRepository.getComplaintComments(complaintId)
                .onSuccess { list ->
                    _comments.value = list
                    _loading.value = false
                }
                .onFailure { err ->
                    _error.value = err.localizedMessage ?: "Failed to load comments"
                    _loading.value = false
                }
        }
    }

    fun addComment(
        complaintId: String,
        comment: String,
        imageBytes: ByteArray?,
        fileExtension: String?,
        onSuccess: () -> Unit
    ) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            complaintRepository.addComplaintComment(complaintId, comment, imageBytes, fileExtension)
                .onSuccess {
                    _loading.value = false
                    loadComments(complaintId)
                    onSuccess()
                }
                .onFailure { err ->
                    _error.value = err.localizedMessage ?: "Failed to post comment"
                    _loading.value = false
                }
        }
    }

    fun loadStaffList() {
        _loading.value = true
        viewModelScope.launch {
            complaintRepository.getMaintenanceStaffList()
                .onSuccess { list ->
                    _staffList.value = list
                    _loading.value = false
                }
                .onFailure { err ->
                    _error.value = err.localizedMessage ?: "Failed to load maintenance staff"
                    _loading.value = false
                }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
