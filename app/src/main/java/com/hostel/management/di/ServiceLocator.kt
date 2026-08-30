package com.hostel.management.di

import android.content.Context
import com.hostel.management.data.repository.*
import com.hostel.management.domain.repository.*
import com.hostel.management.utils.SupabaseConstants
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.realtime.Realtime

object ServiceLocator {
    private lateinit var appContext: Context

    val supabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SupabaseConstants.SUPABASE_URL,
            supabaseKey = SupabaseConstants.SUPABASE_ANON_KEY
        ) {
            install(Postgrest)
            install(Auth)
            install(Storage)
            install(Realtime)
        }
    }

    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(supabaseClient)
    }

    val hostelRepository: HostelRepository by lazy {
        HostelRepositoryImpl(supabaseClient)
    }

    val financeRepository: FinanceRepository by lazy {
        FinanceRepositoryImpl(supabaseClient)
    }

    val complaintRepository: ComplaintRepository by lazy {
        ComplaintRepositoryImpl(supabaseClient)
    }

    val announcementRepository: AnnouncementRepository by lazy {
        AnnouncementRepositoryImpl(supabaseClient)
    }

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }
}
