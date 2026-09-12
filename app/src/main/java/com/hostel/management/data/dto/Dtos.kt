package com.hostel.management.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrganizationDto(
    val id: String,
    val name: String,
    val domain: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    val status: String,
    @SerialName("subscription_plan") val subscriptionPlan: String,
    @SerialName("pricing_model") val pricingModel: String? = "Slab"
)

@Serializable
data class SaasPricingConfigDto(
    val id: String,
    @SerialName("per_student_rate") val perStudentRate: Double,
    @SerialName("slab1_max_students") val slab1MaxStudents: Int,
    @SerialName("slab1_price") val slab1Price: Double,
    @SerialName("slab2_max_students") val slab2MaxStudents: Int,
    @SerialName("slab2_price") val slab2Price: Double,
    @SerialName("slab3_max_students") val slab3MaxStudents: Int,
    @SerialName("slab3_price") val slab3Price: Double
)

@Serializable
data class ProfileDto(
    val id: String,
    @SerialName("organization_id") val organizationId: String? = null,
    val role: String,
    @SerialName("full_name") val fullName: String,
    val email: String,
    val phone: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

@Serializable
data class StudentDto(
    val id: String,
    @SerialName("student_id_number") val studentIdNumber: String,
    val dob: String? = null,
    val gender: String? = null,
    val course: String? = null,
    val department: String? = null,
    @SerialName("academic_year") val academicYear: String? = null,
    @SerialName("parent_name") val parentName: String? = null,
    @SerialName("parent_phone") val parentPhone: String? = null,
    @SerialName("emergency_contact") val emergencyContact: String? = null,
    val address: String? = null,
    @SerialName("admission_date") val admissionDate: String,
    @SerialName("hostel_status") val hostelStatus: String,
    @SerialName("joining_date") val joiningDate: String? = null,
    @SerialName("leaving_date") val leavingDate: String? = null
)

@Serializable
data class HostelDto(
    val id: String,
    @SerialName("organization_id") val organizationId: String,
    val name: String,
    val address: String? = null,
    @SerialName("upi_id") val upiId: String? = null,
    @SerialName("monthly_fee") val monthlyFee: Double? = null,
    @SerialName("advance_deposit") val advanceDeposit: Double? = null
)

@Serializable
data class BuildingDto(
    val id: String,
    @SerialName("hostel_id") val hostelId: String,
    val name: String
)

@Serializable
data class FloorDto(
    val id: String,
    @SerialName("building_id") val buildingId: String,
    @SerialName("floor_number") val floorNumber: Int
)

@Serializable
data class RoomDto(
    val id: String,
    @SerialName("floor_id") val floorId: String,
    @SerialName("room_number") val roomNumber: String,
    val capacity: Int,
    @SerialName("room_type") val roomType: String,
    val status: String,
    val notes: String? = null
)

@Serializable
data class BedDto(
    val id: String,
    @SerialName("room_id") val roomId: String,
    @SerialName("bed_number") val bedNumber: String,
    val status: String
)

@Serializable
data class RoomAllocationDto(
    val id: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("bed_id") val bedId: String,
    @SerialName("allocated_at") val allocatedAt: String,
    @SerialName("vacated_at") val vacatedAt: String? = null,
    val status: String,
    val notes: String? = null,
    @SerialName("recorded_by") val recordedBy: String? = null
)

@Serializable
data class FeeInvoiceDto(
    val id: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("billing_month") val billingMonth: String,
    val amount: Double,
    @SerialName("due_date") val dueDate: String,
    val discount: Double = 0.0,
    @SerialName("late_fee") val lateFee: Double = 0.0,
    @SerialName("amount_paid") val amountPaid: Double = 0.0,
    val balance: Double,
    @SerialName("payment_status") val paymentStatus: String,
    @SerialName("payment_date") val paymentDate: String? = null
)

@Serializable
data class PaymentDto(
    val id: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("fee_invoice_id") val feeInvoiceId: String,
    val amount: Double,
    @SerialName("payment_date") val paymentDate: String,
    @SerialName("payment_method") val paymentMethod: String,
    @SerialName("transaction_id") val transactionId: String? = null,
    @SerialName("recorded_by") val recordedBy: String? = null,
    val notes: String? = null
)

@Serializable
data class ComplaintDto(
    val id: String,
    @SerialName("student_id") val studentId: String,
    val category: String,
    val title: String,
    val description: String,
    val priority: String,
    @SerialName("image_url") val imageUrl: String? = null,
    val status: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class ComplaintAssignmentDto(
    val id: String,
    @SerialName("complaint_id") val complaintId: String,
    @SerialName("staff_id") val staffId: String,
    @SerialName("assigned_at") val assignedAt: String,
    @SerialName("resolved_at") val resolvedAt: String? = null,
    val notes: String? = null
)

@Serializable
data class ComplaintCommentDto(
    val id: String,
    @SerialName("complaint_id") val complaintId: String,
    @SerialName("user_id") val userId: String,
    val comment: String,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class AnnouncementDto(
    val id: String,
    @SerialName("organization_id") val organizationId: String,
    val title: String,
    val message: String,
    @SerialName("target_audience") val targetAudience: String,
    @SerialName("target_hostel_id") val targetHostelId: String? = null,
    @SerialName("target_building_id") val targetBuildingId: String? = null,
    @SerialName("publish_date") val publishDate: String,
    @SerialName("expiry_date") val expiryDate: String? = null,
    val priority: String,
    @SerialName("attachment_url") val attachmentUrl: String? = null,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class NotificationDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val title: String,
    val message: String,
    val type: String,
    @SerialName("related_entity_id") val relatedEntityId: String? = null,
    @SerialName("is_read") val isRead: Boolean,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class AuditLogDto(
    val id: String,
    @SerialName("organization_id") val organizationId: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val action: String,
    @SerialName("entity_name") val entityName: String,
    @SerialName("entity_id") val entityId: String? = null,
    val metadata: String? = null,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class StudentSelfRegistrationDto(
    val id: String,
    @SerialName("hostel_id") val hostelId: String? = null,
    @SerialName("full_name") val fullName: String,
    val email: String,
    val phone: String? = null,
    @SerialName("student_id_number") val studentIdNumber: String? = null,
    @SerialName("aadhaar_number") val aadhaarNumber: String? = null,
    @SerialName("room_number") val roomNumber: String? = null,
    @SerialName("bed_number") val bedNumber: String? = null,
    val status: String = "Pending",
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("utr_number") val utrNumber: String? = null,
    @SerialName("amount_paid") val amountPaid: Double? = 0.0
)

@Serializable
data class MonthlyPaymentSubmissionDto(
    val id: String,
    @SerialName("hostel_id") val hostelId: String? = null,
    @SerialName("student_id") val studentId: String? = null,
    val email: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("room_number") val roomNumber: String? = null,
    @SerialName("bed_number") val bedNumber: String? = null,
    val amount: Double,
    @SerialName("utr_number") val utrNumber: String,
    @SerialName("billing_month") val billingMonth: String,
    val status: String = "Pending",
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)
