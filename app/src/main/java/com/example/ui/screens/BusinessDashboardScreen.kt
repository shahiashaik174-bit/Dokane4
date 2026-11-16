package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.GlassCard
import com.example.ui.components.PlainEmptyState
import com.example.ui.components.SalesBarChart
import com.example.ui.theme.AlertRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividGreen
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.BusinessNavDestination
import com.example.ui.viewmodel.HistoryPeriod
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.AppLanguage
import com.example.util.ShopLocale
import java.util.Locale

/**
 * Main Business tab screen router.
 */
@Composable
fun BusinessScreen(
    authViewModel: AuthViewModel,
    storeViewModel: StoreViewModel,
    modifier: Modifier = Modifier
) {
    val destination by storeViewModel.businessNavDestination.collectAsState()

    BackHandler(enabled = destination != BusinessNavDestination.DASHBOARD) {
        storeViewModel.navigateBackInBusiness()
    }

    if (destination == BusinessNavDestination.DASHBOARD) {
        val canReports by storeViewModel.canViewReportsFlow.collectAsState()
        if (!canReports) {
            com.example.ui.components.NoPermissionScreen(onBack = null, modifier = modifier)
        } else {
            BusinessDashboardScreen(
                storeViewModel = storeViewModel,
                modifier = modifier
            )
        }
    } else {
        BusinessDestinationContent(
            destination = destination,
            authViewModel = authViewModel,
            storeViewModel = storeViewModel,
            modifier = modifier
        )
    }
}

@Composable
fun BusinessDestinationContent(
    destination: BusinessNavDestination,
    authViewModel: AuthViewModel,
    storeViewModel: StoreViewModel,
    modifier: Modifier = Modifier
) {
    val isOwner by storeViewModel.isOwner.collectAsState()
    val isFatherMode by storeViewModel.isFatherMode.collectAsState()
    val canReports by storeViewModel.canViewReportsFlow.collectAsState()
    val canSeeProfit by storeViewModel.canSeeBuyingPriceAndProfitFlow.collectAsState()
    val canBaki by storeViewModel.canAccessCustomersAndBakiFlow.collectAsState()
    val canSuppliers by storeViewModel.canAccessSuppliersAndPurchasesFlow.collectAsState()
    val canStock by storeViewModel.canViewStockFlow.collectAsState()
    val canHistory by storeViewModel.canSeeHistoryFlow.collectAsState()
    val canCashBank by storeViewModel.canAccessCashAndBankFlow.collectAsState()
    val canExpenses by storeViewModel.canAccessExpensesAndIncomeFlow.collectAsState()
    val canNotes by storeViewModel.canAccessNotesFlow.collectAsState()

    val isDestinationAllowed = when (destination) {
        BusinessNavDestination.DASHBOARD -> canReports
        BusinessNavDestination.FINANCIAL_REPORTS_HUB -> canReports
        BusinessNavDestination.BAKI_SCREEN -> canBaki
        BusinessNavDestination.SUPPLIERS_SCREEN -> canSuppliers && !isFatherMode
        BusinessNavDestination.LOW_STOCK_SCREEN -> canStock && canReports
        BusinessNavDestination.SLOW_SELLERS_SCREEN -> canStock && canReports && !isFatherMode
        BusinessNavDestination.REPORT_SALES -> canReports && canHistory
        BusinessNavDestination.REPORT_PURCHASES -> canReports && canSuppliers && !isFatherMode
        BusinessNavDestination.REPORT_DAY_BOOK -> canReports && !isFatherMode
        BusinessNavDestination.REPORT_PROFIT_LOSS -> canReports && canSeeProfit
        BusinessNavDestination.REPORT_CUSTOMER_STATEMENT -> canReports && canBaki && !isFatherMode
        BusinessNavDestination.REPORT_SUPPLIER_STATEMENT -> canReports && canSuppliers && !isFatherMode
        BusinessNavDestination.REPORT_INVENTORY -> canReports && canStock
        BusinessNavDestination.MONTHLY_REPORT -> canReports && canSeeProfit
        BusinessNavDestination.DATA_HEALTH_CHECK -> isOwner && !isFatherMode
        BusinessNavDestination.TEST_CHECKLIST -> isOwner && !isFatherMode
        BusinessNavDestination.CASH_BANK_SCREEN -> canCashBank && !isFatherMode
        BusinessNavDestination.ACCOUNT_DETAILS_SCREEN -> canCashBank && !isFatherMode
        BusinessNavDestination.EXPENSES_SCREEN -> canExpenses && !isFatherMode
        BusinessNavDestination.OTHER_INCOMES_SCREEN -> canExpenses && !isFatherMode
        BusinessNavDestination.MANAGE_CATEGORIES_SCREEN -> canExpenses && !isFatherMode
        BusinessNavDestination.NOTES_SCREEN -> canNotes && !isFatherMode
        BusinessNavDestination.REPORT_EXPENSES -> canReports && canExpenses && !isFatherMode
        BusinessNavDestination.REPORT_INCOMES -> canReports && canExpenses && !isFatherMode
        BusinessNavDestination.STAFF_MANAGEMENT_SCREEN -> isOwner && !isFatherMode
        BusinessNavDestination.STAFF_MEMBER_DETAILS_SCREEN -> isOwner && !isFatherMode
        BusinessNavDestination.REPORT_STAFF -> canReports && !isFatherMode
        BusinessNavDestination.GREETING_CARDS_SCREEN -> !isFatherMode
    }

    if (!isDestinationAllowed) {
        com.example.ui.components.NoPermissionScreen(
            onBack = if (destination != BusinessNavDestination.DASHBOARD) {
                { storeViewModel.navigateBackInBusiness() }
            } else null,
            modifier = modifier
        )
        return
    }

    when (destination) {
        BusinessNavDestination.DASHBOARD -> {
            BusinessDashboardScreen(
                storeViewModel = storeViewModel,
                modifier = modifier
            )
        }
        BusinessNavDestination.FINANCIAL_REPORTS_HUB -> {
            FinancialReportsHubScreen(
                storeViewModel = storeViewModel,
                modifier = modifier
            )
        }
        BusinessNavDestination.BAKI_SCREEN -> {
            BakiMainScreen(
                storeViewModel = storeViewModel,
                modifier = modifier
            )
        }
        BusinessNavDestination.SUPPLIERS_SCREEN -> {
            SuppliersScreen(
                storeViewModel = storeViewModel,
                modifier = modifier
            )
        }
        BusinessNavDestination.LOW_STOCK_SCREEN -> {
            LowStockScreen(
                storeViewModel = storeViewModel,
                modifier = modifier
            )
        }
        BusinessNavDestination.SLOW_SELLERS_SCREEN -> {
            SlowSellersScreen(
                storeViewModel = storeViewModel,
                modifier = modifier
            )
        }
        BusinessNavDestination.REPORT_SALES -> {
            SalesReportScreen(
                storeViewModel = storeViewModel,
                modifier = modifier
            )
        }
        BusinessNavDestination.REPORT_PURCHASES -> {
            PurchaseReportScreen(
                storeViewModel = storeViewModel,
                modifier = modifier
            )
        }
        BusinessNavDestination.REPORT_DAY_BOOK -> {
            DayBookScreen(
                storeViewModel = storeViewModel,
                modifier = modifier
            )
        }
        BusinessNavDestination.REPORT_PROFIT_LOSS -> {
            ProfitAndLossScreen(
                storeViewModel = storeViewModel,
                modifier = modifier
            )
        }
        BusinessNavDestination.REPORT_CUSTOMER_STATEMENT -> {
            CustomerStatementScreen(
                storeViewModel = storeViewModel,
                modifier = modifier
            )
        }
        BusinessNavDestination.REPORT_SUPPLIER_STATEMENT -> {
            SupplierStatementScreen(
                storeViewModel = storeViewModel,
                modifier = modifier
            )
        }
        BusinessNavDestination.REPORT_INVENTORY -> {
            InventoryReportScreen(
                storeViewModel = storeViewModel,
                modifier = modifier
            )
        }
        BusinessNavDestination.MONTHLY_REPORT -> {
            MonthlyReportScreen(
                storeViewModel = storeViewModel,
                modifier = modifier
            )
        }
        BusinessNavDestination.DATA_HEALTH_CHECK -> {
            DataHealthCheckScreen(
                authViewModel = authViewModel,
                storeViewModel = storeViewModel,
                modifier = modifier
            )
        }
        BusinessNavDestination.TEST_CHECKLIST -> {
            TestChecklistScreen(
                storeViewModel = storeViewModel,
                modifier = modifier
            )
        }
        BusinessNavDestination.CASH_BANK_SCREEN -> {
            CashAndBankScreen(
                storeViewModel = storeViewModel,
                modifier = modifier
            )
        }
        BusinessNavDestination.ACCOUNT_DETAILS_SCREEN -> {
            AccountDetailsScreen(
                storeViewModel = storeViewModel,
                modifier = modifier
            )
        }
        BusinessNavDestination.EXPENSES_SCREEN -> {
            ExpensesScreen(
                storeViewModel = storeViewModel,
                modifier = modifier
            )
        }
        BusinessNavDestination.OTHER_INCOMES_SCREEN -> {
            OtherIncomesScreen(
                storeViewModel = storeViewModel,
                modifier = modifier
            )
        }
        BusinessNavDestination.MANAGE_CATEGORIES_SCREEN -> {
            ManageCategoriesScreen(
                storeViewModel = storeViewModel,
                modifier = modifier
            )
        }
        BusinessNavDestination.NOTES_SCREEN -> {
            StoreNotesScreen(
                storeViewModel = storeViewModel,
                modifier = modifier
            )
        }
        BusinessNavDestination.REPORT_EXPENSES -> {
            ExpenseReportScreen(
                storeViewModel = storeViewModel,
                modifier = modifier
            )
        }
        BusinessNavDestination.REPORT_INCOMES -> {
            IncomeReportScreen(
                storeViewModel = storeViewModel,
                modifier = modifier
            )
        }
        BusinessNavDestination.STAFF_MANAGEMENT_SCREEN -> {
            StaffManagementScreen(
                storeViewModel = storeViewModel,
                modifier = modifier
            )
        }
        BusinessNavDestination.STAFF_MEMBER_DETAILS_SCREEN -> {
            StaffMemberDetailsScreen(
                storeViewModel = storeViewModel,
                modifier = modifier
            )
        }
        BusinessNavDestination.REPORT_STAFF -> {
            StaffReportScreen(
                storeViewModel = storeViewModel,
                modifier = modifier
            )
        }
        BusinessNavDestination.GREETING_CARDS_SCREEN -> {
            GreetingCardsScreen(
                storeViewModel = storeViewModel,
                modifier = modifier
            )
        }
    }
}

/**
 * Step 2: Business Tab Dashboard (restyled dark glass + glowing emerald aesthetic, business data only)
 */
@Composable
fun BusinessDashboardScreen(
    storeViewModel: StoreViewModel,
    modifier: Modifier = Modifier
) {
    val currentLanguage by storeViewModel.currentLanguage.collectAsState()
    val period by storeViewModel.businessPeriod.collectAsState()
    val customRange by storeViewModel.businessCustomDateRange.collectAsState()
    val isSimpleMode by storeViewModel.isSimpleMode.collectAsState()
    val isFatherMode by storeViewModel.isFatherMode.collectAsState()

    val canSeeProfit by storeViewModel.canSeeBuyingPriceAndProfitFlow.collectAsState()
    val canBaki by storeViewModel.canAccessCustomersAndBakiFlow.collectAsState()
    val canSuppliers by storeViewModel.canAccessSuppliersAndPurchasesFlow.collectAsState()
    val canCashBank by storeViewModel.canAccessCashAndBankFlow.collectAsState()
    val canExpenses by storeViewModel.canAccessExpensesAndIncomeFlow.collectAsState()
    val canStock by storeViewModel.canViewStockFlow.collectAsState()
    val canHistory by storeViewModel.canSeeHistoryFlow.collectAsState()

    val showSuppliersSection = canSuppliers && !isFatherMode
    val showCashBankSection = canCashBank && !isFatherMode
    val showExpensesSection = canExpenses && !isFatherMode

    val financials by storeViewModel.businessFinancials.collectAsState()
    val periodExpenses by storeViewModel.businessFilteredExpenses.collectAsState()
    val moneyNow by storeViewModel.moneyNow.collectAsState()
    val totalBalance by storeViewModel.totalBalance.collectAsState()
    val chartPoints by storeViewModel.salesChartPoints.collectAsState()
    val topProducts by storeViewModel.topSellingProducts.collectAsState()
    val categoryStats by storeViewModel.salesByCategory.collectAsState()
    val slowSellers by storeViewModel.slowSellingProducts.collectAsState()
    val lowStockList by storeViewModel.lowStockProducts.collectAsState()

    val activeExpenses = remember(periodExpenses) { periodExpenses.filter { !it.isDeleted } }
    val periodExpensesTotal = remember(activeExpenses) { activeExpenses.sumOf { it.amount } }

    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        CustomDateRangePickerDialog(
            initialRange = customRange,
            currentLanguage = currentLanguage,
            onDismiss = { showDatePicker = false },
            onConfirm = { start, end ->
                storeViewModel.setBusinessCustomDateRange(start, end)
                showDatePicker = false
            }
        )
    }

    val effectivePeriod = if (isSimpleMode && period == HistoryPeriod.CUSTOM) HistoryPeriod.TODAY else period
    val periodHeaderTag = when (effectivePeriod) {
        HistoryPeriod.TODAY -> if (currentLanguage == AppLanguage.BN) "আজকে" else "TODAY"
        HistoryPeriod.WEEK -> if (currentLanguage == AppLanguage.BN) "৭ দিন" else "THIS WEEK"
        HistoryPeriod.MONTH -> if (currentLanguage == AppLanguage.BN) "এই মাস" else "THIS MONTH"
        HistoryPeriod.YEAR -> if (currentLanguage == AppLanguage.BN) "এই বছর" else "THIS YEAR"
        HistoryPeriod.ALL -> if (currentLanguage == AppLanguage.BN) "সর্বমোট" else "ALL TIME"
        HistoryPeriod.CUSTOM -> if (currentLanguage == AppLanguage.BN) "নির্দিষ্ট সময়" else "CUSTOM"
    }

    val amberTint = Color(0xFFFFB300)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070907))
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            VividGreen.copy(alpha = 0.16f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.25f, size.height * 0.08f),
                        radius = size.width * 0.85f
                    )
                )
            }
            .testTag("business_dashboard_list"),
        contentPadding = PaddingValues(top = 2.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Header: "Business" + "Full Reports ->" pill
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("business_header_row"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (currentLanguage == AppLanguage.BN) "ব্যবসা" else "Business",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )

                Surface(
                    onClick = { storeViewModel.navigateInBusiness(BusinessNavDestination.FINANCIAL_REPORTS_HUB) },
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xEB1A1F1A),
                    border = BorderStroke(1.dp, Color(0x36FFFFFF)),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 36.dp)
                        .testTag("business_full_reports_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = if (currentLanguage == AppLanguage.BN) "পূর্ণ রিপোর্ট" else "Full Reports",
                            color = TextPrimary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }

        // 2. Period Capsule Tabs
        item {
            HistoryPeriodTabs(
                currentPeriod = effectivePeriod,
                customRange = customRange,
                currentLanguage = currentLanguage,
                includeCustomDate = !isSimpleMode,
                onSelectPeriod = { selected ->
                    if (selected == HistoryPeriod.CUSTOM) {
                        showDatePicker = true
                    } else {
                        storeViewModel.setBusinessPeriod(selected)
                    }
                }
            )
        }

        // 3. Glowing Hero Profit / Sales Summary Card
        item {
            val heroShape = RoundedCornerShape(22.dp)
            val marginPct = if (financials.totalSold > 0.0) {
                (financials.totalProfit / financials.totalSold) * 100.0
            } else 0.0
            val heroValueStr = if (canSeeProfit) {
                ShopLocale.formatMoney(financials.totalProfit, currentLanguage)
            } else {
                ShopLocale.formatMoney(financials.totalSold, currentLanguage)
            }
            val heroFontSize = when {
                heroValueStr.length <= 7 -> 28.sp
                heroValueStr.length <= 10 -> 24.sp
                else -> 20.sp
            }

            Surface(
                shape = heroShape,
                color = Color.Transparent,
                border = BorderStroke(1.4.dp, VividGreen.copy(alpha = 0.78f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .shadow(
                        elevation = 18.dp,
                        shape = heroShape,
                        spotColor = VividGreen.copy(alpha = 0.42f),
                        ambientColor = VividGreen.copy(alpha = 0.22f)
                    )
                    .clip(heroShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF234715), Color(0xFF0E160B))
                        )
                    )
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(enabled = canSeeProfit && (!isSimpleMode || isFatherMode)) {
                                storeViewModel.navigateInBusiness(BusinessNavDestination.REPORT_PROFIT_LOSS)
                            }
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (canSeeProfit) {
                                    if (currentLanguage == AppLanguage.BN) "লাভ · $periodHeaderTag" else "PROFIT · $periodHeaderTag"
                                } else {
                                    if (currentLanguage == AppLanguage.BN) "বিক্রয় · $periodHeaderTag" else "SALES · $periodHeaderTag"
                                },
                                color = Color(0xFFD6E2D2),
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.8.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (canSeeProfit && (!isSimpleMode || isFatherMode)) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = heroValueStr,
                                color = if (!canSeeProfit || financials.totalProfit >= 0.0) VividGreen else AlertRed,
                                fontSize = heroFontSize,
                                lineHeight = (heroFontSize.value + 4f).sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = if (canSeeProfit) Modifier.testTag("business_kpi_profit") else Modifier
                            )

                            if (canSeeProfit) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0x2E5DD62C),
                                    border = BorderStroke(1.dp, VividGreen.copy(alpha = 0.45f))
                                ) {
                                    Text(
                                        text = "▲ ${String.format(Locale.US, "%.1f", marginPct)}%",
                                        color = VividGreen,
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0x1EFFFFFF))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HeroSubMetric(
                                label = if (currentLanguage == AppLanguage.BN) "মোট বিক্রয়" else "Total Sales",
                                value = ShopLocale.formatMoney(financials.totalSold, currentLanguage),
                                modifier = Modifier
                                    .weight(1.1f)
                                    .testTag("business_kpi_sales")
                            )
                            if (showExpensesSection) {
                                HeroSubMetric(
                                    label = if (currentLanguage == AppLanguage.BN) "মোট খরচ" else "Total Expenses",
                                    value = ShopLocale.formatMoney(periodExpensesTotal, currentLanguage),
                                    modifier = Modifier.weight(1.1f)
                                )
                            }
                            HeroSubMetric(
                                label = if (currentLanguage == AppLanguage.BN) "অর্ডার" else "Orders",
                                value = ShopLocale.formatNumber(financials.totalOrders, currentLanguage),
                                modifier = Modifier
                                    .weight(0.7f)
                                    .testTag("business_kpi_orders")
                            )
                        }
                    }
                }
            }
        }

        // 4. Customer Baki & Supplier Due Side-by-Side Row
        if (canBaki || showSuppliersSection) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (canBaki) {
                        BusinessSplitDueCard(
                            title = if (currentLanguage == AppLanguage.BN) "ক্রেতার বাকি" else "CUSTOMER BAKI",
                            amount = ShopLocale.formatMoney(moneyNow.bakiCustomersOwe, currentLanguage),
                            subtitle = if (currentLanguage == AppLanguage.BN) {
                                "${ShopLocale.formatNumber(moneyNow.debtorsCount, currentLanguage)} জন ক্রেতা"
                            } else {
                                "${moneyNow.debtorsCount} customer${if (moneyNow.debtorsCount == 1L) "" else "s"}"
                            },
                            accentColor = amberTint,
                            testTag = "card_open_baki",
                            onClick = { storeViewModel.navigateInBusiness(BusinessNavDestination.BAKI_SCREEN) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                    if (showSuppliersSection) {
                        BusinessSplitDueCard(
                            title = if (currentLanguage == AppLanguage.BN) "মহাজন দেনা" else "SUPPLIER DUE",
                            amount = ShopLocale.formatMoney(moneyNow.debtToSuppliers, currentLanguage),
                            subtitle = if (currentLanguage == AppLanguage.BN) {
                                "${ShopLocale.formatNumber(moneyNow.suppliersCount, currentLanguage)} জন মহাজন"
                            } else {
                                "${moneyNow.suppliersCount} supplier${if (moneyNow.suppliersCount == 1L) "" else "s"}"
                            },
                            accentColor = if (moneyNow.debtToSuppliers > 0.0) amberTint else TextSecondary,
                            testTag = "card_open_suppliers",
                            onClick = { storeViewModel.navigateInBusiness(BusinessNavDestination.SUPPLIERS_SCREEN) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }

        // 5. MONEY NOW Ledger Card (business balances only)
        if (showCashBankSection || canSeeProfit || canBaki || showSuppliersSection) {
            item {
                BusinessDarkGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("money_now_card")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (currentLanguage == AppLanguage.BN) "বর্তমান স্থিতি" else "MONEY NOW",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = if (canSeeProfit) {
                                    if (currentLanguage == AppLanguage.BN) "হিসাব সারসংক্ষেপ" else "Live position"
                                } else "",
                                color = TextMuted,
                                fontSize = 10.5.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        if (showCashBankSection) {
                            MoneyNowLedgerRow(
                                label = if (currentLanguage == AppLanguage.BN) "ক্যাশ ও ব্যাংক" else "Cash + Bank",
                                hint = null,
                                value = ShopLocale.formatMoney(totalBalance, currentLanguage),
                                valueColor = VividGreen
                            )
                        }
                        if (canSeeProfit) {
                            MoneyNowLedgerRow(
                                label = if (currentLanguage == AppLanguage.BN) "মজুদ মূল্য" else "Stock Value",
                                hint = if (currentLanguage == AppLanguage.BN) "(ক্রয়মূল্যে)" else "(at cost)",
                                value = ShopLocale.formatMoney(moneyNow.stockValueAtCost, currentLanguage),
                                valueColor = TextPrimary
                            )
                        }
                        if (canBaki) {
                            MoneyNowLedgerRow(
                                label = if (currentLanguage == AppLanguage.BN) "পাওনা বাকি" else "Customer Baki",
                                hint = null,
                                value = ShopLocale.formatMoney(moneyNow.bakiCustomersOwe, currentLanguage),
                                valueColor = TextPrimary
                            )
                        }
                        if (showSuppliersSection) {
                            MoneyNowLedgerRow(
                                label = if (currentLanguage == AppLanguage.BN) "মহাজন দেনা" else "Owed to suppliers",
                                hint = null,
                                value = ShopLocale.formatMoney(moneyNow.debtToSuppliers, currentLanguage),
                                valueColor = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // 6. SALES Trend Bar Chart Card
        item {
            BusinessDarkGlassCard(
                onClick = if (canHistory) {
                    { storeViewModel.navigateInBusiness(BusinessNavDestination.REPORT_SALES) }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("sales_trend_card")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (currentLanguage == AppLanguage.BN) "বিক্রয় চিত্র" else "SALES",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = when {
                                    chartPoints.isEmpty() -> if (effectivePeriod == HistoryPeriod.YEAR || effectivePeriod == HistoryPeriod.ALL) "Monthly" else "Daily"
                                    effectivePeriod == HistoryPeriod.YEAR || effectivePeriod == HistoryPeriod.ALL -> "${chartPoints.size} mo"
                                    else -> "${chartPoints.size} days"
                                },
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                            if (canHistory) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    SalesBarChart(points = chartPoints)
                }
            }
        }

        // 7. TOP SELLERS + BY CATEGORY Side-by-Side Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BusinessDarkGlassCard(
                    onClick = if (canHistory) {
                        { storeViewModel.navigateInBusiness(BusinessNavDestination.REPORT_SALES) }
                    } else null,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("best_selling_card")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (currentLanguage == AppLanguage.BN) "সেরা পণ্য" else "TOP SELLERS",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.4.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        if (topProducts.isEmpty()) {
                            Text(
                                text = if (currentLanguage == AppLanguage.BN) "কোনো বিক্রয় নেই" else "No sales in period",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        } else {
                            topProducts.take(3).forEachIndexed { idx, stat ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${idx + 1}. ${stat.productName}",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 4.dp)
                                    )
                                    Text(
                                        text = ShopLocale.formatMoney(stat.totalSalesAmount, currentLanguage),
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }

                if (!isSimpleMode) {
                    BusinessDarkGlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("sales_by_category_card")
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (currentLanguage == AppLanguage.BN) "ক্যাটেগরি" else "BY CATEGORY",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.4.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            CategoryDonutChart(
                                stats = categoryStats,
                                compact = true
                            )
                        }
                    }
                }
            }
        }

        // 8. Bottom Compact Summary Row: EXPENSES + LOW STOCK + SLOW SELLERS (only existing business data cards)
        if (showExpensesSection || canStock) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showExpensesSection) {
                        BusinessCompactTile(
                            title = if (currentLanguage == AppLanguage.BN) "খরচ" else "EXPENSES",
                            value = ShopLocale.formatMoney(periodExpensesTotal, currentLanguage),
                            subtitle = if (currentLanguage == AppLanguage.BN) {
                                "${ShopLocale.formatNumber(activeExpenses.size.toLong(), currentLanguage)}টি এন্ট্রি"
                            } else {
                                "${activeExpenses.size} entr${if (activeExpenses.size == 1) "y" else "ies"}"
                            },
                            valueColor = TextPrimary,
                            subtitleColor = amberTint,
                            isHighlightedAmber = false,
                            testTag = "card_open_expenses_summary",
                            onClick = { storeViewModel.navigateInBusiness(BusinessNavDestination.EXPENSES_SCREEN) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }

                    if (canStock) {
                        val hasLow = lowStockList.isNotEmpty()
                        BusinessCompactTile(
                            title = if (currentLanguage == AppLanguage.BN) "কম মজুদ" else "LOW STOCK",
                            value = if (currentLanguage == AppLanguage.BN) {
                                "${ShopLocale.formatNumber(lowStockList.size.toLong(), currentLanguage)} টি"
                            } else {
                                "${lowStockList.size} item${if (lowStockList.size == 1) "" else "s"}"
                            },
                            subtitle = if (hasLow) "At/below limit" else "Stock healthy",
                            valueColor = if (hasLow) amberTint else VividGreen,
                            subtitleColor = TextSecondary,
                            isHighlightedAmber = hasLow,
                            testTag = "card_open_low_stock",
                            onClick = { storeViewModel.navigateInBusiness(BusinessNavDestination.LOW_STOCK_SCREEN) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )

                        if (!isSimpleMode && !isFatherMode) {
                            BusinessCompactTile(
                                title = if (currentLanguage == AppLanguage.BN) "অচল পণ্য" else "SLOW SELLERS",
                                value = if (currentLanguage == AppLanguage.BN) {
                                    "${ShopLocale.formatNumber(slowSellers.size.toLong(), currentLanguage)} টি"
                                } else {
                                    "${slowSellers.size} item${if (slowSellers.size == 1) "" else "s"}"
                                },
                                subtitle = "Zero sales",
                                valueColor = TextPrimary,
                                subtitleColor = TextSecondary,
                                isHighlightedAmber = false,
                                testTag = "card_open_slow_sellers",
                                onClick = { storeViewModel.navigateInBusiness(BusinessNavDestination.SLOW_SELLERS_SCREEN) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroSubMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val valSize = if (value.length <= 9) 14.5.sp else 12.5.sp
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            color = TextPrimary,
            fontSize = valSize,
            lineHeight = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BusinessSplitDueCard(
    title: String,
    amount: String,
    subtitle: String,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val valSize = when {
        amount.length <= 7 -> 20.sp
        amount.length <= 10 -> 17.sp
        else -> 14.5.sp
    }
    BusinessDarkGlassCard(
        onClick = onClick,
        modifier = modifier.testTag(testTag)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color(0xFFD6E2D2),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = amount,
                color = TextPrimary,
                fontSize = valSize,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = accentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MoneyNowLedgerRow(
    label: String,
    hint: String?,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            if (!hint.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = hint,
                    color = TextMuted,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
        Text(
            text = value,
            color = valueColor,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun BusinessCompactTile(
    title: String,
    value: String,
    subtitle: String,
    valueColor: Color,
    subtitleColor: Color,
    isHighlightedAmber: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    val bgBrush = if (isHighlightedAmber) {
        Brush.verticalGradient(listOf(Color(0xEB3B2B0C), Color(0xEE18130A)))
    } else {
        Brush.verticalGradient(listOf(Color(0xEB222622), Color(0xEE121412)))
    }
    val borderBrush = if (isHighlightedAmber) {
        Brush.verticalGradient(listOf(Color(0x88FFB300), Color(0x33FFB300)))
    } else {
        Brush.verticalGradient(listOf(Color(0x3CFFFFFF), Color(0x14FFFFFF)))
    }
    val valueSize = if (value.length <= 7) 15.sp else 13.sp

    Surface(
        onClick = onClick,
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, borderBrush),
        modifier = modifier
            .clip(shape)
            .background(bgBrush)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 9.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = if (isHighlightedAmber) Color(0xFFFFD54F) else Color(0xFFD6E2D2),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = value,
                color = valueColor,
                fontSize = valueSize,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = subtitleColor,
                fontSize = 9.5.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BusinessDarkGlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val bgBrush = Brush.verticalGradient(
        colors = listOf(Color(0xEB222622), Color(0xEE121412))
    )
    val borderBrush = Brush.verticalGradient(
        colors = listOf(Color(0x3DFFFFFF), Color(0x14FFFFFF))
    )

    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = shape,
            color = Color.Transparent,
            border = BorderStroke(1.dp, borderBrush),
            modifier = modifier
                .clip(shape)
                .background(bgBrush)
        ) {
            Box(modifier = Modifier.padding(13.dp)) {
                content()
            }
        }
    } else {
        Surface(
            shape = shape,
            color = Color.Transparent,
            border = BorderStroke(1.dp, borderBrush),
            modifier = modifier
                .clip(shape)
                .background(bgBrush)
        ) {
            Box(modifier = Modifier.padding(13.dp)) {
                content()
            }
        }
    }
}

@Composable
fun KpiStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    val valueFontSize = when {
        value.length > 12 -> 11.5.sp
        value.length > 9 -> 12.5.sp
        else -> 14.sp
    }

    GlassCard(
        modifier = modifier,
        testTag = testTag
    ) {
        Column {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                color = TextSecondary,
                fontSize = 10.5.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontSize = valueFontSize,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun ActionSummaryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
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
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        color = TextSecondary,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ReportNavRow(
    title: String,
    description: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        color = GlassSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0x1A5DD62C)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = VividGreen,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = description,
                        color = TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun SuppliersScreen(
    storeViewModel: StoreViewModel,
    modifier: Modifier = Modifier
) {
    BakiMainScreen(
        storeViewModel = storeViewModel,
        initialTab = 1,
        backButtonTag = "suppliers_back_btn",
        modifier = modifier
    )
}

@Composable
fun LowStockScreen(
    storeViewModel: StoreViewModel,
    modifier: Modifier = Modifier
) {
    val currentLanguage by storeViewModel.currentLanguage.collectAsState()
    val lowStockList by storeViewModel.lowStockProducts.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { storeViewModel.navigateBackInBusiness() },
                modifier = Modifier.testTag("low_stock_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (currentLanguage == AppLanguage.BN) "স্বল্প মজুদের পণ্য (Low Stock)" else "Low Stock Alert",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (currentLanguage == AppLanguage.BN) "${lowStockList.size}টি পণ্যের স্টক কম" else "${lowStockList.size} items at or below alert limit",
                    color = Color(0xFFFFB300),
                    fontSize = 12.sp
                )
            }
        }

        if (lowStockList.isEmpty()) {
            PlainEmptyState(
                message = if (currentLanguage == AppLanguage.BN) "সব পণ্য পর্যাপ্ত আছে" else "All Products Well-Stocked"
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(lowStockList, key = { it.id }) { product ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "low_stock_item_${product.id}"
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (currentLanguage == AppLanguage.BN && product.nameBn.isNotBlank()) product.nameBn else product.name,
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                val unitLabel = ShopLocale.displayUnit(product.unit, currentLanguage)
                                Text(
                                    text = if (storeViewModel.canSeeBuyingPriceAndProfitFlow.collectAsState().value) {
                                        "${product.category.ifBlank { "General" }} • Buy: ${ShopLocale.formatMoney(product.buyPrice, currentLanguage)}/$unitLabel • Sell: ${ShopLocale.formatMoney(product.sellPrice, currentLanguage)}/$unitLabel"
                                    } else {
                                        "${product.category.ifBlank { "General" }} • Sell: ${ShopLocale.formatMoney(product.sellPrice, currentLanguage)}/$unitLabel"
                                    },
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                val unitLabel = ShopLocale.displayUnit(product.unit, currentLanguage)
                                Text(
                                    text = "${ShopLocale.formatNumber(product.stock, currentLanguage)} $unitLabel",
                                    color = if (product.stock <= 0.0) Color(0xFFFF5252) else Color(0xFFFFB300),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = if (currentLanguage == AppLanguage.BN) {
                                        "সতর্কসীমা: ${ShopLocale.formatNumber(product.lowStockLimit, currentLanguage)} $unitLabel"
                                    } else {
                                        "Limit: ${ShopLocale.formatNumber(product.lowStockLimit, currentLanguage)} $unitLabel"
                                    },
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SlowSellersScreen(
    storeViewModel: StoreViewModel,
    modifier: Modifier = Modifier
) {
    val currentLanguage by storeViewModel.currentLanguage.collectAsState()
    val slowSellers by storeViewModel.slowSellingProducts.collectAsState()

    val canSeeProfit by storeViewModel.canSeeBuyingPriceAndProfitFlow.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { storeViewModel.navigateBackInBusiness() },
                modifier = Modifier.testTag("slow_sellers_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (currentLanguage == AppLanguage.BN) "অচল পণ্য (Slow-Selling)" else "Slow-Selling Products",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (currentLanguage == AppLanguage.BN) "${slowSellers.size}টি পণ্যে এই সময়ে কোনো বিক্রি হয়নি" else "${slowSellers.size} items in stock with zero sales in this period",
                    color = Color(0xFFB388FF),
                    fontSize = 12.sp
                )
            }
        }

        if (slowSellers.isEmpty()) {
            PlainEmptyState(
                message = if (currentLanguage == AppLanguage.BN) "কোন অচল পণ্য নেই" else "Zero Stagnant Products"
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(slowSellers, key = { it.id }) { product ->
                    val lockedCapital = product.stock * product.buyPrice
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "slow_seller_item_${product.id}"
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (currentLanguage == AppLanguage.BN && product.nameBn.isNotBlank()) product.nameBn else product.name,
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                val unitLabel = ShopLocale.displayUnit(product.unit, currentLanguage)
                                Text(
                                    text = if (canSeeProfit) {
                                        "Stock: ${ShopLocale.formatNumber(product.stock, currentLanguage)} $unitLabel • Buy: ${ShopLocale.formatMoney(product.buyPrice, currentLanguage)}/$unitLabel"
                                    } else {
                                        "Stock: ${ShopLocale.formatNumber(product.stock, currentLanguage)} $unitLabel • Sell: ${ShopLocale.formatMoney(product.sellPrice, currentLanguage)}/$unitLabel"
                                    },
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            if (canSeeProfit) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = ShopLocale.formatMoney(lockedCapital, currentLanguage),
                                        color = Color(0xFFB388FF),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (currentLanguage == AppLanguage.BN) "আটকা মূলধন" else "Tied-Up Capital",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
