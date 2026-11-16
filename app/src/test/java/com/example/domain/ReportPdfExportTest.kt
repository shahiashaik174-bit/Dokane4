package com.example.domain

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.BusinessCardDraft
import com.example.ui.viewmodel.HistoryPeriod
import com.example.util.AppLanguage
import com.example.util.BusinessCardRenderer
import com.example.util.ReportExportUtil
import com.example.util.ReportSummaryMetric
import com.example.util.ReportTableData
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReportPdfExportTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        org.robolectric.shadows.ShadowLog.stream = System.out
        runCatching {
            val cacheField = androidx.core.content.FileProvider::class.java.getDeclaredField("sCache")
            cacheField.isAccessible = true
            (cacheField.get(null) as? MutableMap<*, *>)?.clear()
        }
        context = ApplicationProvider.getApplicationContext()
        File(context.cacheDir, "reports").deleteRecursively()
        context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)?.deleteRecursively()
    }

    @Test
    fun detailed_report_fits_single_page_when_compact_and_launches_pdf_chooser_on_download() {
        val rows = (1..14).map { idx ->
            listOf(
                "INV-2026-%03d".format(idx),
                "16 Nov 2026",
                if (idx % 2 == 0) "রহিম এন্টারপ্রাইজ ও সন্স (নগদ)" else "Karim General Store",
                "৳${1250 * idx}",
                "৳${50 * idx}",
                "৳${280 * idx}",
                if (idx % 3 == 0) "Baki" else "Paid"
            )
        }
        val data = ReportTableData(
            title = "Detailed Sales Report • বিস্তারিত বিক্রয় রিপোর্ট",
            subtitle = "Complete invoice breakdown with profit and payment collection",
            storeName = "রহিম সুপার শপ · Rahim Super Shop",
            storePhone = "+880 1711-223344",
            storeAddress = "Shop 14, New Market Road, Dhaka",
            periodLabel = "Nov 2026",
            generatedAt = "16 Nov 2026, 07:20 AM",
            headers = listOf("Invoice", "Date", "Customer", "Total", "Discount", "Profit", "Status"),
            rows = rows,
            numericColumns = setOf(3, 4, 5),
            totals = listOf("14 Orders", "", "", "৳131,250", "৳5,250", "৳29,400", ""),
            summaryMetrics = listOf(
                ReportSummaryMetric("Total Sales", "৳131,250", "14 Invoices"),
                ReportSummaryMetric("Cash Collected", "৳98,500", "75.0% collected"),
                ReportSummaryMetric("Customer Baki", "৳32,750", "4 due orders"),
                ReportSummaryMetric("Net Profit", "৳29,400", "Margin 22.4%")
            ),
            notes = "All paid and credit (baki) invoices for November 2026 are included with net profit after discounts."
        )

        assertEquals(1, ReportExportUtil.countPlannedPages(context, data, AppLanguage.EN))
        val pdfFile = ReportExportUtil.generatePdfReport(context, data, "DetailedSalesTest", AppLanguage.EN)
        assertTrue("Report PDF file should exist", pdfFile.exists())
        assertTrue("Report PDF file should be non-empty", pdfFile.length() > 1000L)

        val bytes = pdfFile.readBytes()
        val header = String(bytes.take(8).toByteArray(), Charsets.US_ASCII)
        assertTrue("Generated report must be valid PDF, header=$header", header.startsWith("%PDF-"))

        val app = context as Application
        ReportExportUtil.downloadReportFile(context, pdfFile, AppLanguage.EN)
        val launchedIntent = shadowOf(app).nextStartedActivity
        assertNotNull("Download PDF must pop up chooser intent to open the PDF", launchedIntent)
        assertEquals(Intent.ACTION_CHOOSER, launchedIntent.action)
    }

    @Test
    fun large_report_splits_across_multiple_pages_cleanly_without_crashing() {
        val rows = (1..75).map { idx ->
            listOf(
                "$idx",
                "Product Item #$idx — প্রোডাক্ট নং $idx with extra long descriptive label",
                "${idx * 3} pcs",
                "৳${120 + idx}",
                "৳${(idx * 3) * (120 + idx)}"
            )
        }
        val data = ReportTableData(
            title = "Inventory Valuation Report",
            storeName = "Karim Traders",
            periodLabel = "As of 16 Nov 2026",
            generatedAt = "16 Nov 2026",
            headers = listOf("#", "Product", "Stock", "Rate", "Value"),
            rows = rows,
            numericColumns = setOf(0, 2, 3, 4),
            totals = listOf("Total", "75 Products", "8,550 pcs", "", "৳1,450,000"),
            summaryMetrics = listOf(
                ReportSummaryMetric("Total Products", "75"),
                ReportSummaryMetric("Total Units", "8,550 pcs"),
                ReportSummaryMetric("Cost Valuation", "৳1,450,000")
            ),
            notes = "Multi-page continuation check: every page repeats the header banner and column titles."
        )

        val plannedPages = ReportExportUtil.countPlannedPages(context, data, AppLanguage.EN)
        assertTrue("75-row report should span multiple pages, got $plannedPages", plannedPages >= 2)
        val pdfFile = ReportExportUtil.generatePdfReport(context, data, "LargeInventoryTest", AppLanguage.EN)
        assertTrue(pdfFile.exists() && pdfFile.length() > 2000L)
        val raw = pdfFile.readBytes()
        val header = String(raw.take(8).toByteArray(), Charsets.US_ASCII)
        assertTrue("Multi-page report must be valid PDF, header=$header", header.startsWith("%PDF-"))
        val content = String(raw, Charsets.ISO_8859_1)
        assertTrue("Multi-page PDF should contain /Count $plannedPages", content.contains("/Count $plannedPages"))
    }

    @Test
    fun notes_spill_onto_continuation_page_without_repeating_totals() {
        val rows = (1..26).map { idx ->
            listOf(
                "INV-$idx",
                "ক্রেতা $idx · Customer $idx",
                "৳${500 * idx}"
            )
        }
        val longNotes = (1..12).joinToString(" ") {
            "নোট #$it: বিস্তারিত বিক্রয় ও বাকি হিসাবের সারসংক্ষেপ এবং নগদ আদায়ের তথ্য এই রিপোর্টে অন্তর্ভুক্ত করা হয়েছে।"
        }
        val data = ReportTableData(
            title = "বিস্তারিত বিক্রয় ও নোট রিপোর্ট • Detailed Sales & Notes",
            storeName = "স্টোর ওএস · StoreOS",
            periodLabel = "নভেম্বর ২০২৬",
            generatedAt = "১৬ নভেম্বর ২০২৬",
            headers = listOf("চালান", "ক্রেতা", "মোট"),
            rows = rows,
            numericColumns = setOf(2),
            totals = listOf("মোট", "২৬টি অর্ডার", "৳১৭৫,৫০০"),
            summaryMetrics = listOf(
                ReportSummaryMetric("মোট বিক্রয়", "৳১৭৫,৫০০", "২৬টি চালান"),
                ReportSummaryMetric("নগদ আদায়", "৳১৫০,০০০")
            ),
            notes = longNotes
        )

        val plannedPages = ReportExportUtil.countPlannedPages(context, data, AppLanguage.BN)
        assertTrue("Long notes spill should create >= 2 pages, got $plannedPages", plannedPages >= 2)
        val pdfFile = ReportExportUtil.generatePdfReport(context, data, "NotesSpillTest", AppLanguage.BN)
        assertTrue(pdfFile.exists() && pdfFile.length() > 1500L)
    }

    @Test
    fun report_builders_populate_summary_metrics_and_notes_in_en_and_bn() {
        val store = com.example.data.model.Store(name = "Test Mart", phone = "01700000000", address = "Dhaka")
        val sales = listOf(
            com.example.data.model.Sale(invoiceNo = "INV-1", customerName = "Rahim", grandTotal = 1000.0, discount = 50.0, profit = 200.0, paymentType = "cash"),
            com.example.data.model.Sale(invoiceNo = "INV-2", customerName = "Karim", grandTotal = 500.0, discount = 0.0, profit = 100.0, paymentType = "baki")
        )
        val expenses = listOf(
            com.example.data.model.Expense(category = "Shop Rent", amount = 3000.0, note = "Monthly rent")
        )
        val staffSummary = StaffReportSummary(
            rows = listOf(
                StaffPerformanceRow(
                    memberId = "m1",
                    memberName = "Hasan",
                    roleLabel = "Cashier",
                    salesCount = 2L,
                    totalSales = 1500.0,
                    itemsSold = 5.0,
                    profit = 300.0,
                    paymentsReceived = 1000.0
                )
            ),
            totalSalesCount = 2L,
            totalSalesAmount = 1500.0,
            totalItemsSold = 5.0,
            totalProfit = 300.0,
            totalPaymentsReceived = 1000.0
        )
        val supReport = SupplierStatementReport(
            supplier = com.example.data.model.Supplier(name = "ACI Supply", mobile = "01811111111", balance = 1200.0),
            period = HistoryPeriod.entries.first(),
            startDate = 0L,
            endDate = System.currentTimeMillis(),
            openingBalance = 500.0,
            entries = emptyList(),
            totalPurchases = 2000.0,
            totalPayments = 1300.0,
            totalReturns = 0.0,
            closingBalance = 1200.0
        )

        listOf(AppLanguage.EN, AppLanguage.BN).forEach { lang ->
            val salesReport = com.example.util.ReportDataBuilders.buildSalesReportTable(sales, store, "Today", true, lang)
            assertTrue(salesReport.summaryMetrics.isNotEmpty())
            assertTrue(!salesReport.notes.isNullOrBlank())

            val expenseReport = com.example.util.ReportDataBuilders.buildExpenseReportTable(expenses, store, "Today", lang)
            assertTrue(expenseReport.summaryMetrics.isNotEmpty())
            assertTrue(!expenseReport.notes.isNullOrBlank())

            val supStatement = com.example.util.ReportDataBuilders.buildSupplierStatementReportTable(supReport, store, "All Time", lang)
            assertTrue(supStatement.summaryMetrics.isNotEmpty())
            assertTrue(!supStatement.notes.isNullOrBlank())

            val staffReport = com.example.util.ReportDataBuilders.buildStaffReportTable(staffSummary, store, "Today", true, lang)
            assertTrue(staffReport.summaryMetrics.isNotEmpty())
            assertTrue(!staffReport.notes.isNullOrBlank())
        }
    }

    @Test
    fun business_card_open_pdf_chooser_launches_chooser_intent() {
        val draft = BusinessCardDraft(
            shopName = "রহিম স্টোর",
            ownerName = "আবুল করিম",
            phone = "+880 1711-000000"
        )
        val export = BusinessCardRenderer.exportPdf(context, draft)
        assertTrue(export.file.exists() && export.file.length() > 500L)

        val app = context as Application
        val opened = BusinessCardRenderer.openPdfWithChooser(context, export.file, AppLanguage.BN)
        assertTrue(opened)
        val launchedIntent = shadowOf(app).nextStartedActivity
        assertNotNull("Business Card PDF download should launch chooser", launchedIntent)
        assertEquals(Intent.ACTION_CHOOSER, launchedIntent.action)
    }
}
