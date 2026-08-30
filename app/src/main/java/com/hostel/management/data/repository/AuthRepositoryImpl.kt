package com.hostel.management.data.repository

import com.hostel.management.data.dto.ProfileDto
import com.hostel.management.domain.model.Profile
import com.hostel.management.domain.model.UserRole
import com.hostel.management.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AuthRepositoryImpl(
    private val supabaseClient: SupabaseClient
) : AuthRepository {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val _sessionProfile = MutableStateFlow<Profile?>(null)

    init {
        scope.launch {
            supabaseClient.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val userId = status.session.user?.id
                        if (userId != null) {
                            fetchProfile(userId).onSuccess { profile ->
                                _sessionProfile.value = profile
                            }.onFailure {
                                _sessionProfile.value = null
                            }
                        } else {
                            _sessionProfile.value = null
                        }
                    }
                    else -> {
                        _sessionProfile.value = null
                    }
                }
            }
        }
    }

    private suspend fun fetchProfile(userId: String): Result<Profile> = runCatching {
        val dto = supabaseClient.postgrest.from("profiles")
            .select {
                filter {
                    eq("id", userId)
                }
            }
            .decodeSingle<ProfileDto>()
        
        Profile(
            id = dto.id,
            organizationId = dto.organizationId,
            role = UserRole.valueOf(dto.role),
            fullName = dto.fullName,
            email = dto.email,
            phone = dto.phone,
            avatarUrl = dto.avatarUrl
        )
    }

    override suspend fun login(email: String, password: String): Result<Profile> = runCatching {
        supabaseClient.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        
        val session = supabaseClient.auth.currentSessionOrNull()
            ?: throw IllegalStateException("Session not established after successful login")
        val userId = session.user?.id 
            ?: throw IllegalStateException("User ID not found in session")
            
        val profile = fetchProfile(userId).getOrThrow()
        _sessionProfile.value = profile
        profile
    }

    override suspend fun logout(): Result<Unit> = runCatching {
        supabaseClient.auth.signOut()
        _sessionProfile.value = null
    }

    override suspend fun getCurrentProfile(): Result<Profile?> = runCatching {
        val session = supabaseClient.auth.currentSessionOrNull()
        if (session != null) {
            val userId = session.user?.id ?: return@runCatching null
            val profile = fetchProfile(userId).getOrThrow()
            _sessionProfile.value = profile
            profile
        } else {
            _sessionProfile.value = null
            null
        }
    }

    override fun getSessionFlow(): Flow<Profile?> {
        return _sessionProfile.asStateFlow()
    }
}
