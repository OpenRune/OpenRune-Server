package org.rsmod.content.other.lootingbag

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory

@Execution(ExecutionMode.SAME_THREAD)
class LootingBagTest {
    private val GameTestScope.bag: Inventory
        get() = LootingBag.contents(player)

    @Test
    fun GameTestState.`opening and closing swaps the bag in its slot`() =
        runGameTest(LootingBagScript::class) {
            player.clearAllInvs()
            player.inv[3] = InvObj(LootingBag.BAG, 1)

            player.withProtectedAccess { opHeld1(3) }
            val opened = player.inv[3]?.id
            val open = LootingBag.isOpen(player)
            player.withProtectedAccess { opHeld1(3) }

            assertEquals(InvObj(LootingBag.OPEN_BAG, 1).id, opened)
            assertTrue(open)
            assertEquals(InvObj(LootingBag.BAG, 1), player.inv[3])
            assertFalse(LootingBag.isOpen(player))
            assertEquals(1, player.inv.objs.count { it != null })
        }

    @Test
    fun GameTestState.`adding fills the bag with every obj`() = runGameTest {
        player.clearAllInvs()

        val added = LootingBag.add(player, listOf(InvObj(COAL_NOTE, 5), InvObj(HELM, 1)))

        assertTrue(added)
        assertEquals(5, bag.count(COAL_NOTE))
        assertEquals(1, bag.count(HELM))
    }

    @Test
    fun GameTestState.`a bag without room for every obj takes none`() = runGameTest {
        player.clearAllInvs()
        repeat(bag.size - 1) { bag[it] = InvObj(BEER, 1) }

        val added = LootingBag.add(player, listOf(InvObj(COAL_NOTE, 5), InvObj(HELM, 1)))

        assertFalse(added)
        assertEquals(0, bag.count(COAL_NOTE))
        assertEquals(0, bag.count(HELM))
        assertEquals(bag.size - 1, bag.count(BEER))
    }

    @Test
    fun GameTestState.`a stack that would overflow takes nothing`() = runGameTest {
        player.clearAllInvs()
        bag[0] = InvObj(COAL_NOTE, Int.MAX_VALUE)

        val added = LootingBag.add(player, listOf(InvObj(HELM, 1), InvObj(COAL_NOTE, 1)))

        assertFalse(added)
        assertEquals(Int.MAX_VALUE, bag.count(COAL_NOTE))
        assertEquals(0, bag.count(HELM))
    }

    @Test
    fun GameTestState.`adding merges into the stacks the bag holds`() = runGameTest {
        player.clearAllInvs()
        bag[0] = InvObj(COAL_NOTE, 5)

        val added = LootingBag.add(player, listOf(InvObj(COAL_NOTE, 3), InvObj(HELM, 1)))

        assertTrue(added)
        assertEquals(8, bag.count(COAL_NOTE))
        assertEquals(1, bag.count(HELM))
        assertEquals(2, bag.objs.count { it != null })
    }

    @Test
    fun GameTestState.`a failed add rolls back the stack it merged into`() = runGameTest {
        player.clearAllInvs()
        bag[0] = InvObj(COAL_NOTE, 5)
        for (slot in 1 until bag.size) {
            bag[slot] = InvObj(BEER, 1)
        }
        val before = bag.objs.toList()

        val added = LootingBag.add(player, listOf(InvObj(COAL_NOTE, 3), InvObj(HELM, 1)))

        assertFalse(added)
        assertEquals(before, bag.objs.toList())
    }

    @Test
    fun GameTestState.`opening and closing keeps what the bag holds`() =
        runGameTest(LootingBagScript::class) {
            player.clearAllInvs()
            player.inv[3] = InvObj(LootingBag.BAG, 1)
            bag[0] = InvObj(COAL_NOTE, 5)

            player.withProtectedAccess { opHeld1(3) }
            player.withProtectedAccess { opHeld1(3) }

            assertEquals(5, bag.count(COAL_NOTE))
            assertEquals(1, bag.objs.count { it != null })
            assertEquals(1, player.inv.objs.count { it != null })
        }

    @Test
    fun GameTestState.`a bag in the bank counts as owned`() = runGameTest {
        player.clearAllInvs()
        val unowned = LootingBag.owns(player)
        player.invMap.getOrPut(BANK)[0] = InvObj(LootingBag.OPEN_BAG, 1)

        assertFalse(unowned)
        assertTrue(LootingBag.owns(player))
    }

    private companion object {
        const val BANK = "inv.bank"
        const val BEER = "obj.beer"
        const val HELM = "obj.adamant_full_helm"
        const val COAL_NOTE = "obj.cert_coal"
    }
}
