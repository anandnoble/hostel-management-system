package com.hostel.management.presentation.hostel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hostel.management.domain.model.*
import com.hostel.management.domain.repository.HostelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RoomSetupConfig(
    val roomIndex: Int,
    var bedsCount: Int = 0
)

data class FloorSetupConfig(
    val floorNumber: Int,
    var roomsCount: Int = 0,
    val rooms: MutableList<RoomSetupConfig> = mutableListOf()
)

class HostelViewModel(
    private val hostelRepository: HostelRepository
) : ViewModel() {

    private val _hostels = MutableStateFlow<List<Hostel>>(emptyList())
    val hostels: StateFlow<List<Hostel>> = _hostels.asStateFlow()

    private val _buildings = MutableStateFlow<List<Building>>(emptyList())
    val buildings: StateFlow<List<Building>> = _buildings.asStateFlow()

    private val _floors = MutableStateFlow<List<Floor>>(emptyList())
    val floors: StateFlow<List<Floor>> = _floors.asStateFlow()

    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms: StateFlow<List<Room>> = _rooms.asStateFlow()

    private val _beds = MutableStateFlow<List<Bed>>(emptyList())
    val beds: StateFlow<List<Bed>> = _beds.asStateFlow()

    private val _allocations = MutableStateFlow<List<RoomAllocation>>(emptyList())
    val allocations: StateFlow<List<RoomAllocation>> = _allocations.asStateFlow()

    private val _dashboardStats = MutableStateFlow<Map<String, Any>>(emptyMap())
    val dashboardStats: StateFlow<Map<String, Any>> = _dashboardStats.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadDashboardStats() {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            hostelRepository.getDashboardStats()
                .onSuccess { stats ->
                    _dashboardStats.value = stats
                    _loading.value = false
                }
                .onFailure { err ->
                    _error.value = err.localizedMessage ?: "Failed to load statistics"
                    _loading.value = false
                }
        }
    }

    fun loadHostels() {
        _loading.value = true
        viewModelScope.launch {
            hostelRepository.getHostels()
                .onSuccess { list ->
                    _hostels.value = list
                    _loading.value = false
                }
                .onFailure { err ->
                    _error.value = err.localizedMessage ?: "Failed to load hostels"
                    _loading.value = false
                }
        }
    }

    fun createHostel(name: String, address: String?) {
        _loading.value = true
        viewModelScope.launch {
            hostelRepository.createHostel(name, address)
                .onSuccess {
                    loadHostels()
                }
                .onFailure { err ->
                    _error.value = err.localizedMessage ?: "Failed to create hostel"
                    _loading.value = false
                }
        }
    }

    fun loadBuildings(hostelId: String) {
        _loading.value = true
        viewModelScope.launch {
            hostelRepository.getBuildings(hostelId)
                .onSuccess { list ->
                    _buildings.value = list
                    _loading.value = false
                }
                .onFailure { err ->
                    _error.value = err.localizedMessage ?: "Failed to load blocks"
                    _loading.value = false
                }
        }
    }

    fun createBuilding(hostelId: String, name: String) {
        _loading.value = true
        viewModelScope.launch {
            hostelRepository.createBuilding(hostelId, name)
                .onSuccess {
                    loadBuildings(hostelId)
                }
                .onFailure { err ->
                    _error.value = err.localizedMessage ?: "Failed to create block"
                    _loading.value = false
                }
        }
    }

    fun loadFloors(buildingId: String) {
        _loading.value = true
        viewModelScope.launch {
            hostelRepository.getFloors(buildingId)
                .onSuccess { list ->
                    _floors.value = list
                    _loading.value = false
                }
                .onFailure { err ->
                    _error.value = err.localizedMessage ?: "Failed to load floors"
                    _loading.value = false
                }
        }
    }

    fun createFloor(buildingId: String, floorNumber: Int) {
        _loading.value = true
        viewModelScope.launch {
            hostelRepository.createFloor(buildingId, floorNumber)
                .onSuccess {
                    loadFloors(buildingId)
                }
                .onFailure { err ->
                    _error.value = err.localizedMessage ?: "Failed to create floor"
                    _loading.value = false
                }
        }
    }

    fun loadRooms(floorId: String) {
        _loading.value = true
        viewModelScope.launch {
            hostelRepository.getRooms(floorId)
                .onSuccess { list ->
                    _rooms.value = list
                    _loading.value = false
                }
                .onFailure { err ->
                    _error.value = err.localizedMessage ?: "Failed to load rooms"
                    _loading.value = false
                }
        }
    }

    fun createRoom(floorId: String, roomNumber: String, capacity: Int, roomType: String) {
        _loading.value = true
        viewModelScope.launch {
            hostelRepository.createRoom(floorId, roomNumber, capacity, roomType)
                .onSuccess {
                    loadRooms(floorId)
                }
                .onFailure { err ->
                    _error.value = err.localizedMessage ?: "Failed to create room"
                    _loading.value = false
                }
        }
    }

    fun loadBeds(roomId: String) {
        _loading.value = true
        viewModelScope.launch {
            hostelRepository.getBeds(roomId)
                .onSuccess { list ->
                    _beds.value = list
                    _loading.value = false
                }
                .onFailure { err ->
                    _error.value = err.localizedMessage ?: "Failed to load beds"
                    _loading.value = false
                }
        }
    }

    fun loadAllocations() {
        _loading.value = true
        viewModelScope.launch {
            hostelRepository.getAllocations()
                .onSuccess { list ->
                    _allocations.value = list
                    _loading.value = false
                }
                .onFailure { err ->
                    _error.value = err.localizedMessage ?: "Failed to load allocations"
                    _loading.value = false
                }
        }
    }

    fun allocateRoom(studentId: String, bedId: String, notes: String?, onSuccess: () -> Unit) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            hostelRepository.allocateRoom(studentId, bedId, notes)
                .onSuccess {
                    _loading.value = false
                    onSuccess()
                }
                .onFailure { err ->
                    _error.value = err.localizedMessage ?: "Room allocation failed"
                    _loading.value = false
                }
        }
    }

    fun transferRoom(allocationId: String, newBedId: String, notes: String?, onSuccess: () -> Unit) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            hostelRepository.transferRoom(allocationId, newBedId, notes)
                .onSuccess {
                    _loading.value = false
                    onSuccess()
                }
                .onFailure { err ->
                    _error.value = err.localizedMessage ?: "Room transfer failed"
                    _loading.value = false
                }
        }
    }

    fun vacateRoom(allocationId: String, onSuccess: () -> Unit) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            hostelRepository.vacateRoom(allocationId)
                .onSuccess {
                    _loading.value = false
                    onSuccess()
                }
                .onFailure { err ->
                    _error.value = err.localizedMessage ?: "Vacating room failed"
                    _loading.value = false
                }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun runInitialSetupWizard(
        floorsConfig: List<FloorSetupConfig>,
        roomPrefix: String,
        onComplete: () -> Unit
    ) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                // 1. Create Default Hostel
                val hostelResult = hostelRepository.createHostel("Main Property", "Campus")
                val hostel = hostelResult.getOrThrow()

                // 2. Create Default Building/Block
                val buildingResult = hostelRepository.createBuilding(hostel.id, "Main Block")
                val building = buildingResult.getOrThrow()

                // 3. Create Floors & Rooms dynamically
                for (floorConfig in floorsConfig) {
                    val floorResult = hostelRepository.createFloor(building.id, floorConfig.floorNumber)
                    val floor = floorResult.getOrThrow()

                    // For each room in this floor config
                    for (roomConfig in floorConfig.rooms) {
                        val roomNum = "$roomPrefix${floorConfig.floorNumber}0${roomConfig.roomIndex}" // e.g. Rm-101
                        hostelRepository.createRoom(
                            floorId = floor.id,
                            roomNumber = roomNum,
                            capacity = roomConfig.bedsCount,
                            roomType = "Standard"
                        ).getOrThrow() // This automatically creates beds in the repository up to the capacity
                    }
                }

                // Setup complete, refresh hostels and stats
                loadHostels()
                loadDashboardStats()
                _loading.value = false
                onComplete()
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Failed to complete setup wizard"
                _loading.value = false
            }
        }
    }

    fun deleteFloor(floorId: String, hostelId: String) {
        _loading.value = true
        viewModelScope.launch {
            hostelRepository.deleteFloor(floorId).onSuccess {
                loadFullHostelMap(hostelId)
                loadDashboardStats()
            }.onFailure { err ->
                _error.value = err.localizedMessage ?: "Failed to delete floor"
                _loading.value = false
            }
        }
    }

    fun deleteRoom(roomId: String, hostelId: String) {
        _loading.value = true
        viewModelScope.launch {
            hostelRepository.deleteRoom(roomId).onSuccess {
                loadFullHostelMap(hostelId)
                loadDashboardStats()
            }.onFailure { err ->
                _error.value = err.localizedMessage ?: "Failed to delete room"
                _loading.value = false
            }
        }
    }

    fun deleteBed(bedId: String, hostelId: String) {
        _loading.value = true
        viewModelScope.launch {
            hostelRepository.deleteBed(bedId).onSuccess {
                loadFullHostelMap(hostelId)
                loadDashboardStats()
            }.onFailure { err ->
                _error.value = err.localizedMessage ?: "Failed to delete bed"
                _loading.value = false
            }
        }
    }

    fun addBed(roomId: String, bedNumber: String, hostelId: String) {
        _loading.value = true
        viewModelScope.launch {
            hostelRepository.createBed(roomId, bedNumber).onSuccess {
                loadFullHostelMap(hostelId)
                loadDashboardStats()
            }.onFailure { err ->
                _error.value = err.localizedMessage ?: "Failed to add bed"
                _loading.value = false
            }
        }
    }
    
    fun addRoom(floorId: String, roomNumber: String, capacity: Int, hostelId: String) {
        _loading.value = true
        viewModelScope.launch {
            hostelRepository.createRoom(floorId, roomNumber, capacity, "Standard").onSuccess {
                loadFullHostelMap(hostelId)
                loadDashboardStats()
            }.onFailure { err ->
                _error.value = err.localizedMessage ?: "Failed to add room"
                _loading.value = false
            }
        }
    }
    
    fun addFloor(buildingId: String, floorNumber: Int, hostelId: String) {
        _loading.value = true
        viewModelScope.launch {
            hostelRepository.createFloor(buildingId, floorNumber).onSuccess {
                loadFullHostelMap(hostelId)
                loadDashboardStats()
            }.onFailure { err ->
                _error.value = err.localizedMessage ?: "Failed to add floor"
                _loading.value = false
            }
        }
    }

    private val _hostelMapData = MutableStateFlow<List<BuildingMapData>>(emptyList())
    val hostelMapData: StateFlow<List<BuildingMapData>> = _hostelMapData.asStateFlow()

    fun loadFullHostelMap(hostelId: String) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val buildingMapList = mutableListOf<BuildingMapData>()
                
                // Fetch buildings
                val buildings = hostelRepository.getBuildings(hostelId).getOrThrow()
                
                for (building in buildings) {
                    val floors = hostelRepository.getFloors(building.id).getOrThrow()
                    val floorMapList = mutableListOf<FloorMapData>()
                    
                    for (floor in floors) {
                        val rooms = hostelRepository.getRooms(floor.id).getOrThrow()
                        val fullRooms = rooms.map { room ->
                            val beds = hostelRepository.getBeds(room.id).getOrThrow()
                            room.copy(beds = beds)
                        }
                        floorMapList.add(FloorMapData(floor, fullRooms))
                    }
                    // Sort floors upwards (e.g. highest floor at top, ground at bottom)
                    // We'll reverse it in UI or here. Let's do it in UI, but keep sorted by floorNumber descending
                    buildingMapList.add(BuildingMapData(building, floorMapList.sortedByDescending { it.floor.floorNumber }))
                }
                
                _hostelMapData.value = buildingMapList
                _loading.value = false
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Failed to load hostel map data"
                _loading.value = false
            }
        }
    }
}
