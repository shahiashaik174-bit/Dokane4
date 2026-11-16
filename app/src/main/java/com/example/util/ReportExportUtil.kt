package com.example.util

import android.app.Activity
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.provider.MediaStore
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

data class ReportSummaryMetric(
    val label: String,
    val value: String,
    val detail: String = ""
)

data class ReportTableData(
    val title: String,
    val storeName: String,
    val storePhone: String = "",
    val storeAddress: String = "",
    val periodLabel: String,
    val generatedAt: String,
    val headers: List<String>,
    val rows: List<List<String>>,
    val numericColumns: Set<Int> = emptySet(),
    val rawNumbers: List<List<Double?>> = emptyList(),
    val totals: List<String>? = null,
    val rawTotals: List<Double?> = emptyList(),
    val notes: String? = null,
    val subtitle: String? = null,
    val summaryMetrics: List<ReportSummaryMetric> = emptyList(),
    val sectionTitle: String? = null
)

object ReportExportUtil {

    private const val PAGE_WIDTH = 595 // Standard A4 points (72 dpi)
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_X = 30f
    private const val MARGIN_TOP = 26f
    private const val FOOTER_HEIGHT = 28f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN_X * 2f) // 535pt

    // Palette for polished, print-friendly high-contrast PDF output
    private val COLOR_HEADER_BG = Color.rgb(18, 38, 32)       // Deep forest slate
    private val COLOR_HEADER_ACCENT = Color.rgb(34, 197, 94)  // Emerald accent bar
    private val COLOR_TABLE_HEAD_BG = Color.rgb(28, 52, 44)
    private val COLOR_CONT_BANNER_BG = Color.rgb(241, 246, 243)
    private val COLOR_KPI_BG = Color.rgb(246, 250, 247)
    private val COLOR_KPI_BORDER = Color.rgb(203, 224, 211)
    private val COLOR_ROW_ALT = Color.rgb(248, 250, 252)
    private val COLOR_TOTAL_BG = Color.rgb(232, 244, 237)
    private val COLOR_NOTES_BG = Color.rgb(249, 250, 251)
    private val COLOR_BORDER = Color.rgb(208, 217, 224)
    private val COLOR_TEXT_PRIMARY = Color.rgb(17, 24, 39)
    private val COLOR_TEXT_SECONDARY = Color.rgb(75, 85, 99)
    private val COLOR_TEXT_MUTED = Color.rgb(107, 114, 128)
    private val COLOR_ACCENT_DARK = Color.rgb(16, 122, 68)

    private data class LayoutSpec(
        val bodyTextSp: Float,
        val headerTextSp: Float,
        val kpiValueSp: Float,
        val kpiLabelSp: Float,
        val cellPadX: Float,
        val cellPadY: Float,
        val sectionGap: Float,
        val kpiPadY: Float
    )

    private val COMFORT_SPEC = LayoutSpec(
        bodyTextSp = 8.8f,
        headerTextSp = 8.6f,
        kpiValueSp = 11.2f,
        kpiLabelSp = 7.4f,
        cellPadX = 5.5f,
        cellPadY = 5.5f,
        sectionGap = 10f,
        kpiPadY = 7.5f
    )

    private val COMPACT_SPEC = LayoutSpec(
        bodyTextSp = 7.9f,
        headerTextSp = 7.9f,
        kpiValueSp = 10.2f,
        kpiLabelSp = 7.0f,
        cellPadX = 4.5f,
        cellPadY = 3.5f,
        sectionGap = 7f,
        kpiPadY = 5.5f
    )

    private data class MeasuredRow(
        val cells: List<StaticLayout>,
        val height: Float
    )

    private data class KpiCardMeasured(
        val labelLayout: StaticLayout,
        val valueLayout: StaticLayout,
        val detailLayout: StaticLayout?,
        val height: Float
    )

    private data class PlannedPage(
        val isFirstPage: Boolean,
        val rowIndices: List<Int>,
        val includeTotals: Boolean,
        val includeNotes: Boolean,
        val showEmptyState: Boolean
    )

    private fun getReportsDir(context: Context): File {
        val dir = File(context.cacheDir, "reports")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun isBanglaChar(ch: Char): Boolean {
        val code = ch.code
        return code in 0x0980..0x09FF || code == 0x0964 || code == 0x0965
    }

    private fun splitScriptRuns(text: String): List<Pair<String, Boolean>> {
        if (text.isEmpty()) return emptyList()
        val runs = mutableListOf<Pair<String, Boolean>>()
        val current = StringBuilder()
        var currentIsBangla = isBanglaChar(text[0])

        for (ch in text) {
            val isBangla = if (
                ch.isWhitespace() ||
                ch == '\u200C' ||
                ch == '\u200D' ||
                ch in ".,:-•/|()[]!?@#&+'\"·৳%"
            ) {
                currentIsBangla
            } else {
                isBanglaChar(ch)
            }
            if (isBangla != currentIsBangla && current.isNotEmpty()) {
                runs.add(current.toString() to currentIsBangla)
                current.clear()
                currentIsBangla = isBangla
            }
            current.append(ch)
        }
        if (current.isNotEmpty()) {
            runs.add(current.toString() to currentIsBangla)
        }
        return runs
    }

    private class CustomTypefaceSpan(
        private val customTypeface: Typeface,
        private val isBold: Boolean
    ) : MetricAffectingSpan() {
        private fun apply(paint: Paint) {
            paint.typeface = if (isBold) {
                Typeface.create(customTypeface, Typeface.BOLD)
            } else {
                customTypeface
            }
            paint.isFakeBoldText = isBold && paint.typeface?.isBold != true
        }

        override fun updateDrawState(tp: TextPaint) = apply(tp)
        override fun updateMeasureState(tp: TextPaint) = apply(tp)
    }

    private fun buildStyledSpanned(
        text: String,
        banglaTf: Typeface,
        latinTf: Typeface,
        isBold: Boolean
    ): CharSequence {
        val runs = splitScriptRuns(text)
        if (runs.isEmpty()) return ""
        val ssb = SpannableStringBuilder()
        runs.forEach { (segment, isBangla) ->
            val start = ssb.length
            ssb.append(segment)
            ssb.setSpan(
                CustomTypefaceSpan(if (isBangla) banglaTf else latinTf, isBold),
                start,
                ssb.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return ssb
    }

    private fun createStaticLayout(
        text: String,
        width: Float,
        textSizeSp: Float,
        color: Int,
        align: Layout.Alignment,
        banglaTf: Typeface,
        latinTf: Typeface,
        isBold: Boolean = false
    ): StaticLayout {
        val safeText = text.ifEmpty { " " }
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG or Paint.LINEAR_TEXT_FLAG).apply {
            this.color = color
            this.textSize = textSizeSp
            this.typeface = if (isBold) Typeface.create(latinTf, Typeface.BOLD) else latinTf
        }
        val spanned = buildStyledSpanned(safeText, banglaTf, latinTf, isBold)
        return StaticLayout.Builder
            .obtain(spanned, 0, spanned.length, tp, width.toInt().coerceAtLeast(12))
            .setAlignment(align)
            .setLineSpacing(1.5f, 1.05f)
            .setIncludePad(false)
            .build()
    }

    private fun StaticLayout.drawAt(canvas: Canvas, x: Float, y: Float) {
        canvas.save()
        canvas.translate(x, y)
        draw(canvas)
        canvas.restore()
    }

    /**
     * Generates a detailed, easy-to-understand, print-ready A4 PDF.
     * - Automatically fits compact/medium reports cleanly onto 1 single page when readable.
     * - When content exceeds 1 single page, splits rows cleanly across continuation pages with
     *   repeated headers, consistent footers ("Page X of Y"), and zero broken/truncated rows.
     */
    fun generatePdfReport(
        context: Context,
        data: ReportTableData,
        fileName: String,
        lang: AppLanguage = AppLanguage.EN
    ): File {
        val file = File(getReportsDir(context), if (fileName.endsWith(".pdf")) fileName else "$fileName.pdf")

        val banglaTf = runCatching {
            ResourcesCompat.getFont(context, R.font.noto_sans_bengali)
        }.getOrNull() ?: Typeface.SANS_SERIF
        val latinTf = runCatching {
            ResourcesCompat.getFont(context, R.font.plus_jakarta_sans)
        }.getOrNull() ?: Typeface.SANS_SERIF

        val effectiveMetrics = resolveSummaryMetrics(data, lang)
        val colCount = data.headers.size.coerceAtLeast(1)
        val colWidths = computeColumnWidths(data, colCount, banglaTf, latinTf)

        // 1. Plan pagination: try COMFORT single-page -> COMPACT single-page -> COMFORT multi-page
        val comfortFirstPass = planReportLayout(data, effectiveMetrics, colWidths, COMFORT_SPEC, banglaTf, latinTf, lang)
        val chosenPlan = if (comfortFirstPass.pages.size == 1) {
            comfortFirstPass
        } else {
            val compactPass = planReportLayout(data, effectiveMetrics, colWidths, COMPACT_SPEC, banglaTf, latinTf, lang)
            if (compactPass.pages.size == 1) compactPass else comfortFirstPass
        }

        val nativeSucceeded = runCatching {
            val doc = PdfDocument()
            try {
                val totalPages = chosenPlan.pages.size
                chosenPlan.pages.forEachIndexed { idx, plannedPage ->
                    val pageNumber = idx + 1
                    val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    val page = doc.startPage(pageInfo)
                    renderPage(
                        canvas = page.canvas,
                        data = data,
                        plan = chosenPlan,
                        page = plannedPage,
                        pageNumber = pageNumber,
                        totalPages = totalPages,
                        colWidths = colWidths,
                        banglaTf = banglaTf,
                        latinTf = latinTf,
                        lang = lang
                    )
                    doc.finishPage(page)
                }
                FileOutputStream(file).use { out ->
                    doc.writeTo(out)
                }
            } finally {
                runCatching { doc.close() }
            }
            true
        }.getOrDefault(false)

        if (!nativeSucceeded || !file.exists() || file.length() == 0L) {
            writeFallbackReportPdf(
                file = file,
                data = data,
                plan = chosenPlan,
                colWidths = colWidths,
                banglaTf = banglaTf,
                latinTf = latinTf,
                lang = lang
            )
        }

        return file
    }

    internal fun countPlannedPages(
        context: Context,
        data: ReportTableData,
        lang: AppLanguage = AppLanguage.EN
    ): Int {
        val banglaTf = runCatching {
            ResourcesCompat.getFont(context, R.font.noto_sans_bengali)
        }.getOrNull() ?: Typeface.SANS_SERIF
        val latinTf = runCatching {
            ResourcesCompat.getFont(context, R.font.plus_jakarta_sans)
        }.getOrNull() ?: Typeface.SANS_SERIF
        val effectiveMetrics = resolveSummaryMetrics(data, lang)
        val colCount = data.headers.size.coerceAtLeast(1)
        val colWidths = computeColumnWidths(data, colCount, banglaTf, latinTf)
        val comfortFirstPass = planReportLayout(data, effectiveMetrics, colWidths, COMFORT_SPEC, banglaTf, latinTf, lang)
        val chosenPlan = if (comfortFirstPass.pages.size == 1) {
            comfortFirstPass
        } else {
            val compactPass = planReportLayout(data, effectiveMetrics, colWidths, COMPACT_SPEC, banglaTf, latinTf, lang)
            if (compactPass.pages.size == 1) compactPass else comfortFirstPass
        }
        return chosenPlan.pages.size
    }

    private fun writeFallbackReportPdf(
        file: File,
        data: ReportTableData,
        plan: PlannedReport,
        colWidths: FloatArray,
        banglaTf: Typeface,
        latinTf: Typeface,
        lang: AppLanguage
    ) {
        val totalPages = plan.pages.size
        val bmpW = PAGE_WIDTH * 2 // 1190px (144 DPI crisp A4)
        val bmpH = PAGE_HEIGHT * 2 // 1684px
        val pageStreams = mutableListOf<Pair<ByteArray, ByteArray>>()

        plan.pages.forEachIndexed { idx, plannedPage ->
            val bmp = android.graphics.Bitmap.createBitmap(bmpW, bmpH, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.save()
            canvas.scale(2f, 2f)
            renderPage(
                canvas = canvas,
                data = data,
                plan = plan,
                page = plannedPage,
                pageNumber = idx + 1,
                totalPages = totalPages,
                colWidths = colWidths,
                banglaTf = banglaTf,
                latinTf = latinTf,
                lang = lang
            )
            canvas.restore()

            val jpegBaos = java.io.ByteArrayOutputStream()
            bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, jpegBaos)
            bmp.recycle()
            val contentStream = "q\n$PAGE_WIDTH 0 0 $PAGE_HEIGHT 0 0 cm\n/Im1 Do\nQ\n"
                .toByteArray(Charsets.US_ASCII)
            pageStreams.add(jpegBaos.toByteArray() to contentStream)
        }

        // Objects: 1=Catalog, 2=Pages, then per page (3 objects: Page, Image XObject, Contents)
        val objCount = 3 + totalPages * 3
        val offsets = IntArray(objCount)
        val out = java.io.ByteArrayOutputStream()
        fun writeAscii(s: String) = out.write(s.toByteArray(Charsets.US_ASCII))

        writeAscii("%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n")
        offsets[1] = out.size()
        writeAscii("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")

        val kidsRefs = (0 until totalPages).joinToString(" ") { p -> "${3 + p * 3} 0 R" }
        offsets[2] = out.size()
        writeAscii("2 0 obj\n<< /Type /Pages /Kids [$kidsRefs] /Count $totalPages >>\nendobj\n")

        pageStreams.forEachIndexed { idx, (jpegBytes, contentBytes) ->
            val pageObjId = 3 + idx * 3
            val imgObjId = pageObjId + 1
            val contentObjId = pageObjId + 2

            offsets[pageObjId] = out.size()
            writeAscii(
                "$pageObjId 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $PAGE_WIDTH $PAGE_HEIGHT] " +
                    "/Resources << /XObject << /Im1 $imgObjId 0 R >> >> /Contents $contentObjId 0 R >>\nendobj\n"
            )

            offsets[imgObjId] = out.size()
            writeAscii(
                "$imgObjId 0 obj\n<< /Type /XObject /Subtype /Image /Width $bmpW /Height $bmpH " +
                    "/ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${jpegBytes.size} >>\nstream\n"
            )
            out.write(jpegBytes)
            writeAscii("\nendstream\nendobj\n")

            offsets[contentObjId] = out.size()
            writeAscii("$contentObjId 0 obj\n<< /Length ${contentBytes.size} >>\nstream\n")
            out.write(contentBytes)
            writeAscii("\nendstream\nendobj\n")
        }

        val xrefOffset = out.size()
        writeAscii("xref\n0 $objCount\n")
        writeAscii("0000000000 65535 f \n")
        for (i in 1 until objCount) {
            writeAscii(String.format(java.util.Locale.US, "%010d 00000 n \n", offsets[i]))
        }
        writeAscii("trailer\n<< /Size $objCount /Root 1 0 R >>\nstartxref\n$xrefOffset\n%%EOF\n")

        FileOutputStream(file).use { fos ->
            fos.write(out.toByteArray())
        }
    }

    private fun resolveSummaryMetrics(data: ReportTableData, lang: AppLanguage): List<ReportSummaryMetric> {
        if (data.summaryMetrics.isNotEmpty()) return data.summaryMetrics.take(8)
        val derived = mutableListOf<ReportSummaryMetric>()
        val entriesLabel = if (lang == AppLanguage.BN) "মোট রেকর্ড" else "Total Entries"
        derived.add(
            ReportSummaryMetric(
                label = entriesLabel,
                value = ShopLocale.formatNumber(data.rows.size, lang),
                detail = data.periodLabel
            )
        )
        val totals = data.totals
        if (totals != null) {
            for (i in data.headers.indices) {
                val cellVal = totals.getOrNull(i)?.trim().orEmpty()
                if (cellVal.isBlank()) continue
                if (i == 0 && cellVal.lowercase().contains("total")) continue
                if (derived.size >= 4) break
                derived.add(
                    ReportSummaryMetric(
                        label = data.headers[i],
                        value = cellVal
                    )
                )
            }
        }
        return derived
    }

    private fun computeColumnWidths(
        data: ReportTableData,
        colCount: Int,
        banglaTf: Typeface,
        latinTf: Typeface
    ): FloatArray {
        val measurePaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.LINEAR_TEXT_FLAG).apply {
            textSize = 9.0f
            typeface = banglaTf
        }
        val rawWeights = FloatArray(colCount) { 48f }
        for (c in 0 until colCount) {
            val headerW = measurePaint.measureText(data.headers.getOrElse(c) { "" }) + 16f
            var maxCellW = headerW
            val isNumeric = data.numericColumns.contains(c)
            data.rows.take(60).forEach { row ->
                val txt = row.getOrElse(c) { "" }
                val w = measurePaint.measureText(txt) + 14f
                val capped = if (isNumeric) min(w, 120f) else min(w, 220f)
                if (capped > maxCellW) maxCellW = capped
            }
            data.totals?.getOrNull(c)?.takeIf { it.isNotBlank() }?.let { totalTxt ->
                val w = measurePaint.measureText(totalTxt) + 16f
                val capped = if (isNumeric) min(w, 130f) else min(w, 180f)
                if (capped > maxCellW) maxCellW = capped
            }
            val minCol = if (isNumeric) 54f else 48f
            rawWeights[c] = max(minCol, maxCellW)
        }
        val totalWeight = rawWeights.sum().coerceAtLeast(1f)
        return FloatArray(colCount) { c ->
            (rawWeights[c] / totalWeight) * CONTENT_WIDTH
        }
    }

    private data class PlannedReport(
        val spec: LayoutSpec,
        val headerBlockHeight: Float,
        val contBannerHeight: Float,
        val storeTitleLayout: StaticLayout,
        val contactLayout: StaticLayout?,
        val reportTitleLayout: StaticLayout,
        val subtitleLayout: StaticLayout?,
        val metaLayout: StaticLayout,
        val contTitleLayout: StaticLayout,
        val kpiRows: List<List<KpiCardMeasured>>,
        val kpiSectionHeading: StaticLayout?,
        val tableHeaderRow: MeasuredRow,
        val tableRows: List<MeasuredRow>,
        val totalsRow: MeasuredRow?,
        val notesHeadingLayout: StaticLayout?,
        val notesBodyLayout: StaticLayout?,
        val notesBlockHeight: Float,
        val emptyStateLayout: StaticLayout,
        val pages: List<PlannedPage>
    )

    private fun planReportLayout(
        data: ReportTableData,
        metrics: List<ReportSummaryMetric>,
        colWidths: FloatArray,
        spec: LayoutSpec,
        banglaTf: Typeface,
        latinTf: Typeface,
        lang: AppLanguage
    ): PlannedReport {
        val storeDisplay = data.storeName.ifBlank { ShopLocale.get("receipt_header_store_default", lang) }
        val innerHeaderW = CONTENT_WIDTH - 24f

        val storeTitleLayout = createStaticLayout(
            text = storeDisplay,
            width = innerHeaderW,
            textSizeSp = 14.5f,
            color = Color.WHITE,
            align = Layout.Alignment.ALIGN_NORMAL,
            banglaTf = banglaTf,
            latinTf = latinTf,
            isBold = true
        )
        val contactStr = listOf(data.storePhone.trim(), data.storeAddress.trim())
            .filter { it.isNotBlank() }
            .joinToString("  •  ")
        val contactLayout = contactStr.takeIf { it.isNotBlank() }?.let {
            createStaticLayout(
                text = it,
                width = innerHeaderW,
                textSizeSp = 8.2f,
                color = Color.rgb(209, 227, 217),
                align = Layout.Alignment.ALIGN_NORMAL,
                banglaTf = banglaTf,
                latinTf = latinTf
            )
        }
        val reportTitleLayout = createStaticLayout(
            text = data.title,
            width = innerHeaderW,
            textSizeSp = 11.8f,
            color = COLOR_HEADER_ACCENT,
            align = Layout.Alignment.ALIGN_NORMAL,
            banglaTf = banglaTf,
            latinTf = latinTf,
            isBold = true
        )
        val subtitleLayout = data.subtitle?.takeIf { it.isNotBlank() }?.let {
            createStaticLayout(
                text = it,
                width = innerHeaderW,
                textSizeSp = 8.2f,
                color = Color.rgb(220, 232, 224),
                align = Layout.Alignment.ALIGN_NORMAL,
                banglaTf = banglaTf,
                latinTf = latinTf
            )
        }
        val metaText = ShopLocale.format("report_period_meta", lang, data.periodLabel, data.generatedAt)
        val metaLayout = createStaticLayout(
            text = metaText,
            width = innerHeaderW,
            textSizeSp = 8.2f,
            color = Color.rgb(229, 237, 232),
            align = Layout.Alignment.ALIGN_NORMAL,
            banglaTf = banglaTf,
            latinTf = latinTf
        )

        var headerH = 12f + storeTitleLayout.height + 2f
        if (contactLayout != null) headerH += contactLayout.height + 3f
        headerH += 5f + reportTitleLayout.height + 2f
        if (subtitleLayout != null) headerH += subtitleLayout.height + 2f
        headerH += 3f + metaLayout.height + 11f

        val contText = "$storeDisplay  •  ${data.title} ${ShopLocale.get("report_continued_suffix", lang)}  |  ${data.periodLabel}"
        val contTitleLayout = createStaticLayout(
            text = contText,
            width = CONTENT_WIDTH - 16f,
            textSizeSp = 8.6f,
            color = COLOR_TEXT_PRIMARY,
            align = Layout.Alignment.ALIGN_NORMAL,
            banglaTf = banglaTf,
            latinTf = latinTf,
            isBold = true
        )
        val contBannerHeight = contTitleLayout.height + 12f

        // Measure KPI summary grid (up to 4 per row)
        val kpiRows = mutableListOf<List<KpiCardMeasured>>()
        val kpiSectionHeading = if (metrics.isNotEmpty()) {
            createStaticLayout(
                text = ShopLocale.get("report_summary_heading", lang),
                width = CONTENT_WIDTH,
                textSizeSp = 9.0f,
                color = COLOR_TEXT_PRIMARY,
                align = Layout.Alignment.ALIGN_NORMAL,
                banglaTf = banglaTf,
                latinTf = latinTf,
                isBold = true
            )
        } else null

        if (metrics.isNotEmpty()) {
            metrics.chunked(4).forEach { chunk ->
                val gap = 7f
                val cardW = (CONTENT_WIDTH - gap * (chunk.size - 1)) / chunk.size.toFloat()
                val textW = (cardW - 14f).coerceAtLeast(24f)
                val rowCards = chunk.map { metric ->
                    val lbl = createStaticLayout(
                        text = metric.label,
                        width = textW,
                        textSizeSp = spec.kpiLabelSp,
                        color = COLOR_TEXT_SECONDARY,
                        align = Layout.Alignment.ALIGN_NORMAL,
                        banglaTf = banglaTf,
                        latinTf = latinTf
                    )
                    val valL = createStaticLayout(
                        text = metric.value,
                        width = textW,
                        textSizeSp = spec.kpiValueSp,
                        color = COLOR_TEXT_PRIMARY,
                        align = Layout.Alignment.ALIGN_NORMAL,
                        banglaTf = banglaTf,
                        latinTf = latinTf,
                        isBold = true
                    )
                    val detL = metric.detail.takeIf { it.isNotBlank() }?.let {
                        createStaticLayout(
                            text = it,
                            width = textW,
                            textSizeSp = 6.9f,
                            color = COLOR_TEXT_MUTED,
                            align = Layout.Alignment.ALIGN_NORMAL,
                            banglaTf = banglaTf,
                            latinTf = latinTf
                        )
                    }
                    var ch = spec.kpiPadY + lbl.height + 2.5f + valL.height + spec.kpiPadY
                    if (detL != null) ch += 2f + detL.height
                    KpiCardMeasured(lbl, valL, detL, ch)
                }
                val uniformH = rowCards.maxOf { it.height }
                kpiRows.add(rowCards.map { it.copy(height = uniformH) })
            }
        }

        // Measure table header row
        val tableHeaderRow = measureTableRow(
            cells = data.headers,
            colWidths = colWidths,
            numericColumns = data.numericColumns,
            textSizeSp = spec.headerTextSp,
            color = Color.WHITE,
            isBold = true,
            cellPadX = spec.cellPadX,
            cellPadY = spec.cellPadY + 1f,
            banglaTf = banglaTf,
            latinTf = latinTf
        )

        // Measure all data rows
        val tableRows = data.rows.map { row ->
            measureTableRow(
                cells = row,
                colWidths = colWidths,
                numericColumns = data.numericColumns,
                textSizeSp = spec.bodyTextSp,
                color = COLOR_TEXT_PRIMARY,
                isBold = false,
                cellPadX = spec.cellPadX,
                cellPadY = spec.cellPadY,
                banglaTf = banglaTf,
                latinTf = latinTf
            )
        }

        // Measure totals row
        val totalsRow = data.totals?.let { totals ->
            measureTableRow(
                cells = totals,
                colWidths = colWidths,
                numericColumns = data.numericColumns,
                textSizeSp = spec.bodyTextSp,
                color = COLOR_TEXT_PRIMARY,
                isBold = true,
                cellPadX = spec.cellPadX,
                cellPadY = spec.cellPadY + 1f,
                banglaTf = banglaTf,
                latinTf = latinTf
            )
        }

        // Measure detailed notes block
        val cleanNotes = data.notes?.trim().orEmpty()
        var notesHeadingLayout: StaticLayout? = null
        var notesBodyLayout: StaticLayout? = null
        var notesBlockHeight = 0f
        if (cleanNotes.isNotBlank()) {
            notesHeadingLayout = createStaticLayout(
                text = ShopLocale.get("report_notes_heading", lang),
                width = CONTENT_WIDTH - 20f,
                textSizeSp = 8.6f,
                color = COLOR_ACCENT_DARK,
                align = Layout.Alignment.ALIGN_NORMAL,
                banglaTf = banglaTf,
                latinTf = latinTf,
                isBold = true
            )
            notesBodyLayout = createStaticLayout(
                text = cleanNotes,
                width = CONTENT_WIDTH - 20f,
                textSizeSp = spec.bodyTextSp,
                color = COLOR_TEXT_SECONDARY,
                align = Layout.Alignment.ALIGN_NORMAL,
                banglaTf = banglaTf,
                latinTf = latinTf
            )
            notesBlockHeight = 9f + notesHeadingLayout.height + 3f + notesBodyLayout.height + 9f
        }

        val emptyStateLayout = createStaticLayout(
            text = ShopLocale.get("report_no_rows", lang),
            width = CONTENT_WIDTH - 20f,
            textSizeSp = 9.5f,
            color = COLOR_TEXT_MUTED,
            align = Layout.Alignment.ALIGN_CENTER,
            banglaTf = banglaTf,
            latinTf = latinTf
        )

        // Paginate into PlannedPages
        val bottomLimit = PAGE_HEIGHT - FOOTER_HEIGHT - 6f
        val pages = mutableListOf<PlannedPage>()

        var y = MARGIN_TOP + headerH + spec.sectionGap
        if (kpiSectionHeading != null && kpiRows.isNotEmpty()) {
            y += kpiSectionHeading.height + 4f
            kpiRows.forEachIndexed { rIdx, row ->
                y += row.first().height + if (rIdx < kpiRows.lastIndex) 6f else 0f
            }
            y += spec.sectionGap
        }

        // Table header starts on Page 1
        y += tableHeaderRow.height

        var currentIsFirst = true
        val currentRowIndices = mutableListOf<Int>()

        if (tableRows.isEmpty()) {
            y += emptyStateLayout.height + 24f
        } else {
            for (rIdx in tableRows.indices) {
                val rh = tableRows[rIdx].height
                if (y + rh > bottomLimit && (currentRowIndices.isNotEmpty() || !currentIsFirst)) {
                    pages.add(
                        PlannedPage(
                            isFirstPage = currentIsFirst,
                            rowIndices = currentRowIndices.toList(),
                            includeTotals = false,
                            includeNotes = false,
                            showEmptyState = false
                        )
                    )
                    currentIsFirst = false
                    currentRowIndices.clear()
                    y = MARGIN_TOP + contBannerHeight + 8f + tableHeaderRow.height
                }
                currentRowIndices.add(rIdx)
                y += rh
            }
        }

        var includeTotalsOnCurrentPage = false
        if (totalsRow != null) {
            if (y + totalsRow.height > bottomLimit) {
                pages.add(
                    PlannedPage(
                        isFirstPage = currentIsFirst,
                        rowIndices = currentRowIndices.toList(),
                        includeTotals = false,
                        includeNotes = false,
                        showEmptyState = tableRows.isEmpty()
                    )
                )
                currentIsFirst = false
                currentRowIndices.clear()
                y = MARGIN_TOP + contBannerHeight + 8f + tableHeaderRow.height
            }
            y += totalsRow.height
            includeTotalsOnCurrentPage = true
        }

        var includeNotesOnCurrentPage = false
        if (notesBlockHeight > 0f) {
            if (y + spec.sectionGap + notesBlockHeight > bottomLimit) {
                pages.add(
                    PlannedPage(
                        isFirstPage = currentIsFirst,
                        rowIndices = currentRowIndices.toList(),
                        includeTotals = includeTotalsOnCurrentPage,
                        includeNotes = false,
                        showEmptyState = tableRows.isEmpty() && currentIsFirst
                    )
                )
                currentIsFirst = false
                currentRowIndices.clear()
                includeTotalsOnCurrentPage = false
                y = MARGIN_TOP + contBannerHeight + 8f
            }
            includeNotesOnCurrentPage = true
        }

        pages.add(
            PlannedPage(
                isFirstPage = currentIsFirst,
                rowIndices = currentRowIndices.toList(),
                includeTotals = includeTotalsOnCurrentPage,
                includeNotes = includeNotesOnCurrentPage,
                showEmptyState = tableRows.isEmpty() && currentIsFirst
            )
        )

        return PlannedReport(
            spec = spec,
            headerBlockHeight = headerH,
            contBannerHeight = contBannerHeight,
            storeTitleLayout = storeTitleLayout,
            contactLayout = contactLayout,
            reportTitleLayout = reportTitleLayout,
            subtitleLayout = subtitleLayout,
            metaLayout = metaLayout,
            contTitleLayout = contTitleLayout,
            kpiRows = kpiRows,
            kpiSectionHeading = kpiSectionHeading,
            tableHeaderRow = tableHeaderRow,
            tableRows = tableRows,
            totalsRow = totalsRow,
            notesHeadingLayout = notesHeadingLayout,
            notesBodyLayout = notesBodyLayout,
            notesBlockHeight = notesBlockHeight,
            emptyStateLayout = emptyStateLayout,
            pages = pages
        )
    }

    private fun measureTableRow(
        cells: List<String>,
        colWidths: FloatArray,
        numericColumns: Set<Int>,
        textSizeSp: Float,
        color: Int,
        isBold: Boolean,
        cellPadX: Float,
        cellPadY: Float,
        banglaTf: Typeface,
        latinTf: Typeface
    ): MeasuredRow {
        val layouts = colWidths.indices.map { c ->
            val raw = cells.getOrElse(c) { "" }
            val align = if (numericColumns.contains(c)) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL
            val textW = (colWidths[c] - cellPadX * 2f).coerceAtLeast(14f)
            createStaticLayout(
                text = raw,
                width = textW,
                textSizeSp = textSizeSp,
                color = color,
                align = align,
                banglaTf = banglaTf,
                latinTf = latinTf,
                isBold = isBold
            )
        }
        val maxContentH = layouts.maxOfOrNull { it.height.toFloat() } ?: 10f
        return MeasuredRow(cells = layouts, height = maxContentH + cellPadY * 2f)
    }

    private fun renderPage(
        canvas: Canvas,
        data: ReportTableData,
        plan: PlannedReport,
        page: PlannedPage,
        pageNumber: Int,
        totalPages: Int,
        colWidths: FloatArray,
        banglaTf: Typeface,
        latinTf: Typeface,
        lang: AppLanguage
    ) {
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.7f
            color = COLOR_BORDER
        }

        // Crisp white page canvas
        fillPaint.color = Color.WHITE
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), fillPaint)

        var y = MARGIN_TOP
        val spec = plan.spec

        if (page.isFirstPage) {
            // 1. Polished Dark Forest Header Card with Emerald Left Stripe
            val headerRect = RectF(MARGIN_X, y, MARGIN_X + CONTENT_WIDTH, y + plan.headerBlockHeight)
            fillPaint.color = COLOR_HEADER_BG
            canvas.drawRoundRect(headerRect, 6f, 6f, fillPaint)

            fillPaint.color = COLOR_HEADER_ACCENT
            canvas.drawRoundRect(RectF(MARGIN_X, y, MARGIN_X + 4.5f, y + plan.headerBlockHeight), 3f, 3f, fillPaint)

            var hy = y + 12f
            val hx = MARGIN_X + 14f
            plan.storeTitleLayout.drawAt(canvas, hx, hy)
            hy += plan.storeTitleLayout.height + 2f
            plan.contactLayout?.let {
                it.drawAt(canvas, hx, hy)
                hy += it.height + 3f
            }
            hy += 5f
            plan.reportTitleLayout.drawAt(canvas, hx, hy)
            hy += plan.reportTitleLayout.height + 2f
            plan.subtitleLayout?.let {
                it.drawAt(canvas, hx, hy)
                hy += it.height + 2f
            }
            hy += 3f
            plan.metaLayout.drawAt(canvas, hx, hy)

            y += plan.headerBlockHeight + spec.sectionGap

            // 2. Key Summary KPI Cards Grid
            if (plan.kpiSectionHeading != null && plan.kpiRows.isNotEmpty()) {
                plan.kpiSectionHeading.drawAt(canvas, MARGIN_X, y)
                y += plan.kpiSectionHeading.height + 4f

                plan.kpiRows.forEachIndexed { rIdx, cardRow ->
                    val gap = 7f
                    val cardW = (CONTENT_WIDTH - gap * (cardRow.size - 1)) / cardRow.size.toFloat()
                    val rowH = cardRow.first().height
                    cardRow.forEachIndexed { cIdx, card ->
                        val cx = MARGIN_X + cIdx * (cardW + gap)
                        val rect = RectF(cx, y, cx + cardW, y + rowH)
                        fillPaint.color = COLOR_KPI_BG
                        canvas.drawRoundRect(rect, 5f, 5f, fillPaint)
                        strokePaint.color = COLOR_KPI_BORDER
                        canvas.drawRoundRect(rect, 5f, 5f, strokePaint)

                        // Top accent line
                        fillPaint.color = COLOR_HEADER_ACCENT
                        canvas.drawRect(cx + 6f, y, cx + cardW - 6f, y + 1.8f, fillPaint)

                        var ky = y + spec.kpiPadY
                        card.labelLayout.drawAt(canvas, cx + 7f, ky)
                        ky += card.labelLayout.height + 2.5f
                        card.valueLayout.drawAt(canvas, cx + 7f, ky)
                        ky += card.valueLayout.height + 2f
                        card.detailLayout?.drawAt(canvas, cx + 7f, ky)
                    }
                    y += rowH + if (rIdx < plan.kpiRows.lastIndex) 6f else 0f
                }
                y += spec.sectionGap
            }
        } else {
            // Continuation Banner
            val bannerRect = RectF(MARGIN_X, y, MARGIN_X + CONTENT_WIDTH, y + plan.contBannerHeight)
            fillPaint.color = COLOR_CONT_BANNER_BG
            canvas.drawRoundRect(bannerRect, 5f, 5f, fillPaint)
            strokePaint.color = COLOR_KPI_BORDER
            canvas.drawRoundRect(bannerRect, 5f, 5f, strokePaint)
            fillPaint.color = COLOR_ACCENT_DARK
            canvas.drawRect(MARGIN_X, y + 3f, MARGIN_X + 3.5f, y + plan.contBannerHeight - 3f, fillPaint)
            plan.contTitleLayout.drawAt(canvas, MARGIN_X + 10f, y + 6f)
            y += plan.contBannerHeight + 8f
        }

        // 3. Table Column Header + Data Rows + Totals (drawn whenever rows/totals/empty state exist on this page)
        val hasTableContent = page.rowIndices.isNotEmpty() || page.includeTotals || page.showEmptyState
        if (hasTableContent) {
            drawTableRow(
                canvas = canvas,
                row = plan.tableHeaderRow,
                y = y,
                colWidths = colWidths,
                bgColor = COLOR_TABLE_HEAD_BG,
                fillPaint = fillPaint,
                strokePaint = strokePaint,
                cellPadX = spec.cellPadX,
                cellPadY = spec.cellPadY + 1f,
                drawVerticalGuides = false,
                topHeavyBorder = false
            )
            y += plan.tableHeaderRow.height

            if (page.showEmptyState) {
                fillPaint.color = COLOR_ROW_ALT
                val emptyH = plan.emptyStateLayout.height + 20f
                canvas.drawRect(MARGIN_X, y, MARGIN_X + CONTENT_WIDTH, y + emptyH, fillPaint)
                strokePaint.color = COLOR_BORDER
                canvas.drawRect(MARGIN_X, y, MARGIN_X + CONTENT_WIDTH, y + emptyH, strokePaint)
                plan.emptyStateLayout.drawAt(canvas, MARGIN_X + 10f, y + 10f)
                y += emptyH
            } else {
                page.rowIndices.forEachIndexed { localIdx, rowIdx ->
                    val row = plan.tableRows[rowIdx]
                    val bg = if (rowIdx % 2 == 1) COLOR_ROW_ALT else Color.WHITE
                    drawTableRow(
                        canvas = canvas,
                        row = row,
                        y = y,
                        colWidths = colWidths,
                        bgColor = bg,
                        fillPaint = fillPaint,
                        strokePaint = strokePaint,
                        cellPadX = spec.cellPadX,
                        cellPadY = spec.cellPadY,
                        drawVerticalGuides = true,
                        topHeavyBorder = false
                    )
                    y += row.height
                }
            }

            if (page.includeTotals && plan.totalsRow != null) {
                drawTableRow(
                    canvas = canvas,
                    row = plan.totalsRow,
                    y = y,
                    colWidths = colWidths,
                    bgColor = COLOR_TOTAL_BG,
                    fillPaint = fillPaint,
                    strokePaint = strokePaint,
                    cellPadX = spec.cellPadX,
                    cellPadY = spec.cellPadY + 1f,
                    drawVerticalGuides = false,
                    topHeavyBorder = true
                )
                y += plan.totalsRow.height
            }
        }

        // 4. Detailed Notes & Insights Box
        if (page.includeNotes && plan.notesHeadingLayout != null && plan.notesBodyLayout != null) {
            y += spec.sectionGap
            val notesRect = RectF(MARGIN_X, y, MARGIN_X + CONTENT_WIDTH, y + plan.notesBlockHeight)
            fillPaint.color = COLOR_NOTES_BG
            canvas.drawRoundRect(notesRect, 5f, 5f, fillPaint)
            strokePaint.color = COLOR_KPI_BORDER
            canvas.drawRoundRect(notesRect, 5f, 5f, strokePaint)
            fillPaint.color = COLOR_ACCENT_DARK
            canvas.drawRect(MARGIN_X, y + 4f, MARGIN_X + 3f, y + plan.notesBlockHeight - 4f, fillPaint)

            var ny = y + 9f
            plan.notesHeadingLayout.drawAt(canvas, MARGIN_X + 12f, ny)
            ny += plan.notesHeadingLayout.height + 3f
            plan.notesBodyLayout.drawAt(canvas, MARGIN_X + 12f, ny)
        }

        // 5. Consistent Footer on Every Page
        drawPolishedFooter(canvas, pageNumber, totalPages, data.generatedAt, banglaTf, latinTf, lang)
    }

    private fun drawTableRow(
        canvas: Canvas,
        row: MeasuredRow,
        y: Float,
        colWidths: FloatArray,
        bgColor: Int,
        fillPaint: Paint,
        strokePaint: Paint,
        cellPadX: Float,
        cellPadY: Float,
        drawVerticalGuides: Boolean,
        topHeavyBorder: Boolean
    ) {
        fillPaint.color = bgColor
        canvas.drawRect(MARGIN_X, y, MARGIN_X + CONTENT_WIDTH, y + row.height, fillPaint)

        var x = MARGIN_X
        row.cells.forEachIndexed { cIdx, cellLayout ->
            cellLayout.drawAt(canvas, x + cellPadX, y + cellPadY)
            x += colWidths[cIdx]
            if (drawVerticalGuides && cIdx < colWidths.lastIndex) {
                strokePaint.color = Color.rgb(232, 237, 242)
                strokePaint.strokeWidth = 0.5f
                canvas.drawLine(x, y, x, y + row.height, strokePaint)
            }
        }

        if (topHeavyBorder) {
            strokePaint.color = COLOR_ACCENT_DARK
            strokePaint.strokeWidth = 1.1f
            canvas.drawLine(MARGIN_X, y, MARGIN_X + CONTENT_WIDTH, y, strokePaint)
        }
        strokePaint.color = COLOR_BORDER
        strokePaint.strokeWidth = 0.6f
        canvas.drawLine(MARGIN_X, y + row.height, MARGIN_X + CONTENT_WIDTH, y + row.height, strokePaint)
    }

    private fun drawPolishedFooter(
        canvas: Canvas,
        pageNumber: Int,
        totalPages: Int,
        generatedAt: String,
        banglaTf: Typeface,
        latinTf: Typeface,
        lang: AppLanguage
    ) {
        val ruleY = PAGE_HEIGHT - FOOTER_HEIGHT
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER
            strokeWidth = 0.7f
        }
        canvas.drawLine(MARGIN_X, ruleY, MARGIN_X + CONTENT_WIDTH, ruleY, linePaint)

        val brand = "${ShopLocale.get("report_footer_brand", lang)}  •  $generatedAt"
        val pageLabel = ShopLocale.format(
            "report_page_of",
            lang,
            ShopLocale.formatNumber(pageNumber, lang),
            ShopLocale.formatNumber(totalPages, lang)
        )
        val leftW = CONTENT_WIDTH * 0.72f
        val rightW = CONTENT_WIDTH * 0.28f

        val leftLayout = createStaticLayout(
            text = brand,
            width = leftW,
            textSizeSp = 7.6f,
            color = COLOR_TEXT_MUTED,
            align = Layout.Alignment.ALIGN_NORMAL,
            banglaTf = banglaTf,
            latinTf = latinTf
        )
        val rightLayout = createStaticLayout(
            text = pageLabel,
            width = rightW,
            textSizeSp = 7.8f,
            color = COLOR_TEXT_SECONDARY,
            align = Layout.Alignment.ALIGN_OPPOSITE,
            banglaTf = banglaTf,
            latinTf = latinTf,
            isBold = true
        )
        leftLayout.drawAt(canvas, MARGIN_X, ruleY + 6f)
        rightLayout.drawAt(canvas, MARGIN_X + leftW, ruleY + 6f)
    }

    /**
     * Generates RFC 4180 compliant CSV file.
     * CRITICAL: Prepends UTF-8 Byte Order Mark (\uFEFF) so Excel on Windows, Mac, and Android
     * immediately opens Bengali Unicode text without broken characters (mojibake).
     * Real numeric cells are written as unquoted ASCII numbers so formulas work.
     */
    fun generateCsvReport(
        context: Context,
        data: ReportTableData,
        fileName: String,
        lang: AppLanguage = AppLanguage.EN
    ): File {
        val file = File(getReportsDir(context), if (fileName.endsWith(".csv")) fileName else "$fileName.csv")

        FileOutputStream(file).use { fos ->
            // 1. Write UTF-8 BOM
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

            val writer = fos.bufferedWriter(StandardCharsets.UTF_8)

            // Shop Header
            writer.write(csvEscape(data.storeName.ifBlank { ShopLocale.get("receipt_header_store_default", lang) }))
            writer.newLine()
            if (data.storePhone.isNotBlank() || data.storeAddress.isNotBlank()) {
                writer.write(csvEscape("${data.storePhone} ${data.storeAddress}".trim()))
                writer.newLine()
            }
            writer.write(csvEscape(data.title))
            writer.newLine()
            writer.write(csvEscape(ShopLocale.format("report_period_prefix", lang, data.periodLabel)))
            writer.write(",")
            writer.write(csvEscape(ShopLocale.format("report_generated_prefix", lang, data.generatedAt)))
            writer.newLine()
            writer.newLine()

            // Headers
            writer.write(data.headers.joinToString(",") { csvEscape(it) })
            writer.newLine()

            // Rows
            for (rowIndex in data.rows.indices) {
                val row = data.rows[rowIndex]
                val rawRowNumbers = data.rawNumbers.getOrNull(rowIndex)
                val lineParts = mutableListOf<String>()

                for (colIndex in row.indices) {
                    val rawVal = rawRowNumbers?.getOrNull(colIndex)
                    if (data.numericColumns.contains(colIndex)) {
                        if (rawVal != null) {
                            lineParts.add(if (rawVal % 1.0 == 0.0) rawVal.toLong().toString() else String.format(java.util.Locale.US, "%.2f", rawVal))
                        } else {
                            val sanitized = ShopLocale.toAsciiDigits(row[colIndex]).replace("৳", "").replace(",", "").trim()
                            val parsed = sanitized.toDoubleOrNull()
                            if (parsed != null) {
                                lineParts.add(if (parsed % 1.0 == 0.0) parsed.toLong().toString() else String.format(java.util.Locale.US, "%.2f", parsed))
                            } else {
                                lineParts.add(csvEscape(row[colIndex]))
                            }
                        }
                    } else {
                        lineParts.add(csvEscape(row[colIndex]))
                    }
                }
                writer.write(lineParts.joinToString(","))
                writer.newLine()
            }

            // Totals row
            if (data.totals != null) {
                val totalParts = mutableListOf<String>()
                for (colIndex in data.totals.indices) {
                    val rawVal = data.rawTotals.getOrNull(colIndex)
                    if (data.numericColumns.contains(colIndex)) {
                        if (rawVal != null) {
                            totalParts.add(if (rawVal % 1.0 == 0.0) rawVal.toLong().toString() else String.format(java.util.Locale.US, "%.2f", rawVal))
                        } else {
                            val sanitized = ShopLocale.toAsciiDigits(data.totals[colIndex]).replace("৳", "").replace(",", "").trim()
                            val parsed = sanitized.toDoubleOrNull()
                            if (parsed != null) {
                                totalParts.add(if (parsed % 1.0 == 0.0) parsed.toLong().toString() else String.format(java.util.Locale.US, "%.2f", parsed))
                            } else {
                                totalParts.add(csvEscape(data.totals[colIndex]))
                            }
                        }
                    } else {
                        totalParts.add(csvEscape(data.totals[colIndex]))
                    }
                }
                writer.write(totalParts.joinToString(","))
                writer.newLine()
            }

            if (!data.notes.isNullOrBlank()) {
                writer.newLine()
                writer.write(csvEscape(ShopLocale.format("report_note_prefix", lang, data.notes)))
                writer.newLine()
            }

            writer.flush()
        }

        return file
    }

    private fun csvEscape(value: String): String {
        val trimmed = value.trim()
        val mustQuote = trimmed.contains(",") || trimmed.contains("\"") || trimmed.contains("\n") || trimmed.contains("\r")
        return if (mustQuote) {
            "\"${trimmed.replace("\"", "\"\"")}\""
        } else {
            trimmed
        }
    }

    /**
     * Native share sheet integration using FileProvider.
     */
    fun shareReportFile(
        context: Context,
        file: File,
        mimeType: String,
        subject: String,
        lang: AppLanguage = AppLanguage.EN
    ) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri(file.name, uri)
        }
        val chooser = Intent.createChooser(intent, ShopLocale.get("share_report_chooser", lang)).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(chooser)
    }

    /**
     * Native print integration using Android PrintManager.
     * Streams the generated PDF directly to the system Print Spooler.
     */
    fun printReportFile(
        context: Context,
        file: File,
        reportName: String,
        lang: AppLanguage = AppLanguage.EN
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            showToastOnMain(context, ShopLocale.get("printing_not_supported", lang), Toast.LENGTH_SHORT)
            shareReportFile(context, file, "application/pdf", reportName, lang)
            return
        }

        val printAdapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }

                val info = PrintDocumentInfo.Builder("$reportName.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                    .build()

                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                if (destination == null) {
                    callback?.onWriteFailed("Missing destination file descriptor")
                    return
                }

                try {
                    FileInputStream(file).use { input ->
                        FileOutputStream(destination.fileDescriptor).use { output ->
                            val buf = ByteArray(8192)
                            var len: Int
                            while (input.read(buf).also { len = it } > 0) {
                                if (cancellationSignal?.isCanceled == true) {
                                    callback?.onWriteCancelled()
                                    return
                                }
                                output.write(buf, 0, len)
                            }
                        }
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }

        printManager.print(reportName, printAdapter, PrintAttributes.Builder().build())
    }

    /**
     * Saves the generated file to public Downloads (and app-external Downloads) and,
     * for PDF files, automatically pops up an app chooser to open the PDF directly.
     */
    fun downloadReportFile(context: Context, file: File, lang: AppLanguage = AppLanguage.EN) {
        val isPdf = file.extension.equals("pdf", ignoreCase = true)
        val mimeType = if (isPdf) "application/pdf" else "text/csv"
        copyToPublicDownloads(context, file, file.name, mimeType)
        copyToAppExternalDownloads(context, file, file.name)

        val toastMsg = when {
            isPdf -> ShopLocale.get("export_saved_pdf_downloads", lang)
            file.extension.equals("csv", ignoreCase = true) -> ShopLocale.get("export_saved_csv_downloads", lang)
            else -> ShopLocale.format("saved_to_file", lang, file.name)
        }
        showToastOnMain(context, toastMsg, Toast.LENGTH_LONG)

        if (isPdf) {
            openPdfWithChooser(context, file, lang)
        }
    }

    private tailrec fun findActivity(context: Context): Activity? = when (context) {
        is Activity -> context
        is android.content.ContextWrapper -> findActivity(context.baseContext)
        else -> null
    }

    fun openPdfWithChooser(
        context: Context,
        file: File,
        lang: AppLanguage = AppLanguage.EN
    ): Boolean {
        return try {
            val authority = "${context.packageName}.fileprovider"
            val uri = runCatching {
                FileProvider.getUriForFile(context, authority, file)
            }.getOrElse { primaryErr ->
                val extTarget = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?.let { dir -> File(dir, file.name) }
                if (extTarget != null && extTarget.exists()) {
                    FileProvider.getUriForFile(context, authority, extTarget)
                } else {
                    throw primaryErr
                }
            }
            val needsNewTask = findActivity(context) == null
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = ClipData.newRawUri(file.name, uri)
            }
            val canView = context.packageManager
                .queryIntentActivities(viewIntent, PackageManager.MATCH_DEFAULT_ONLY)
                .isNotEmpty()
            val targetIntent = if (canView) {
                Intent.createChooser(viewIntent, ShopLocale.get("open_pdf_chooser", lang)).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    if (needsNewTask) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
            } else {
                val fallbackSend = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    clipData = ClipData.newRawUri(file.name, uri)
                }
                Intent.createChooser(fallbackSend, ShopLocale.get("open_pdf_chooser", lang)).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    if (needsNewTask) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
            }
            context.startActivity(targetIntent)
            true
        } catch (t: Throwable) {
            android.util.Log.e("ReportExportUtil", "Failed to open PDF chooser for ${file.absolutePath}", t)
            false
        }
    }

    private fun copyToPublicDownloads(
        context: Context,
        sourceFile: File,
        displayName: String,
        mimeType: String
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return runCatching {
            val resolver = context.contentResolver
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/StoreOS Reports")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(collection, values) ?: return false
            resolver.openOutputStream(uri)?.use { out ->
                sourceFile.inputStream().use { input -> input.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        }.getOrDefault(false)
    }

    private fun copyToAppExternalDownloads(context: Context, sourceFile: File, fileName: String) {
        runCatching {
            val extDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return@runCatching
            if (!extDir.exists()) extDir.mkdirs()
            val target = File(extDir, fileName)
            sourceFile.copyTo(target, overwrite = true)
        }
    }

    private fun showToastOnMain(context: Context, message: String, duration: Int) {
        runOnMain {
            Toast.makeText(context, message, duration).show()
        }
    }

    private fun runOnMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            Handler(Looper.getMainLooper()).post(action)
        }
    }
}
