package com.hostel.management.domain.repository

import com.hostel.management.domain.model.*

interface HostelRepository {
    suspend fun getHostels(): Result<List<Hostel>>
    suspend fun createHostel(name: String, address: String?): Result<Hostel>
    suspend fun getBuildings(hostelId: String): Result<List<Building>>
    suspend fun createBuilding(hostelId: String, name: String): Result<Building>
    suspend fun getFloors(buildingId: String): Result<List<Floor>>
    suspend fun createFloor(buildingId: String, floorNumber: Int): Result<Floor>
    suspend fun getRooms(floorId: String): Result<List<Room>>
    suspend fun createRoom(floorId: String, roomNumber: String, capacity: Int, roomType: String): Result<Room>
    suspend fun getBeds(roomId: String): Result<List<Bed>>
    suspend fun createBed(roomId: String, bedNumber: String): Result<Bed>
    
    // Structural deletions
    suspend fun deleteFloor(floorId: String): Result<Unit>
    suspend fun deleteRoom(roomId: String): Result<Unit>
    suspend fun deleteBed(bedId: String): Result<Unit>
    
    // Allocations
    suspend fun getAllocations(): Result<List<RoomAllocation>>
    suspend fun allocateRoom(studentId: String, bedId: String, notes: String?): Result<Unit>
    suspend fun transferRoom(allocationId: String, newBedId: String, notes: String?): Result<Unit>
    suspend fun vacateRoom(allocationId: String): Result<Unit>
    
    // Students
    suspend fun getStudents(searchQuery: String? = null, statusFilter: String? = null): Result<List<Student>>
    suspend fun getStudentDetails(studentId: String): Result<Student>
    suspend fun addStudent(student: Student, email: String, password: String): Result<Unit>
    suspend fun updateStudent(student: Student): Result<Unit>
    suspend fun vacateStudent(studentId: String): Result<Unit>
    
    // Dashboard Stats
    suspend fun getDashboardStats(): Result<Map<String, Any>>
    
    // Hostel Payment Config
    suspend fun updateHostelPaymentConfig(hostelId: String, upiId: String, monthlyFee: Double, advanceDeposit: Double): Result<Unit>
    
    // Self Registrations
    suspend fun getPendingSelfRegistrations(): Result<List<StudentSelfRegistration>>
    suspend fun updateSelfRegistrationStatus(id: String, status: String): Result<Unit>

    // Monthly Payment Submissions
    suspend fun getMonthlyPaymentSubmissions(): Result<List<MonthlyPaymentSubmission>>
    suspend fun verifyMonthlyPayment(id: String, status: String): Result<Unit>
}
