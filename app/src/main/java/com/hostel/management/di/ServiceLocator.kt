package com.hostel.management.di

import android.content.Context
import com.hostel.management.data.local.AppDatabase
import com.hostel.management.data.repository.*
import com.hostel.management.data.sync.SyncManager
import com.hostel.management.data.sync.SyncPreferences
import com.hostel.management.domain.repository.*
import com.hostel.management.utils.SupabaseConstants
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

import io.ktor.client.engine.android.Android

object ServiceLocator {
    private lateinit var appContext: Context

    val supabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SupabaseConstants.SUPABASE_URL,
            supabaseKey = SupabaseConstants.SUPABASE_ANON_KEY
        ) {
            httpEngine = Android.create {
                connectTimeout = 30_000
                socketTimeout = 30_000
            }
            defaultSerializer = KotlinXSerializer(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
            install(Postgrest)
            install(Auth)
            install(Storage)
            install(Realtime)
        }
    }

    val database by lazy {
        AppDatabase.getInstance(appContext)
    }

    val syncPreferences by lazy {
        SyncPreferences(appContext)
    }

    val syncManager by lazy {
        SyncManager(database, supabaseClient, syncPreferences)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(supabaseClient)
    }

    val hostelRepository: HostelRepository by lazy {
        HostelRepositoryImpl(supabaseClient, database, syncManager)
    }

    val financeRepository: FinanceRepository by lazy {
        FinanceRepositoryImpl(supabaseClient, database, syncManager)
    }

    val complaintRepository: ComplaintRepository by lazy {
        ComplaintRepositoryImpl(supabaseClient, database, syncManager)
    }

    val announcementRepository: AnnouncementRepository by lazy {
        AnnouncementRepositoryImpl(supabaseClient, database, syncManager)
    }

    val organizationRepository: OrganizationRepository by lazy {
        OrganizationRepositoryImpl(supabaseClient)
    }

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }
}
