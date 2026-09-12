package com.hostel.management.presentation.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hostel.management.domain.model.*
import com.hostel.management.presentation.auth.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeesListScreen(
    financeViewModel: FinanceViewModel,
    authViewModel: AuthViewModel,
    onNavigateToRecordPayment: (studentId: String, invoiceId: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val invoices by financeViewModel.invoices.collectAsState()
    val loading by financeViewModel.loading.collectAsState()
    val userProfile by authViewModel.currentProfile.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showGenerateBillingDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userProfile) {
        if (userProfile?.role == UserRole.STUDENT) {
            financeViewModel.loadInvoices(studentId = userProfile?.id)
        } else {
            financeViewModel.loadInvoices()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mess Dues & Invoices") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (userProfile?.role == UserRole.SUPER_ADMIN || userProfile?.role == UserRole.ACCOUNTANT) {
                        IconButton(onClick = { showGenerateBillingDialog = true }) {
                            Icon(Icons.Default.PostAdd, contentDescription = "Generate Bills")
                        }
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
            // Search Bar for admins
            if (userProfile?.role != UserRole.STUDENT) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by student name...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
            }

            if (loading && invoices.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val filteredInvoices = invoices.filter {
                    userProfile?.role == UserRole.STUDENT ||
                    searchQuery.isBlank() ||
                    it.studentName?.contains(searchQuery, ignoreCase = true) == true
                }

                if (filteredInvoices.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No fee invoices found.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredInvoices) { invoice ->
                            val statusColor = when (invoice.paymentStatus) {
                                "Paid" -> Color(0xFF4CAF50)
                                "Partially Paid" -> Color(0xFF2196F3)
                                "Pending" -> Color(0xFFFF9800)
                                "Overdue" -> Color(0xFFF44336)
                                else -> Color(0xFF9E9E9E)
                            }

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
                                        Column {
                                            Text(
                                                text = if (userProfile?.role == UserRole.STUDENT) "Billing: ${invoice.billingMonth}" else invoice.studentName ?: "Student",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                            if (userProfile?.role != UserRole.STUDENT) {
                                                Text(
                                                    text = "Month: ${invoice.billingMonth} | ID: ${invoice.studentRollNumber}",
                                                    fontSize = 12.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                        Text(
                                            text = invoice.paymentStatus.uppercase(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = statusColor,
                                            modifier = Modifier
                                                .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Total: ₹${invoice.amount}", fontSize = 12.sp)
                                            Text("Paid: ₹${invoice.amountPaid}", fontSize = 12.sp, color = Color.Gray)
                                            Text("Balance: ₹${invoice.balance}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Due Date: ${invoice.dueDate}", fontSize = 12.sp, color = Color.Gray)
                                            if (invoice.paymentStatus != "Paid" && invoice.paymentStatus != "Waived" &&
                                                (userProfile?.role == UserRole.SUPER_ADMIN || userProfile?.role == UserRole.ACCOUNTANT)) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Button(
                                                    onClick = { onNavigateToRecordPayment(invoice.studentId, invoice.id) },
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(30.dp)
                                                ) {
                                                    Text("Record Payment", fontSize = 11.sp)
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
        }

        // Generate Billing Month Dialog
        if (showGenerateBillingDialog) {
            var billingMonth by remember { mutableStateOf("2026-08") }
            var defaultAmount by remember { mutableStateOf("2500.00") }
            var dueDate by remember { mutableStateOf("2026-08-15") }

            AlertDialog(
                onDismissRequest = { showGenerateBillingDialog = false },
                title = { Text("Generate Monthly Invoices") },
                confirmButton = {
                    Button(
                        onClick = {
                            financeViewModel.generateInvoices(
                                month = billingMonth,
                                defaultAmount = defaultAmount.toDoubleOrNull() ?: 2500.0,
                                dueDate = dueDate
                            ) {
                                financeViewModel.loadInvoices()
                                showGenerateBillingDialog = false
                            }
                        }
                    ) {
                        Text("Generate")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showGenerateBillingDialog = false }) {
                        Text("Cancel")
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = billingMonth,
                            onValueChange = { billingMonth = it },
                            label = { Text("Billing Month (YYYY-MM)") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        )
                        OutlinedTextField(
                            value = defaultAmount,
                            onValueChange = { defaultAmount = it },
                            label = { Text("Default Amount (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        )
                        OutlinedTextField(
                            value = dueDate,
                            onValueChange = { dueDate = it },
                            label = { Text("Due Date (YYYY-MM-DD)") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentScreen(
    studentId: String,
    invoiceId: String,
    financeViewModel: FinanceViewModel,
    onNavigateBack: () -> Unit
) {
    var amountToPay by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var transactionId by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val invoices by financeViewModel.invoices.collectAsState()
    val loading by financeViewModel.loading.collectAsState()
    val error by financeViewModel.error.collectAsState()

    val targetInvoice = invoices.firstOrNull { it.id == invoiceId }

    LaunchedEffect(targetInvoice) {
        if (targetInvoice != null && amountToPay.isEmpty()) {
            amountToPay = targetInvoice.balance.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Record Payment") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            if (targetInvoice == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Invoice not found.")
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Invoice Summary", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Student Name: ${targetInvoice.studentName ?: "Unknown"}")
                        Text("Roll Number: ${targetInvoice.studentRollNumber ?: "-"}")
                        Text("Billing Month: ${targetInvoice.billingMonth}")
                        Text("Pending Dues: ₹${targetInvoice.balance}", fontWeight = FontWeight.Bold, color = Color(0xFFF44336))
                    }
                }

                Text("Payment Information", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountToPay,
                    onValueChange = { amountToPay = it },
                    label = { Text("Amount Paid (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )

                // Payment Method Dropdown
                var paymentMethodExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = paymentMethodExpanded,
                    onExpandedChange = { paymentMethodExpanded = !paymentMethodExpanded }
                ) {
                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Method") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentMethodExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = paymentMethodExpanded,
                        onDismissRequest = { paymentMethodExpanded = false }
                    ) {
                        val methods = listOf("Cash", "UPI", "Bank Transfer", "Card", "Online", "Other")
                        methods.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m) },
                                onClick = {
                                    paymentMethod = m
                                    paymentMethodExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = transactionId,
                    onValueChange = { transactionId = it },
                    label = { Text("Transaction / Reference ID") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Comments") },
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                )

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
                        val amount = amountToPay.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            financeViewModel.recordPayment(
                                studentId = studentId,
                                invoiceId = invoiceId,
                                amount = amount,
                                paymentMethod = paymentMethod,
                                transactionId = transactionId.ifBlank { null },
                                notes = notes.ifBlank { null }
                            ) {
                                onNavigateBack()
                            }
                        }
                    },
                    enabled = amountToPay.toDoubleOrNull() != null && !loading,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    if (loading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Record Transaction", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
