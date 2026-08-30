package com.hostel.management.domain.repository

import com.hostel.management.domain.model.Complaint
import com.hostel.management.domain.model.ComplaintComment
import com.hostel.management.domain.model.Profile

interface ComplaintRepository {
    suspend fun getComplaints(
        studentId: String? = null,
        statusFilter: String? = null,
        categoryFilter: String? = null
    ): Result<List<Complaint>>
    
    suspend fun createComplaint(
        category: String,
        title: String,
        description: String,
        priority: String,
        imageBytes: ByteArray? = null,
        fileExtension: String? = null
    ): Result<Complaint>
    
    suspend fun updateComplaintStatus(complaintId: String, status: String): Result<Unit>
    suspend fun assignComplaint(complaintId: String, staffId: String, notes: String?): Result<Unit>
    suspend fun getComplaintComments(complaintId: String): Result<List<ComplaintComment>>
    
    suspend fun addComplaintComment(
        complaintId: String,
        comment: String,
        imageBytes: ByteArray? = null,
        fileExtension: String? = null
    ): Result<Unit>
    
    suspend fun getMaintenanceStaffList(): Result<List<Profile>>
    suspend fun getAssignedComplaints(staffId: String): Result<List<Complaint>>
}
