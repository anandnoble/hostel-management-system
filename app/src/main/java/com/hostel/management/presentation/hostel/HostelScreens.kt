package com.hostel.management.presentation.hostel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hostel.management.domain.model.*
import com.hostel.management.presentation.student.StudentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostelStructureScreen(
    hostelViewModel: HostelViewModel,
    onNavigateToFloorRooms: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val hostels by hostelViewModel.hostels.collectAsState()
    val buildings by hostelViewModel.buildings.collectAsState()
    val floors by hostelViewModel.floors.collectAsState()
    val loading by hostelViewModel.loading.collectAsState()

    var selectedHostel by remember { mutableStateOf<Hostel?>(null) }
    var selectedBuilding by remember { mutableStateOf<Building?>(null) }

    LaunchedEffect(Unit) {
        hostelViewModel.loadHostels()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hostel Structure") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            if (loading && hostels.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Text("Select Hostel", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))

                // Hostels Row/List
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            hostels.forEach { hostel ->
                                val isSelected = selectedHostel?.id == hostel.id
                                Button(
                                    onClick = {
                                        selectedHostel = hostel
                                        selectedBuilding = null
                                        hostelViewModel.loadBuildings(hostel.id)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(hostel.name)
                                }
                            }
                        }
                    }

                    if (selectedHostel != null) {
                        item {
                            Text("Select Block/Building", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (buildings.isEmpty() && !loading) {
                            item {
                                Text("No blocks created for this hostel.", color = Color.Gray, modifier = Modifier.padding(8.dp))
                            }
                        } else {
                            item {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                ) {
                                    buildings.forEach { building ->
                                        val isSelected = selectedBuilding?.id == building.id
                                        Button(
                                            onClick = {
                                                selectedBuilding = building
                                                hostelViewModel.loadFloors(building.id)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(building.name)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (selectedBuilding != null) {
                        item {
                            Text("Select Floor", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (floors.isEmpty() && !loading) {
                            item {
                                Text("No floors created for this block.", color = Color.Gray, modifier = Modifier.padding(8.dp))
                            }
                        } else {
                            items(floors) { floor ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { onNavigateToFloorRooms(floor.id) },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (floor.floorNumber == 0) "Ground Floor" else "Floor ${floor.floorNumber}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomManagementScreen(
    floorId: String,
    hostelViewModel: HostelViewModel,
    onNavigateToAllocation: (studentId: String?, bedId: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val rooms by hostelViewModel.rooms.collectAsState()
    val beds by hostelViewModel.beds.collectAsState()
    val allocations by hostelViewModel.allocations.collectAsState()
    val loading by hostelViewModel.loading.collectAsState()

    var showRoomDetailsDialog by remember { mutableStateOf<Room?>(null) }

    LaunchedEffect(floorId) {
        hostelViewModel.loadRooms(floorId)
        hostelViewModel.loadAllocations()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rooms Map & Occupancy") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            if (loading && rooms.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (rooms.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No rooms in this floor.", color = Color.Gray)
                }
            } else {
                // 1. FLOOR OVERVIEW CAPACITY MAP (Warden/Admin Quick Scan)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Floor Capacity Quick Scan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            val totalBeds = rooms.sumOf { it.capacity }
                            val occupiedBeds = rooms.sumOf { it.occupiedBedsCount }
                            val availableBeds = totalBeds - occupiedBeds
                            Text(
                                text = "$availableBeds Available • $occupiedBeds Occupied",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (availableBeds > 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        // Horizontal list of rooms and their compact bed status slots
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(rooms) { room ->
                                val isRoomFull = room.occupiedBedsCount == room.capacity
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isRoomFull) Color(0xFFF44336).copy(alpha = 0.4f) else Color(0xFF4CAF50).copy(alpha = 0.4f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = room.roomNumber,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (room.beds.isNotEmpty()) {
                                                room.beds.forEach { bed ->
                                                    val color = when (bed.status) {
                                                        "Available" -> Color(0xFF4CAF50)
                                                        "Occupied" -> Color(0xFFF44336)
                                                        "Maintenance" -> Color(0xFF9E9E9E)
                                                        "Inactive" -> Color(0xFF333333)
                                                        "Reserved" -> Color(0xFF2196F3)
                                                        else -> Color(0xFF9E9E9E)
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .background(color, RoundedCornerShape(2.dp))
                                                    )
                                                }
                                            } else {
                                                // Fallback to capacity count if beds not loaded yet
                                                repeat(room.capacity) { idx ->
                                                    val isOccupied = idx < room.occupiedBedsCount
                                                    Box(
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .background(if (isOccupied) Color(0xFFF44336) else Color(0xFF4CAF50), RoundedCornerShape(2.dp))
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (isRoomFull) "FULL" else "${room.capacity - room.occupiedBedsCount} Avail",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isRoomFull) Color(0xFFF44336) else Color(0xFF4CAF50)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. DETAILED ROOMS GRID
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(rooms) { room ->
                        val statusColor = when (room.status) {
                            "Available" -> Color(0xFF4CAF50)
                            "Partially occupied" -> Color(0xFF2196F3)
                            "Full" -> Color(0xFFFF9800)
                            "Maintenance" -> Color(0xFFF44336)
                            else -> Color(0xFF9E9E9E)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, statusColor.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                                .clickable {
                                    showRoomDetailsDialog = room
                                    hostelViewModel.loadBeds(room.id)
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Room ${room.roomNumber}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = room.status.uppercase(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor,
                                        modifier = Modifier
                                            .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = room.roomType,
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                
                                // Visual Bed Map Row inside room card
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (room.beds.isNotEmpty()) {
                                        room.beds.forEach { bed ->
                                            val color = when (bed.status) {
                                                "Available" -> Color(0xFF4CAF50)
                                                "Occupied" -> Color(0xFFF44336)
                                                "Maintenance" -> Color(0xFF9E9E9E)
                                                "Inactive" -> Color(0xFF333333)
                                                "Reserved" -> Color(0xFF2196F3)
                                                else -> Color(0xFF9E9E9E)
                                            }
                                            val symbol = when (bed.status) {
                                                "Available" -> "✓"
                                                "Occupied" -> "●"
                                                "Maintenance" -> "⚙"
                                                "Inactive" -> "🚫"
                                                "Reserved" -> "🔒"
                                                else -> "?"
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .background(color.copy(alpha = 0.12f), RoundedCornerShape(5.dp))
                                                    .border(1.dp, color, RoundedCornerShape(5.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = symbol,
                                                    color = color,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    } else {
                                        // Fallback if beds lists are not loaded yet
                                        repeat(room.capacity) { idx ->
                                            val isOccupied = idx < room.occupiedBedsCount
                                            val color = if (isOccupied) Color(0xFFF44336) else Color(0xFF4CAF50)
                                            val symbol = if (isOccupied) "●" else "✓"
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .background(color.copy(alpha = 0.12f), RoundedCornerShape(5.dp))
                                                    .border(1.dp, color, RoundedCornerShape(5.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = symbol,
                                                    color = color,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                
                                val available = room.capacity - room.occupiedBedsCount
                                Text(
                                    text = if (available == 0) "FULL" else "$available Avail • ${room.occupiedBedsCount} Occ",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Room Details & Beds Modal Dialog
        if (showRoomDetailsDialog != null) {
            val r = showRoomDetailsDialog!!
            AlertDialog(
                onDismissRequest = { showRoomDetailsDialog = null },
                confirmButton = {
                    TextButton(onClick = { showRoomDetailsDialog = null }) {
                        Text("Close")
                    }
                },
                title = { Text("Room ${r.roomNumber} Details") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    ) {
                        Text("Type: ${r.roomType}", fontSize = 14.sp)
                        Text("Status: ${r.status}", fontSize = 14.sp)
                        if (!r.notes.isNullOrBlank()) {
                            Text("Notes: ${r.notes}", fontSize = 12.sp, color = Color.Gray)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Visual Bed Map", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        if (beds.isEmpty() && loading) {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        } else {
                            // Large Movie Ticket Style slots in dialog
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            ) {
                                beds.forEach { bed ->
                                    val color = when (bed.status) {
                                        "Available" -> Color(0xFF4CAF50)
                                        "Occupied" -> Color(0xFFF44336)
                                        "Maintenance" -> Color(0xFF9E9E9E)
                                        "Inactive" -> Color(0xFF333333)
                                        "Reserved" -> Color(0xFF2196F3)
                                        else -> Color(0xFF9E9E9E)
                                    }
                                    val symbol = when (bed.status) {
                                        "Available" -> "✓"
                                        "Occupied" -> "●"
                                        "Maintenance" -> "⚙"
                                        "Inactive" -> "🚫"
                                        "Reserved" -> "🔒"
                                        else -> "?"
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                            .border(1.5.dp, color, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = bed.bedNumber,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = symbol,
                                                color = color,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Beds & Occupants Details", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            beds.forEach { bed ->
                                val activeAlloc = allocations.firstOrNull { it.bedId == bed.id && it.status == "Active" }
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(bed.bedNumber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            if (activeAlloc != null) {
                                                Text("Occupant: ${activeAlloc.studentName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                            } else {
                                                Text("Status: ${bed.status}", fontSize = 11.sp, color = Color.Gray)
                                            }
                                        }

                                        if (activeAlloc != null) {
                                            IconButton(
                                                onClick = {
                                                    hostelViewModel.vacateRoom(activeAlloc.id) {
                                                        hostelViewModel.loadAllocations()
                                                        hostelViewModel.loadRooms(floorId)
                                                        showRoomDetailsDialog = null
                                                    }
                                                }
                                            ) {
                                                Icon(Icons.Default.PersonRemove, contentDescription = "Vacate Bed", tint = MaterialTheme.colorScheme.error)
                                            }
                                        } else if (bed.status == "Available") {
                                            Button(
                                                onClick = {
                                                    showRoomDetailsDialog = null
                                                    onNavigateToAllocation(null, bed.id)
                                                },
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("Allocate", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomAllocationScreen(
    preselectedStudentId: String?,
    preselectedBedId: String?,
    hostelViewModel: HostelViewModel,
    studentViewModel: StudentViewModel,
    onNavigateBack: () -> Unit
) {
    var selectedStudent by remember { mutableStateOf<Student?>(null) }
    var selectedHostel by remember { mutableStateOf<Hostel?>(null) }
    var selectedBuilding by remember { mutableStateOf<Building?>(null) }
    var selectedFloor by remember { mutableStateOf<Floor?>(null) }
    var selectedRoom by remember { mutableStateOf<Room?>(null) }
    var selectedBed by remember { mutableStateOf<Bed?>(null) }
    var notes by remember { mutableStateOf("") }

    val hostels by hostelViewModel.hostels.collectAsState()
    val buildings by hostelViewModel.buildings.collectAsState()
    val floors by hostelViewModel.floors.collectAsState()
    val rooms by hostelViewModel.rooms.collectAsState()
    val beds by hostelViewModel.beds.collectAsState()
    val students by studentViewModel.students.collectAsState()

    val loading by hostelViewModel.loading.collectAsState()
    val error by hostelViewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        hostelViewModel.loadHostels()
        studentViewModel.loadStudents(status = "Active")
        
        if (preselectedStudentId != null) {
            studentViewModel.loadStudentDetails(preselectedStudentId)
        }
    }

    val selectedStudentDetail by studentViewModel.selectedStudent.collectAsState()
    LaunchedEffect(selectedStudentDetail) {
        if (preselectedStudentId != null && selectedStudentDetail != null) {
            selectedStudent = selectedStudentDetail
        }
    }

    // Automatically load beds when room changes
    LaunchedEffect(selectedRoom) {
        selectedRoom?.let { room ->
            hostelViewModel.loadBeds(room.id)
            selectedBed = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Allocate Bed Map") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // STEP 1: STUDENT SELECTION
            Text("1. Select Student", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            if (preselectedStudentId != null && selectedStudent != null) {
                // Preselected student card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(selectedStudent!!.profile.fullName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("ID: ${selectedStudent!!.studentIdNumber} • ${selectedStudent!!.course ?: ""}", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            } else {
                var studentMenuExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = studentMenuExpanded,
                    onExpandedChange = { studentMenuExpanded = !studentMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedStudent?.profile?.fullName ?: "Select Student",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = studentMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = studentMenuExpanded,
                        onDismissRequest = { studentMenuExpanded = false }
                    ) {
                        students.forEach { s ->
                            DropdownMenuItem(
                                text = { Text("${s.profile.fullName} (${s.studentIdNumber})") },
                                onClick = {
                                    selectedStudent = s
                                    studentMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (selectedStudent != null) {
                Spacer(modifier = Modifier.height(20.dp))
                // STEP 2: SELECT HOSTEL
                Text("2. Select Hostel", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    hostels.forEach { hostel ->
                        val isSelected = selectedHostel?.id == hostel.id
                        Button(
                            onClick = {
                                selectedHostel = hostel
                                selectedBuilding = null
                                selectedFloor = null
                                selectedRoom = null
                                selectedBed = null
                                hostelViewModel.loadBuildings(hostel.id)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(hostel.name, fontSize = 12.sp)
                        }
                    }
                }
            }

            if (selectedHostel != null) {
                Spacer(modifier = Modifier.height(16.dp))
                // STEP 3: SELECT BLOCK/BUILDING
                Text("3. Select Block/Building", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    buildings.forEach { building ->
                        val isSelected = selectedBuilding?.id == building.id
                        Button(
                            onClick = {
                                selectedBuilding = building
                                selectedFloor = null
                                selectedRoom = null
                                selectedBed = null
                                hostelViewModel.loadFloors(building.id)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(building.name, fontSize = 12.sp)
                        }
                    }
                }
            }

            if (selectedBuilding != null) {
                Spacer(modifier = Modifier.height(16.dp))
                // STEP 4: SELECT FLOOR
                Text("4. Select Floor", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    floors.forEach { floor ->
                        val isSelected = selectedFloor?.id == floor.id
                        Button(
                            onClick = {
                                selectedFloor = floor
                                selectedRoom = null
                                selectedBed = null
                                hostelViewModel.loadRooms(floor.id)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(if (floor.floorNumber == 0) "G Floor" else "Floor ${floor.floorNumber}", fontSize = 12.sp)
                        }
                    }
                }
            }

            if (selectedFloor != null) {
                Spacer(modifier = Modifier.height(16.dp))
                // STEP 5: SELECT ROOM (WITH MINI COMPACT BED STATUS MAPS)
                Text("5. Select Room", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                if (rooms.isEmpty()) {
                    Text("No rooms in this floor.", color = Color.Gray, fontSize = 12.sp)
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 220.dp)
                    ) {
                        items(rooms) { room ->
                            val isSelected = selectedRoom?.id == room.id
                            val available = room.capacity - room.occupiedBedsCount
                            val statusColor = when (room.status) {
                                "Available" -> Color(0xFF4CAF50)
                                "Partially occupied" -> Color(0xFF2196F3)
                                "Full" -> Color(0xFFFF9800)
                                "Maintenance" -> Color(0xFFF44336)
                                else -> Color(0xFF9E9E9E)
                            }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedRoom = room
                                    },
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Room ${room.roomNumber}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(statusColor, CircleShape)
                                        )
                                    }
                                    Text(room.roomType, fontSize = 10.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Compact Bed indicators
                                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        if (room.beds.isNotEmpty()) {
                                            room.beds.forEach { bed ->
                                                val c = when (bed.status) {
                                                    "Available" -> Color(0xFF4CAF50)
                                                    "Occupied" -> Color(0xFFF44336)
                                                    "Maintenance" -> Color(0xFF9E9E9E)
                                                    "Inactive" -> Color(0xFF333333)
                                                    "Reserved" -> Color(0xFF2196F3)
                                                    else -> Color(0xFF9E9E9E)
                                                }
                                                Box(modifier = Modifier.size(8.dp).background(c, RoundedCornerShape(1.5.dp)))
                                            }
                                        } else {
                                            repeat(room.capacity) { idx ->
                                                val isOcc = idx < room.occupiedBedsCount
                                                Box(modifier = Modifier.size(8.dp).background(if (isOcc) Color(0xFFF44336) else Color(0xFF4CAF50), RoundedCornerShape(1.5.dp)))
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (available == 0) "FULL" else "$available Available",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (available > 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (selectedRoom != null) {
                Spacer(modifier = Modifier.height(20.dp))
                // STEP 6: MOVIE TICKET STYLE SEAT/BED SELECTION
                Text("6. Select Bed (Movie Ticket Style Map)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))

                // Legend
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendItem("Avail", Color(0xFF4CAF50), "✓")
                        LegendItem("Occ", Color(0xFFF44336), "●")
                        LegendItem("Select", Color(0xFFFFB300), "✓")
                        LegendItem("Reserv", Color(0xFF2196F3), "🔒")
                        LegendItem("Maint", Color(0xFF9E9E9E), "⚙")
                        LegendItem("Disab", Color(0xFF333333), "🚫")
                    }
                }

                if (beds.isEmpty() && loading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    }
                } else if (beds.isEmpty()) {
                    Text("No beds registered in this room.", color = Color.Gray, fontSize = 12.sp)
                } else {
                    // Bed Selection Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.heightIn(max = 140.dp).padding(vertical = 8.dp)
                    ) {
                        items(beds) { bed ->
                            val isSelected = selectedBed?.id == bed.id
                            
                            val (color, symbol, isSelectable) = when {
                                isSelected -> Triple(Color(0xFFFFB300), "✓", true)
                                bed.status == "Available" -> Triple(Color(0xFF4CAF50), "✓", true)
                                bed.status == "Occupied" -> Triple(Color(0xFFF44336), "●", false)
                                bed.status == "Maintenance" -> Triple(Color(0xFF9E9E9E), "⚙", false)
                                bed.status == "Inactive" -> Triple(Color(0xFF333333), "🚫", false)
                                bed.status == "Reserved" -> Triple(Color(0xFF2196F3), "🔒", false)
                                else -> Triple(Color(0xFF9E9E9E), "?", false)
                            }

                            Card(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clickable(enabled = isSelectable) {
                                        selectedBed = if (isSelected) null else bed
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) color.copy(alpha = 0.2f) else color.copy(alpha = 0.08f)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isSelected) 2.5.dp else 1.5.dp,
                                    color
                                )
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = bed.bedNumber,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = symbol,
                                        color = color,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (selectedBed != null) {
                Spacer(modifier = Modifier.height(20.dp))
                // SUMMARY CARD
                Text("7. Allocation Summary", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ready to Allocate", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Divider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        
                        SummaryRow("Student", selectedStudent?.profile?.fullName ?: "-")
                        SummaryRow("Hostel", selectedHostel?.name ?: "-")
                        SummaryRow("Block", selectedBuilding?.name ?: "-")
                        SummaryRow("Floor", if (selectedFloor?.floorNumber == 0) "Ground Floor" else "Floor ${selectedFloor?.floorNumber}")
                        SummaryRow("Room & Bed", "Room ${selectedRoom?.roomNumber} — ${selectedBed?.bedNumber}")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Notes", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("Optional allocation notes...") },
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )
            }

            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val sId = selectedStudent?.id
                    val bId = selectedBed?.id ?: preselectedBedId
                    if (sId != null && bId != null) {
                        hostelViewModel.allocateRoom(sId, bId, notes) {
                            onNavigateBack()
                        }
                    }
                },
                enabled = selectedStudent != null && (selectedBed != null || preselectedBedId != null) && !loading,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Confirm Allocation", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color, symbol: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(color.copy(alpha = 0.12f), RoundedCornerShape(3.dp))
                .border(1.dp, color, RoundedCornerShape(3.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(symbol, color = color, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}
