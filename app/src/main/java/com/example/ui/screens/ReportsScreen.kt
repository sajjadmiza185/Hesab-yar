package com.example.ui.screens

import androidx.compose.ui.graphics.Color
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
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import com.example.ui.components.BarbergButton as Button
import androidx.compose.material3.ButtonDefaults
import com.example.ui.components.BarbergCard as Card
import androidx.compose.material3.CardDefaults
import com.example.ui.theme.LocalBarbarg3D
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.CustomerAccountSummary
import com.example.ui.theme.BarbargError
import com.example.ui.theme.BarbargSuccess
import com.example.ui.viewmodel.BarbargViewModel
import com.example.util.CsvExporter
import com.example.util.CurrencyFormatter
import com.example.util.JalaliCalendar

@Composable
fun ReportsScreen(
    viewModel: BarbargViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val summaries by viewModel.customerAccountSummaries.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()

    var activeTab by remember { mutableIntStateOf(0) } // 0: ماهانه, 1: بدهکاران
    var selectedMonth by remember(uiState.selectedMonth) { mutableIntStateOf(uiState.selectedMonth) }
    var selectedGroupId by remember { mutableStateOf<Long?>(null) }

    val unit = uiState.currencyUnit
    val persianDigits = uiState.usePersianDigits
    val monthName = JalaliCalendar.getMonthName(selectedMonth)

    val filteredSummaries = remember(summaries, selectedGroupId) {
        if (selectedGroupId == null) summaries
        else summaries.filter { it.customer.groupId == selectedGroupId }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("reports_screen")
    ) {
        // Tab Row: گزارش ماهانه | بدهکاران
        ScrollableTabRow(selectedTabIndex = activeTab) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("گزارش ماهانه") }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("بدهکاران") }
            )
            Tab(
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                text = { Text("گزارش پیامک‌ها") }
            )
        }

        when (activeTab) {
            0 -> MonthlyReportView(
                month = selectedMonth,
                monthName = monthName,
                year = uiState.selectedYear,
                summaries = filteredSummaries,
                groups = groups,
                selectedGroupId = selectedGroupId,
                onMonthChange = { selectedMonth = it },
                onGroupChange = { selectedGroupId = it },
                unit = unit,
                persianDigits = persianDigits,
                onExportCsv = {
                    viewModel.exportReportCsv("MONTHLY") { content, fileName ->
                        CsvExporter.shareCsv(context, content, fileName)
                    }
                }
            )

            1 -> DebtorsReportView(
                summaries = filteredSummaries,
                groups = groups,
                selectedGroupId = selectedGroupId,
                onGroupChange = { selectedGroupId = it },
                unit = unit,
                persianDigits = persianDigits,
                onExportCsv = {
                    viewModel.exportReportCsv("DEBTORS") { content, fileName ->
                        CsvExporter.shareCsv(context, content, fileName)
                    }
                }
            )
            2 -> SmsArchiveView(
                viewModel = viewModel
            )        }
    }
}

@Composable
fun MonthlyReportView(
    month: Int,
    monthName: String,
    year: Int,
    summaries: List<CustomerAccountSummary>,
    groups: List<com.example.model.CustomerGroup>,
    selectedGroupId: Long?,
    onMonthChange: (Int) -> Unit,
    onGroupChange: (Long?) -> Unit,
    unit: String,
    persianDigits: Boolean,
    onExportCsv: () -> Unit
) {
    val totalCharges = summaries.sumOf { it.monthlyCharges[month] ?: 0L }
    val totalPaid = summaries.sumOf { it.monthlyPayments[month] ?: 0L }
    val balance = totalCharges - totalPaid

    Column(modifier = Modifier.fillMaxSize()) {
        // Month Selector
        ScrollableTabRow(
            selectedTabIndex = month - 1,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            (1..12).forEach { m ->
                val isSelected = month == m
                Tab(
                    selected = isSelected,
                    onClick = { onMonthChange(m) },
                    text = {
                        Text(
                            text = JalaliCalendar.getMonthName(m),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        // Summary Bar & Export Button
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = if (LocalBarbarg3D.current) 8.dp else 1.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "گزارش مالی $monthName سال ${CurrencyFormatter.toPersianDigits(year.toString())}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${summaries.size} مشتری در این لیست",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onExportCsv,
                        colors = ButtonDefaults.buttonColors(containerColor = BarbargSuccess),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("export_monthly_csv_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("خروجی اکسل (CSV)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Divider()
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.weight(1f).border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), RoundedCornerShape(10.dp)),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 7.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("کل باربرگ", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                CurrencyFormatter.formatAmount(totalCharges, unit, persianDigits),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 2
                            )
                        }
                    }
                    Box(modifier = Modifier.width(2.dp).height(48.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.8f), RoundedCornerShape(1.dp)))
                    Surface(
                        modifier = Modifier.weight(1f).border(1.dp, BarbargSuccess.copy(alpha = 0.75f), RoundedCornerShape(10.dp)),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 7.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("کل دریافتی", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSecondaryContainer, maxLines = 1)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                CurrencyFormatter.formatAmount(totalPaid, unit, persianDigits),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = BarbargSuccess,
                                maxLines = 2
                            )
                        }
                    }
                    Box(modifier = Modifier.width(2.dp).height(48.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.8f), RoundedCornerShape(1.dp)))
                    Surface(
                        modifier = Modifier.weight(1f).border(1.dp, (if (balance > 0) BarbargError else BarbargSuccess).copy(alpha = 0.8f), RoundedCornerShape(10.dp)),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 7.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("مانده ماه", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onErrorContainer, maxLines = 1)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                CurrencyFormatter.formatAmount(balance, unit, persianDigits),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                color = if (balance > 0) BarbargError else BarbargSuccess,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }

        // List Table of Customers for this month
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(summaries) { index, item ->
                val mCharge = item.monthlyCharges[month] ?: 0L
                val mPaid = item.monthlyPayments[month] ?: 0L
                val mBal = mCharge - mPaid
                val hasMonthCharge = item.monthlyCharges.containsKey(month)
                val statusColor = when {
                    !hasMonthCharge -> Color(0xFF1976D2)
                    mBal > 0 -> Color(0xFFD32F2F)
                    else -> Color(0xFF2E7D32)
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (LocalBarbarg3D.current) 10.dp else 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, statusColor, RoundedCornerShape(10.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Customer identity column
                        Row(
                            modifier = Modifier.weight(1.05f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = CurrencyFormatter.toPersianDigits("${index + 1}."),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.customer.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2
                                )
                                item.group?.let { g ->
                                    Text(
                                        text = g.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // Divider between customer name and money columns
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(46.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.72f), RoundedCornerShape(1.dp))
                        )

                        // Charge and payment column, separated from each other
                        Column(
                            modifier = Modifier.weight(1.25f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "مبلغ: ${CurrencyFormatter.formatAmount(mCharge, unit, persianDigits)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.52f))
                            )
                            Text(
                                text = "پرداختی: ${CurrencyFormatter.formatAmount(mPaid, unit, persianDigits)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = BarbargSuccess,
                                maxLines = 2
                            )
                        }

                        // Divider between charge/payment and balance
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(46.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.72f), RoundedCornerShape(1.dp))
                        )

                        Surface(
                            color = if (mBal > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f)
                            else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.95f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (mBal > 0) "بدهی: " + CurrencyFormatter.formatNumber(mBal, persianDigits)
                                else if (mBal < 0) "بستانکار"
                                else "تسویه",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (mBal > 0) MaterialTheme.colorScheme.onErrorContainer else BarbargSuccess,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DebtorsReportView(
    summaries: List<CustomerAccountSummary>,
    groups: List<com.example.model.CustomerGroup>,
    selectedGroupId: Long?,
    onGroupChange: (Long?) -> Unit,
    unit: String,
    persianDigits: Boolean,
    onExportCsv: () -> Unit
) {
    val debtors = summaries.filter { it.balance > 0 }.sortedByDescending { it.balance }
    val totalDebt = debtors.sumOf { it.balance }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        
    ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("مجموع کل مطالبات و بدهی‌ها", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        CurrencyFormatter.formatAmount(totalDebt, unit, persianDigits),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = BarbargError
                    )
                    Text(
                        "${debtors.size} مشتری دارای مانده بدهی معوق",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = onExportCsv,
                    colors = ButtonDefaults.buttonColors(containerColor = BarbargError),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("export_debtors_csv_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("خروجی بدهکاران", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (debtors.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "تمامی حساب‌ها تسویه هستند و هیچ مشتری بدهکاری وجود ندارد.",
                    color = BarbargSuccess,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(debtors) { index, item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (LocalBarbarg3D.current) 8.dp else 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = CurrencyFormatter.toPersianDigits("${index + 1}."),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        item.customer.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2
                                    )
                                    if (item.customer.phone.isNotBlank()) {
                                        Text(
                                            text = if (persianDigits) CurrencyFormatter.toPersianDigits(item.customer.phone) else item.customer.phone,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(54.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.75f), RoundedCornerShape(1.dp))
                            )

                            Column(
                                modifier = Modifier.weight(1.1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "بدهی: ${CurrencyFormatter.formatAmount(item.balance, unit, persianDigits)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    color = BarbargError,
                                    maxLines = 2
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .height(1.dp)
                                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                )
                                Text(
                                    text = "کل صورتحساب: ${CurrencyFormatter.formatAmount(item.totalCharges, unit, persianDigits)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}





