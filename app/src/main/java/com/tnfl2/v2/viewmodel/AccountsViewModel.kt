package com.tnfl2.v2.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tnfl2.v2.SessionManager
import com.tnfl2.v2.network.AuthRepository
import com.tnfl2.v2.network.SaleItem
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Holds the calculated financial data for the Accounts screen.
 */
data class AccountSummary(
    val totalSaleAmount: Double = 0.0,
    val totalProfit: Double = 0.0,
    val totalInvestment: Double = 0.0,
    val salesDetails: List<SaleItem> = emptyList()
)

class AccountsViewModel : ViewModel() {

    // UI State properties
    val summary = mutableStateOf<AccountSummary?>(null)
    val isLoading = mutableStateOf(false)
    val error = mutableStateOf<String?>(null)

    private val repository = AuthRepository()

    // Formatter for currency, can be shared
    val currencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        currency = Currency.getInstance("INR")
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    /**
     * Fetches all necessary data for the selected date range and calculates the summary.
     */
    fun fetchAccountSummary(token: String, startDate: Long, endDate: Long) {
        viewModelScope.launch {
            isLoading.value = true
            SessionManager.incrementLoading()
            error.value = null
            summary.value = null // Clear previous summary

            try {
                // 1. Fetch Sales Data for the date range
                val salesResult = repository.getSales(token, startDate, endDate)
                val salesResponse = salesResult.getOrThrow()
                val salesList = salesResponse.data
                
                val totalSaleAmount = salesList.sumOf { it.totalSalesAmount }
                // Calculate total profit: Sum of (profitAmount + kitchenSales - totalExpensesAmount)
                val totalProfit = salesList.sumOf { it.profitAmount + it.kitchenSales - it.totalExpensesAmount }

                // 2. Fetch Product Master to calculate total investment
                val productMasterResult = repository.getProductMaster(token)
                val productMaster = productMasterResult.getOrThrow()
                val totalInvestment = productMaster.productList.sumOf { product ->
                    (product.stock * product.purchasePrice)
                }

                // 3. Update the UI state with the calculated summary
                summary.value = AccountSummary(
                    totalSaleAmount = totalSaleAmount,
                    totalProfit = totalProfit,
                    totalInvestment = totalInvestment,
                    salesDetails = salesList
                )

            } catch (e: Exception) {
                error.value = "Failed to fetch data: ${e.message}"
            } finally {
                isLoading.value = false
                SessionManager.decrementLoading()
            }
        }
    }
}
