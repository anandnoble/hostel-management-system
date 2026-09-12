package com.hostel.management.data.sync

import android.util.Log
import com.hostel.management.data.dto.*
import com.hostel.management.data.local.AppDatabase
import com.hostel.management.data.local.entity.*
import com.hostel.management.util.toJsonObject
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class SyncManager(
    private val db: AppDatabase,
    private val supabaseClient: SupabaseClient,
    private val syncPreferences: SyncPreferences
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    val pendingSyncCount: Flow<Int> = db.syncQueueDao().observePendingCount()

    fun triggerAutoSyncIfEnabled() {
        if (syncPreferences.isAutoSyncEnabled.value && !_isSyncing.value) {
            scope.launch {
                syncAll()
            }
        }
    }

    suspend fun syncAll(): Result<Unit> = runCatching {
        if (_isSyncing.value) return Result.success(Unit)
        _isSyncing.value = true
        try {
            pushLocalChangesToCloud()
            pullCloudToLocal()
            syncPreferences.updateLastSyncTime()
        } catch (e: Exception) {
            Log.e("SyncManager", "Sync failed: ${e.message}", e)
            throw e
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun pushLocalChangesToCloud() {
        val pendingItems = db.syncQueueDao().getAllPendingSyncItems()
        if (pendingItems.isEmpty()) return

        for (item in pendingItems) {
            try {
                val payloadJsonObj = json.parseToJsonElement(item.payloadJson).jsonObject
                val table = getTableNameForEntity(item.entityType)

                when (item.action) {
                    "INSERT" -> {
                        supabaseClient.postgrest.from(table).insert(payloadJsonObj)
                    }
                    "UPDATE" -> {
                        supabaseClient.postgrest.from(table).update(payloadJsonObj) {
                            filter { eq("id", item.entityId) }
                        }
                    }
                    "DELETE" -> {
                        supabaseClient.postgrest.from(table).delete {
                            filter { eq("id", item.entityId) }
                        }
                    }
                }
                db.syncQueueDao().deleteSyncItem(item.id)
            } catch (e: Exception) {
                Log.w("SyncManager", "Failed to push item ${item.id} (${item.entityType}): ${e.message}")
                // Keep item in queue to retry later
            }
        }
    }

    suspend fun pullCloudToLocal() {
        // 1. Hostels
        runCatching {
            val dtos = supabaseClient.postgrest.from("hostels").select().decodeList<HostelDto>()
            val entities = dtos.map { HostelEntity(it.id, it.organizationId, it.name, it.address, it.upiId, it.monthlyFee, it.advanceDeposit) }
            db.hostelDao().insertHostels(entities)
        }

        // 2. Buildings
        runCatching {
            val dtos = supabaseClient.postgrest.from("buildings").select().decodeList<BuildingDto>()
            val entities = dtos.map { BuildingEntity(it.id, it.hostelId, it.name) }
            db.hostelDao().insertBuildings(entities)
        }

        // 3. Floors
        runCatching {
            val dtos = supabaseClient.postgrest.from("floors").select().decodeList<FloorDto>()
            val entities = dtos.map { FloorEntity(it.id, it.buildingId, it.floorNumber) }
            db.hostelDao().insertFloors(entities)
        }

        // 4. Rooms
        runCatching {
            val dtos = supabaseClient.postgrest.from("rooms").select().decodeList<RoomDto>()
            val entities = dtos.map { RoomEntity(it.id, it.floorId, it.roomNumber, it.capacity, it.roomType, it.status, it.notes) }
            db.hostelDao().insertRooms(entities)
        }

        // 5. Beds
        runCatching {
            val dtos = supabaseClient.postgrest.from("beds").select().decodeList<BedDto>()
            val entities = dtos.map { BedEntity(it.id, it.roomId, it.bedNumber, it.status) }
            db.hostelDao().insertBeds(entities)
        }

        // 6. Profiles
        runCatching {
            val dtos = supabaseClient.postgrest.from("profiles").select().decodeList<ProfileDto>()
            val entities = dtos.map { ProfileEntity(it.id, it.organizationId, it.role, it.fullName, it.email, it.phone, it.avatarUrl) }
            db.studentDao().insertProfiles(entities)
        }

        // 7. Students
        runCatching {
            val dtos = supabaseClient.postgrest.from("students").select().decodeList<StudentDto>()
            val entities = dtos.map {
                StudentEntity(
                    id = it.id,
                    studentIdNumber = it.studentIdNumber,
                    dob = it.dob,
                    gender = it.gender,
                    course = it.course,
                    department = it.department,
                    academicYear = it.academicYear,
                    parentName = it.parentName,
                    parentPhone = it.parentPhone,
                    emergencyContact = it.emergencyContact,
                    address = it.address,
                    admissionDate = it.admissionDate,
                    hostelStatus = it.hostelStatus,
                    joiningDate = it.joiningDate,
                    leavingDate = it.leavingDate
                )
            }
            db.studentDao().insertStudents(entities)
        }

        // 8. Room Allocations
        runCatching {
            val dtos = supabaseClient.postgrest.from("room_allocations").select().decodeList<RoomAllocationDto>()
            val entities = dtos.map {
                RoomAllocationEntity(it.id, it.studentId, it.bedId, it.allocatedAt, it.vacatedAt, it.status, it.notes, it.recordedBy)
            }
            db.allocationDao().insertAllocations(entities)
        }

        // 9. Fee Invoices
        runCatching {
            val dtos = supabaseClient.postgrest.from("fee_invoices").select().decodeList<FeeInvoiceDto>()
            val entities = dtos.map {
                FeeInvoiceEntity(it.id, it.studentId, it.billingMonth, it.amount, it.dueDate, it.discount, it.lateFee, it.amountPaid, it.balance, it.paymentStatus, it.paymentDate)
            }
            db.financeDao().insertInvoices(entities)
        }

        // 10. Payments
        runCatching {
            val dtos = supabaseClient.postgrest.from("payments").select().decodeList<PaymentDto>()
            val entities = dtos.map {
                PaymentEntity(it.id, it.studentId, it.feeInvoiceId, it.amount, it.paymentDate, it.paymentMethod, it.transactionId, it.recordedBy, it.notes)
            }
            db.financeDao().insertPayments(entities)
        }

        // 11. Complaints
        runCatching {
            val dtos = supabaseClient.postgrest.from("complaints").select().decodeList<ComplaintDto>()
            val entities = dtos.map {
                ComplaintEntity(it.id, it.studentId, it.category, it.title, it.description, it.priority, it.imageUrl, it.status, it.createdAt, it.updatedAt)
            }
            db.complaintDao().insertComplaints(entities)
        }

        // 12. Complaint Comments
        runCatching {
            val dtos = supabaseClient.postgrest.from("complaint_comments").select().decodeList<ComplaintCommentDto>()
            val entities = dtos.map {
                ComplaintCommentEntity(it.id, it.complaintId, it.userId, it.comment, it.imageUrl, it.createdAt)
            }
            db.complaintDao().insertComments(entities)
        }

        // 13. Announcements
        runCatching {
            val dtos = supabaseClient.postgrest.from("announcements").select().decodeList<AnnouncementDto>()
            val entities = dtos.map {
                AnnouncementEntity(it.id, it.organizationId, it.title, it.message, it.targetAudience, it.targetHostelId, it.targetBuildingId, it.publishDate, it.expiryDate, it.priority, it.attachmentUrl, it.createdAt)
            }
            db.announcementDao().insertAnnouncements(entities)
        }
    }

    private fun getTableNameForEntity(entityType: String): String {
        return when (entityType.uppercase()) {
            "HOSTEL" -> "hostels"
            "BUILDING" -> "buildings"
            "FLOOR" -> "floors"
            "ROOM" -> "rooms"
            "BED" -> "beds"
            "PROFILE" -> "profiles"
            "STUDENT" -> "students"
            "ALLOCATION" -> "room_allocations"
            "FEE_INVOICE" -> "fee_invoices"
            "PAYMENT" -> "payments"
            "COMPLAINT" -> "complaints"
            "COMPLAINT_COMMENT" -> "complaint_comments"
            "ANNOUNCEMENT" -> "announcements"
            else -> entityType.lowercase()
        }
    }
}
