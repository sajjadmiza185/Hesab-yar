package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import com.example.ui.components.BarbergButton as Button
import androidx.compose.material3.ButtonDefaults
import com.example.ui.components.BarbergCard as Card
import androidx.compose.material3.CardDefaults
import com.example.ui.theme.LocalBarbarg3D
import androidx.compose.material3.Divider
import com.example.ui.components.BarbergFilledTonalButton as FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.example.ui.components.BarbergOutlinedButton as OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.CustomerAccountSummary
import com.example.ui.theme.BarbargError
import com.example.ui.theme.BarbargSuccess
import com.example.ui.theme.BarbargWarning
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.BarbargViewModel
import com.example.util.CurrencyFormatter
import com.example.util.JalaliCalendar

@Composable
fun DashboardScreen(
    viewModel: BarbargViewModel,
    onNavigate: (AppScreen) -> Unit,
    onOpenCustomerDetail: (Long) -> Unit,
    onOpenPaymentDialog: (customerId: Long?) -> Unit,
    onOpenBatchPaymentDialog: () -> Unit,
    onOpenCustomerDialog: () -> Unit,
    onOpenSmsNotifications: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val summaries by viewModel.customerAccountSummaries.collectAsStateWithLifecycle()
    val monthlyData by viewModel.monthlyBreakdown.collectAsStateWithLifecycle()

    val todayJalali = JalaliCalendar.today()
    val todayFormatted = JalaliCalendar.formatWithDayOfWeek(todayJalali, uiState.usePersianDigits)
    val unit = uiState.currencyUnit
    val persianDigits = uiState.usePersianDigits

    val topDebtors = summaries
        .filter { it.balance > 0 }
        .sortedByDescending { it.balance }
        .take(5)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header & Today date banner
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().then(Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp)))
            
    ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Keep the app title on its own top line.
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = "مدیریت پرداخت مشتریان",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        val pendingSmsCount by viewModel.pendingSmsCount.collectAsStateWithLifecycle()
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            androidx.compose.material3.BadgedBox(badge = {
                                if (pendingSmsCount > 0) androidx.compose.material3.Badge { Text(pendingSmsCount.toString()) }
                            }) {
                                androidx.compose.material3.IconButton(onClick = onOpenSmsNotifications) {
                                    Icon(Icons.Default.Notifications, contentDescription = "اعلان‌ها", tint = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            Text("اعلان‌ها", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    // Date and fiscal year are aligned below the title and use the same font size.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = todayFormatted,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        ) {
                            Text(
                                text = "سال مالی: ${if (persianDigits) CurrencyFormatter.toPersianDigits(uiState.selectedYear.toString()) else uiState.selectedYear.toString()}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4 KPI Summary Cards (Grid)
        item {
            Text(
                text = "خلاصه مالی و وضعیت",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Total Outstanding Debt
                    KpiCard(
                        title = "مانده بدهی کل",
                        amount = CurrencyFormatter.formatAmount(stats.totalOutstandingDebt, unit, persianDigits),
                        icon = Icons.Default.TrendingDown,
                        iconTint = BarbargError,
                        bgColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        subtitle = "${CurrencyFormatter.formatNumber(stats.activeDebtorsCount.toLong(), persianDigits)} مشتری بدهکار",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.setDebtorOnlyFilter(true); viewModel.setGroupFilter(null); onNavigate(AppScreen.CUSTOMERS) }
                    )

                    // Total Payments
                    KpiCard(
                        title = "کل دریافتی‌ها",
                        amount = CurrencyFormatter.formatAmount(stats.totalPaymentsAmount, unit, persianDigits),
                        icon = Icons.Default.TrendingUp,
                        iconTint = BarbargSuccess,
                        bgColor = MaterialTheme.colorScheme.surfaceVariant,
                        subtitle = "وصول شده",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppScreen.TRANSACTIONS) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Total Charges
                    KpiCard(
                        title = "مجموع مبالغ",
                        amount = CurrencyFormatter.formatAmount(stats.totalChargesAmount, unit, persianDigits),
                        icon = Icons.Default.ReceiptLong,
                        iconTint = MaterialTheme.colorScheme.primary,
                        bgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        subtitle = "سال مالی ${if (persianDigits) CurrencyFormatter.toPersianDigits(uiState.selectedYear.toString()) else uiState.selectedYear.toString()}",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppScreen.MONTHLY_CHARGES) }
                    )

                    // Total Customers
                    KpiCard(
                        title = "تعداد مشتریان",
                        amount = "${CurrencyFormatter.formatNumber(stats.totalCustomersCount.toLong(), persianDigits)} نفر",
                        icon = Icons.Default.GroupAdd,
                        iconTint = BarbargWarning,
                        bgColor = MaterialTheme.colorScheme.surfaceVariant,
                        subtitle = "طرف حساب فعال",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.setDebtorOnlyFilter(false); viewModel.setGroupFilter(null); onNavigate(AppScreen.CUSTOMERS) }
                    )
                }
            }
        }

        // Quick Actions Row
        item {
            Text(
                text = "دسترسی سریع",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    title = "ثبت پرداخت",
                    icon = Icons.Default.Payments,
                    color = BarbargSuccess,
                    onClick = { onOpenPaymentDialog(null) },
                    modifier = Modifier.weight(1f),
                    testTag = "quick_action_payment"
                )
                QuickActionButton(
                    title = "پرداخت گروهی",
                    icon = Icons.Default.GroupAdd,
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = onOpenBatchPaymentDialog,
                    modifier = Modifier.weight(1f),
                    testTag = "quick_action_batch_payment"
                )
                QuickActionButton(
                    title = "مشتری جدید",
                    icon = Icons.Default.PersonAdd,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onOpenCustomerDialog,
                    modifier = Modifier.weight(1f),
                    testTag = "quick_action_new_customer"
                )
                QuickActionButton(
                    title = "مبالغ ماهانه",
                    icon = Icons.Default.CalendarMonth,
                    color = BarbargWarning,
                    onClick = { onNavigate(AppScreen.MONTHLY_CHARGES) },
                    modifier = Modifier.weight(1f),
                    testTag = "quick_action_monthly_charges"
                )
            }
        }

        // 12-Month Financial Timeline (Farvardin to Esfand)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "روند ۱۲ ماهه (فروردین تا اسفند)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                FilledTonalButton(
                    onClick = { onNavigate(AppScreen.REPORTS) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("مشاهده گزارش", fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = if (LocalBarbarg3D.current) 10.dp else 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(monthlyData) { m ->
                            val today = JalaliCalendar.today()
                            val isCurrentMonth = uiState.selectedYear == today.year && m.month == today.month
                            val isFutureMonth = uiState.selectedYear > today.year ||
                                (uiState.selectedYear == today.year && m.month > today.month)
                            MonthBarItem(
                                monthSummary = m,
                                isCurrentMonth = isCurrentMonth,
                                showReceivedAmount = !isFutureMonth,
                                unit = unit,
                                persianDigits = persianDigits,
                                onSelect = {
                                    viewModel.setSelectedMonth(m.month)
                                    onNavigate(AppScreen.MONTHLY_CHARGES)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Top Debtors Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "بزرگترین بدهکاران",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (topDebtors.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            viewModel.toggleDebtorOnlyFilter()
                            onNavigate(AppScreen.CUSTOMERS)
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("مشاهده همه بدهکاران", fontSize = 12.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (topDebtors.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                
    ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = if (summaries.isEmpty()) "هنوز هیچ مشتری یا صورتحسابی ثبت نشده است." else "تمامی مشتریان تسویه هستند و بدهی معوقه‌ای وجود ندارد.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    topDebtors.forEach { item ->
                        DebtorRowCard(
                            item = item,
                            unit = unit,
                            persianDigits = persianDigits,
                            onClick = { onOpenCustomerDetail(item.customer.id) },
                            onPayClick = { onOpenPaymentDialog(item.customer.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    amount: String,
    icon: ImageVector,
    iconTint: Color,
    bgColor: Color,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (LocalBarbarg3D.current) 8.dp else 1.dp),
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)).clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun QuickActionButton(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (LocalBarbarg3D.current) 8.dp else 1.dp),
        modifier = modifier
            .clickable(onClick = onClick)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .testTag(testTag)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun MonthBarItem(
    monthSummary: com.example.model.MonthFinancialSummary,
    isCurrentMonth: Boolean,
    showReceivedAmount: Boolean,
    unit: String,
    persianDigits: Boolean,
    onSelect: () -> Unit
) {
    val collectionRate = if (monthSummary.totalCharges > 0) {
        (monthSummary.totalPayments.toFloat() / monthSummary.totalCharges.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (LocalBarbarg3D.current) 6.dp else 1.dp),
        modifier = Modifier
            .width(100.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = monthSummary.monthName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isCurrentMonth) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (showReceivedAmount) CurrencyFormatter.formatAmount(monthSummary.totalPayments, unit, persianDigits) else "—",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (showReceivedAmount) BarbargSuccess else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (showReceivedAmount) "دریافتی واقعی" else "ماه آینده",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))
            if (showReceivedAmount) {
                LinearProgressIndicator(
                    progress = { collectionRate },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = BarbargSuccess,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${(collectionRate * 100).toInt()}% وصول",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 9.sp,
                    color = BarbargSuccess
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun DebtorRowCard(
    item: CustomerAccountSummary,
    unit: String,
    persianDigits: Boolean,
    onClick: () -> Unit,
    onPayClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (LocalBarbarg3D.current) 8.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.customer.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    item.group?.let { g ->
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = g.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "بدهی: ${CurrencyFormatter.formatAmount(item.balance, unit, persianDigits)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = BarbargError
                )
            }

            Button(
                onClick = onPayClick,
                colors = ButtonDefaults.buttonColors(containerColor = BarbargSuccess),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("تسویه", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}


