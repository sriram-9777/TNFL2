package com.tnfl2.v2

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tnfl2.v2.network.*
import com.tnfl2.v2.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import java.text.NumberFormat
import java.text.SimpleDateFormat
import com.tnfl2.v2.ui.components.MonthYearPickerDialog
import com.tnfl2.v2.SessionManager
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(token: String) {
    var salesItems by remember { mutableStateOf<List<SaleItem>>(emptyList()) }
    val authRepository = remember { AuthRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showMonthYearPicker by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState()
    var selectedDateRange by remember { mutableStateOf<Pair<Long, Long>?>(null) }

    var totalSalesQty by remember { mutableStateOf(0) }
    var isLoadingQty by remember { mutableStateOf(false) }
    var filterMode by remember { mutableStateOf("Last Sale") }

    fun fetchData(startDate: Long, endDate: Long) {
        if (token.isNotEmpty()) {
            SessionManager.incrementLoading()
            scope.launch {
                authRepository.getSales(token, startDate / 1000, endDate / 1000).fold(
                    onSuccess = { response: com.tnfl2.v2.network.SalesResponse ->
                        salesItems = response.data
                        SessionManager.decrementLoading()
                        Toast.makeText(context, "Overview updated", Toast.LENGTH_SHORT).show()
                        
                        // Calculate quantity based on mode
                        isLoadingQty = true
                        if (filterMode == "Last Sale") {
                            val lastSale = response.data.lastOrNull()
                            if (lastSale != null) {
                                scope.launch {
                                    val id = lastSale.id?.toString() ?: ""
                                    if (id.isNotEmpty()) {
                                        totalSalesQty = authRepository.getSaleById(token, id).getOrNull()?.data?.productList?.filter { it.sales > 0 }?.sumOf { it.sales } ?: 0
                                    } else {
                                        totalSalesQty = 0
                                    }
                                    isLoadingQty = false
                                }
                            } else {
                                totalSalesQty = 0
                                isLoadingQty = false
                            }
                        } else {
                            // Aggregate for range
                            scope.launch {
                                val deferredQtys = response.data.map { sale ->
                                    async {
                                        val id = sale.id?.toString() ?: ""
                                        if (id.isNotEmpty()) {
                                            authRepository.getSaleById(token, id).getOrNull()?.data?.productList?.filter { it.sales > 0 }?.sumOf { it.sales } ?: 0
                                        } else 0
                                    }
                                }
                                totalSalesQty = deferredQtys.awaitAll().sum()
                                isLoadingQty = false
                            }
                        }
                    },
                    onFailure = {
                        SessionManager.decrementLoading()
                        Toast.makeText(context, "Failed to load overview: ${it.message}", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    LaunchedEffect(token, filterMode, selectedDateRange) {
        val today = Calendar.getInstance()
        val startOfMonth = today.clone() as Calendar
        startOfMonth.set(Calendar.DAY_OF_MONTH, 1)
        val start = selectedDateRange?.first ?: startOfMonth.timeInMillis
        val end = selectedDateRange?.second ?: today.timeInMillis
        
        // Ensure selectedDateRange is initialized
        if (selectedDateRange == null && filterMode != "All") {
            selectedDateRange = Pair(start, end)
        }
        
        when (filterMode) {
            "Last Sale" -> {
                fetchData(start, end)
            }
            "Range" -> {
                fetchData(start, end)
            }
            "All" -> {
                fetchData(0, System.currentTimeMillis())
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            colors = DatePickerDefaults.colors(
                containerColor = Color.White.copy(alpha = 0.95f),
            ),
            confirmButton = {
                TextButton(
                    onClick = { 
                        showDatePicker = false
                        dateRangePickerState.selectedStartDateMillis?.let { start ->
                            val end = dateRangePickerState.selectedEndDateMillis ?: start
                            selectedDateRange = Pair(start, end)
                            filterMode = "Range"
                        }
                    }
                ) {
                    Text("OK", color = TNFL2_Primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState, 
                modifier = Modifier.padding(16.dp),
                colors = DatePickerDefaults.colors(
                    titleContentColor = TNFL2_Primary,
                    headlineContentColor = TNFL2_Primary,
                    selectedDayContainerColor = TNFL2_Primary,
                    todayContentColor = TNFL2_Primary,
                    todayDateBorderColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }

    if (showMonthYearPicker) {
        val calendar = Calendar.getInstance()
        MonthYearPickerDialog(
            initialMonth = calendar.get(Calendar.MONTH),
            initialYear = calendar.get(Calendar.YEAR),
            onDismiss = { showMonthYearPicker = false },
            onConfirm = { month, year ->
                showMonthYearPicker = false
                val startCal = Calendar.getInstance()
                startCal.set(year, month, 1, 0, 0, 0)
                startCal.set(Calendar.MILLISECOND, 0)
                
                val endCal = Calendar.getInstance()
                endCal.set(year, month, 1, 23, 59, 59)
                endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                
                selectedDateRange = Pair(startCal.timeInMillis, endCal.timeInMillis)
                filterMode = "Range"
            }
        )
    }

    val displaySales: Double
    val displayProfit: Double
    val displayExpenses: Double

    if (filterMode == "Last Sale") {
        val lastSale = salesItems.lastOrNull()
        displaySales = lastSale?.totalSalesAmount ?: 0.0
        displayProfit = lastSale?.profitAmount ?: 0.0
        displayExpenses = lastSale?.totalExpensesAmount ?: 0.0
    } else {
        displaySales = salesItems.sumOf { it.totalSalesAmount }
        displayProfit = salesItems.sumOf { it.profitAmount }
        displayExpenses = salesItems.sumOf { it.totalExpensesAmount }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        // Overview Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Overview",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { showDatePicker = true }
            ) {
                val displayRangeText = if (filterMode == "All") {
                    "Lifetime"
                } else {
                    selectedDateRange?.let {
                        val sdf = SimpleDateFormat("MMM dd, yy", Locale.getDefault())
                        "${sdf.format(Date(it.first))} - ${sdf.format(Date(it.second))}"
                    } ?: "Select Date"
                }
                Text(
                    text = displayRangeText,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filter",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Mode badge under overview title
        Box(
            modifier = Modifier
                .border(BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f)), RoundedCornerShape(12.dp))
                .background(Color(0xFFEFF6FF), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = filterMode,
                color = Color(0xFF2563EB),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main Sales Amount Gradient Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(24.dp),
                    clip = false
                ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF0F766E), Color(0xFF2563EB))
                        )
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (filterMode == "Last Sale") "Last Sales Amount" else "Total Sales Amount",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        
                        // Profit Badge
                        val isProfitPositive = displayProfit >= 0
                        val badgeBg = if (isProfitPositive) Color(0xFFDCFCE7).copy(alpha = 0.2f) else Color(0xFFFEE2E2).copy(alpha = 0.2f)
                        val badgeText = if (isProfitPositive) Color(0xFF4ADE80) else Color(0xFFF87171)
                        val arrow = if (isProfitPositive) "↑ " else "↓ "
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(badgeBg)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = arrow + currencyFormat.format(displayProfit),
                                color = badgeText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = currencyFormat.format(displaySales),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Bottom sale date text
                    val saleDateText = remember(salesItems, filterMode) {
                        val lastSale = salesItems.lastOrNull()
                        if (lastSale != null) {
                            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                            "Sale Date: " + sdf.format(Date(lastSale.timeCreatedAt * 1000L))
                        } else {
                            "No sales recorded"
                        }
                    }
                    Text(
                        text = saleDateText,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Side-by-side Sub-cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Card - Quantity
            Card(
                modifier = Modifier
                    .weight(1f)
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (filterMode == "Last Sale") "Last Qty" else "Total Qty",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFFEFF6FF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isLoadingQty) "..." else totalSalesQty.toString(),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            
            // Right Card - Expenses
            Card(
                modifier = Modifier
                    .weight(1f)
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (filterMode == "Last Sale") "Last Exp." else "Total Exp.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFFFEF2F2), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = currencyFormat.format(displayExpenses),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Toggle Control Bar (Last Sale / Range / All)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val modes = listOf("Last Sale", "Range", "All")
                modes.forEach { mode ->
                    val isSelected = filterMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (isSelected) Color(0xFF005696) else Color.Transparent)
                            .clickable { filterMode = mode }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode,
                            color = if (isSelected) Color.White else Color(0xFF64748B),
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons Row (Date Range / Month/Year / Refresh)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ActionButton(
                modifier = Modifier.weight(1.1f),
                icon = Icons.Default.CalendarMonth,
                text = "Date Range",
                onClick = { showDatePicker = true }
            )
            ActionButton(
                modifier = Modifier.weight(1.1f),
                icon = Icons.Default.DateRange,
                text = "Month/Year",
                onClick = { showMonthYearPicker = true }
            )
            ActionButton(
                modifier = Modifier.weight(0.9f),
                icon = Icons.Default.Refresh,
                text = "Refresh",
                onClick = {
                    val range = selectedDateRange ?: Pair(
                        Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis,
                        System.currentTimeMillis()
                    )
                    fetchData(range.first, range.second)
                }
            )
        }
    }
}

@Composable
fun ActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = Color(0xFF005696),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(token: String, onAddProduct: () -> Unit, refreshKey: Int) {
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    val authRepository = remember { AuthRepository() }
    val scope = rememberCoroutineScope()
    var dashboardResponse by remember { mutableStateOf<DashboardResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    // Note: isLoading kept for conditional content display; GlobalLoader handles spinner
    val context = LocalContext.current
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var internalRefreshKey by remember { mutableIntStateOf(0) }

    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
    }

    var selectedCategory by remember { mutableStateOf("All") }

    val filteredProducts = products.filter {
        (selectedCategory == "All" || it.category.equals(selectedCategory, ignoreCase = true)) &&
        (it.sku.contains(searchQuery, ignoreCase = true) ||
         it.brand.contains(searchQuery, ignoreCase = true) ||
         it.category.contains(searchQuery, ignoreCase = true))
    }

    LaunchedEffect(token, refreshKey, internalRefreshKey) {
        if (token.isNotEmpty()) {
            isLoading = true
            SessionManager.incrementLoading()
            scope.launch {
                authRepository.getProductMaster(token).fold(
                    onSuccess = {
                        products = it.productList
                        isLoading = false
                        SessionManager.decrementLoading()
                        Toast.makeText(context, "Products loaded", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = {
                        isLoading = false
                        SessionManager.decrementLoading()
                        Toast.makeText(context, "Error loading products: ${it.message}", Toast.LENGTH_LONG).show()
                    }
                )
            }
            scope.launch {
                authRepository.getDashboard(token).fold(
                    onSuccess = {
                        dashboardResponse = it
                    },
                    onFailure = {}
                )
            }
        }
    }

    // Edit Product Bottom Sheet
    if (editingProduct != null) {
        EditProductBottomSheet(
            product = editingProduct!!,
            onDismiss = { editingProduct = null },
            onSave = { request ->
                scope.launch {
                    authRepository.updateProduct(token, request).fold(
                        onSuccess = {
                            Toast.makeText(context, "Product updated!", Toast.LENGTH_SHORT).show()
                            editingProduct = null
                            internalRefreshKey++
                        },
                        onFailure = { e ->
                            Toast.makeText(context, "Update failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddProduct,
                containerColor = Color(0xFF005696),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Product")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Metrics Summary Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Products",
                    value = products.size.toString(),
                    icon = Icons.Default.Inventory2,
                    backgroundColor = Color(0xFF2563EB)
                )
                val lowStockCount = products.count { it.stock in 1..5 }
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Low Stock",
                    value = lowStockCount.toString(),
                    icon = Icons.Default.Warning,
                    backgroundColor = Color(0xFFF97316)
                )
                val outOfStockCount = products.count { it.stock == 0 }
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Out of Stock",
                    value = outOfStockCount.toString(),
                    icon = Icons.Default.ShoppingCart,
                    backgroundColor = Color(0xFFEF4444)
                )
            }

            // Pill-Shaped Search Input
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search SKU, brand or category...", color = Color.Gray) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.Gray
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color.Gray
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(28.dp))
            )

            // Horizontal Filter Chips
            val categories = remember(products) {
                listOf("All") + products.map { it.category }.distinct().sorted()
            }
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory == category
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Color(0xFF005696) else MaterialTheme.colorScheme.surface)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = category.uppercase(Locale.ROOT),
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Sub-header Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredProducts.size} items",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = "Investment",
                        tint = Color(0xFF005696),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val investmentVal = dashboardResponse?.investmentAmount?.toDoubleOrNull() ?: 0.0
                    Text(
                        text = currencyFormat.format(investmentVal),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Products List lazy column
            if (!isLoading) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredProducts) { product ->
                        ProductCard(
                            product = product,
                            onEdit = { editingProduct = product }
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    // GlobalLoader handles the spinner overlay
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    backgroundColor: Color
) {
    Card(
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = value,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

data class ProductDetails(
    val stock: String = "",
    val purchasePrice: String = "",
    val salePrice: String = "",
    val profitAmount: String = ""
)

@Composable
fun AddProductScreen(token: String, onProductAdded: () -> Unit, onCancel: () -> Unit) {
    var brand by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Wine") }

    val authRepository = remember { AuthRepository() }
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(type) {
        if (type != "Liquor" && type != "Wine") {
            brand = type.uppercase(Locale.ROOT)
        } else {
            brand = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE4F6F5),
                        Color(0xFF9DD4E9)
                    )
                )
            )
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Add New Product", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            var typeExpanded by remember { mutableStateOf(false) }
            val types = listOf("Wine", "Liquor", "Beer", "COOLDRINKS", "CIGERATTE")
            Box(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                OutlinedTextField(
                    value = type, onValueChange = {}, enabled = false,
                    label = { Text("Type") },
                    modifier = Modifier.fillMaxWidth().clickable { typeExpanded = true },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, "Select Type", tint = Color.Black) },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = Color.Black,
                        disabledBorderColor = Color.Gray,
                        disabledLabelColor = Color.DarkGray,
                        disabledContainerColor = Color.White.copy(alpha = 0.3f),
                        disabledTrailingIconColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }, modifier = Modifier.fillMaxWidth(0.4f)) {
                    types.forEach { typeName ->
                        DropdownMenuItem(text = { Text(typeName) }, onClick = { type = typeName; typeExpanded = false })
                    }
                }
            }
            OutlinedTextField(
                value = sku, 
                onValueChange = { sku = it }, 
                label = { Text("SKU Name") }, 
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedTextColor = Color.Black,
                    focusedTextColor = Color.Black,
                    unfocusedLabelColor = Color.DarkGray,
                    focusedLabelColor = Color.DarkGray,
                    unfocusedBorderColor = Color.Gray,
                    focusedBorderColor = Color.Black,
                    unfocusedContainerColor = Color.White.copy(alpha = 0.3f),
                    focusedContainerColor = Color.White.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (type == "Liquor" || type == "Wine") {
                var brandExpanded by remember { mutableStateOf(false) }
                val brands = if (type == "Liquor") {
                    listOf("RUM", "VODKA", "BRANDY", "WHISKY", "SCOTCH")
                } else {
                    listOf("RED", "WHITE", "ROSE", "SPARKLING")
                }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = brand, onValueChange = {}, enabled = false, label = { Text(if (type == "Wine") "Wine Type" else "Brand") },
                        modifier = Modifier.fillMaxWidth().clickable { brandExpanded = true },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, "Select Brand", tint = Color.Black) },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = Color.Black,
                            disabledBorderColor = Color.Gray,
                            disabledLabelColor = Color.DarkGray,
                            disabledContainerColor = Color.White.copy(alpha = 0.3f),
                            disabledTrailingIconColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    DropdownMenu(expanded = brandExpanded, onDismissRequest = { brandExpanded = false }, modifier = Modifier.fillMaxWidth()) {
                        brands.forEach { brandName ->
                            DropdownMenuItem(text = { Text(brandName) }, onClick = { brand = brandName; brandExpanded = false })
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = brand, onValueChange = {}, label = { Text("Brand") }, modifier = Modifier.fillMaxWidth(), enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = Color.Black,
                        disabledBorderColor = Color.Gray,
                        disabledLabelColor = Color.DarkGray,
                        disabledContainerColor = Color.White.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        ProductDetailsTable(
            type = type,
            onAddProductClicked = { productDetails ->
                scope.launch {
                    isLoading = true
                    SessionManager.incrementLoading()
                    val detailItems = productDetails.mapNotNull { (volume, details) ->
                        val stock = details.stock.toIntOrNull()
                        val purchasePrice = details.purchasePrice.toDoubleOrNull()
                        val salePrice = details.salePrice.toDoubleOrNull()
                        val profitAmount = details.profitAmount.toDoubleOrNull()
                        if (stock != null && purchasePrice != null && salePrice != null && profitAmount != null) {
                            ProductDetailItem(
                                size = volume, stock = stock, purchasePrice = purchasePrice,
                                salePrice = salePrice, profitAmount = profitAmount
                            )
                        } else null
                    }
                    if (sku.isNotBlank() && brand.isNotBlank() && type.isNotBlank() && detailItems.isNotEmpty()) {
                        val finalRequest = AddProductRequest(sku, brand, type, detailItems)
                        authRepository.addProduct(token, finalRequest).fold(
                            onSuccess = { 
                                isLoading = false
                                SessionManager.decrementLoading()
                                Toast.makeText(context, "Product added successfully!", Toast.LENGTH_SHORT).show()
                                onProductAdded() 
                            },
                            onFailure = {
                                isLoading = false
                                SessionManager.decrementLoading()
                                Toast.makeText(context, "Failed to add product: ${it.message}", Toast.LENGTH_LONG).show()
                            }
                        )
                    } else {
                        isLoading = false
                        SessionManager.decrementLoading()
                    }
                }
            },
            onCancel = onCancel,
            isLoading = isLoading
        )
    }
}

@Composable
private fun ProductDetailsTable(
    type: String,
    onAddProductClicked: (Map<String, ProductDetails>) -> Unit,
    onCancel: () -> Unit,
    isLoading: Boolean
) {
    val volumes = remember(type) {
        when (type) {
            "Beer", "COOLDRINKS", "CIGERATTE" -> listOf("650ML", "500ML", "325ML")
            else -> listOf("180 ML", "375 ML", "750 ML", "1800 ML")
        }
    }

    var productDetails by remember(volumes) { mutableStateOf<Map<String, ProductDetails>>(emptyMap()) }

    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.weight(1f))
            volumes.forEach { volume ->
                Text(text = volume, modifier = Modifier.weight(1f).padding(horizontal = 2.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color.Black)
            }
        }
        HorizontalDivider(color = Color.Gray.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Stock", modifier = Modifier.weight(1f).padding(end = 4.dp), fontWeight = FontWeight.Bold, color = Color.Black)
            volumes.forEach { volume ->
                OutlinedTextField(
                    value = productDetails[volume]?.stock ?: "",
                    onValueChange = { newValue ->
                        val newMap = productDetails.toMutableMap()
                        newMap[volume] = (newMap[volume] ?: ProductDetails()).copy(stock = newValue)
                        productDetails = newMap
                    },
                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp).height(50.dp),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, color = Color.Black),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Black,
                        unfocusedBorderColor = Color.Gray,
                        unfocusedContainerColor = Color.White.copy(alpha = 0.2f),
                        focusedContainerColor = Color.White.copy(alpha = 0.4f),
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    ),
                    shape = RoundedCornerShape(4.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Purchase\nPrice\n(MRP)", modifier = Modifier.weight(1f).padding(end = 4.dp), fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 12.sp)
            volumes.forEach { volume ->
                OutlinedTextField(
                    value = productDetails[volume]?.purchasePrice ?: "",
                    onValueChange = { newPurchasePrice ->
                        val newMap = productDetails.toMutableMap()
                        val currentDetails = newMap[volume] ?: ProductDetails()
                        val salePrice = currentDetails.salePrice.toDoubleOrNull()
                        val purchasePrice = newPurchasePrice.toDoubleOrNull()
                        val profit = if (salePrice != null && purchasePrice != null) (salePrice - purchasePrice).toString() else ""
                        newMap[volume] = currentDetails.copy(purchasePrice = newPurchasePrice, profitAmount = profit)
                        productDetails = newMap
                    },
                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp).height(50.dp),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, color = Color.Black),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Black,
                        unfocusedBorderColor = Color.Gray,
                        unfocusedContainerColor = Color.White.copy(alpha = 0.2f),
                        focusedContainerColor = Color.White.copy(alpha = 0.4f),
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    ),
                    shape = RoundedCornerShape(4.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Sale\nPrice", modifier = Modifier.weight(1f).padding(end = 4.dp), fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 12.sp)
            volumes.forEach { volume ->
                OutlinedTextField(
                    value = productDetails[volume]?.salePrice ?: "",
                    onValueChange = { newSalePrice ->
                        val newMap = productDetails.toMutableMap()
                        val currentDetails = newMap[volume] ?: ProductDetails()
                        val salePrice = newSalePrice.toDoubleOrNull()
                        val purchasePrice = currentDetails.purchasePrice.toDoubleOrNull()
                        val profit = if (salePrice != null && purchasePrice != null) (salePrice - purchasePrice).toString() else ""
                        newMap[volume] = currentDetails.copy(salePrice = newSalePrice, profitAmount = profit)
                        productDetails = newMap
                    },
                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp).height(50.dp),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, color = Color.Black),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Black,
                        unfocusedBorderColor = Color.Gray,
                        unfocusedContainerColor = Color.White.copy(alpha = 0.2f),
                        focusedContainerColor = Color.White.copy(alpha = 0.4f),
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    ),
                    shape = RoundedCornerShape(4.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Profit\nAmount", modifier = Modifier.weight(1f).padding(end = 4.dp), fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 12.sp)
            volumes.forEach { volume ->
                OutlinedTextField(
                    value = productDetails[volume]?.profitAmount ?: "", onValueChange = {}, enabled = false,
                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp).height(50.dp),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, color = Color.Black),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = Color.Black,
                        disabledBorderColor = Color.Gray,
                        disabledContainerColor = Color.White.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(4.dp),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f, fill = false))


        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = onCancel,
                modifier = Modifier.padding(end = 16.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF53A8E0), contentColor = Color.Black),
                shape = RoundedCornerShape(50)
            ) {
                Text("Cancel", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { onAddProductClicked(productDetails) },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF53A8E0), contentColor = Color.Black),
                shape = RoundedCornerShape(50)
            ) {
                Text("Add Product", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(token: String, onAddSale: () -> Unit, onSaleConfirmed: () -> Unit) {
    var salesItems by remember { mutableStateOf<List<SaleItem>>(emptyList()) }
    val authRepository = remember { AuthRepository() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
    }

    var showAddSaleScreen by remember { mutableStateOf(false) }
    var editingSale by remember { mutableStateOf<EditSaleItem?>(null) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var viewingSaleDetails by remember { mutableStateOf<EditSaleItem?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showMonthYearPicker by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState()
    var selectedDateRange by remember { mutableStateOf<Pair<Long, Long>?>(null) }

    fun fetchData(startDate: Long, endDate: Long) {
        if (token.isNotEmpty()) {
            SessionManager.incrementLoading()
            scope.launch {
                authRepository.getSales(token, startDate / 1000, endDate / 1000).fold(
                    onSuccess = { 
                        salesItems = it.data.reversed()
                        SessionManager.decrementLoading()
                        Toast.makeText(context, "Sales history updated", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { 
                        SessionManager.decrementLoading()
                        Toast.makeText(context, "Error loading sales: ${it.message}", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    if (showAddSaleScreen) {
        AddSaleScreen(
            token = token,
            initialSale = editingSale,
            onSaleConfirmed = {
                showAddSaleScreen = false
                editingSale = null
                onSaleConfirmed()
            },
            onCancel = { 
                showAddSaleScreen = false
                editingSale = null
            }
        )
    } else {
        LaunchedEffect(token, onSaleConfirmed) {
            val now = System.currentTimeMillis()
            selectedDateRange = null
            fetchData(0, now)
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                colors = DatePickerDefaults.colors(
                    containerColor = Color.White.copy(alpha = 0.95f),
                ),
                confirmButton = {
                    TextButton(
                        onClick = { 
                            showDatePicker = false
                            dateRangePickerState.selectedStartDateMillis?.let { start ->
                                val end = dateRangePickerState.selectedEndDateMillis ?: start
                                selectedDateRange = Pair(start, end)
                                fetchData(start, end)
                            }
                        }
                    ) {
                        Text("OK", color = TNFL2_Primary, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDatePicker = false }
                    ) {
                        Text("Cancel")
                    }
                }
            ) {
                DateRangePicker(
                    state = dateRangePickerState, 
                    modifier = Modifier.padding(16.dp),
                    colors = DatePickerDefaults.colors(
                        titleContentColor = TNFL2_Primary,
                        headlineContentColor = TNFL2_Primary,
                        selectedDayContainerColor = TNFL2_Primary,
                        todayContentColor = TNFL2_Primary,
                        todayDateBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        if (showMonthYearPicker) {
            val calendar = Calendar.getInstance()
            MonthYearPickerDialog(
                initialMonth = calendar.get(Calendar.MONTH),
                initialYear = calendar.get(Calendar.YEAR),
                onDismiss = { showMonthYearPicker = false },
                onConfirm = { month, year ->
                    showMonthYearPicker = false
                    val startCal = Calendar.getInstance()
                    startCal.set(year, month, 1, 0, 0, 0)
                    startCal.set(Calendar.MILLISECOND, 0)
                    
                    val endCal = Calendar.getInstance()
                    endCal.set(year, month, 1, 23, 59, 59)
                    endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                    
                    selectedDateRange = Pair(startCal.timeInMillis, endCal.timeInMillis)
                    fetchData(startCal.timeInMillis, endCal.timeInMillis)
                }
            )
        }

        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onAddSale,
                    containerColor = Color(0xFF0D9488), // Teal color from design
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Sale")
                }
            }
        ) { paddingValues ->
            val totalOverallSales = salesItems.sumOf { it.totalSalesAmount }
            val totalOverallProfit = salesItems.sumOf { it.profitAmount }
            val totalOverallExpenses = salesItems.sumOf { it.totalExpensesAmount }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp)
            ) {
                // Filter Header Row
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Date Chip
                        val dateText = selectedDateRange?.let {
                            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            "${sdf.format(Date(it.first))} - ${sdf.format(Date(it.second))}"
                        } ?: "All History"

                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE2F0D9), RoundedCornerShape(20.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = dateText,
                                color = Color(0xFF385723),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Filter Menu trigger
                        Box {
                            IconButton(onClick = { showFilterMenu = true }) {
                                Icon(Icons.Default.FilterList, contentDescription = "Filter menu", tint = Color(0xFF0F172A))
                            }
                            DropdownMenu(
                                expanded = showFilterMenu,
                                onDismissRequest = { showFilterMenu = false },
                                modifier = Modifier.background(Color.White.copy(alpha = 0.95f))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Date Range") },
                                    onClick = { showFilterMenu = false; showDatePicker = true },
                                    leadingIcon = { Icon(Icons.Default.CalendarMonth, null, tint = TNFL2_Primary) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Month/Year") },
                                    onClick = { showFilterMenu = false; showMonthYearPicker = true },
                                    leadingIcon = { Icon(Icons.Default.DateRange, null, tint = TNFL2_Primary) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Show All") },
                                    onClick = { 
                                        showFilterMenu = false
                                        selectedDateRange = null
                                        fetchData(0, System.currentTimeMillis())
                                    },
                                    leadingIcon = { Icon(Icons.Default.History, null, tint = TNFL2_Primary) }
                                )
                            }
                        }
                    }
                }

                // KPI Card
                item {
                    KPISaleCard(
                        totalSales = totalOverallSales,
                        totalProfit = totalOverallProfit,
                        billCount = salesItems.size,
                        formatter = currencyFormat
                    )
                }

                // Sub-Cards Row
                item {
                    SalesMetricsCardsRow(
                        billCount = salesItems.size,
                        totalProfit = totalOverallProfit,
                        totalExpenses = totalOverallExpenses,
                        formatter = currencyFormat
                    )
                }

                // Recent sales list header / placeholder spacer
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Sales Items List
                itemsIndexed(salesItems) { index, sale ->
                    SaleItemCard(
                        sale = sale, 
                        isEditable = index == 0,
                        onEdit = {
                            scope.launch {
                                val saleId = sale.id?.toString() ?: ""
                                if (saleId.isNotEmpty()) {
                                    authRepository.getSaleById(token, saleId).fold(
                                        onSuccess = { response ->
                                            editingSale = response.data
                                            showAddSaleScreen = true
                                        },
                                        onFailure = { 
                                            Toast.makeText(context, "Error fetching details: ${it.message}", Toast.LENGTH_LONG).show()
                                        }
                                    )
                                } else {
                                    Toast.makeText(context, "Sale ID is missing", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onViewDetails = {
                            scope.launch {
                                val saleId = sale.id?.toString() ?: ""
                                if (saleId.isNotEmpty()) {
                                    authRepository.getSaleById(token, saleId).fold(
                                        onSuccess = { response ->
                                            viewingSaleDetails = response.data
                                            showDetailsDialog = true
                                        },
                                        onFailure = { 
                                            Toast.makeText(context, "Error fetching details: ${it.message}", Toast.LENGTH_LONG).show()
                                        }
                                    )
                                } else {
                                    Toast.makeText(context, "Sale ID is missing", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }

        if (showDetailsDialog && viewingSaleDetails != null) {
            SaleItemDetailsDialog(
                saleDetails = viewingSaleDetails!!,
                onDismiss = { showDetailsDialog = false }
            )
        }
    }
}

@Composable
fun KPISaleCard(
    totalSales: Double,
    totalProfit: Double,
    billCount: Int,
    formatter: NumberFormat
) {
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
                        colors = listOf(Color(0xFF0D9488), Color(0xFF0F766E)) // Teal gradient
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "TOTAL SALES",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatter.format(totalSales),
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bills badge
                    Box(
                        modifier = Modifier
                            .background(Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "$billCount Bills",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Profit Badge
                    Box(
                        modifier = Modifier
                            .background(Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "↑",
                                color = Color(0xFF86EFAC),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatter.format(totalProfit),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SalesMetricsCardsRow(
    billCount: Int,
    totalProfit: Double,
    totalExpenses: Double,
    formatter: NumberFormat
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Bills Card (Blue)
        SalesMetricSubCard(
            modifier = Modifier.weight(1f),
            title = "Bills",
            value = billCount.toString(),
            icon = Icons.Default.Description,
            backgroundColor = Color(0xFF1D88E5)
        )
        // Profit Card (Green)
        SalesMetricSubCard(
            modifier = Modifier.weight(1.2f),
            title = "Profit",
            value = formatter.format(totalProfit),
            icon = Icons.Default.ArrowUpward,
            backgroundColor = Color(0xFF2E7D32)
        )
        // Expenses Card (Red)
        SalesMetricSubCard(
            modifier = Modifier.weight(1.2f),
            title = "Expenses",
            value = formatter.format(totalExpenses),
            icon = Icons.Default.ArrowDownward,
            backgroundColor = Color(0xFFC62828)
        )
    }
}

@Composable
fun SalesMetricSubCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    backgroundColor: Color
) {
    Card(
        modifier = modifier.height(95.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = value,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2
            )
        }
    }
}

@Composable
fun SaleItemCard(
    sale: SaleItem,
    isEditable: Boolean = false,
    onEdit: () -> Unit = {},
    onViewDetails: () -> Unit = {}
) {
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
    }
    val dateFormat = remember {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetails() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = dateFormat.format(Date(sale.timeCreatedAt * 1000)),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Invoice #${sale.invoiceNumber}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Visibility button (blue circle)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFEFF6FF), CircleShape)
                            .clickable { onViewDetails() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "View Details",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Edit button (green circle, only if editable)
                    if (isEditable) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFFEFFDF5), CircleShape)
                                .clickable { onEdit() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Sale",
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pills row (Gross, Expenses, Digital)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Gross Pill
                SaleBreakdownPill(
                    modifier = Modifier.weight(1f),
                    label = "Gross",
                    value = currencyFormat.format(sale.totalSalesAmount),
                    bgColor = Color(0xFFDCFCE7),
                    labelColor = Color(0xFF15803D),
                    valueColor = Color(0xFF166534)
                )
                // Expenses Pill
                SaleBreakdownPill(
                    modifier = Modifier.weight(1f),
                    label = "Expenses",
                    value = currencyFormat.format(sale.totalExpensesAmount),
                    bgColor = Color(0xFFFEE2E2),
                    labelColor = Color(0xFFB91C1C),
                    valueColor = Color(0xFF991B1B)
                )
                // Digital Pill
                SaleBreakdownPill(
                    modifier = Modifier.weight(1f),
                    label = "Digital",
                    value = currencyFormat.format(sale.totalDigitalAmount),
                    bgColor = Color(0xFFDBEAFE),
                    labelColor = Color(0xFF1D4ED8),
                    valueColor = Color(0xFF1E40AF)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Final Settlement Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF0F9FF), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = null,
                        tint = Color(0xFF1D4ED8),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Final Settlement",
                        color = Color(0xFF1D4ED8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = currencyFormat.format(sale.finalCashSettlement),
                    color = Color(0xFF1D4ED8),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
fun SaleBreakdownPill(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    bgColor: Color,
    labelColor: Color,
    valueColor: Color
) {
    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column {
            Text(
                text = label,
                color = labelColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun SaleItemDetailsDialog(
    saleDetails: EditSaleItem,
    onDismiss: () -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN")) }
    val totalQty = remember(saleDetails.productList) {
        saleDetails.productList.filter { it.sales > 0 }.sumOf { it.sales }
    }
    val totalAmount = remember(saleDetails.productList) {
        saleDetails.productList.filter { it.sales > 0 }.sumOf { it.totalSaleAmount }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Items - Bill: ${saleDetails.invoiceNumber}") },
        text = {
            LazyColumn {
                item {
                    Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                        Text("SKU", modifier = Modifier.weight(2.5f), fontWeight = FontWeight.Bold)
                        Text("Qty", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                        Text("Price", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                        Text("Total", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    }
                    HorizontalDivider()
                }
                items(saleDetails.productList) { product ->
                    if (product.sales > 0) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(product.sku, modifier = Modifier.weight(2.5f), style = MaterialTheme.typography.bodySmall)
                            Text(product.sales.toString(), modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                            Text(currencyFormat.format(product.salePrice), modifier = Modifier.weight(1.5f), textAlign = androidx.compose.ui.text.style.TextAlign.End, style = MaterialTheme.typography.bodySmall)
                            Text(currencyFormat.format(product.totalSaleAmount), modifier = Modifier.weight(1.5f), textAlign = androidx.compose.ui.text.style.TextAlign.End, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item {
                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Text("Total", modifier = Modifier.weight(2.5f), fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                        Text(totalQty.toString(), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                        Spacer(modifier = Modifier.weight(1.5f))
                        Text(currencyFormat.format(totalAmount), modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.End)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(token: String) {
    var expenses by remember { mutableStateOf<List<ExpenseItem>>(emptyList()) }
    val authRepository = remember { AuthRepository() }
    val scope = rememberCoroutineScope()
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN")) }
    var isLoading by remember { mutableStateOf(false) }
    // Note: isLoading kept for conditional content display; GlobalLoader handles spinner
    val context = LocalContext.current

    var showDatePicker by remember { mutableStateOf(false) }
    var showMonthYearPicker by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis,
        initialSelectedEndDateMillis = System.currentTimeMillis()
    )
    var selectedDateRange by remember { 
        mutableStateOf<Pair<Long, Long>>(
            Pair(
                dateRangePickerState.selectedStartDateMillis!!,
                dateRangePickerState.selectedEndDateMillis!!
            )
        )
     }

    fun fetchData(startDate: Long, endDate: Long) {
        if (token.isNotEmpty()) {
            isLoading = true
            SessionManager.incrementLoading()
            scope.launch {
                authRepository.getExpenses(token, startDate / 1000, endDate / 1000).fold(
                    onSuccess = { 
                        expenses = it.data 
                        isLoading = false
                        SessionManager.decrementLoading()
                        Toast.makeText(context, "Expenses loaded", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { 
                        isLoading = false
                        SessionManager.decrementLoading()
                        Toast.makeText(context, "Error loading expenses: ${it.message}", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    LaunchedEffect(selectedDateRange) {
        fetchData(selectedDateRange.first, selectedDateRange.second)
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            colors = DatePickerDefaults.colors(
                containerColor = Color.White.copy(alpha = 0.95f),
            ),
            confirmButton = {
                TextButton(
                    onClick = { 
                        showDatePicker = false
                        dateRangePickerState.selectedStartDateMillis?.let { start ->
                            val end = dateRangePickerState.selectedEndDateMillis ?: start
                            selectedDateRange = Pair(start, end)
                        }
                    }
                ) {
                    Text("OK", color = TNFL2_Primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState, 
                modifier = Modifier.padding(16.dp),
                colors = DatePickerDefaults.colors(
                    titleContentColor = TNFL2_Primary,
                    headlineContentColor = TNFL2_Primary,
                    selectedDayContainerColor = TNFL2_Primary,
                    todayContentColor = TNFL2_Primary,
                    todayDateBorderColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }

    if (showMonthYearPicker) {
        val calendar = Calendar.getInstance()
        MonthYearPickerDialog(
            initialMonth = calendar.get(Calendar.MONTH),
            initialYear = calendar.get(Calendar.YEAR),
            onDismiss = { showMonthYearPicker = false },
            onConfirm = { month, year ->
                showMonthYearPicker = false
                val startCal = Calendar.getInstance()
                startCal.set(year, month, 1, 0, 0, 0)
                startCal.set(Calendar.MILLISECOND, 0)
                
                val endCal = Calendar.getInstance()
                endCal.set(year, month, 1, 23, 59, 59)
                endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                
                selectedDateRange = Pair(startCal.timeInMillis, endCal.timeInMillis)
            }
        )
    }

    val totalExpensesVal = expenses.sumOf { it.totalAmount.toDoubleOrNull() ?: 0.0 }
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Date chip filter row
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date Range Chip
                val sdf = SimpleDateFormat("dd MMM yy", Locale.getDefault())
                val rangeText = "${sdf.format(Date(selectedDateRange.first))} - ${sdf.format(Date(selectedDateRange.second))}"

                Box(
                    modifier = Modifier
                        .background(Color(0xFFFEE2E2), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = rangeText,
                        color = Color(0xFFB91C1C),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Filter Menu Trigger
                Box {
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter Menu", tint = Color(0xFF0F172A))
                    }
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false },
                        modifier = Modifier.background(Color.White.copy(alpha = 0.95f))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Date Range") },
                            onClick = { showFilterMenu = false; showDatePicker = true },
                            leadingIcon = { Icon(Icons.Default.CalendarMonth, null, tint = TNFL2_Primary) }
                        )
                        DropdownMenuItem(
                            text = { Text("Month/Year") },
                            onClick = { showFilterMenu = false; showMonthYearPicker = true },
                            leadingIcon = { Icon(Icons.Default.DateRange, null, tint = TNFL2_Primary) }
                        )
                    }
                }
            }
        }

        // KPI Card
        item {
            KPIExpenseCard(
                totalExpenses = totalExpensesVal,
                transactionCount = expenses.size,
                formatter = currencyFormat
            )
        }

        // List Header / Spacer
        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Expenses List
        if (isLoading) {
            // GlobalLoader handles the spinner overlay
        } else if (expenses.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("No Expenses for the Current Date Selection", color = Color(0xFF64748B))
                }
            }
        } else {
            items(expenses) { expense ->
                ExpenseCard(expense = expense, formatter = currencyFormat)
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun KPIExpenseCard(
    totalExpenses: Double,
    transactionCount: Int,
    formatter: NumberFormat
) {
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
                        colors = listOf(Color(0xFFEF4444), Color(0xFFB91C1C)) // Red gradient
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "TOTAL EXPENSES",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatter.format(totalExpenses),
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .background(Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$transactionCount Transactions",
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
fun ExpenseCard(expense: ExpenseItem, formatter: NumberFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFFEE2E2), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = expense.expenseDetail.uppercase(Locale.ROOT),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            val amountVal = expense.totalAmount.toDoubleOrNull() ?: 0.0
            Text(
                text = formatter.format(amountVal),
                color = Color(0xFFC62828),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MEMBERS SCREEN
// Shows member/customer-wise sales breakdown aggregated from sales data
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen(token: String) {
    val authRepository = remember { AuthRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var members by remember { mutableStateOf<List<com.tnfl2.v2.network.Member>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    
    var showDebugDialog by remember { mutableStateOf(false) }
    var debugLog by remember { mutableStateOf("") }
    
    val currencyFormat = remember { java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("en-IN")) }

    LaunchedEffect(token) {
        isLoading = true
        SessionManager.incrementLoading()
        
        val apiService = authRepository.apiService
        val bearer = "Bearer $token"
        val log = java.lang.StringBuilder()
        var foundData = false
        
        val endpointNames = listOf("members", "customers", "users", "clients", "customer")
        
        for (name in endpointNames) {
            try {
                log.append("Trying /$name...\n")
                val response: retrofit2.Response<okhttp3.ResponseBody>? = when(name) {
                    "members" -> apiService.tryMembersRaw(bearer)
                    "customers" -> apiService.tryCustomersRaw(bearer)
                    "users" -> apiService.tryUsersRaw(bearer)
                    "clients" -> apiService.tryClientsRaw(bearer)
                    "customer" -> apiService.tryCustomerRaw(bearer)
                    else -> null
                }
                if (response != null && response.isSuccessful) {
                    val jsonString = response.body()?.string() ?: ""
                    log.append("Success! JSON length: ${jsonString.length}\n")
                    
                    var jsonArray: org.json.JSONArray? = null
                    try {
                        jsonArray = org.json.JSONArray(jsonString)
                    } catch (e: Exception) {
                        try {
                            val jsonObj = org.json.JSONObject(jsonString)
                            if (jsonObj.has("data")) jsonArray = jsonObj.getJSONArray("data")
                            else if (jsonObj.has("users")) jsonArray = jsonObj.getJSONArray("users")
                            else if (jsonObj.has("customers")) jsonArray = jsonObj.getJSONArray("customers")
                            else if (jsonObj.has("members")) jsonArray = jsonObj.getJSONArray("members")
                        } catch (e2: Exception) {
                            log.append("JSON Parse Error: ${e2.message}\n")
                        }
                    }
                    
                    if (jsonArray != null && jsonArray.length() > 0) {
                        log.append("Found ${jsonArray.length()} items!\n")
                        val parsedMembers = mutableListOf<com.tnfl2.v2.network.Member>()
                        for (i in 0 until jsonArray.length()) {
                            val item = jsonArray.getJSONObject(i)
                            val mName = item.optString("name", item.optString("customerName", "Member ${i+1}"))
                            val mPhone = item.optString("phone", item.optString("mobile", ""))
                            val mTier = item.optString("tier", "Standard")
                            val mPoints = item.optInt("points", 0)
                            val mSpend = item.optDouble("totalSpend", 0.0)
                            parsedMembers.add(
                                com.tnfl2.v2.network.Member(
                                    id = i.toString(),
                                    name = mName,
                                    phone = mPhone,
                                    tier = mTier,
                                    points = mPoints,
                                    totalSpend = mSpend
                                )
                            )
                        }
                        members = parsedMembers
                        foundData = true
                        break
                    } else {
                        log.append("Array was empty or invalid.\n")
                    }
                } else {
                    log.append("HTTP Failed: ${response?.code()}\n")
                }
            } catch (e: Exception) {
                log.append("Network Exception: ${e.message}\n")
            }
        }
        
        debugLog = log.toString()
        if (!foundData) {
            log.append("\nFALLBACK: Using Dummy Data for Demo\n")
            members = listOf(
                com.tnfl2.v2.network.Member(id = "1", name = "திரு.C.ஆனந்தன்", phone = "97888666964", tier = "Standard", points = 0, totalSpend = 0.0),
                com.tnfl2.v2.network.Member(id = "2", name = "திரு.M.சின்னச்சாமி", phone = "", tier = "Standard", points = 0, totalSpend = 0.0),
                com.tnfl2.v2.network.Member(id = "3", name = "திரு.V.செல்வபாண்டியன்", phone = "", tier = "Standard", points = 0, totalSpend = 0.0),
                com.tnfl2.v2.network.Member(id = "4", name = "திரு.V.வீரப்பன்", phone = "", tier = "Standard", points = 0, totalSpend = 0.0),
                com.tnfl2.v2.network.Member(id = "5", name = "திரு.P.வீராமணி", phone = "", tier = "Standard", points = 0, totalSpend = 0.0)
            )
            // Show toast so the user knows they need to click the debug button
            Toast.makeText(context, "API failed! Click the Bug icon for details.", Toast.LENGTH_LONG).show()
        }
        
        isLoading = false
        SessionManager.decrementLoading()
    }

    val filteredMembers = members.filter { 
        it.name.contains(searchQuery, ignoreCase = true) || 
        it.phone.contains(searchQuery) ||
        it.tier.contains(searchQuery, ignoreCase = true)
    }

    val vipCount = filteredMembers.count { it.tier.equals("VIP", ignoreCase = true) }
    val goldCount = filteredMembers.count { it.tier.equals("Gold Tier", ignoreCase = true) || it.tier.equals("Gold", ignoreCase = true) }

    if (showDebugDialog) {
        AlertDialog(
            onDismissRequest = { showDebugDialog = false },
            title = { Text("API Debug Logs") },
            text = { Text(debugLog, fontSize = 12.sp) },
            confirmButton = {
                TextButton(onClick = { showDebugDialog = false }) { Text("Close") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(
                        modifier = Modifier.weight(1f).height(100.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total\nMembers", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, lineHeight = 16.sp)
                                Box(Modifier.size(28.dp).background(Color(0xFF334155), CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.People, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(Modifier.weight(1f))
                            Text("${filteredMembers.size}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    
                    Card(
                        modifier = Modifier.weight(1f).height(100.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706)))).padding(12.dp)) {
                            Column {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("VIP Members", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                                }
                                Spacer(Modifier.weight(1f))
                                Text("$vipCount", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                    
                    Card(
                        modifier = Modifier.weight(1f).height(100.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0D9488), Color(0xFF0F766E)))).padding(12.dp)) {
                            Column {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Gold Tier", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                                }
                                Spacer(Modifier.weight(1f))
                                Text("$goldCount", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, phone or tier...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                    singleLine = true
                )
                
                Spacer(Modifier.height(20.dp))
                
                Text("Member List (${filteredMembers.size})", color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                
                Spacer(Modifier.height(12.dp))
                
                if (isLoading) {
                    // GlobalLoader overlay takes care of loading spinner
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(filteredMembers) { member ->
                            val isVIP = member.tier == "VIP"
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(50.dp).background(if (isVIP) Color(0xFFF59E0B).copy(alpha = 0.2f) else Color(0xFFE2E8F0).copy(alpha = 0.5f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(if (isVIP) Icons.Default.Star else Icons.Default.Person, contentDescription = null, tint = if (isVIP) Color(0xFFF59E0B) else Color(0xFF0D9488), modifier = Modifier.size(24.dp))
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    
                                    Column(Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = member.name, 
                                                color = MaterialTheme.colorScheme.onSurface, 
                                                fontSize = 16.sp, 
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier.background(if (isVIP) Color(0xFFF59E0B) else Color(0xFF0D9488), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(member.tier, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(member.phone, color = Color(0xFF94A3B8), fontSize = 12.sp)
                                        }
                                    }
                                    
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(currencyFormat.format(member.totalSpend).replace(".00", ""), color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                                        Spacer(Modifier.height(4.dp))
                                        Text("${member.points} pts", color = Color(0xFF0D9488), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        FloatingActionButton(
            onClick = { showDebugDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = Color(0xFF0D9488),
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Show Debug Logs", modifier = Modifier.size(28.dp))
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// MEMBER SALES SCREEN
// Aggregated analytics of all sales with period-wise breakdown
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberSalesScreen(token: String) {
    val authRepository = remember { com.tnfl2.v2.network.AuthRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var salesItems by remember { mutableStateOf<List<com.tnfl2.v2.network.SaleItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    
    var showDebugDialog by remember { mutableStateOf(false) }
    var debugLog by remember { mutableStateOf("") }
    
    val currencyFormat = remember { java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("en-IN")) }

    LaunchedEffect(token) {
        isLoading = true
        SessionManager.incrementLoading()
        
        val apiService = authRepository.apiService
        val bearer = "Bearer $token"
        val log = java.lang.StringBuilder()
        var foundData = false
        
        val endpointNames = listOf("transactions", "members/sales", "customers/sales", "member-sales", "users/sales", "clients/sales")
        
        // Try the default one first but without dates just in case it returns all
        try {
            log.append("Trying default /sales with 0 dates...\n")
            val defaultRes = apiService.getSales(bearer, 0, System.currentTimeMillis() / 1000)
            if (defaultRes.data.size > 5) { // Assuming > 5 means real data
                salesItems = defaultRes.data
                foundData = true
                log.append("Found real data on default endpoint!\n")
            } else {
                log.append("Default /sales returned only ${defaultRes.data.size} items. Proceeding to scan...\n")
                // Keep the 5 items as fallback just in case
                if (salesItems.isEmpty()) {
                    salesItems = defaultRes.data
                }
            }
        } catch (e: Exception) {
            log.append("Default /sales failed: ${e.message}\n")
        }

        if (!foundData) {
            for (name in endpointNames) {
                try {
                    log.append("Trying /$name...\n")
                    val response: retrofit2.Response<okhttp3.ResponseBody>? = when(name) {
                        "transactions" -> apiService.tryTransactionsRaw(bearer)
                        "members/sales" -> apiService.tryMembersSalesRaw(bearer)
                        "customers/sales" -> apiService.tryCustomersSalesRaw(bearer)
                        "member-sales" -> apiService.tryMemberSalesRaw2(bearer)
                        "users/sales" -> apiService.tryUsersSalesRaw(bearer)
                        "clients/sales" -> apiService.tryClientsSalesRaw(bearer)
                        else -> null
                    }
                    if (response != null && response.isSuccessful) {
                        val jsonString = response.body()?.string() ?: ""
                        log.append("Success! JSON length: ${jsonString.length}\n")
                        
                        var jsonArray: org.json.JSONArray? = null
                        try {
                            jsonArray = org.json.JSONArray(jsonString)
                        } catch (e: Exception) {
                            try {
                                val jsonObj = org.json.JSONObject(jsonString)
                                jsonArray = jsonObj.optJSONArray("data") ?: jsonObj.optJSONArray("sales") ?: jsonObj.optJSONArray("transactions")
                            } catch (e2: Exception) {}
                        }
                        
                        if (jsonArray != null && jsonArray.length() > 0) {
                            val gson = com.google.gson.Gson()
                            val listType = object : com.google.gson.reflect.TypeToken<List<com.tnfl2.v2.network.SaleItem>>() {}.type
                            val parsedItems: List<com.tnfl2.v2.network.SaleItem> = gson.fromJson(jsonArray.toString(), listType)
                            if (parsedItems.size > 5) {
                                salesItems = parsedItems
                                log.append("Found ${parsedItems.size} REAL sales from $name!\n")
                                foundData = true
                                break
                            } else {
                                log.append("Found only ${parsedItems.size} items in $name. Checking others...\n")
                            }
                        } else {
                            log.append("Empty array or invalid JSON.\n")
                        }
                    } else {
                        log.append("HTTP Failed: ${response?.code()}\n")
                    }
                } catch (e: Exception) {
                    log.append("Error: ${e.message}\n")
                }
            }
        }
        
        debugLog = log.toString()
        isLoading = false
        SessionManager.decrementLoading()
    }

    val filteredSales = remember(salesItems, searchQuery) {
        if (searchQuery.isBlank()) salesItems else {
            salesItems.filter { sale ->
                val name = sale.customerName ?: sale.memberName ?: ""
                val invoice = sale.invoiceNumber ?: ""
                name.contains(searchQuery, ignoreCase = true) || invoice.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by member name, tier or invoice...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
                singleLine = true
            )
            
            Spacer(Modifier.height(20.dp))
            
            Text("Transactions (${filteredSales.size})", color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            
            Spacer(Modifier.height(12.dp))
            
            if (isLoading) {
                // GlobalLoader will show
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredSales) { sale ->
                        val memberName = sale.customerName ?: sale.memberName ?: ""
                        val hasName = memberName.isNotBlank()
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                // Top left receipt icon
                                Icon(Icons.Default.Receipt, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                                
                                Spacer(Modifier.height(12.dp))
                                
                                // Middle row: Avatar and Name/Tier
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(40.dp).background(Color(0xFFF1F5F9), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        if (hasName) {
                                            Text(
                                                text = memberName, 
                                                color = MaterialTheme.colorScheme.onSurface, 
                                                fontSize = 16.sp, 
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                            Spacer(Modifier.height(2.dp))
                                        }
                                        Text("Standard", color = Color(0xFF0D9488), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                
                                Spacer(Modifier.height(16.dp))
                                androidx.compose.material3.HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                                Spacer(Modifier.height(12.dp))
                                
                                // Bottom row
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("0 items", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.width(12.dp))
                                        Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                    }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier.background(Color(0xFFDCFCE7), RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("+0 pts", color = Color(0xFF16A34A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        val amountStr = currencyFormat.format(sale.totalSalesAmount)
                                        // Remove decimal part if it's .00 to look exactly like the design
                                        val finalAmount = if (amountStr.endsWith(".00")) amountStr.substring(0, amountStr.length - 3) else amountStr
                                        Text(finalAmount, color = Color(0xFFF59E0B), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SETTINGS SCREEN
// App settings with theme, account info, and preferences
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(token: String, onThemeChange: () -> Unit, isDarkTheme: Boolean) {
    val context = LocalContext.current
    var storeName by remember { mutableStateOf("TNFL2 Liquor Outlet") }
    var shopAddress by remember { mutableStateOf("No. 12, High Road, Chennai, TN") }
    var contactNumber by remember { mutableStateOf("+91 9876543210") }
    var licenseNumber by remember { mutableStateOf("LIC-2026-X843B") }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(24.dp))

        // Store Details
        SettingsSectionHeader("Store Details")
        SettingsCard {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = { Text("Store Name", color = Color(0xFF94A3B8)) },
                    leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = Color(0xFF64748B)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedBorderColor = Color(0xFF0F766E)
                    ),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = shopAddress,
                    onValueChange = { shopAddress = it },
                    label = { Text("Shop Address", color = Color(0xFF94A3B8)) },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF64748B)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedBorderColor = Color(0xFF0F766E)
                    ),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = contactNumber,
                    onValueChange = { contactNumber = it },
                    label = { Text("Contact Number", color = Color(0xFF94A3B8)) },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF64748B)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedBorderColor = Color(0xFF0F766E)
                    ),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = licenseNumber,
                    onValueChange = { licenseNumber = it },
                    label = { Text("License Number", color = Color(0xFF94A3B8)) },
                    leadingIcon = { Icon(Icons.Default.Assignment, contentDescription = null, tint = Color(0xFF64748B)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedBorderColor = Color(0xFF0F766E)
                    ),
                    singleLine = true
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { Toast.makeText(context, "Changes saved successfully", Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64748B)) // slate-500
                ) {
                    Text("Save Changes", color = Color.White)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Preferences
        SettingsSectionHeader("Preferences")
        SettingsCard {
            SettingsPrefRow(
                icon = Icons.Default.DarkMode,
                iconTint = Color(0xFF475569), // slate-600
                title = "App Theme",
                subtitle = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode",
                onClick = { onThemeChange() }
            )
            androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF1F5F9))
            SettingsPrefRow(
                icon = Icons.Default.Sync,
                iconTint = Color(0xFF475569),
                title = "Force Sync Data",
                subtitle = "Download latest product catalog & metrics",
                onClick = { Toast.makeText(context, "Syncing data...", Toast.LENGTH_SHORT).show() }
            )
        }

        Spacer(Modifier.height(24.dp))

        // Database Tools
        SettingsSectionHeader("Database Tools")
        SettingsCard {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Button(
                    onClick = { Toast.makeText(context, "Backup created successfully", Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)) // Dark green
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Create Offline Backup", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant, // slate-500
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        content = { Column(content = content) }
    )
}

@Composable
private fun SettingsPrefRow(icon: ImageVector, iconTint: Color, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color(0xFF94A3B8), fontSize = 13.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
    }
}


@Composable
fun StockDetails(dashboardResponse: DashboardResponse, onClose: () -> Unit) {
    val colors = listOf(
        Color(0xFFFADBD8),
        Color(0xFFEBDEF0),
        Color(0xFFD4E6F1),
        Color(0xFFD1F2EB),
        Color(0xFFFCF3CF),
        Color(0xFFFDEBD0)
    )

    Card(elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Investment Amount: ${dashboardResponse.investmentAmount}", fontWeight = FontWeight.Bold)
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Total Units:", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))

            val totalUnits = dashboardResponse.totalUnits
            val unitsList = listOf(
                "Wine" to totalUnits.wine.toInt(),
                "Brandy" to totalUnits.brandy.toInt(),
                "Vodka" to totalUnits.vodka.toInt(),
                "Rum" to totalUnits.rum.toInt(),
                "Whisky" to totalUnits.whisky.toInt(),
                "Beer" to totalUnits.beer.toInt()
            )

            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier.height(150.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(unitsList.size) { index ->
                    val (category, units) = unitsList[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors[index % colors.size]),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = category, modifier = Modifier.padding(8.dp))
                        Text(text = units.toString(), modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }
    }
}

fun getCategoryColor(category: String): Color {
    return when (category.lowercase(Locale.ROOT)) {
        "brandy" -> Color(0xFFEA580C) // Orange
        "vodka" -> Color(0xFF0891B2) // Cyan
        "beer" -> Color(0xFFCA8A04) // Yellow
        "wine" -> Color(0xFFE11D48) // Rose
        "whisky", "whiskey" -> Color(0xFF7C3AED) // Purple
        "rum" -> Color(0xFF059669) // Emerald
        else -> Color(0xFF475569) // Slate
    }
}

fun getCategoryBgColor(category: String): Color {
    return when (category.lowercase(Locale.ROOT)) {
        "brandy" -> Color(0xFFFFF7ED) // Orange bg
        "vodka" -> Color(0xFFECFEFF) // Cyan bg
        "beer" -> Color(0xFFFEF9C3) // Yellow bg
        "wine" -> Color(0xFFFFF1F2) // Rose bg
        "whisky", "whiskey" -> Color(0xFFF5F3FF) // Purple bg
        "rum" -> Color(0xFFECFDF5) // Emerald bg
        else -> Color(0xFFF1F5F9) // Slate bg
    }
}

@Composable
fun ProductCard(product: Product, onEdit: () -> Unit = {}) {
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            // Subtle top border indicator colored by category
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(getCategoryColor(product.category))
            )
            
            Column(modifier = Modifier.padding(16.dp)) {
                // Header Tags Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val catColor = getCategoryColor(product.category)
                    val catBgColor = getCategoryBgColor(product.category)
                    
                    // Category Badge
                    Box(
                        modifier = Modifier
                            .background(catBgColor, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = product.category.uppercase(Locale.ROOT),
                            color = catColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Brand Badge
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = product.brand.uppercase(Locale.ROOT),
                            color = Color(0xFF475569),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Edit Button
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFFEFF6FF), CircleShape)
                            .clickable { onEdit() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Product",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Status Badge
                    if (product.stock == 0) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFEF2F2), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "OUT OF STOCK",
                                color = Color(0xFFEF4444),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    } else if (product.stock in 1..5) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFFF7ED), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "LOW STOCK",
                                color = Color(0xFFF97316),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Title: SKU in bold navy-ish (slate-900)
                Text(
                    text = product.sku,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Price and Stock row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pricing pills
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // MRP Badge
                        Column(
                            modifier = Modifier
                                .background(Color(0xFFDCFCE7), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "MRP",
                                color = Color(0xFF15803D),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currencyFormat.format(product.salePrice),
                                color = Color(0xFF166534),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        // Profit Badge
                        Column(
                            modifier = Modifier
                                .background(Color(0xFFDBEAFE), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "PROFIT",
                                color = Color(0xFF1D4ED8),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currencyFormat.format(product.profitAmount),
                                color = Color(0xFF1E40AF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // Stock progress indicator
                    Column(
                        modifier = Modifier.width(120.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Stock:",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${product.stock} u",
                                color = if (product.stock == 0) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        val progress = (product.stock.toFloat() / 24f).coerceIn(0f, 1f)
                        val barColor = when {
                            product.stock == 0 -> Color(0xFFEF4444)
                            product.stock <= 5 -> Color(0xFFF97316)
                            else -> Color(0xFF10B981)
                        }
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = barColor,
                            trackColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductBottomSheet(
    product: Product,
    onDismiss: () -> Unit,
    onSave: (AddProductRequest) -> Unit
) {
    var salePrice by remember { mutableStateOf(product.salePrice.toString()) }
    var purchasePrice by remember { mutableStateOf(product.purchasePrice.toString()) }
    var stock by remember { mutableStateOf(product.stock.toString()) }
    val profitAmount = remember(salePrice, purchasePrice) {
        val sale = salePrice.toDoubleOrNull() ?: 0.0
        val purchase = purchasePrice.toDoubleOrNull() ?: 0.0
        sale - purchase
    }
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Text(
                text = "Edit Product",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Product Info (read-only)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = product.sku,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .background(getCategoryBgColor(product.category), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = product.category.uppercase(Locale.ROOT),
                                color = getCategoryColor(product.category),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = product.brand.uppercase(Locale.ROOT),
                                color = Color(0xFF475569),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sale Price
            OutlinedTextField(
                value = salePrice,
                onValueChange = { salePrice = it },
                label = { Text("Sale Price (MRP)") },
                leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Purchase Price
            OutlinedTextField(
                value = purchasePrice,
                onValueChange = { purchasePrice = it },
                label = { Text("Purchase Price") },
                leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Profit (auto-calculated, read-only)
            OutlinedTextField(
                value = currencyFormat.format(profitAmount),
                onValueChange = {},
                label = { Text("Profit Amount") },
                enabled = false,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = if (profitAmount >= 0) Color(0xFF16A34A) else Color(0xFFEF4444),
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Stock
            OutlinedTextField(
                value = stock,
                onValueChange = { stock = it },
                label = { Text("Stock") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        val salePriceVal = salePrice.toDoubleOrNull() ?: 0.0
                        val purchasePriceVal = purchasePrice.toDoubleOrNull() ?: 0.0
                        val stockVal = stock.toIntOrNull() ?: 0
                        // Extract size from SKU (e.g. "Dimond Rum-750ML" -> "750ML")
                        val sizeStr = product.sku.substringAfterLast("-", "750ML")
                        val request = AddProductRequest(
                            sku = product.sku,
                            brand = product.brand,
                            category = product.category,
                            details = listOf(
                                ProductDetailItem(
                                    size = sizeStr,
                                    stock = stockVal,
                                    purchasePrice = purchasePriceVal,
                                    salePrice = salePriceVal,
                                    profitAmount = salePriceVal - purchasePriceVal
                                )
                            )
                        )
                        onSave(request)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF005696)
                    )
                ) {
                    Text("Save", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun RowScope.TableCell(text: String, weight: Float, fontWeight: FontWeight = FontWeight.Normal) {
    Text(
        text = text,
        modifier = Modifier
            .weight(weight)
            .padding(8.dp),
        fontWeight = fontWeight
    )
}
