package com.hostel.management.presentation.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hostel.management.domain.model.*
import com.hostel.management.presentation.announcement.AnnouncementViewModel
import com.hostel.management.presentation.auth.AuthViewModel
import com.hostel.management.presentation.complaint.ComplaintViewModel
import com.hostel.management.presentation.finance.FinanceViewModel
import com.hostel.management.presentation.hostel.HostelViewModel
import com.hostel.management.presentation.student.StudentViewModel

// -------------------------------------------------------------
// HELPER COMPOSABLES
// -------------------------------------------------------------

@Composable
fun DashboardHeader(
    profile: Profile?,
    onLogout: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Welcome Back,",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = profile?.fullName ?: "User",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = profile?.role?.name?.replace("_", " ") ?: "Role",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row {
                IconButton(onClick = onNotificationsClick) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                IconButton(onClick = onLogout) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Logout",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun ShortcutItem(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(color.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(68.dp)
        )
    }
}

// -------------------------------------------------------------
// 1. ADMIN DASHBOARD
// -------------------------------------------------------------
@Composable
fun AdminDashboardScreen(
    authViewModel: AuthViewModel,
    hostelViewModel: HostelViewModel,
    onLogout: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val profile by authViewModel.currentProfile.collectAsState()
    val stats by hostelViewModel.dashboardStats.collectAsState()

    LaunchedEffect(Unit) {
        hostelViewModel.loadDashboardStats()
    }

    Scaffold(
        bottomBar = {
            // Can add bottom navigation if needed
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            DashboardHeader(
                profile = profile,
                onLogout = onLogout,
                onNotificationsClick = { onNavigate("notifications") }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Key metrics grid
                Text("System Overview", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    MetricCard(
                        title = "Students",
                        value = stats["totalStudents"]?.toString() ?: "0",
                        icon = Icons.Default.People,
                        color = Color(0xFF3F51B5),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Beds Occupied",
                        value = "${stats["occupiedBeds"] ?: 0}/${stats["totalBeds"] ?: 0}",
                        icon = Icons.Default.Bed,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    MetricCard(
                        title = "Occupancy Rate",
                        value = "${stats["occupancyPct"] ?: 0}%",
                        icon = Icons.Default.PieChart,
                        color = Color(0xFFFF9800),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Pending Dues",
                        value = "₹${stats["pendingFees"] ?: 0}",
                        icon = Icons.Default.Payments,
                        color = Color(0xFFF44336),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Shortcuts Panel
                Text("Quick Actions", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            ShortcutItem("Students", Icons.Default.People, Color(0xFF3F51B5)) { onNavigate("student_list") }
                            ShortcutItem("Rooms Grid", Icons.Default.GridOn, Color(0xFF009688)) { onNavigate("hostel_structure") }
                            ShortcutItem("Allocations", Icons.Default.AssignmentInd, Color(0xFFE91E63)) { onNavigate("room_allocation") }
                            ShortcutItem("Finance", Icons.Default.AccountBalanceWallet, Color(0xFF4CAF50)) { onNavigate("fees_list") }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            ShortcutItem("Complaints", Icons.Default.Warning, Color(0xFFF44336)) { onNavigate("complaints_list") }
                            ShortcutItem("Announce", Icons.Default.Campaign, Color(0xFF9C27B0)) { onNavigate("announcements") }
                            ShortcutItem("Reports", Icons.Default.Assessment, Color(0xFF673AB7)) { onNavigate("reports") }
                            ShortcutItem("Audit Logs", Icons.Default.History, Color(0xFF607D8B)) { onNavigate("audit_logs") }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

// -------------------------------------------------------------
// 2. STUDENT DASHBOARD
// -------------------------------------------------------------
@Composable
fun StudentDashboardScreen(
    authViewModel: AuthViewModel,
    hostelViewModel: HostelViewModel,
    financeViewModel: FinanceViewModel,
    complaintViewModel: ComplaintViewModel,
    announcementViewModel: AnnouncementViewModel,
    onLogout: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val profile by authViewModel.currentProfile.collectAsState()
    val allocations by hostelViewModel.allocations.collectAsState()
    val invoices by financeViewModel.invoices.collectAsState()
    val complaints by complaintViewModel.complaints.collectAsState()
    val announcements by announcementViewModel.announcements.collectAsState()

    LaunchedEffect(profile) {
        profile?.id?.let { studentId ->
            hostelViewModel.loadAllocations()
            financeViewModel.loadInvoices(studentId = studentId)
            complaintViewModel.loadComplaints(studentId = studentId)
            announcementViewModel.loadAnnouncements()
        }
    }

    val activeAllocation = allocations.firstOrNull { it.studentId == profile?.id && it.status == "Active" }
    val pendingInvoice = invoices.firstOrNull { it.paymentStatus != "Paid" && it.paymentStatus != "Waived" }
    val openComplaint = complaints.firstOrNull { it.status != "Resolved" && it.status != "Closed" }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            DashboardHeader(
                profile = profile,
                onLogout = onLogout,
                onNotificationsClick = { onNavigate("notifications") }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Room Allocation details card
                Text("My Accommodation", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (activeAllocation != null) {
                            Text(
                                text = "Your Current Assignment",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            // Visual 5-row list of Hostel, Block, Floor, Room, Bed
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "🏢 My Hostel", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                Text(text = activeAllocation.hostelName ?: "Not Assigned", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "🧱 My Block/Building", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                Text(text = activeAllocation.buildingName ?: "Not Assigned", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "🪜 My Floor", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                val flText = if (activeAllocation.floorNumber != null) {
                                    if (activeAllocation.floorNumber == 0) "Ground Floor" else "Floor ${activeAllocation.floorNumber}"
                                } else {
                                    "Not Assigned"
                                }
                                Text(text = flText, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "🚪 My Room", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                Text(text = activeAllocation.roomNumber ?: "Not Assigned", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "🛏 My Bed", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                Text(text = activeAllocation.bedNumber ?: "Not Assigned", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Allocated on: ${activeAllocation.allocatedAt.take(10)}",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("No Room Allocated Yet", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                                    Text("Please contact the warden for room/bed selection.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Finance & Complaints summaries
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Billing Dues
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp)
                            .clickable { onNavigate("fees_list") },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF4CAF50).copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Receipt, contentDescription = null, tint = Color(0xFF4CAF50))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Mess Dues", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (pendingInvoice != null) "₹${pendingInvoice.balance}" else "No Dues",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (pendingInvoice != null) Color(0xFFF44336) else Color(0xFF4CAF50)
                            )
                        }
                    }

                    // Complaints
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 6.dp)
                            .clickable { onNavigate("complaints_list") },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFFF9800).copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Build, contentDescription = null, tint = Color(0xFFFF9800))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Complaints", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = openComplaint?.status ?: "All Clear",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (openComplaint != null) Color(0xFFFF9800) else Color(0xFF4CAF50)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Recent Announcements section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Announcements", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { onNavigate("announcements") }) {
                        Text("View All")
                    }
                }

                if (announcements.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("No announcements today.", fontSize = 13.sp, color = Color.Gray)
                        }
                    }
                } else {
                    announcements.take(2).forEach { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    val pillColor = if (item.priority == "High") Color(0xFFF44336) else MaterialTheme.colorScheme.primary
                                    Text(
                                        text = item.priority,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = pillColor,
                                        modifier = Modifier
                                            .background(pillColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = item.message, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

// -------------------------------------------------------------
// 3. ACCOUNTANT DASHBOARD
// -------------------------------------------------------------
@Composable
fun AccountantDashboardScreen(
    authViewModel: AuthViewModel,
    financeViewModel: FinanceViewModel,
    onLogout: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val profile by authViewModel.currentProfile.collectAsState()
    val stats by financeViewModel.financialStats.collectAsState()

    LaunchedEffect(Unit) {
        financeViewModel.loadFinancialStats()
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            DashboardHeader(
                profile = profile,
                onLogout = onLogout,
                onNotificationsClick = { onNavigate("notifications") }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text("Finance Summary", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    MetricCard(
                        title = "Total Collected",
                        value = "₹${stats["totalCollected"] ?: 0}",
                        icon = Icons.Default.TrendingUp,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Today's Payment",
                        value = "₹${stats["todayCollected"] ?: 0}",
                        icon = Icons.Default.MonetizationOn,
                        color = Color(0xFF3F51B5),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    MetricCard(
                        title = "Pending Fees",
                        value = "₹${stats["totalPending"] ?: 0}",
                        icon = Icons.Default.HourglassEmpty,
                        color = Color(0xFFFF9800),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Overdue Amount",
                        value = "₹${stats["totalOverdue"] ?: 0}",
                        icon = Icons.Default.AssignmentLate,
                        color = Color(0xFFF44336),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Quick Actions", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        ShortcutItem("Fee List", Icons.Default.ReceiptLong, Color(0xFF4CAF50)) { onNavigate("fees_list") }
                        ShortcutItem("Generate Bills", Icons.Default.PostAdd, Color(0xFF009688)) {
                            // Can trigger bottom sheet / dialog to input billing month
                            onNavigate("fees_list")
                        }
                        ShortcutItem("Reports", Icons.Default.Assessment, Color(0xFF9C27B0)) { onNavigate("reports") }
                        ShortcutItem("Students", Icons.Default.People, Color(0xFF3F51B5)) { onNavigate("student_list") }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

// -------------------------------------------------------------
// 4. MAINTENANCE DASHBOARD
// -------------------------------------------------------------
@Composable
fun MaintenanceDashboardScreen(
    authViewModel: AuthViewModel,
    complaintViewModel: ComplaintViewModel,
    onLogout: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val profile by authViewModel.currentProfile.collectAsState()
    val assignedComplaints by complaintViewModel.assignedComplaints.collectAsState()
    val loading by complaintViewModel.loading.collectAsState()

    LaunchedEffect(profile) {
        profile?.id?.let { staffId ->
            complaintViewModel.loadAssignedComplaints(staffId)
        }
    }

    val highPriorityCount = assignedComplaints.count { it.priority == "High" || it.priority == "Emergency" }
    val pendingCount = assignedComplaints.count { it.status == "Assigned" || it.status == "In Progress" }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            DashboardHeader(
                profile = profile,
                onLogout = onLogout,
                onNotificationsClick = { onNavigate("notifications") }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text("Task Summary", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    MetricCard(
                        title = "My Total Tasks",
                        value = assignedComplaints.size.toString(),
                        icon = Icons.Default.Build,
                        color = Color(0xFF607D8B),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Urgent Tasks",
                        value = highPriorityCount.toString(),
                        icon = Icons.Default.ReportProblem,
                        color = Color(0xFFF44336),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Assigned Complaints", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { profile?.id?.let { complaintViewModel.loadAssignedComplaints(it) } }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (loading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (assignedComplaints.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("No complaints assigned to you!", fontWeight = FontWeight.Bold)
                                Text("Sit back and relax, or check in later.", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(assignedComplaints) { complaint ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigate("complaint_details/${complaint.id}") },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val priorityColor = when (complaint.priority) {
                                                "Emergency" -> Color(0xFFD32F2F)
                                                "High" -> Color(0xFFF44336)
                                                "Medium" -> Color(0xFFFF9800)
                                                else -> Color(0xFF4CAF50)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(priorityColor, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = complaint.category, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = priorityColor)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = complaint.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(text = complaint.description, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    
                                    // Status Badge
                                    Text(
                                        text = complaint.status,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
