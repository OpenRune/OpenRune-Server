package org.rsmod.content.other.special.weapons

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.*
import org.mockito.Mockito.mock
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.content.other.special.weapons.scripts.charge.WeaponChargeTransfer
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
@Execution(ExecutionMode.SAME_THREAD)
class WeaponChargeTransferTest {
    @Test fun `full charge and refund preserve exact resources`() {
        for (recipe in WeaponChargeTransfer.entries) {
            val player = player()
            val original = InvObj(recipe.empty)
            player.inv[0] = original
            supply(player, recipe, recipe.maximum)
            assertTrue(recipe.transfer(player, player.inv, 0, original, recipe.maximum, false))
            val charged = player.inv[0]!!
            assertEquals(recipe.charged.asRSCM(), charged.id)
            assertEquals(recipe.maximum, recipe.count(charged.copy()))
            assertEquals(27, player.inv.freeSpace())
            assertFalse(recipe.transfer(player, player.inv, 0, charged, 1, false))
            assertTrue(recipe.transfer(player, player.inv, 0, charged, recipe.maximum, true), "$recipe refund")
            assertEquals(original, player.inv[0])
            val expected = player().also { supply(it, recipe, recipe.maximum) }
            assertEquals(expected.inv[1], player.inv[1])
            assertEquals(expected.inv[2], player.inv[2])
        }
    }

    @Test fun `replacement during dialogue and invalid amounts cannot spend resources`() {
        for (recipe in WeaponChargeTransfer.entries) {
            val p = player()
            val original = InvObj(recipe.empty)
            p.inv[0] = original.copy()
            supply(p, recipe, 10)
            assertFalse(recipe.transfer(p, p.inv, 0, original, 10, false))
            val replacement = p.inv[0]!!
            assertFalse(recipe.transfer(p, p.inv, 0, replacement, -1, false))
            assertFalse(recipe.transfer(p, p.inv, 0, replacement, Int.MAX_VALUE, false))
            assertFalse(recipe.transfer(p, p.inv, 0, replacement, 11, false))
            assertSame(replacement, p.inv[0])
            val expected = player().also { supply(it, recipe, 10) }
            assertEquals(expected.inv[1], p.inv[1])
            assertEquals(expected.inv[2], p.inv[2])
        }
    }

    @Test fun `insufficient refund space rolls back charges and every material`() {
        for (recipe in WeaponChargeTransfer.entries) {
            val p = player()
            p.inv[0] = InvObj(recipe.empty)
            supply(p, recipe, 10)
            assertTrue(recipe.transfer(p, p.inv, 0, p.inv[0]!!, 10, false))
            val charged = p.inv[0]!!
            for (slot in 1..27) p.inv[slot] = InvObj("obj.dragon_dagger")
            if (recipe == WeaponChargeTransfer.SHADOW) p.inv[1] = null // Only one of two rune stacks fits.
            assertFalse(recipe.transfer(p, p.inv, 0, charged, 10, true))
            assertSame(charged, p.inv[0])
            if (recipe == WeaponChargeTransfer.SHADOW) Assertions.assertNull(p.inv[1])
            assertEquals(10, recipe.count(charged))
        }
    }

    private fun supply(player: Player, recipe: WeaponChargeTransfer, amount: Int) {
        if (recipe == WeaponChargeTransfer.SHADOW) {
            player.inv[1] = InvObj("obj.chaosrune", amount * 5)
            player.inv[2] = InvObj("obj.soulrune", amount * 2)
        } else player.inv[1] = InvObj("obj.ancient_essence", amount)
    }

    private fun player() = Player().apply {
        inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
    }

    companion object {
        @JvmStatic @BeforeAll fun setup() {
            ServerCacheManager.init(240).close()
            with(InvTransactionsScript(mock(PlayerItemStorage::class.java))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
        }
    }
}
