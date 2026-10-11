package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import net.rsprot.protocol.game.incoming.buttons.If3Button
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.net.rsprot.handlers.If3ButtonHandler
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.hook.*
import org.rsmod.api.player.input.*
import org.rsmod.api.player.interact.*
import org.rsmod.api.player.protect.*
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.player.worn.HeldEquipOp
import org.rsmod.content.skills.magic.spell.teleports.*
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.*
import org.rsmod.game.queue.*
import org.rsmod.game.seq.EntitySeq
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

@ResourceLock("ServerCacheManager")
internal class MasterScrollBookInteractionTest {
    @Test fun `real item use fills every native scroll up to one thousand and preserves split Watson counters`() {
        val f = Fixture(); f.put(0, MasterScrollBook.EMPTY)
        for (scroll in TeleportScroll.entries) { f.put(1, scroll.item, 1200); f.use(1, 0); assertEquals(1000, MasterScrollBook.count(f.player, scroll), scroll.name); assertEquals(200, f.player.inv[1]!!.count); assertEquals(MasterScrollBook.FILLED.asRSCM(), f.player.inv[0]!!.id) }
        assertEquals(20000, MasterScrollBook.total(f.player))
        assertEquals(3, f.player.vars["varbit.bookofscrolls_watson_highbits"]); assertEquals(232, f.player.vars["varbit.bookofscrolls_watson_lowbits"])
    }

    @Test fun `native Set as default uses the actual CS2 encoding and held teleport reaches every destination`() {
        for (scroll in TeleportScroll.entries) {
            val f = Fixture(); f.put(0, MasterScrollBook.FILLED); MasterScrollBook.setCount(f.player, scroll, 2)
            f.op(HeldOp.Op3); f.button(scroll, 2); assertEquals(scroll.ordinal + 1, MasterScrollBook.defaultId(f.player), scroll.name)
            f.op(HeldOp.Op1); f.respond(true); f.finish(); assertEquals(scroll.destination, f.player.coords, scroll.name); assertEquals(1, MasterScrollBook.count(f.player, scroll)); assertEquals(EntitySeq.ZERO, f.player.pendingSequence)
            if (scroll.ordinal >= 15) { assertEquals(1, f.player.vars["varbit.bookofscrolls_selected_teleport_highbits"]); assertEquals(scroll.ordinal + 1 - 15, f.player.vars["varbit.bookofscrolls_selected_teleport_lowbits"]) }
        }
    }

    @Test fun `native Activate consumes only on actual deferred arrival and cancellation preserves the book`() {
        for (cancel in listOf(false, true)) {
            val f = Fixture(delayed = true); f.put(0, MasterScrollBook.FILLED); MasterScrollBook.setCount(f.player, TeleportScroll.Nardah, 1); val from = f.player.coords
            f.op(HeldOp.Op3); f.button(TeleportScroll.Nardah, 1); f.finish(); assertEquals(from, f.player.coords); assertEquals(1, MasterScrollBook.count(f.player, TeleportScroll.Nardah))
            f.release = true; if (cancel)f.denial = "Denied"; f.finish()
            assertEquals(if (cancel)from else TeleportScroll.Nardah.destination, f.player.coords)
            assertEquals(if (cancel)1 else 0, MasterScrollBook.count(f.player, TeleportScroll.Nardah)); assertEquals(if (cancel)MasterScrollBook.FILLED.asRSCM()else MasterScrollBook.EMPTY.asRSCM(), f.player.inv[0]!!.id)
        }
        val f = Fixture(); f.put(0, MasterScrollBook.FILLED); MasterScrollBook.setCount(f.player, TeleportScroll.Nardah, 1); val from = f.player.coords
        f.op(HeldOp.Op3); f.button(TeleportScroll.Nardah, 1); f.player.inv[0] = null; f.finish(); assertEquals(from, f.player.coords); assertEquals(1, MasterScrollBook.count(f.player, TeleportScroll.Nardah))
    }

    @Test fun `native Remove handles a full inventory and late loss without erasing stored scrolls`() {
        for (mode in 0..2) {
            val f = Fixture(); f.put(0, MasterScrollBook.FILLED); MasterScrollBook.setCount(f.player, TeleportScroll.Watson, 1000)
            if (mode == 1)for (i in 1..27)f.put(i, "obj.lobster")
            f.op(HeldOp.Op3); f.button(TeleportScroll.Watson, 3)
            if (mode == 2)f.player.inv[0] = null
            f.player.activeCoroutine!!.resumeWith(ResumePCountDialogInput(300))
            assertEquals(if (mode == 0)700 else 1000, MasterScrollBook.count(f.player, TeleportScroll.Watson)); assertEquals(if (mode == 0)300 else 0, f.player.inv.physicalCount(TeleportScroll.Watson.item))
        }
    }

    @Test fun `Revenant native menu supports all entrances and warning cancellation never consumes a scroll`() {
        for (option in 1..4) {
            val f = Fixture(); f.put(0, MasterScrollBook.FILLED); MasterScrollBook.setCount(f.player, TeleportScroll.Revenants, 2); val from = f.player.coords
            f.op(HeldOp.Op3); f.button(TeleportScroll.Revenants, 1); f.respond(option != 4, if (option == 4)1 else option); f.finish()
            assertEquals(when (option) { 1 -> TeleportScroll.Revenants.destination; 2 -> CoordGrid(3069, 3740); 3 -> CoordGrid(3075, 3653); else -> from }, f.player.coords)
            assertEquals(if (option == 4)2 else 1, MasterScrollBook.count(f.player, TeleportScroll.Revenants))
        }
    }
    private class Fixture(delayed: Boolean = false) {
        val bus = EventBus(); val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache()); val map = sharedMap; val queues = WorldQueueList(); val areas = mock(AreaChecker::class.java)
        var denial: String? = null; var release = false
        val validator = PlayerTeleportValidator(setOf(PlayerTeleportValidateHook { _, _, _ -> denial }), if (delayed)setOf(object : PlayerTeleportDelayHook { override fun remaining(player: Player, type: TeleportType, areas: AreaChecker) = if (release)0 else 2 })else emptySet(), queues)
        val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); coords = CoordGrid(3222, 3218); currentMapClock = 100; processedMapClock = 100; statMap.setBaseLevel("stat.hitpoints", 99); statMap.setCurrentLevel("stat.hitpoints", 99) }
        val launcher: ProtectedAccessLauncher; val held: HeldInteractions
        init {
            for (c in TeleportScroll.entries.map { it.destination } + listOf(player.coords, CoordGrid(3069, 3740), CoordGrid(3075, 3653)))map.allocateIfAbsent(c.x, c.z, c.level)
            val context = ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }, getCollision = { map }, getAreaChecker = { areas }, getTeleportValidator = { validator }, getAlignment = { TextAlignment() })
            val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(context); launcher = ProtectedAccessLauncher(factory)
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) { scripts.startup() }
            with(MasterScrollBookScript(TeleportScrollTravel(validator, areas, map))) { scripts.startup() }
            val ctor = HeldInteractions::class.java.declaredConstructors.single().apply { isAccessible = true }; held = ctor.newInstance(bus, mock(ctor.parameterTypes[1]), mock(ctor.parameterTypes[2]), HeldEquipOp(bus))as HeldInteractions
        }
        fun put(slot: Int, symbol: String, n: Int = 1) { player.inv[slot] = InvObj(symbol, n) }
        fun run(action: suspend ProtectedAccess.() -> Unit) { player.ifClose(bus); launcher.launch(player) { action(this) } }
        fun op(op: HeldOp) = run { held.interact(this, inv, 0, op) }
        fun use(first: Int, second: Int) = run { HeldUInteractions(bus).interact(this, inv, ServerCacheManager.getItem(inv[first]!!.id)!!, first, ServerCacheManager.getItem(inv[second]!!.id)!!, second) }
        fun button(scroll: TeleportScroll, op: Int) { val c = If3Button::class.java.declaredConstructors.single { it.parameterCount == 5 }; If3ButtonHandler(bus, launcher).handle(player, c.newInstance("component.bookofscrolls:teleportscroll_${MasterScrollBook.suffix(scroll)}".asRSCM(), -1, 65535, op, null)as If3Button) }
        fun respond(accept: Boolean, option: Int = 1) { var menus = 0; repeat(3) { val c = player.activeCoroutine ?: return; if (!c.isAwaiting(ResumePauseButtonInput::class))return; val menu = player.ui.containsModal("interface.chatmenu"); c.resumeWith(ResumePauseButtonInput(if (menu)"component.chatmenu:options"else "component.messagebox:continue", if (menu) { if (menus++ == 0)option else if (accept)1 else 2 } else -1)) } }
        fun finish() { repeat(7) { player.currentMapClock++; player.processedMapClock = player.currentMapClock; player.activeCoroutine?.advance(); val i = queues.iterator(); while (i.hasNext()) { val q = i.next(); if (--q.remainingCycles <= 0) { i.remove(); q.action() } }; i.cleanUp() } }
    }
    companion object { val sharedMap = CollisionFlagMap()

    @JvmStatic
    @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
