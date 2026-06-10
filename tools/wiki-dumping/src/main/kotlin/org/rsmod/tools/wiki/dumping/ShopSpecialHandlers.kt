package org.rsmod.tools.wiki.dumping

/**
 * Manual overrides for shops whose wiki layout does not map 1:1 from slug alone.
 */
object ShopSpecialHandlers {
    private val whiteKnightRankByInv =
        mapOf(
            "whiteknight_armoury1" to "Novice",
            "whiteknight_armoury2" to "Peon",
            "whiteknight_armoury3" to "Page",
            "whiteknight_armoury4" to "Noble",
            "whiteknight_armoury5" to "Adept",
            "whiteknight_armoury6" to "Master",
        )

    private val ardougneBakerSideByInv =
        mapOf(
            "bakery" to "east",
            "bakery2" to "west",
        )

    private val wikiArticleByInv =
        mapOf(
            "mcannonshop" to "[[Nulodion]]",
            "werewolfgeneralstore" to "[[General Store (Canifis)]]",
            "death_pub" to "[[The Toad and Chicken]]",
            "feud_alispub" to "[[The Asp & Snake Bar.]]",
            "darkruneshop_crap" to "[[Mage of Zamorak]]",
            "darkruneshop_uber" to "[[Mage of Zamorak]]",
            "roguetrader_alim_defendbj_inv" to "[[Ali's Discount Wares.]]",
            "roguetrader_alim_assaultbj_inv" to "[[Ali's Discount Wares.]]",
            "roguetrader_alim_meanpclothes_inv" to "[[Ali's Discount Wares.]]",
            "roguetrader_alim_carpetclothes_inv" to "[[Ali's Discount Wares.]]",
            "roguetrader_alim_runeretail_inv" to "[[Ali's Discount Wares.]]",
            "roguetrader_alim_runewholesale_inv" to "[[Ali's Discount Wares.]]",
        )

    private val wikiStoreByInv =
        mapOf(
            "darkruneshop_crap" to "Pre-miniquest",
            "darkruneshop_uber" to "Post-miniquest",
            "roguetrader_alim_defendbj_inv" to "blackjacks|Defensive",
            "roguetrader_alim_assaultbj_inv" to "blackjacks|Offensive",
            "roguetrader_alim_meanpclothes_inv" to "clothing|Menaphite gear",
            "roguetrader_alim_carpetclothes_inv" to "clothing|Desert gear",
            "roguetrader_alim_runeretail_inv" to "runes|Elemental runes",
            "roguetrader_alim_runewholesale_inv" to "runes|Catalytic runes",
        )

    private val shopDisplayNameByInv =
        mapOf(
            "darkruneshop_crap" to "Battle Runes",
            "darkruneshop_uber" to "Battle Runes",
        )

    fun resolveRow(row: ShopNameMapper.ShopCsvEntry): ShopNameMapper.ShopCsvEntry {
        wikiArticleByInv[row.inv]?.let { wikiArticle ->
            return row.copy(
                wikiArticle = row.wikiArticle.ifBlank { wikiArticle },
                wikiStore = row.wikiStore.ifBlank { wikiStoreByInv[row.inv].orEmpty() },
            )
        }

        ardougneBakerSideByInv[row.inv]?.let { side ->
            return row.copy(
                wikiArticle = row.wikiArticle.ifBlank { "[[Ardougne Baker's Stall.]]" },
                wikiStore = row.wikiStore.ifBlank { side },
            )
        }

        val whiteKnightRank = whiteKnightRankByInv[row.inv] ?: return row
        return row.copy(
            wikiArticle = row.wikiArticle.ifBlank { "[[White Knight Armoury]]" },
            wikiStore = row.wikiStore.ifBlank { whiteKnightRank },
        )
    }

    fun resolveShopDisplayName(inv: String, infoboxName: String?): String? =
        shopDisplayNameByInv[inv] ?: infoboxName?.takeIf { it.isNotBlank() }

    /** Drops wiki rows with stock=0 (used by tiered shops to mean "not sold at this rank"). */
    fun shouldIncludeStockLine(row: ShopNameMapper.ShopCsvEntry, stock: Int): Boolean {
        if (stock > 0) {
            return true
        }
        return row.inv !in whiteKnightRankByInv
    }
}
