package com.hostel.management.domain.repository

import com.hostel.management.domain.model.Profile
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<Profile>
    suspend fun logout(): Result<Unit>
    suspend fun getCurrentProfile(): Result<Profile?>
    fun getSessionFlow(): Flow<Profile?>
}
