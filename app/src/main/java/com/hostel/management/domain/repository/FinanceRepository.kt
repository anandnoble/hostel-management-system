package com.hostel.management.domain.repository

import com.hostel.management.domain.model.FeeInvoice
import com.hostel.management.domain.model.Payment

interface FinanceRepository {
    suspend fun getFeeInvoices(studentId: String? = null, monthFilter: String? = null): Result<List<FeeInvoice>>
    suspend fun generateMonthlyInvoices(month: String, defaultAmount: Double, dueDate: String): Result<Unit>
    suspend fun recordPayment(
        studentId: String,
        invoiceId: String,
        amount: Double,
        paymentMethod: String,
        transactionId: String?,
        notes: String?
    ): Result<Unit>
    suspend fun getPayments(studentId: String? = null): Result<List<Payment>>
    suspend fun getFinancialStats(): Result<Map<String, Any>>
}
