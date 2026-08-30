package com.hostel.management.domain.repository

import com.hostel.management.domain.model.Announcement
import com.hostel.management.domain.model.AuditLog
import com.hostel.management.domain.model.Notification

interface AnnouncementRepository {
    suspend fun getAnnouncements(): Result<List<Announcement>>
    suspend fun createAnnouncement(
        title: String,
        message: String,
        targetAudience: String, // "ALL", "HOSTEL", "BUILDING"
        targetHostelId: String?,
        targetBuildingId: String?,
        priority: String
    ): Result<Unit>
    
    suspend fun getNotifications(): Result<List<Notification>>
    suspend fun markNotificationRead(notificationId: String): Result<Unit>
    suspend fun getAuditLogs(): Result<List<AuditLog>>
}
