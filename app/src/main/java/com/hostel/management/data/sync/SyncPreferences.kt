package com.hostel.management.data.sync

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SyncPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("sync_preferences", Context.MODE_PRIVATE)

    private val _isAutoSyncEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_AUTO_SYNC, true)
    )
    val isAutoSyncEnabled: StateFlow<Boolean> = _isAutoSyncEnabled.asStateFlow()

    private val _lastSyncTimeFormatted = MutableStateFlow(
        prefs.getString(KEY_LAST_SYNC_TIME, "Never") ?: "Never"
    )
    val lastSyncTimeFormatted: StateFlow<String> = _lastSyncTimeFormatted.asStateFlow()

    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SYNC, enabled).apply()
        _isAutoSyncEnabled.value = enabled
    }

    fun updateLastSyncTime() {
        val nowFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        prefs.edit().putString(KEY_LAST_SYNC_TIME, nowFormatted).apply()
        _lastSyncTimeFormatted.value = nowFormatted
    }

    companion object {
        private const val KEY_AUTO_SYNC = "auto_sync_enabled"
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"
    }
}
