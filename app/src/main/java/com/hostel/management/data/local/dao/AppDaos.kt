package com.hostel.management.data.local.dao

import androidx.room.*
import com.hostel.management.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HostelDao {
    @Query("SELECT * FROM hostels")
    suspend fun getAllHostels(): List<HostelEntity>

    @Query("SELECT * FROM hostels")
    fun observeAllHostels(): Flow<List<HostelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHostels(hostels: List<HostelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHostel(hostel: HostelEntity)

    // Buildings
    @Query("SELECT * FROM buildings WHERE hostelId = :hostelId")
    suspend fun getBuildingsForHostel(hostelId: String): List<BuildingEntity>

    @Query("SELECT * FROM buildings")
    suspend fun getAllBuildings(): List<BuildingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuildings(buildings: List<BuildingEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuilding(building: BuildingEntity)

    // Floors
    @Query("SELECT * FROM floors WHERE buildingId = :buildingId")
    suspend fun getFloorsForBuilding(buildingId: String): List<FloorEntity>

    @Query("SELECT * FROM floors")
    suspend fun getAllFloors(): List<FloorEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFloors(floors: List<FloorEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFloor(floor: FloorEntity)

    @Query("DELETE FROM floors WHERE id = :floorId")
    suspend fun deleteFloor(floorId: String)

    // Rooms
    @Query("SELECT * FROM rooms WHERE floorId = :floorId")
    suspend fun getRoomsForFloor(floorId: String): List<RoomEntity>

    @Query("SELECT * FROM rooms")
    suspend fun getAllRooms(): List<RoomEntity>

    @Query("SELECT * FROM rooms WHERE id = :roomId")
    suspend fun getRoomById(roomId: String): RoomEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRooms(rooms: List<RoomEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoom(room: RoomEntity)

    @Query("DELETE FROM rooms WHERE id = :roomId")
    suspend fun deleteRoom(roomId: String)

    // Beds
    @Query("SELECT * FROM beds WHERE roomId = :roomId")
    suspend fun getBedsForRoom(roomId: String): List<BedEntity>

    @Query("SELECT * FROM beds")
    suspend fun getAllBeds(): List<BedEntity>

    @Query("SELECT * FROM beds WHERE id = :bedId")
    suspend fun getBedById(bedId: String): BedEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeds(beds: List<BedEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBed(bed: BedEntity)

    @Query("DELETE FROM beds WHERE id = :bedId")
    suspend fun deleteBed(bedId: String)

    @Query("UPDATE beds SET status = :status WHERE id = :bedId")
    suspend fun updateBedStatus(bedId: String, status: String)

    @Query("UPDATE rooms SET status = :status WHERE id = :roomId")
    suspend fun updateRoomStatus(roomId: String, status: String)
}

@Dao
interface StudentDao {
    @Query("SELECT * FROM profiles WHERE role = 'STUDENT'")
    suspend fun getAllStudentProfiles(): List<ProfileEntity>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfileById(id: String): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<ProfileEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    @Query("SELECT * FROM students")
    suspend fun getAllStudents(): List<StudentEntity>

    @Query("SELECT * FROM students WHERE id = :id")
    suspend fun getStudentById(id: String): StudentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<StudentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity)

    @Query("UPDATE students SET hostelStatus = :status, leavingDate = :leavingDate WHERE id = :studentId")
    suspend fun vacateStudent(studentId: String, status: String = "Vacated", leavingDate: String)
}

@Dao
interface AllocationDao {
    @Query("SELECT * FROM room_allocations")
    suspend fun getAllAllocations(): List<RoomAllocationEntity>

    @Query("SELECT * FROM room_allocations WHERE id = :id")
    suspend fun getAllocationById(id: String): RoomAllocationEntity?

    @Query("SELECT * FROM room_allocations WHERE studentId = :studentId AND status = 'Active' LIMIT 1")
    suspend fun getActiveAllocationForStudent(studentId: String): RoomAllocationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllocations(allocations: List<RoomAllocationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllocation(allocation: RoomAllocationEntity)

    @Query("UPDATE room_allocations SET status = :status, vacatedAt = :vacatedAt WHERE id = :id")
    suspend fun updateAllocationStatus(id: String, status: String, vacatedAt: String? = null)
}

@Dao
interface FinanceDao {
    @Query("SELECT * FROM fee_invoices")
    suspend fun getAllInvoices(): List<FeeInvoiceEntity>

    @Query("SELECT * FROM fee_invoices WHERE studentId = :studentId")
    suspend fun getInvoicesForStudent(studentId: String): List<FeeInvoiceEntity>

    @Query("SELECT * FROM fee_invoices WHERE id = :id")
    suspend fun getInvoiceById(id: String): FeeInvoiceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoices(invoices: List<FeeInvoiceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: FeeInvoiceEntity)

    @Query("SELECT * FROM payments")
    suspend fun getAllPayments(): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE studentId = :studentId")
    suspend fun getPaymentsForStudent(studentId: String): List<PaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<PaymentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)
}

@Dao
interface ComplaintDao {
    @Query("SELECT * FROM complaints ORDER BY createdAt DESC")
    suspend fun getAllComplaints(): List<ComplaintEntity>

    @Query("SELECT * FROM complaints WHERE studentId = :studentId ORDER BY createdAt DESC")
    suspend fun getComplaintsForStudent(studentId: String): List<ComplaintEntity>

    @Query("SELECT * FROM complaints WHERE id = :id")
    suspend fun getComplaintById(id: String): ComplaintEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComplaints(complaints: List<ComplaintEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComplaint(complaint: ComplaintEntity)

    @Query("UPDATE complaints SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateComplaintStatus(id: String, status: String, updatedAt: String)

    // Comments
    @Query("SELECT * FROM complaint_comments WHERE complaintId = :complaintId ORDER BY createdAt ASC")
    suspend fun getCommentsForComplaint(complaintId: String): List<ComplaintCommentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComments(comments: List<ComplaintCommentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: ComplaintCommentEntity)

    @Query("SELECT * FROM profiles WHERE role = 'MAINTENANCE_STAFF'")
    suspend fun getMaintenanceStaff(): List<ProfileEntity>
}

@Dao
interface AnnouncementDao {
    @Query("SELECT * FROM announcements ORDER BY createdAt DESC")
    suspend fun getAllAnnouncements(): List<AnnouncementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncements(announcements: List<AnnouncementEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncement(announcement: AnnouncementEntity)
}

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue ORDER BY id ASC")
    suspend fun getAllPendingSyncItems(): List<SyncQueueEntity>

    @Query("SELECT COUNT(*) FROM sync_queue")
    suspend fun getPendingCount(): Int

    @Query("SELECT COUNT(*) FROM sync_queue")
    fun observePendingCount(): Flow<Int>

    @Insert
    suspend fun enqueueSyncItem(item: SyncQueueEntity)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteSyncItem(id: Int)

    @Query("DELETE FROM sync_queue")
    suspend fun clearSyncQueue()
}
