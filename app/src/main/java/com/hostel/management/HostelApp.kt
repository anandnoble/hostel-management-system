package com.hostel.management

import android.app.Application
import com.hostel.management.di.ServiceLocator

class HostelApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize our service locator (dependency injection) with the application context
        ServiceLocator.initialize(this)
    }
}
