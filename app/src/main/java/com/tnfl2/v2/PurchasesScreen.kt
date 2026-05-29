package com.tnfl2.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tnfl2.v2.network.PurchaseItem
import com.tnfl2.v2.viewmodel.PurchasesViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PurchasesScreen(token: String, onAddPurchase: () -> Unit) {
    val viewModel: PurchasesViewModel = viewModel()
    val purchases = viewModel.purchases.value
    val isLoading = viewModel.isLoading.value
    val error = viewModel.error.value
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    var showDetailsDialog by remember { mutableStateOf(false) }
    var selectedPurchase by remember { mutableStateOf<PurchaseItem?>(null) }

    LaunchedEffect(token) {
        viewModel.fetchPurchases(token)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddPurchase,
                containerColor = Color(0xFF1D88E5), // Vibrant blue matching screenshots
                contentColor = Color.White,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .height(48.dp)
                    .padding(horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("New Purchase", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    ) { paddingValues ->
        val totalAmount = purchases.sumOf { it.billTotalAmount }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // KPI Card
            item {
                Spacer(modifier = Modifier.height(16.dp))
                KPIPurchaseCard(totalPurchaseAmount = totalAmount, formatter = viewModel.currencyFormatter)
            }

            // Metrics Grid
            item {
                MetricsGridRow(
                    totalBills = purchases.size,
                    totalQty = purchases.sumOf { it.billTotalUnits },
                    totalExpenses = totalAmount,
                    formatter = viewModel.currencyFormatter
                )
            }

            // Recent Purchases Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Purchases",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "View All",
                        color = Color(0xFF1D88E5),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { /* View all clicked */ }
                    )
                }
            }

            // Purchases list
            if (error != null) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("Error: $error", color = Color(0xFFEF4444))
                    }
                }
            } else if (isLoading) {
                // GlobalLoader handles the spinner overlay
            } else if (purchases.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("No purchases found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(purchases) { purchase ->
                    PurchaseCard(
                        purchase = purchase,
                        formatter = viewModel.currencyFormatter,
                        dateFormatter = dateFormatter,
                        onViewDetails = {
                            selectedPurchase = it
                            showDetailsDialog = true
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp)) // Extra space to scroll above FAB
            }
        }
    }

    if (showDetailsDialog && selectedPurchase != null) {
        PurchaseDetailsDialog(
            purchase = selectedPurchase!!,
            formatter = viewModel.currencyFormatter,
            onDismiss = { showDetailsDialog = false }
        )
    }
}

@Composable
fun KPIPurchaseCard(totalPurchaseAmount: Double, formatter: NumberFormat) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF1D4ED8), Color(0xFF3B82F6))
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "TOTAL PURCHASE AMOUNT",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatter.format(totalPurchaseAmount),
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .background(Color(0x22FFFFFF), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "This Month",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun MetricsGridRow(
    totalBills: Int,
    totalQty: Int,
    totalExpenses: Double,
    formatter: NumberFormat
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Total Bills Card
        MetricSubCard(
            modifier = Modifier.weight(1f),
            title = "Total Bills",
            value = totalBills.toString(),
            icon = Icons.Default.ReceiptLong,
            iconBgColor = Color(0xFFEFF6FF),
            iconColor = Color(0xFF2563EB)
        )
        // Total Qty Card
        MetricSubCard(
            modifier = Modifier.weight(1f),
            title = "Total Qty",
            value = totalQty.toString(),
            icon = Icons.Default.Inventory,
            iconBgColor = Color(0xFFF5F3FF),
            iconColor = Color(0xFF7C3AED)
        )
        // Expenses Card
        MetricSubCard(
            modifier = Modifier.weight(1.2f),
            title = "Expenses",
            value = formatter.format(totalExpenses),
            icon = Icons.Default.CurrencyRupee,
            iconBgColor = Color(0xFFFEF2F2),
            iconColor = Color(0xFFEF4444)
        )
    }
}

@Composable
fun MetricSubCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconColor: Color
) {
    Card(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(iconBgColor, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun PurchaseCard(
    purchase: PurchaseItem,
    formatter: NumberFormat,
    dateFormatter: SimpleDateFormat,
    onViewDetails: (PurchaseItem) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetails(purchase) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFEFF6FF), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Bill #${purchase.billNumber}",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                val formattedDate = dateFormatter.format(Date(purchase.purchaseDate * 1000))
                val itemsCount = purchase.billTotalUnits
                Text(
                    text = "$formattedDate · $itemsCount items",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formatter.format(purchase.billTotalAmount),
                    color = Color(0xFF1D88E5),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun PurchaseDetailsDialog(
    purchase: PurchaseItem,
    formatter: NumberFormat,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Items - Bill: ${purchase.billNumber}") },
        text = {
            LazyColumn {
                item {
                    Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                        Text("SKU", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold)
                        Text("Qty", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                        Text("Price", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    }
                    HorizontalDivider()
                }
                items(purchase.purchaseList) { product ->
                     Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(product.sku, modifier = Modifier.weight(2f), style = MaterialTheme.typography.bodySmall)
                        Text(product.purchaseQty.toString(), modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                        Text(formatter.format(product.purchasePrice), modifier = Modifier.weight(1.5f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
