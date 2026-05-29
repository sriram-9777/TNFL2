package com.tnfl2.v2

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tnfl2.v2.network.*
import com.tnfl2.v2.ui.theme.GradientBackground
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSaleScreen(
    token: String,
    initialSale: EditSaleItem? = null,
    onCancel: () -> Unit,
    onSaleConfirmed: () -> Unit
) {
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    val authRepository = remember { AuthRepository() }
    val scope = rememberCoroutineScope()
    val closingStocks = remember { mutableStateMapOf<String, String>() }
    var totalSaleAmount by remember { mutableStateOf(0.0) }
    var currentScreen by remember { mutableStateOf("Sales") }
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    // Wire initial loading to GlobalLoader
    LaunchedEffect(Unit) { SessionManager.incrementLoading() }

    fun calculateTotalSaleAmount(productList: List<Product>) {
        val expense = SessionManager.expenseAmount.toDoubleOrNull() ?: 0.0
        totalSaleAmount = productList.sumOf { product ->
            val closingStockStr = closingStocks[product.sku] ?: ""
            val closingStock = closingStockStr.toDoubleOrNull() ?: 0.0
            val openingStock = product.openingStock.toDouble()
            val sales = openingStock - closingStock
            if (sales > 0 && closingStockStr.isNotEmpty()) sales * product.salePrice else 0.0
        } - expense
    }

    LaunchedEffect(token, initialSale) {
        if (token.isNotEmpty()) {
            scope.launch {
                try {
                    val productMasterResult = authRepository.getProductMaster(token)
                    
                    productMasterResult.fold(
                        onSuccess = { master ->
                            if (initialSale != null) {
                                // EDIT MODE: Prefill from initialSale
                                val draftSkus = initialSale.productList?.map { it.sku }?.toSet() ?: emptySet()
                                products = master.productList.filter { it.sku in draftSkus }
                                
                                initialSale.productList?.forEach { draftProd ->
                                    closingStocks[draftProd.sku] = draftProd.closingStock.toString()
                                }
                                SessionManager.expenseAmount = initialSale.totalExpensesAmount.toString()
                                calculateTotalSaleAmount(products)
                                Toast.makeText(context, "Sale loaded for editing", Toast.LENGTH_SHORT).show()
                            } else {
                                // NEW SALE MODE: Check for draft
                                val salesDraftResult = authRepository.getSalesDraft(token)
                                salesDraftResult.fold(
                                    onSuccess = { draftResponse ->
                                        val draft = draftResponse.data
                                        if (draft != null) {
                                            val draftSkus = draft.productList.map { it.sku }.toSet()
                                            products = master.productList.filter { it.sku in draftSkus }
                                            draft.productList.forEach { draftProd ->
                                                closingStocks[draftProd.sku] = draftProd.closingStock.toString()
                                            }
                                            SessionManager.expenseAmount = draft.totalExpensesAmount.toString()
                                            calculateTotalSaleAmount(products)
                                            Toast.makeText(context, "Draft loaded from server", Toast.LENGTH_SHORT).show()
                                        } else {
                                            // NEW SALE MODE: No draft, try fetching last confirmed sale
                                            products = master.productList
                                            val lastSaleResult = authRepository.getSales(token, 0, System.currentTimeMillis() / 1000)
                                            lastSaleResult.fold(
                                                onSuccess = { salesResponse ->
                                                    val lastSaleSummary = salesResponse.data.lastOrNull()
                                                    if (lastSaleSummary != null) {
                                                        val lastSaleId = lastSaleSummary.id.toString()
                                                        scope.launch {
                                                            authRepository.getSaleById(token, lastSaleId).fold(
                                                                onSuccess = { detailedSale ->
                                                                    val lastProductMap = detailedSale.data.productList.associateBy { it.sku }
                                                                    products.forEach { product ->
                                                                        val lastProd = lastProductMap[product.sku]
                                                                        if (lastProd != null) {
                                                                            val lastSalesQty = lastProd.sales
                                                                            val suggestedClosing = (product.openingStock - lastSalesQty).coerceAtLeast(0)
                                                                            closingStocks[product.sku] = suggestedClosing.toString()
                                                                        } else if (!closingStocks.containsKey(product.sku)) {
                                                                            closingStocks[product.sku] = SessionManager.draftClosingStocks[product.sku] ?: ""
                                                                        }
                                                                    }
                                                                    calculateTotalSaleAmount(products)
                                                                    Toast.makeText(context, "Last sales values applied", Toast.LENGTH_SHORT).show()
                                                                },
                                                                onFailure = {
                                                                    // Fallback to draft or empty
                                                                    products.forEach { product ->
                                                                        if (!closingStocks.containsKey(product.sku)) {
                                                                            closingStocks[product.sku] = SessionManager.draftClosingStocks[product.sku] ?: ""
                                                                        }
                                                                    }
                                                                    calculateTotalSaleAmount(products)
                                                                }
                                                            )
                                                        }
                                                    } else {
                                                        // No previous sales found
                                                        products.forEach { product ->
                                                            if (!closingStocks.containsKey(product.sku)) {
                                                                closingStocks[product.sku] = SessionManager.draftClosingStocks[product.sku] ?: ""
                                                            }
                                                        }
                                                        calculateTotalSaleAmount(products)
                                                    }
                                                },
                                                onFailure = {
                                                    // Fallback to draft or empty
                                                    products.forEach { product ->
                                                        if (!closingStocks.containsKey(product.sku)) {
                                                            closingStocks[product.sku] = SessionManager.draftClosingStocks[product.sku] ?: ""
                                                        }
                                                    }
                                                    calculateTotalSaleAmount(products)
                                                }
                                            )
                                        }
                                    },
                                    onFailure = {
                                        products = master.productList
                                        products.forEach { product ->
                                            closingStocks[product.sku] = SessionManager.draftClosingStocks[product.sku] ?: ""
                                        }
                                        calculateTotalSaleAmount(products)
                                    }
                                )
                            }
                        },
                        onFailure = { e ->
                            Toast.makeText(context, "Failed to fetch products: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    )
                } finally {
                    isLoading = false
                    SessionManager.decrementLoading()
                }
            }
        }
    }

    fun confirmSale(isFinal: Boolean = false) {
        val productList = products.mapNotNull { product ->
            val closingStockString = closingStocks[product.sku]
            if (closingStockString.isNullOrEmpty()) return@mapNotNull null

            val closingStock = closingStockString.toIntOrNull() ?: 0
            val sales = product.openingStock - closingStock
            if (sales <= 0) return@mapNotNull null

            DraftProduct(
                sku = product.sku,
                openingStock = product.openingStock,
                purchaseStock = 0,
                stock = product.openingStock,
                closingStock = closingStock,
                sales = sales,
                salePrice = product.salePrice,
                totalSaleAmount = sales * product.salePrice,
                category = product.category
            )
        }

        if (productList.isEmpty()) {
            Toast.makeText(context, "No items with valid sales.", Toast.LENGTH_LONG).show()
            return
        }

        val request = ConfirmSaleRequest(
            productList = productList,
            expenseList = emptyList(),
            payments = emptyMap(),
            totalSalesAmount = totalSaleAmount,
            totalExpensesAmount = SessionManager.expenseAmount.toDoubleOrNull() ?: 0.0,
            totalDigitalAmount = 0.0,
            finalCashSettlement = totalSaleAmount,
            saleDate = initialSale?.timeCreatedAt ?: (Date().time / 1000),
            openingPettyCash = 5000,
            kitchenSales = 0,
            cashInHand = 0,
            closingPettyCash = null,
            diffSettlement = 0,
            isStoreRoomAvailable = false,
            id = initialSale?.id?.toString()
        )

        scope.launch {
            if (!isFinal) {
                authRepository.saveDraft(token, request).fold(
                    onSuccess = {
                        Toast.makeText(context, "Draft Saved Successfully!", Toast.LENGTH_SHORT).show()
                        SessionManager.clearDraft()
                        onSaleConfirmed()
                    },
                    onFailure = { e ->
                        Toast.makeText(context, "Draft Save Failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                )
            } else {
                if (initialSale != null) {
                    authRepository.updateSale(token, request).fold(
                        onSuccess = {
                            Toast.makeText(context, "Sale Updated Successfully!", Toast.LENGTH_SHORT).show()
                            SessionManager.clearDraft()
                            onSaleConfirmed()
                        },
                        onFailure = { e ->
                            Toast.makeText(context, "Sale Update Failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    )
                } else {
                    authRepository.confirmSale(token, request).fold(
                        onSuccess = {
                            Toast.makeText(context, "Sale Confirmed Successfully!", Toast.LENGTH_SHORT).show()
                            SessionManager.clearDraft()
                            onSaleConfirmed()
                        },
                        onFailure = { e ->
                            Toast.makeText(context, "Sale Confirmation Failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }
        }
    }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            when (currentScreen) {
                    "Sales" -> {
                        Text("Add Sale", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            item {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    TableCell(text = "SKU", weight = 2f, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                    TableCell(text = "Opening", weight = 1f, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                    TableCell(text = "Closing", weight = 1f, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                    TableCell(text = "Price", weight = 1f, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                            items(products) { product ->
                                ProductRow(
                                    product = product,
                                    closingStock = closingStocks[product.sku] ?: "",
                                    onClosingStockChange = { newValue ->
                                        closingStocks[product.sku] = newValue
                                        calculateTotalSaleAmount(products)
                                    }
                                )
                                HorizontalDivider(color = Color.White.copy(alpha = 0.5f))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Total Sale Amount: ${NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(totalSaleAmount)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(onClick = onCancel, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)) { Text("Cancel") }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = { confirmSale(false) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)) { Text("Save Draft") }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = { currentScreen = "Expenses" }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)) { Text("Next") }
                        }
                    }
                    "Expenses" -> {
                        ExpensesScreen(
                            totalSaleAmount = totalSaleAmount,
                            expenseReason = SessionManager.expenseReason,
                            onExpenseReasonChange = { SessionManager.expenseReason = it },
                            expenseAmount = SessionManager.expenseAmount,
                            onExpenseAmountChange = {
                                SessionManager.expenseAmount = it
                                calculateTotalSaleAmount(products)
                            },
                            onNext = { currentScreen = "Final Settlement" },
                            onBack = { currentScreen = "Sales" },
                            onCancel = onCancel,
                            onSave = { confirmSale(false) }
                        )
                    }
                    "Final Settlement" -> {
                        FinalSettlementScreen(
                            totalSaleAmount = totalSaleAmount,
                            onBack = { currentScreen = "Expenses" },
                            onCancel = onCancel,
                            onSave = { confirmSale(true) }
                        )
                    }
                }
            }
        }
    }

@Composable
fun ProductRow(
    product: Product,
    closingStock: String,
    onClosingStockChange: (String) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TableCell(text = product.sku, weight = 2f, color = MaterialTheme.colorScheme.onBackground)
        TableCell(text = product.openingStock.toString(), weight = 1f, color = MaterialTheme.colorScheme.onBackground)
        TextField(
            value = closingStock,
            onValueChange = onClosingStockChange,
            modifier = Modifier.weight(1f),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )
        TableCell(text = product.salePrice.toString(), weight = 1f, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
fun RowScope.TableCell(
    text: String,
    weight: Float,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = Color.Unspecified
) {
    Text(
        text = text,
        modifier = Modifier.weight(weight).padding(4.dp),
        fontWeight = fontWeight,
        style = MaterialTheme.typography.bodyMedium,
        color = color
    )
}

@Composable
fun ExpensesScreen(
    totalSaleAmount: Double,
    expenseReason: String,
    onExpenseReasonChange: (String) -> Unit,
    expenseAmount: String,
    onExpenseAmountChange: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Expenses", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = expenseReason,
            onValueChange = onExpenseReasonChange,
            label = { Text("Reason for expense") },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = expenseAmount,
            onValueChange = onExpenseAmountChange,
            label = { Text("Amount for this expense") },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "Total Sale Amount: ${NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(totalSaleAmount)}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(onClick = onCancel, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)) { Text("Cancel") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)) { Text("Back") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onSave, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)) { Text("Save Draft") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onNext, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)) { Text("Next") }
        }
    }
}

@Composable
fun FinalSettlementScreen(totalSaleAmount: Double, onBack: () -> Unit, onCancel: () -> Unit, onSave: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Final Settlement", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Total Sale Amount: ${NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(totalSaleAmount)}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.weight(1f))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(onClick = onCancel, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)) { Text("Cancel") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)) { Text("Back") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onSave, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)) { Text("Confirm") }
        }
    }
}
