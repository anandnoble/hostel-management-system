package com.hostel.management.presentation.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hostel.management.domain.model.Student
import com.hostel.management.domain.repository.HostelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StudentViewModel(
    private val hostelRepository: HostelRepository
) : ViewModel() {

    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students: StateFlow<List<Student>> = _students.asStateFlow()

    private val _pendingRegistrations = MutableStateFlow<List<com.hostel.management.domain.model.StudentSelfRegistration>>(emptyList())
    val pendingRegistrations: StateFlow<List<com.hostel.management.domain.model.StudentSelfRegistration>> = _pendingRegistrations.asStateFlow()

    private val _monthlyPaymentSubmissions = MutableStateFlow<List<com.hostel.management.domain.model.MonthlyPaymentSubmission>>(emptyList())
    val monthlyPaymentSubmissions: StateFlow<List<com.hostel.management.domain.model.MonthlyPaymentSubmission>> = _monthlyPaymentSubmissions.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedStudent = MutableStateFlow<Student?>(null)
    val selectedStudent: StateFlow<Student?> = _selectedStudent.asStateFlow()

    fun loadPendingRegistrations() {
        viewModelScope.launch {
            hostelRepository.getPendingSelfRegistrations()
                .onSuccess { list ->
                    _pendingRegistrations.value = list
                }
        }
        loadMonthlyPaymentSubmissions()
    }

    fun loadMonthlyPaymentSubmissions() {
        viewModelScope.launch {
            hostelRepository.getMonthlyPaymentSubmissions()
                .onSuccess { list ->
                    _monthlyPaymentSubmissions.value = list
                }
        }
    }

    fun updateSelfRegistrationStatus(
        id: String,
        status: String,
        paymentStatus: String = "Paid",
        amountPaid: Double = 0.0,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            hostelRepository.updateSelfRegistrationStatus(id, status, paymentStatus, amountPaid)
                .onSuccess {
                    loadPendingRegistrations()
                    onSuccess()
                }
        }
    }

    fun verifyMonthlyPayment(id: String, status: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            hostelRepository.verifyMonthlyPayment(id, status)
                .onSuccess {
                    loadMonthlyPaymentSubmissions()
                    onSuccess()
                }
        }
    }

    fun loadStudents(query: String? = null, status: String? = null) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            hostelRepository.getStudents(query, status)
                .onSuccess { list ->
                    _students.value = list
                    _loading.value = false
                }
                .onFailure { err ->
                    _error.value = err.localizedMessage ?: "Failed to load students"
                    _loading.value = false
                }
        }
    }

    fun loadStudentDetails(studentId: String) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            hostelRepository.getStudentDetails(studentId)
                .onSuccess { student ->
                    _selectedStudent.value = student
                    _loading.value = false
                }
                .onFailure { err ->
                    _error.value = err.localizedMessage ?: "Failed to load student details"
                    _loading.value = false
                }
        }
    }

    fun addStudent(student: Student, email: String, password: String, onSuccess: () -> Unit) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            hostelRepository.addStudent(student, email, password)
                .onSuccess {
                    _loading.value = false
                    onSuccess()
                }
                .onFailure { err ->
                    _error.value = err.localizedMessage ?: "Failed to add student"
                    _loading.value = false
                }
        }
    }

    fun updateStudent(student: Student, onSuccess: () -> Unit) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            hostelRepository.updateStudent(student)
                .onSuccess {
                    _loading.value = false
                    onSuccess()
                }
                .onFailure { err ->
                    _error.value = err.localizedMessage ?: "Failed to update student"
                    _loading.value = false
                }
        }
    }

    fun vacateStudent(studentId: String, onSuccess: () -> Unit) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            hostelRepository.vacateStudent(studentId)
                .onSuccess {
                    _loading.value = false
                    onSuccess()
                }
                .onFailure { err ->
                    _error.value = err.localizedMessage ?: "Failed to vacate student"
                    _loading.value = false
                }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
