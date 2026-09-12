package com.hostel.management.domain.model

data class Organization(
    val id: String,
    val name: String,
    val domain: String? = null,
    val logoUrl: String? = null,
    val status: String = "Active",
    val subscriptionPlan: String = "Free",
    val totalStudents: Int = 0,
    val adminEmail: String? = null,
    val adminName: String? = null,
    val contactPhone: String? = null,
    val pricingModel: String = "Slab" // "Per-Student" or "Slab"
)

data class SaasPricingConfig(
    val id: String = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
    val perStudentRate: Double = 15.0,
    val slab1MaxStudents: Int = 50,
    val slab1Price: Double = 499.0,
    val slab2MaxStudents: Int = 150,
    val slab2Price: Double = 1299.0,
    val slab3MaxStudents: Int = 300,
    val slab3Price: Double = 2499.0
)

data class Hostel(
    val id: String,
    val organizationId: String,
    val name: String,
    val address: String?,
    val upiId: String? = null,
    val monthlyFee: Double? = 0.0,
    val advanceDeposit: Double? = 0.0
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

// Data wrappers for the 2D View Map
data class FloorMapData(
    val floor: Floor,
    val rooms: List<Room>
)

data class BuildingMapData(
    val building: Building,
    val floors: List<FloorMapData>
)

data class StudentSelfRegistration(
    val id: String,
    val hostelId: String?,
    val fullName: String,
    val email: String,
    val phone: String?,
    val studentIdNumber: String?,
    val aadhaarNumber: String?,
    val roomNumber: String?,
    val bedNumber: String?,
    val status: String,
    val notes: String?,
    val createdAt: String?,
    val utrNumber: String? = null,
    val amountPaid: Double? = 0.0
)

data class MonthlyPaymentSubmission(
    val id: String,
    val hostelId: String?,
    val studentId: String?,
    val email: String,
    val fullName: String,
    val roomNumber: String?,
    val bedNumber: String?,
    val amount: Double,
    val utrNumber: String,
    val billingMonth: String,
    val status: String,
    val notes: String?,
    val createdAt: String?
)
