package com.hostel.management.presentation.dashboard

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.hostel.management.presentation.complaint.ComplaintViewModel
import com.hostel.management.presentation.finance.FinanceViewModel
import com.hostel.management.presentation.hostel.HostelViewModel
import com.hostel.management.presentation.student.StudentViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    hostelViewModel: HostelViewModel,
    studentViewModel: StudentViewModel,
    financeViewModel: FinanceViewModel,
    complaintViewModel: ComplaintViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }

    val dashboardStats by hostelViewModel.dashboardStats.collectAsState()
    val students by studentViewModel.students.collectAsState()
    val invoices by financeViewModel.invoices.collectAsState()
    val complaints by complaintViewModel.complaints.collectAsState()

    LaunchedEffect(Unit) {
        hostelViewModel.loadDashboardStats()
        studentViewModel.loadStudents()
        financeViewModel.loadInvoices()
        complaintViewModel.loadComplaints()
    }

    val tabs = listOf("Occupancy", "Students", "Fees", "Complaints")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports & Analytics") },
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
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(label, fontSize = 13.sp) }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> OccupancyReportTab(dashboardStats, context)
                    1 -> StudentsReportTab(students, context)
                    2 -> FeesReportTab(invoices, context)
                    3 -> ComplaintsReportTab(complaints, context)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB CONTENTS
// -------------------------------------------------------------

@Composable
fun OccupancyReportTab(stats: Map<String, Any>, context: Context) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text("Occupancy Statistics", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))

            ReportRow("Total Hostels", stats["totalHostels"]?.toString() ?: "0")
            ReportRow("Total Rooms", stats["totalRooms"]?.toString() ?: "0")
            ReportRow("Total Beds Capacity", stats["totalBeds"]?.toString() ?: "0")
            ReportRow("Occupied Beds", stats["occupiedBeds"]?.toString() ?: "0")
            ReportRow("Available Beds", stats["availableBeds"]?.toString() ?: "0")
            ReportRow("Overall Occupancy Rate", "${stats["occupancyPct"] ?: 0}%")

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val csv = "Metric,Value\n" +
                            "Total Hostels,${stats["totalHostels"] ?: 0}\n" +
                            "Total Rooms,${stats["totalRooms"] ?: 0}\n" +
                            "Total Beds Capacity,${stats["totalBeds"] ?: 0}\n" +
                            "Occupied Beds,${stats["occupiedBeds"] ?: 0}\n" +
                            "Available Beds,${stats["availableBeds"] ?: 0}\n" +
                            "Occupancy Rate,${stats["occupancyPct"] ?: 0}%\n"
                    shareReportCsv(context, "occupancy_report.csv", csv)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Occupancy to CSV")
            }
        }
    }
}

@Composable
fun StudentsReportTab(students: List<com.hostel.management.domain.model.Student>, context: Context) {
    val total = students.size
    val active = students.count { it.hostelStatus == "Active" }
    val vacated = students.count { it.hostelStatus == "Vacated" }
    val suspended = students.count { it.hostelStatus == "Suspended" }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text("Students Demographics", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))

            ReportRow("Total Registered Students", total.toString())
            ReportRow("Active Residents", active.toString())
            ReportRow("Vacated Students", vacated.toString())
            ReportRow("Suspended Records", suspended.toString())

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val builder = StringBuilder("Student ID,Name,Course,Status,Admission Date\n")
                    students.forEach { s ->
                        builder.append("${s.studentIdNumber},${s.profile.fullName},${s.course ?: ""},${s.hostelStatus},${s.admissionDate}\n")
                    }
                    shareReportCsv(context, "students_report.csv", builder.toString())
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Students list to CSV")
            }
        }
    }
}

@Composable
fun FeesReportTab(invoices: List<com.hostel.management.domain.model.FeeInvoice>, context: Context) {
    val totalBilled = invoices.sumOf { it.amount + it.lateFee - it.discount }
    val totalCollected = invoices.sumOf { it.amountPaid }
    val totalPending = invoices.sumOf { it.balance }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text("Financial Invoicing Summary", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))

            ReportRow("Total Invoiced Amount", "₹$totalBilled")
            ReportRow("Total Collected Amount", "₹$totalCollected")
            ReportRow("Total Outstanding Balance", "₹$totalPending")

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val builder = StringBuilder("Student Name,Month,Billed,Paid,Balance,Status\n")
                    invoices.forEach { inv ->
                        builder.append("${inv.studentName ?: ""},${inv.billingMonth},${inv.amount},${inv.amountPaid},${inv.balance},${inv.paymentStatus}\n")
                    }
                    shareReportCsv(context, "finance_report.csv", builder.toString())
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Transactions to CSV")
            }
        }
    }
}

@Composable
fun ComplaintsReportTab(complaints: List<com.hostel.management.domain.model.Complaint>, context: Context) {
    val total = complaints.size
    val open = complaints.count { it.status != "Resolved" && it.status != "Closed" }
    val resolved = complaints.count { it.status == "Resolved" || it.status == "Closed" }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text("Complaints Breakdown", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))

            ReportRow("Total Complaints Logged", total.toString())
            ReportRow("Active Open Issues", open.toString())
            ReportRow("Resolved/Closed Issues", resolved.toString())

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val builder = StringBuilder("Category,Title,Priority,Status,Created At\n")
                    complaints.forEach { c ->
                        builder.append("${c.category},${c.title},${c.priority},${c.status},${c.createdAt.take(10)}\n")
                    }
                    shareReportCsv(context, "complaints_report.csv", builder.toString())
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Complaints to CSV")
            }
        }
    }
}

@Composable
fun ReportRow(label: String, value: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontWeight = FontWeight.Medium)
            Text(value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

// -------------------------------------------------------------
// SECURE CSV SHARE
// -------------------------------------------------------------
private fun shareReportCsv(context: Context, fileName: String, csvContent: String) {
    try {
        val file = File(context.cacheDir, fileName)
        file.writeText(csvContent)

        val uri = FileProvider.getUriForFile(
            context,
            "com.hostel.management.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Report CSV"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
