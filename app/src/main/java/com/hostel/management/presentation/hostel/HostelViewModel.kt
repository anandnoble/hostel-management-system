package com.hostel.management.presentation.hostel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hostel.management.domain.model.*
import com.hostel.management.domain.repository.HostelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
}
