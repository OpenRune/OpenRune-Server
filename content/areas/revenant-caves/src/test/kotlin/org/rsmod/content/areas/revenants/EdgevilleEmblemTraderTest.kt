package org.rsmod.content.areas.revenants

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.NpcInteractions
import org.rsmod.api.player.interact.NpcUInteractions
import org.rsmod.api.player.protect.*
import org.rsmod.content.areas.wilderness.EmblemTraderScript
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.inv.*
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
@OptIn(org.rsmod.annotations.InternalApi::class)
internal class EdgevilleEmblemTraderTest {
    @Test fun `Edgeville talk and exchange clicks pay all six artefacts and notes exactly once`() {
        for (op in listOf(InteractionOp.Op1, InteractionOp.Op3)) {
            val f = Fixture()
            AncientArtefact.entries.forEachIndexed { i, a -> f.player.inv[i] = InvObj(a.item) }
            f.player.inv[6] = InvObj("obj.cert_wild_cave_artifact_500", 2)
            f.click(op); f.accept()
            assertEquals(32_500_000, f.player.inv.physicalCount("obj.coins"))
            assertTrue(RevenantTraderScript.carried(f.player).isEmpty())
            f.click(InteractionOp.Op3)
            assertEquals(32_500_000, f.player.inv.physicalCount("obj.coins"))
        }
    }

    @Test fun `cancel changed inventory death logout and movement never redeem artefacts`() {
        for (cause in listOf("cancel", "changed", "death", "logout", "move")) {
            val f = Fixture(); f.player.inv[0] = InvObj(AncientArtefact.Relic.item)
            f.click(InteractionOp.Op3)
            when (cause) {
                "changed" -> f.player.inv[0] = InvObj(AncientArtefact.Emblem.item)
                "death" -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0)
                "logout" -> f.player.pendingLogout = true
                "move" -> f.player.coords = f.player.coords.translateX(1)
            }
            f.accept(cause != "cancel")
            assertEquals(0, f.player.inv.physicalCount("obj.coins"))
            assertEquals(1, RevenantTraderScript.carried(f.player).size)
        }
    }

    @Test fun `coin overflow rolls back artefacts in the real trader route`() {
        val f = Fixture(); f.player.inv[0] = InvObj(AncientArtefact.Emblem.item); f.player.inv[1] = InvObj("obj.coins", Int.MAX_VALUE)
        f.click(InteractionOp.Op3); f.accept()
        assertEquals(Int.MAX_VALUE, f.player.inv.physicalCount("obj.coins"))
        assertEquals(1, f.player.inv.physicalCount(AncientArtefact.Emblem.item))
    }

    @Test fun `item on Edgeville trader accepts each artefact and its noted form`() {
        for (artefact in AncientArtefact.entries) for (noted in listOf(false, true)) {
            val f = Fixture()
            val symbol = if (noted) "obj.cert_${artefact.item.removePrefix("obj.")}" else artefact.item
            f.player.inv[0] = InvObj(symbol, if (noted) 3 else 1)
            val type = ServerCacheManager.getItem(symbol.asRSCM())!!
            val ctor = NpcUInteractions::class.java.getDeclaredConstructor(EventBus::class.java).apply { isAccessible = true }
            val producer = ctor.newInstance(f.bus)
            f.launcher.launch(f.player) { producer.interactOp(this, f.npc, f.player.inv, 0, f.npc.visType, type) }
            f.accept()
            assertEquals(artefact.coins * (if (noted) 3 else 1), f.player.inv.physicalCount("obj.coins"))
            assertTrue(RevenantTraderScript.carried(f.player).isEmpty())
        }
    }

    private class Fixture {
        val bus = EventBus(); val context = ScriptContext(bus, CheatCommandMap(), EngineQueueCache())
        val player = Player().apply { coords = CoordGrid(3088, 3505); currentMapClock = 100; processedMapClock = 100; inv = Inventory(ServerCacheManager.getInventory("inv.inv".asRSCM())!!, arrayOfNulls(28)) }
        val npc = Npc(ServerCacheManager.getNpc("npc.emblem_trader".asRSCM())!!, player.coords.translateX(1)).apply { slotId = 2; assignUid() }
        val launcher: ProtectedAccessLauncher
        init {
            val factory = mock(ProtectedAccessContextFactory::class.java).apply { `when`(create()).thenReturn(ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }, getAlignment = { TextAlignment() })) }
            launcher = ProtectedAccessLauncher(factory)
            val trader = RevenantTraderScript()
            with(trader) { context.startup() }
            with(EmblemTraderScript(setOf(trader))) { context.startup() }
        }
        fun click(op: InteractionOp) {
            val producer = NpcInteractions(bus)
            producer.interact(player, npc, op)
            launcher.launch(player) { assertTrue(bus.publish(this, producer.opTrigger(player, npc, op)!!)) }
        }
        fun accept(yes: Boolean = true) { player.resumeActiveCoroutine(ResumePauseButtonInput("component.chatmenu:options", if (yes) 1 else 2)) }
    }
    companion object {
        @JvmStatic @BeforeAll fun setup() {
            ServerCacheManager.init(240).close()
            with(InvTransactionsScript(mock(PlayerItemStorage::class.java))) { ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup() }
        }
    }
}
