package com.hostel.management.data.repository

import com.hostel.management.data.local.AppDatabase
import com.hostel.management.data.local.entity.FeeInvoiceEntity
import com.hostel.management.data.local.entity.PaymentEntity
import com.hostel.management.data.local.entity.SyncQueueEntity
import com.hostel.management.data.sync.SyncManager
import com.hostel.management.domain.model.FeeInvoice
import com.hostel.management.domain.model.Payment
import com.hostel.management.domain.repository.FinanceRepository
import com.hostel.management.di.ServiceLocator
import io.github.jan.supabase.SupabaseClient
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class FinanceRepositoryImpl(
    private val supabaseClient: SupabaseClient,
    private val db: AppDatabase,
    private val syncManager: SyncManager
) : FinanceRepository {

    override suspend fun getFeeInvoices(
        studentId: String?,
        monthFilter: String?
    ): Result<List<FeeInvoice>> = runCatching {
        var listDto = if (studentId != null) {
            db.financeDao().getInvoicesForStudent(studentId)
        } else {
            db.financeDao().getAllInvoices()
        }

        if (listDto.isEmpty()) {
            syncManager.pullCloudToLocal()
            listDto = if (studentId != null) {
                db.financeDao().getInvoicesForStudent(studentId)
            } else {
                db.financeDao().getAllInvoices()
            }
        }

        val filtered = listDto.filter { dto ->
            val matchStudent = studentId == null || dto.studentId == studentId
            val matchMonth = monthFilter == null || dto.billingMonth == monthFilter
            matchStudent && matchMonth
        }

        val studentProfiles = db.studentDao().getAllStudentProfiles().associateBy { it.id }
        val students = db.studentDao().getAllStudents().associateBy { it.id }

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
        val activeStudents = db.studentDao().getAllStudents().filter { it.hostelStatus == "Active" }

        for (student in activeStudents) {
            val newInvoiceId = UUID.randomUUID().toString()
            val entity = FeeInvoiceEntity(
                id = newInvoiceId,
                studentId = student.id,
                billingMonth = month,
                amount = defaultAmount,
                dueDate = dueDate,
                balance = defaultAmount,
                paymentStatus = "Pending"
            )

            db.financeDao().insertInvoice(entity)

            val payload = buildJsonObject {
                put("id", newInvoiceId)
                put("student_id", student.id)
                put("billing_month", month)
                put("amount", defaultAmount)
                put("due_date", dueDate)
                put("balance", defaultAmount)
                put("payment_status", "Pending")
            }.toString()

            db.syncQueueDao().enqueueSyncItem(
                SyncQueueEntity(
                    entityType = "FEE_INVOICE",
                    entityId = newInvoiceId,
                    action = "INSERT",
                    payloadJson = payload
                )
            )
        }

        syncManager.triggerAutoSyncIfEnabled()
    }

    override suspend fun recordPayment(
        studentId: String,
        invoiceId: String,
        amount: Double,
        paymentMethod: String,
        transactionId: String?,
        notes: String?
    ): Result<Unit> = runCatching {
        val accountantProfile = ServiceLocator.authRepository.getCurrentProfile().getOrNull()

        val invoice = db.financeDao().getInvoiceById(invoiceId)
            ?: throw IllegalStateException("Invoice not found locally")

        val newPaid = invoice.amountPaid + amount
        val newBalance = invoice.balance - amount
        val nowString = SimpleDateFormat("yyyy-MM-dd HH:mm:ssZZZZZ", Locale.US).format(Date())

        val newStatus = when {
            newBalance <= 0 -> "Paid"
            newPaid > 0 -> "Partially Paid"
            else -> "Pending"
        }

        val updatedInvoice = invoice.copy(
            amountPaid = newPaid,
            balance = newBalance,
            paymentStatus = newStatus,
            paymentDate = if (newStatus == "Paid") nowString else invoice.paymentDate
        )

        db.financeDao().insertInvoice(updatedInvoice)

        val newPaymentId = UUID.randomUUID().toString()
        val paymentEntity = PaymentEntity(
            id = newPaymentId,
            studentId = studentId,
            feeInvoiceId = invoiceId,
            amount = amount,
            paymentDate = nowString,
            paymentMethod = paymentMethod,
            transactionId = transactionId,
            recordedBy = accountantProfile?.id,
            notes = notes
        )

        db.financeDao().insertPayment(paymentEntity)

        // Queue Sync Queue items
        val invoicePayload = buildJsonObject {
            put("amount_paid", newPaid)
            put("balance", newBalance)
            put("payment_status", newStatus)
            if (newStatus == "Paid") put("payment_date", nowString)
        }.toString()

        db.syncQueueDao().enqueueSyncItem(
            SyncQueueEntity(
                entityType = "FEE_INVOICE",
                entityId = invoiceId,
                action = "UPDATE",
                payloadJson = invoicePayload
            )
        )

        val paymentPayload = buildJsonObject {
            put("id", newPaymentId)
            put("student_id", studentId)
            put("fee_invoice_id", invoiceId)
            put("amount", amount)
            put("payment_date", nowString)
            put("payment_method", paymentMethod)
            if (transactionId != null) put("transaction_id", transactionId)
            if (accountantProfile?.id != null) put("recorded_by", accountantProfile.id)
            if (notes != null) put("notes", notes)
        }.toString()

        db.syncQueueDao().enqueueSyncItem(
            SyncQueueEntity(
                entityType = "PAYMENT",
                entityId = newPaymentId,
                action = "INSERT",
                payloadJson = paymentPayload
            )
        )

        syncManager.triggerAutoSyncIfEnabled()
    }

    override suspend fun getPayments(studentId: String?): Result<List<Payment>> = runCatching {
        var listDto = if (studentId != null) {
            db.financeDao().getPaymentsForStudent(studentId)
        } else {
            db.financeDao().getAllPayments()
        }

        if (listDto.isEmpty()) {
            syncManager.pullCloudToLocal()
            listDto = if (studentId != null) {
                db.financeDao().getPaymentsForStudent(studentId)
            } else {
                db.financeDao().getAllPayments()
            }
        }

        val studentProfiles = db.studentDao().getAllStudentProfiles().associateBy { it.id }

        listDto.map { dto ->
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
        val invoices = db.financeDao().getAllInvoices()
        val payments = db.financeDao().getAllPayments()

        val totalBilled = invoices.sumOf { it.amount + it.lateFee - it.discount }
        val totalCollected = payments.sumOf { it.amount }
        val totalPending = invoices.filter { it.paymentStatus == "Pending" || it.paymentStatus == "Partially Paid" }.sumOf { it.balance }
        val totalOverdue = invoices.filter { it.paymentStatus == "Overdue" }.sumOf { it.balance }

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
