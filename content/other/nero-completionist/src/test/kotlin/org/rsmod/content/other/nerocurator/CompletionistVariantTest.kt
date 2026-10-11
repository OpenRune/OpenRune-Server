@file:OptIn(org.rsmod.annotations.InternalApi::class)
package org.rsmod.content.other.nerocurator

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.hook.PlayerTeleportValidateHook
import org.rsmod.api.player.hook.PlayerTeleportValidator
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.worn.HeldEquipOp
import org.rsmod.api.random.GameRandom
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

@ResourceLock("ServerCacheManager")
class CompletionistVariantTest {
    @Test fun `actual held teleport options arrive near the corresponding guild farm and monastery and obey denials`() {
        for ((symbol, op, destination) in listOf(Triple(CompletionistVariantScript.ARDY, HeldOp.Op3, CompletionistVariantScript.MONASTERY), Triple(CompletionistVariantScript.ARDY, HeldOp.Op4, CompletionistVariantScript.FARM), Triple(CompletionistVariantScript.MYTHICAL, HeldOp.Op3, CompletionistVariantScript.MYTHS))) {
            val f = Fixture(); f.player.inv[0] = InvObj(symbol); val start = f.player.coords
            f.denial = "Teleport blocked"; f.held(op); assertEquals(start, f.player.coords)
            f.denial = null; f.held(op); assertTrue(f.player.coords.chebyshevDistance(destination) <= 3, symbol)
            for (mode in 0..2) { val g = Fixture(); g.player.inv[0] = InvObj(symbol); when (mode) { 0 -> g.player.pendingLogout = true; 1 -> g.player.loggingOut = true; 2 -> g.player.statMap.setCurrentLevel("stat.hitpoints", 0) }; val before = g.player.coords; g.held(op); assertEquals(before, g.player.coords) }
        }
    }

    @Test fun `actual Commune toggles persisted collection and Ava variants collect once per hundred ticks while worn`() {
        for (symbol in CompletionistVariantScript.AVA) {
            val f = Fixture(); f.player.inv[0] = InvObj(symbol); f.held(HeldOp.Op4); assertEquals(true, f.player.attr[CompletionistVariantScript.JUNK_DISABLED]); val saved = f.player.attr.toPersistentMap(); assertEquals(true, saved["completionist_ava_junk_disabled"])
            f.held(HeldOp.Op4); assertEquals(false, f.player.attr[CompletionistVariantScript.JUNK_DISABLED]); f.player.inv[0] = null; f.player.worn[1] = InvObj(symbol)
            f.script.onPostTick(f.player); f.player.currentMapClock = 199; f.script.onPostTick(f.player); assertEquals(0, f.player.inv.physicalCount("obj.steel_arrow"))
            f.player.currentMapClock = 200; f.script.onPostTick(f.player); assertEquals(1, f.player.inv.physicalCount("obj.steel_arrow")); f.script.onPostTick(f.player); assertEquals(1, f.player.inv.physicalCount("obj.steel_arrow"))
            f.player.attr[CompletionistVariantScript.JUNK_DISABLED] = true; f.player.currentMapClock = 300; f.script.onPostTick(f.player); assertEquals(1, f.player.inv.physicalCount("obj.steel_arrow"))
            f.player.attr.putAllFromPersistence(saved); assertEquals(true, f.player.attr[CompletionistVariantScript.JUNK_DISABLED])
        }
    }
    private class Fixture {
        val bus = EventBus(); val random = mock(GameRandom::class.java); val areas = mock(AreaChecker::class.java); var denial: String? = null
        val validator = PlayerTeleportValidator(setOf(PlayerTeleportValidateHook { _, _, _ -> denial }))
        val collision = CollisionFlagMap().apply { for (d in listOf(CoordGrid(3100, 3500), CompletionistVariantScript.MONASTERY, CompletionistVariantScript.FARM, CompletionistVariantScript.MYTHS)) for (x in -8..8 step 8) for (z in -8..8 step 8) allocateIfAbsent(d.x + x, d.z + z, 0) }
        val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); coords = CoordGrid(3100, 3500); currentMapClock = 100; processedMapClock = 100; statMap.setCurrentLevel("stat.hitpoints", 99) }
        val producer = HeldInteractions::class.java.declaredConstructors.single().apply { isAccessible = true }.let { it.newInstance(bus, mock(it.parameterTypes[1]), mock(it.parameterTypes[2]), HeldEquipOp(bus)) as HeldInteractions }
        val context = ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }, getAlignment = { TextAlignment() }, getHeldInteractions = { producer }, getCollision = { collision }, getRandom = { random }, getAreaChecker = { areas }, getTeleportValidator = { validator })
        val launcher = ProtectedAccessLauncher(mock(ProtectedAccessContextFactory::class.java).apply { `when`(create()).thenReturn(context) })
        val script = CompletionistVariantScript(validator, areas, random)
        init { val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache()); with(InvTransactionsScript(PlayerItemStorage(emptySet()))) { scripts.startup() }; with(script) { scripts.startup() } }
        fun held(op: HeldOp) { assertTrue(launcher.launch(player) { producer.interact(this, inv, 0, op) }) }
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
