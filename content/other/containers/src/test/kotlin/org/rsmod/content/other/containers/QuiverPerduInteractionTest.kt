@file:OptIn(org.rsmod.annotations.InternalApi::class)

package org.rsmod.content.other.containers

import dev.openrune.ParamMap
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.death.*
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.obj.plugin.ObjTakePlugin
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.hook.*
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.*
import org.rsmod.api.player.protect.*
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.player.worn.DizanasQuiver
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.inv.*
import org.rsmod.game.obj.Obj
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
internal class QuiverPerduInteractionTest {
    @Test fun `all native Perdu variants lock charged quivers atomically and unlock returns parchment and ninety five percent`() {
        for (npc in QuiverPerduScript.NPCS) {
            val f = Fixture(npc); val original = f.basic(7, 150000); f.player.inv[0] = original; f.player.inv[1] = InvObj(QuiverPerduScript.PARCHMENT); f.player.inv[2] = InvObj("obj.coins", 500000)
            f.use(); f.confirm(); val locked = f.player.inv[0]!!
            assertTrue(QuiverForms.all[0].locked(locked)); assertEquals(original.vars, locked.vars); assertEquals(0, f.player.inv.physicalCount("obj.coins")); assertEquals(0, f.player.inv.physicalCount(QuiverPerduScript.PARCHMENT)); assertEquals(1, f.player.attr[QuiverLocks.KEY]!!["basic"])
            f.use(); f.confirm(); assertEquals(original, f.player.inv[0]); assertEquals(475000, f.player.inv.physicalCount("obj.coins")); assertEquals(1, f.player.inv.physicalCount(QuiverPerduScript.PARCHMENT)); assertEquals(0, f.player.attr[QuiverLocks.KEY]!!["basic"])
        }
    }

    @Test fun `Perdu bank payment and all native broken or mangled repairs retain exact spent charge progress and lock`() {
        for (family in QuiverForms.all) for (symbol in listOf(family.broken, family.lockedBroken, family.mangled)) {
            val f = Fixture(); val original = InvObj(symbol, vars = if (family.chargeable) f.basic(0, 120000).vars else 0); f.player.inv[0] = original
            val fee = family.repairCost(original); f.player.inv[1] = InvObj("obj.coins", 123); f.bank[0] = InvObj("obj.coins", fee - 123)
            f.use(); f.confirm(); val repaired = f.player.inv[0]!!
            assertEquals(family.usable(original, family.locked(original)), repaired); assertEquals(0, f.player.inv.physicalCount("obj.coins")); assertEquals(0, f.bank.physicalCount("obj.coins"))
            assertEquals(if (family.chargeable) DizanasQuiver.Charges(0, 120000) else DizanasQuiver.Charges(0, 0), family.charges(repaired))
        }
    }

    @Test fun `Perdu insufficient fees parchment missing full refund inventory and cancelled or stale inputs roll back`() {
        for (mode in 0..8) {
            val f = Fixture(); val family = QuiverForms.all[0]; f.player.inv[0] = if (mode == 8) InvObj(family.locked) else InvObj(family.unlocked); f.player.inv[1] = InvObj(QuiverPerduScript.PARCHMENT); f.player.inv[2] = InvObj("obj.coins", 500000)
            when (mode) { 0 -> f.player.inv[2] = InvObj("obj.coins", 499999); 1 -> f.player.inv[1] = null; 8 -> for (slot in 1..27) f.player.inv[slot] = InvObj("obj.lobster") }
            f.use()
            when (mode) { 3 -> f.player.inv[0] = f.player.inv[0]!!.copy(); 4 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0); 5 -> f.player.pendingLogout = true; 6 -> f.player.loggingOut = true; 7 -> f.player.coords = f.player.coords.translateX(1) }
            val snapshot = f.player.inv.objs.copyOf(); f.confirm(mode != 2); assertArrayEquals(snapshot, f.player.inv.objs); assertNull(f.player.attr[QuiverLocks.KEY])
        }
    }

    @Test fun `native PvP death preview is pure and below twenty returns broken quiver but spills ammo charges and exact repair coins once`() {
        val f = Fixture(); val family = QuiverForms.all[0]; f.player.worn[1] = f.basic(7, 150000); f.ammo(); org.rsmod.api.player.vars.VarPlayerIntMapSetter.set(f.player, "varbit.worn_items_dizanas_quiver_charged", 1); val ctx = f.context(true, 20); val handling = f.handling(ctx); val result = f.drops.selectDrops(f.player, ctx, handling)
        repeat(2) { assertEquals(result, f.drops.selectDrops(f.player, ctx, handling)); assertEquals(100, DizanasQuiver.stored(f.player)!!.count); assertTrue(f.player.attr.toPersistentMap().isEmpty()) }
        assertEquals(family.broken.asRSCM(), result.kept.single().id); assertEquals(DizanasQuiver.Charges(0, 149993), family.charges(result.kept.single())); assertEquals(0L, result.coinsForKiller)
        val finish = f.drops.prepareCompletion(ctx); f.drops.applyDrops(f.player, result, handling, ctx.coords); finish(); finish()
        assertNull(DizanasQuiver.stored(f.player)); assertEquals(0, f.player.vars["varbit.worn_items_dizanas_quiver_charged"]); assertEquals(270000, f.groundCount("obj.coins")); assertEquals(7, f.groundCount(DizanasQuiverScript.SPLINTER)); assertEquals(100, f.groundCount("obj.rune_arrow")); assertEquals(family.broken.asRSCM(), f.player.inv[0]!!.id)
    }

    @Test fun `all quiver families destroy unlocked forms deep and keep permanent locked broken or mangled forms with exact fees`() {
        for (family in QuiverForms.all) for (locked in listOf(false, true)) for (level in listOf(20, 21)) {
            val f = Fixture(); val base = if (family.chargeable) f.basic(2, 100000) else InvObj(family.unlocked); f.player.inv[0] = family.usable(base, locked); val ctx = f.context(true, level); val handling = f.handling(ctx)
            val result = f.drops.selectDrops(f.player, ctx, handling); val returned = family.lost(f.player.inv[0]!!, level > 20)
            assertEquals(if (returned == null) emptyList<InvObj>() else listOf(returned), result.kept)
            val fee = if (level == 20) family.repairFee else if (locked) 500000 else family.deepCoins
            assertEquals(fee, result.lostTradeable.filter { it.id == "obj.coins".asRSCM() }.sumOf { it.count }); assertTrue(result.lostTradeable.none { QuiverForms.family(it) != null })
            val finish = f.drops.prepareCompletion(ctx); f.drops.applyDrops(f.player, result, handling, ctx.coords); finish(); assertEquals(fee, f.groundCount("obj.coins")); if (locked) assertEquals(1, f.player.attr[QuiverLocks.KEY]!![family.key])
        }
    }

    @Test fun `protected PvP quiver keeps its charges while extra ammo is lost and bank-only quivers are unaffected`() {
        val f = Fixture(); val item = f.basic(7, 150000); f.player.inv[0] = item; f.ammo(); val ctx = f.context(true, 30); val handling = f.handling(ctx, keep = 1); val result = f.drops.selectDrops(f.player, ctx, handling)
        assertEquals(listOf(item), result.kept); assertEquals(listOf(InvObj("obj.rune_arrow", 100)), result.lostTradeable)
        val finish = f.drops.prepareCompletion(ctx); f.drops.applyDrops(f.player, result, handling, ctx.coords); finish(); assertEquals(item, f.player.inv[0]); assertNull(DizanasQuiver.stored(f.player))
        val g = Fixture(); g.bank[0] = item; g.ammo(); val other = g.context(true, 30); val selection = g.drops.selectDrops(g.player, other, g.handling(other)); val complete = g.drops.prepareCompletion(other); g.drops.applyDrops(g.player, selection, g.handling(other), other.coords); complete(); assertEquals(item, g.bank[0]); assertEquals(100, DizanasQuiver.stored(g.player)!!.count); assertEquals(0, g.registry.count())
    }

    @Test fun `candidate locked PvM metadata keeps quiver unbroken without consuming protected slots and unrepaired PvP items cannot print fees`() {
        val f = Fixture(); val family = QuiverForms.all[0]; val item = family.usable(f.basic(7, 150000), true); f.player.inv[0] = item
        val other = listOf("obj.abyssal_whip", "obj.dragon_warhammer", "obj.rune_longsword"); other.forEachIndexed { index, symbol -> f.player.inv[index + 1] = InvObj(symbol) }; f.ammo()
        val ctx = f.context(false, -1); val handling = f.handling(ctx, keep = 3); val result = f.drops.selectDrops(f.player, ctx, handling); assertEquals(4, result.kept.size); assertTrue(result.kept.contains(item)); assertTrue(result.lostTradeable.isEmpty()); assertTrue(result.lostUntradeable.isEmpty())
        val finish = f.drops.prepareCompletion(ctx); f.drops.applyDrops(f.player, result, handling, ctx.coords); finish(); assertEquals(100, DizanasQuiver.stored(f.player)!!.count)
        for (family in QuiverForms.all) for (symbol in listOf(family.broken, family.lockedBroken, family.mangled)) {
            val g = Fixture(); val item = InvObj(symbol); g.player.inv[0] = item; val pvp = g.context(true, 30); val alreadyBroken = g.drops.selectDrops(g.player, pvp, g.handling(pvp)); assertEquals(if (family.locked(item)) listOf(item) else emptyList<InvObj>(), alreadyBroken.kept); assertEquals(if (family.locked(item)) emptyList<InvObj>() else listOf(InvObj("obj.coins", family.deepCoins)), alreadyBroken.lostTradeable); assertEquals(0L, alreadyBroken.coinsForKiller)
        }
    }

    @Test fun `native PvM death keeps each locked usable quiver equipped but ordinary broken forms still reach the grave`() {
        for (family in QuiverForms.all) {
            val f = Fixture(); val item = family.usable(if (family.chargeable) f.basic(7, 150000) else InvObj(family.unlocked), true); f.player.worn[1] = item; f.ammo()
            val ctx = f.context(false, -1); val selection = f.drops.selectDrops(f.player, ctx, f.handling(ctx)); val complete = f.drops.prepareCompletion(ctx)
            f.drops.applyDrops(f.player, selection, f.handling(ctx), ctx.coords); complete(); complete()
            assertEquals(item, f.player.worn[1]); assertEquals(0, f.player.inv.occupiedSpace()); assertEquals(1, f.player.vars["varbit.worn_items_dizanas_quiver_charged"]); assertEquals(100, DizanasQuiver.stored(f.player)!!.count); assertEquals(0, f.registry.count())
            val g = Fixture(); val broken = InvObj(family.broken); g.player.inv[0] = broken; val death = g.context(false, -1); val drops = g.drops.selectDrops(g.player, death, g.handling(death))
            assertTrue(drops.kept.isEmpty()); assertEquals(listOf(broken), drops.lostUntradeable); g.drops.applyDrops(g.player, drops, g.handling(death), death.coords); assertEquals(1, g.registry.count())
        }
    }

    @Test fun `paid lock count persists and native reacquisition restores only the missing paid copy`() {
        val f = Fixture(); val family = QuiverForms.all[0]; f.player.attr[QuiverLocks.KEY] = mapOf("basic" to 1)
        val restored = Fixture(); restored.player.attr.putAllFromPersistence(f.player.attr.toPersistentMap()); restored.player.inv[0] = InvObj(family.unlocked); restored.player.inv[1] = InvObj(family.unlocked)
        QuiverLocks.restore(restored.player); assertTrue(family.locked(restored.player.inv[0]!!)); assertFalse(family.locked(restored.player.inv[1]!!))
        val g = Fixture(); g.player.attr.putAllFromPersistence(f.player.attr.toPersistentMap()); val loot = Obj.fromOwner(g.player, g.player.coords, InvObj(family.unlocked)); assertTrue(g.repo.add(loot, 600)); g.take(loot); assertTrue(family.locked(g.player.inv[0]!!)); assertEquals(0, g.registry.count())
    }

    private class Fixture(npc: String = QuiverPerduScript.NPCS.first()) {
        val bus = EventBus(); val clock = MapClock(100); val registry = ObjRegistry(ZoneUpdateMap()); val repo = ObjRepository(clock, registry)
        val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); coords = CoordGrid(3100, 3500); currentMapClock = 100; processedMapClock = 100; observerUUID = 1001; statMap.setBaseLevel("stat.hitpoints", 99); statMap.setCurrentLevel("stat.hitpoints", 99) }
        val bank = player.invMap.getOrPut("inv.bank"); val npc = Npc(npc, player.coords).apply { slotId = 2; assignUid() }; val launcher: ProtectedAccessLauncher
        val drops = PlayerDeathDrops(clock, repo, mock(MarketPrices::class.java), GroundItemDropResolver(emptySet()), setOf(QuiverDeathHook()))
        init {
            // Apply exactly the new server-only pack metadata to the otherwise accepted test cache.
            for (family in QuiverForms.all) for (symbol in family.symbols) {
                val type = ServerCacheManager.getItem(symbol.asRSCM())!!; type.tradeable = false
                val kept = family.locked(InvObj(symbol)) && !family.broken(InvObj(symbol)); type.paramsRaw = (type.paramsRaw.orEmpty() + ("param.death_always_kept".asRSCM() to if (kept) 1 else 0)).toMutableMap(); type.paramMap = ParamMap(type.paramsRaw!!)
            }
            val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }, getAlignment = { TextAlignment() })); launcher = ProtectedAccessLauncher(factory)
            val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache()); with(InvTransactionsScript(PlayerItemStorage(emptySet()))) { scripts.startup() }; with(QuiverPerduScript()) { scripts.startup() }; with(ObjTakePlugin(repo, PlayerObjTakeValidator(emptySet()), setOf(QuiverPickupLocks()))) { scripts.startup() }
        }
        fun basic(count: Int, progress: Int) = DizanasQuiver.withCharges(InvObj("obj.dizanas_quiver_uncharged"), DizanasQuiver.Charges(count, progress))
        fun use() { player.ifClose(bus); val ctor = NpcUInteractions::class.java.getDeclaredConstructor(EventBus::class.java).apply { isAccessible = true }; val producer = ctor.newInstance(bus); assertTrue(launcher.launch(player) { producer.interactOp(this, npc, inv, 0, npc.type, ServerCacheManager.getItem(inv[0]!!.id)!!) }) }
        fun confirm(yes: Boolean = true) { player.activeCoroutine?.resumeWith(ResumePauseButtonInput("component.chatmenu:options", if (yes) 1 else 2)) }
        fun context(pvp: Boolean, level: Int) = PlayerDeathContext(player, player.coords, pvp, level, false, false, pvp, false, pvp, 0, if (pvp) Player().apply { observerUUID = 2002 } else null)
        fun handling(ctx: PlayerDeathContext, keep: Int = 0) = PlayerDeathHandling(keep, ctx.killer ?: player, 6000, 0, false, if (ctx.isPvpDeath) UntradeableHandling.COINS else UntradeableHandling.DROP)
        fun ammo() = DizanasQuiver.store(player, InvObj("obj.rune_arrow", 100))
        fun groundCount(symbol: String) = registry.findAll(player.coords).filter { it.type == symbol.asRSCM() }.sumOf { it.count }
        fun take(obj: Obj) { val producer = ObjInteractions(bus); producer.interact(player, obj, InteractionOp.Op3); launcher.launch(player) { assertTrue(bus.publish(this, checkNotNull(producer.opTrigger(obj, InteractionOp.Op3)))) } }
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
