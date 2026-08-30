package com.hostel.management.data.repository

import com.hostel.management.data.dto.FeeInvoiceDto
import com.hostel.management.data.dto.PaymentDto
import com.hostel.management.data.dto.ProfileDto
import com.hostel.management.data.dto.StudentDto
import com.hostel.management.domain.model.FeeInvoice
import com.hostel.management.domain.model.Payment
import com.hostel.management.domain.repository.FinanceRepository
import com.hostel.management.di.ServiceLocator
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FinanceRepositoryImpl(
    private val supabaseClient: SupabaseClient
) : FinanceRepository {

    override suspend fun getFeeInvoices(
        studentId: String?,
        monthFilter: String?
    ): Result<List<FeeInvoice>> = runCatching {
        var query = supabaseClient.postgrest.from("fee_invoices").select()
        
        val listDto = query.decodeList<FeeInvoiceDto>()
        if (listDto.isEmpty()) return Result.success(emptyList())

        val filtered = listDto.filter { dto ->
            val matchStudent = studentId == null || dto.studentId == studentId
            val matchMonth = monthFilter == null || dto.billingMonth == monthFilter
            matchStudent && matchMonth
        }

        val studentProfiles = supabaseClient.postgrest.from("profiles").select().decodeList<ProfileDto>().associateBy { it.id }
        val students = supabaseClient.postgrest.from("students").select().decodeList<StudentDto>().associateBy { it.id }

        filtered.map { dto ->
            val profile = studentProfiles[dto.studentId]
            val student = students[dto.studentId]
            FeeInvoice(
                id = dto.id,
                studentId = dto.studentId,
                studentName = profile?.fullName,
                studentRollNumber = student?.studentIdNumber,
                billingMonth = dto.billingMonth,
                amount = dto.amount,
                dueDate = dto.dueDate,
                discount = dto.discount,
                lateFee = dto.lateFee,
                amountPaid = dto.amountPaid,
                balance = dto.balance,
                paymentStatus = dto.paymentStatus,
                paymentDate = dto.paymentDate
            )
        }
    }

    override suspend fun generateMonthlyInvoices(
        month: String,
        defaultAmount: Double,
        dueDate: String
    ): Result<Unit> = runCatching {
        // Fetch all active students in the organization
        val activeStudents = supabaseClient.postgrest.from("students").select {
            filter {
                eq("hostel_status", "Active")
            }
        }.decodeList<StudentDto>()

        for (student in activeStudents) {
            try {
                // Insert a fee invoice
                supabaseClient.postgrest.from("fee_invoices").insert(
                    buildJsonObject {
                        put("student_id", student.id)
                        put("billing_month", month)
                        put("amount", defaultAmount)
                        put("due_date", dueDate)
                        put("balance", defaultAmount)
                        put("payment_status", "Pending")
                    }
                )
            } catch (e: Exception) {
                // If student invoice already generated, skip (handles unique constraint conflict gracefully)
                e.printStackTrace()
            }
        }
    }

    override suspend fun recordPayment(
        studentId: String,
        invoiceId: String,
        amount: Double,
        paymentMethod: String,
        transactionId: String?,
        notes: String?
    ): Result<Unit> = runCatching {
        val accountantProfile = ServiceLocator.authRepository.getCurrentProfile().getOrThrow()
            ?: throw IllegalStateException("User not logged in")

        // 1. Fetch current invoice
        val invoice = supabaseClient.postgrest.from("fee_invoices").select {
            filter { eq("id", invoiceId) }
        }.decodeSingle<FeeInvoiceDto>()

        val newPaid = invoice.amountPaid + amount
        val newBalance = invoice.balance - amount
        val nowString = SimpleDateFormat("yyyy-MM-dd HH:mm:ssZZZZZ", Locale.US).format(Date())

        val newStatus = when {
            newBalance <= 0 -> "Paid"
            newPaid > 0 -> "Partially Paid"
            else -> "Pending"
        }

        // 2. Update invoice
        supabaseClient.postgrest.from("fee_invoices").update(
            buildJsonObject {
                put("amount_paid", newPaid)
                put("balance", newBalance)
                put("payment_status", newStatus)
                if (newStatus == "Paid") put("payment_date", nowString)
            }
        ) {
            filter { eq("id", invoiceId) }
        }

        // 3. Create payment record
        supabaseClient.postgrest.from("payments").insert(
            buildJsonObject {
                put("student_id", studentId)
                put("fee_invoice_id", invoiceId)
                put("amount", amount)
                put("payment_method", paymentMethod)
                if (transactionId != null) put("transaction_id", transactionId)
                put("recorded_by", accountantProfile.id)
                if (notes != null) put("notes", notes)
            }
        )
    }

    override suspend fun getPayments(studentId: String?): Result<List<Payment>> = runCatching {
        val listDto = supabaseClient.postgrest.from("payments").select().decodeList<PaymentDto>()
        
        val filtered = if (studentId != null) {
            listDto.filter { it.studentId == studentId }
        } else {
            listDto
        }

        val studentProfiles = supabaseClient.postgrest.from("profiles").select().decodeList<ProfileDto>().associateBy { it.id }

        filtered.map { dto ->
            val profile = studentProfiles[dto.studentId]
            Payment(
                id = dto.id,
                studentId = dto.studentId,
                studentName = profile?.fullName,
                feeInvoiceId = dto.feeInvoiceId,
                amount = dto.amount,
                paymentDate = dto.paymentDate,
                paymentMethod = dto.paymentMethod,
                transactionId = dto.transactionId,
                recordedBy = dto.recordedBy,
                notes = dto.notes
            )
        }
    }

    override suspend fun getFinancialStats(): Result<Map<String, Any>> = runCatching {
        val invoices = supabaseClient.postgrest.from("fee_invoices").select().decodeList<FeeInvoiceDto>()
        val payments = supabaseClient.postgrest.from("payments").select().decodeList<PaymentDto>()

        val totalBilled = invoices.sumOf { it.amount + it.lateFee - it.discount }
        val totalCollected = payments.sumOf { it.amount }
        val totalPending = invoices.filter { it.paymentStatus == "Pending" || it.paymentStatus == "Partially Paid" }.sumOf { it.balance }
        val totalOverdue = invoices.filter { it.paymentStatus == "Overdue" }.sumOf { it.balance }

        // Fetch today's payments
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val todayCollected = payments.filter { it.paymentDate.startsWith(todayStr) }.sumOf { it.amount }

        mapOf(
            "totalBilled" to totalBilled,
            "totalCollected" to totalCollected,
            "totalPending" to totalPending,
            "totalOverdue" to totalOverdue,
            "todayCollected" to todayCollected
        )
    }
}
