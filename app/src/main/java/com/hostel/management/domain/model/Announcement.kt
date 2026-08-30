package com.hostel.management.domain.model

data class Announcement(
    val id: String,
    val organizationId: String,
    val title: String,
    val message: String,
    val targetAudience: String, // "ALL", "HOSTEL", "BUILDING"
    val targetHostelId: String? = null,
    val targetBuildingId: String? = null,
    val publishDate: String,
    val expiryDate: String? = null,
    val priority: String, // "Low", "Normal", "High"
    val attachmentUrl: String? = null,
    val createdAt: String
)

data class Notification(
    val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val type: String,
    val relatedEntityId: String? = null,
    val isRead: Boolean,
    val createdAt: String
)

data class AuditLog(
    val id: String,
    val organizationId: String?,
    val userId: String?,
    val userName: String? = null,
    val action: String,
    val entityName: String,
    val entityId: String?,
    val metadata: String?, // JSON string
    val createdAt: String
)
