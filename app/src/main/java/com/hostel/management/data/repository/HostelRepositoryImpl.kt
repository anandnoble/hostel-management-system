package com.hostel.management.data.repository

import com.hostel.management.data.dto.*
import com.hostel.management.domain.model.*
import com.hostel.management.domain.repository.HostelRepository
import com.hostel.management.di.ServiceLocator
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HostelRepositoryImpl(
    private val supabaseClient: SupabaseClient
) : HostelRepository {

    override suspend fun getHostels(): Result<List<Hostel>> = runCatching {
        supabaseClient.postgrest.from("hostels").select().decodeList<HostelDto>().map { dto ->
            Hostel(dto.id, dto.organizationId, dto.name, dto.address)
        }
    }

    override suspend fun createHostel(name: String, address: String?): Result<Hostel> = runCatching {
        val profile = ServiceLocator.authRepository.getCurrentProfile().getOrThrow()
            ?: throw IllegalStateException("User not logged in")
        val orgId = profile.organizationId 
            ?: throw IllegalStateException("User has no organization")

        val dto = supabaseClient.postgrest.from("hostels").insert(
            mapOf("name" to name, "address" to address, "organization_id" to orgId)
        ) { select() }.decodeSingle<HostelDto>()

        Hostel(dto.id, dto.organizationId, dto.name, dto.address)
    }

    override suspend fun getBuildings(hostelId: String): Result<List<Building>> = runCatching {
        supabaseClient.postgrest.from("buildings").select {
            filter {
                eq("hostel_id", hostelId)
            }
        }.decodeList<BuildingDto>().map { dto ->
            Building(dto.id, dto.hostelId, dto.name)
        }
    }

    override suspend fun createBuilding(hostelId: String, name: String): Result<Building> = runCatching {
        val dto = supabaseClient.postgrest.from("buildings").insert(
            mapOf("hostel_id" to hostelId, "name" to name)
        ) { select() }.decodeSingle<BuildingDto>()

        Building(dto.id, dto.hostelId, dto.name)
    }

    override suspend fun getFloors(buildingId: String): Result<List<Floor>> = runCatching {
        supabaseClient.postgrest.from("floors").select {
            filter {
                eq("building_id", buildingId)
            }
        }.decodeList<FloorDto>().map { dto ->
            Floor(dto.id, dto.buildingId, dto.floorNumber)
        }
    }

    override suspend fun createFloor(buildingId: String, floorNumber: Int): Result<Floor> = runCatching {
        val dto = supabaseClient.postgrest.from("floors").insert(
            mapOf("building_id" to buildingId, "floor_number" to floorNumber)
        ) { select() }.decodeSingle<FloorDto>()

        Floor(dto.id, dto.buildingId, dto.floorNumber)
    }

    override suspend fun getRooms(floorId: String): Result<List<Room>> = runCatching {
        val roomsDto = supabaseClient.postgrest.from("rooms").select {
            filter {
                eq("floor_id", floorId)
            }
        }.decodeList<RoomDto>()
        
        // Count occupied beds for these rooms
        roomsDto.map { dto ->
            val beds = supabaseClient.postgrest.from("beds").select {
                filter {
                    eq("room_id", dto.id)
                }
            }.decodeList<BedDto>()
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
        val dto = supabaseClient.postgrest.from("rooms").insert(
            mapOf(
                "floor_id" to floorId,
                "room_number" to roomNumber,
                "capacity" to capacity,
                "room_type" to roomType,
                "status" to "Available"
            )
        ) { select() }.decodeSingle<RoomDto>()

        // Pre-create beds for the room
        for (i in 1..capacity) {
            val bedLabel = "Bed " + ('A'.code + i - 1).toChar()
            supabaseClient.postgrest.from("beds").insert(
                mapOf(
                    "room_id" to dto.id,
                    "bed_number" to bedLabel,
                    "status" to "Available"
                )
            )
        }

        Room(
            id = dto.id,
            floorId = dto.floorId,
            roomNumber = dto.roomNumber,
            capacity = dto.capacity,
            roomType = dto.roomType,
            status = dto.status,
            notes = dto.notes,
            bedsCount = capacity,
            occupiedBedsCount = 0
        )
    }

    override suspend fun getBeds(roomId: String): Result<List<Bed>> = runCatching {
        supabaseClient.postgrest.from("beds").select {
            filter {
                eq("room_id", roomId)
            }
        }.decodeList<BedDto>().map { dto ->
            Bed(dto.id, dto.roomId, dto.bedNumber, dto.status)
        }
    }

    override suspend fun getAllocations(): Result<List<RoomAllocation>> = runCatching {
        val allocationsDto = supabaseClient.postgrest.from("room_allocations").select().decodeList<RoomAllocationDto>()
        if (allocationsDto.isEmpty()) return Result.success(emptyList())

        val studentIds = allocationsDto.map { it.studentId }.distinct()
        val bedIds = allocationsDto.map { it.bedId }.distinct()

        val profiles = supabaseClient.postgrest.from("profiles").select().decodeList<ProfileDto>().associateBy { it.id }
        val students = supabaseClient.postgrest.from("students").select().decodeList<StudentDto>().associateBy { it.id }
        
        val beds = supabaseClient.postgrest.from("beds").select().decodeList<BedDto>().associateBy { it.id }
        val rooms = supabaseClient.postgrest.from("rooms").select().decodeList<RoomDto>().associateBy { it.id }
        val hostels = supabaseClient.postgrest.from("hostels").select().decodeList<HostelDto>().associateBy { it.id }
        
        // Also need mapping from room to floor -> building -> hostel
        val floors = supabaseClient.postgrest.from("floors").select().decodeList<FloorDto>().associateBy { it.id }
        val buildings = supabaseClient.postgrest.from("buildings").select().decodeList<BuildingDto>().associateBy { it.id }

        allocationsDto.map { dto ->
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
        val recorder = ServiceLocator.authRepository.getCurrentProfile().getOrThrow()
            ?: throw IllegalStateException("User not logged in")

        // 1. Create allocation record
        supabaseClient.postgrest.from("room_allocations").insert(
            buildJsonObject {
                put("student_id", studentId)
                put("bed_id", bedId)
                put("status", "Active")
                if (notes != null) put("notes", notes)
                put("recorded_by", recorder.id)
            }
        )

        // 2. Update bed status
        supabaseClient.postgrest.from("beds").update(
            mapOf("status" to "Occupied")
        ) {
            filter {
                eq("id", bedId)
            }
        }

        // 3. Update room status based on occupancy
        updateRoomStatusFromBeds(bedId)
    }

    override suspend fun transferRoom(
        allocationId: String,
        newBedId: String,
        notes: String?
    ): Result<Unit> = runCatching {
        val recorder = ServiceLocator.authRepository.getCurrentProfile().getOrThrow()
            ?: throw IllegalStateException("User not logged in")

        // Fetch old allocation
        val oldAlloc = supabaseClient.postgrest.from("room_allocations").select {
            filter { eq("id", allocationId) }
        }.decodeSingle<RoomAllocationDto>()

        val oldBedId = oldAlloc.bedId
        val studentId = oldAlloc.studentId

        val nowString = SimpleDateFormat("yyyy-MM-dd HH:mm:ssZZZZZ", Locale.US).format(Date())

        // 1. Close old allocation
        supabaseClient.postgrest.from("room_allocations").update(
            mapOf(
                "status" to "Transferred",
                "vacated_at" to nowString
            )
        ) {
            filter { eq("id", allocationId) }
        }

        // 2. Mark old bed as available
        supabaseClient.postgrest.from("beds").update(
            mapOf("status" to "Available")
        ) {
            filter { eq("id", oldBedId) }
        }

        // 3. Create new allocation
        supabaseClient.postgrest.from("room_allocations").insert(
            buildJsonObject {
                put("student_id", studentId)
                put("bed_id", newBedId)
                put("status", "Active")
                put("notes", "Transferred. Notes: ${notes ?: ""}")
                put("recorded_by", recorder.id)
            }
        )

        // 4. Mark new bed as occupied
        supabaseClient.postgrest.from("beds").update(
            mapOf("status" to "Occupied")
        ) {
            filter { eq("id", newBedId) }
        }

        // Update old room and new room statuses
        updateRoomStatusFromBeds(oldBedId)
        updateRoomStatusFromBeds(newBedId)
    }

    override suspend fun vacateRoom(allocationId: String): Result<Unit> = runCatching {
        val oldAlloc = supabaseClient.postgrest.from("room_allocations").select {
            filter { eq("id", allocationId) }
        }.decodeSingle<RoomAllocationDto>()

        val oldBedId = oldAlloc.bedId
        val nowString = SimpleDateFormat("yyyy-MM-dd HH:mm:ssZZZZZ", Locale.US).format(Date())

        // 1. Close allocation
        supabaseClient.postgrest.from("room_allocations").update(
            mapOf(
                "status" to "Vacated",
                "vacated_at" to nowString
            )
        ) {
            filter { eq("id", allocationId) }
        }

        // 2. Mark bed as available
        supabaseClient.postgrest.from("beds").update(
            mapOf("status" to "Available")
        ) {
            filter { eq("id", oldBedId) }
        }

        // 3. Update student status in profiles if they are fully leaving
        // (Warden will decide to mark student vacated separately, or we keep them active)
        
        updateRoomStatusFromBeds(oldBedId)
    }

    private suspend fun updateRoomStatusFromBeds(bedId: String) {
        val bed = supabaseClient.postgrest.from("beds").select {
            filter { eq("id", bedId) }
        }.decodeSingle<BedDto>()
        
        val roomId = bed.roomId
        val roomBeds = supabaseClient.postgrest.from("beds").select {
            filter { eq("room_id", roomId) }
        }.decodeList<BedDto>()

        val totalBeds = roomBeds.size
        val occupiedBeds = roomBeds.count { it.status == "Occupied" }

        val newStatus = when {
            occupiedBeds == 0 -> "Available"
            occupiedBeds == totalBeds -> "Full"
            else -> "Partially occupied"
        }

        supabaseClient.postgrest.from("rooms").update(
            mapOf("status" to newStatus)
        ) {
            filter { eq("id", roomId) }
        }
    }

    override suspend fun getStudents(
        searchQuery: String?,
        statusFilter: String?
    ): Result<List<Student>> = runCatching {
        // Query profiles with role STUDENT
        var profilesQuery = supabaseClient.postgrest.from("profiles").select {
            filter {
                eq("role", "STUDENT")
            }
        }.decodeList<ProfileDto>()

        if (!searchQuery.isNullOrBlank()) {
            profilesQuery = profilesQuery.filter {
                it.fullName.contains(searchQuery, ignoreCase = true) || it.email.contains(searchQuery, ignoreCase = true)
            }
        }

        val studentDtos = supabaseClient.postgrest.from("students").select().decodeList<StudentDto>().associateBy { it.id }

        val studentsList = profilesQuery.mapNotNull { pDto ->
            val sDto = studentDtos[pDto.id] ?: return@mapNotNull null
            
            // Check status filter
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

        studentsList
    }

    override suspend fun getStudentDetails(studentId: String): Result<Student> = runCatching {
        val pDto = supabaseClient.postgrest.from("profiles").select {
            filter { eq("id", studentId) }
        }.decodeSingle<ProfileDto>()

        val sDto = supabaseClient.postgrest.from("students").select {
            filter { eq("id", studentId) }
        }.decodeSingle<StudentDto>()

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
        val adminProfile = ServiceLocator.authRepository.getCurrentProfile().getOrThrow()
            ?: throw IllegalStateException("User not logged in")
        val orgId = adminProfile.organizationId
            ?: throw IllegalStateException("User has no organization")

        val newUser = supabaseClient.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            // Custom user metadata which handle_new_user trigger maps to profiles table!
            data = buildJsonObject {
                put("full_name", student.profile.fullName)
                put("role", "STUDENT")
                put("organization_id", orgId)
            }
        }
        
        val newUserId = newUser?.id ?: throw IllegalStateException("Failed to retrieve new user ID")

        // 2. Insert detailed student information
        supabaseClient.postgrest.from("students").insert(
            buildJsonObject {
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
            }
        )
    }

    override suspend fun updateStudent(student: Student): Result<Unit> = runCatching {
        // 1. Update Profile (Name/Phone)
        supabaseClient.postgrest.from("profiles").update(
            buildJsonObject {
                put("full_name", student.profile.fullName)
                put("phone", student.profile.phone)
            }
        ) {
            filter { eq("id", student.id) }
        }

        // 2. Update Student Info
        supabaseClient.postgrest.from("students").update(
            buildJsonObject {
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
            }
        ) {
            filter { eq("id", student.id) }
        }
    }

    override suspend fun vacateStudent(studentId: String): Result<Unit> = runCatching {
        // 1. Set status to Vacated in Students
        supabaseClient.postgrest.from("students").update(
            mapOf(
                "hostel_status" to "Vacated",
                "leaving_date" to SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            )
        ) {
            filter { eq("id", studentId) }
        }

        // 2. Release room allocation if active
        val activeAlloc = supabaseClient.postgrest.from("room_allocations").select {
            filter {
                eq("student_id", studentId)
                eq("status", "Active")
            }
        }.decodeSingleOrNull<RoomAllocationDto>()

        if (activeAlloc != null) {
            vacateRoom(activeAlloc.id).getOrThrow()
        }
    }

    override suspend fun getDashboardStats(): Result<Map<String, Any>> = runCatching {
        // Compute dashboard analytics based on database values
        val hostels = getHostels().getOrThrow()
        
        val rooms = supabaseClient.postgrest.from("rooms").select().decodeList<RoomDto>()
        val beds = supabaseClient.postgrest.from("beds").select().decodeList<BedDto>()
        val students = supabaseClient.postgrest.from("students").select {
            filter { eq("hostel_status", "Active") }
        }.decodeList<StudentDto>()

        val totalStudents = students.size
        val totalRooms = rooms.size
        val totalBeds = beds.size
        val occupiedBeds = beds.count { it.status == "Occupied" }
        val availableBeds = totalBeds - occupiedBeds
        val occupancyPct = if (totalBeds > 0) (occupiedBeds.toDouble() / totalBeds * 100).toInt() else 0

        // Invoices and complaints counts
        val complaints = supabaseClient.postgrest.from("complaints").select().decodeList<ComplaintDto>()
        val pendingComplaints = complaints.count { it.status != "Resolved" && it.status != "Closed" }
        val emergencyComplaints = complaints.count { it.priority == "Emergency" && it.status != "Resolved" && it.status != "Closed" }

        val invoices = supabaseClient.postgrest.from("fee_invoices").select().decodeList<FeeInvoiceDto>()
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
}
