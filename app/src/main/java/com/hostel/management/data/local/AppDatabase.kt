package com.hostel.management.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hostel.management.data.local.dao.*
import com.hostel.management.data.local.entity.*

@Database(
    entities = [
        HostelEntity::class,
        BuildingEntity::class,
        FloorEntity::class,
        RoomEntity::class,
        BedEntity::class,
        ProfileEntity::class,
        StudentEntity::class,
        RoomAllocationEntity::class,
        FeeInvoiceEntity::class,
        PaymentEntity::class,
        ComplaintEntity::class,
        ComplaintCommentEntity::class,
        AnnouncementEntity::class,
        SyncQueueEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun hostelDao(): HostelDao
    abstract fun studentDao(): StudentDao
    abstract fun allocationDao(): AllocationDao
    abstract fun financeDao(): FinanceDao
    abstract fun complaintDao(): ComplaintDao
    abstract fun announcementDao(): AnnouncementDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hostel_management.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
