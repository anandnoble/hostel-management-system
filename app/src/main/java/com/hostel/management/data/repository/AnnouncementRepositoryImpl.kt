package com.hostel.management.data.repository

import com.hostel.management.data.local.AppDatabase
import com.hostel.management.data.local.entity.AnnouncementEntity
import com.hostel.management.data.local.entity.SyncQueueEntity
import com.hostel.management.data.sync.SyncManager
import com.hostel.management.domain.model.Announcement
import com.hostel.management.domain.model.AuditLog
import com.hostel.management.domain.model.Notification
import com.hostel.management.domain.repository.AnnouncementRepository
import com.hostel.management.di.ServiceLocator
import io.github.jan.supabase.SupabaseClient
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AnnouncementRepositoryImpl(
    private val supabaseClient: SupabaseClient,
    private val db: AppDatabase,
    private val syncManager: SyncManager
) : AnnouncementRepository {

    override suspend fun getAnnouncements(): Result<List<Announcement>> = runCatching {
        var local = db.announcementDao().getAllAnnouncements()
        if (local.isEmpty()) {
            syncManager.pullCloudToLocal()
            local = db.announcementDao().getAllAnnouncements()
        }
        local.map { dto ->
            Announcement(
                id = dto.id,
                organizationId = dto.organizationId,
                title = dto.title,
                message = dto.message,
                targetAudience = dto.targetAudience,
                targetHostelId = dto.targetHostelId,
                targetBuildingId = dto.targetBuildingId,
                publishDate = dto.publishDate,
                expiryDate = dto.expiryDate,
                priority = dto.priority,
                attachmentUrl = dto.attachmentUrl,
                createdAt = dto.createdAt
            )
        }
    }

    override suspend fun createAnnouncement(
        title: String,
        message: String,
        targetAudience: String,
        targetHostelId: String?,
        targetBuildingId: String?,
        priority: String
    ): Result<Unit> = runCatching {
        val user = ServiceLocator.authRepository.getCurrentProfile().getOrThrow()
            ?: throw IllegalStateException("User not logged in")
        val orgId = user.organizationId
            ?: throw IllegalStateException("User has no organization")

        val newId = UUID.randomUUID().toString()
        val nowString = SimpleDateFormat("yyyy-MM-dd HH:mm:ssZZZZZ", Locale.US).format(Date())

        val entity = AnnouncementEntity(
            id = newId,
            organizationId = orgId,
            title = title,
            message = message,
            targetAudience = targetAudience,
            targetHostelId = targetHostelId,
            targetBuildingId = targetBuildingId,
            publishDate = nowString,
            expiryDate = null,
            priority = priority,
            attachmentUrl = null,
            createdAt = nowString
        )

        db.announcementDao().insertAnnouncement(entity)

        val payload = buildJsonObject {
            put("id", newId)
            put("organization_id", orgId)
            put("title", title)
            put("message", message)
            put("target_audience", targetAudience)
            if (targetHostelId != null) put("target_hostel_id", targetHostelId)
            if (targetBuildingId != null) put("target_building_id", targetBuildingId)
            put("priority", priority)
            put("publish_date", nowString)
            put("created_at", nowString)
        }.toString()

        db.syncQueueDao().enqueueSyncItem(
            SyncQueueEntity(
                entityType = "ANNOUNCEMENT",
                entityId = newId,
                action = "INSERT",
                payloadJson = payload
            )
        )

        syncManager.triggerAutoSyncIfEnabled()
    }

    override suspend fun getNotifications(): Result<List<Notification>> = runCatching {
        emptyList()
    }

    override suspend fun markNotificationRead(notificationId: String): Result<Unit> = runCatching {
        Unit
    }

    override suspend fun getAuditLogs(): Result<List<AuditLog>> = runCatching {
        emptyList()
    }
}
