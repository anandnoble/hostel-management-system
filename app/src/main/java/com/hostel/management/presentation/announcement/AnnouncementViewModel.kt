package com.hostel.management.presentation.announcement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hostel.management.domain.model.Announcement
import com.hostel.management.domain.model.AuditLog
import com.hostel.management.domain.model.Notification
import com.hostel.management.domain.repository.AnnouncementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AnnouncementViewModel(
    private val announcementRepository: AnnouncementRepository
) : ViewModel() {

    private val _announcements = MutableStateFlow<List<Announcement>>(emptyList())
    val announcements: StateFlow<List<Announcement>> = _announcements.asStateFlow()

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<AuditLog>>(emptyList())
    val auditLogs: StateFlow<List<AuditLog>> = _auditLogs.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadAnnouncements() {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            announcementRepository.getAnnouncements()
                .onSuccess { list ->
                    _announcements.value = list
                    _loading.value = false
                }
                .onFailure { err ->
                    _error.value = err.localizedMessage ?: "Failed to load announcements"
                    _loading.value = false
                }
        }
    }

    fun createAnnouncement(
        title: String,
        message: String,
        targetAudience: String,
        targetHostelId: String?,
        targetBuildingId: String?,
        priority: String,
        onSuccess: () -> Unit
    ) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            announcementRepository.createAnnouncement(title, message, targetAudience, targetHostelId, targetBuildingId, priority)
                .onSuccess {
                    _loading.value = false
                    onSuccess()
                }
                .onFailure { err ->
                    _error.value = err.localizedMessage ?: "Failed to create announcement"
                    _loading.value = false
                }
        }
    }

    fun loadNotifications() {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            announcementRepository.getNotifications()
                .onSuccess { list ->
                    _notifications.value = list
                    _loading.value = false
                }
                .onFailure { err ->
                    _error.value = err.localizedMessage ?: "Failed to load notifications"
                    _loading.value = false
                }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            announcementRepository.markNotificationRead(notificationId)
                .onSuccess {
                    loadNotifications()
                }
        }
    }

    fun loadAuditLogs() {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            announcementRepository.getAuditLogs()
                .onSuccess { list ->
                    _auditLogs.value = list
                    _loading.value = false
                }
                .onFailure { err ->
                    _error.value = err.localizedMessage ?: "Failed to load audit logs"
                    _loading.value = false
                }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
