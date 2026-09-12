package com.hostel.management.data.repository

import com.hostel.management.data.local.AppDatabase
import com.hostel.management.data.local.entity.ComplaintCommentEntity
import com.hostel.management.data.local.entity.ComplaintEntity
import com.hostel.management.data.local.entity.SyncQueueEntity
import com.hostel.management.data.sync.SyncManager
import com.hostel.management.domain.model.*
import com.hostel.management.domain.repository.ComplaintRepository
import com.hostel.management.di.ServiceLocator
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ComplaintRepositoryImpl(
    private val supabaseClient: SupabaseClient,
    private val db: AppDatabase,
    private val syncManager: SyncManager
) : ComplaintRepository {

    override suspend fun getComplaints(
        studentId: String?,
        statusFilter: String?,
        categoryFilter: String?
    ): Result<List<Complaint>> = runCatching {
        var listDto = if (studentId != null) {
            db.complaintDao().getComplaintsForStudent(studentId)
        } else {
            db.complaintDao().getAllComplaints()
        }

        if (listDto.isEmpty()) {
            syncManager.pullCloudToLocal()
            listDto = if (studentId != null) {
                db.complaintDao().getComplaintsForStudent(studentId)
            } else {
                db.complaintDao().getAllComplaints()
            }
        }

        var filtered = listDto
        if (statusFilter != null) {
            filtered = filtered.filter { it.status == statusFilter }
        }
        if (categoryFilter != null) {
            filtered = filtered.filter { it.category == categoryFilter }
        }

        val studentProfiles = db.studentDao().getAllStudentProfiles().associateBy { it.id }

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
            runCatching {
                val fileName = "complaint_${UUID.randomUUID()}.${fileExtension ?: "jpg"}"
                val bucket = supabaseClient.storage.from("complaints")
                bucket.upload(fileName, imageBytes, upsert = true)
                uploadedUrl = bucket.publicUrl(fileName)
            }
        }

        val newId = UUID.randomUUID().toString()
        val nowString = SimpleDateFormat("yyyy-MM-dd HH:mm:ssZZZZZ", Locale.US).format(Date())

        val entity = ComplaintEntity(
            id = newId,
            studentId = student.id,
            category = category,
            title = title,
            description = description,
            priority = priority,
            imageUrl = uploadedUrl,
            status = "Submitted",
            createdAt = nowString,
            updatedAt = nowString
        )

        db.complaintDao().insertComplaint(entity)

        val payload = buildJsonObject {
            put("id", newId)
            put("student_id", student.id)
            put("category", category)
            put("title", title)
            put("description", description)
            put("priority", priority)
            if (uploadedUrl != null) put("image_url", uploadedUrl)
            put("status", "Submitted")
            put("created_at", nowString)
            put("updated_at", nowString)
        }.toString()

        db.syncQueueDao().enqueueSyncItem(
            SyncQueueEntity(
                entityType = "COMPLAINT",
                entityId = newId,
                action = "INSERT",
                payloadJson = payload
            )
        )

        syncManager.triggerAutoSyncIfEnabled()

        Complaint(
            id = newId,
            studentId = student.id,
            studentName = student.fullName,
            category = category,
            title = title,
            description = description,
            priority = priority,
            imageUrl = uploadedUrl,
            status = "Submitted",
            createdAt = nowString,
            updatedAt = nowString
        )
    }

    override suspend fun updateComplaintStatus(complaintId: String, status: String): Result<Unit> = runCatching {
        val nowString = SimpleDateFormat("yyyy-MM-dd HH:mm:ssZZZZZ", Locale.US).format(Date())
        db.complaintDao().updateComplaintStatus(complaintId, status, nowString)

        val payload = buildJsonObject {
            put("status", status)
            put("updated_at", nowString)
        }.toString()

        db.syncQueueDao().enqueueSyncItem(
            SyncQueueEntity(
                entityType = "COMPLAINT",
                entityId = complaintId,
                action = "UPDATE",
                payloadJson = payload
            )
        )

        syncManager.triggerAutoSyncIfEnabled()
    }

    override suspend fun assignComplaint(
        complaintId: String,
        staffId: String,
        notes: String?
    ): Result<Unit> = runCatching {
        updateComplaintStatus(complaintId, "Assigned").getOrThrow()
    }

    override suspend fun getComplaintComments(complaintId: String): Result<List<ComplaintComment>> = runCatching {
        var listDto = db.complaintDao().getCommentsForComplaint(complaintId)
        if (listDto.isEmpty()) {
            syncManager.pullCloudToLocal()
            listDto = db.complaintDao().getCommentsForComplaint(complaintId)
        }

        val profiles = db.studentDao().getAllStudentProfiles().associateBy { it.id }

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
            runCatching {
                val fileName = "comment_${UUID.randomUUID()}.${fileExtension ?: "jpg"}"
                val bucket = supabaseClient.storage.from("complaints")
                bucket.upload(fileName, imageBytes, upsert = true)
                uploadedUrl = bucket.publicUrl(fileName)
            }
        }

        val newId = UUID.randomUUID().toString()
        val nowString = SimpleDateFormat("yyyy-MM-dd HH:mm:ssZZZZZ", Locale.US).format(Date())

        val entity = ComplaintCommentEntity(
            id = newId,
            complaintId = complaintId,
            userId = user.id,
            comment = comment,
            imageUrl = uploadedUrl,
            createdAt = nowString
        )

        db.complaintDao().insertComment(entity)

        val payload = buildJsonObject {
            put("id", newId)
            put("complaint_id", complaintId)
            put("user_id", user.id)
            put("comment", comment)
            if (uploadedUrl != null) put("image_url", uploadedUrl)
            put("created_at", nowString)
        }.toString()

        db.syncQueueDao().enqueueSyncItem(
            SyncQueueEntity(
                entityType = "COMPLAINT_COMMENT",
                entityId = newId,
                action = "INSERT",
                payloadJson = payload
            )
        )

        syncManager.triggerAutoSyncIfEnabled()
    }

    override suspend fun getMaintenanceStaffList(): Result<List<Profile>> = runCatching {
        var staff = db.complaintDao().getMaintenanceStaff()
        if (staff.isEmpty()) {
            syncManager.pullCloudToLocal()
            staff = db.complaintDao().getMaintenanceStaff()
        }
        staff.map { dto ->
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

    override suspend fun getAssignedComplaints(staffId: String): Result<List<Complaint>> =
        getComplaints(studentId = null, statusFilter = "Assigned", categoryFilter = null)
}
