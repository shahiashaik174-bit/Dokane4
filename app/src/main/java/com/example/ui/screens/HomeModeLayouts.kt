package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.SavedBusinessStrategy
import com.example.data.model.StrategyActionTarget
import com.example.domain.BakiOverview
import com.example.domain.DailyCashflowPoint
import com.example.domain.HomeInsights
import com.example.domain.MoneyNowSummary
import com.example.domain.SevenDayCashflowSummary
import com.example.domain.StockSummary
import com.example.domain.StoreFinancials
import com.example.ui.components.BusinessStrategyCard
import com.example.ui.theme.AlertRed
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividGreen
import com.example.util.AppLanguage
import com.example.util.ShopLocale
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

internal fun formatHomeMoney(amount: Double, lang: AppLanguage, isHidden: Boolean): String {
    if (isHidden) return "${ShopLocale.CURRENCY}••••"
    if (lang == AppLanguage.BN) {
        return ShopLocale.formatMoney(amount, lang)
    }
    val nf = NumberFormat.getNumberInstance(Locale.US).apply {
        val hasCents = abs(amount - amount.roundToInt()) >= 0.01
        minimumFractionDigits = if (hasCents) 2 else 0
        maximumFractionDigits = if (hasCents) 2 else 0
    }
    return "${ShopLocale.CURRENCY}${nf.format(amount)}"
}

@Composable
private fun BentoGlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
    padding: PaddingValues = PaddingValues(14.dp),
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    var mod = modifier
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xEB222622), Color(0xEC141614))
            )
        )
    if (testTag != null) mod = mod.testTag(testTag)
    if (onClick != null) mod = mod.clickable(onClick = onClick)

    Surface(
        modifier = mod,
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0x36FFFFFF), Color(0x10FFFFFF))
            )
        )
    ) {
        Box(modifier = Modifier.padding(padding)) {
            content()
        }
    }
}

@Composable
fun FatherModeHomeLayout(
    financials: StoreFinancials,
    stockSummary: StockSummary,
    currentLanguage: AppLanguage,
    isPrivacyHidden: Boolean,
    onNavigateToSell: () -> Unit,
    onAddProduct: () -> Unit,
    onOpenTodaySummary: () -> Unit,
    onOpenLowStock: () -> Unit,
    onOpenReport: () -> Unit,
    businessStrategy: SavedBusinessStrategy? = null,
    onStrategyAction: (StrategyActionTarget) -> Unit = {}
) {
    val lowCount = stockSummary.lowStockCount + stockSummary.outOfStockCount
    val amberAccent = Color(0xFFFFB300)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("father_home_list"),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. SELL (Biggest primary action)
        item(key = "father_home_sell") {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color.Transparent,
                border = BorderStroke(1.6.dp, VividGreen.copy(alpha = 0.85f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(98.dp)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    VividGreen.copy(alpha = 0.34f),
                                    Color.Transparent
                                )
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.3f),
                            radius = size.width * 0.78f
                        )
                    }
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF1F3C16),
                                Color(0xFF28501A),
                                Color(0xFF1E3914)
                            )
                        )
                    )
                    .clickable(onClick = onNavigateToSell)
                    .testTag("home_new_sale_btn")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = VividGreen,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingBag,
                                    contentDescription = null,
                                    tint = Color(0xFF0A1406),
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = if (currentLanguage == AppLanguage.BN) "বিক্রি" else "Sell",
                            color = TextPrimary,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Sell",
                        tint = VividGreen,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }

        // 2. ADD PRODUCT (Big button)
        item(key = "father_home_add_product") {
            BentoGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp),
                onClick = onAddProduct,
                testTag = "father_home_add_product_btn",
                padding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = VividGreen.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, VividGreen.copy(alpha = 0.45f)),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AddBox,
                                    contentDescription = null,
                                    tint = VividGreen,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = ShopLocale.get("father_home_add_product", currentLanguage),
                            color = TextPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // 3. TODAY'S SUMMARY
        item(key = "father_home_today_summary") {
            BentoGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 112.dp),
                onClick = onOpenTodaySummary,
                testTag = "home_todays_summary_card",
                padding = PaddingValues(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = ShopLocale.get("father_home_today_summary", currentLanguage),
                            color = TextSecondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatHomeMoney(financials.todaySales, currentLanguage, isPrivacyHidden),
                            color = VividGreen,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = VividGreen.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, VividGreen.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "${ShopLocale.formatNumber(financials.todayOrdersCount, currentLanguage)} ${if (currentLanguage == AppLanguage.BN) "অর্ডার" else "orders"}",
                            color = VividGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // 3B. BUSINESS STRATEGY ("আজকের পরামর্শ") below Today's Summary (Father mode: read-only, 1st action larger text)
        if (businessStrategy != null) {
            item(key = "father_home_business_strategy") {
                BusinessStrategyCard(
                    strategy = businessStrategy,
                    isCompactSingleActionMode = true,
                    showRefreshButton = false,
                    isRefreshing = false,
                    refreshesRemaining = 0,
                    onRefresh = {},
                    onActionClick = onStrategyAction
                )
            }
        }

        // 4 & 5. LOW STOCK + REPORT (Big 2-column Bento row)
        item(key = "father_home_bottom_bento") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BentoGlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .height(148.dp),
                    onClick = onOpenLowStock,
                    testTag = "home_low_stock_card",
                    padding = PaddingValues(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (lowCount > 0) amberAccent.copy(alpha = 0.22f) else VividGreen.copy(alpha = 0.18f),
                            border = BorderStroke(
                                1.dp,
                                if (lowCount > 0) amberAccent.copy(alpha = 0.55f) else VividGreen.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = if (lowCount > 0) amberAccent else VividGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Text(
                            text = ShopLocale.formatNumber(lowCount, currentLanguage),
                            color = if (lowCount > 0) amberAccent else VividGreen,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = ShopLocale.get("father_home_low_stock", currentLanguage),
                            color = TextPrimary,
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                BentoGlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .height(148.dp),
                    onClick = onOpenReport,
                    testTag = "father_home_report_btn",
                    padding = PaddingValues(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = VividGreen.copy(alpha = 0.20f),
                            border = BorderStroke(1.dp, VividGreen.copy(alpha = 0.45f)),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Assessment,
                                    contentDescription = null,
                                    tint = VividGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Text(
                            text = "PDF · Excel",
                            color = VividGreen,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = ShopLocale.get("father_home_report", currentLanguage),
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleHomeLayout(
    financials: StoreFinancials,
    homeInsights: HomeInsights,
    stockSummary: StockSummary,
    bakiOverview: BakiOverview,
    currentLanguage: AppLanguage,
    isPrivacyHidden: Boolean,
    canSell: Boolean,
    canViewStock: Boolean,
    canAccessBaki: Boolean,
    canSeeSalesSummary: Boolean,
    onRequestSwitchToDetailed: () -> Unit,
    onNavigateToSell: () -> Unit,
    onNavigateToStock: () -> Unit,
    onNavigateToBaki: () -> Unit,
    onNavigateToSalesSummary: () -> Unit,
    businessStrategy: SavedBusinessStrategy? = null,
    canViewStrategy: Boolean = false,
    canRefreshStrategy: Boolean = false,
    isStrategyRefreshing: Boolean = false,
    strategyRefreshesRemaining: Int = 0,
    onRefreshStrategy: () -> Unit = {},
    onStrategyAction: (StrategyActionTarget) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_dashboard_list"),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 122.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Centered Simple mode capsule
        item(key = "simple_mode_pill") {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1A1E1A),
                border = BorderStroke(1.dp, Color(0x30FFFFFF)),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onRequestSwitchToDetailed)
                    .testTag("home_simple_mode_pill")
            ) {
                Text(
                    text = if (currentLanguage == AppLanguage.BN) "সিম্পল মোড" else "Simple mode",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }

        // Large glowing circular sales gauge
        if (canSeeSalesSummary) {
            item(key = "simple_sales_ring") {
                val ringProgress = remember(financials.todaySales, homeInsights.yesterdaySales) {
                    when {
                        homeInsights.yesterdaySales > 0.0 ->
                            (financials.todaySales / homeInsights.yesterdaySales).toFloat().coerceIn(0.08f, 1f)
                        financials.todaySales > 0.0 -> 0.72f
                        else -> 0f
                    }
                }
                SimpleSalesRingHero(
                    todaySales = financials.todaySales,
                    ordersCount = financials.todayOrdersCount,
                    progress = ringProgress,
                    currentLanguage = currentLanguage,
                    isPrivacyHidden = isPrivacyHidden,
                    onClick = onNavigateToSalesSummary
                )
            }
        }

        // Business Strategy card below Today's Summary (Simple mode: 1st action larger text)
        if (canViewStrategy && businessStrategy != null) {
            item(key = "simple_business_strategy") {
                BusinessStrategyCard(
                    strategy = businessStrategy,
                    isCompactSingleActionMode = true,
                    showRefreshButton = canRefreshStrategy,
                    isRefreshing = isStrategyRefreshing,
                    refreshesRemaining = strategyRefreshesRemaining,
                    onRefresh = onRefreshStrategy,
                    onActionClick = onStrategyAction
                )
            }
        }

        // Full-width glowing green-tinted "Sell Products ->" glass button
        if (canSell) {
            item(key = "simple_sell_cta") {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, Color(0x457EEB4B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        VividGreen.copy(alpha = 0.24f),
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width * 0.55f, size.height * 0.2f),
                                    radius = size.width * 0.72f
                                )
                            )
                        }
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xEE182614),
                                    Color(0xEE1E3716),
                                    Color(0xEE182C13)
                                )
                            )
                        )
                        .clickable(onClick = onNavigateToSell)
                        .testTag("home_new_sale_btn")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x1CFFFFFF),
                                border = BorderStroke(1.dp, Color(0x28FFFFFF)),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingBag,
                                        contentDescription = null,
                                        tint = TextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = if (currentLanguage == AppLanguage.BN) "পণ্য বিক্রি করুন" else "Sell Products",
                                color = TextPrimary,
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Sell",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Bottom 2-column Bento cards: Stock & Products | Customer Baki
        if (canViewStock || canAccessBaki) {
            item(key = "simple_bottom_bento") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (canViewStock) {
                        BentoGlassCard(
                            modifier = Modifier
                                .weight(1f)
                                .height(136.dp),
                            onClick = onNavigateToStock,
                            testTag = "home_stock_card",
                            padding = PaddingValues(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0x20FFFFFF),
                                    border = BorderStroke(1.dp, Color(0x28FFFFFF)),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Build,
                                            contentDescription = null,
                                            tint = TextPrimary,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = if (currentLanguage == AppLanguage.BN) "স্টক ও পণ্য" else "Stock & Products",
                                        color = TextPrimary,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = if (currentLanguage == AppLanguage.BN) {
                                            "${ShopLocale.formatNumber(stockSummary.totalItems, currentLanguage)}টি পণ্য"
                                        } else {
                                            "${stockSummary.totalItems} items"
                                        },
                                        color = TextSecondary,
                                        fontSize = 12.5.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    if (canAccessBaki) {
                        BentoGlassCard(
                            modifier = Modifier
                                .weight(1f)
                                .height(136.dp),
                            onClick = onNavigateToBaki,
                            testTag = "home_baki_card",
                            padding = PaddingValues(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (currentLanguage == AppLanguage.BN) "ক্রেতার বাকি" else "Customer Baki",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = formatHomeMoney(bakiOverview.totalBakiOwed, currentLanguage, isPrivacyHidden),
                                    color = VividGreen,
                                    fontSize = 23.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (currentLanguage == AppLanguage.BN) {
                                        "${ShopLocale.formatNumber(bakiOverview.debtorCount, currentLanguage)} জন ক্রেতা"
                                    } else {
                                        "${bakiOverview.debtorCount} customers"
                                    },
                                    color = TextSecondary,
                                    fontSize = 12.5.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
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
private fun SimpleSalesRingHero(
    todaySales: Double,
    ordersCount: Long,
    progress: Float,
    currentLanguage: AppLanguage,
    isPrivacyHidden: Boolean,
    onClick: () -> Unit
) {
    val numberPart = remember(todaySales, currentLanguage, isPrivacyHidden) {
        if (isPrivacyHidden) {
            "••••"
        } else if (currentLanguage == AppLanguage.BN) {
            ShopLocale.formatMoney(todaySales, currentLanguage).replace(ShopLocale.CURRENCY, "").trim()
        } else {
            val nf = NumberFormat.getNumberInstance(Locale.US).apply {
                val hasCents = abs(todaySales - todaySales.roundToInt()) >= 0.01
                minimumFractionDigits = if (hasCents) 2 else 0
                maximumFractionDigits = if (hasCents) 2 else 0
            }
            nf.format(todaySales)
        }
    }

    Box(
        modifier = Modifier
            .size(248.dp)
            .drawBehind {
                // Soft emerald radial glow beneath the ring matching reference
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            VividGreen.copy(alpha = 0.36f),
                            VividGreen.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.56f, size.height * 0.68f),
                        radius = size.width * 0.72f
                    )
                )
            }
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick)
                .testTag("home_todays_summary_card")
        )
        Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            val strokePx = 11.dp.toPx()
            val inset = strokePx / 2f

            // Inner dark glass disk
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF282D28), Color(0xFF131613)),
                    center = Offset(size.width * 0.42f, size.height * 0.34f),
                    radius = size.width * 0.65f
                ),
                radius = (size.minDimension / 2f) - strokePx * 0.45f
            )

            // Outer glass bezel
            drawCircle(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0x55FFFFFF), Color(0x14FFFFFF))
                ),
                radius = (size.minDimension / 2f),
                style = Stroke(width = 1.2.dp.toPx())
            )

            // Background circular track
            drawArc(
                color = Color(0x28FFFFFF),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.width - strokePx, size.height - strokePx),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Vivid green progress arc
            if (progress > 0f) {
                val sweep = 360f * progress
                drawArc(
                    color = VividGreen.copy(alpha = 0.25f),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(size.width - strokePx, size.height - strokePx),
                    style = Stroke(width = strokePx + 6.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFF7BFF48),
                            VividGreen,
                            Color(0xFF3E9C1A),
                            Color(0xFF7BFF48)
                        )
                    ),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(size.width - strokePx, size.height - strokePx),
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Text(
                text = if (currentLanguage == AppLanguage.BN) "আজকের বিক্রি" else "TODAY'S SALES",
                color = TextSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.8.sp,
                maxLines = 1,
                softWrap = false
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = VividGreen, fontWeight = FontWeight.ExtraBold, fontSize = 27.sp)) {
                        append(ShopLocale.CURRENCY)
                    }
                    withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 33.sp)) {
                        append(numberPart)
                    }
                },
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("home_tile_today_sales")
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (currentLanguage == AppLanguage.BN) {
                    "${ShopLocale.formatNumber(ordersCount, currentLanguage)}টি অর্ডার"
                } else {
                    if (ordersCount == 1L) "1 order" else "$ordersCount orders"
                },
                color = TextSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun DetailedHomeLayout(
    financials: StoreFinancials,
    homeInsights: HomeInsights,
    bakiOverview: BakiOverview,
    totalBalance: Double,
    moneyNow: MoneyNowSummary,
    lowStockProducts: List<Product>,
    sevenDayCashflow: SevenDayCashflowSummary,
    recentSales: List<Sale>,
    currentLanguage: AppLanguage,
    isPrivacyHidden: Boolean,
    canSeeSalesSummary: Boolean,
    canSeeProfit: Boolean,
    canAccessBaki: Boolean,
    canReceivePayment: Boolean,
    canAccessCashAndBank: Boolean,
    canAccessSuppliers: Boolean,
    canPaySupplier: Boolean,
    canViewStock: Boolean,
    canAddEditProducts: Boolean,
    canViewReports: Boolean,
    canAccessExpenses: Boolean,
    canSeeHistory: Boolean,
    onOpenSalesSummary: () -> Unit,
    onOpenBaki: () -> Unit,
    onOpenCashAndBank: () -> Unit,
    onOpenSuppliers: () -> Unit,
    onOpenLowStock: () -> Unit,
    onRestockProduct: (Product) -> Unit,
    onOpenCashflowDetail: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSaleDetail: (Sale) -> Unit,
    onQuickPaymentIn: () -> Unit,
    onQuickPaymentOut: () -> Unit,
    onQuickPurchase: () -> Unit,
    onQuickExpense: () -> Unit,
    onQuickAddParty: () -> Unit,
    businessStrategy: SavedBusinessStrategy? = null,
    canViewStrategy: Boolean = false,
    canRefreshStrategy: Boolean = false,
    isStrategyRefreshing: Boolean = false,
    strategyRefreshesRemaining: Int = 0,
    onRefreshStrategy: () -> Unit = {},
    onStrategyAction: (StrategyActionTarget) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_dashboard_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 122.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Hero TODAY glowing card (NO Profile 60%, NO % vs expecting; real % vs yesterday only when yesterdaySales > 0)
        if (canSeeSalesSummary) {
            item(key = "detailed_hero_today") {
                DetailedTodayHeroCard(
                    financials = financials,
                    homeInsights = homeInsights,
                    currentLanguage = currentLanguage,
                    isPrivacyHidden = isPrivacyHidden,
                    canSeeProfit = canSeeProfit,
                    onClick = onOpenSalesSummary
                )
            }
        }

        // 1B. Business Strategy card directly below Today's Summary (hidden for staff without Reports permission)
        if (canViewStrategy && businessStrategy != null) {
            item(key = "detailed_business_strategy") {
                BusinessStrategyCard(
                    strategy = businessStrategy,
                    isCompactSingleActionMode = false,
                    showRefreshButton = canRefreshStrategy,
                    isRefreshing = isStrategyRefreshing,
                    refreshesRemaining = strategyRefreshesRemaining,
                    onRefresh = onRefreshStrategy,
                    onActionClick = onStrategyAction
                )
            }
        }

        // 2. Asymmetric 2-Column Bento: Left = TO RECEIVE, Right = CASH & BANK + TO GIVE
        val showLeftBaki = canAccessBaki || canReceivePayment
        val showRightTopCash = canAccessCashAndBank
        val showRightBottomGive = canAccessSuppliers || canPaySupplier

        if (showLeftBaki || showRightTopCash || showRightBottomGive) {
            item(key = "detailed_bento_money") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (showLeftBaki) {
                        BentoGlassCard(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            onClick = onOpenBaki,
                            testTag = "home_to_receive_card",
                            padding = PaddingValues(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (currentLanguage == AppLanguage.BN) "পাওনা" else "TO RECEIVE",
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = 0.5.sp,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = formatHomeMoney(bakiOverview.totalBakiOwed, currentLanguage, isPrivacyHidden),
                                        color = TextPrimary,
                                        fontSize = 21.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (currentLanguage == AppLanguage.BN) {
                                            "বাকি • ${ShopLocale.formatNumber(bakiOverview.debtorCount, currentLanguage)} জন"
                                        } else {
                                            "Baki • ${bakiOverview.debtorCount} customers"
                                        },
                                        color = TextSecondary,
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val topDebtors = bakiOverview.debtors.take(3)
                                    if (topDebtors.isEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0x185DD62C)
                                        ) {
                                            Text(
                                                text = if (currentLanguage == AppLanguage.BN) "পরিশোধিত" else "All settled",
                                                color = VividGreen,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    } else {
                                        topDebtors.forEach { debtor ->
                                            val initial = debtor.customer.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "C"
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFF282C28),
                                                border = BorderStroke(1.dp, Color(0x36FFFFFF)),
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = initial,
                                                        color = TextPrimary,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showRightTopCash || showRightBottomGive) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (showRightTopCash) {
                                BentoGlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    onClick = onOpenCashAndBank,
                                    testTag = "home_cash_bank_card",
                                    padding = PaddingValues(14.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (currentLanguage == AppLanguage.BN) "ক্যাশ ও ব্যাংক" else "CASH & BANK",
                                                color = TextSecondary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                letterSpacing = 0.5.sp,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = TextMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = formatHomeMoney(totalBalance, currentLanguage, isPrivacyHidden),
                                            color = TextPrimary,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            if (showRightBottomGive) {
                                BentoGlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    onClick = onOpenSuppliers,
                                    testTag = "home_to_give_card",
                                    padding = PaddingValues(14.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (currentLanguage == AppLanguage.BN) "দেনা" else "TO GIVE",
                                                color = TextSecondary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                letterSpacing = 0.5.sp,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = TextMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = formatHomeMoney(moneyNow.debtToSuppliers, currentLanguage, isPrivacyHidden),
                                            color = TextPrimary,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (currentLanguage == AppLanguage.BN) {
                                                "${ShopLocale.formatNumber(moneyNow.suppliersCount, currentLanguage)} মহাজন"
                                            } else {
                                                "${moneyNow.suppliersCount} suppliers"
                                            },
                                            color = TextSecondary,
                                            fontSize = 11.5.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. LOW STOCK Split Card
        if (canViewStock) {
            item(key = "detailed_low_stock") {
                DetailedLowStockCard(
                    lowStockProducts = lowStockProducts,
                    currentLanguage = currentLanguage,
                    canAddEdit = canAddEditProducts,
                    onOpenLowStock = onOpenLowStock,
                    onRestockProduct = onRestockProduct
                )
            }
        }

        // 4. 7-DAY CASHFLOW Card
        if (canViewReports || canAccessCashAndBank) {
            item(key = "detailed_cashflow") {
                DetailedSevenDayCashflowCard(
                    summary = sevenDayCashflow,
                    currentLanguage = currentLanguage,
                    isPrivacyHidden = isPrivacyHidden,
                    onClick = onOpenCashflowDetail
                )
            }
        }

        // 5. 5 Circular Quick Actions Row (permission-filtered, real actions only)
        val quickActions = buildList {
            if (canReceivePayment || canAccessBaki) {
                add(Triple("Payment In", Icons.Default.CurrencyExchange, onQuickPaymentIn))
            }
            if (canPaySupplier || canAccessSuppliers) {
                add(Triple("Payment Out", Icons.Default.SwapHoriz, onQuickPaymentOut))
            }
            if (canAccessSuppliers) {
                add(Triple("Purchase", Icons.Default.ShoppingCart, onQuickPurchase))
            }
            if (canAccessExpenses) {
                add(Triple("Expense", Icons.Default.AttachMoney, onQuickExpense))
            }
            if (canAccessBaki || canAccessSuppliers) {
                add(Triple("Add Party", Icons.Default.PersonAdd, onQuickAddParty))
            }
        }

        if (quickActions.isNotEmpty()) {
            item(key = "detailed_quick_actions") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    quickActions.forEach { (label, icon, action) ->
                        QuickActionCircleButton(
                            label = label,
                            icon = icon,
                            onClick = action,
                            modifier = Modifier.weight(1f),
                            testTag = "home_qa_${label.lowercase().replace(' ', '_')}"
                        )
                    }
                }
            }
        }

        // 6. RECENT SALES Card
        if (canSeeSalesSummary) {
            item(key = "detailed_recent_sales") {
                BentoGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "home_recent_sales_card",
                    padding = PaddingValues(14.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = canSeeHistory, onClick = onOpenHistory)
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (currentLanguage == AppLanguage.BN) "সাম্প্রতিক বিক্রি" else "RECENT SALES",
                                color = TextSecondary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "History",
                                tint = TextMuted,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        if (recentSales.isEmpty()) {
                            Text(
                                text = if (currentLanguage == AppLanguage.BN) "এখনো কোনো বিক্রি হয়নি" else "No sales recorded yet. Tap SELL below.",
                                color = TextMuted,
                                fontSize = 12.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            recentSales.take(4).forEachIndexed { index, sale ->
                                val displayName = sale.customerName.ifBlank { "Cash Sale" }
                                val initial = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "C"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(enabled = canSeeHistory) { onOpenSaleDetail(sale) }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF282D28),
                                            border = BorderStroke(1.dp, Color(0x28FFFFFF)),
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = initial,
                                                    color = TextSecondary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = displayName,
                                            color = TextPrimary,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = formatHomeMoney(sale.grandTotal, currentLanguage, isPrivacyHidden),
                                        color = TextPrimary,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                if (index < recentSales.take(4).lastIndex) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(Color(0x10FFFFFF))
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

@Composable
private fun DetailedTodayHeroCard(
    financials: StoreFinancials,
    homeInsights: HomeInsights,
    currentLanguage: AppLanguage,
    isPrivacyHidden: Boolean,
    canSeeProfit: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            VividGreen.copy(alpha = 0.24f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.55f),
                        radius = size.width * 0.65f
                    )
                )
            }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape),
            shape = shape,
            color = Color.Transparent,
            border = BorderStroke(1.3.dp, Color(0xFF53DA24))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xF5152012), Color(0xF70C120A))
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(onClick = onClick)
                        .testTag("home_todays_summary_card")
                )
                // Real sparkline wave rendered behind hero sales figure
                if (homeInsights.todaySparklinePoints.size >= 2) {
                    Canvas(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(top = 34.dp, bottom = 36.dp)
                    ) {
                        val pts = homeInsights.todaySparklinePoints
                        val stepX = size.width / (pts.size - 1).coerceAtLeast(1)
                        val coords = pts.mapIndexed { i, v ->
                            val y = size.height - (v.coerceIn(0f, 1f) * size.height * 0.82f) - size.height * 0.08f
                            Offset(i * stepX, y)
                        }
                        val linePath = Path().apply {
                            moveTo(coords.first().x, coords.first().y)
                            for (i in 0 until coords.lastIndex) {
                                val p0 = coords[i]
                                val p1 = coords[i + 1]
                                val cx = (p0.x + p1.x) / 2f
                                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                            }
                        }
                        val fillPath = Path().apply {
                            addPath(linePath)
                            lineTo(size.width, size.height)
                            lineTo(0f, size.height)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(VividGreen.copy(alpha = 0.22f), Color.Transparent)
                            )
                        )
                        drawPath(
                            path = linePath,
                            color = VividGreen.copy(alpha = 0.28f),
                            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        drawPath(
                            path = linePath,
                            color = VividGreen.copy(alpha = 0.85f),
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top chips row: TODAY + optional real % vs yesterday (never fabricated!)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x305DD62C),
                            border = BorderStroke(1.dp, Color(0x455DD62C))
                        ) {
                            Text(
                                text = if (currentLanguage == AppLanguage.BN) "আজ" else "TODAY",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        val pct = homeInsights.salesChangePercent
                        if (pct != null) {
                            val isUp = pct >= 0.0
                            val arrow = if (isUp) "▲" else "▼"
                            val roundedPct = abs(pct).roundToInt()
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x281D2B18),
                                border = BorderStroke(
                                    1.dp,
                                    if (isUp) Color(0x455DD62C) else Color(0x45E53935)
                                ),
                                modifier = Modifier.testTag("home_vs_yesterday_chip")
                            ) {
                                Text(
                                    text = "$arrow $roundedPct% vs yesterday",
                                    color = if (isUp) VividGreen else AlertRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Big VividGreen Sales Figure
                    Text(
                        text = formatHomeMoney(financials.todaySales, currentLanguage, isPrivacyHidden),
                        color = VividGreen,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("home_tile_today_sales")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bottom 3-stat row (PROFIT | ORDERS | ITEMS SOLD)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (canSeeProfit) {
                            HeroSubStat(
                                label = if (currentLanguage == AppLanguage.BN) "লাভ" else "PROFIT",
                                value = formatHomeMoney(financials.todayProfit, currentLanguage, isPrivacyHidden),
                                modifier = Modifier.weight(1f),
                                testTag = "home_tile_net_profit"
                            )
                        }
                        HeroSubStat(
                            label = if (currentLanguage == AppLanguage.BN) "অর্ডার" else "ORDERS",
                            value = ShopLocale.formatNumber(financials.todayOrdersCount, currentLanguage),
                            modifier = Modifier.weight(1f),
                            testTag = "home_tile_orders_completed"
                        )
                        HeroSubStat(
                            label = if (currentLanguage == AppLanguage.BN) "বিক্রিত পণ্য" else "ITEMS SOLD",
                            value = ShopLocale.formatNumber(financials.todayItemsSold, currentLanguage),
                            modifier = Modifier.weight(1f),
                            testTag = "home_tile_items_sold"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroSubStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Column(
        modifier = modifier.testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DetailedLowStockCard(
    lowStockProducts: List<Product>,
    currentLanguage: AppLanguage,
    canAddEdit: Boolean,
    onOpenLowStock: () -> Unit,
    onRestockProduct: (Product) -> Unit
) {
    val amber = Color(0xFFFFA726)

    BentoGlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpenLowStock,
        testTag = "home_low_stock_card",
        padding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left count pane
            Column(
                modifier = Modifier
                    .weight(0.34f)
                    .padding(end = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (currentLanguage == AppLanguage.BN) "স্বল্প মজুদ" else "LOW STOCK",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = ShopLocale.formatNumber(lowStockProducts.size.toLong(), currentLanguage),
                    color = if (lowStockProducts.isNotEmpty()) amber else VividGreen,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false
                )
            }

            // Vertical divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(GlassBorder)
            )

            // Right top-2 items pane
            Column(
                modifier = Modifier
                    .weight(0.66f)
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (lowStockProducts.isEmpty()) {
                    Text(
                        text = if (currentLanguage == AppLanguage.BN) "সব পণ্যের মজুদ পর্যাপ্ত" else "All stock levels healthy",
                        color = VividGreen,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    lowStockProducts.take(2).forEach { prod ->
                        val isOut = prod.stock <= 0.0
                        val badgeColor = if (isOut) AlertRed else amber
                        val maxRef = (prod.lowStockLimit * 2.0).coerceAtLeast(5.0).toFloat()
                        val fraction = (prod.stock.toFloat() / maxRef).coerceIn(0.08f, 1f)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = canAddEdit) { onRestockProduct(prod) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = prod.name,
                                    color = TextPrimary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${ShopLocale.formatNumber(prod.stock, currentLanguage)} ${ShopLocale.displayUnit(prod.unit, currentLanguage)}",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = badgeColor.copy(alpha = 0.22f)
                                ) {
                                    Text(
                                        text = if (isOut) "Out" else "Low",
                                        color = badgeColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0x22FFFFFF))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = if (isOut) 0.1f else fraction)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(badgeColor)
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
private fun DetailedSevenDayCashflowCard(
    summary: SevenDayCashflowSummary,
    currentLanguage: AppLanguage,
    isPrivacyHidden: Boolean,
    onClick: () -> Unit
) {
    BentoGlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        testTag = "home_cashflow_card",
        padding = PaddingValues(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (currentLanguage == AppLanguage.BN) "৭ দিনের ক্যাশফ্লো" else "7-DAY CASHFLOW",
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    softWrap = false
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(17.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1.85f)) {
                    CashflowDualLineCanvas(
                        points = summary.points,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        summary.points.forEach { pt ->
                            Text(
                                text = pt.dayLabel,
                                color = TextMuted,
                                fontSize = 10.5.sp,
                                maxLines = 1,
                                softWrap = false,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1.15f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(VividGreen)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "In",
                            color = VividGreen,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = formatHomeMoney(summary.totalIn, currentLanguage, isPrivacyHidden),
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(TextSecondary)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Out",
                            color = TextSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = formatHomeMoney(summary.totalOut, currentLanguage, isPrivacyHidden),
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CashflowDualLineCanvas(
    points: List<DailyCashflowPoint>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val maxVal = points.maxOf { maxOf(it.cashIn, it.cashOut) }.coerceAtLeast(1.0)
        val stepX = size.width / (points.size - 1).coerceAtLeast(1)

        fun buildSmoothPath(values: List<Double>): Path {
            val coords = values.mapIndexed { i, v ->
                val ratio = (v / maxVal).toFloat().coerceIn(0f, 1f)
                val y = size.height - (ratio * size.height * 0.80f) - size.height * 0.08f
                Offset(i * stepX, y)
            }
            return Path().apply {
                moveTo(coords.first().x, coords.first().y)
                for (i in 0 until coords.lastIndex) {
                    val p0 = coords[i]
                    val p1 = coords[i + 1]
                    val cx = (p0.x + p1.x) / 2f
                    cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                }
            }
        }

        val outPath = buildSmoothPath(points.map { it.cashOut })
        val inPath = buildSmoothPath(points.map { it.cashIn })
        val inFill = Path().apply {
            addPath(inPath)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }

        // Subtle area glow under In line
        drawPath(
            path = inFill,
            brush = Brush.verticalGradient(
                colors = listOf(VividGreen.copy(alpha = 0.22f), Color.Transparent)
            )
        )

        // Out line (soft gray)
        drawPath(
            path = outPath,
            color = TextSecondary.copy(alpha = 0.55f),
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // In line glow + crisp stroke
        drawPath(
            path = inPath,
            color = VividGreen.copy(alpha = 0.30f),
            style = Stroke(width = 5.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawPath(
            path = inPath,
            color = VividGreen,
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
private fun QuickActionCircleButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = Color.Transparent,
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0x45FFFFFF), Color(0x14FFFFFF))
                )
            ),
            modifier = Modifier.size(48.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF282D28), Color(0xFF151815))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = TextPrimary,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.2).sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
