package com.example.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.res.ResourcesCompat
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.BusinessCardDraft
import com.example.data.model.BusinessCardFontPair
import com.example.data.model.BusinessCardFontSize
import com.example.data.model.BusinessCardLayout
import com.example.data.model.BusinessCardPalettes
import com.example.data.model.BusinessCardSide
import com.example.data.model.BusinessCardTextAlign
import com.example.util.BusinessCardLocalStore
import com.example.util.BusinessCardRenderer
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BusinessCardEngineAndExportTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        BusinessCardLocalStore.getRootDir(context).deleteRecursively()
    }

    @Test
    fun all_12_palettes_enforce_role_rules_and_minimum_4_5_contrast_normal_and_light() {
        assertEquals(12, BusinessCardPalettes.ALL.size)

        BusinessCardPalettes.ALL.forEach { palette ->
            assertEquals("Palette ${palette.nameEn} must have 4 colors", 4, palette.hexColors.size)
            val sortedLum = palette.hexColors
                .map { BusinessCardPaletteEngine.parseHexColor(it) }
                .sortedBy { BusinessCardPaletteEngine.relativeLuminance(it) }
            val darkest = sortedLum.first()
            val lightest = sortedLum.last()

            // Normal (dark) card mode
            val darkRoles = BusinessCardPaletteEngine.resolveRoles(palette, isLightCard = false)
            assertEquals("Dark card bg should be darkest for ${palette.nameEn}", darkest, darkRoles.backgroundArgb)
            assertEquals("Dark card main text should be lightest for ${palette.nameEn}", lightest, darkRoles.mainTextArgb)
            assertTrue(
                "Main text contrast >= 4.5 for ${palette.nameEn}, was ${darkRoles.mainContrastRatio}",
                darkRoles.mainContrastRatio >= 4.5
            )
            assertTrue(
                "Highlight text contrast >= 4.5 for ${palette.nameEn}, was ${darkRoles.highlightContrastRatio}",
                darkRoles.highlightContrastRatio >= 4.5
            )

            // Light card mode (swaps background and main text)
            val lightRoles = BusinessCardPaletteEngine.resolveRoles(palette, isLightCard = true)
            assertEquals("Light card bg should be lightest for ${palette.nameEn}", lightest, lightRoles.backgroundArgb)
            assertEquals("Light card main text should be darkest for ${palette.nameEn}", darkest, lightRoles.mainTextArgb)
            assertTrue(
                "Light card main text contrast >= 4.5 for ${palette.nameEn}, was ${lightRoles.mainContrastRatio}",
                lightRoles.mainContrastRatio >= 4.5
            )
            assertTrue(
                "Light card highlight text contrast >= 4.5 for ${palette.nameEn}, was ${lightRoles.highlightContrastRatio}",
                lightRoles.highlightContrastRatio >= 4.5
            )

            // Background photo overlay + split panel left-panel contrast
            listOf(darkRoles, lightRoles).forEach { roles ->
                val (photoMain, photoHighlight) = BusinessCardPaletteEngine.effectiveTextRolesForCanvas(
                    roles = roles,
                    hasBackgroundPhoto = true
                )
                assertTrue(
                    "Photo main text contrast >= 4.5 on dark overlay for ${palette.nameEn}",
                    BusinessCardPaletteEngine.contrastRatio(photoMain, android.graphics.Color.BLACK) >= 4.5
                )
                assertTrue(
                    "Photo highlight text contrast >= 4.5 on dark overlay for ${palette.nameEn}",
                    BusinessCardPaletteEngine.contrastRatio(photoHighlight, android.graphics.Color.BLACK) >= 4.5
                )
                val panelText = BusinessCardPaletteEngine.resolveSplitPanelTextColor(
                    panelBgArgb = roles.splitPanelArgb,
                    roles = roles
                )
                assertTrue(
                    "Split panel text contrast >= 4.5 for ${palette.nameEn}",
                    BusinessCardPaletteEngine.contrastRatio(panelText, roles.splitPanelArgb) >= 4.5
                )
            }
        }
    }

    @Test
    fun bundled_bangla_and_latin_fonts_load_and_split_mixed_runs() {
        BusinessCardFontPair.entries.forEach { pair ->
            assertNotNull("Bangla font ${pair.name} missing", ResourcesCompat.getFont(context, pair.banglaFontRes))
            assertNotNull("Latin font ${pair.name} missing", ResourcesCompat.getFont(context, pair.latinFontRes))
        }

        val mixed = "রহিম এন্টারপ্রাইজ · Rahim Enterprise"
        val runs = BusinessCardRenderer.splitScriptRuns(mixed)
        assertTrue("Expected separate Bangla and Latin runs", runs.size >= 2)
        assertTrue(runs.any { it.second && it.first.contains("রহিম") })
        assertTrue(runs.any { !it.second && it.first.contains("Rahim Enterprise") })

        // Bangla conjunct with ZWJ (\u200D) and Dari (।) must stay inside one contiguous Bangla run
        val conjunct = "র\u200D্যাব ও বিশ্বাসযোগ্য সেবা।"
        val conjunctRuns = BusinessCardRenderer.splitScriptRuns(conjunct)
        assertEquals(1, conjunctRuns.size)
        assertTrue(conjunctRuns.single().second)
        assertEquals(conjunct, conjunctRuns.single().first)
    }

    @Test
    fun background_crop_enforces_3_5_to_2_ratio_and_png_pdf_exports_succeed_with_bangla() {
        // Create a square 1000x1000 test photo and verify cropping produces exact 1.75 (3.5:2) ratio
        val rawBgFile = File(context.cacheDir, "raw_square_bg.png")
        val rawSquare = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.rgb(30, 80, 60))
        }
        FileOutputStream(rawBgFile).use { rawSquare.compress(Bitmap.CompressFormat.PNG, 100, it) }
        rawSquare.recycle()

        val croppedBgUri = BusinessCardLocalStore.importAndCropBackground(context, Uri.fromFile(rawBgFile))
        val decodedBg = BusinessCardLocalStore.decodeLocalBitmap(croppedBgUri)!!
        assertEquals(1050, decodedBg.width)
        assertEquals(600, decodedBg.height)
        assertTrue(abs((decodedBg.width.toFloat() / decodedBg.height.toFloat()) - 1.75f) < 0.01f)
        decodedBg.recycle()

        val draft = BusinessCardDraft(
            paletteId = "deep_navy",
            isLightCard = false,
            layout = BusinessCardLayout.SPLIT_PANEL,
            side = BusinessCardSide.FRONT,
            fontPair = BusinessCardFontPair.SERIF_PLAYFAIR,
            fontSize = BusinessCardFontSize.LARGE,
            textAlign = BusinessCardTextAlign.LEFT,
            shopName = "রহিম এন্টারপ্রাইজ · Rahim Enterprise",
            ownerName = "মোঃ করিম উদ্দিন",
            title = "Proprietor · প্রোপ্রাইটর",
            tagline = "সুলভ মূল্যে মানসম্মত পণ্য",
            phone = "+880 1712-345678",
            email = "rahim@store.com",
            address = "দোকান ১৪, নিউ মার্কেট, ঢাকা",
            backgroundImageUri = croppedBgUri,
            backgroundOverlayAlpha = 0.50f
        )

        // Export PNG and verify 1050x600 (3.5 x 2 ratio) + distinct rendered pixels
        val pngResult = BusinessCardRenderer.exportPng(context, draft)
        assertTrue(pngResult.file.exists() && pngResult.file.length() > 1000L)
        val pngBmp = BitmapFactory.decodeFile(pngResult.file.absolutePath)
        assertNotNull(pngBmp)
        assertEquals(1050, pngBmp.width)
        assertEquals(600, pngBmp.height)
        val samplePixels = IntArray(pngBmp.width)
        pngBmp.getPixels(samplePixels, 0, pngBmp.width, 0, 120, pngBmp.width, 1)
        assertTrue("Rendered card bitmap must contain drawn artwork/text", samplePixels.toSet().size > 2)
        pngBmp.recycle()

        // Export PDF and verify non-empty PDF stream + exact 252x144 pt print MediaBox
        val pdfResult = BusinessCardRenderer.exportPdf(context, draft)
        assertTrue(pdfResult.file.exists() && pdfResult.file.length() > 500L)
        val pdfBytes = pdfResult.file.readBytes()
        val header = String(pdfBytes.take(8).toByteArray(), Charsets.US_ASCII)
        assertTrue("PDF should start with %PDF-, got $header", header.startsWith("%PDF-"))
        val pdfContent = String(pdfBytes, Charsets.ISO_8859_1)
        assertTrue("PDF MediaBox should match 3.5x2 in (252x144 pt)", pdfContent.contains("252") && pdfContent.contains("144"))
    }

    @Test
    fun my_cards_phone_only_storage_saves_loads_and_deletes_without_cloud() {
        val first = BusinessCardDraft(
            paletteId = "forest_cream",
            shopName = "পদ্মা স্টোর",
            ownerName = "আলী হোসেন",
            title = "মালিক",
            phone = "01711000000"
        )
        val second = BusinessCardDraft(
            paletteId = "old_money",
            isLightCard = true,
            layout = BusinessCardLayout.CENTERED,
            shopName = "Padma Books",
            ownerName = "Ali Hossain"
        )

        val saved1 = BusinessCardLocalStore.saveCard(context, first)
        val saved2 = BusinessCardLocalStore.saveCard(context, second)

        val loaded = BusinessCardLocalStore.loadSavedCards(context)
        assertEquals(2, loaded.size)
        assertEquals(saved2.id, loaded.first().id)
        assertEquals("Padma Books", loaded[0].draft.shopName)
        assertTrue(loaded[0].draft.isLightCard)
        assertEquals("পদ্মা স্টোর", loaded[1].draft.shopName)

        val afterDelete = BusinessCardLocalStore.deleteCard(context, saved1.id)
        assertEquals(1, afterDelete.size)
        assertEquals(saved2.id, afterDelete.single().id)
    }
}
