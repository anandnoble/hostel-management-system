package com.hostel.management.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hostels")
data class HostelEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val name: String,
    val address: String? = null,
    val upiId: String? = null,
    val monthlyFee: Double? = 0.0,
    val advanceDeposit: Double? = 0.0
)

@Entity(tableName = "buildings")
data class BuildingEntity(
    @PrimaryKey val id: String,
    val hostelId: String,
    val name: String
)

@Entity(tableName = "floors")
data class FloorEntity(
    @PrimaryKey val id: String,
    val buildingId: String,
    val floorNumber: Int
)

@Entity(tableName = "rooms")
data class RoomEntity(
    @PrimaryKey val id: String,
    val floorId: String,
    val roomNumber: String,
    val capacity: Int,
    val roomType: String,
    val status: String,
    val notes: String? = null
)

@Entity(tableName = "beds")
data class BedEntity(
    @PrimaryKey val id: String,
    val roomId: String,
    val bedNumber: String,
    val status: String
)

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val organizationId: String? = null,
    val role: String,
    val fullName: String,
    val email: String,
    val phone: String? = null,
    val avatarUrl: String? = null
)

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey val id: String,
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
    val hostelStatus: String,
    val joiningDate: String? = null,
    val leavingDate: String? = null
)

@Entity(tableName = "room_allocations")
data class RoomAllocationEntity(
    @PrimaryKey val id: String,
    val studentId: String,
    val bedId: String,
    val allocatedAt: String,
    val vacatedAt: String? = null,
    val status: String,
    val notes: String? = null,
    val recordedBy: String? = null
)

@Entity(tableName = "fee_invoices")
data class FeeInvoiceEntity(
    @PrimaryKey val id: String,
    val studentId: String,
    val billingMonth: String,
    val amount: Double,
    val dueDate: String,
    val discount: Double = 0.0,
    val lateFee: Double = 0.0,
    val amountPaid: Double = 0.0,
    val balance: Double,
    val paymentStatus: String,
    val paymentDate: String? = null
)

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey val id: String,
    val studentId: String,
    val feeInvoiceId: String,
    val amount: Double,
    val paymentDate: String,
    val paymentMethod: String,
    val transactionId: String? = null,
    val recordedBy: String? = null,
    val notes: String? = null
)

@Entity(tableName = "complaints")
data class ComplaintEntity(
    @PrimaryKey val id: String,
    val studentId: String,
    val category: String,
    val title: String,
    val description: String,
    val priority: String,
    val imageUrl: String? = null,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)

@Entity(tableName = "complaint_comments")
data class ComplaintCommentEntity(
    @PrimaryKey val id: String,
    val complaintId: String,
    val userId: String,
    val comment: String,
    val imageUrl: String? = null,
    val createdAt: String
)

@Entity(tableName = "announcements")
data class AnnouncementEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val title: String,
    val message: String,
    val targetAudience: String,
    val targetHostelId: String? = null,
    val targetBuildingId: String? = null,
    val publishDate: String,
    val expiryDate: String? = null,
    val priority: String,
    val attachmentUrl: String? = null,
    val createdAt: String
)

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val entityType: String,
    val entityId: String,
    val action: String, // "INSERT", "UPDATE", "DELETE"
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis()
)
