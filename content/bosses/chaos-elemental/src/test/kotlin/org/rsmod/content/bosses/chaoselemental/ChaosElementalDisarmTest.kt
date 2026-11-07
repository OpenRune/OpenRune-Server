@file:OptIn(UncheckedType::class)

package org.rsmod.content.bosses.chaoselemental

import dev.openrune.types.InventoryServerType
import dev.openrune.types.util.UncheckedType
import dev.openrune.util.Wearpos
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.testing.random.SequenceRandom
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory

class ChaosElementalDisarmTest {

    @Test
    fun `right hand weapon is unequipped first`() {
        val worn = inventory()
        worn[Wearpos.Hat.slot] = InvObj(1, 1)
        worn[Wearpos.Front.slot] = InvObj(2, 1)
        worn[Wearpos.RightHand.slot] = InvObj(3, 1)
        worn[Wearpos.Torso.slot] = InvObj(4, 1)

        val random = SequenceRandom().apply { next = 1 }

        assertEquals(listOf(Wearpos.RightHand.slot), madnessUnequipSlots(worn, random))
    }

    @Test
    fun `random count up to four is honoured`() {
        val worn = inventory()
        worn[Wearpos.Hat.slot] = InvObj(1, 1)
        worn[Wearpos.Front.slot] = InvObj(2, 1)
        worn[Wearpos.RightHand.slot] = InvObj(3, 1)
        worn[Wearpos.Torso.slot] = InvObj(4, 1)

        val random =
            SequenceRandom().apply {
                next = 3
                then = 0
                then = 0
            }

        assertEquals(
            listOf(Wearpos.RightHand.slot, Wearpos.Hat.slot, Wearpos.Front.slot),
            madnessUnequipSlots(worn, random),
        )
    }

    @Test
    fun `at most four items are unequipped`() {
        val worn = inventory()
        for (wearpos in Wearpos.entries) {
            worn[wearpos.slot] = InvObj(wearpos.slot, 1)
        }

        val random =
            SequenceRandom().apply {
                next = 4
                then = 0
                then = 0
                then = 0
            }

        assertEquals(4, madnessUnequipSlots(worn, random).size)
    }

    @Test
    fun `empty equipment unequips nothing`() {
        assertEquals(emptyList<Int>(), madnessUnequipSlots(inventory(), SequenceRandom()))
    }

    @Test
    fun `client only slots are ignored`() {
        val worn = inventory()
        worn[Wearpos.Arms.slot] = InvObj(1, 1)
        worn[Wearpos.Hair.slot] = InvObj(2, 1)
        worn[Wearpos.Jaw.slot] = InvObj(3, 1)

        assertEquals(emptyList<Int>(), madnessUnequipSlots(worn, SequenceRandom()))
    }

    private fun inventory(): Inventory =
        Inventory(InventoryServerType(size = 14, flags = 0), arrayOfNulls(14))
}
