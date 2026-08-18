package org.rsmod.tools.wiki.dumping.dropfill

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StrictObjResolverTest {
    // The Dragon knife page: four versions, only one of which is named exactly
    // like the page — the drop row names the item that is actually dropped.
    private val dragonKnife =
        listOf(
            InfoboxItemRow(itemName = "Dragon knife", ids = listOf(22804)),
            InfoboxItemRow(itemName = "Dragon knife(p++)", ids = listOf(22810)),
            InfoboxItemRow(itemName = "Dragon knife(p)", ids = listOf(22806)),
            InfoboxItemRow(itemName = "Dragon knife(p+)", ids = listOf(22808)),
        )

    @Test
    fun `exact item name selects the base version among variants`() {
        assertEquals(listOf(22804), StrictObjResolver.narrowByItemName(dragonKnife, "Dragon knife"))
    }

    @Test
    fun `no version named exactly like the item narrows to nothing`() {
        assertEquals(
            emptyList<Int>(),
            StrictObjResolver.narrowByItemName(dragonKnife, "Dragon knife(kp)"),
        )
    }

    @Test
    fun `two versions sharing the exact name stay ambiguous`() {
        val rows =
            listOf(
                InfoboxItemRow(itemName = "Coins", ids = listOf(995)),
                InfoboxItemRow(itemName = "Coins", ids = listOf(996)),
            )
        assertEquals(listOf(995, 996), StrictObjResolver.narrowByItemName(rows, "Coins"))
    }

    @Test
    fun `duplicate ids across exact-name rows collapse to one`() {
        val rows =
            listOf(
                InfoboxItemRow(itemName = "Coins", ids = listOf(995)),
                InfoboxItemRow(itemName = "Coins", ids = listOf(995)),
            )
        assertEquals(listOf(995), StrictObjResolver.narrowByItemName(rows, "Coins"))
    }

    @Test
    fun `rows without an item name are ignored`() {
        val rows =
            listOf(
                InfoboxItemRow(itemName = null, ids = listOf(1)),
                InfoboxItemRow(itemName = "Bones", ids = listOf(526)),
            )
        assertEquals(listOf(526), StrictObjResolver.narrowByItemName(rows, "Bones"))
    }
}
