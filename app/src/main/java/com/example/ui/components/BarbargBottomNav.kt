package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalBarbarg3DStyle
import com.example.ui.viewmodel.AppScreen

data class NavItem(
    val screen: AppScreen,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun BarbargBottomNav(
    currentScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit,
    modifier: Modifier = Modifier,
    blueGloss3D: Boolean = false // Legacy parameter preserved for backward-compatibility
) {
    val style = LocalBarbarg3DStyle.current
    val items = listOf(
        NavItem(
            screen = AppScreen.DASHBOARD,
            title = "داشبورد",
            selectedIcon = Icons.Filled.Dashboard,
            unselectedIcon = Icons.Outlined.Dashboard,
            testTag = "nav_dashboard"
        ),
        NavItem(
            screen = AppScreen.CUSTOMERS,
            title = "مشتریان",
            selectedIcon = Icons.Filled.People,
            unselectedIcon = Icons.Outlined.People,
            testTag = "nav_customers"
        ),
        NavItem(
            screen = AppScreen.MONTHLY_CHARGES,
            title = "مبالغ ماهانه",
            selectedIcon = Icons.Filled.CalendarMonth,
            unselectedIcon = Icons.Outlined.CalendarMonth,
            testTag = "nav_monthly_charges"
        ),
        NavItem(
            screen = AppScreen.TRANSACTIONS,
            title = "تراکنش‌ها",
            selectedIcon = Icons.Filled.ReceiptLong,
            unselectedIcon = Icons.Outlined.ReceiptLong,
            testTag = "nav_transactions"
        ),
        NavItem(
            screen = AppScreen.REPORTS,
            title = "گزارش‌ها",
            selectedIcon = Icons.Filled.Analytics,
            unselectedIcon = Icons.Outlined.Analytics,
            testTag = "nav_reports"
        ),
        NavItem(
            screen = AppScreen.SETTINGS,
            title = "تنظیمات",
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings,
            testTag = "nav_settings"
        )
    )

    val navShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val resolvedBorder = style.navBorder?.let { BorderStroke(1.25.dp, it) }

    Surface(
        modifier = modifier
            .shadow(
                elevation = 16.dp,
                shape = navShape,
                clip = false,
                ambientColor = style.cardAmbientShadow,
                spotColor = style.cardAmbientShadow
            )
            .clip(navShape)
            .background(style.navSurfaceBrush, navShape),
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = navShape,
        border = resolvedBorder
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            windowInsets = androidx.compose.foundation.layout.WindowInsets.navigationBars,
            modifier = Modifier
        ) {
            items.forEach { item ->
                val isSelected = currentScreen == item.screen
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onScreenSelected(item.screen) },
                    icon = {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.title
                        )
                    },
                    label = {
                        Text(
                            text = item.title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag(item.testTag)
                )
            }
        }
    }
}
