package com.hostel.management.domain.model

data class Complaint(
    val id: String,
    val studentId: String,
    val studentName: String? = null,
    val roomNumber: String? = null,
    val category: String, // "Electrical", "Plumbing", etc.
    val title: String,
    val description: String,
    val priority: String, // "Low", "Medium", "High", "Emergency"
    val imageUrl: String? = null,
    val status: String, // "Submitted", "Acknowledged", "Assigned", "In Progress", "Waiting", "Resolved", "Closed", "Reopened"
    val createdAt: String,
    val updatedAt: String
)

data class ComplaintAssignment(
    val id: String,
    val complaintId: String,
    val staffId: String,
    val staffName: String? = null,
    val assignedAt: String,
    val resolvedAt: String? = null,
    val notes: String? = null
)

data class ComplaintComment(
    val id: String,
    val complaintId: String,
    val userId: String,
    val userName: String? = null,
    val comment: String,
    val imageUrl: String? = null,
    val createdAt: String
)
