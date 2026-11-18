package org.rsmod.content.minigames.sepulchre

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.interf.IfButtonOp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

/* Obj transaction system is not thread-safe. */
@Execution(ExecutionMode.SAME_THREAD)
class SepulchreToolStorageTest {
    private fun GameTestState.runStorageTest(body: GameTestScope.() -> Unit) =
        runGameTest(SepulchreToolStorageScript::class) {
            advance()
            player.clearAllInvs()
            body()
        }

    private fun GameTestScope.openStorage() {
        val type = checkNotNull(ServerCacheManager.getNpc(STRANGER.asRSCM(RSCMType.NPC)))
        startAt(CoordGrid(2400, 5985, 0))
        val npc = spawnNpc(CoordGrid(2400, 5986, 0), type)
        player.opNpc5(npc)
        advance(2)
        assertTrue(player.ui.containsModal("interface.hallowed_tools"))
    }

    private fun GameTestScope.click(name: String, op: IfButtonOp = IfButtonOp.Op1) {
        player.ifButton(component(name), op = op)
    }

    private fun GameTestScope.total(obj: String, varbit: String): Int =
        player.inv.count(obj) + player.vars[varbit]

    @Test
    fun GameTestState.`tokens stored and taken back are all accounted for`() = runStorageTest {
        repeat(5) { player.inv[it] = InvObj(TOKEN, 1) }
        openStorage()

        click(QUANTITY_ALL)
        click(SIDE_TOKEN)
        advance()
        assertEquals(0, player.inv.count(TOKEN))
        assertEquals(5, player.vars[TOKEN_STORAGE])

        click(QUANTITY_1)
        click(MAIN_TOKEN)
        advance()
        assertEquals(1, player.inv.count(TOKEN))
        assertEquals(4, player.vars[TOKEN_STORAGE])

        click(MAIN_TOKEN, IfButtonOp.Op4)
        advance()
        assertEquals(5, player.inv.count(TOKEN))
        assertEquals(0, player.vars[TOKEN_STORAGE])
    }

    @Test
    fun GameTestState.`a full inventory takes nothing out of storage`() = runStorageTest {
        setVar(GRAPPLE_STORAGE, 1)
        player.fillInv(InvObj(FILLER, 1))
        openStorage()

        click(MAIN_GRAPPLE)
        advance()

        assertEquals(1, player.vars[GRAPPLE_STORAGE])
        assertEquals(0, player.inv.count(GRAPPLE))
        assertEquals(28, player.inv.count(FILLER))
    }

    @Test
    fun GameTestState.`withdrawing all takes only what the free slots hold`() = runStorageTest {
        setVar(TOKEN_STORAGE, 10)
        repeat(25) { player.inv[it] = InvObj(FILLER, 1) }
        openStorage()

        click(QUANTITY_ALL)
        click(MAIN_TOKEN)
        advance()

        assertEquals(3, player.inv.count(TOKEN))
        assertEquals(7, player.vars[TOKEN_STORAGE])
        assertEquals(10, total(TOKEN, TOKEN_STORAGE))
    }

    @Test
    fun GameTestState.`storage stops at its limit and leaves the rest carried`() = runStorageTest {
        setVar(TOKEN_STORAGE, 998)
        repeat(5) { player.inv[it] = InvObj(TOKEN, 1) }
        setVar(HAMMER_STORAGE, 1)
        player.inv[5] = InvObj(HAMMER, 1)
        openStorage()

        click(QUANTITY_ALL)
        click(SIDE_TOKEN)
        click(SIDE_HAMMER)
        advance()

        assertEquals(1000, player.vars[TOKEN_STORAGE])
        assertEquals(3, player.inv.count(TOKEN))
        assertEquals(1, player.vars[HAMMER_STORAGE])
        assertEquals(1, player.inv.count(HAMMER))
    }

    @Test
    fun GameTestState.`two clicks in one tick move a tool once`() = runStorageTest {
        player.inv[0] = InvObj(RING, 1)
        openStorage()

        click(SIDE_RING)
        click(SIDE_RING)
        advance()
        assertEquals(1, player.vars[RING_STORAGE])
        assertEquals(0, player.inv.count(RING))

        click(MAIN_RING)
        click(MAIN_RING)
        advance()
        assertEquals(0, player.vars[RING_STORAGE])
        assertEquals(1, player.inv.count(RING))
    }

    @Test
    fun GameTestState.`a tool gone before the click is processed is not stored`() =
        runStorageTest {
            player.inv[0] = InvObj(SYMBOL, 1)
            openStorage()

            click(SIDE_SYMBOL)
            player.inv[0] = null
            advance()

            assertEquals(0, player.vars[SYMBOL_STORAGE])
            assertEquals(0, player.inv.count(SYMBOL))
        }

    @Test
    fun GameTestState.`store all and withdraw all move every tool both ways`() = runStorageTest {
        val tools = listOf(GRAPPLE, HAMMER, RING, SYMBOL)
        tools.forEachIndexed { slot, tool -> player.inv[slot] = InvObj(tool, 1) }
        repeat(3) { player.inv[tools.size + it] = InvObj(TOKEN, 1) }
        openStorage()

        click(STORE_ALL)
        advance()
        assertEquals(0, player.inv.objs.count { it != null })
        assertEquals(3, player.vars[TOKEN_STORAGE])

        click(WITHDRAW_ALL)
        advance()
        tools.forEach { assertEquals(1, player.inv.count(it)) }
        assertEquals(3, player.inv.count(TOKEN))
        assertEquals(0, player.vars[TOKEN_STORAGE])
        assertEquals(0, player.vars[GRAPPLE_STORAGE])
    }

    private fun component(name: String) =
        ServerCacheManager.fromComponent(name.asRSCM(RSCMType.COMPONENT))

    private companion object {
        const val STRANGER = "npc.hallowed_lobby_npc_3op"
        const val FILLER = "obj.beer"
        const val TOKEN = "obj.hallowed_token"
        const val GRAPPLE = "obj.hallowed_grapple"
        const val HAMMER = "obj.hallowed_hammer"
        const val RING = "obj.hallowed_ring"
        const val SYMBOL = "obj.hallowed_symbol"
        const val TOKEN_STORAGE = "varbit.hallowed_storage_token"
        const val GRAPPLE_STORAGE = "varbit.hallowed_storage_grapple"
        const val HAMMER_STORAGE = "varbit.hallowed_storage_hammer"
        const val RING_STORAGE = "varbit.hallowed_storage_ring"
        const val SYMBOL_STORAGE = "varbit.hallowed_storage_symbol"
        const val SIDE_TOKEN = "component.hallowed_tool_side:token"
        const val SIDE_HAMMER = "component.hallowed_tool_side:hammer"
        const val SIDE_RING = "component.hallowed_tool_side:ring"
        const val SIDE_SYMBOL = "component.hallowed_tool_side:symbol"
        const val MAIN_TOKEN = "component.hallowed_tools:token"
        const val MAIN_GRAPPLE = "component.hallowed_tools:grapple"
        const val MAIN_RING = "component.hallowed_tools:ring"
        const val STORE_ALL = "component.hallowed_tool_side:store_all"
        const val WITHDRAW_ALL = "component.hallowed_tools:withdrawal_all"
        const val QUANTITY_1 = "component.hallowed_tools:quantity_1"
        const val QUANTITY_ALL = "component.hallowed_tools:quantity_all"
    }
}
