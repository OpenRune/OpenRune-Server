package org.rsmod.content.travel.teleports

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.rsmod.map.CoordGrid

class TeleportItemTest {
    private val somewhere =
        listOf(TeleportDestination("Somewhere", CoordGrid(3222, 3218, 0), wilderness = false))

    private val glory =
        TeleportItem(
            label = "amulet",
            objs = (4 downTo 1).map { "obj.amulet_of_glory_$it" },
            destinations = somewhere,
            uncharged = "obj.amulet_of_glory",
        )

    private val dueling =
        TeleportItem(
            label = "ring",
            objs = (2 downTo 1).map { "obj.ring_of_dueling_$it" },
            destinations = somewhere,
        )

    private val memoirs =
        TeleportItem(
            label = "book",
            objs = listOf("obj.veos_kharedsts_memoirs"),
            destinations = somewhere,
            chargeVarbit = "varbit.veos_memoir_charges",
            maxCharges = 100,
        )

    @Test
    fun `a chain steps down one obj per charge and ends uncharged or as dust`() {
        assertEquals("obj.amulet_of_glory_3", glory.afterCharge("obj.amulet_of_glory_4"))
        assertEquals("obj.amulet_of_glory", glory.afterCharge("obj.amulet_of_glory_1"))
        assertEquals(3, glory.chargesAfter("obj.amulet_of_glory_4"))
        assertEquals(0, glory.chargesAfter("obj.amulet_of_glory_1"))
        assertNull(dueling.afterCharge("obj.ring_of_dueling_1"))
    }

    @Test
    fun `objs outside the chain and varbit items keep their obj`() {
        assertEquals("obj.coins", glory.afterCharge("obj.coins"))
        assertNull(glory.chargesAfter("obj.coins"))
        val book = "obj.veos_kharedsts_memoirs"
        assertEquals(book, memoirs.afterCharge(book))
        assertNull(memoirs.chargesAfter(book))
    }
}
