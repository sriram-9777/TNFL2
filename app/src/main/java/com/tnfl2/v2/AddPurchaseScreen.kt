package com.tnfl2.v2

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tnfl2.v2.network.Product
import com.tnfl2.v2.ui.theme.*
import com.tnfl2.v2.viewmodel.AddPurchaseViewModel
import com.tnfl2.v2.viewmodel.TempPurchaseItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPurchaseScreen(token: String, onNavigateBack: () -> Unit) {
    val viewModel: AddPurchaseViewModel = viewModel()
    var currentStep by remember { mutableIntStateOf(1) } // 1: Add Items, 2: Invoice Details
    val context = LocalContext.current

    LaunchedEffect(token) {
        viewModel.fetchProducts(token)
        viewModel.fetchPurchaseDraft(token)
    }

    // Observe error state
    LaunchedEffect(viewModel.error.value) {
        viewModel.error.value?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    GradientBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (currentStep == 1) "Add Purchase Items" else "Invoice Details") },
                    navigationIcon = {
                        IconButton(onClick = { if (currentStep == 1) onNavigateBack() else currentStep = 1 }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                if (currentStep == 1) {
                    ItemEntryStep(viewModel, onNext = { currentStep = 2 })
                } else {
                    InvoiceDetailsStep(viewModel, token, onDone = onNavigateBack)
                }
            }
        }
    }
}

data class CaseType(val label: String, val units: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEntryStep(viewModel: AddPurchaseViewModel, onNext: () -> Unit) {
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var looseQtyInput by remember { mutableStateOf("") }
    var caseQtyInput by remember { mutableStateOf("") }
    var amountPerItem by remember { mutableStateOf("") }
    
    var productExpanded by remember { mutableStateOf(false) }
    var caseTypeExpanded by remember { mutableStateOf(false) }

    val caseTypes = listOf(
        CaseType("1000ML (9)", 9),
        CaseType("750ML (12)", 12),
        CaseType("375ML (24)", 24),
        CaseType("180ML (48)", 48)
    )
    
    var selectedCaseType by remember { mutableStateOf(caseTypes[1]) } // Default to 750ML (12)

    val purchaseStock = remember(selectedCaseType, caseQtyInput, looseQtyInput) {
        val cases = caseQtyInput.toIntOrNull() ?: 0
        val loose = looseQtyInput.toIntOrNull() ?: 0
        (cases * selectedCaseType.units) + loose
    }

    val totalAmount = remember(purchaseStock, amountPerItem) {
        val price = amountPerItem.toDoubleOrNull() ?: 0.0
        purchaseStock * price
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // SKU Selection
                ExposedDropdownMenuBox(
                    expanded = productExpanded,
                    onExpandedChange = { productExpanded = !productExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedProduct?.sku ?: "Select SKU",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Product Item") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = productExpanded,
                        onDismissRequest = { productExpanded = false }
                    ) {
                        viewModel.products.value.forEach { product ->
                            DropdownMenuItem(
                                text = { Text(product.sku) },
                                onClick = {
                                    selectedProduct = product
                                    amountPerItem = product.purchasePrice.toString()
                                    productExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Case Type Selection
                    ExposedDropdownMenuBox(
                        expanded = caseTypeExpanded,
                        onExpandedChange = { caseTypeExpanded = !caseTypeExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedCaseType.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Case Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = caseTypeExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = caseTypeExpanded,
                            onDismissRequest = { caseTypeExpanded = false }
                        ) {
                            caseTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.label) },
                                    onClick = {
                                        selectedCaseType = type
                                        caseTypeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = caseQtyInput,
                        onValueChange = { caseQtyInput = it },
                        label = { Text("No. of Cases") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                OutlinedTextField(
                    value = looseQtyInput,
                    onValueChange = { looseQtyInput = it },
                    label = { Text("Loose Units") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = amountPerItem,
                    onValueChange = { amountPerItem = it },
                    label = { Text("Price Per Item") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Qty: $purchaseStock", fontWeight = FontWeight.Bold)
                    Text("Total: ${viewModel.currencyFormatter.format(totalAmount)}", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        if (selectedProduct != null && purchaseStock > 0) {
                            val openingStock = selectedProduct!!.stock
                            val cases = caseQtyInput.toIntOrNull() ?: 0
                            viewModel.addTempItem(
                                TempPurchaseItem(
                                    sku = selectedProduct!!.sku,
                                    openingStock = openingStock,
                                    purchaseStock = purchaseStock,
                                    stock = openingStock + purchaseStock,
                                    purchaseAmount = totalAmount,
                                    cases = cases
                                )
                            )
                            caseQtyInput = ""; looseQtyInput = ""; selectedProduct = null; amountPerItem = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add to List")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Added Items List", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(viewModel.tempPurchaseList) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                ) {
                    Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.sku, fontWeight = FontWeight.Bold)
                            val pricePerUnit = if (item.purchaseStock != 0) item.purchaseAmount / item.purchaseStock else 0.0
                            Text("Qty: ${item.purchaseStock} @ ${viewModel.currencyFormatter.format(pricePerUnit)}")
                        }
                        Text(viewModel.currencyFormatter.format(item.purchaseAmount), fontWeight = FontWeight.Bold)
                        IconButton(onClick = { viewModel.removeTempItem(item) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red)
                        }
                    }
                }
            }
        }

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            enabled = viewModel.tempPurchaseList.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = TNFL2_Primary)
        ) {
            Text("Next: Invoice Details")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailsStep(viewModel: AddPurchaseViewModel, token: String, onDone: () -> Unit) {
    var billNumber by remember { mutableStateOf(viewModel.billNo.value) }
    var purchaseDate by remember { mutableLongStateOf(viewModel.purchaseDate.value ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = purchaseDate
    )
    
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    // Update billNumber if it changes in the viewModel (e.g., when draft is loaded)
    LaunchedEffect(viewModel.billNo.value) {
        if (viewModel.billNo.value.isNotEmpty()) {
            billNumber = viewModel.billNo.value
        }
    }
    
    LaunchedEffect(viewModel.purchaseDate.value) {
        viewModel.purchaseDate.value?.let {
            purchaseDate = it
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = billNumber,
                    onValueChange = { billNumber = it },
                    label = { Text("Bill Number") },
                    modifier = Modifier.fillMaxWidth()
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = dateFormatter.format(Date(purchaseDate)),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Purchase Date") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Overlay to capture clicks since the text field is read-only
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showDatePicker = true }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                Text("Summary", fontWeight = FontWeight.Bold)
                val totalBill = viewModel.tempPurchaseList.sumOf { it.purchaseAmount }
                val totalQty = viewModel.tempPurchaseList.sumOf { it.purchaseStock }
                Text("Total Items: ${viewModel.tempPurchaseList.size}")
                Text("Total Quantity: $totalQty")
                Text("Total Bill Amount: ${viewModel.currencyFormatter.format(totalBill)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { 
                    viewModel.savePurchaseDraft(token, billNumber, purchaseDate / 1000) {
                        Toast.makeText(context, "Draft Saved Successfully!", Toast.LENGTH_SHORT).show()
                        onDone()
                    } 
                },
                modifier = Modifier.weight(1f),
                enabled = billNumber.isNotEmpty() && !viewModel.isLoading.value,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = TNFL2_Primary)
            ) {
                Text("Save Draft")
            }

            Button(
                onClick = { 
                    viewModel.savePurchaseFinal(token, billNumber, purchaseDate / 1000) {
                        Toast.makeText(context, "Purchase Saved Successfully!", Toast.LENGTH_SHORT).show()
                        onDone()
                    } 
                },
                modifier = Modifier.weight(1f),
                enabled = billNumber.isNotEmpty() && !viewModel.isLoading.value,
                colors = ButtonDefaults.buttonColors(containerColor = TNFL2_Primary, contentColor = Color.White)
            ) {
                Text("Save")
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        purchaseDate = datePickerState.selectedDateMillis ?: purchaseDate
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
