@file:OptIn(org.rsmod.annotations.InternalApi::class)
package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.dbcol.DbHelper
import io.netty.buffer.Unpooled
import net.rsprot.buffer.extensions.pVarInt2s
import net.rsprot.protocol.game.incoming.buttons.IfScriptTrigger
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.net.rsprot.handlers.IfScriptTriggerHandler
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.NpcInteractions
import org.rsmod.api.player.ironman.setGamemode
import org.rsmod.api.player.protect.*
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.content.interfaces.omnishop.OmnishopScript
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.*
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.inv.*
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
internal class MixologyContainerRewardsTest {
    @Test fun `native Supervisor Rewards opens the real shop and native buy unlocks storage once at exact resin prices`() {
        val f = Fixture(); f.funds(7750, 6300, 8950); f.open(); assertTrue(f.player.ui.containsModal("interface.omnishop_main")); f.buy(MixologyContainerRewards.STORAGE)
        assertEquals(1, f.player.vars[PotionStorage.UNLOCKED]); assertEquals(listOf(0, 0, 0), f.points()); assertEquals(0, f.player.inv.occupiedSpace()); f.funds(7750, 6300, 8950); f.buy(MixologyContainerRewards.STORAGE, 4); assertEquals(listOf(7750, 6300, 8950), f.points()); assertEquals(0, f.player.inv.occupiedSpace())
    }

    @Test fun `actual shop buy produces disassembled barrel and reagent pouch and truncates requested quantity to funds and space`() {
        for ((stock, id, costs) in listOf(Triple("dbrow.mixology_stock_prepot", 30002, listOf(17250, 14000, 18600)), Triple("dbrow.mixology_stock_reagents_pouch", 29996, listOf(13800, 11200, 15100)))) {
            val f = Fixture(); f.funds(costs[0] * 3, costs[1] * 3, costs[2] * 3); f.open(); f.buy(stock, 2); assertEquals(3, f.player.inv.filterNotNull().count { it.id == id }); assertEquals(listOf(0, 0, 0), f.points())
            val g = Fixture(); g.funds(costs[0] * 2, costs[1] * 2, costs[2] * 2); for (slot in 0..26)g.player.inv[slot] = InvObj("obj.lobster"); g.open(); g.buy(stock, 4); assertEquals(id, g.player.inv[27]!!.id); assertEquals(costs, g.points())
        }
    }

    @Test fun `full inventory purchase rolls back item rewards but account storage needs no slot and UIM cannot unlock`() {
        for (mode in 0..2) { val f = Fixture(); f.funds(17250, 14000, 18600); for (slot in 0..27)f.player.inv[slot] = InvObj("obj.lobster"); if (mode == 2)f.player.setGamemode(2); f.open(); f.buy(if (mode == 0)"dbrow.mixology_stock_prepot" else MixologyContainerRewards.STORAGE); assertEquals(28, f.player.inv.occupiedSpace()); assertEquals(if (mode == 1)1 else 0, f.player.vars[PotionStorage.UNLOCKED]); assertEquals(if (mode == 1)listOf(9500, 7700, 9650) else listOf(17250, 14000, 18600), f.points()) }
    }

    @Test fun `native shop input rejects closed or different shop death logout invalid stock and invalid purchase options`() {
        for (mode in 0..6) { val f = Fixture(); f.funds(17250, 14000, 18600); f.open(); when (mode) { 0 -> f.player.ifClose(f.bus); 1 -> VarPlayerIntMapSetter.set(f.player, "varp.omnishop_lastshop", -1); 2 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0); 3 -> f.player.pendingLogout = true; 4 -> f.player.loggingOut = true }; f.buy("dbrow.mixology_stock_prepot", if (mode == 5)0 else if (mode == 6)5 else 1); assertEquals(listOf(17250, 14000, 18600), f.points()); assertEquals(0, f.player.inv.occupiedSpace()) }
        val g = Fixture(); g.funds(17250, 14000, 18600); g.open(); g.packet(999, 1); assertEquals(0, g.player.inv.occupiedSpace()); assertEquals(listOf(17250, 14000, 18600), g.points())
    }

    @Test fun `Talk-to reaches the native rewards shop through actual menu input and insufficient resin preserves all currencies`() {
        val f = Fixture(); f.talk(); f.player.activeCoroutine!!.resumeWith(ResumePauseButtonInput("component.chatmenu:options", 0)); assertTrue(f.player.ui.containsModal("interface.omnishop_main")); f.funds(17250, 13999, 18600); f.buy("dbrow.mixology_stock_prepot"); assertEquals(listOf(17250, 13999, 18600), f.points()); assertEquals(0, f.player.inv.occupiedSpace())
    }
    private class Fixture {
        val bus = EventBus(); val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache()); val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); coords = CoordGrid(3000, 3000); currentMapClock = 100; processedMapClock = 100; statMap.setBaseLevel("stat.hitpoints", 99); statMap.setCurrentLevel("stat.hitpoints", 99) }
        val npc = Npc("npc.mm_lab_supervisor_2op", CoordGrid(3001, 3000)).apply { slotId = 2; assignUid() }; val launcher: ProtectedAccessLauncher
        init { val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }, getAlignment = { TextAlignment() })); launcher = ProtectedAccessLauncher(factory); with(InvTransactionsScript(PlayerItemStorage(emptySet()))) { scripts.startup() }; with(OmnishopScript(setOf(MixologyContainerRewards()))) { scripts.startup() }; with(MixologyContainerShopScript()) { scripts.startup() } }
        fun funds(mox: Int, aga: Int, lye: Int) { for ((key, value)in MixologyContainerRewards.CURRENCIES.values.zip(listOf(mox, aga, lye)))VarPlayerIntMapSetter.set(player, key, value) }
        fun points() = MixologyContainerRewards.CURRENCIES.values.map { player.vars[it] }
        fun open() = interact(InteractionOp.Op3)
        fun talk() = interact(InteractionOp.Op1)
        private fun interact(op: InteractionOp) { player.ifClose(bus); val producer = NpcInteractions(bus); producer.interact(player, npc, op); launcher.launch(player) { bus.publish(this, checkNotNull(producer.opTrigger(player, npc, op))) } }
        fun buy(stock: String, option: Int = 1) { val values = DbHelper.row(MixologyContainerRewards.SHOP.asRSCM()).getColumn("dbcol.omnishop_shop_data:omnishop_shop_stock").column.values!!.map { (it as Number).toInt() }; packet(values.indexOf(stock.asRSCM()), option) }
        fun packet(index: Int, option: Int) { val buffer = Unpooled.buffer(); buffer.pVarInt2s(MixologyContainerRewards.SHOP.asRSCM()); buffer.pVarInt2s(index); buffer.pVarInt2s(option); val ctor = IfScriptTrigger::class.java.declaredConstructors.single { it.parameterCount == 5 && it.parameterTypes[1] == Int::class.java }.apply { isAccessible = true }; val packet = ctor.newInstance("component.omnishop_main:trigger_buy".asRSCM(), -1, -1, 0, buffer)as IfScriptTrigger; assertEquals("component.omnishop_main:trigger_buy".asRSCM(), packet.combinedId); IfScriptTriggerHandler(bus, launcher).handle(player, packet); assertEquals(0, buffer.refCnt()) }
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
