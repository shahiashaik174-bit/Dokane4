package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.data.model.StrategyActionTarget
import com.example.ui.components.BackupPinDialog
import com.example.ui.components.BackupPinDialogMode
import com.example.ui.components.FatherLockInfoDialog
import com.example.ui.components.FloatingGlassPillBottomBar
import com.example.ui.components.GlassCard
import com.example.ui.components.NoPermissionScreen
import com.example.ui.components.ShopNavTab
import com.example.ui.components.VividButton
import com.example.ui.theme.AlertRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividGreen
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.BusinessNavDestination
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.AppLanguage
import com.example.util.ShopLocale
import com.example.util.SyncStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class HomeQuickDialog {
    NONE,
    NOTIFICATIONS,
    PAYMENT_IN,
    PAYMENT_OUT,
    PURCHASE,
    EXPENSE,
    ADD_PARTY,
    CONFIRM_DETAILED_MODE,
    FATHER_LOCK_INFO
}

@Composable
fun MainShopScreen(
    authViewModel: AuthViewModel,
    storeViewModel: StoreViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTab by rememberSaveable { mutableStateOf(ShopNavTab.HOME) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var activeDialog by remember { mutableStateOf(HomeQuickDialog.NONE) }
    var quickRestockProduct by remember { mutableStateOf<Product?>(null) }

    val currentLanguage by storeViewModel.currentLanguage.collectAsState()
    val store by storeViewModel.store.collectAsState()
    val syncStatus by storeViewModel.syncStatus.collectAsState()
    val isPrivacyHidden by storeViewModel.isPrivacyHidden.collectAsState()
    val lowStockProducts by storeViewModel.lowStockProducts.collectAsState()
    val bakiOverview by storeViewModel.bakiOverview.collectAsState()

    val isRemoved by storeViewModel.isRemoved.collectAsState()
    val canSell by storeViewModel.canSellFlow.collectAsState()
    val canViewStock by storeViewModel.canViewStockFlow.collectAsState()
    val canSeeHistory by storeViewModel.canSeeHistoryFlow.collectAsState()
    val canSeeProfit by storeViewModel.canSeeBuyingPriceAndProfitFlow.collectAsState()
    val canViewReports by storeViewModel.canViewReportsFlow.collectAsState()
    val canAddEdit by storeViewModel.canAddEditProductsFlow.collectAsState()
    val canAccessBaki by storeViewModel.canAccessCustomersAndBakiFlow.collectAsState()
    val canReceivePayment by storeViewModel.canReceivePaymentFlow.collectAsState()
    val canAccessSuppliers by storeViewModel.canAccessSuppliersAndPurchasesFlow.collectAsState()
    val canPaySupplier by storeViewModel.canPaySupplierFlow.collectAsState()
    val canAccessCashAndBank by storeViewModel.canAccessCashAndBankFlow.collectAsState()
    val canAccessExpenses by storeViewModel.canAccessExpensesAndIncomeFlow.collectAsState()

    var showAddWizard by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    val isBusy by storeViewModel.isBusy.collectAsState()
    val errorMessage by storeViewModel.errorMessage.collectAsState()
    val statusMessage by storeViewModel.statusMessage.collectAsState()
    val isSimpleMode by storeViewModel.isSimpleMode.collectAsState()
    val isFatherMode by storeViewModel.isFatherMode.collectAsState()
    val isOwner by storeViewModel.isOwner.collectAsState()
    val currentMember by storeViewModel.currentMember.collectAsState()
    val hasOwnerPin by storeViewModel.hasOwnerPin.collectAsState()
    var showFatherExitPinDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(currentTab) {
        storeViewModel.setActiveSubNavTab(currentTab)
    }

    LaunchedEffect(isFatherMode) {
        if (isFatherMode && showSettings) {
            showSettings = false
        }
    }

    LaunchedEffect(statusMessage) {
        val msg = statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Long
            )
            storeViewModel.clearStatusMessage()
        }
    }

    LaunchedEffect(errorMessage, showAddWizard, editingProduct) {
        val err = errorMessage
        if (!err.isNullOrBlank() && !showAddWizard && editingProduct == null) {
            snackbarHostState.showSnackbar(
                message = err,
                duration = SnackbarDuration.Long
            )
            storeViewModel.clearErrorMessage()
        }
    }

    fun openSubDestination(targetTab: ShopNavTab, dest: BusinessNavDestination) {
        if (targetTab == ShopNavTab.MORE) {
            storeViewModel.resetMoreToRoot()
        } else {
            storeViewModel.resetBusinessToRoot()
        }
        storeViewModel.setActiveSubNavTab(targetTab)
        storeViewModel.navigateInBusiness(dest)
        currentTab = targetTab
    }

    val moreDest by storeViewModel.moreNavDestination.collectAsState()
    BackHandler(enabled = !showSettings && currentTab == ShopNavTab.MORE && moreDest == BusinessNavDestination.DASHBOARD) {
        currentTab = ShopNavTab.HOME
    }

    if (isRemoved) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = AlertRed,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (currentLanguage == AppLanguage.BN) "অ্যাক্সেস প্রত্যাহার করা হয়েছে" else "Access Revoked",
                        color = AlertRed,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (currentLanguage == AppLanguage.BN) "আপনি আর এই দোকানে অ্যাক্সেস করতে পারবেন না। দয়া করে দোকানের মালিকের সাথে যোগাযোগ করুন।"
                        else "You no longer have access to this store. Please contact the store owner.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    VividButton(
                        text = if (currentLanguage == AppLanguage.BN) "লগ আউট করুন" else "Sign Out",
                        onClick = { authViewModel.signOut(context) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        return
    }

    if (showSettings) {
        SettingsScreen(
            authViewModel = authViewModel,
            storeViewModel = storeViewModel,
            onBack = { showSettings = false },
            onLogout = {
                showSettings = false
                authViewModel.signOut(context)
            },
            onNavigateDestination = { dest ->
                showSettings = false
                openSubDestination(ShopNavTab.MORE, dest)
            },
            modifier = modifier
        )
        return
    }

    val alertCount = (if (canViewStock) lowStockProducts.size else 0) +
        (if (canAccessBaki) bakiOverview.debtorCount.toInt() else 0)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070907))
            .drawBehind {
                // Top-left ambient emerald glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            VividGreen.copy(alpha = 0.22f),
                            VividGreen.copy(alpha = 0.06f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.06f, size.height * 0.04f),
                        radius = size.width * 0.72f
                    )
                )
                // Soft lower emerald glow when on Home Simple mode
                if (currentTab == ShopNavTab.HOME && isSimpleMode) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                VividGreen.copy(alpha = 0.20f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.65f, size.height * 0.50f),
                            radius = size.width * 0.68f
                        )
                    )
                }
            }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                ShopTopBar(
                    storeName = store?.name?.ifBlank { "StoreOS" } ?: "StoreOS",
                    syncStatus = syncStatus,
                    currentLanguage = currentLanguage,
                    isSimpleMode = isSimpleMode,
                    isFatherMode = isFatherMode,
                    isPrivacyHidden = isPrivacyHidden,
                    alertCount = alertCount,
                    isMoreSelected = currentTab == ShopNavTab.MORE,
                    onTogglePrivacy = { storeViewModel.togglePrivacyHidden() },
                    onOpenNotifications = { activeDialog = HomeQuickDialog.NOTIFICATIONS },
                    onOpenFatherLockInfo = { activeDialog = HomeQuickDialog.FATHER_LOCK_INFO },
                    onOpenMore = {
                        if (currentTab == ShopNavTab.MORE) {
                            storeViewModel.resetMoreToRoot()
                        } else {
                            storeViewModel.setActiveSubNavTab(ShopNavTab.MORE)
                            currentTab = ShopNavTab.MORE
                        }
                    },
                    onOpenSettings = {
                        if (isFatherMode) {
                            storeViewModel.setSimpleMode(false)
                        } else {
                            showSettings = true
                        }
                    }
                )
            },
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 72.dp)
                ) { data ->
                    val toastShape = RoundedCornerShape(16.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(toastShape)
                            .background(Color(0xF01C221D))
                            .border(1.dp, VividGreen.copy(alpha = 0.45f), toastShape)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = data.visuals.message,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        val actionLabel = data.visuals.actionLabel
                        if (!actionLabel.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = actionLabel,
                                color = VividGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { data.performAction() }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            },
            bottomBar = {
                FloatingGlassPillBottomBar(
                    currentTab = currentTab,
                    onTabSelected = { target ->
                        if (target == currentTab) {
                            when (target) {
                                ShopNavTab.MORE -> storeViewModel.resetMoreToRoot()
                                ShopNavTab.BUSINESS -> storeViewModel.resetBusinessToRoot()
                                else -> {}
                            }
                        }
                        storeViewModel.setActiveSubNavTab(target)
                        currentTab = target
                    },
                    canSell = canSell,
                    canViewStock = canViewStock,
                    canSeeHistory = canSeeHistory,
                    canViewReports = canViewReports,
                    isFatherMode = isFatherMode
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    ShopNavTab.HOME -> HomeDashboardContent(
                        storeViewModel = storeViewModel,
                        onNavigateToSell = { if (canSell) currentTab = ShopNavTab.SELL },
                        onNavigateToStock = { if (canViewStock) currentTab = ShopNavTab.STOCK },
                        onNavigateToHistory = {
                            if (canSeeHistory) currentTab = ShopNavTab.HISTORY
                            else if (canViewReports) openSubDestination(ShopNavTab.BUSINESS, BusinessNavDestination.DASHBOARD)
                        },
                        onOpenSaleDetail = { sale ->
                            if (canSeeHistory) {
                                storeViewModel.selectSaleDetail(sale)
                                currentTab = ShopNavTab.HISTORY
                            }
                        },
                        onOpenBaki = {
                            if (canAccessBaki) {
                                val tab = if (canViewReports) ShopNavTab.BUSINESS else ShopNavTab.MORE
                                openSubDestination(tab, BusinessNavDestination.BAKI_SCREEN)
                            }
                        },
                        onOpenCashAndBank = {
                            if (canAccessCashAndBank) {
                                openSubDestination(ShopNavTab.MORE, BusinessNavDestination.CASH_BANK_SCREEN)
                            }
                        },
                        onOpenSuppliers = {
                            if (canAccessSuppliers) {
                                val tab = if (canViewReports) ShopNavTab.BUSINESS else ShopNavTab.MORE
                                openSubDestination(tab, BusinessNavDestination.SUPPLIERS_SCREEN)
                            }
                        },
                        onOpenLowStock = {
                            if (isFatherMode && canViewStock) {
                                storeViewModel.setStockFilterLowOnlyTrigger(true)
                                currentTab = ShopNavTab.STOCK
                            } else if (canViewStock && canViewReports) {
                                openSubDestination(ShopNavTab.BUSINESS, BusinessNavDestination.LOW_STOCK_SCREEN)
                            } else if (canViewStock) {
                                currentTab = ShopNavTab.STOCK
                            }
                        },
                        onOpenCashflowDetail = {
                            if (canViewReports) {
                                openSubDestination(ShopNavTab.BUSINESS, BusinessNavDestination.REPORT_DAY_BOOK)
                            } else if (canAccessCashAndBank) {
                                openSubDestination(ShopNavTab.MORE, BusinessNavDestination.CASH_BANK_SCREEN)
                            }
                        },
                        onRestockProduct = { prod ->
                            if (canAddEdit) quickRestockProduct = prod
                        },
                        onRequestSwitchToDetailed = {
                            if (isFatherMode) {
                                storeViewModel.setSimpleMode(false)
                            } else {
                                activeDialog = HomeQuickDialog.CONFIRM_DETAILED_MODE
                            }
                        },
                        onQuickPaymentIn = { activeDialog = HomeQuickDialog.PAYMENT_IN },
                        onQuickPaymentOut = { activeDialog = HomeQuickDialog.PAYMENT_OUT },
                        onQuickPurchase = { activeDialog = HomeQuickDialog.PURCHASE },
                        onQuickExpense = { activeDialog = HomeQuickDialog.EXPENSE },
                        onQuickAddParty = { activeDialog = HomeQuickDialog.ADD_PARTY },
                        onAddProduct = { if (canAddEdit) showAddWizard = true },
                        onOpenReports = {
                            if (canViewReports) {
                                openSubDestination(ShopNavTab.MORE, BusinessNavDestination.FINANCIAL_REPORTS_HUB)
                            }
                        }
                    )
                    ShopNavTab.STOCK -> if (canViewStock) {
                        StockScreen(storeViewModel = storeViewModel)
                    } else {
                        NoPermissionScreen(onBack = { currentTab = ShopNavTab.HOME })
                    }
                    ShopNavTab.SELL -> if (canSell) {
                        SellScreen(
                            storeViewModel = storeViewModel,
                            onBack = { currentTab = ShopNavTab.HOME }
                        )
                    } else {
                        NoPermissionScreen(onBack = { currentTab = ShopNavTab.HOME })
                    }
                    ShopNavTab.HISTORY -> if (canSeeHistory) {
                        HistoryScreen(
                            storeViewModel = storeViewModel,
                            onBack = { currentTab = ShopNavTab.HOME },
                            onNavigateToStock = { currentTab = ShopNavTab.STOCK }
                        )
                    } else {
                        NoPermissionScreen(onBack = { currentTab = ShopNavTab.HOME })
                    }
                    ShopNavTab.BUSINESS -> if (canViewReports) {
                        BusinessScreen(
                            authViewModel = authViewModel,
                            storeViewModel = storeViewModel
                        )
                    } else {
                        NoPermissionScreen(onBack = { currentTab = ShopNavTab.HOME })
                    }
                    ShopNavTab.MORE -> MoreScreen(
                        authViewModel = authViewModel,
                        storeViewModel = storeViewModel,
                        onBack = { currentTab = ShopNavTab.HOME },
                        onOpenSettings = if (isFatherMode) null else { { showSettings = true } }
                    )
                }
            }
        }
    }

    // Dialogs & Quick Actions
    when (activeDialog) {
        HomeQuickDialog.NOTIFICATIONS -> HomeNotificationsDialog(
            lowStockProducts = lowStockProducts,
            bakiOverview = bakiOverview,
            syncStatus = syncStatus,
            currentLanguage = currentLanguage,
            canViewStock = canViewStock,
            canAccessBaki = canAccessBaki,
            onRestockProduct = { prod ->
                if (canAddEdit) quickRestockProduct = prod
                else if (canViewStock) currentTab = ShopNavTab.STOCK
            },
            onOpenLowStock = {
                if (canViewStock && canViewReports) {
                    openSubDestination(ShopNavTab.BUSINESS, BusinessNavDestination.LOW_STOCK_SCREEN)
                } else if (canViewStock) {
                    currentTab = ShopNavTab.STOCK
                }
            },
            onOpenBaki = {
                if (canAccessBaki) {
                    val tab = if (canViewReports) ShopNavTab.BUSINESS else ShopNavTab.MORE
                    openSubDestination(tab, BusinessNavDestination.BAKI_SCREEN)
                }
            },
            onDismiss = { activeDialog = HomeQuickDialog.NONE }
        )
        HomeQuickDialog.PAYMENT_IN -> HomePaymentInDialog(
            storeViewModel = storeViewModel,
            currentLanguage = currentLanguage,
            onDismiss = { activeDialog = HomeQuickDialog.NONE }
        )
        HomeQuickDialog.PAYMENT_OUT -> HomePaymentOutDialog(
            storeViewModel = storeViewModel,
            currentLanguage = currentLanguage,
            onDismiss = { activeDialog = HomeQuickDialog.NONE }
        )
        HomeQuickDialog.PURCHASE -> HomeRecordPurchaseDialog(
            storeViewModel = storeViewModel,
            currentLanguage = currentLanguage,
            onDismiss = { activeDialog = HomeQuickDialog.NONE }
        )
        HomeQuickDialog.EXPENSE -> AddExpenseDialog(
            storeViewModel = storeViewModel,
            currentLanguage = currentLanguage,
            onDismiss = { activeDialog = HomeQuickDialog.NONE },
            onConfirm = { amount, category, date, accId, accName, note, receiptUri ->
                storeViewModel.recordExpense(
                    amount = amount,
                    category = category,
                    date = date,
                    accountId = accId,
                    accountName = accName,
                    note = note,
                    receiptUri = receiptUri,
                    onSuccess = { activeDialog = HomeQuickDialog.NONE }
                )
            }
        )
        HomeQuickDialog.ADD_PARTY -> HomeAddPartyDialog(
            storeViewModel = storeViewModel,
            currentLanguage = currentLanguage,
            canAddCustomer = canAccessBaki,
            canAddSupplier = canAccessSuppliers,
            onDismiss = { activeDialog = HomeQuickDialog.NONE }
        )
        HomeQuickDialog.CONFIRM_DETAILED_MODE -> AlertDialog(
            onDismissRequest = { activeDialog = HomeQuickDialog.NONE },
            containerColor = Color(0xFF1A1E1A),
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Text(
                    text = if (currentLanguage == AppLanguage.BN) "ডিটেইলড মোডে যাবেন?" else "Switch to Detailed Mode?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (currentLanguage == AppLanguage.BN) "সকল হিসাব, ক্যাশফ্লো এবং দ্রুত অ্যাকশন দেখতে ডিটেইলড মোড চালু করুন।"
                    else "Show profit, cash & bank, supplier dues, 7-day cashflow, and recent sales on Home."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        storeViewModel.setSimpleMode(false)
                        activeDialog = HomeQuickDialog.NONE
                    }
                ) {
                    Text("Switch", color = VividGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { activeDialog = HomeQuickDialog.NONE }) {
                    Text("Stay Simple", color = TextSecondary)
                }
            }
        )
        HomeQuickDialog.FATHER_LOCK_INFO -> FatherLockInfoDialog(
            currentLanguage = currentLanguage,
            isOwner = isOwner,
            onTryDetailedMode = {
                activeDialog = HomeQuickDialog.NONE
                storeViewModel.setSimpleMode(false)
            },
            onTurnOffFatherMode = {
                activeDialog = HomeQuickDialog.NONE
                showFatherExitPinDialog = true
            },
            onDismiss = { activeDialog = HomeQuickDialog.NONE }
        )
        HomeQuickDialog.NONE -> {}
    }

    if (showFatherExitPinDialog && isOwner) {
        val targetMemberId = currentMember?.userId?.ifBlank { null } ?: store?.ownerId ?: ""
        BackupPinDialog(
            mode = if (hasOwnerPin) BackupPinDialogMode.ENTER_PIN else BackupPinDialogMode.SET_PIN,
            currentLanguage = currentLanguage,
            customDescription = ShopLocale.get("father_mode_pin_toggle_desc", currentLanguage),
            onSetPin = { pin, _ ->
                storeViewModel.setOwnerPin(pin) {
                    storeViewModel.setFatherModeForMember(
                        memberId = targetMemberId,
                        enabled = false,
                        memberDisplayName = currentMember?.displayName ?: "",
                        memberEmail = currentMember?.email ?: "",
                        onSuccess = { showFatherExitPinDialog = false }
                    )
                }
            },
            onVerifyPin = { pin, onResult ->
                storeViewModel.verifyOwnerPin(pin) { ok ->
                    onResult(ok)
                    if (ok) {
                        storeViewModel.setFatherModeForMember(
                            memberId = targetMemberId,
                            enabled = false,
                            memberDisplayName = currentMember?.displayName ?: "",
                            memberEmail = currentMember?.email ?: "",
                            onSuccess = { showFatherExitPinDialog = false }
                        )
                    }
                }
            },
            onChangePin = { _, _, _ -> },
            onDismiss = { showFatherExitPinDialog = false }
        )
    }

    if (quickRestockProduct != null) {
        QuickRestockDialog(
            product = quickRestockProduct!!,
            isFatherMode = isFatherMode,
            onDismiss = { quickRestockProduct = null },
            onConfirm = { amount ->
                storeViewModel.addQuickStock(quickRestockProduct!!.id, amount)
                quickRestockProduct = null
            }
        )
    }

    if (showAddWizard) {
        val shopType by storeViewModel.shopType.collectAsState()
        val availableCategories by storeViewModel.availableProductCategories.collectAsState()
        val unitPresets by storeViewModel.unitPresets.collectAsState()
        val photoStorageMode by storeViewModel.photoStorageMode.collectAsState()
        AddProductWizardDialog(
            currentLanguage = currentLanguage,
            isBusy = isBusy,
            canSeeBuyingPrice = canSeeProfit,
            isSimpleMode = isSimpleMode,
            isFatherMode = isFatherMode,
            hasOwnerPin = hasOwnerPin,
            onVerifyOwnerPin = { pin, onResult -> storeViewModel.verifyOwnerPin(pin, onResult) },
            onSetOwnerPin = { pin, onDone -> storeViewModel.setOwnerPin(pin, onDone) },
            shopType = shopType,
            availableCategories = availableCategories,
            unitPresets = unitPresets,
            photoStorageMode = photoStorageMode,
            externalError = errorMessage,
            onDismiss = {
                storeViewModel.clearErrorMessage()
                showAddWizard = false
            },
            onConfirm = { name, nameBn, category, unit, buy, sell, stock, lowLimit, photo, barcode, wholesale, mrp, location, partyRate, buyUnit, buyConversion, sellUnits ->
                storeViewModel.addNewProductWizard(
                    name = name,
                    nameBn = nameBn,
                    category = category,
                    unit = unit,
                    buyPrice = buy,
                    sellPrice = sell,
                    stock = stock,
                    lowStockLimit = lowLimit,
                    photoUri = photo,
                    barcode = barcode,
                    wholesalePrice = wholesale,
                    mrp = mrp,
                    itemLocation = location,
                    partyRate = partyRate,
                    buyUnit = buyUnit,
                    buyConversion = buyConversion,
                    sellUnits = sellUnits,
                    onSuccess = { showAddWizard = false }
                )
            }
        )
    }

    if (editingProduct != null) {
        val shopType by storeViewModel.shopType.collectAsState()
        val availableCategories by storeViewModel.availableProductCategories.collectAsState()
        val unitPresets by storeViewModel.unitPresets.collectAsState()
        val photoStorageMode by storeViewModel.photoStorageMode.collectAsState()
        AddProductWizardDialog(
            productToEdit = editingProduct,
            currentLanguage = currentLanguage,
            isBusy = isBusy,
            canSeeBuyingPrice = canSeeProfit,
            isSimpleMode = isSimpleMode,
            isFatherMode = isFatherMode,
            hasOwnerPin = hasOwnerPin,
            onVerifyOwnerPin = { pin, onResult -> storeViewModel.verifyOwnerPin(pin, onResult) },
            onSetOwnerPin = { pin, onDone -> storeViewModel.setOwnerPin(pin, onDone) },
            shopType = shopType,
            availableCategories = availableCategories,
            unitPresets = unitPresets,
            photoStorageMode = photoStorageMode,
            externalError = errorMessage,
            onDismiss = {
                storeViewModel.clearErrorMessage()
                editingProduct = null
            },
            onConfirm = { name, nameBn, category, unit, buy, sell, stock, lowLimit, photo, barcode, wholesale, mrp, location, partyRate, buyUnit, buyConversion, sellUnits ->
                storeViewModel.updateProductWizard(
                    productId = editingProduct!!.id,
                    name = name,
                    nameBn = nameBn,
                    category = category,
                    unit = unit,
                    buyPrice = buy,
                    sellPrice = sell,
                    stock = stock,
                    lowStockLimit = lowLimit,
                    photoUri = photo,
                    barcode = barcode,
                    wholesalePrice = wholesale,
                    mrp = mrp,
                    itemLocation = location,
                    partyRate = partyRate,
                    buyUnit = buyUnit,
                    buyConversion = buyConversion,
                    sellUnits = sellUnits,
                    onSuccess = { editingProduct = null }
                )
            },
            onDelete = if (isOwner || isFatherMode) {
                {
                    val target = editingProduct
                    if (target != null) {
                        storeViewModel.deleteProduct(target.id) {
                            editingProduct = null
                        }
                    }
                }
            } else null
        )
    }
}

@Composable
fun ShopTopBar(
    storeName: String,
    syncStatus: SyncStatus,
    currentLanguage: AppLanguage,
    isSimpleMode: Boolean,
    isFatherMode: Boolean = false,
    isPrivacyHidden: Boolean,
    alertCount: Int,
    isMoreSelected: Boolean,
    onTogglePrivacy: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenFatherLockInfo: () -> Unit = {},
    onOpenMore: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val dateLabel = remember(currentLanguage) {
        SimpleDateFormat("EEE, dd MMM yyyy", Locale.US).format(Date())
    }
    val greetingLabel = remember(currentLanguage) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (currentLanguage == AppLanguage.BN) {
            when {
                hour < 12 -> "শুভ সকাল"
                hour < 17 -> "শুভ দুপুর"
                else -> "শুভ সন্ধ্যা"
            }
        } else {
            when {
                hour < 12 -> "Good morning"
                hour < 17 -> "Good afternoon"
                else -> "Good evening"
            }
        }
    }

    val syncDotColor = when (syncStatus) {
        SyncStatus.ONLINE -> VividGreen
        SyncStatus.SYNCING -> Color(0xFFFFB300)
        SyncStatus.OFFLINE -> AlertRed
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .padding(end = 6.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Transparent,
                border = BorderStroke(1.dp, Color(0x36FFFFFF)),
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF1F301B), Color(0xFF131912))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = storeName,
                        tint = VividGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (isSimpleMode) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = storeName,
                            color = TextPrimary,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(syncDotColor)
                        )
                    }
                    Text(
                        text = dateLabel,
                        color = TextSecondary,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = greetingLabel,
                        color = TextSecondary,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = storeName,
                            color = TextPrimary,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(syncDotColor)
                        )
                    }
                }
            }
        }

        // Top-right controls: Father Mode lock chip (when locked) OR standard buttons (Settings hidden in Father mode)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isFatherMode) {
                val amberAccent = Color(0xFFFFB300)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0x334D3800),
                    border = BorderStroke(1.dp, amberAccent.copy(alpha = 0.72f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(onClick = onOpenFatherLockInfo)
                        .testTag("top_bar_father_mode_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = ShopLocale.get("father_mode_chip", currentLanguage),
                            tint = amberAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = ShopLocale.get("father_mode_chip", currentLanguage),
                            color = amberAccent,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            TopBarCircleIconButton(
                icon = if (isPrivacyHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (isPrivacyHidden) "Show amounts" else "Hide amounts",
                isActive = isPrivacyHidden,
                onClick = onTogglePrivacy,
                testTag = "top_bar_privacy_btn"
            )
            TopBarCircleIconButton(
                icon = Icons.Default.NotificationsNone,
                contentDescription = "Alerts",
                hasBadge = alertCount > 0,
                onClick = onOpenNotifications,
                testTag = "top_bar_notifications_btn"
            )
            TopBarCircleIconButton(
                icon = Icons.Default.GridView,
                contentDescription = "More",
                isActive = isMoreSelected,
                onClick = onOpenMore,
                testTag = "nav_more"
            )
            if (!isFatherMode) {
                TopBarCircleIconButton(
                    icon = Icons.Default.Settings,
                    contentDescription = "Settings",
                    onClick = onOpenSettings,
                    testTag = "top_bar_settings_btn"
                )
            }
        }
    }
}

@Composable
private fun TopBarCircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean = false,
    hasBadge: Boolean = false,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = CircleShape,
        color = Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (isActive) VividGreen.copy(alpha = 0.7f) else Color(0x2EFFFFFF)
        ),
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isActive) {
                            listOf(Color(0x385DD62C), Color(0x1C5DD62C))
                        } else {
                            listOf(Color(0xFF262A26), Color(0xFF161816))
                        }
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isActive) VividGreen else TextPrimary,
                modifier = Modifier.size(17.dp)
            )
            if (hasBadge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 6.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(VividGreen)
                )
            }
        }
    }
}

@Composable
fun HomeDashboardContent(
    storeViewModel: StoreViewModel,
    onNavigateToSell: () -> Unit,
    onNavigateToStock: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onOpenSaleDetail: (com.example.data.model.Sale) -> Unit,
    onOpenBaki: () -> Unit,
    onOpenCashAndBank: () -> Unit,
    onOpenSuppliers: () -> Unit,
    onOpenLowStock: () -> Unit,
    onOpenCashflowDetail: () -> Unit,
    onRestockProduct: (Product) -> Unit,
    onRequestSwitchToDetailed: () -> Unit,
    onQuickPaymentIn: () -> Unit,
    onQuickPaymentOut: () -> Unit,
    onQuickPurchase: () -> Unit,
    onQuickExpense: () -> Unit,
    onQuickAddParty: () -> Unit,
    onAddProduct: () -> Unit = {},
    onOpenReports: () -> Unit = {}
) {
    val currentLanguage by storeViewModel.currentLanguage.collectAsState()
    val financials by storeViewModel.financials.collectAsState()
    val stockSummary by storeViewModel.stockSummary.collectAsState()
    val homeInsights by storeViewModel.homeInsights.collectAsState()
    val bakiOverview by storeViewModel.bakiOverview.collectAsState()
    val totalBalance by storeViewModel.totalBalance.collectAsState()
    val moneyNow by storeViewModel.moneyNow.collectAsState()
    val lowStockProducts by storeViewModel.lowStockProducts.collectAsState()
    val sevenDayCashflow by storeViewModel.sevenDayCashflow.collectAsState()
    val recentSales by storeViewModel.recentSales.collectAsState()
    val isSimpleMode by storeViewModel.isSimpleMode.collectAsState()
    val isFatherMode by storeViewModel.isFatherMode.collectAsState()
    val isOwner by storeViewModel.isOwner.collectAsState()
    val isPrivacyHidden by storeViewModel.isPrivacyHidden.collectAsState()
    val businessStrategy by storeViewModel.businessStrategy.collectAsState()
    val isStrategyRefreshing by storeViewModel.isStrategyRefreshing.collectAsState()
    val strategyRefreshesRemaining by storeViewModel.strategyRefreshesRemainingToday.collectAsState()

    val canSell by storeViewModel.canSellFlow.collectAsState()
    val canAddEdit by storeViewModel.canAddEditProductsFlow.collectAsState()
    val canSeeProfit by storeViewModel.canSeeBuyingPriceAndProfitFlow.collectAsState()
    val canViewStock by storeViewModel.canViewStockFlow.collectAsState()
    val canSeeHistory by storeViewModel.canSeeHistoryFlow.collectAsState()
    val canViewReports by storeViewModel.canViewReportsFlow.collectAsState()
    val canAccessBaki by storeViewModel.canAccessCustomersAndBakiFlow.collectAsState()
    val canReceivePayment by storeViewModel.canReceivePaymentFlow.collectAsState()
    val canAccessCashAndBank by storeViewModel.canAccessCashAndBankFlow.collectAsState()
    val canAccessSuppliers by storeViewModel.canAccessSuppliersAndPurchasesFlow.collectAsState()
    val canPaySupplier by storeViewModel.canPaySupplierFlow.collectAsState()
    val canAccessExpenses by storeViewModel.canAccessExpensesAndIncomeFlow.collectAsState()

    val canSeeSalesSummary = canSeeHistory || canViewReports
    val canViewStrategy = canViewReports
    val canRefreshStrategy = isOwner && !isFatherMode

    val handleStrategyAction: (StrategyActionTarget) -> Unit = { target ->
        when (target) {
            StrategyActionTarget.SELL -> if (canSell) onNavigateToSell() else onOpenReports()
            StrategyActionTarget.STOCK -> if (canViewStock) onNavigateToStock() else onOpenReports()
            StrategyActionTarget.LOW_STOCK -> if (canViewStock) onOpenLowStock() else onOpenReports()
            StrategyActionTarget.BAKI -> if (canAccessBaki || canReceivePayment) onOpenBaki() else onOpenReports()
            StrategyActionTarget.REPORTS -> if (canViewReports) onOpenReports()
        }
    }

    if (isFatherMode) {
        FatherModeHomeLayout(
            financials = financials,
            stockSummary = stockSummary,
            currentLanguage = currentLanguage,
            isPrivacyHidden = isPrivacyHidden,
            onNavigateToSell = onNavigateToSell,
            onAddProduct = onAddProduct,
            onOpenTodaySummary = onNavigateToHistory,
            onOpenLowStock = onOpenLowStock,
            onOpenReport = onOpenReports,
            businessStrategy = businessStrategy,
            onStrategyAction = handleStrategyAction
        )
    } else if (isSimpleMode) {
        SimpleHomeLayout(
            financials = financials,
            homeInsights = homeInsights,
            stockSummary = stockSummary,
            bakiOverview = bakiOverview,
            currentLanguage = currentLanguage,
            isPrivacyHidden = isPrivacyHidden,
            canSell = canSell,
            canViewStock = canViewStock,
            canAccessBaki = canAccessBaki || canReceivePayment,
            canSeeSalesSummary = canSeeSalesSummary,
            onRequestSwitchToDetailed = onRequestSwitchToDetailed,
            onNavigateToSell = onNavigateToSell,
            onNavigateToStock = onNavigateToStock,
            onNavigateToBaki = onOpenBaki,
            onNavigateToSalesSummary = onNavigateToHistory,
            businessStrategy = businessStrategy,
            canViewStrategy = canViewStrategy,
            canRefreshStrategy = canRefreshStrategy,
            isStrategyRefreshing = isStrategyRefreshing,
            strategyRefreshesRemaining = strategyRefreshesRemaining,
            onRefreshStrategy = { storeViewModel.refreshBusinessStrategy() },
            onStrategyAction = handleStrategyAction
        )
    } else {
        DetailedHomeLayout(
            financials = financials,
            homeInsights = homeInsights,
            bakiOverview = bakiOverview,
            totalBalance = totalBalance,
            moneyNow = moneyNow,
            lowStockProducts = lowStockProducts,
            sevenDayCashflow = sevenDayCashflow,
            recentSales = recentSales,
            currentLanguage = currentLanguage,
            isPrivacyHidden = isPrivacyHidden,
            canSeeSalesSummary = canSeeSalesSummary,
            canSeeProfit = canSeeProfit,
            canAccessBaki = canAccessBaki,
            canReceivePayment = canReceivePayment,
            canAccessCashAndBank = canAccessCashAndBank,
            canAccessSuppliers = canAccessSuppliers,
            canPaySupplier = canPaySupplier,
            canViewStock = canViewStock,
            canAddEditProducts = canAddEdit,
            canViewReports = canViewReports,
            canAccessExpenses = canAccessExpenses,
            canSeeHistory = canSeeHistory,
            onOpenSalesSummary = onNavigateToHistory,
            onOpenBaki = onOpenBaki,
            onOpenCashAndBank = onOpenCashAndBank,
            onOpenSuppliers = onOpenSuppliers,
            onOpenLowStock = onOpenLowStock,
            onRestockProduct = onRestockProduct,
            onOpenCashflowDetail = onOpenCashflowDetail,
            onOpenHistory = onNavigateToHistory,
            onOpenSaleDetail = onOpenSaleDetail,
            onQuickPaymentIn = onQuickPaymentIn,
            onQuickPaymentOut = onQuickPaymentOut,
            onQuickPurchase = onQuickPurchase,
            onQuickExpense = onQuickExpense,
            onQuickAddParty = onQuickAddParty,
            businessStrategy = businessStrategy,
            canViewStrategy = canViewStrategy,
            canRefreshStrategy = canRefreshStrategy,
            isStrategyRefreshing = isStrategyRefreshing,
            strategyRefreshesRemaining = strategyRefreshesRemaining,
            onRefreshStrategy = { storeViewModel.refreshBusinessStrategy() },
            onStrategyAction = handleStrategyAction
        )
    }
}
