package org.rsmod.tools.wiki.dumping.wiki.bucket

import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ShopBucketsTest {
    private fun buckets(response: String): ShopBuckets =
        ShopBuckets(
            BucketSource(
                Files.createTempDirectory("shop-buckets"),
                throttleMillis = 0,
                fetch = { response },
            )
        )

    @Test
    fun `storeTables groups by store_notes and adapts fields`() = runBlocking {
        val b =
            buckets(
                """{"bucket":[
              {"sold_item":"Bread","store_stock":"15","restock_time":"100","store_sell_multiplier":"1000","store_buy_multiplier":"800","store_delta":"20","store_currency":"Coins","store_notes":""},
              {"sold_item":"Cake","store_stock":"5","restock_time":"800","store_sell_multiplier":"1000","store_buy_multiplier":"800","store_delta":"20","store_currency":"Coins","store_notes":""},
              {"sold_item":"Toktz-xil-ul","store_stock":"500","restock_time":"10","store_sell_multiplier":"1500","store_buy_multiplier":"150","store_delta":"20","store_currency":"Tokkul","store_notes":"(Karamja gloves)"}
            ]}"""
            )
        val tables = b.storeTables("Any Shop")
        assertEquals(2, tables.size)
        val default = tables[0]
        assertNull(default.nameNotes)
        assertEquals(listOf("Bread", "Cake"), default.lines.map { it.name })
        assertEquals(15, default.lines[0].stock)
        assertEquals(100, default.lines[0].restockCycles)
        assertEquals(1000, default.sellMultiplier)
        val gloves = tables[1]
        assertEquals("Karamja gloves", gloves.nameNotes)
        assertEquals(1500, gloves.sellMultiplier)
    }

    @Test
    fun `selectTable resolves default and namenotes selectors`() = runBlocking {
        // Real `store_notes` shapes verified live 2026-08-17 against Ali's Discount Wares
        // (subkey-before-section, no comma) and Culinaromancer's Chest (section-prefixed with a
        // comma, colliding with an unprefixed table using the same subkey text).
        val b =
            buckets(
                """{"bucket":[
              {"sold_item":"Pot","store_stock":"5","restock_time":"100","store_sell_multiplier":"1000","store_buy_multiplier":"550","store_delta":"2","store_currency":"Coins","store_notes":"(general)"},
              {"sold_item":"Oak blackjack","store_stock":"5","restock_time":"100","store_sell_multiplier":"1000","store_buy_multiplier":"550","store_delta":"2","store_currency":"Coins","store_notes":"(Defensive blackjacks)"},
              {"sold_item":"Bread","store_stock":"15","restock_time":"100","store_sell_multiplier":"1300","store_buy_multiplier":"400","store_delta":"20","store_currency":"Coins","store_notes":"(food, 2 Subquests)"},
              {"sold_item":"Ring of wealth","store_stock":"1","restock_time":"100","store_sell_multiplier":"1300","store_buy_multiplier":"400","store_delta":"20","store_currency":"Coins","store_notes":"(2 Subquest)"}
            ]}"""
            )
        val tables = b.storeTables("Any Shop")
        assertEquals("Pot", b.selectTable(tables, "general")!!.lines.single().name)
        assertEquals(
            "Oak blackjack",
            b.selectTable(tables, "blackjacks|Defensive")!!.lines.single().name,
        )
        // section+subkey must disambiguate the food-prefixed table from the unprefixed one that
        // happens to share the same subkey text.
        assertEquals("Bread", b.selectTable(tables, "food|2 Subquests")!!.lines.single().name)
        assertEquals(
            "Ring of wealth",
            b.selectTable(tables, "items|2 Subquest")!!.lines.single().name,
        )
        assertNull(b.selectTable(tables, "no-such-table"))
    }

    @Test
    fun `selectTable treats a blank or 'Default' selector as the unnamed table`() = runBlocking {
        val b =
            buckets(
                """{"bucket":[
              {"sold_item":"Scimitar","store_stock":"5","restock_time":"100","store_sell_multiplier":"1000","store_buy_multiplier":"500","store_delta":"20","store_currency":"Coins","store_notes":""},
              {"sold_item":"Special scimitar","store_stock":"5","restock_time":"100","store_sell_multiplier":"1000","store_buy_multiplier":"500","store_delta":"20","store_currency":"Coins","store_notes":"(Monkey Madness I)"}
            ]}"""
            )
        val tables = b.storeTables("Any Shop")
        assertEquals("Scimitar", b.selectTable(tables, null)!!.lines.single().name)
        assertEquals("Scimitar", b.selectTable(tables, "Default")!!.lines.single().name)
    }

    @Test
    fun `listShops adapts infobox_shop rows`() = runBlocking {
        val b =
            buckets(
                """{"bucket":[{"page_name":"Bob's Brilliant Axes.","shop_name":"Bob's Brilliant Axes"}]}"""
            )
        val shops = b.listShops()
        assertEquals("Bob's Brilliant Axes.", shops.single().pageTitle)
        assertEquals("Bob's Brilliant Axes", shops.single().infoboxName)
        assertNull(shops.single().rsName)
    }

    @Test
    fun `itemPageIds reads repeated id field`() = runBlocking {
        val b = buckets("""{"bucket":[{"id":["2309"]}]}""")
        assertEquals(listOf(2309), b.itemPageIds("Bread"))
    }

    @Test
    fun `itemVersions parses default_version as key-presence and reads the repeated item_id`() =
        runBlocking {
            // Real infobox_item shape verified live 2026-08-17 against the Waterskin and Candle
            // pages: `default_version` is present (empty string) on the default row, absent on the
            // others — it is not a literal boolean.
            val b =
                buckets(
                    """{"bucket":[
                  {"item_id":["1825"],"version_anchor":"(3)","item_name":"Waterskin(3)"},
                  {"item_id":["1823"],"default_version":"","version_anchor":"(4)","item_name":"Waterskin(4)"}
                ]}"""
                )
            val versions = b.itemVersions("Waterskin")
            assertEquals(2, versions.size)
            assertEquals(1825, versions[0].id)
            assertEquals("(3)", versions[0].versionAnchor)
            assertEquals(false, versions[0].isDefault)
            assertEquals(1823, versions[1].id)
            assertEquals(true, versions[1].isDefault)
        }

    @Test
    fun `itemVersionsByName filters non-numeric ids and finds names on other pages`() =
        runBlocking {
            // Real infobox_item shape verified live 2026-08-17: "Unlit torch" (page "Torch") and
            // "Bronze spear(kp)" (page "Bronze spear") aren't derivable from the display name by
            // stripping a suffix, but an exact item_name match finds them directly. "Candle" also
            // matches a decoy non-game row whose item_id is the literal string "interface8128",
            // which
            // must be dropped rather than parsed as a real id.
            val b =
                buckets(
                    """{"bucket":[
              {"page_name":"Torch","default_version":"","item_name":"Unlit torch","item_id":["596"],"version_anchor":"Unlit"}
            ]}"""
                )
            val versions = b.itemVersionsByName("Unlit torch")
            assertEquals(1, versions.size)
            assertEquals(596, versions.single().id)
            assertEquals(true, versions.single().isDefault)

            val candle =
                buckets(
                    """{"bucket":[
              {"page_name":"Candle","default_version":"","item_name":"Candle","item_id":["36"],"version_anchor":"Unlit"},
              {"item_name":"Candle","default_version":"","page_name":"Candles (interface item)","item_id":["interface8128"]}
            ]}"""
                )
            val candleVersions = candle.itemVersionsByName("Candle")
            assertEquals(1, candleVersions.size)
            assertEquals(36, candleVersions.single().id)
        }

    @Test
    fun `matchVersionBySuffix handles both live anchor shapes`() = runBlocking {
        // Waterskin's anchor is "(N)" (parens around the digit); Antipoison's is "N dose" (a
        // leading word token, no parens) — both verified live 2026-08-17. Bronze spear's "(kp)"
        // maps to an unrelated anchor ("Karambwan poison") that must NOT fuzzy-match "kp".
        val waterskin =
            listOf(
                ItemVersion("Waterskin(3)", 1825, "(3)", isDefault = false),
                ItemVersion("Waterskin(4)", 1823, "(4)", isDefault = true),
            )
        val bucketsInstance = buckets("{}")
        assertEquals(1825, bucketsInstance.matchVersionBySuffix(waterskin, "3")!!.id)
        assertEquals(1823, bucketsInstance.matchVersionBySuffix(waterskin, "4")!!.id)
        assertNull(bucketsInstance.matchVersionBySuffix(waterskin, "5"))

        val antipoison =
            listOf(
                ItemVersion("Antipoison(3)", 175, "3 dose", isDefault = false),
                ItemVersion("Antipoison(4)", 2446, "4 dose", isDefault = true),
            )
        assertEquals(175, bucketsInstance.matchVersionBySuffix(antipoison, "3")!!.id)

        val spear =
            listOf(
                ItemVersion("Bronze spear(kp)", 3170, "Karambwan poison", isDefault = false),
                ItemVersion("Bronze spear", 1237, "Unpoisoned", isDefault = true),
            )
        assertNull(bucketsInstance.matchVersionBySuffix(spear, "kp"))
    }

    @Test
    fun `defaultVersion accepts exactly one default row, declines ambiguity`() = runBlocking {
        val b = buckets("{}")
        // Candle: Unlit (default) / Lit — verified live 2026-08-17.
        val candle =
            listOf(
                ItemVersion("Candle", 36, "Unlit", isDefault = true),
                ItemVersion("Lit candle", 33, "Lit", isDefault = false),
            )
        assertEquals(36, b.defaultVersion(candle)!!.id)

        val noDefault =
            listOf(
                ItemVersion("A", 1, "X", isDefault = false),
                ItemVersion("B", 2, "Y", isDefault = false),
            )
        assertNull(b.defaultVersion(noDefault))

        val twoDefaults =
            listOf(
                ItemVersion("A", 1, "X", isDefault = true),
                ItemVersion("B", 2, "Y", isDefault = true),
            )
        assertNull(b.defaultVersion(twoDefaults))
    }
}
