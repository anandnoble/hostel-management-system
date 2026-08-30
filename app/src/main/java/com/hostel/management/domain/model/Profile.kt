package com.hostel.management.domain.model

data class Profile(
    val id: String,
    val organizationId: String?,
    val role: UserRole,
    val fullName: String,
    val email: String,
    val phone: String? = null,
    val avatarUrl: String? = null
)

data class Student(
    val id: String,
    val profile: Profile,
    val studentIdNumber: String,
    val dob: String? = null,
    val gender: String? = null,
    val course: String? = null,
    val department: String? = null,
    val academicYear: String? = null,
    val parentName: String? = null,
    val parentPhone: String? = null,
    val emergencyContact: String? = null,
    val address: String? = null,
    val admissionDate: String,
    val hostelStatus: String, // "Active", "Vacated", "Suspended", "Archived"
    val joiningDate: String? = null,
    val leavingDate: String? = null
)
