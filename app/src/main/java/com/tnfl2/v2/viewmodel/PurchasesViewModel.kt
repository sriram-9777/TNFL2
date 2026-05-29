package com.tnfl2.v2.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tnfl2.v2.SessionManager
import com.tnfl2.v2.network.AuthRepository
import com.tnfl2.v2.network.PurchaseItem
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

class PurchasesViewModel : ViewModel() {
    val purchases = mutableStateOf<List<PurchaseItem>>(emptyList())
    val isLoading = mutableStateOf(false)
    val error = mutableStateOf<String?>(null)

    private val repository = AuthRepository()

    val currencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        currency = Currency.getInstance("INR")
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    fun fetchPurchases(token: String) {
        viewModelScope.launch {
            isLoading.value = true
            SessionManager.incrementLoading()
            error.value = null
            try {
                val result = repository.getPurchases(token)
                if (result.isSuccess) {
                    purchases.value = result.getOrThrow().data
                } else {
                    error.value = result.exceptionOrNull()?.message ?: "Failed to fetch purchases"
                }
            } catch (e: Exception) {
                error.value = e.message ?: "An error occurred"
            } finally {
                isLoading.value = false
                SessionManager.decrementLoading()
            }
        }
    }
}
