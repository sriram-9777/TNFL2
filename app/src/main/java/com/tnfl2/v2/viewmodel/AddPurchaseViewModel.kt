package com.tnfl2.v2.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tnfl2.v2.network.AuthRepository
import com.tnfl2.v2.network.Product
import com.tnfl2.v2.network.PurchaseDraftProductItem
import com.tnfl2.v2.network.PurchaseDraftRequest
import com.tnfl2.v2.SessionManager
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

data class TempPurchaseItem(
    val sku: String,
    val openingStock: Int,
    val purchaseStock: Int,
    val stock: Int,
    val purchaseAmount: Double,
    val cases: Int
)

class AddPurchaseViewModel : ViewModel() {

    private val repository = AuthRepository()

    val products = mutableStateOf<List<Product>>(emptyList())
    val tempPurchaseList = mutableStateListOf<TempPurchaseItem>()
    val isLoading = mutableStateOf(false)
    val error = mutableStateOf<String?>(null)
    val draftSaved = mutableStateOf(false)

    // Draft specific state to pre-fill InvoiceDetailsStep
    val billNo = mutableStateOf("")
    val purchaseDate = mutableStateOf<Long?>(null)

    val currencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        currency = Currency.getInstance("INR")
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    fun fetchProducts(token: String) {
        viewModelScope.launch {
            isLoading.value = true
            SessionManager.incrementLoading()
            try {
                val result = repository.getProductMaster(token)
                if (result.isSuccess) {
                    products.value = result.getOrThrow().productList
                } else {
                    error.value = result.exceptionOrNull()?.message ?: "Failed to fetch products"
                }
            } catch (e: Exception) {
                error.value = e.message
            } finally {
                isLoading.value = false
                SessionManager.decrementLoading()
            }
        }
    }

    fun fetchPurchaseDraft(token: String) {
        viewModelScope.launch {
            isLoading.value = true
            SessionManager.incrementLoading()
            try {
                val result = repository.getPurchaseDraft(token)
                if (result.isSuccess) {
                    val drafts = result.getOrThrow().data
                    // Use the first draft if the list is not empty
                    if (!drafts.isNullOrEmpty()) {
                        val draft = drafts[0]
                        billNo.value = draft.billNo
                        purchaseDate.value = draft.purchaseDate
                        tempPurchaseList.clear()
                        tempPurchaseList.addAll(draft.productList.map {
                            TempPurchaseItem(
                                sku = it.sku,
                                openingStock = it.openingStock,
                                purchaseStock = it.purchaseStock,
                                stock = it.stock,
                                purchaseAmount = it.purchaseAmount,
                                cases = it.cases
                            )
                        })
                    }
                }
            } catch (e: Exception) {
                // Silently ignore or handle draft fetch error
                println("Draft fetch error: ${e.message}")
            } finally {
                isLoading.value = false
                SessionManager.decrementLoading()
            }
        }
    }

    fun addTempItem(item: TempPurchaseItem) {
        tempPurchaseList.add(item)
    }

    fun removeTempItem(item: TempPurchaseItem) {
        tempPurchaseList.remove(item)
    }

    fun savePurchaseDraft(token: String, billNumber: String, purchaseDate: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            isLoading.value = true
            SessionManager.incrementLoading()
            error.value = null
            draftSaved.value = false
            try {
                val totalAmount = tempPurchaseList.sumOf { it.purchaseAmount }
                val totalQuantity = tempPurchaseList.sumOf { it.purchaseStock }
                val productList = tempPurchaseList.map {
                    PurchaseDraftProductItem(
                        sku = it.sku,
                        openingStock = it.openingStock,
                        purchaseStock = it.purchaseStock,
                        stock = it.stock,
                        purchaseAmount = it.purchaseAmount,
                        cases = it.cases
                    )
                }

                val request = PurchaseDraftRequest(
                    billNo = billNumber,
                    purchaseDate = purchaseDate,
                    purchaseAmount = totalAmount.toString(),
                    totalQuantity = totalQuantity,
                    productList = productList
                )

                val result = repository.savePurchaseDraft(token, request)
                if (result.isSuccess) {
                    draftSaved.value = true
                    tempPurchaseList.clear()
                    onDone()
                } else {
                    error.value = result.exceptionOrNull()?.message ?: "Failed to save draft"
                }
            } catch (e: Exception) {
                error.value = e.message
            } finally {
                isLoading.value = false
                SessionManager.decrementLoading()
            }
        }
    }

    fun savePurchaseFinal(token: String, billNumber: String, purchaseDate: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            isLoading.value = true
            SessionManager.incrementLoading()
            error.value = null
            try {
                val totalAmount = tempPurchaseList.sumOf { it.purchaseAmount }
                val totalQuantity = tempPurchaseList.sumOf { it.purchaseStock }
                val productList = tempPurchaseList.map {
                    PurchaseDraftProductItem(
                        sku = it.sku,
                        openingStock = it.openingStock,
                        purchaseStock = it.purchaseStock,
                        stock = it.stock,
                        purchaseAmount = it.purchaseAmount,
                        cases = it.cases
                    )
                }

                val request = PurchaseDraftRequest(
                    billNo = billNumber,
                    purchaseDate = purchaseDate,
                    purchaseAmount = String.format("%.2f", totalAmount),
                    totalQuantity = totalQuantity,
                    productList = productList
                )

                val result = repository.savePurchase(token, request)
                if (result.isSuccess) {
                    val response = result.getOrThrow()
                    if (response.isSuccessful) {
                        tempPurchaseList.clear()
                        onDone()
                    } else {
                        error.value = "Failed to save purchase: ${response.code()}"
                    }
                } else {
                    error.value = result.exceptionOrNull()?.message ?: "Failed to save purchase"
                }
            } catch (e: Exception) {
                error.value = e.message
            } finally {
                isLoading.value = false
                SessionManager.decrementLoading()
            }
        }
    }
}
