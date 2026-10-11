@file:OptIn(org.rsmod.annotations.InternalApi::class)

package org.rsmod.content.other.nerocurator

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import net.rsprot.protocol.game.incoming.resumed.ResumePauseButton
import net.rsprot.protocol.util.CombinedId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.death.*
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.net.rsprot.handlers.ResumePauseButtonHandler
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.hook.GroundItemDropResolver
import org.rsmod.api.player.interact.NpcUInteractions
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.protect.clearPendingAction
import org.rsmod.api.player.worn.DizanasQuiver
import org.rsmod.api.player.worn.hasMaxCapeMinorPerks
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
class CompletionistProtectionTest {
    @Test fun `all custom Perdu families lock unlock and repair every state through actual NPC item input`() {
        for (npc in CompletionistPerduScript.NPCS) for (family in CompletionistProtection.all) {
            val f = Fixture(npc); f.player.inv[0] = InvObj(family.unlocked); f.player.inv[1] = InvObj("obj.trouver_parchment"); f.player.inv[2] = InvObj("obj.coins", 500000)
            f.use(); f.confirm(); assertEquals(family.locked.asRSCM(), f.player.inv[0]!!.id); assertEquals(0, f.player.inv.physicalCount("obj.coins")); assertEquals(0, f.player.inv.physicalCount("obj.trouver_parchment"))
            f.use(); f.confirm(); assertEquals(family.unlocked.asRSCM(), f.player.inv[0]!!.id); assertEquals(475000, f.player.inv.physicalCount("obj.coins")); assertEquals(1, f.player.inv.physicalCount("obj.trouver_parchment"))
            for (state in listOf(family.broken, family.lockedBroken, family.mangled)) {
                f.player.inv.fillNulls(); f.bank.fillNulls(); val original = InvObj(state); f.player.inv[0] = original; f.player.inv[1] = InvObj("obj.coins", 123); f.bank[0] = InvObj("obj.coins", family.fee(original) - 123)
                f.use(); f.confirm(); assertEquals(family.usable(original, family.locked(original)), f.player.inv[0]); assertEquals(0, f.player.inv.physicalCount("obj.coins")); assertEquals(0, f.bank.physicalCount("obj.coins"))
            }
        }
    }

    @Test fun `Perdu rejects insufficient fee missing parchment full refund cancellation stale death logout and movement`() {
        for (mode in 0..8) {
            val f = Fixture(); val family = CompletionistProtection.all.first(); f.player.inv[0] = InvObj(if (mode == 8) family.locked else family.unlocked); f.player.inv[1] = InvObj("obj.trouver_parchment"); f.player.inv[2] = InvObj("obj.coins", 500000)
            when (mode) { 0 -> f.player.inv[2] = InvObj("obj.coins", 499999); 1 -> f.player.inv[1] = null; 8 -> for (slot in 1..27) f.player.inv[slot] = InvObj("obj.shark") }
            f.use(); when (mode) { 3 -> f.player.inv[0] = f.player.inv[0]!!.copy(); 4 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0); 5 -> f.player.pendingLogout = true; 6 -> f.player.loggingOut = true; 7 -> f.player.coords = f.player.coords.translateX(1) }
            val before = f.player.inv.objs.copyOf(); f.confirm(mode != 2); assertArrayEquals(before, f.player.inv.objs); assertEquals(0, f.bank.physicalCount("obj.coins"))
        }
    }

    @Test fun `native death selection application and actual ground rewards cover all families and low deep or protected PvP`() {
        for (family in CompletionistProtection.all) for (locked in listOf(false, true)) for (level in listOf(20, 21)) for (protected in listOf(false, true)) {
            val f = Fixture(); val item = InvObj(if (locked) family.locked else family.unlocked); f.player.worn[1] = item
            val ctx = f.context(true, level); val handling = f.handling(ctx, if (protected) 1 else 0); val before = f.player.worn.objs.copyOf(); val selection = f.drops.selectDrops(f.player, ctx, handling)
            repeat(2) { assertEquals(selection, f.drops.selectDrops(f.player, ctx, handling)); assertArrayEquals(before, f.player.worn.objs) }
            val finish = f.drops.prepareCompletion(ctx); f.drops.applyDrops(f.player, selection, handling, ctx.coords); finish(); finish()
            val expected = if (protected) item else if (level == 20) InvObj(if (locked) family.lockedBroken else family.broken) else if (locked) InvObj(family.mangled) else null
            assertEquals(expected, f.player.inv[0]); assertEquals(if (protected) 0 else if (level == 20) family.repairFee else if (locked) 500000 else CompletionistProtection.DEEP_COINS, f.ground("obj.coins"))
        }
    }

    @Test fun `broken capes cannot consume protection slots or be worn and repeated deaths never pay another repair bounty`() {
        for (family in CompletionistProtection.all) for (state in listOf(family.broken, family.lockedBroken, family.mangled)) {
            val f = Fixture(); f.player.inv[0] = InvObj(state); f.player.inv[1] = InvObj("obj.rune_scimitar"); val ctx = f.context(true, 20); val selection = f.drops.selectDrops(f.player, ctx, f.handling(ctx, 1))
            assertTrue(selection.kept.any { it.id == "obj.rune_scimitar".asRSCM() }); assertTrue(selection.kept.any { it.id == state.asRSCM() }); assertTrue(selection.lostTradeable.isEmpty()); assertEquals(-1, ServerCacheManager.getItem(state.asRSCM())!!.wearpos1)
        }
    }

    @Test fun `PvM locked cape is retained equipped and Dizana PvP extra ammo spills only once even when cape protected`() {
        for (family in CompletionistProtection.all) {
            val f = Fixture(); val item = InvObj(family.locked); f.player.worn[1] = item; val ctx = f.context(false, 0); val handling = f.handling(ctx); val selection = f.drops.selectDrops(f.player, ctx, handling); val finish = f.drops.prepareCompletion(ctx)
            f.drops.applyDrops(f.player, selection, handling, ctx.coords); finish(); finish(); assertEquals(item, f.player.worn[1]); assertTrue(f.player.inv.objs.all { it == null })
        }
        val f = Fixture(); f.player.worn[1] = InvObj("obj.nero_completionist_dizana_locked"); DizanasQuiver.store(f.player, InvObj("obj.rune_arrow", 100)); val ctx = f.context(true, 30); val handling = f.handling(ctx, 1)
        val selection = f.drops.selectDrops(f.player, ctx, handling); assertEquals(100, DizanasQuiver.stored(f.player)!!.count); val finish = f.drops.prepareCompletion(ctx); f.drops.applyDrops(f.player, selection, handling, ctx.coords); finish(); finish(); assertNull(DizanasQuiver.stored(f.player)); assertEquals(100, f.ground("obj.rune_arrow"))
    }

    @Test fun `all wearable variants retain guild and pouch exceptions and broken variants do not`() {
        val f = Fixture(); for (cape in CompletionistRecipes.all.map { it.cape } + listOf("obj.nero_completionist_2011", "obj.nero_completionist_2011_trimmed")) { f.player.worn[1] = InvObj(cape); assertTrue(f.player.hasMaxCapeMinorPerks(), cape) }
        f.player.worn[1] = InvObj("obj.nero_completionist_fire_broken"); assertFalse(f.player.hasMaxCapeMinorPerks())
    }
    private class Fixture(npcName: String = CompletionistPerduScript.NPCS.first()) {
        val bus = EventBus(); val clock = MapClock(100); val registry = ObjRegistry(ZoneUpdateMap()); val repo = ObjRepository(clock, registry)
        val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); coords = CoordGrid(3100, 3500); currentMapClock = 100; processedMapClock = 100; observerUUID = 1001; statMap.setBaseLevel("stat.hitpoints", 99); statMap.setCurrentLevel("stat.hitpoints", 99) }
        val bank = player.invMap.getOrPut("inv.bank"); val npc = Npc(npcName, player.coords).apply { slotId = 2; assignUid() }
        val launcher = ProtectedAccessLauncher(mock(ProtectedAccessContextFactory::class.java).apply { `when`(create()).thenReturn(ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }, getAlignment = { TextAlignment() })) })
        val drops = PlayerDeathDrops(clock, repo, mock(MarketPrices::class.java), GroundItemDropResolver(emptySet()), setOf(CompletionistDeathHook()))
        init { val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache()); with(InvTransactionsScript(PlayerItemStorage(emptySet()))) { scripts.startup() }; with(CompletionistPerduScript()) { scripts.startup() } }
        fun use() { player.clearPendingAction(bus); val producer = NpcUInteractions::class.java.getDeclaredConstructor(EventBus::class.java).apply { isAccessible = true }.newInstance(bus); assertTrue(launcher.launch(player) { producer.interactOp(this, npc, inv, 0, npc.type, ServerCacheManager.getItem(inv[0]!!.id)!!) }) }
        fun confirm(yes: Boolean = true) { ResumePauseButtonHandler().handle(player, ResumePauseButton(CombinedId("component.chatmenu:options".asRSCM(RSCMType.COMPONENT)), if (yes) 1 else 2)) }
        fun context(pvp: Boolean, level: Int) = PlayerDeathContext(player, player.coords, pvp, level, false, false, pvp, false, pvp, 0, if (pvp) Player().apply { observerUUID = 2002 } else null)
        fun handling(ctx: PlayerDeathContext, keep: Int = 0) = PlayerDeathHandling(keep, ctx.killer ?: player, 6000, 0, false, if (ctx.isPvpDeath && ctx.wildernessLevel > 20) UntradeableHandling.COINS else UntradeableHandling.DROP)
        fun ground(symbol: String) = registry.findAll(player.coords).filter { it.type == symbol.asRSCM() }.sumOf { it.count }
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
