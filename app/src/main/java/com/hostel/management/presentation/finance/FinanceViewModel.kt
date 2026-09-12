package com.hostel.management.presentation.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hostel.management.domain.model.FeeInvoice
import com.hostel.management.domain.model.Payment
import com.hostel.management.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FinanceViewModel(
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val _invoices = MutableStateFlow<List<FeeInvoice>>(emptyList())
    val invoices: StateFlow<List<FeeInvoice>> = _invoices.asStateFlow()

    private val _payments = MutableStateFlow<List<Payment>>(emptyList())
    val payments: StateFlow<List<Payment>> = _payments.asStateFlow()

    private val _financialStats = MutableStateFlow<Map<String, Any>>(emptyMap())
    val financialStats: StateFlow<Map<String, Any>> = _financialStats.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadInvoices(studentId: String? = null, monthFilter: String? = null) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            financeRepository.getFeeInvoices(studentId, monthFilter)
                .onSuccess { list ->
                    _invoices.value = list
                    _loading.value = false
                }
                .onFailure { err ->
                    _error.value = err.localizedMessage ?: "Failed to load fee invoices"
                    _loading.value = false
                }
        }
    }

    fun loadPayments(studentId: String? = null) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            financeRepository.getPayments(studentId)
                .onSuccess { list ->
                    _payments.value = list
                    _loading.value = false
                }
                .onFailure { err ->
                    _error.value = err.localizedMessage ?: "Failed to load payments"
                    _loading.value = false
                }
        }
    }

    fun loadFinancialStats() {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            financeRepository.getFinancialStats()
                .onSuccess { stats ->
                    _financialStats.value = stats
                    _loading.value = false
                }
                .onFailure { err ->
                    _error.value = err.localizedMessage ?: "Failed to load financial stats"
                    _loading.value = false
                }
        }
    }

    fun generateInvoices(month: String, defaultAmount: Double, dueDate: String, onSuccess: () -> Unit) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            financeRepository.generateMonthlyInvoices(month, defaultAmount, dueDate)
                .onSuccess {
                    _loading.value = false
                    onSuccess()
                }
                .onFailure { err ->
                    _error.value = err.localizedMessage ?: "Invoices generation failed"
                    _loading.value = false
                }
        }
    }

    fun recordPayment(
        studentId: String,
        invoiceId: String,
        amount: Double,
        paymentMethod: String,
        transactionId: String?,
        notes: String?,
        onSuccess: () -> Unit
    ) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            financeRepository.recordPayment(studentId, invoiceId, amount, paymentMethod, transactionId, notes)
                .onSuccess {
                    _loading.value = false
                    onSuccess()
                }
                .onFailure { err ->
                    _error.value = err.localizedMessage ?: "Failed to record payment"
                    _loading.value = false
                }
        }
    }

    fun recordPaymentForMonth(
        studentId: String,
        month: String,
        amount: Double,
        paymentMethod: String,
        notes: String?,
        onSuccess: () -> Unit
    ) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            val invoicesResult = financeRepository.getFeeInvoices(studentId, month)
            var invoiceId: String? = null
            
            if (invoicesResult.isSuccess) {
                val invoices = invoicesResult.getOrNull() ?: emptyList()
                val existingInvoice = invoices.find { it.billingMonth == month }
                
                if (existingInvoice != null) {
                    invoiceId = existingInvoice.id
                } else {
                    _error.value = "No invoice generated for $month. Generate invoices first."
                    _loading.value = false
                    return@launch
                }
            } else {
                _error.value = "Failed to verify invoice"
                _loading.value = false
                return@launch
            }

            if (invoiceId != null) {
                financeRepository.recordPayment(studentId, invoiceId, amount, paymentMethod, null, notes)
                    .onSuccess {
                        _loading.value = false
                        onSuccess()
                    }
                    .onFailure { err ->
                        _error.value = err.localizedMessage ?: "Payment failed"
                        _loading.value = false
                    }
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
