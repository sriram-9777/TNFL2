package com.tnfl2.v2

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class NavItem(val title: String, val icon: ImageVector, val iconColor: Color)

val navItems = listOf(
    NavItem("Dashboard", Icons.Default.Dashboard, Color(0xFFB75C1C)),
    NavItem("Products", Icons.Default.Inventory2, Color(0xFFA855F7)),
    NavItem("Purchases", Icons.Default.ShoppingCart, Color(0xFF3B82F6)),
    NavItem("Sales", Icons.AutoMirrored.Filled.ReceiptLong, Color(0xFF14B8A6)),
    NavItem("Expenses", Icons.Default.Payments, Color(0xFFF43F5E)),
    NavItem("Members", Icons.Default.People, Color(0xFF0EA5E9)),
    NavItem("Accounts", Icons.Default.AccountBalanceWallet, Color(0xFF10B981)),
    NavItem("POS", Icons.Default.Monitor, Color(0xFF06B6D4)),
    NavItem("Member Sales", Icons.AutoMirrored.Filled.Assignment, Color(0xFFF59E0B)),
    NavItem("Settings", Icons.Default.Settings, Color(0xFF64748B))
)

@Composable
fun NavigationDrawer(selectedItem: NavItem, onNavItemClicked: (NavItem) -> Unit) {
    val isDark = com.tnfl2.v2.ui.theme.LocalThemeIsDark.current

    // Dynamic surface and text colors that swap per theme
    val drawerBg = MaterialTheme.colorScheme.surface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val selectedBg = if (isDark) Color(0xFF2A1F18) else Color(0xFFF7ECE4)
    val datePillBg = if (isDark) Color(0xFF2A1F18) else Color(0xFFF5EFEB)
    val selectedIconBg = if (isDark) Color(0xFF3D2B1F) else Color.White
    val dividerColor = MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(drawerBg)
            .padding(16.dp)
    ) {
        // Drawer Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp, top = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Circular Logo
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .border(BorderStroke(1.5.dp, Color(0xFFB75C1C)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalBar,
                        contentDescription = "Logo",
                        tint = Color(0xFFB75C1C),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "TNFL2",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFB75C1C)
                    )
                    Text(
                        text = "Liquor Management",
                        fontSize = 12.sp,
                        color = textSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date Pill Badge
            val currentDateText = remember {
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                sdf.format(Date())
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(datePillBg)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = Color(0xFFB75C1C),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = currentDateText,
                        color = Color(0xFFB75C1C),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Menu Items (scrollable)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            navItems.forEach { item ->
                val isSelected = item.title == selectedItem.title

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) selectedBg else Color.Transparent)
                        .clickable { onNavItemClicked(item) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(20.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color(0xFFB75C1C))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) selectedIconBg else Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = if (isSelected) Color(0xFFB75C1C) else item.iconColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = item.title,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else textSecondary,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                            )
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFB75C1C))
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = dividerColor)
        Spacer(modifier = Modifier.height(16.dp))

        // Logout button
        val logoutItem = NavItem("Logout", Icons.AutoMirrored.Filled.Logout, Color(0xFFEF4444))
        val logoutBg = if (isDark) Color(0xFF2A1212) else Color(0xFFFEF2F2)
        val logoutBorder = if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.5f) else Color(0xFFFCA5A5).copy(alpha = 0.5f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(BorderStroke(1.dp, logoutBorder), RoundedCornerShape(12.dp))
                .background(logoutBg)
                .clickable { onNavItemClicked(logoutItem) }
                .padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Logout",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Logout",
                    color = Color(0xFFEF4444),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
