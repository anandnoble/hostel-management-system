package com.hostel.management.domain.model

data class FeeInvoice(
    val id: String,
    val studentId: String,
    val studentName: String? = null,
    val studentRollNumber: String? = null,
    val billingMonth: String, // "YYYY-MM"
    val amount: Double,
    val dueDate: String,
    val discount: Double = 0.0,
    val lateFee: Double = 0.0,
    val amountPaid: Double = 0.0,
    val balance: Double,
    val paymentStatus: String, // "Paid", "Partially Paid", "Pending", "Overdue", "Waived"
    val paymentDate: String? = null
)

data class Payment(
    val id: String,
    val studentId: String,
    val studentName: String? = null,
    val feeInvoiceId: String,
    val amount: Double,
    val paymentDate: String,
    val paymentMethod: String, // "Cash", "UPI", "Bank Transfer", "Card", "Online", "Other"
    val transactionId: String? = null,
    val recordedBy: String? = null,
    val notes: String? = null
)
