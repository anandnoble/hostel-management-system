package com.hostel.management.data.repository

import com.hostel.management.data.dto.*
import com.hostel.management.data.local.AppDatabase
import com.hostel.management.data.local.entity.*
import com.hostel.management.data.sync.SyncManager
import com.hostel.management.domain.model.*
import com.hostel.management.domain.repository.HostelRepository
import com.hostel.management.di.ServiceLocator
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class HostelRepositoryImpl(
    private val supabaseClient: SupabaseClient,
    private val db: AppDatabase,
    private val syncManager: SyncManager
) : HostelRepository {

    override suspend fun getHostels(): Result<List<Hostel>> = runCatching {
        var local = db.hostelDao().getAllHostels()
        if (local.isEmpty()) {
            // Attempt initial pull from cloud if local DB is empty
            runCatching { syncManager.pullCloudToLocal() }
            local = db.hostelDao().getAllHostels()
        }
        if (local.isEmpty()) {
            // Pre-seed default offline hostel if offline on first launch
            val defaultHostel = HostelEntity("hostel_default", "org_default", "Main Campus Hostel", "Campus Road")
            db.hostelDao().insertHostel(defaultHostel)
            local = listOf(defaultHostel)
        }
        local.map { Hostel(it.id, it.organizationId, it.name, it.address, it.upiId, it.monthlyFee, it.advanceDeposit) }
    }

    override suspend fun createHostel(name: String, address: String?): Result<Hostel> = runCatching {
        val profile = ServiceLocator.authRepository.getCurrentProfile().getOrNull()
        val orgId = profile?.organizationId ?: "org_default"
        val newId = UUID.randomUUID().toString()

        val entity = HostelEntity(
            id = newId,
            organizationId = orgId,
            name = name,
            address = address
        )

        // 1. Save to local Room DB immediately
        db.hostelDao().insertHostel(entity)

        // 2. Queue for cloud sync
        val payload = buildJsonObject {
            put("id", newId)
            put("organization_id", orgId)
            put("name", name)
            if (address != null) put("address", address)
        }.toString()

        db.syncQueueDao().enqueueSyncItem(
            SyncQueueEntity(
                entityType = "HOSTEL",
                entityId = newId,
                action = "INSERT",
                payloadJson = payload
            )
        )

        // 3. Trigger auto sync if online
        syncManager.triggerAutoSyncIfEnabled()

        Hostel(newId, orgId, name, address)
    }

    override suspend fun getBuildings(hostelId: String): Result<List<Building>> = runCatching {
        var local = db.hostelDao().getBuildingsForHostel(hostelId)
        if (local.isEmpty()) {
            runCatching { syncManager.pullCloudToLocal() }
            local = db.hostelDao().getBuildingsForHostel(hostelId)
        }
        local.map { Building(it.id, it.hostelId, it.name) }
    }

    override suspend fun createBuilding(hostelId: String, name: String): Result<Building> = runCatching {
        val newId = UUID.randomUUID().toString()
        val entity = BuildingEntity(id = newId, hostelId = hostelId, name = name)

        db.hostelDao().insertBuilding(entity)

        val payload = buildJsonObject {
            put("id", newId)
            put("hostel_id", hostelId)
            put("name", name)
        }.toString()

        db.syncQueueDao().enqueueSyncItem(
            SyncQueueEntity(
                entityType = "BUILDING",
                entityId = newId,
                action = "INSERT",
                payloadJson = payload
            )
        )

        syncManager.triggerAutoSyncIfEnabled()

        Building(newId, hostelId, name)
    }

    override suspend fun getFloors(buildingId: String): Result<List<Floor>> = runCatching {
        var local = db.hostelDao().getFloorsForBuilding(buildingId)
        if (local.isEmpty()) {
            runCatching { syncManager.pullCloudToLocal() }
            local = db.hostelDao().getFloorsForBuilding(buildingId)
        }
        local.map { Floor(it.id, it.buildingId, it.floorNumber) }
    }

    override suspend fun createFloor(buildingId: String, floorNumber: Int): Result<Floor> = runCatching {
        val newId = UUID.randomUUID().toString()
        val entity = FloorEntity(id = newId, buildingId = buildingId, floorNumber = floorNumber)

        db.hostelDao().insertFloor(entity)

        val payload = buildJsonObject {
            put("id", newId)
            put("building_id", buildingId)
            put("floor_number", floorNumber)
        }.toString()

        db.syncQueueDao().enqueueSyncItem(
            SyncQueueEntity(
                entityType = "FLOOR",
                entityId = newId,
                action = "INSERT",
                payloadJson = payload
            )
        )

        syncManager.triggerAutoSyncIfEnabled()

        Floor(newId, buildingId, floorNumber)
    }

    override suspend fun getRooms(floorId: String): Result<List<Room>> = runCatching {
        var localRooms = db.hostelDao().getRoomsForFloor(floorId)
        if (localRooms.isEmpty()) {
            runCatching { syncManager.pullCloudToLocal() }
            localRooms = db.hostelDao().getRoomsForFloor(floorId)
        }

        localRooms.map { dto ->
            val beds = db.hostelDao().getBedsForRoom(dto.id)
            val occupied = beds.count { it.status == "Occupied" }
            Room(
                id = dto.id,
                floorId = dto.floorId,
                roomNumber = dto.roomNumber,
                capacity = dto.capacity,
                roomType = dto.roomType,
                status = dto.status,
                notes = dto.notes,
                bedsCount = beds.size,
                occupiedBedsCount = occupied,
                beds = beds.map { Bed(it.id, it.roomId, it.bedNumber, it.status) }
            )
        }
    }

    override suspend fun createRoom(
        floorId: String,
        roomNumber: String,
        capacity: Int,
        roomType: String
    ): Result<Room> = runCatching {
        val newRoomId = UUID.randomUUID().toString()
        val roomEntity = RoomEntity(
            id = newRoomId,
            floorId = floorId,
            roomNumber = roomNumber,
            capacity = capacity,
            roomType = roomType,
            status = "Available"
        )

        db.hostelDao().insertRoom(roomEntity)

        val roomPayload = buildJsonObject {
            put("id", newRoomId)
            put("floor_id", floorId)
            put("room_number", roomNumber)
            put("capacity", capacity)
            put("room_type", roomType)
            put("status", "Available")
        }.toString()

        db.syncQueueDao().enqueueSyncItem(
            SyncQueueEntity(
                entityType = "ROOM",
                entityId = newRoomId,
                action = "INSERT",
                payloadJson = roomPayload
            )
        )

        // Pre-create beds for the room locally & enqueue sync
        val createdBeds = mutableListOf<BedEntity>()
        for (i in 1..capacity) {
            val bedId = UUID.randomUUID().toString()
            val bedLabel = "Bed " + ('A'.code + i - 1).toChar()
            val bedEntity = BedEntity(id = bedId, roomId = newRoomId, bedNumber = bedLabel, status = "Available")
            createdBeds.add(bedEntity)

            val bedPayload = buildJsonObject {
                put("id", bedId)
                put("room_id", newRoomId)
                put("bed_number", bedLabel)
                put("status", "Available")
            }.toString()

            db.syncQueueDao().enqueueSyncItem(
                SyncQueueEntity(
                    entityType = "BED",
                    entityId = bedId,
                    action = "INSERT",
                    payloadJson = bedPayload
                )
            )
        }
        db.hostelDao().insertBeds(createdBeds)

        syncManager.triggerAutoSyncIfEnabled()

        Room(
            id = newRoomId,
            floorId = floorId,
            roomNumber = roomNumber,
            capacity = capacity,
            roomType = roomType,
            status = "Available",
            notes = null,
            bedsCount = capacity,
            occupiedBedsCount = 0
        )
    }

    override suspend fun getBeds(roomId: String): Result<List<Bed>> = runCatching {
        var local = db.hostelDao().getBedsForRoom(roomId)
        if (local.isEmpty()) {
            runCatching { syncManager.pullCloudToLocal() }
            local = db.hostelDao().getBedsForRoom(roomId)
        }
        local.map { Bed(it.id, it.roomId, it.bedNumber, it.status) }
    }

    override suspend fun createBed(roomId: String, bedNumber: String): Result<Bed> = runCatching {
        val newId = UUID.randomUUID().toString()
        val entity = BedEntity(id = newId, roomId = roomId, bedNumber = bedNumber, status = "Available")

        db.hostelDao().insertBed(entity)

        val payload = buildJsonObject {
            put("id", newId)
            put("room_id", roomId)
            put("bed_number", bedNumber)
            put("status", "Available")
        }.toString()

        db.syncQueueDao().enqueueSyncItem(
            SyncQueueEntity(
                entityType = "BED",
                entityId = newId,
                action = "INSERT",
                payloadJson = payload
            )
        )

        syncManager.triggerAutoSyncIfEnabled()

        Bed(newId, roomId, bedNumber, "Available")
    }

    override suspend fun deleteFloor(floorId: String): Result<Unit> = runCatching {
        db.hostelDao().deleteFloor(floorId)
        db.syncQueueDao().enqueueSyncItem(
            SyncQueueEntity(
                entityType = "FLOOR",
                entityId = floorId,
                action = "DELETE",
                payloadJson = "{}"
            )
        )
        syncManager.triggerAutoSyncIfEnabled()
    }

    override suspend fun deleteRoom(roomId: String): Result<Unit> = runCatching {
        db.hostelDao().deleteRoom(roomId)
        db.syncQueueDao().enqueueSyncItem(
            SyncQueueEntity(
                entityType = "ROOM",
                entityId = roomId,
                action = "DELETE",
                payloadJson = "{}"
            )
        )
        syncManager.triggerAutoSyncIfEnabled()
    }

    override suspend fun deleteBed(bedId: String): Result<Unit> = runCatching {
        db.hostelDao().deleteBed(bedId)
        db.syncQueueDao().enqueueSyncItem(
            SyncQueueEntity(
                entityType = "BED",
                entityId = bedId,
                action = "DELETE",
                payloadJson = "{}"
            )
        )
        syncManager.triggerAutoSyncIfEnabled()
    }

    override suspend fun getAllocations(): Result<List<RoomAllocation>> = runCatching {
        var allocs = db.allocationDao().getAllAllocations()
        if (allocs.isEmpty()) {
            runCatching { syncManager.pullCloudToLocal() }
            allocs = db.allocationDao().getAllAllocations()
        }

        val profiles = db.studentDao().getAllStudentProfiles().associateBy { it.id }
        val students = db.studentDao().getAllStudents().associateBy { it.id }
        val beds = db.hostelDao().getAllBeds().associateBy { it.id }
        val rooms = db.hostelDao().getAllRooms().associateBy { it.id }
        val floors = db.hostelDao().getAllFloors().associateBy { it.id }
        val buildings = db.hostelDao().getAllBuildings().associateBy { it.id }
        val hostels = db.hostelDao().getAllHostels().associateBy { it.id }

        allocs.map { dto ->
            val student = students[dto.studentId]
            val profile = profiles[dto.studentId]
            val bed = beds[dto.bedId]
            val room = bed?.let { rooms[it.roomId] }
            val floor = room?.let { floors[it.floorId] }
            val building = floor?.let { buildings[it.buildingId] }
            val hostel = building?.let { hostels[it.hostelId] }

            RoomAllocation(
                id = dto.id,
                studentId = dto.studentId,
                bedId = dto.bedId,
                allocatedAt = dto.allocatedAt,
                vacatedAt = dto.vacatedAt,
                status = dto.status,
                notes = dto.notes,
                recordedBy = dto.recordedBy,
                studentName = profile?.fullName,
                studentRollNumber = student?.studentIdNumber,
                hostelName = hostel?.name,
                buildingName = building?.name,
                floorNumber = floor?.floorNumber,
                roomNumber = room?.roomNumber,
                bedNumber = bed?.bedNumber
            )
        }
    }

    override suspend fun allocateRoom(
        studentId: String,
        bedId: String,
        notes: String?
    ): Result<Unit> = runCatching {
        val recorder = ServiceLocator.authRepository.getCurrentProfile().getOrNull()
        val newAllocId = UUID.randomUUID().toString()
        val nowString = SimpleDateFormat("yyyy-MM-dd HH:mm:ssZZZZZ", Locale.US).format(Date())

        val allocEntity = RoomAllocationEntity(
            id = newAllocId,
            studentId = studentId,
            bedId = bedId,
            allocatedAt = nowString,
            status = "Active",
            notes = notes,
            recordedBy = recorder?.id
        )

        // 1. Insert allocation in Room DB
        db.allocationDao().insertAllocation(allocEntity)

        // 2. Mark bed as Occupied in Room DB
        db.hostelDao().updateBedStatus(bedId, "Occupied")

        // 3. Update room status in Room DB
        updateRoomStatusFromBeds(bedId)

        // 4. Enqueue Sync Items
        val allocPayload = buildJsonObject {
            put("id", newAllocId)
            put("student_id", studentId)
            put("bed_id", bedId)
            put("status", "Active")
            if (notes != null) put("notes", notes)
            if (recorder?.id != null) put("recorded_by", recorder.id)
        }.toString()

        db.syncQueueDao().enqueueSyncItem(
            SyncQueueEntity(
                entityType = "ALLOCATION",
                entityId = newAllocId,
                action = "INSERT",
                payloadJson = allocPayload
            )
        )

        val bedPayload = buildJsonObject {
            put("status", "Occupied")
        }.toString()

        db.syncQueueDao().enqueueSyncItem(
            SyncQueueEntity(
                entityType = "BED",
                entityId = bedId,
                action = "UPDATE",
                payloadJson = bedPayload
            )
        )

        syncManager.triggerAutoSyncIfEnabled()
    }

    override suspend fun transferRoom(
        allocationId: String,
        newBedId: String,
        notes: String?
    ): Result<Unit> = runCatching {
        val recorder = ServiceLocator.authRepository.getCurrentProfile().getOrNull()
        val oldAlloc = db.allocationDao().getAllocationById(allocationId)
            ?: throw IllegalStateException("Allocation not found")

        val oldBedId = oldAlloc.bedId
        val studentId = oldAlloc.studentId
        val nowString = SimpleDateFormat("yyyy-MM-dd HH:mm:ssZZZZZ", Locale.US).format(Date())

        // 1. Close old allocation locally
        db.allocationDao().updateAllocationStatus(allocationId, "Transferred", nowString)

        // 2. Mark old bed available locally
        db.hostelDao().updateBedStatus(oldBedId, "Available")

        // 3. Create new allocation locally
        val newAllocId = UUID.randomUUID().toString()
        val newAllocEntity = RoomAllocationEntity(
            id = newAllocId,
            studentId = studentId,
            bedId = newBedId,
            allocatedAt = nowString,
            status = "Active",
            notes = "Transferred. Notes: ${notes ?: ""}",
            recordedBy = recorder?.id
        )
        db.allocationDao().insertAllocation(newAllocEntity)

        // 4. Mark new bed occupied locally
        db.hostelDao().updateBedStatus(newBedId, "Occupied")

        // 5. Update room statuses locally
        updateRoomStatusFromBeds(oldBedId)
        updateRoomStatusFromBeds(newBedId)

        // Queue Sync Queue items
        val oldAllocPayload = buildJsonObject {
            put("status", "Transferred")
            put("vacated_at", nowString)
        }.toString()
        db.syncQueueDao().enqueueSyncItem(SyncQueueEntity(entityType = "ALLOCATION", entityId = allocationId, action = "UPDATE", payloadJson = oldAllocPayload))

        val oldBedPayload = buildJsonObject { put("status", "Available") }.toString()
        db.syncQueueDao().enqueueSyncItem(SyncQueueEntity(entityType = "BED", entityId = oldBedId, action = "UPDATE", payloadJson = oldBedPayload))

        val newAllocPayload = buildJsonObject {
            put("id", newAllocId)
            put("student_id", studentId)
            put("bed_id", newBedId)
            put("status", "Active")
            put("notes", "Transferred. Notes: ${notes ?: ""}")
            if (recorder?.id != null) put("recorded_by", recorder.id)
        }.toString()
        db.syncQueueDao().enqueueSyncItem(SyncQueueEntity(entityType = "ALLOCATION", entityId = newAllocId, action = "INSERT", payloadJson = newAllocPayload))

        val newBedPayload = buildJsonObject { put("status", "Occupied") }.toString()
        db.syncQueueDao().enqueueSyncItem(SyncQueueEntity(entityType = "BED", entityId = newBedId, action = "UPDATE", payloadJson = newBedPayload))

        syncManager.triggerAutoSyncIfEnabled()
    }

    override suspend fun vacateRoom(allocationId: String): Result<Unit> = runCatching {
        val oldAlloc = db.allocationDao().getAllocationById(allocationId)
            ?: throw IllegalStateException("Allocation not found")

        val oldBedId = oldAlloc.bedId
        val nowString = SimpleDateFormat("yyyy-MM-dd HH:mm:ssZZZZZ", Locale.US).format(Date())

        db.allocationDao().updateAllocationStatus(allocationId, "Vacated", nowString)
        db.hostelDao().updateBedStatus(oldBedId, "Available")
        updateRoomStatusFromBeds(oldBedId)

        val allocPayload = buildJsonObject {
            put("status", "Vacated")
            put("vacated_at", nowString)
        }.toString()
        db.syncQueueDao().enqueueSyncItem(SyncQueueEntity(entityType = "ALLOCATION", entityId = allocationId, action = "UPDATE", payloadJson = allocPayload))

        val bedPayload = buildJsonObject { put("status", "Available") }.toString()
        db.syncQueueDao().enqueueSyncItem(SyncQueueEntity(entityType = "BED", entityId = oldBedId, action = "UPDATE", payloadJson = bedPayload))

        syncManager.triggerAutoSyncIfEnabled()
    }

    private suspend fun updateRoomStatusFromBeds(bedId: String) {
        val bed = db.hostelDao().getBedById(bedId) ?: return
        val roomId = bed.roomId
        val roomBeds = db.hostelDao().getBedsForRoom(roomId)

        val totalBeds = roomBeds.size
        val occupiedBeds = roomBeds.count { it.status == "Occupied" }

        val newStatus = when {
            occupiedBeds == 0 -> "Available"
            occupiedBeds == totalBeds -> "Full"
            else -> "Partially occupied"
        }

        db.hostelDao().updateRoomStatus(roomId, newStatus)
        val roomPayload = buildJsonObject { put("status", newStatus) }.toString()
        db.syncQueueDao().enqueueSyncItem(SyncQueueEntity(entityType = "ROOM", entityId = roomId, action = "UPDATE", payloadJson = roomPayload))
    }

    override suspend fun getStudents(
        searchQuery: String?,
        statusFilter: String?
    ): Result<List<Student>> = runCatching {
        var profilesList = db.studentDao().getAllStudentProfiles()
        var studentDtos = db.studentDao().getAllStudents().associateBy { it.id }

        if (profilesList.isEmpty()) {
            runCatching { syncManager.pullCloudToLocal() }
            profilesList = db.studentDao().getAllStudentProfiles()
            studentDtos = db.studentDao().getAllStudents().associateBy { it.id }
        }

        if (!searchQuery.isNullOrBlank()) {
            profilesList = profilesList.filter {
                it.fullName.contains(searchQuery, ignoreCase = true) || it.email.contains(searchQuery, ignoreCase = true)
            }
        }

        profilesList.mapNotNull { pDto ->
            val sDto = studentDtos[pDto.id] ?: return@mapNotNull null
            if (statusFilter != null && sDto.hostelStatus != statusFilter) {
                return@mapNotNull null
            }

            val profile = Profile(
                id = pDto.id,
                organizationId = pDto.organizationId,
                role = UserRole.STUDENT,
                fullName = pDto.fullName,
                email = pDto.email,
                phone = pDto.phone,
                avatarUrl = pDto.avatarUrl
            )

            Student(
                id = sDto.id,
                profile = profile,
                studentIdNumber = sDto.studentIdNumber,
                dob = sDto.dob,
                gender = sDto.gender,
                course = sDto.course,
                department = sDto.department,
                academicYear = sDto.academicYear,
                parentName = sDto.parentName,
                parentPhone = sDto.parentPhone,
                emergencyContact = sDto.emergencyContact,
                address = sDto.address,
                admissionDate = sDto.admissionDate,
                hostelStatus = sDto.hostelStatus,
                joiningDate = sDto.joiningDate,
                leavingDate = sDto.leavingDate
            )
        }
    }

    override suspend fun getStudentDetails(studentId: String): Result<Student> = runCatching {
        val pDto = db.studentDao().getProfileById(studentId)
            ?: throw IllegalStateException("Profile not found locally")
        val sDto = db.studentDao().getStudentById(studentId)
            ?: throw IllegalStateException("Student not found locally")

        val profile = Profile(
            id = pDto.id,
            organizationId = pDto.organizationId,
            role = UserRole.STUDENT,
            fullName = pDto.fullName,
            email = pDto.email,
            phone = pDto.phone,
            avatarUrl = pDto.avatarUrl
        )

        Student(
            id = sDto.id,
            profile = profile,
            studentIdNumber = sDto.studentIdNumber,
            dob = sDto.dob,
            gender = sDto.gender,
            course = sDto.course,
            department = sDto.department,
            academicYear = sDto.academicYear,
            parentName = sDto.parentName,
            parentPhone = sDto.parentPhone,
            emergencyContact = sDto.emergencyContact,
            address = sDto.address,
            admissionDate = sDto.admissionDate,
            hostelStatus = sDto.hostelStatus,
            joiningDate = sDto.joiningDate,
            leavingDate = sDto.leavingDate
        )
    }

    override suspend fun addStudent(
        student: Student,
        email: String,
        password: String
    ): Result<Unit> = runCatching {
        val adminProfile = ServiceLocator.authRepository.getCurrentProfile().getOrNull()
        val orgId = adminProfile?.organizationId ?: "org_default"

        var newUserId = UUID.randomUUID().toString()

        // Try Supabase auth signup if online
        val remoteSignUp = runCatching {
            val newUser = supabaseClient.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                data = buildJsonObject {
                    put("full_name", student.profile.fullName)
                    put("role", "STUDENT")
                    put("organization_id", orgId)
                }
            }
            newUser?.id
        }

        if (remoteSignUp.isSuccess && remoteSignUp.getOrNull() != null) {
            newUserId = remoteSignUp.getOrNull()!!
        }

        val profileEntity = ProfileEntity(
            id = newUserId,
            organizationId = orgId,
            role = "STUDENT",
            fullName = student.profile.fullName,
            email = email,
            phone = student.profile.phone,
            avatarUrl = student.profile.avatarUrl
        )

        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val studentEntity = StudentEntity(
            id = newUserId,
            studentIdNumber = student.studentIdNumber,
            dob = student.dob,
            gender = student.gender,
            course = student.course,
            department = student.department,
            academicYear = student.academicYear,
            parentName = student.parentName,
            parentPhone = student.parentPhone,
            emergencyContact = student.emergencyContact,
            address = student.address,
            admissionDate = todayDate,
            hostelStatus = "Active"
        )

        db.studentDao().insertProfile(profileEntity)
        db.studentDao().insertStudent(studentEntity)

        val studentPayload = buildJsonObject {
            put("id", newUserId)
            put("student_id_number", student.studentIdNumber)
            if (student.dob != null) put("dob", student.dob)
            if (student.gender != null) put("gender", student.gender)
            if (student.course != null) put("course", student.course)
            if (student.department != null) put("department", student.department)
            if (student.academicYear != null) put("academic_year", student.academicYear)
            if (student.parentName != null) put("parent_name", student.parentName)
            if (student.parentPhone != null) put("parent_phone", student.parentPhone)
            if (student.emergencyContact != null) put("emergency_contact", student.emergencyContact)
            if (student.address != null) put("address", student.address)
            put("hostel_status", "Active")
        }.toString()

        db.syncQueueDao().enqueueSyncItem(
            SyncQueueEntity(
                entityType = "STUDENT",
                entityId = newUserId,
                action = "INSERT",
                payloadJson = studentPayload
            )
        )

        syncManager.triggerAutoSyncIfEnabled()
    }

    override suspend fun updateStudent(student: Student): Result<Unit> = runCatching {
        val pDto = db.studentDao().getProfileById(student.id)
        if (pDto != null) {
            val updatedProfile = pDto.copy(
                fullName = student.profile.fullName,
                phone = student.profile.phone
            )
            db.studentDao().insertProfile(updatedProfile)
        }

        val sDto = db.studentDao().getStudentById(student.id)
        if (sDto != null) {
            val updatedStudent = sDto.copy(
                dob = student.dob ?: sDto.dob,
                gender = student.gender ?: sDto.gender,
                course = student.course ?: sDto.course,
                department = student.department ?: sDto.department,
                academicYear = student.academicYear ?: sDto.academicYear,
                parentName = student.parentName ?: sDto.parentName,
                parentPhone = student.parentPhone ?: sDto.parentPhone,
                emergencyContact = student.emergencyContact ?: sDto.emergencyContact,
                address = student.address ?: sDto.address,
                hostelStatus = student.hostelStatus
            )
            db.studentDao().insertStudent(updatedStudent)
        }

        val studentPayload = buildJsonObject {
            if (student.dob != null) put("dob", student.dob)
            if (student.gender != null) put("gender", student.gender)
            if (student.course != null) put("course", student.course)
            if (student.department != null) put("department", student.department)
            if (student.academicYear != null) put("academic_year", student.academicYear)
            if (student.parentName != null) put("parent_name", student.parentName)
            if (student.parentPhone != null) put("parent_phone", student.parentPhone)
            if (student.emergencyContact != null) put("emergency_contact", student.emergencyContact)
            if (student.address != null) put("address", student.address)
            put("hostel_status", student.hostelStatus)
        }.toString()

        db.syncQueueDao().enqueueSyncItem(
            SyncQueueEntity(
                entityType = "STUDENT",
                entityId = student.id,
                action = "UPDATE",
                payloadJson = studentPayload
            )
        )

        syncManager.triggerAutoSyncIfEnabled()
    }

    override suspend fun vacateStudent(studentId: String): Result<Unit> = runCatching {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        db.studentDao().vacateStudent(studentId, "Vacated", todayStr)

        val activeAlloc = db.allocationDao().getActiveAllocationForStudent(studentId)
        if (activeAlloc != null) {
            vacateRoom(activeAlloc.id)
        }

        val studentPayload = buildJsonObject {
            put("hostel_status", "Vacated")
            put("leaving_date", todayStr)
        }.toString()

        db.syncQueueDao().enqueueSyncItem(
            SyncQueueEntity(
                entityType = "STUDENT",
                entityId = studentId,
                action = "UPDATE",
                payloadJson = studentPayload
            )
        )

        syncManager.triggerAutoSyncIfEnabled()
    }

    override suspend fun getDashboardStats(): Result<Map<String, Any>> = runCatching {
        val hostels = getHostels().getOrThrow()
        val rooms = db.hostelDao().getAllRooms()
        val beds = db.hostelDao().getAllBeds()
        val activeStudents = db.studentDao().getAllStudents().filter { it.hostelStatus == "Active" }

        val totalStudents = activeStudents.size
        val totalRooms = rooms.size
        val totalBeds = beds.size
        val occupiedBeds = beds.count { it.status == "Occupied" }
        val availableBeds = totalBeds - occupiedBeds
        val occupancyPct = if (totalBeds > 0) (occupiedBeds.toDouble() / totalBeds * 100).toInt() else 0

        val complaints = db.complaintDao().getAllComplaints()
        val pendingComplaints = complaints.count { it.status != "Resolved" && it.status != "Closed" }
        val emergencyComplaints = complaints.count { it.priority == "Emergency" && it.status != "Resolved" && it.status != "Closed" }

        val invoices = db.financeDao().getAllInvoices()
        val pendingFees = invoices.filter { it.paymentStatus != "Paid" && it.paymentStatus != "Waived" }.sumOf { it.balance }

        mapOf(
            "totalHostels" to hostels.size,
            "totalStudents" to totalStudents,
            "totalRooms" to totalRooms,
            "totalBeds" to totalBeds,
            "occupiedBeds" to occupiedBeds,
            "availableBeds" to availableBeds,
            "occupancyPct" to occupancyPct,
            "pendingComplaints" to pendingComplaints,
            "emergencyComplaints" to emergencyComplaints,
            "pendingFees" to pendingFees
        )
    }

    override suspend fun updateHostelPaymentConfig(
        hostelId: String,
        upiId: String,
        monthlyFee: Double,
        advanceDeposit: Double
    ): Result<Unit> = runCatching {
        val profile = ServiceLocator.authRepository.getCurrentProfile().getOrNull()
        val rawOrgId = profile?.organizationId ?: ""
        val orgId = if (isUuid(rawOrgId)) rawOrgId else "00000000-0000-0000-0000-000000000000"

        var targetHostelId = if (isUuid(hostelId)) hostelId else UUID.randomUUID().toString()

        // 1. Primary Save via SECURITY DEFINER RPC (bypasses RLS issues)
        var remoteSavedId: String? = null
        try {
            val params = buildJsonObject {
                put("p_hostel_id", targetHostelId)
                put("p_upi_id", upiId)
                put("p_monthly_fee", monthlyFee)
                put("p_advance_deposit", advanceDeposit)
            }
            val rpcResult = supabaseClient.postgrest.rpc("save_hostel_payment_config", params).decodeAs<kotlinx.serialization.json.JsonObject>()
            val hostelIdVal = rpcResult["hostel_id"]?.toString()?.replace("\"", "")
            if (!hostelIdVal.isNullOrBlank()) {
                remoteSavedId = hostelIdVal
            }
        } catch (e: Exception) {
            println("RPC save_hostel_payment_config error: ${e.message}")
        }

        if (!remoteSavedId.isNullOrBlank()) {
            targetHostelId = remoteSavedId
        }

        // 2. Secondary Direct Update on public.hostels table in Supabase
        try {
            val updatePayload = buildJsonObject {
                put("upi_id", upiId)
                put("monthly_fee", monthlyFee)
                put("advance_deposit", advanceDeposit)
            }
            supabaseClient.postgrest.from("hostels").update(updatePayload)
        } catch (e: Exception) {
            println("Direct update hostels table note: ${e.message}")
        }

        // 3. Update local Room DB with final targetHostelId
        val localHostel = db.hostelDao().getAllHostels().find { it.id == hostelId || it.id == targetHostelId } 
            ?: db.hostelDao().getAllHostels().firstOrNull()

        val updatedEntity = HostelEntity(
            id = targetHostelId,
            organizationId = orgId,
            name = localHostel?.name ?: "Main Campus Hostel",
            address = localHostel?.address,
            upiId = upiId,
            monthlyFee = monthlyFee,
            advanceDeposit = advanceDeposit
        )
        db.hostelDao().insertHostel(updatedEntity)
    }

    private fun isUuid(str: String): Boolean {
        if (str.isBlank()) return false
        return try {
            UUID.fromString(str)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getPendingSelfRegistrations(): Result<List<StudentSelfRegistration>> = runCatching {
        val dtos = supabaseClient.postgrest.from("student_self_registrations")
            .select { filter { eq("status", "Pending") } }
            .decodeList<StudentSelfRegistrationDto>()
        dtos.map { dto ->
            val extractedUtr = dto.utrNumber ?: run {
                val notes = dto.notes ?: ""
                val utrMatch = Regex("UTR:\\s*([^\\s|]+)").find(notes)
                utrMatch?.groupValues?.get(1)
            }
            val extractedAmount = if (dto.amountPaid != null && dto.amountPaid > 0) dto.amountPaid else run {
                val notes = dto.notes ?: ""
                val amtMatch = Regex("Advance:\\s*₹?\\s*([0-9.]+)").find(notes)
                amtMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
            }
            StudentSelfRegistration(
                id = dto.id,
                hostelId = dto.hostelId,
                fullName = dto.fullName,
                email = dto.email,
                phone = dto.phone,
                studentIdNumber = dto.studentIdNumber,
                aadhaarNumber = dto.aadhaarNumber,
                roomNumber = dto.roomNumber,
                bedNumber = dto.bedNumber,
                status = dto.status,
                notes = dto.notes,
                createdAt = dto.createdAt,
                utrNumber = extractedUtr,
                amountPaid = extractedAmount
            )
        }
    }

    override suspend fun updateSelfRegistrationStatus(
        id: String,
        status: String,
        paymentStatus: String,
        amountPaid: Double
    ): Result<Unit> = runCatching {
        // 1. Try SECURITY DEFINER RPC first (allocates bed, creates profile & invoice in DB)
        try {
            val params = buildJsonObject {
                put("p_reg_id", id)
                put("p_status", status)
                put("p_payment_status", paymentStatus)
                put("p_amount_paid", amountPaid)
            }
            supabaseClient.postgrest.rpc("approve_student_self_registration", params)
        } catch (e: Exception) {
            println("RPC approve_student_self_registration note: ${e.message}")
        }

        // 2. Direct PostgREST table update fallback
        try {
            val updatePayload = buildJsonObject {
                put("status", status)
                if (amountPaid > 0) put("amount_paid", amountPaid)
            }
            supabaseClient.postgrest.from("student_self_registrations").update(updatePayload) {
                filter { eq("id", id) }
            }
        } catch (e: Exception) {
            println("Direct update note: ${e.message}")
        }

        // 3. Force cloud pull to update local DB with created invoices & bed allocations
        try {
            syncManager.pullCloudToLocal()
        } catch (e: Exception) {
            println("Sync after approval note: ${e.message}")
        }
    }

    override suspend fun getMonthlyPaymentSubmissions(): Result<List<MonthlyPaymentSubmission>> = runCatching {
        val dtos = supabaseClient.postgrest.from("monthly_payment_submissions")
            .select { filter { eq("status", "Pending") } }
            .decodeList<MonthlyPaymentSubmissionDto>()
        dtos.map { dto ->
            MonthlyPaymentSubmission(
                id = dto.id,
                hostelId = dto.hostelId,
                studentId = dto.studentId,
                email = dto.email,
                fullName = dto.fullName,
                roomNumber = dto.roomNumber,
                bedNumber = dto.bedNumber,
                amount = dto.amount,
                utrNumber = dto.utrNumber,
                billingMonth = dto.billingMonth,
                status = dto.status,
                notes = dto.notes,
                createdAt = dto.createdAt
            )
        }
    }

    override suspend fun verifyMonthlyPayment(id: String, status: String): Result<Unit> = runCatching {
        val updatePayload = buildJsonObject {
            put("status", status)
        }
        supabaseClient.postgrest.from("monthly_payment_submissions").update(updatePayload) {
            filter { eq("id", id) }
        }
    }
}
