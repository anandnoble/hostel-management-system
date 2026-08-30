package com.hostel.management.data.repository

import com.hostel.management.data.dto.*
import com.hostel.management.domain.model.*
import com.hostel.management.domain.repository.ComplaintRepository
import com.hostel.management.di.ServiceLocator
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ComplaintRepositoryImpl(
    private val supabaseClient: SupabaseClient
) : ComplaintRepository {

    override suspend fun getComplaints(
        studentId: String?,
        statusFilter: String?,
        categoryFilter: String?
    ): Result<List<Complaint>> = runCatching {
        var query = supabaseClient.postgrest.from("complaints").select()
        val listDto = query.decodeList<ComplaintDto>()
        if (listDto.isEmpty()) return Result.success(emptyList())

        var filtered = listDto
        if (studentId != null) {
            filtered = filtered.filter { it.studentId == studentId }
        }
        if (statusFilter != null) {
            filtered = filtered.filter { it.status == statusFilter }
        }
        if (categoryFilter != null) {
            filtered = filtered.filter { it.category == categoryFilter }
        }

        val studentProfiles = supabaseClient.postgrest.from("profiles").select().decodeList<ProfileDto>().associateBy { it.id }

        filtered.map { dto ->
            val profile = studentProfiles[dto.studentId]
            Complaint(
                id = dto.id,
                studentId = dto.studentId,
                studentName = profile?.fullName,
                category = dto.category,
                title = dto.title,
                description = dto.description,
                priority = dto.priority,
                imageUrl = dto.imageUrl,
                status = dto.status,
                createdAt = dto.createdAt,
                updatedAt = dto.updatedAt
            )
        }
    }

    override suspend fun createComplaint(
        category: String,
        title: String,
        description: String,
        priority: String,
        imageBytes: ByteArray?,
        fileExtension: String?
    ): Result<Complaint> = runCatching {
        val student = ServiceLocator.authRepository.getCurrentProfile().getOrThrow()
            ?: throw IllegalStateException("User not logged in")

        var uploadedUrl: String? = null
        if (imageBytes != null) {
            // Upload to Supabase Storage
            val fileName = "complaint_${UUID.randomUUID()}.${fileExtension ?: "jpg"}"
            val bucket = supabaseClient.storage.from("complaints")
            bucket.upload(fileName, imageBytes, upsert = true)
            uploadedUrl = bucket.publicUrl(fileName)
        }

        val nowString = SimpleDateFormat("yyyy-MM-dd HH:mm:ssZZZZZ", Locale.US).format(Date())

        val dto = supabaseClient.postgrest.from("complaints").insert(
            buildJsonObject {
                put("student_id", student.id)
                put("category", category)
                put("title", title)
                put("description", description)
                put("priority", priority)
                if (uploadedUrl != null) put("image_url", uploadedUrl)
                put("status", "Submitted")
            }
        ) { select() }.decodeSingle<ComplaintDto>()

        Complaint(
            id = dto.id,
            studentId = dto.studentId,
            studentName = student.fullName,
            category = dto.category,
            title = dto.title,
            description = dto.description,
            priority = dto.priority,
            imageUrl = dto.imageUrl,
            status = dto.status,
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt
        )
    }

    override suspend fun updateComplaintStatus(complaintId: String, status: String): Result<Unit> = runCatching {
        val nowString = SimpleDateFormat("yyyy-MM-dd HH:mm:ssZZZZZ", Locale.US).format(Date())

        supabaseClient.postgrest.from("complaints").update(
            buildJsonObject {
                put("status", status)
                put("updated_at", nowString)
            }
        ) {
            filter { eq("id", complaintId) }
        }
    }

    override suspend fun assignComplaint(
        complaintId: String,
        staffId: String,
        notes: String?
    ): Result<Unit> = runCatching {
        // Create complaint assignment
        supabaseClient.postgrest.from("complaint_assignments").insert(
            buildJsonObject {
                put("complaint_id", complaintId)
                put("staff_id", staffId)
                if (notes != null) put("notes", notes)
            }
        )

        // Update status of complaint to Assigned
        updateComplaintStatus(complaintId, "Assigned").getOrThrow()
    }

    override suspend fun getComplaintComments(complaintId: String): Result<List<ComplaintComment>> = runCatching {
        val listDto = supabaseClient.postgrest.from("complaint_comments").select {
            filter { eq("complaint_id", complaintId) }
        }.decodeList<ComplaintCommentDto>()

        if (listDto.isEmpty()) return Result.success(emptyList())

        val profiles = supabaseClient.postgrest.from("profiles").select().decodeList<ProfileDto>().associateBy { it.id }

        listDto.map { dto ->
            val profile = profiles[dto.userId]
            ComplaintComment(
                id = dto.id,
                complaintId = dto.complaintId,
                userId = dto.userId,
                userName = profile?.fullName,
                comment = dto.comment,
                imageUrl = dto.imageUrl,
                createdAt = dto.createdAt
            )
        }
    }

    override suspend fun addComplaintComment(
        complaintId: String,
        comment: String,
        imageBytes: ByteArray?,
        fileExtension: String?
    ): Result<Unit> = runCatching {
        val user = ServiceLocator.authRepository.getCurrentProfile().getOrThrow()
            ?: throw IllegalStateException("User not logged in")

        var uploadedUrl: String? = null
        if (imageBytes != null) {
            val fileName = "comment_${UUID.randomUUID()}.${fileExtension ?: "jpg"}"
            val bucket = supabaseClient.storage.from("complaints")
            bucket.upload(fileName, imageBytes, upsert = true)
            uploadedUrl = bucket.publicUrl(fileName)
        }

        supabaseClient.postgrest.from("complaint_comments").insert(
            buildJsonObject {
                put("complaint_id", complaintId)
                put("user_id", user.id)
                put("comment", comment)
                if (uploadedUrl != null) put("image_url", uploadedUrl)
            }
        )
    }

    override suspend fun getMaintenanceStaffList(): Result<List<Profile>> = runCatching {
        supabaseClient.postgrest.from("profiles").select {
            filter {
                eq("role", "MAINTENANCE_STAFF")
            }
        }.decodeList<ProfileDto>().map { dto ->
            Profile(
                id = dto.id,
                organizationId = dto.organizationId,
                role = UserRole.MAINTENANCE_STAFF,
                fullName = dto.fullName,
                email = dto.email,
                phone = dto.phone,
                avatarUrl = dto.avatarUrl
            )
        }
    }

    override suspend fun getAssignedComplaints(staffId: String): Result<List<Complaint>> = runCatching {
        val assignments = supabaseClient.postgrest.from("complaint_assignments").select {
            filter {
                eq("staff_id", staffId)
            }
        }.decodeList<ComplaintAssignmentDto>()

        if (assignments.isEmpty()) return Result.success(emptyList())

        val complaintIds = assignments.map { it.complaintId }
        val allComplaints = supabaseClient.postgrest.from("complaints").select().decodeList<ComplaintDto>()
        
        val filtered = allComplaints.filter { it.id in complaintIds }
        val studentProfiles = supabaseClient.postgrest.from("profiles").select().decodeList<ProfileDto>().associateBy { it.id }

        filtered.map { dto ->
            val profile = studentProfiles[dto.studentId]
            Complaint(
                id = dto.id,
                studentId = dto.studentId,
                studentName = profile?.fullName,
                category = dto.category,
                title = dto.title,
                description = dto.description,
                priority = dto.priority,
                imageUrl = dto.imageUrl,
                status = dto.status,
                createdAt = dto.createdAt,
                updatedAt = dto.updatedAt
            )
        }
    }
}
