package com.example.domain

import com.example.data.model.Product
import com.example.util.AppLanguage

enum class ShopType(val id: String) {
    GROCERY("grocery"),
    HARDWARE("hardware"),
    PHARMACY("pharmacy"),
    CLOTHING("clothing"),
    ELECTRONICS("electronics"),
    STATIONERY("stationery"),
    GENERAL("general");

    companion object {
        fun fromId(raw: String?): ShopType? {
            val clean = raw?.trim()?.lowercase().orEmpty()
            if (clean.isEmpty()) return null
            return entries.firstOrNull { it.id == clean }
        }
    }
}

data class ShopUnitPreset(
    val id: String,
    val labelEn: String,
    val labelBn: String,
    val category: String,
    val stockUnit: String,
    val buyUnit: String,
    val buyConversion: Double,
    val sellUnits: Map<String, Double> = emptyMap()
)

object ShopTypeCatalog {

    fun displayName(type: ShopType, lang: AppLanguage): String {
        return when (lang) {
            AppLanguage.EN -> when (type) {
                ShopType.GROCERY -> "Grocery"
                ShopType.HARDWARE -> "Hardware"
                ShopType.PHARMACY -> "Pharmacy"
                ShopType.CLOTHING -> "Clothing"
                ShopType.ELECTRONICS -> "Electronics"
                ShopType.STATIONERY -> "Stationery"
                ShopType.GENERAL -> "General"
            }
            AppLanguage.BN -> when (type) {
                ShopType.GROCERY -> "মুদি দোকান"
                ShopType.HARDWARE -> "হার্ডওয়্যার"
                ShopType.PHARMACY -> "ফার্মেসি"
                ShopType.CLOTHING -> "কাপড়ের দোকান"
                ShopType.ELECTRONICS -> "ইলেকট্রনিক্স"
                ShopType.STATIONERY -> "স্টেশনারি"
                ShopType.GENERAL -> "সাধারণ দোকান"
            }
        }
    }

    fun subtitle(type: ShopType, lang: AppLanguage): String {
        return when (lang) {
            AppLanguage.EN -> when (type) {
                ShopType.GROCERY -> "sack → kg, carton → litre"
                ShopType.HARDWARE -> "box → kg, roll → meter"
                ShopType.PHARMACY -> "box → pack → pcs"
                ShopType.CLOTHING -> "dozen → pcs, roll → meter"
                ShopType.ELECTRONICS -> "roll → meter, carton → pcs"
                ShopType.STATIONERY -> "box → pcs, carton → pack"
                ShopType.GENERAL -> "pcs, box → pcs, sack → kg"
            }
            AppLanguage.BN -> when (type) {
                ShopType.GROCERY -> "বস্তা → কেজি, কার্টন → লিটার"
                ShopType.HARDWARE -> "বক্স → কেজি, রোল → মিটার"
                ShopType.PHARMACY -> "বক্স → প্যাক → পিস"
                ShopType.CLOTHING -> "ডজন → পিস, রোল → মিটার"
                ShopType.ELECTRONICS -> "রোল → মিটার, কার্টন → পিস"
                ShopType.STATIONERY -> "বক্স → পিস, কার্টন → প্যাক"
                ShopType.GENERAL -> "পিস, বক্স → পিস, বস্তা → কেজি"
            }
        }
    }

    fun startingCategories(type: ShopType): List<String> {
        return when (type) {
            ShopType.HARDWARE -> listOf(
                "Hand Tools",
                "Power Tools",
                "Nuts & Bolts",
                "Electrical",
                "Paint",
                "Plumbing"
            )
            ShopType.GROCERY -> listOf(
                "Rice & Grains",
                "Oil & Ghee",
                "Spices",
                "Dairy",
                "Beverages",
                "Snacks",
                "Personal Care"
            )
            ShopType.PHARMACY -> listOf(
                "Tablets & Capsules",
                "Syrup & Drops",
                "First Aid",
                "Personal Care",
                "Baby Care",
                "Devices"
            )
            ShopType.CLOTHING -> listOf(
                "Men's Wear",
                "Women's Wear",
                "Kids Wear",
                "Fabrics",
                "Footwear",
                "Accessories"
            )
            ShopType.ELECTRONICS -> listOf(
                "Mobile Accessories",
                "Cables & Wires",
                "Lighting",
                "Appliances",
                "Batteries",
                "Audio"
            )
            ShopType.STATIONERY -> listOf(
                "Notebooks & Paper",
                "Pens & Pencils",
                "Office Supplies",
                "Art & Craft",
                "Files & Folders",
                "School Bags"
            )
            ShopType.GENERAL -> listOf(
                "Daily Essentials",
                "Packaged Goods",
                "Beverages",
                "Household",
                "Personal Care",
                "General"
            )
        }
    }

    fun unitPresets(type: ShopType): List<ShopUnitPreset> {
        return when (type) {
            ShopType.HARDWARE -> listOf(
                ShopUnitPreset(
                    id = "hw_nails",
                    labelEn = "Nails: box → kg",
                    labelBn = "পেরেক: বক্স → কেজি",
                    category = "Nuts & Bolts",
                    stockUnit = "kg",
                    buyUnit = "box",
                    buyConversion = 25.0,
                    sellUnits = mapOf("gram" to 1000.0)
                ),
                ShopUnitPreset(
                    id = "hw_wire",
                    labelEn = "Wire: roll → meter",
                    labelBn = "তার: রোল → মিটার",
                    category = "Electrical",
                    stockUnit = "meter",
                    buyUnit = "roll",
                    buyConversion = 90.0,
                    sellUnits = mapOf("feet" to 3.2808)
                ),
                ShopUnitPreset(
                    id = "hw_paint",
                    labelEn = "Paint: carton → litre",
                    labelBn = "রং: কার্টন → লিটার",
                    category = "Paint",
                    stockUnit = "litre",
                    buyUnit = "carton",
                    buyConversion = 4.0,
                    sellUnits = mapOf("ml" to 1000.0)
                ),
                ShopUnitPreset(
                    id = "hw_tools",
                    labelEn = "Tools: box → pcs",
                    labelBn = "টুলস: বক্স → পিস",
                    category = "Hand Tools",
                    stockUnit = "pcs",
                    buyUnit = "box",
                    buyConversion = 10.0
                )
            )
            ShopType.GROCERY -> listOf(
                ShopUnitPreset(
                    id = "gr_rice",
                    labelEn = "Rice: sack → kg",
                    labelBn = "চাল: বস্তা → কেজি",
                    category = "Rice & Grains",
                    stockUnit = "kg",
                    buyUnit = "sack",
                    buyConversion = 50.0,
                    sellUnits = mapOf("gram" to 1000.0)
                ),
                ShopUnitPreset(
                    id = "gr_oil",
                    labelEn = "Oil: carton → litre",
                    labelBn = "তেল: কার্টন → লিটার",
                    category = "Oil & Ghee",
                    stockUnit = "litre",
                    buyUnit = "carton",
                    buyConversion = 12.0,
                    sellUnits = mapOf("ml" to 1000.0)
                ),
                ShopUnitPreset(
                    id = "gr_pack",
                    labelEn = "Packs: box → pcs",
                    labelBn = "প্যাক: বক্স → পিস",
                    category = "Snacks",
                    stockUnit = "pcs",
                    buyUnit = "box",
                    buyConversion = 12.0
                )
            )
            ShopType.PHARMACY -> listOf(
                ShopUnitPreset(
                    id = "ph_tab",
                    labelEn = "Tablets: box → pack",
                    labelBn = "ট্যাবলেট: বক্স → প্যাক",
                    category = "Tablets & Capsules",
                    stockUnit = "pack",
                    buyUnit = "box",
                    buyConversion = 10.0,
                    sellUnits = mapOf("pcs" to 10.0)
                ),
                ShopUnitPreset(
                    id = "ph_syr",
                    labelEn = "Syrup: carton → pcs",
                    labelBn = "সিরাপ: কার্টন → পিস",
                    category = "Syrup & Drops",
                    stockUnit = "pcs",
                    buyUnit = "carton",
                    buyConversion = 12.0
                ),
                ShopUnitPreset(
                    id = "ph_aid",
                    labelEn = "First Aid: pack → pcs",
                    labelBn = "ফাস্ট এইড: প্যাক → পিস",
                    category = "First Aid",
                    stockUnit = "pcs",
                    buyUnit = "pack",
                    buyConversion = 12.0
                )
            )
            ShopType.CLOTHING -> listOf(
                ShopUnitPreset(
                    id = "cl_pcs",
                    labelEn = "Wear: dozen → pcs",
                    labelBn = "পোশাক: ডজন → পিস",
                    category = "Men's Wear",
                    stockUnit = "pcs",
                    buyUnit = "dozen",
                    buyConversion = 12.0
                ),
                ShopUnitPreset(
                    id = "cl_fab",
                    labelEn = "Fabric: roll → meter",
                    labelBn = "কাপড়: রোল → মিটার",
                    category = "Fabrics",
                    stockUnit = "meter",
                    buyUnit = "roll",
                    buyConversion = 25.0,
                    sellUnits = mapOf("feet" to 3.2808)
                ),
                ShopUnitPreset(
                    id = "cl_pack",
                    labelEn = "Items: pack → pcs",
                    labelBn = "আইটেম: প্যাক → পিস",
                    category = "Accessories",
                    stockUnit = "pcs",
                    buyUnit = "pack",
                    buyConversion = 6.0
                )
            )
            ShopType.ELECTRONICS -> listOf(
                ShopUnitPreset(
                    id = "el_wire",
                    labelEn = "Cable: roll → meter",
                    labelBn = "কেবল: রোল → মিটার",
                    category = "Cables & Wires",
                    stockUnit = "meter",
                    buyUnit = "roll",
                    buyConversion = 100.0,
                    sellUnits = mapOf("feet" to 3.2808)
                ),
                ShopUnitPreset(
                    id = "el_bulb",
                    labelEn = "Bulbs: carton → pcs",
                    labelBn = "বাল্ব: কার্টন → পিস",
                    category = "Lighting",
                    stockUnit = "pcs",
                    buyUnit = "carton",
                    buyConversion = 20.0
                ),
                ShopUnitPreset(
                    id = "el_bat",
                    labelEn = "Cells: box → pcs",
                    labelBn = "ব্যাটারি: বক্স → পিস",
                    category = "Batteries",
                    stockUnit = "pcs",
                    buyUnit = "box",
                    buyConversion = 24.0
                )
            )
            ShopType.STATIONERY -> listOf(
                ShopUnitPreset(
                    id = "st_pen",
                    labelEn = "Pens: box → pcs",
                    labelBn = "কলম: বক্স → পিস",
                    category = "Pens & Pencils",
                    stockUnit = "pcs",
                    buyUnit = "box",
                    buyConversion = 12.0
                ),
                ShopUnitPreset(
                    id = "st_nb",
                    labelEn = "Books: dozen → pcs",
                    labelBn = "খাতা: ডজন → পিস",
                    category = "Notebooks & Paper",
                    stockUnit = "pcs",
                    buyUnit = "dozen",
                    buyConversion = 12.0
                ),
                ShopUnitPreset(
                    id = "st_paper",
                    labelEn = "Paper: carton → pack",
                    labelBn = "কাগজ: কার্টন → প্যাক",
                    category = "Office Supplies",
                    stockUnit = "pack",
                    buyUnit = "carton",
                    buyConversion = 5.0
                )
            )
            ShopType.GENERAL -> listOf(
                ShopUnitPreset(
                    id = "gn_pcs",
                    labelEn = "Standard: pcs",
                    labelBn = "সাধারণ: পিস",
                    category = "Daily Essentials",
                    stockUnit = "pcs",
                    buyUnit = "pcs",
                    buyConversion = 1.0
                ),
                ShopUnitPreset(
                    id = "gn_box",
                    labelEn = "Boxed: box → pcs",
                    labelBn = "বক্স: বক্স → পিস",
                    category = "Packaged Goods",
                    stockUnit = "pcs",
                    buyUnit = "box",
                    buyConversion = 12.0
                ),
                ShopUnitPreset(
                    id = "gn_sack",
                    labelEn = "Bulk: sack → kg",
                    labelBn = "বস্তা: বস্তা → কেজি",
                    category = "Daily Essentials",
                    stockUnit = "kg",
                    buyUnit = "sack",
                    buyConversion = 25.0,
                    sellUnits = mapOf("gram" to 1000.0)
                )
            )
        }
    }

    fun presetForCategory(type: ShopType, category: String): ShopUnitPreset? {
        val clean = category.trim()
        val presets = unitPresets(type)
        return presets.firstOrNull { it.category.equals(clean, ignoreCase = true) }
            ?: when (type) {
                ShopType.HARDWARE -> presets.firstOrNull { it.id == "hw_tools" }
                else -> presets.firstOrNull()
            }
    }

    fun mergeSuggestedCategories(existing: List<String>, type: ShopType): List<String> {
        val result = existing.map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
        for (cat in startingCategories(type)) {
            if (result.none { it.equals(cat, ignoreCase = true) }) {
                result.add(cat)
            }
        }
        return result
    }

    fun demoStoreName(type: ShopType): String = when (type) {
        ShopType.GROCERY -> "Bismillah Grocery Demo (মুদি)"
        ShopType.HARDWARE -> "Bismillah Hardware Demo (হার্ডওয়্যার)"
        ShopType.PHARMACY -> "Shifa Pharmacy Demo (ফার্মেসি)"
        ShopType.CLOTHING -> "Aarong Fabrics Demo (কাপড়)"
        ShopType.ELECTRONICS -> "TechPoint Electronics Demo"
        ShopType.STATIONERY -> "Boimela Stationery Demo"
        ShopType.GENERAL -> "Bismillah General Demo (সাধারণ)"
    }

    fun demoProducts(type: ShopType): List<Product> {
        return when (type) {
            ShopType.HARDWARE -> listOf(
                Product(
                    id = "demo_hw_1",
                    storeId = "demo_store",
                    name = "Wire Nails 2 inch",
                    nameBn = "তারকাঁটা পেরেক ২ ইঞ্চি",
                    category = "Nuts & Bolts",
                    unit = "kg",
                    buyUnit = "box",
                    buyConversion = 25.0,
                    sellUnits = mapOf("gram" to 1000.0),
                    buyPrice = 120.0,
                    sellPrice = 160.0,
                    stock = 50.0,
                    lowStockLimit = 10.0,
                    isDemo = true
                ),
                Product(
                    id = "demo_hw_2",
                    storeId = "demo_store",
                    name = "BRB Copper Wire 1.5mm",
                    nameBn = "বিআরবি কপার তার ১.৫ মিমি",
                    category = "Electrical",
                    unit = "meter",
                    buyUnit = "roll",
                    buyConversion = 90.0,
                    sellUnits = mapOf("feet" to 3.2808),
                    buyPrice = 32.0,
                    sellPrice = 42.0,
                    stock = 180.0,
                    lowStockLimit = 30.0,
                    isDemo = true
                ),
                Product(
                    id = "demo_hw_3",
                    storeId = "demo_store",
                    name = "Berger Interior Paint",
                    nameBn = "বার্জার ইন্টেরিয়র পেইন্ট",
                    category = "Paint",
                    unit = "litre",
                    buyUnit = "carton",
                    buyConversion = 4.0,
                    sellUnits = mapOf("ml" to 1000.0),
                    buyPrice = 420.0,
                    sellPrice = 490.0,
                    stock = 24.0,
                    lowStockLimit = 6.0,
                    isDemo = true
                ),
                Product(
                    id = "demo_hw_4",
                    storeId = "demo_store",
                    name = "Claw Hammer 16oz",
                    nameBn = "ক্ল হ্যামার ১৬ আউন্স",
                    category = "Hand Tools",
                    unit = "pcs",
                    buyUnit = "box",
                    buyConversion = 10.0,
                    buyPrice = 240.0,
                    sellPrice = 310.0,
                    stock = 15.0,
                    lowStockLimit = 4.0,
                    isDemo = true
                ),
                Product(
                    id = "demo_hw_5",
                    storeId = "demo_store",
                    name = "Angle Grinder 4 inch",
                    nameBn = "অ্যাঙ্গেল গ্রাইন্ডার ৪ ইঞ্চি",
                    category = "Power Tools",
                    unit = "pcs",
                    buyUnit = "pcs",
                    buyConversion = 1.0,
                    buyPrice = 2450.0,
                    sellPrice = 2950.0,
                    stock = 6.0,
                    lowStockLimit = 2.0,
                    isDemo = true
                ),
                Product(
                    id = "demo_hw_6",
                    storeId = "demo_store",
                    name = "PVC Elbow 1 inch",
                    nameBn = "পিভিসি এলবো ১ ইঞ্চি",
                    category = "Plumbing",
                    unit = "pcs",
                    buyUnit = "pack",
                    buyConversion = 20.0,
                    buyPrice = 22.0,
                    sellPrice = 32.0,
                    stock = 60.0,
                    lowStockLimit = 15.0,
                    isDemo = true
                )
            )
            ShopType.GROCERY -> listOf(
                Product(
                    id = "demo_gr_1",
                    storeId = "demo_store",
                    name = "Miniket Rice",
                    nameBn = "মিনিকেট চাল",
                    category = "Rice & Grains",
                    unit = "kg",
                    buyUnit = "sack",
                    buyConversion = 50.0,
                    sellUnits = mapOf("gram" to 1000.0),
                    buyPrice = 68.0,
                    sellPrice = 78.0,
                    stock = 125.0,
                    lowStockLimit = 25.0,
                    isDemo = true
                ),
                Product(
                    id = "demo_gr_2",
                    storeId = "demo_store",
                    name = "Rupchanda Soybean Oil",
                    nameBn = "রূপচাঁদা সয়াবিন তেল",
                    category = "Oil & Ghee",
                    unit = "litre",
                    buyUnit = "carton",
                    buyConversion = 12.0,
                    sellUnits = mapOf("ml" to 1000.0),
                    buyPrice = 165.0,
                    sellPrice = 185.0,
                    stock = 36.0,
                    lowStockLimit = 8.0,
                    isDemo = true
                ),
                Product(
                    id = "demo_gr_3",
                    storeId = "demo_store",
                    name = "Turmeric Powder 200g",
                    nameBn = "হলুদ গুঁড়া ২০০ গ্রাম",
                    category = "Spices",
                    unit = "pack",
                    buyUnit = "box",
                    buyConversion = 20.0,
                    buyPrice = 70.0,
                    sellPrice = 85.0,
                    stock = 40.0,
                    lowStockLimit = 10.0,
                    isDemo = true
                ),
                Product(
                    id = "demo_gr_4",
                    storeId = "demo_store",
                    name = "Aarong Butter 200g",
                    nameBn = "আড়ং মাখন ২০০ গ্রাম",
                    category = "Dairy",
                    unit = "pack",
                    buyUnit = "box",
                    buyConversion = 12.0,
                    buyPrice = 180.0,
                    sellPrice = 210.0,
                    stock = 14.0,
                    lowStockLimit = 5.0,
                    isDemo = true
                ),
                Product(
                    id = "demo_gr_5",
                    storeId = "demo_store",
                    name = "Mango Juice 1L",
                    nameBn = "ম্যাঙ্গো জুস ১ লিটার",
                    category = "Beverages",
                    unit = "pcs",
                    buyUnit = "carton",
                    buyConversion = 12.0,
                    buyPrice = 90.0,
                    sellPrice = 110.0,
                    stock = 24.0,
                    lowStockLimit = 6.0,
                    isDemo = true
                ),
                Product(
                    id = "demo_gr_6",
                    storeId = "demo_store",
                    name = "Lifebuoy Soap 100g",
                    nameBn = "লাইফবয় সাবান ১০০ গ্রাম",
                    category = "Personal Care",
                    unit = "pcs",
                    buyUnit = "box",
                    buyConversion = 24.0,
                    buyPrice = 48.0,
                    sellPrice = 60.0,
                    stock = 4.0,
                    lowStockLimit = 10.0,
                    isDemo = true
                )
            )
            ShopType.PHARMACY -> listOf(
                Product(
                    id = "demo_ph_1",
                    storeId = "demo_store",
                    name = "Napa 500mg Tablet",
                    nameBn = "নাপা ৫০০ মি.গ্রা.",
                    category = "Tablets & Capsules",
                    unit = "pack",
                    buyUnit = "box",
                    buyConversion = 50.0,
                    sellUnits = mapOf("pcs" to 10.0),
                    buyPrice = 10.0,
                    sellPrice = 12.0,
                    stock = 80.0,
                    lowStockLimit = 15.0,
                    isDemo = true
                ),
                Product(
                    id = "demo_ph_2",
                    storeId = "demo_store",
                    name = "Cough Relief Syrup 100ml",
                    nameBn = "কাফ সিরাপ ১০০ মি.লি.",
                    category = "Syrup & Drops",
                    unit = "pcs",
                    buyUnit = "carton",
                    buyConversion = 12.0,
                    buyPrice = 65.0,
                    sellPrice = 80.0,
                    stock = 18.0,
                    lowStockLimit = 5.0,
                    isDemo = true
                ),
                Product(
                    id = "demo_ph_3",
                    storeId = "demo_store",
                    name = "Elastic Bandage Roll",
                    nameBn = "ইলাস্টিক ব্যান্ডেজ রোল",
                    category = "First Aid",
                    unit = "pcs",
                    buyUnit = "pack",
                    buyConversion = 12.0,
                    buyPrice = 30.0,
                    sellPrice = 45.0,
                    stock = 24.0,
                    lowStockLimit = 6.0,
                    isDemo = true
                ),
                Product(
                    id = "demo_ph_4",
                    storeId = "demo_store",
                    name = "Digital Thermometer",
                    nameBn = "ডিজিটাল থার্মোমিটার",
                    category = "Devices",
                    unit = "pcs",
                    buyUnit = "box",
                    buyConversion = 10.0,
                    buyPrice = 140.0,
                    sellPrice = 190.0,
                    stock = 9.0,
                    lowStockLimit = 3.0,
                    isDemo = true
                )
            )
            ShopType.CLOTHING -> listOf(
                Product(
                    id = "demo_cl_1",
                    storeId = "demo_store",
                    name = "Cotton Polo T-Shirt",
                    nameBn = "কটন পোলো টি-শার্ট",
                    category = "Men's Wear",
                    unit = "pcs",
                    buyUnit = "dozen",
                    buyConversion = 12.0,
                    buyPrice = 280.0,
                    sellPrice = 380.0,
                    stock = 36.0,
                    lowStockLimit = 8.0,
                    isDemo = true
                ),
                Product(
                    id = "demo_cl_2",
                    storeId = "demo_store",
                    name = "Linen Cotton Fabric",
                    nameBn = "লিনেন কটন কাপড়",
                    category = "Fabrics",
                    unit = "meter",
                    buyUnit = "roll",
                    buyConversion = 25.0,
                    sellUnits = mapOf("feet" to 3.2808),
                    buyPrice = 180.0,
                    sellPrice = 240.0,
                    stock = 50.0,
                    lowStockLimit = 10.0,
                    isDemo = true
                ),
                Product(
                    id = "demo_cl_3",
                    storeId = "demo_store",
                    name = "Ladies Printed Kurti",
                    nameBn = "লেডিস প্রিন্টেড কুর্তি",
                    category = "Women's Wear",
                    unit = "pcs",
                    buyUnit = "pack",
                    buyConversion = 6.0,
                    buyPrice = 620.0,
                    sellPrice = 850.0,
                    stock = 12.0,
                    lowStockLimit = 4.0,
                    isDemo = true
                ),
                Product(
                    id = "demo_cl_4",
                    storeId = "demo_store",
                    name = "Leather Belt Classic",
                    nameBn = "চামড়ার বেল্ট",
                    category = "Accessories",
                    unit = "pcs",
                    buyUnit = "dozen",
                    buyConversion = 12.0,
                    buyPrice = 220.0,
                    sellPrice = 320.0,
                    stock = 18.0,
                    lowStockLimit = 4.0,
                    isDemo = true
                )
            )
            ShopType.ELECTRONICS -> listOf(
                Product(
                    id = "demo_el_1",
                    storeId = "demo_store",
                    name = "LED Bulb 12W Cool White",
                    nameBn = "এলইডি বাল্ব ১২ ওয়াট",
                    category = "Lighting",
                    unit = "pcs",
                    buyUnit = "carton",
                    buyConversion = 20.0,
                    buyPrice = 110.0,
                    sellPrice = 150.0,
                    stock = 40.0,
                    lowStockLimit = 10.0,
                    isDemo = true
                ),
                Product(
                    id = "demo_el_2",
                    storeId = "demo_store",
                    name = "Two-Core Power Cable",
                    nameBn = "টু-কোর পাওয়ার কেবল",
                    category = "Cables & Wires",
                    unit = "meter",
                    buyUnit = "roll",
                    buyConversion = 100.0,
                    sellUnits = mapOf("feet" to 3.2808),
                    buyPrice = 28.0,
                    sellPrice = 38.0,
                    stock = 150.0,
                    lowStockLimit = 25.0,
                    isDemo = true
                ),
                Product(
                    id = "demo_el_3",
                    storeId = "demo_store",
                    name = "USB-C Fast Charger 20W",
                    nameBn = "ইউএসবি-সি ফাস্ট চার্জার",
                    category = "Mobile Accessories",
                    unit = "pcs",
                    buyUnit = "box",
                    buyConversion = 10.0,
                    buyPrice = 340.0,
                    sellPrice = 480.0,
                    stock = 15.0,
                    lowStockLimit = 4.0,
                    isDemo = true
                ),
                Product(
                    id = "demo_el_4",
                    storeId = "demo_store",
                    name = "AA Alkaline Battery Pack",
                    nameBn = "এএ ব্যাটারি",
                    category = "Batteries",
                    unit = "pcs",
                    buyUnit = "box",
                    buyConversion = 24.0,
                    buyPrice = 25.0,
                    sellPrice = 35.0,
                    stock = 48.0,
                    lowStockLimit = 12.0,
                    isDemo = true
                )
            )
            ShopType.STATIONERY -> listOf(
                Product(
                    id = "demo_st_1",
                    storeId = "demo_store",
                    name = "Gel Ballpoint Pen",
                    nameBn = "জেল বলপেন",
                    category = "Pens & Pencils",
                    unit = "pcs",
                    buyUnit = "box",
                    buyConversion = 12.0,
                    buyPrice = 8.0,
                    sellPrice = 12.0,
                    stock = 120.0,
                    lowStockLimit = 24.0,
                    isDemo = true
                ),
                Product(
                    id = "demo_st_2",
                    storeId = "demo_store",
                    name = "A4 Copy Paper 500 Sheets",
                    nameBn = "এ৪ কাগজ রিম",
                    category = "Office Supplies",
                    unit = "pack",
                    buyUnit = "carton",
                    buyConversion = 5.0,
                    buyPrice = 420.0,
                    sellPrice = 480.0,
                    stock = 20.0,
                    lowStockLimit = 5.0,
                    isDemo = true
                ),
                Product(
                    id = "demo_st_3",
                    storeId = "demo_store",
                    name = "Hardcover Notebook 200p",
                    nameBn = "হার্ডকভার খাতা ২০০ পৃষ্ঠা",
                    category = "Notebooks & Paper",
                    unit = "pcs",
                    buyUnit = "dozen",
                    buyConversion = 12.0,
                    buyPrice = 65.0,
                    sellPrice = 90.0,
                    stock = 36.0,
                    lowStockLimit = 10.0,
                    isDemo = true
                ),
                Product(
                    id = "demo_st_4",
                    storeId = "demo_store",
                    name = "Color Pencil 24 Shades",
                    nameBn = "রং পেন্সিল ২৪ রঙ",
                    category = "Art & Craft",
                    unit = "pcs",
                    buyUnit = "box",
                    buyConversion = 12.0,
                    buyPrice = 135.0,
                    sellPrice = 180.0,
                    stock = 14.0,
                    lowStockLimit = 4.0,
                    isDemo = true
                )
            )
            ShopType.GENERAL -> listOf(
                Product(
                    id = "demo_gn_1",
                    storeId = "demo_store",
                    name = "Basmati Rice 1kg Pack",
                    nameBn = "বাসমতি চাল",
                    category = "Daily Essentials",
                    unit = "kg",
                    buyUnit = "sack",
                    buyConversion = 25.0,
                    sellUnits = mapOf("gram" to 1000.0),
                    buyPrice = 110.0,
                    sellPrice = 135.0,
                    stock = 50.0,
                    lowStockLimit = 10.0,
                    isDemo = true
                ),
                Product(
                    id = "demo_gn_2",
                    storeId = "demo_store",
                    name = "Mineral Water 500ml",
                    nameBn = "মিনারেল ওয়াটার ৫০০ মি.লি.",
                    category = "Beverages",
                    unit = "pcs",
                    buyUnit = "carton",
                    buyConversion = 24.0,
                    buyPrice = 14.0,
                    sellPrice = 20.0,
                    stock = 48.0,
                    lowStockLimit = 12.0,
                    isDemo = true
                ),
                Product(
                    id = "demo_gn_3",
                    storeId = "demo_store",
                    name = "Dishwash Liquid 500ml",
                    nameBn = "ডিশওয়াশ লিকুইড",
                    category = "Household",
                    unit = "pcs",
                    buyUnit = "box",
                    buyConversion = 12.0,
                    buyPrice = 115.0,
                    sellPrice = 140.0,
                    stock = 18.0,
                    lowStockLimit = 5.0,
                    isDemo = true
                ),
                Product(
                    id = "demo_gn_4",
                    storeId = "demo_store",
                    name = "Toilet Tissue Roll",
                    nameBn = "টিস্যু রোল",
                    category = "Personal Care",
                    unit = "pcs",
                    buyUnit = "pack",
                    buyConversion = 10.0,
                    buyPrice = 28.0,
                    sellPrice = 38.0,
                    stock = 30.0,
                    lowStockLimit = 8.0,
                    isDemo = true
                )
            )
        }
    }
}
