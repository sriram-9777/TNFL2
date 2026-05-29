package com.tnfl2.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tnfl2.v2.ui.theme.LocalThemeIsDark
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onLogout: () -> Unit, token: String, onThemeChange: () -> Unit, isDarkTheme: Boolean = false) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var selectedNavItem by remember { mutableStateOf<NavItem>(navItems.first()) }
    val scope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf<String>("Dashboard") }
    var productScreenRefreshKey by remember { mutableStateOf(0) }
    var salesDetailsJson by remember { mutableStateOf("") }
    var triggerPosCart by remember { mutableStateOf(false) }

    val isDark = LocalThemeIsDark.current

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NavigationDrawer(selectedItem = selectedNavItem) {
                selectedNavItem = it
                currentScreen = it.title
                scope.launch { drawerState.close() }
            }
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                val isDashboard = currentScreen == "Dashboard"
                val isProducts = currentScreen == "Products"
                val isPurchases = currentScreen == "Purchases"
                val isSales = currentScreen == "Sales"
                val isExpenses = currentScreen == "Expenses"
                val isMembers = currentScreen == "Members"
                val isPOS = currentScreen == "POS"
                val isMemberSales = currentScreen == "Member Sales"
                val isSettings = currentScreen == "Settings"
                val isAccounts = currentScreen == "Accounts"
                val isCustomHeader = isDashboard || isProducts || isPurchases || isSales || isExpenses || isMembers || isPOS || isMemberSales || isSettings || isAccounts

                // In dark mode use a deeper teal; in light mode use the original green gradient
                val topBarColors = if (isDark)
                    listOf(Color(0xFF0D5E58), Color(0xFF083E3A))
                else
                    listOf(Color(0xFF0F766E), Color(0xFF0B5E56))

                val backgroundModifier = if (isCustomHeader) {
                    Modifier.background(brush = Brush.horizontalGradient(colors = topBarColors))
                } else {
                    Modifier
                }
                
                Box(modifier = backgroundModifier) {
                    TopAppBar(
                        title = {
                            if (isCustomHeader) {
                                Column {
                                    Text(
                                        text = when {
                                            isDashboard -> "Dashboard"
                                            isProducts -> "Products"
                                            isPurchases -> "Purchases"
                                            isSales -> "Sales"
                                            isExpenses -> "Expenses"
                                            isMembers -> "Members"
                                            isPOS -> "POS"
                                            isMemberSales -> "Member Sales"
                                            isSettings -> "Settings"
                                            isAccounts -> "Accounts"
                                            else -> currentScreen
                                        },
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    )
                                    Text(
                                        text = when {
                                            isDashboard -> "Business overview"
                                            isProducts -> "Manage your inventory & stock"
                                            isPurchases -> "Track your purchase records"
                                            isSales -> "Track your sales performance"
                                            isExpenses -> "Track your business expenses"
                                            isMembers -> "Manage your customers & membership program"
                                            isPOS -> "Quick order entry & billing"
                                            isMemberSales -> "Analytics & weekly breakdown"
                                            isSettings -> "App preferences & account info"
                                            isAccounts -> "Track your payments & dues"
                                            else -> ""
                                        },
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                Text(currentScreen)
                            }
                        },

                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Menu",
                                    tint = if (isCustomHeader) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        actions = {
                            if (isCustomHeader) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // TNFL2 badge
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0x22FFFFFF))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "TNFL2",
                                            color = Color(0xFFFACC15),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    if (isPOS) {
                                        IconButton(onClick = { triggerPosCart = true }) {
                                            Icon(
                                                imageVector = Icons.Default.ShoppingCart,
                                                contentDescription = "Cart",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                    IconButton(onClick = onThemeChange) {
                                        Icon(
                                            imageVector = if (isDark) Icons.Default.WbSunny else Icons.Default.NightsStay,
                                            contentDescription = "Toggle theme",
                                            tint = Color.White
                                        )
                                    }
                                }
                            } else {
                                IconButton(onClick = onThemeChange) {
                                    Icon(
                                        imageVector = if (isDark) Icons.Default.WbSunny else Icons.Default.NightsStay,
                                        contentDescription = "Toggle theme"
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = if (isCustomHeader) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when (currentScreen) {
                    "Dashboard" -> DashboardScreen(token = token)
                    "Products" -> ProductsScreen(token = token, refreshKey = productScreenRefreshKey, onAddProduct = { currentScreen = "Add Product" })
                    "Add Product" -> AddProductScreen(token = token, onProductAdded = { currentScreen = "Products" }, onCancel = { currentScreen = "Products" })
                    "Purchases" -> PurchasesScreen(token = token, onAddPurchase = { currentScreen = "Add Purchase" })
                    "Add Purchase" -> AddPurchaseScreen(token = token, onNavigateBack = { currentScreen = "Purchases" })
                    "Sales" -> SalesScreen(token = token, onAddSale = { currentScreen = "Add Sale" }, onSaleConfirmed = { currentScreen = "Sales" })
                    "Add Sale" -> AddSaleScreen(token = token, onCancel = { currentScreen = "Sales" }, onSaleConfirmed = { currentScreen = "Sales" })
                    "Expenses" -> ExpensesScreen(token = token)
                    "Accounts" -> AccountsScreen(token = token, onShowDetails = { json -> 
                        salesDetailsJson = json
                        currentScreen = "Sales Details"
                    })
                    "Sales Details" -> SalesDetailViewOnly(salesDetailsJson = salesDetailsJson, onBack = { currentScreen = "Accounts" })
                    "Members" -> MembersScreen(token = token)
                    "POS" -> POSScreen(token = token, showCartTrigger = triggerPosCart, onCartHandled = { triggerPosCart = false })
                    "Member Sales" -> MemberSalesScreen(token = token)
                    "Settings" -> SettingsScreen(token = token, onThemeChange = onThemeChange, isDarkTheme = isDarkTheme)
                    "Logout" -> onLogout()
                }
            }
        }
    }
}
