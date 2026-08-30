package com.hostel.management.presentation.complaint

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hostel.management.domain.model.*
import com.hostel.management.presentation.auth.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintsListScreen(
    complaintViewModel: ComplaintViewModel,
    authViewModel: AuthViewModel,
    onNavigateToDetails: (String) -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val complaints by complaintViewModel.complaints.collectAsState()
    val loading by complaintViewModel.loading.collectAsState()
    val userProfile by authViewModel.currentProfile.collectAsState()

    var selectedStatusFilter by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userProfile, selectedStatusFilter) {
        userProfile?.id?.let { userId ->
            if (userProfile?.role == UserRole.STUDENT) {
                complaintViewModel.loadComplaints(studentId = userId, status = selectedStatusFilter)
            } else if (userProfile?.role == UserRole.MAINTENANCE_STAFF) {
                complaintViewModel.loadAssignedComplaints(staffId = userId)
            } else {
                complaintViewModel.loadComplaints(status = selectedStatusFilter)
            }
        }
    }

    val displayList = if (userProfile?.role == UserRole.MAINTENANCE_STAFF) {
        val assigned by complaintViewModel.assignedComplaints.collectAsState()
        if (selectedStatusFilter != null) assigned.filter { it.status == selectedStatusFilter } else assigned
    } else {
        complaints
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Maintenance Complaints") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (userProfile?.role == UserRole.STUDENT) {
                FloatingActionButton(
                    onClick = onNavigateToCreate,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Complaint")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Status filter chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                val filters = listOf("Submitted", "Assigned", "In Progress", "Resolved", "Closed")
                LazyColumn(modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            filters.forEach { status ->
                                val isSelected = selectedStatusFilter == status
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedStatusFilter = if (isSelected) null else status
                                    },
                                    label = { Text(status) }
                                )
                            }
                        }
                    }
                }
            }

            if (loading && displayList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (displayList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No complaints found.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayList) { complaint ->
                        val priorityColor = when (complaint.priority) {
                            "Emergency" -> Color(0xFFD32F2F)
                            "High" -> Color(0xFFF44336)
                            "Medium" -> Color(0xFFFF9800)
                            else -> Color(0xFF4CAF50)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToDetails(complaint.id) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(8.dp).background(priorityColor, CircleShape))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${complaint.category} • ${complaint.priority}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = priorityColor
                                        )
                                    }
                                    Text(
                                        text = complaint.status,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = complaint.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = complaint.description,
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Submitted by ${complaint.studentName ?: "Student"} on ${complaint.createdAt.take(10)}",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
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
fun ComplaintDetailsScreen(
    complaintId: String,
    complaintViewModel: ComplaintViewModel,
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit
) {
    val complaints by complaintViewModel.complaints.collectAsState()
    val assignedComplaints by complaintViewModel.assignedComplaints.collectAsState()
    val comments by complaintViewModel.comments.collectAsState()
    val staffList by complaintViewModel.staffList.collectAsState()

    val loading by complaintViewModel.loading.collectAsState()
    val userProfile by authViewModel.currentProfile.collectAsState()
    val context = LocalContext.current

    val complaint = complaints.firstOrNull { it.id == complaintId } 
        ?: assignedComplaints.firstOrNull { it.id == complaintId }

    var commentText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var imageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var showAssignStaffDialog by remember { mutableStateOf(false) }

    LaunchedEffect(complaintId) {
        complaintViewModel.loadComments(complaintId)
        complaintViewModel.loadStaffList()
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use {
                    imageBytes = it.readBytes()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Complaint Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (loading && complaint == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (complaint == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Complaint details not found.")
            }
        } else {
            val c = complaint!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        // Title, details and photo
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = c.category, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        text = c.status,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = c.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = c.description, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "Priority: ${c.priority}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                                if (!c.imageUrl.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    AsyncImage(
                                        model = c.imageUrl,
                                        contentDescription = "Attachment",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                }
                            }
                        }
                    }

                    // Admin assignment options
                    if (userProfile?.role == UserRole.SUPER_ADMIN || userProfile?.role == UserRole.HOSTEL_ADMIN) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Warden Action", fontWeight = FontWeight.Bold)
                                        Text("Assign to maintenance technician", fontSize = 11.sp)
                                    }
                                    Button(onClick = { showAssignStaffDialog = true }) {
                                        Text("Assign")
                                    }
                                }
                            }
                        }
                    }

                    // Maintenance actions
                    if (userProfile?.role == UserRole.MAINTENANCE_STAFF) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Technician Task Update", fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Button(
                                            onClick = {
                                                complaintViewModel.updateStatus(c.id, "In Progress") {
                                                    complaintViewModel.loadComplaints()
                                                    onNavigateBack()
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Accept / In Progress", fontSize = 11.sp)
                                        }

                                        Button(
                                            onClick = {
                                                complaintViewModel.updateStatus(c.id, "Resolved") {
                                                    complaintViewModel.loadComplaints()
                                                    onNavigateBack()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Mark Resolved", fontSize = 11.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Student actions (Close or Reopen)
                    if (userProfile?.role == UserRole.STUDENT && c.status == "Resolved") {
                        item {
                            Button(
                                onClick = {
                                    complaintViewModel.updateStatus(c.id, "Closed") {
                                        complaintViewModel.loadComplaints(studentId = userProfile?.id)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Confirm Resolution & Close Complaint", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Comments and notes thread header
                    item {
                        Text("Updates & Comments", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    if (comments.isEmpty()) {
                        item {
                            Text("No work updates posted yet.", color = Color.Gray, fontSize = 13.sp)
                        }
                    } else {
                        items(comments) { comment ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = comment.userName ?: "User", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(text = comment.createdAt.take(16).replace("T", " "), fontSize = 10.sp, color = Color.Gray)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = comment.comment, fontSize = 13.sp)
                                    
                                    if (!comment.imageUrl.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        AsyncImage(
                                            model = comment.imageUrl,
                                            contentDescription = "Comment Photo",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(120.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Add comment input layout at bottom
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { pickImageLauncher.launch("image/*") }) {
                                Icon(
                                    imageVector = if (selectedImageUri != null) Icons.Default.CheckCircle else Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Attach image",
                                    tint = if (selectedImageUri != null) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                                )
                            }
                            OutlinedTextField(
                                value = commentText,
                                onValueChange = { commentText = it },
                                placeholder = { Text("Write work note / comment...") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                            )
                            IconButton(
                                onClick = {
                                    if (commentText.isNotBlank() || imageBytes != null) {
                                        complaintViewModel.addComment(c.id, commentText, imageBytes, "jpg") {
                                            commentText = ""
                                            selectedImageUri = null
                                            imageBytes = null
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }

        // Warden Assign Technician Dialog
        if (showAssignStaffDialog && complaint != null) {
            AlertDialog(
                onDismissRequest = { showAssignStaffDialog = false },
                title = { Text("Assign Complaint") },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showAssignStaffDialog = false }) {
                        Text("Cancel")
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Select maintenance technician:", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 12.dp))
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)) {
                            items(staffList) { staff ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            complaintViewModel.assignComplaint(complaint.id, staff.id, "Assigned by Warden") {
                                                complaintViewModel.loadComplaints()
                                                showAssignStaffDialog = false
                                                onNavigateBack()
                                            }
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Text(staff.fullName, modifier = Modifier.padding(14.dp), fontWeight = FontWeight.SemiBold)
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
fun CreateComplaintScreen(
    complaintViewModel: ComplaintViewModel,
    onNavigateBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Electrical") }
    var priority by remember { mutableStateOf("Medium") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var imageBytes by remember { mutableStateOf<ByteArray?>(null) }

    val loading by complaintViewModel.loading.collectAsState()
    val error by complaintViewModel.error.collectAsState()
    val context = LocalContext.current

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use {
                    imageBytes = it.readBytes()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Submit Complaint") },
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
            Text("Issue details", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))

            // Category Dropdown
            var categoryExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    val categories = listOf("Electrical", "Plumbing", "Furniture", "Internet", "Cleaning", "AC", "Fan", "Water", "Bathroom", "Security", "Other")
                    categories.forEach { c ->
                        DropdownMenuItem(
                            text = { Text(c) },
                            onClick = {
                                category = c
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Priority Dropdown
            var priorityExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = priorityExpanded,
                onExpandedChange = { priorityExpanded = !priorityExpanded }
            ) {
                OutlinedTextField(
                    value = priority,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Priority") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = priorityExpanded,
                    onDismissRequest = { priorityExpanded = false }
                ) {
                    val priorities = listOf("Low", "Medium", "High", "Emergency")
                    priorities.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p) },
                            onClick = {
                                priority = p
                                priorityExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Complaint Title / Summary") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Describe the issue in detail") },
                maxLines = 5,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
            )

            Text("Image Attachment", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                Button(
                    onClick = { pickImageLauncher.launch("image/*") },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Select Photo")
                }
                Spacer(modifier = Modifier.width(16.dp))
                if (selectedImageUri != null) {
                    Text("Photo Selected", color = Color(0xFF4BB543), fontWeight = FontWeight.Bold)
                } else {
                    Text("No image attached.", color = Color.Gray, fontSize = 12.sp)
                }
            }

            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = {
                    if (title.isNotBlank() && description.isNotBlank()) {
                        complaintViewModel.createComplaint(
                            category = category,
                            title = title,
                            description = description,
                            priority = priority,
                            imageBytes = imageBytes,
                            fileExtension = "jpg"
                        ) {
                            onNavigateBack()
                        }
                    }
                },
                enabled = title.isNotBlank() && description.isNotBlank() && !loading,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Submit Complaint", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
