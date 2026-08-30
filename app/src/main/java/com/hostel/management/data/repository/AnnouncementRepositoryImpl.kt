package com.hostel.management.data.repository

import com.hostel.management.data.dto.AnnouncementDto
import com.hostel.management.data.dto.AuditLogDto
import com.hostel.management.data.dto.NotificationDto
import com.hostel.management.data.dto.ProfileDto
import com.hostel.management.domain.model.Announcement
import com.hostel.management.domain.model.AuditLog
import com.hostel.management.domain.model.Notification
import com.hostel.management.domain.repository.AnnouncementRepository
import com.hostel.management.di.ServiceLocator
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AnnouncementRepositoryImpl(
    private val supabaseClient: SupabaseClient
) : AnnouncementRepository {

    override suspend fun getAnnouncements(): Result<List<Announcement>> = runCatching {
        supabaseClient.postgrest.from("announcements").select().decodeList<AnnouncementDto>().map { dto ->
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

        supabaseClient.postgrest.from("announcements").insert(
            buildJsonObject {
                put("organization_id", orgId)
                put("title", title)
                put("message", message)
                put("target_audience", targetAudience)
                if (targetHostelId != null) put("target_hostel_id", targetHostelId)
                if (targetBuildingId != null) put("target_building_id", targetBuildingId)
                put("priority", priority)
            }
        )
    }

    override suspend fun getNotifications(): Result<List<Notification>> = runCatching {
        val user = ServiceLocator.authRepository.getCurrentProfile().getOrThrow()
            ?: throw IllegalStateException("User not logged in")

        supabaseClient.postgrest.from("notifications").select {
            filter {
                eq("user_id", user.id)
            }
        }.decodeList<NotificationDto>().map { dto ->
            Notification(
                id = dto.id,
                userId = dto.userId,
                title = dto.title,
                message = dto.message,
                type = dto.type,
                relatedEntityId = dto.relatedEntityId,
                isRead = dto.isRead,
                createdAt = dto.createdAt
            )
        }
    }

    override suspend fun markNotificationRead(notificationId: String): Result<Unit> = runCatching {
        supabaseClient.postgrest.from("notifications").update(
            mapOf("is_read" to true)
        ) {
            filter {
                eq("id", notificationId)
            }
        }
    }

    override suspend fun getAuditLogs(): Result<List<AuditLog>> = runCatching {
        val listDto = supabaseClient.postgrest.from("audit_logs").select().decodeList<AuditLogDto>()
        if (listDto.isEmpty()) return Result.success(emptyList())

        val profiles = supabaseClient.postgrest.from("profiles").select().decodeList<ProfileDto>().associateBy { it.id }

        listDto.map { dto ->
            val profile = dto.userId?.let { profiles[it] }
            AuditLog(
                id = dto.id,
                organizationId = dto.organizationId,
                userId = dto.userId,
                userName = profile?.fullName,
                action = dto.action,
                entityName = dto.entityName,
                entityId = dto.entityId,
                metadata = dto.metadata,
                createdAt = dto.createdAt
            )
        }
    }
}
