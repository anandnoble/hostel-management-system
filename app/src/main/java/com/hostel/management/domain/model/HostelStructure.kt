package com.hostel.management.domain.model

data class Organization(
    val id: String,
    val name: String,
    val domain: String?
)

data class Hostel(
    val id: String,
    val organizationId: String,
    val name: String,
    val address: String?
)

data class Building(
    val id: String,
    val hostelId: String,
    val name: String
)

data class Floor(
    val id: String,
    val buildingId: String,
    val floorNumber: Int
)

data class Room(
    val id: String,
    val floorId: String,
    val roomNumber: String,
    val capacity: Int,
    val roomType: String,
    val status: String, // "Available", "Partially occupied", "Full", "Maintenance", "Reserved", "Inactive"
    val notes: String? = null,
    val bedsCount: Int = 0,
    val occupiedBedsCount: Int = 0,
    val beds: List<Bed> = emptyList()
)

data class Bed(
    val id: String,
    val roomId: String,
    val bedNumber: String,
    val status: String // "Available", "Occupied", "Maintenance", "Inactive"
)

data class RoomAllocation(
    val id: String,
    val studentId: String,
    val bedId: String,
    val allocatedAt: String,
    val vacatedAt: String? = null,
    val status: String, // "Active", "Transferred", "Vacated"
    val notes: String? = null,
    val recordedBy: String? = null,
    // Helper fields for display
    val studentName: String? = null,
    val studentRollNumber: String? = null,
    val hostelName: String? = null,
    val buildingName: String? = null,
    val floorNumber: Int? = null,
    val roomNumber: String? = null,
    val bedNumber: String? = null
)
