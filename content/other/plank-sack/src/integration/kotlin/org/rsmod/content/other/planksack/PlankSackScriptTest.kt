package org.rsmod.content.other.planksack

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.util.UncheckedType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.inv.storage.PlayerItemStorageContext
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj

@OptIn(UncheckedType::class)
@Execution(ExecutionMode.SAME_THREAD)
class PlankSackScriptTest {
    @Test
    fun `fill, check and empty are the sack's first, third and fourth inventory ops`() {
        val type = checkNotNull(ServerCacheManager.getItem(PlankSack.SACK.asRSCM(RSCMType.OBJ)))
        assertEquals(0, type.interfaceOptions.indexOf("Fill"))
        assertEquals(2, type.interfaceOptions.indexOf("Check"))
        assertEquals(3, type.interfaceOptions.indexOf("Empty"))
    }

    private fun Player.give(slot: Int, obj: String) {
        inv[slot] = InvObj(obj.asRSCM(RSCMType.OBJ), 1)
    }

    @Test
    fun GameTestState.`fill stops at 28 planks and empty gives them all back`() =
        runInjectedGameTest(PlankSackScript::class, null, PlankSackScript::class) { script ->
            player.clearInv()
            player.give(0, PlankSack.SACK)
            for (slot in 1 until 21) player.give(slot, "obj.plank_oak")
            with(script) { player.withProtectedAccess { assertEquals(20, fill(PlankSack.planks)) } }
            for (slot in 1 until 21) player.give(slot, "obj.woodplank")
            with(script) { player.withProtectedAccess { assertEquals(8, fill(PlankSack.planks)) } }
            assertEquals(20, player.vars["varbit.plank_sack_oak"])
            assertEquals(8, player.vars["varbit.plank_sack_plain"])
            assertEquals(12, player.inv.physicalCount("obj.woodplank"))
            assertEquals(0, player.inv.physicalCount("obj.plank_oak"))

            with(script) { player.withProtectedAccess { assertEquals(15, empty()) } }
            assertEquals(0, player.inv.freeSpace())
            assertEquals(13, PlankSack.total(player))
            val inInv =
                player.inv.physicalCount("obj.woodplank") + player.inv.physicalCount("obj.plank_oak")
            assertEquals(27, inInv)
            assertEquals(40, inInv + PlankSack.total(player))
        }

    @Test
    fun GameTestState.`stored planks count as held while the sack is in the inventory`() =
        runInjectedGameTest(PlankSackScript::class, null, PlankSackScript::class) { script ->
            player.clearInv()
            player.give(0, PlankSack.SACK)
            player.give(1, "obj.plank_teak")
            player.give(2, "obj.plank_teak")
            with(script) { player.withProtectedAccess { fill(PlankSack.planks) } }
            val hook = PlankSackStorageHook()
            val ctx = PlayerItemStorageContext(player, player.inv, "obj.plank_teak")
            assertTrue(hook.shouldProcess(ctx))
            assertEquals(2, hook.contains(ctx))
            assertEquals(1, hook.remove(ctx, 1))
            assertEquals(1, player.vars["varbit.plank_sack_teak"])
            player.inv[0] = null
            assertFalse(hook.shouldProcess(ctx))
        }
}
