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
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

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
    var grossSaleAmount by remember { mutableStateOf(0.0) }
    var totalSaleAmount by remember { mutableStateOf(0.0) }
    var currentScreen by remember { mutableStateOf("Sales") }
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    // Wire initial loading to GlobalLoader
    LaunchedEffect(Unit) { SessionManager.incrementLoading() }

    fun calculateTotalSaleAmount(productList: List<Product>) {
        val expense = SessionManager.expenseAmount.toDoubleOrNull() ?: 0.0
        grossSaleAmount = productList.sumOf { product ->
            val closingStockStr = closingStocks[product.sku] ?: ""
            val closingStock = closingStockStr.toDoubleOrNull() ?: 0.0
            val openingStock = product.openingStock.toDouble()
            val sales = openingStock - closingStock
            if (sales > 0 && closingStockStr.isNotEmpty()) sales * product.salePrice else 0.0
        }
        totalSaleAmount = grossSaleAmount - expense
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

        val expenseTotal = SessionManager.expenseAmount.toDoubleOrNull() ?: 0.0
        val request = ConfirmSaleRequest(
            productList = productList,
            expenseList = emptyList(),
            payments = emptyMap(),
            totalSalesAmount = grossSaleAmount,
            totalExpensesAmount = expenseTotal,
            totalDigitalAmount = 0.0,
            finalCashSettlement = grossSaleAmount - expenseTotal,
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
        Column(modifier = Modifier.fillMaxSize()) {
            val tabs = listOf("Sales", "Expenses", "Final Settlement")
            val selectedTabIndex = tabs.indexOf(currentScreen).coerceAtLeast(0)

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTabIndex == index
                    Tab(
                        selected = isSelected,
                        onClick = { currentScreen = title },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            when (currentScreen) {
                    "Sales" -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                            ) {
                                item {
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                        Text("SKU", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                        Text("Opening", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                        Text("Closing", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                        Text("Price", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                }
                            }
                            // Bottom Action Bar
                            Surface(
                                shadowElevation = 16.dp,
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Total Amount", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                        Text(
                                            text = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(totalSaleAmount),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(onClick = { confirmSale(false) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text("Save Draft") }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(onClick = { currentScreen = "Expenses" }, modifier = Modifier.weight(1f)) { Text("Next") }
                                    }
                                }
                            }
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
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = product.sku,
            modifier = Modifier.weight(2f).padding(end = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Text(
            text = product.openingStock.toString(),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        OutlinedTextField(
            value = closingStock,
            onValueChange = onClosingStockChange,
            modifier = Modifier.weight(1.2f).height(56.dp),
            textStyle = LocalTextStyle.current.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = FontWeight.Bold),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        Text(
            text = product.salePrice.toString(),
            modifier = Modifier.weight(1f).padding(start = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
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
        Column(modifier = Modifier.weight(1f).padding(16.dp)) {
            Text("Expenses", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = expenseReason,
                onValueChange = onExpenseReasonChange,
                label = { Text("Reason for expense") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = expenseAmount,
                onValueChange = onExpenseAmountChange,
                label = { Text("Amount for this expense") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }
        // Bottom Action Bar
        Surface(
            shadowElevation = 16.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Total Amount", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                    Text(
                        text = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(totalSaleAmount),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(4.dp))
                    OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(onClick = onSave, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text("Save", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(onClick = onNext, modifier = Modifier.weight(1f)) { Text("Next") }
                }
            }
        }
    }
}

@Composable
fun FinalSettlementScreen(totalSaleAmount: Double, onBack: () -> Unit, onCancel: () -> Unit, onSave: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f).padding(16.dp)) {
            Text("Final Settlement", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Ready to confirm?", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(totalSaleAmount),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        // Bottom Action Bar
        Surface(
            shadowElevation = 16.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Total Amount", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                    Text(
                        text = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(totalSaleAmount),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onSave, modifier = Modifier.weight(1.5f)) { Text("Confirm") }
                }
            }
        }
    }
}
