package com.example.util

import com.example.data.model.AccountStatementReport
import com.example.data.model.Expense
import com.example.data.model.OtherIncome
import com.example.data.model.Product
import com.example.data.model.Purchase
import com.example.data.model.PurchaseReturn
import com.example.data.model.Sale
import com.example.data.model.Store
import com.example.domain.CustomerStatementReport
import com.example.domain.DayBookEntry
import com.example.domain.LedgerEntryType
import com.example.domain.MonthlyReportData
import com.example.domain.ProfitAndLossStatement
import com.example.domain.StaffReportSummary
import com.example.domain.SupplierStatementReport

object ReportDataBuilders {

    private fun nowFormatted(lang: AppLanguage): String =
        ShopLocale.formatDateLong(System.currentTimeMillis(), lang)

    fun buildMonthlyReportTable(
        reportData: MonthlyReportData,
        year: Int,
        month: Int,
        store: Store?,
        canSeeProfit: Boolean,
        lang: AppLanguage = AppLanguage.EN,
        includeSupplierDebt: Boolean = true
    ): ReportTableData {
        val currentMonthLabel = ShopLocale.formatMonthYear(year, month, lang)

        val headers = if (canSeeProfit) {
            listOf(
                ShopLocale.get("col_product", lang),
                ShopLocale.get("col_qty_sold", lang),
                ShopLocale.get("col_total_amount", lang),
                ShopLocale.get("col_profit", lang)
            )
        } else {
            listOf(
                ShopLocale.get("col_product", lang),
                ShopLocale.get("col_qty_sold", lang),
                ShopLocale.get("col_total_amount", lang)
            )
        }

        val rows = reportData.itemsSold.map { stat ->
            val qtyWithUnit = "${ShopLocale.formatNumber(stat.quantitySold, lang)} ${ShopLocale.displayUnit(stat.unit, lang)}"
            if (canSeeProfit) {
                listOf(
                    stat.productName,
                    qtyWithUnit,
                    ShopLocale.formatMoney(stat.totalSalesAmount, lang),
                    ShopLocale.formatMoney(stat.totalProfit, lang)
                )
            } else {
                listOf(
                    stat.productName,
                    qtyWithUnit,
                    ShopLocale.formatMoney(stat.totalSalesAmount, lang)
                )
            }
        }
        val rawRows = reportData.itemsSold.map { stat ->
            if (canSeeProfit) {
                listOf(null, stat.quantitySold.toDouble(), stat.totalSalesAmount, stat.totalProfit)
            } else {
                listOf(null, stat.quantitySold.toDouble(), stat.totalSalesAmount)
            }
        }

        val totalOrdersText = ShopLocale.format("total_orders_row", lang, reportData.totalOrders)
        val totalItemsText = ShopLocale.format("items_count_label", lang, reportData.totalItemsSold)
        val totals = if (canSeeProfit) {
            listOf(
                totalOrdersText,
                totalItemsText,
                ShopLocale.formatMoney(reportData.totalSales, lang),
                ShopLocale.formatMoney(reportData.totalProfit, lang)
            )
        } else {
            listOf(
                totalOrdersText,
                totalItemsText,
                ShopLocale.formatMoney(reportData.totalSales, lang)
            )
        }
        val rawTotals = if (canSeeProfit) {
            listOf(null, reportData.totalItemsSold.toDouble(), reportData.totalSales, reportData.totalProfit)
        } else {
            listOf(null, reportData.totalItemsSold.toDouble(), reportData.totalSales)
        }

        val title = ShopLocale.format("rep_monthly_title", lang, currentMonthLabel)
        val notes = when {
            !includeSupplierDebt && canSeeProfit -> {
                if (lang == AppLanguage.BN) {
                    "মজুদ পণ্যের মূল্য: ${ShopLocale.formatMoney(reportData.stockValueAtCost, lang)} | পাওনা বাকি: ${ShopLocale.formatMoney(reportData.bakiCustomersOwe, lang)}"
                } else {
                    "Stock Value: ${ShopLocale.formatMoney(reportData.stockValueAtCost, lang)} | Total Baki: ${ShopLocale.formatMoney(reportData.bakiCustomersOwe, lang)}"
                }
            }
            !includeSupplierDebt -> {
                if (lang == AppLanguage.BN) {
                    "পাওনা বাকি: ${ShopLocale.formatMoney(reportData.bakiCustomersOwe, lang)}"
                } else {
                    "Total Baki: ${ShopLocale.formatMoney(reportData.bakiCustomersOwe, lang)}"
                }
            }
            canSeeProfit -> ShopLocale.format(
                "monthly_notes_profit",
                lang,
                ShopLocale.formatMoney(reportData.stockValueAtCost, lang),
                ShopLocale.formatMoney(reportData.bakiCustomersOwe, lang),
                ShopLocale.formatMoney(reportData.debtToSuppliers, lang)
            )
            else -> ShopLocale.format(
                "monthly_notes_no_profit",
                lang,
                ShopLocale.formatMoney(reportData.bakiCustomersOwe, lang),
                ShopLocale.formatMoney(reportData.debtToSuppliers, lang)
            )
        }

        val summaryMetrics = buildList {
            add(
                ReportSummaryMetric(
                    label = if (lang == AppLanguage.BN) "মোট বিক্রয়" else "Total Sales",
                    value = ShopLocale.formatMoney(reportData.totalSales, lang),
                    detail = ShopLocale.format("total_orders_row", lang, reportData.totalOrders)
                )
            )
            if (canSeeProfit) {
                val marginPct = if (reportData.totalSales > 0.0) {
                    (reportData.totalProfit / reportData.totalSales) * 100.0
                } else 0.0
                add(
                    ReportSummaryMetric(
                        label = if (lang == AppLanguage.BN) "মোট লাভ" else "Total Profit",
                        value = ShopLocale.formatMoney(reportData.totalProfit, lang),
                        detail = if (lang == AppLanguage.BN) {
                            "লাভের হার: ${ShopLocale.formatNumber(marginPct.toInt(), lang)}%"
                        } else {
                            "Margin: ${String.format(java.util.Locale.US, "%.1f%%", marginPct)}"
                        }
                    )
                )
                add(
                    ReportSummaryMetric(
                        label = if (lang == AppLanguage.BN) "মজুদ পণ্যের মূল্য" else "Stock Value (Cost)",
                        value = ShopLocale.formatMoney(reportData.stockValueAtCost, lang)
                    )
                )
            }
            add(
                ReportSummaryMetric(
                    label = if (lang == AppLanguage.BN) "ক্রেতাদের বাকি" else "Customer Baki",
                    value = ShopLocale.formatMoney(reportData.bakiCustomersOwe, lang)
                )
            )
        }

        return ReportTableData(
            title = title,
            storeName = store?.name ?: ShopLocale.get("receipt_header_store_default", lang),
            storePhone = store?.phone ?: "",
            storeAddress = store?.address ?: "",
            periodLabel = currentMonthLabel,
            generatedAt = nowFormatted(lang),
            headers = headers,
            rows = rows,
            numericColumns = if (canSeeProfit) setOf(1, 2, 3) else setOf(1, 2),
            rawNumbers = rawRows,
            totals = totals,
            rawTotals = rawTotals,
            notes = notes,
            summaryMetrics = summaryMetrics
        )
    }

    fun buildSalesReportTable(
        sales: List<Sale>,
        store: Store?,
        periodLabel: String,
        canSeeProfit: Boolean,
        lang: AppLanguage = AppLanguage.EN
    ): ReportTableData {
        val totalAmount = sales.sumOf { it.grandTotal }
        val totalDiscount = sales.sumOf { it.discount }
        val totalProfit = sales.sumOf { it.profit }

        val rows = sales.map { s ->
            val dateStr = s.createdAt?.toDate()?.time?.let { ShopLocale.formatDateShort(it, lang) } ?: "—"
            val status = if (s.paymentType == "baki") ShopLocale.get("status_baki", lang) else ShopLocale.get("status_paid", lang)
            val custName = ShopLocale.displayCustomerName(s.customerName, lang)
            if (canSeeProfit) {
                listOf(
                    s.invoiceNo,
                    dateStr,
                    custName,
                    ShopLocale.formatMoney(s.grandTotal, lang),
                    ShopLocale.formatMoney(s.discount, lang),
                    ShopLocale.formatMoney(s.profit, lang),
                    status
                )
            } else {
                listOf(
                    s.invoiceNo,
                    dateStr,
                    custName,
                    ShopLocale.formatMoney(s.grandTotal, lang),
                    ShopLocale.formatMoney(s.discount, lang),
                    status
                )
            }
        }
        val rawRows = sales.map { s ->
            if (canSeeProfit) {
                listOf(null, null, null, s.grandTotal, s.discount, s.profit, null)
            } else {
                listOf(null, null, null, s.grandTotal, s.discount, null)
            }
        }
        val totalLabel = ShopLocale.format("total_orders_row", lang, sales.size)
        val totals = if (canSeeProfit) {
            listOf(
                totalLabel,
                "",
                "",
                ShopLocale.formatMoney(totalAmount, lang),
                ShopLocale.formatMoney(totalDiscount, lang),
                ShopLocale.formatMoney(totalProfit, lang),
                ""
            )
        } else {
            listOf(
                totalLabel,
                "",
                "",
                ShopLocale.formatMoney(totalAmount, lang),
                ShopLocale.formatMoney(totalDiscount, lang),
                ""
            )
        }
        val rawTotals = if (canSeeProfit) {
            listOf(null, null, null, totalAmount, totalDiscount, totalProfit, null)
        } else {
            listOf(null, null, null, totalAmount, totalDiscount, null)
        }

        val headers = if (canSeeProfit) {
            listOf(
                ShopLocale.get("col_invoice", lang),
                ShopLocale.get("col_date", lang),
                ShopLocale.get("col_customer", lang),
                ShopLocale.get("col_total", lang),
                ShopLocale.get("col_discount", lang),
                ShopLocale.get("col_profit", lang),
                ShopLocale.get("col_status", lang)
            )
        } else {
            listOf(
                ShopLocale.get("col_invoice", lang),
                ShopLocale.get("col_date", lang),
                ShopLocale.get("col_customer", lang),
                ShopLocale.get("col_total", lang),
                ShopLocale.get("col_discount", lang),
                ShopLocale.get("col_status", lang)
            )
        }

        val paidTotal = sales.filter { it.paymentType != "baki" }.sumOf { it.grandTotal }
        val bakiTotal = sales.filter { it.paymentType == "baki" }.sumOf { it.grandTotal }
        val salesSummary = buildList {
            add(
                ReportSummaryMetric(
                    label = if (lang == AppLanguage.BN) "মোট বিক্রয়" else "Total Sales",
                    value = ShopLocale.formatMoney(totalAmount, lang),
                    detail = totalLabel
                )
            )
            add(
                ReportSummaryMetric(
                    label = if (lang == AppLanguage.BN) "নগদ আদায়" else "Paid Sales",
                    value = ShopLocale.formatMoney(paidTotal, lang)
                )
            )
            add(
                ReportSummaryMetric(
                    label = if (lang == AppLanguage.BN) "বাকি বিক্রয়" else "Baki (Due)",
                    value = ShopLocale.formatMoney(bakiTotal, lang)
                )
            )
            if (canSeeProfit) {
                add(
                    ReportSummaryMetric(
                        label = if (lang == AppLanguage.BN) "মোট লাভ" else "Total Profit",
                        value = ShopLocale.formatMoney(totalProfit, lang),
                        detail = if (lang == AppLanguage.BN) {
                            "ছাড়: ${ShopLocale.formatMoney(totalDiscount, lang)}"
                        } else {
                            "Discount: ${ShopLocale.formatMoney(totalDiscount, lang)}"
                        }
                    )
                )
            }
        }

        return ReportTableData(
            title = ShopLocale.get("rep_sales_title", lang),
            storeName = store?.name ?: ShopLocale.get("receipt_header_store_default", lang),
            storePhone = store?.phone ?: "",
            storeAddress = store?.address ?: "",
            periodLabel = periodLabel,
            generatedAt = nowFormatted(lang),
            headers = headers,
            rows = rows,
            numericColumns = if (canSeeProfit) setOf(3, 4, 5) else setOf(3, 4),
            rawNumbers = rawRows,
            totals = totals,
            rawTotals = rawTotals,
            notes = if (lang == AppLanguage.BN) {
                "মোট অর্ডার: ${ShopLocale.formatNumber(sales.size, lang)}টি • নগদ: ${ShopLocale.formatMoney(paidTotal, lang)} • বাকি: ${ShopLocale.formatMoney(bakiTotal, lang)} • মোট ছাড়: ${ShopLocale.formatMoney(totalDiscount, lang)}"
            } else {
                "Orders: ${sales.size} • Paid: ${ShopLocale.formatMoney(paidTotal, lang)} • Baki: ${ShopLocale.formatMoney(bakiTotal, lang)} • Total Discount: ${ShopLocale.formatMoney(totalDiscount, lang)}"
            },
            summaryMetrics = salesSummary
        )
    }

    fun buildPurchaseReportTable(
        purchases: List<Purchase>,
        returns: List<PurchaseReturn>,
        store: Store?,
        periodLabel: String,
        lang: AppLanguage = AppLanguage.EN
    ): ReportTableData {
        val totalPurchases = purchases.sumOf { it.totalAmount }
        val totalPaid = purchases.sumOf { it.paidAmount }
        val totalDue = purchases.sumOf { it.dueAmount }

        val rows = purchases.map { p ->
            val dateStr = p.createdAt?.toDate()?.time?.let { ShopLocale.formatDateShort(it, lang) } ?: "—"
            val status = if (p.paymentType == "credit") ShopLocale.get("status_credit_due", lang) else ShopLocale.get("status_paid", lang)
            listOf(
                p.purchaseNo,
                dateStr,
                p.supplierName,
                ShopLocale.formatMoney(p.totalAmount, lang),
                ShopLocale.formatMoney(p.paidAmount, lang),
                ShopLocale.formatMoney(p.dueAmount, lang),
                status
            )
        }
        val rawRows = purchases.map { p ->
            listOf(null, null, null, p.totalAmount, p.paidAmount, p.dueAmount, null)
        }
        val totals = listOf(
            ShopLocale.format("total_orders_row", lang, purchases.size),
            "",
            "",
            ShopLocale.formatMoney(totalPurchases, lang),
            ShopLocale.formatMoney(totalPaid, lang),
            ShopLocale.formatMoney(totalDue, lang),
            ""
        )
        val rawTotals = listOf<Double?>(null, null, null, totalPurchases, totalPaid, totalDue, null)
        val totalReturns = returns.sumOf { it.amount }
        val purchaseSummary = listOf(
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "মোট ক্রয়" else "Total Purchases",
                value = ShopLocale.formatMoney(totalPurchases, lang),
                detail = ShopLocale.format("total_orders_row", lang, purchases.size)
            ),
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "পরিশোধিত" else "Total Paid",
                value = ShopLocale.formatMoney(totalPaid, lang)
            ),
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "সরবরাহকারী বকেয়া" else "Total Due",
                value = ShopLocale.formatMoney(totalDue, lang)
            ),
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "ক্রয় ফেরত" else "Purchase Returns",
                value = ShopLocale.formatMoney(totalReturns, lang),
                detail = if (lang == AppLanguage.BN) "${returns.size}টি ফেরত" else "${returns.size} returns"
            )
        )
        return ReportTableData(
            title = ShopLocale.get("rep_purchase_title", lang),
            storeName = store?.name ?: ShopLocale.get("receipt_header_store_default", lang),
            storePhone = store?.phone ?: "",
            storeAddress = store?.address ?: "",
            periodLabel = periodLabel,
            generatedAt = nowFormatted(lang),
            headers = listOf(
                ShopLocale.get("col_order_no", lang),
                ShopLocale.get("col_date", lang),
                ShopLocale.get("col_supplier", lang),
                ShopLocale.get("col_total", lang),
                ShopLocale.get("col_paid", lang),
                ShopLocale.get("col_due", lang),
                ShopLocale.get("col_status", lang)
            ),
            rows = rows,
            numericColumns = setOf(3, 4, 5),
            rawNumbers = rawRows,
            totals = totals,
            rawTotals = rawTotals,
            notes = if (lang == AppLanguage.BN) {
                "মোট ক্রয়: ${ShopLocale.formatMoney(totalPurchases, lang)} | পরিশোধিত: ${ShopLocale.formatMoney(totalPaid, lang)} | বকেয়া: ${ShopLocale.formatMoney(totalDue, lang)} | ফেরত: ${ShopLocale.formatMoney(totalReturns, lang)}"
            } else {
                "Total Purchases: ${ShopLocale.formatMoney(totalPurchases, lang)} | Paid: ${ShopLocale.formatMoney(totalPaid, lang)} | Due: ${ShopLocale.formatMoney(totalDue, lang)} | Returns: ${ShopLocale.formatMoney(totalReturns, lang)}"
            },
            summaryMetrics = purchaseSummary
        )
    }

    fun buildDayBookReportTable(
        entries: List<DayBookEntry>,
        store: Store?,
        periodLabel: String,
        lang: AppLanguage = AppLanguage.EN
    ): ReportTableData {
        val totalIn = entries.filter { it.isIncome }.sumOf { it.amount }
        val totalOut = entries.filter { !it.isIncome }.sumOf { it.amount }

        val rows = entries.map { e ->
            val dateStr = ShopLocale.formatDateShort(e.timestamp, lang)
            val direction = if (e.isIncome) ShopLocale.get("flow_in", lang) else ShopLocale.get("flow_out", lang)
            val typeStr = ShopLocale.displayEntryType(e.type, lang)
            val partyStr = ShopLocale.displayCustomerName(e.partyName, lang)
            listOf(
                dateStr,
                typeStr,
                e.referenceNo,
                partyStr,
                direction,
                ShopLocale.formatMoney(e.amount, lang)
            )
        }
        val rawRows = entries.map { e ->
            listOf(null, null, null, null, null, if (e.isIncome) e.amount else -e.amount)
        }
        val totals = listOf(
            ShopLocale.get("net_flow_row", lang),
            "",
            "",
            ShopLocale.format(
                "daybook_totals_note",
                lang,
                ShopLocale.formatMoney(totalIn, lang),
                ShopLocale.formatMoney(totalOut, lang)
            ),
            "",
            ShopLocale.formatMoney(totalIn - totalOut, lang)
        )
        val rawTotals = listOf<Double?>(null, null, null, null, null, totalIn - totalOut)
        val netFlow = totalIn - totalOut
        val dayBookSummary = listOf(
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "মোট জমা (আয়)" else "Total Cash In",
                value = ShopLocale.formatMoney(totalIn, lang)
            ),
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "মোট খরচ (ব্যয়)" else "Total Cash Out",
                value = ShopLocale.formatMoney(totalOut, lang)
            ),
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "নিট প্রবাহ" else "Net Cash Flow",
                value = ShopLocale.formatMoney(netFlow, lang)
            ),
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "মোট লেনদেন" else "Transactions",
                value = ShopLocale.formatNumber(entries.size, lang)
            )
        )
        return ReportTableData(
            title = ShopLocale.get("rep_day_book_title", lang),
            storeName = store?.name ?: ShopLocale.get("receipt_header_store_default", lang),
            storePhone = store?.phone ?: "",
            storeAddress = store?.address ?: "",
            periodLabel = periodLabel,
            generatedAt = nowFormatted(lang),
            headers = listOf(
                ShopLocale.get("col_datetime", lang),
                ShopLocale.get("col_type", lang),
                ShopLocale.get("col_ref", lang),
                ShopLocale.get("col_party", lang),
                ShopLocale.get("col_flow", lang),
                ShopLocale.get("col_amount", lang)
            ),
            rows = rows,
            numericColumns = setOf(5),
            rawNumbers = rawRows,
            totals = totals,
            rawTotals = rawTotals,
            notes = totals[3],
            summaryMetrics = dayBookSummary
        )
    }

    fun buildProfitLossReportTable(
        pnl: ProfitAndLossStatement,
        isStockBased: Boolean,
        store: Store?,
        periodLabel: String,
        lang: AppLanguage = AppLanguage.EN,
        isFatherMode: Boolean = false
    ): ReportTableData {
        val effectiveStockBased = if (isFatherMode) true else isStockBased
        val cogsMethod = if (effectiveStockBased) ShopLocale.get("pnl_stock_based", lang) else ShopLocale.get("pnl_purchases_based", lang)
        val rows = mutableListOf(
            listOf(ShopLocale.get("pnl_net_sales", lang), ShopLocale.formatMoney(pnl.netSales, lang)),
            listOf(
                ShopLocale.format("pnl_cogs_label", lang, cogsMethod),
                "-${ShopLocale.formatMoney(pnl.costOfGoodsSold, lang)}"
            )
        )
        val rawRows = mutableListOf(
            listOf<Double?>(null, pnl.netSales),
            listOf<Double?>(null, -pnl.costOfGoodsSold)
        )
        if (isFatherMode) {
            rows.add(listOf(ShopLocale.get("col_profit", lang), ShopLocale.formatMoney(pnl.grossProfit, lang)))
            rawRows.add(listOf<Double?>(null, pnl.grossProfit))
        } else {
            rows.add(listOf(ShopLocale.get("pnl_gross_profit", lang), ShopLocale.formatMoney(pnl.grossProfit, lang)))
            rows.add(listOf(ShopLocale.get("pnl_other_income", lang), "+${ShopLocale.formatMoney(pnl.otherIncome, lang)}"))
            rows.add(listOf(ShopLocale.get("pnl_operating_expenses", lang), "-${ShopLocale.formatMoney(pnl.expenses, lang)}"))
            rawRows.add(listOf<Double?>(null, pnl.grossProfit))
            rawRows.add(listOf<Double?>(null, pnl.otherIncome))
            rawRows.add(listOf<Double?>(null, -pnl.expenses))
            pnl.expensesByCategory.forEach { (cat, amt) ->
                rows.add(listOf("  • ${ShopLocale.displayCategory(cat, lang)}", "-${ShopLocale.formatMoney(amt, lang)}"))
                rawRows.add(listOf<Double?>(null, -amt))
            }
            rows.add(listOf(ShopLocale.get("pnl_net_profit", lang), ShopLocale.formatMoney(pnl.netProfit, lang)))
            rawRows.add(listOf<Double?>(null, pnl.netProfit))
        }

        val totalAmount = if (isFatherMode) pnl.grossProfit else pnl.netProfit
        val totalLabel = if (isFatherMode) ShopLocale.get("col_profit", lang).uppercase() else ShopLocale.get("pnl_net_profit", lang).uppercase()

        val grossMarginPct = if (pnl.netSales > 0.0) (pnl.grossProfit / pnl.netSales) * 100.0 else 0.0
        val netMarginPct = if (pnl.netSales > 0.0) (pnl.netProfit / pnl.netSales) * 100.0 else 0.0
        val pnlSummary = if (isFatherMode) {
            listOf(
                ReportSummaryMetric(ShopLocale.get("pnl_net_sales", lang), ShopLocale.formatMoney(pnl.netSales, lang)),
                ReportSummaryMetric(ShopLocale.get("col_profit", lang), ShopLocale.formatMoney(pnl.grossProfit, lang))
            )
        } else {
            listOf(
                ReportSummaryMetric(ShopLocale.get("pnl_net_sales", lang), ShopLocale.formatMoney(pnl.netSales, lang)),
                ReportSummaryMetric(
                    ShopLocale.get("pnl_gross_profit", lang),
                    ShopLocale.formatMoney(pnl.grossProfit, lang),
                    String.format(java.util.Locale.US, "%.1f%%", grossMarginPct)
                ),
                ReportSummaryMetric(ShopLocale.get("pnl_operating_expenses", lang), ShopLocale.formatMoney(pnl.expenses, lang)),
                ReportSummaryMetric(
                    ShopLocale.get("pnl_net_profit", lang),
                    ShopLocale.formatMoney(pnl.netProfit, lang),
                    String.format(java.util.Locale.US, "%.1f%%", netMarginPct)
                )
            )
        }

        return ReportTableData(
            title = if (isFatherMode) ShopLocale.get("nav_business", lang) else ShopLocale.get("rep_pnl_title", lang),
            storeName = store?.name ?: ShopLocale.get("receipt_header_store_default", lang),
            storePhone = store?.phone ?: "",
            storeAddress = store?.address ?: "",
            periodLabel = periodLabel,
            generatedAt = nowFormatted(lang),
            headers = listOf(ShopLocale.get("col_line_item", lang), ShopLocale.get("col_amount", lang)),
            rows = rows,
            numericColumns = setOf(1),
            rawNumbers = rawRows,
            totals = listOf(totalLabel, ShopLocale.formatMoney(totalAmount, lang)),
            rawTotals = listOf(null, totalAmount),
            notes = if (effectiveStockBased) ShopLocale.get("pnl_note_stock", lang) else ShopLocale.get("pnl_note_purchase", lang),
            summaryMetrics = pnlSummary
        )
    }

    fun buildExpenseReportTable(
        expenses: List<Expense>,
        store: Store?,
        periodLabel: String,
        lang: AppLanguage = AppLanguage.EN
    ): ReportTableData {
        val totalExpenses = expenses.sumOf { it.amount }
        val rows = expenses.map { exp ->
            listOf(
                ShopLocale.formatDateLongFullMonth(exp.date, lang),
                ShopLocale.displayCategory(exp.category, lang),
                ShopLocale.displayAccountName(exp.accountName, exp.accountId, lang),
                exp.note,
                ShopLocale.formatMoneyFixed2(exp.amount, lang)
            )
        }
        val rawNumbers = expenses.map { exp ->
            listOf<Double?>(null, null, null, null, exp.amount)
        }
        val totalFormatted = ShopLocale.formatMoneyFixed2(totalExpenses, lang)
        val totals = listOf(
            ShopLocale.get("total_expenses_row", lang),
            "",
            "",
            ShopLocale.format("entries_count", lang, expenses.size),
            totalFormatted
        )
        val rawTotals = listOf<Double?>(null, null, null, null, totalExpenses)
        val byCat = expenses.groupBy { ShopLocale.displayCategory(it.category, lang) }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
        val topCat = byCat.firstOrNull()
        val avgExp = if (expenses.isNotEmpty()) totalExpenses / expenses.size else 0.0
        val expenseSummary = buildList {
            add(
                ReportSummaryMetric(
                    label = if (lang == AppLanguage.BN) "মোট খরচ" else "Total Expenses",
                    value = totalFormatted,
                    detail = ShopLocale.format("entries_count", lang, expenses.size)
                )
            )
            if (topCat != null) {
                val pct = if (totalExpenses > 0.0) (topCat.second / totalExpenses) * 100.0 else 0.0
                add(
                    ReportSummaryMetric(
                        label = if (lang == AppLanguage.BN) "সর্বোচ্চ খাত" else "Top Category",
                        value = topCat.first,
                        detail = "${ShopLocale.formatMoneyFixed2(topCat.second, lang)} (${String.format(java.util.Locale.US, "%.1f%%", pct)})"
                    )
                )
            }
            add(
                ReportSummaryMetric(
                    label = if (lang == AppLanguage.BN) "গড় খরচ" else "Average Entry",
                    value = ShopLocale.formatMoneyFixed2(avgExp, lang)
                )
            )
        }
        val catBreakdownNote = if (byCat.isNotEmpty()) {
            val topText = byCat.take(4).joinToString(" • ") { "${it.first}: ${ShopLocale.formatMoneyFixed2(it.second, lang)}" }
            "${ShopLocale.get("total_expenses_row", lang)}: $totalFormatted | $topText"
        } else {
            "${ShopLocale.get("total_expenses_row", lang)}: $totalFormatted"
        }

        return ReportTableData(
            title = ShopLocale.get("rep_expense_title", lang),
            storeName = store?.name ?: ShopLocale.get("receipt_header_store_default", lang),
            storePhone = store?.phone ?: "",
            storeAddress = store?.address ?: "",
            periodLabel = periodLabel,
            generatedAt = nowFormatted(lang),
            headers = listOf(
                ShopLocale.get("col_date", lang),
                ShopLocale.get("col_category", lang),
                ShopLocale.get("col_account", lang),
                ShopLocale.get("col_note", lang),
                ShopLocale.get("col_amount", lang)
            ),
            rows = rows,
            numericColumns = setOf(4),
            rawNumbers = rawNumbers,
            totals = totals,
            rawTotals = rawTotals,
            notes = catBreakdownNote,
            summaryMetrics = expenseSummary
        )
    }

    fun buildOtherIncomeReportTable(
        incomes: List<OtherIncome>,
        store: Store?,
        periodLabel: String,
        lang: AppLanguage = AppLanguage.EN
    ): ReportTableData {
        val totalIncome = incomes.sumOf { it.amount }
        val rows = incomes.map { inc ->
            listOf(
                ShopLocale.formatDateLongFullMonth(inc.date, lang),
                ShopLocale.displayCategory(inc.category, lang),
                ShopLocale.displayAccountName(inc.accountName, inc.accountId, lang),
                inc.note,
                ShopLocale.formatMoneyFixed2(inc.amount, lang)
            )
        }
        val rawNumbers = incomes.map { inc ->
            listOf<Double?>(null, null, null, null, inc.amount)
        }
        val totalFormatted = "+${ShopLocale.formatMoneyFixed2(totalIncome, lang)}"
        val totals = listOf(
            ShopLocale.get("total_other_income_row", lang),
            "",
            "",
            ShopLocale.format("entries_count", lang, incomes.size),
            totalFormatted
        )
        val rawTotals = listOf<Double?>(null, null, null, null, totalIncome)
        val byCat = incomes.groupBy { ShopLocale.displayCategory(it.category, lang) }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
        val topCat = byCat.firstOrNull()
        val avgInc = if (incomes.isNotEmpty()) totalIncome / incomes.size else 0.0
        val incomeSummary = buildList {
            add(
                ReportSummaryMetric(
                    label = if (lang == AppLanguage.BN) "মোট অন্যান্য আয়" else "Total Other Income",
                    value = ShopLocale.formatMoneyFixed2(totalIncome, lang),
                    detail = ShopLocale.format("entries_count", lang, incomes.size)
                )
            )
            if (topCat != null) {
                val pct = if (totalIncome > 0.0) (topCat.second / totalIncome) * 100.0 else 0.0
                add(
                    ReportSummaryMetric(
                        label = if (lang == AppLanguage.BN) "প্রধান উৎস" else "Top Source",
                        value = topCat.first,
                        detail = "${ShopLocale.formatMoneyFixed2(topCat.second, lang)} (${String.format(java.util.Locale.US, "%.1f%%", pct)})"
                    )
                )
            }
            add(
                ReportSummaryMetric(
                    label = if (lang == AppLanguage.BN) "গড় আয়" else "Average Entry",
                    value = ShopLocale.formatMoneyFixed2(avgInc, lang)
                )
            )
        }
        val catBreakdownNote = if (byCat.isNotEmpty()) {
            val topText = byCat.take(4).joinToString(" • ") { "${it.first}: ${ShopLocale.formatMoneyFixed2(it.second, lang)}" }
            "${ShopLocale.get("total_other_income_row", lang)}: ${ShopLocale.formatMoneyFixed2(totalIncome, lang)} | $topText"
        } else {
            "${ShopLocale.get("total_other_income_row", lang)}: ${ShopLocale.formatMoneyFixed2(totalIncome, lang)}"
        }

        return ReportTableData(
            title = ShopLocale.get("rep_other_income_title", lang),
            storeName = store?.name ?: ShopLocale.get("receipt_header_store_default", lang),
            storePhone = store?.phone ?: "",
            storeAddress = store?.address ?: "",
            periodLabel = periodLabel,
            generatedAt = nowFormatted(lang),
            headers = listOf(
                ShopLocale.get("col_date", lang),
                ShopLocale.get("col_category", lang),
                ShopLocale.get("col_account", lang),
                ShopLocale.get("col_note", lang),
                ShopLocale.get("col_amount", lang)
            ),
            rows = rows,
            numericColumns = setOf(4),
            rawNumbers = rawNumbers,
            totals = totals,
            rawTotals = rawTotals,
            notes = catBreakdownNote,
            summaryMetrics = incomeSummary
        )
    }

    fun buildInventoryReportTable(
        products: List<Product>,
        store: Store?,
        canSeeProfit: Boolean,
        lang: AppLanguage = AppLanguage.EN
    ): ReportTableData {
        val now = nowFormatted(lang)
        val totalItemsCount = products.sumOf { it.stock }
        val totalCostValue = products.sumOf { it.stock * it.buyPrice }
        val totalSaleValue = products.sumOf { it.stock * it.sellPrice }

        val rows = products.map { p ->
            val stockText = "${ShopLocale.formatNumber(p.stock, lang)} ${ShopLocale.displayUnit(p.unit, lang)}"
            if (canSeeProfit) {
                listOf(
                    p.name,
                    stockText,
                    ShopLocale.formatMoney(p.buyPrice, lang),
                    ShopLocale.formatMoney(p.sellPrice, lang),
                    ShopLocale.formatMoney(p.stock * p.buyPrice, lang)
                )
            } else {
                listOf(
                    p.name,
                    stockText,
                    ShopLocale.formatMoney(p.sellPrice, lang),
                    ShopLocale.formatMoney(p.stock * p.sellPrice, lang)
                )
            }
        }
        val rawRows = products.map { p ->
            if (canSeeProfit) {
                listOf(null, p.stock.toDouble(), p.buyPrice, p.sellPrice, p.stock * p.buyPrice)
            } else {
                listOf(null, p.stock.toDouble(), p.sellPrice, p.stock * p.sellPrice)
            }
        }
        val totalProductsLabel = ShopLocale.format("total_products_row", lang, products.size)
        val totalItemsLabel = ShopLocale.format("items_count_label", lang, totalItemsCount)
        val totals = if (canSeeProfit) {
            listOf(
                totalProductsLabel,
                totalItemsLabel,
                "",
                "",
                ShopLocale.formatMoney(totalCostValue, lang)
            )
        } else {
            listOf(
                totalProductsLabel,
                totalItemsLabel,
                "",
                ShopLocale.formatMoney(totalSaleValue, lang)
            )
        }
        val rawTotals = if (canSeeProfit) {
            listOf(null, totalItemsCount.toDouble(), null, null, totalCostValue)
        } else {
            listOf(null, totalItemsCount.toDouble(), null, totalSaleValue)
        }
        val headers = if (canSeeProfit) {
            listOf(
                ShopLocale.get("col_product", lang),
                ShopLocale.get("col_stock", lang),
                ShopLocale.get("col_buy_price", lang),
                ShopLocale.get("col_sell_price", lang),
                ShopLocale.get("col_cost_value", lang)
            )
        } else {
            listOf(
                ShopLocale.get("col_product", lang),
                ShopLocale.get("col_stock", lang),
                ShopLocale.get("col_sell_price", lang),
                ShopLocale.get("col_sale_value", lang)
            )
        }
        val invSummary = buildList {
            add(
                ReportSummaryMetric(
                    label = if (lang == AppLanguage.BN) "মোট পণ্য" else "Total Products",
                    value = ShopLocale.formatNumber(products.size, lang),
                    detail = totalItemsLabel
                )
            )
            if (canSeeProfit) {
                add(
                    ReportSummaryMetric(
                        label = if (lang == AppLanguage.BN) "ক্রয় মূল্য (মজুদ)" else "Total Cost Value",
                        value = ShopLocale.formatMoney(totalCostValue, lang)
                    )
                )
            }
            add(
                ReportSummaryMetric(
                    label = if (lang == AppLanguage.BN) "বিক্রয় মূল্য (সম্ভাব্য)" else "Total Sale Value",
                    value = ShopLocale.formatMoney(totalSaleValue, lang)
                )
            )
        }
        return ReportTableData(
            title = ShopLocale.get("rep_inventory_title", lang),
            storeName = store?.name ?: ShopLocale.get("receipt_header_store_default", lang),
            storePhone = store?.phone ?: "",
            storeAddress = store?.address ?: "",
            periodLabel = ShopLocale.format("as_of_date", lang, now),
            generatedAt = now,
            headers = headers,
            rows = rows,
            numericColumns = if (canSeeProfit) setOf(1, 2, 3, 4) else setOf(1, 2, 3),
            rawNumbers = rawRows,
            totals = totals,
            rawTotals = rawTotals,
            notes = if (canSeeProfit) {
                if (lang == AppLanguage.BN) {
                    "সম্ভাব্য বিক্রয় মূল্য: ${ShopLocale.formatMoney(totalSaleValue, lang)} • সম্ভাব্য লাভ: ${ShopLocale.formatMoney(totalSaleValue - totalCostValue, lang)}"
                } else {
                    "Potential Retail Value: ${ShopLocale.formatMoney(totalSaleValue, lang)} • Potential Gross Profit: ${ShopLocale.formatMoney(totalSaleValue - totalCostValue, lang)}"
                }
            } else null,
            summaryMetrics = invSummary
        )
    }

    fun buildCustomerStatementReportTable(
        rep: CustomerStatementReport,
        store: Store?,
        periodLabel: String,
        lang: AppLanguage = AppLanguage.EN
    ): ReportTableData {
        val rows = rep.entries.map { e ->
            val dStr = ShopLocale.formatDateShort(e.timestamp, lang)
            val isBakiSale = e.type == LedgerEntryType.BAKI_SALE
            val typeStr = if (isBakiSale) ShopLocale.get("entry_baki_sale", lang) else ShopLocale.get("entry_payment_in", lang)
            val signedAmt = if (isBakiSale) "+${ShopLocale.formatMoney(e.amount, lang)}" else "-${ShopLocale.formatMoney(e.amount, lang)}"
            listOf(
                dStr,
                typeStr,
                e.referenceNo,
                signedAmt,
                ShopLocale.formatMoney(e.runningBalance, lang)
            )
        }
        val rawRows = rep.entries.map { e ->
            val isBakiSale = e.type == LedgerEntryType.BAKI_SALE
            listOf<Double?>(null, null, null, if (isBakiSale) e.amount else -e.amount, e.runningBalance)
        }
        val totals = listOf(
            ShopLocale.get("closing_balance_row", lang),
            "",
            "",
            ShopLocale.format(
                "cust_stmt_totals_note",
                lang,
                ShopLocale.formatMoney(rep.totalBakiSales, lang),
                ShopLocale.formatMoney(rep.totalPayments, lang)
            ),
            ShopLocale.formatMoney(rep.closingBalance, lang)
        )
        val rawTotals = listOf<Double?>(null, null, null, null, rep.closingBalance)
        val custSummary = listOf(
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "পূর্বের জের" else "Opening Balance",
                value = ShopLocale.formatMoney(rep.openingBalance, lang)
            ),
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "মোট বাকি বিক্রয়" else "Baki Sales",
                value = ShopLocale.formatMoney(rep.totalBakiSales, lang)
            ),
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "মোট আদায়" else "Payments Received",
                value = ShopLocale.formatMoney(rep.totalPayments, lang)
            ),
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "সমাপনী বকেয়া" else "Closing Balance",
                value = ShopLocale.formatMoney(rep.closingBalance, lang)
            )
        )
        return ReportTableData(
            title = ShopLocale.format("rep_customer_stmt_title", lang, rep.customer.name),
            storeName = store?.name ?: ShopLocale.get("receipt_header_store_default", lang),
            storePhone = store?.phone ?: "",
            storeAddress = store?.address ?: "",
            periodLabel = periodLabel,
            generatedAt = nowFormatted(lang),
            headers = listOf(
                ShopLocale.get("col_date", lang),
                ShopLocale.get("col_type", lang),
                ShopLocale.get("col_ref", lang),
                ShopLocale.get("col_amount", lang),
                ShopLocale.get("col_balance", lang)
            ),
            rows = rows,
            numericColumns = setOf(3, 4),
            rawNumbers = rawRows,
            totals = totals,
            rawTotals = rawTotals,
            notes = "${ShopLocale.format("opening_balance_note", lang, ShopLocale.formatMoney(rep.openingBalance, lang))} • ${ShopLocale.get("closing_balance_row", lang)}: ${ShopLocale.formatMoney(rep.closingBalance, lang)}",
            summaryMetrics = custSummary
        )
    }

    fun buildSupplierStatementReportTable(
        rep: SupplierStatementReport,
        store: Store?,
        periodLabel: String,
        lang: AppLanguage = AppLanguage.EN
    ): ReportTableData {
        val rows = rep.entries.map { e ->
            val dStr = ShopLocale.formatDateShort(e.timestamp, lang)
            listOf(
                dStr,
                ShopLocale.displayEntryType(e.type, lang),
                e.referenceNo,
                ShopLocale.formatMoney(e.amount, lang),
                ShopLocale.formatMoney(e.runningBalance, lang)
            )
        }
        val rawRows = rep.entries.map { e ->
            listOf<Double?>(null, null, null, e.amount, e.runningBalance)
        }
        val totals = listOf(
            ShopLocale.get("closing_balance_row", lang),
            "",
            "",
            ShopLocale.format(
                "sup_stmt_totals_note",
                lang,
                ShopLocale.formatMoney(rep.totalPurchases, lang),
                ShopLocale.formatMoney(rep.totalReturns, lang)
            ),
            ShopLocale.formatMoney(rep.closingBalance, lang)
        )
        val rawTotals = listOf<Double?>(null, null, null, null, rep.closingBalance)
        val supSummary = listOf(
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "পূর্বের জের" else "Opening Balance",
                value = ShopLocale.formatMoney(rep.openingBalance, lang)
            ),
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "মোট ক্রয়" else "Total Purchases",
                value = ShopLocale.formatMoney(rep.totalPurchases, lang)
            ),
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "ক্রয় ফেরত" else "Total Returns",
                value = ShopLocale.formatMoney(rep.totalReturns, lang)
            ),
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "সমাপনী বকেয়া" else "Closing Balance",
                value = ShopLocale.formatMoney(rep.closingBalance, lang)
            )
        )
        return ReportTableData(
            title = ShopLocale.format("rep_supplier_stmt_title", lang, rep.supplier.name),
            storeName = store?.name ?: ShopLocale.get("receipt_header_store_default", lang),
            storePhone = store?.phone ?: "",
            storeAddress = store?.address ?: "",
            periodLabel = periodLabel,
            generatedAt = nowFormatted(lang),
            headers = listOf(
                ShopLocale.get("col_date", lang),
                ShopLocale.get("col_type", lang),
                ShopLocale.get("col_ref", lang),
                ShopLocale.get("col_amount", lang),
                ShopLocale.get("col_balance", lang)
            ),
            rows = rows,
            numericColumns = setOf(3, 4),
            rawNumbers = rawRows,
            totals = totals,
            rawTotals = rawTotals,
            notes = "${ShopLocale.format("opening_balance_note", lang, ShopLocale.formatMoney(rep.openingBalance, lang))} • ${ShopLocale.get("closing_balance_row", lang)}: ${ShopLocale.formatMoney(rep.closingBalance, lang)}",
            summaryMetrics = supSummary
        )
    }

    fun buildAccountStatementReportTable(
        rep: AccountStatementReport,
        store: Store?,
        periodLabel: String,
        lang: AppLanguage = AppLanguage.EN
    ): ReportTableData {
        val rows = rep.entries.map { entry ->
            listOf(
                ShopLocale.formatDateShort(entry.timestamp, lang),
                ShopLocale.displayEntryType(entry.type, lang),
                entry.referenceNo,
                if (entry.moneyIn > 0.0) ShopLocale.formatMoneyFixed2(entry.moneyIn, lang) else "—",
                if (entry.moneyOut > 0.0) ShopLocale.formatMoneyFixed2(entry.moneyOut, lang) else "—",
                ShopLocale.formatMoneyFixed2(entry.runningBalance, lang)
            )
        }
        val rawRows = rep.entries.map { entry ->
            listOf(
                null,
                null,
                null,
                if (entry.moneyIn > 0.0) entry.moneyIn else null,
                if (entry.moneyOut > 0.0) entry.moneyOut else null,
                entry.runningBalance
            )
        }
        val totals = listOf(
            ShopLocale.get("closing_balance_row", lang),
            "",
            "",
            ShopLocale.formatMoneyFixed2(rep.totalMoneyIn, lang),
            ShopLocale.formatMoneyFixed2(rep.totalMoneyOut, lang),
            ShopLocale.formatMoneyFixed2(rep.closingBalance, lang)
        )
        val rawTotals = listOf<Double?>(null, null, null, rep.totalMoneyIn, rep.totalMoneyOut, rep.closingBalance)
        val accDisplay = ShopLocale.displayAccountName(rep.account.name, rep.account.id, lang)
        val acctSummary = listOf(
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "পূর্বের জের" else "Opening Balance",
                value = ShopLocale.formatMoneyFixed2(rep.openingBalance, lang)
            ),
            ReportSummaryMetric(
                label = ShopLocale.get("col_money_in", lang),
                value = ShopLocale.formatMoneyFixed2(rep.totalMoneyIn, lang)
            ),
            ReportSummaryMetric(
                label = ShopLocale.get("col_money_out", lang),
                value = ShopLocale.formatMoneyFixed2(rep.totalMoneyOut, lang)
            ),
            ReportSummaryMetric(
                label = ShopLocale.get("closing_balance_row", lang),
                value = ShopLocale.formatMoneyFixed2(rep.closingBalance, lang)
            )
        )
        return ReportTableData(
            title = ShopLocale.format("rep_account_stmt_title", lang, accDisplay),
            storeName = store?.name ?: ShopLocale.get("receipt_header_store_default", lang),
            storePhone = store?.phone ?: "",
            storeAddress = store?.address ?: "",
            periodLabel = periodLabel,
            generatedAt = nowFormatted(lang),
            headers = listOf(
                ShopLocale.get("col_date", lang),
                ShopLocale.get("col_type", lang),
                ShopLocale.get("col_ref", lang),
                ShopLocale.get("col_money_in", lang),
                ShopLocale.get("col_money_out", lang),
                ShopLocale.get("col_running_balance", lang)
            ),
            rows = rows,
            numericColumns = setOf(3, 4, 5),
            rawNumbers = rawRows,
            totals = totals,
            rawTotals = rawTotals,
            notes = "${ShopLocale.format("opening_balance_note", lang, ShopLocale.formatMoneyFixed2(rep.openingBalance, lang))} • ${ShopLocale.get("closing_balance_row", lang)}: ${ShopLocale.formatMoneyFixed2(rep.closingBalance, lang)}",
            summaryMetrics = acctSummary
        )
    }

    fun buildStaffReportTable(
        staffSummary: StaffReportSummary,
        store: Store?,
        periodLabel: String,
        canSeeProfit: Boolean,
        lang: AppLanguage = AppLanguage.EN
    ): ReportTableData {
        val headers = if (canSeeProfit) {
            listOf(
                ShopLocale.get("col_staff_member", lang),
                ShopLocale.get("col_role", lang),
                ShopLocale.get("col_orders", lang),
                ShopLocale.get("col_qty_sold", lang),
                ShopLocale.get("col_sales_total", lang),
                ShopLocale.get("col_profit", lang),
                ShopLocale.get("col_payments_in", lang)
            )
        } else {
            listOf(
                ShopLocale.get("col_staff_member", lang),
                ShopLocale.get("col_role", lang),
                ShopLocale.get("col_orders", lang),
                ShopLocale.get("col_qty_sold", lang),
                ShopLocale.get("col_sales_total", lang),
                ShopLocale.get("col_payments_in", lang)
            )
        }
        val rows = staffSummary.rows.map { row ->
            val roleDisplay = ShopLocale.displayStaffRole(row.roleLabel, lang)
            val displayMember = if (row.memberName == "Owner") ShopLocale.get("role_owner", lang) else row.memberName
            if (canSeeProfit) {
                listOf(
                    displayMember,
                    roleDisplay,
                    ShopLocale.formatNumber(row.salesCount, lang),
                    ShopLocale.formatNumber(row.itemsSold, lang),
                    ShopLocale.formatMoney(row.totalSales, lang),
                    ShopLocale.formatMoney(row.profit, lang),
                    ShopLocale.formatMoney(row.paymentsReceived, lang)
                )
            } else {
                listOf(
                    displayMember,
                    roleDisplay,
                    ShopLocale.formatNumber(row.salesCount, lang),
                    ShopLocale.formatNumber(row.itemsSold, lang),
                    ShopLocale.formatMoney(row.totalSales, lang),
                    ShopLocale.formatMoney(row.paymentsReceived, lang)
                )
            }
        }
        val rawRows: List<List<Double?>> = staffSummary.rows.map { row ->
            if (canSeeProfit) {
                listOf(null, null, row.salesCount.toDouble(), row.itemsSold.toDouble(), row.totalSales, row.profit, row.paymentsReceived)
            } else {
                listOf(null, null, row.salesCount.toDouble(), row.itemsSold.toDouble(), row.totalSales, row.paymentsReceived)
            }
        }
        val totalStaffLabel = ShopLocale.format("total_staff_row", lang, staffSummary.rows.size)
        val totals = if (canSeeProfit) {
            listOf(
                totalStaffLabel,
                "",
                ShopLocale.formatNumber(staffSummary.totalSalesCount, lang),
                ShopLocale.formatNumber(staffSummary.totalItemsSold, lang),
                ShopLocale.formatMoney(staffSummary.totalSalesAmount, lang),
                ShopLocale.formatMoney(staffSummary.totalProfit, lang),
                ShopLocale.formatMoney(staffSummary.totalPaymentsReceived, lang)
            )
        } else {
            listOf(
                totalStaffLabel,
                "",
                ShopLocale.formatNumber(staffSummary.totalSalesCount, lang),
                ShopLocale.formatNumber(staffSummary.totalItemsSold, lang),
                ShopLocale.formatMoney(staffSummary.totalSalesAmount, lang),
                ShopLocale.formatMoney(staffSummary.totalPaymentsReceived, lang)
            )
        }
        val rawTotals = if (canSeeProfit) {
            listOf(null, null, staffSummary.totalSalesCount.toDouble(), staffSummary.totalItemsSold.toDouble(), staffSummary.totalSalesAmount, staffSummary.totalProfit, staffSummary.totalPaymentsReceived)
        } else {
            listOf(null, null, staffSummary.totalSalesCount.toDouble(), staffSummary.totalItemsSold.toDouble(), staffSummary.totalSalesAmount, staffSummary.totalPaymentsReceived)
        }
        val staffSummaryMetrics = buildList {
            add(
                ReportSummaryMetric(
                    label = ShopLocale.get("sales_recorded_lbl", lang),
                    value = ShopLocale.formatMoney(staffSummary.totalSalesAmount, lang),
                    detail = ShopLocale.format(
                        "staff_orders_items_val",
                        lang,
                        ShopLocale.formatNumber(staffSummary.totalSalesCount, lang),
                        ShopLocale.formatNumber(staffSummary.totalItemsSold, lang)
                    )
                )
            )
            if (canSeeProfit) {
                add(
                    ReportSummaryMetric(
                        label = ShopLocale.get("profit_generated_lbl", lang),
                        value = ShopLocale.formatMoney(staffSummary.totalProfit, lang)
                    )
                )
            }
            add(
                ReportSummaryMetric(
                    label = ShopLocale.get("cust_payments_received_lbl", lang),
                    value = ShopLocale.formatMoney(staffSummary.totalPaymentsReceived, lang)
                )
            )
            add(
                ReportSummaryMetric(
                    label = if (lang == AppLanguage.BN) "মোট স্টাফ" else "Team Members",
                    value = ShopLocale.formatNumber(staffSummary.rows.size, lang)
                )
            )
        }
        return ReportTableData(
            title = ShopLocale.get("rep_staff_title", lang),
            storeName = store?.name ?: ShopLocale.get("receipt_header_store_default", lang),
            storePhone = store?.phone ?: "",
            storeAddress = store?.address ?: "",
            periodLabel = periodLabel,
            generatedAt = nowFormatted(lang),
            headers = headers,
            rows = rows,
            numericColumns = if (canSeeProfit) setOf(2, 3, 4, 5, 6) else setOf(2, 3, 4, 5),
            rawNumbers = rawRows,
            totals = totals,
            rawTotals = rawTotals,
            notes = if (lang == AppLanguage.BN) {
                "মোট ${ShopLocale.formatNumber(staffSummary.rows.size, lang)} জন স্টাফ • মোট অর্ডার: ${ShopLocale.formatNumber(staffSummary.totalSalesCount, lang)} • ক্রেতার বাকি আদায়: ${ShopLocale.formatMoney(staffSummary.totalPaymentsReceived, lang)}"
            } else {
                "Active Team: ${staffSummary.rows.size} staff • Total Orders: ${staffSummary.totalSalesCount} • Customer Payments Collected: ${ShopLocale.formatMoney(staffSummary.totalPaymentsReceived, lang)}"
            },
            summaryMetrics = staffSummaryMetrics
        )
    }
}
