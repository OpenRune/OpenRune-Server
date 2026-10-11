package org.rsmod.content.other.pouches

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.combat.weapon.WeaponSpeeds
import org.rsmod.api.death.*
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.player.bonus.WornBonuses
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.hook.GroundItemDropResolver
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.worn.HeldEquipOp
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.content.other.pouches.lootingbag.*
import org.rsmod.content.other.pouches.runepouch.*
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.*
import org.rsmod.game.obj.Obj
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
internal class PouchDeathIntegrationTest {
    @Test fun `actual death coroutine clears carried bag storage while its death lock is active`() {
        val f = Fixture(); f.player.inv[0] = InvObj(LootingBags.CLOSED); f.player.lootingBag[0] = InvObj("obj.coins", 123); f.player.saveLootingBag()
        f.player.inv[1] = InvObj(RunePouches.DIVINE)
        val c = RunePouchContents.empty(4); c.add(RunePouchRunes.byObj("obj.airrune".asRSCM())!!.compactId, 50); f.player.writeRunePouch(c)
        VarPlayerIntMapSetter.set(f.player, "varbit.emptyondeath_runepouch", 1)
        VarPlayerIntMapSetter.set(f.player, "varbit.inside_wilderness", 1)
        val respawn = CoordGrid(3222, 3218)
        val death = PlayerDeath(MapClock(100), f.drops, PlayerDeathHandlingResolver(emptySet()), emptySet(), setOf(object : PlayerRespawnHook { override fun respawnCoords(player: Player) = respawn }), mock(org.rsmod.api.area.checker.AreaChecker::class.java))
        f.launcher.launch(f.player) { death.death(this) }
        repeat(9) { f.player.currentMapClock++; f.player.processedMapClock = f.player.currentMapClock; f.player.activeCoroutine?.advance() }
        assertEquals(respawn, f.player.coords); assertTrue(f.player.lootingBag.isEmpty()); assertTrue(f.player.readRunePouch(4).isEmpty()); assertNull(f.player.attr[LOOTING_BAG_ATTR])
        assertEquals(123, f.ground.single { it.type == "obj.coins".asRSCM() }.count); assertEquals(50, f.ground.single { it.type == "obj.airrune".asRSCM() }.count)
    }

    @Test fun `native death selection and application preserve PvM runes unless the empty setting is enabled`() {
        for (symbol in RunePouches.ALL) for (empty in listOf(false, true)) {
            val f = Fixture(); f.player.inv[0] = InvObj(symbol); val slots = f.player.runePouchSlots()!!
            val contents = RunePouchContents.empty(slots)
            for ((i, rune) in listOf("obj.airrune", "obj.waterrune", "obj.earthrune", "obj.firerune").take(slots).withIndex()) contents.add(RunePouchRunes.byObj(rune.asRSCM())!!.compactId, i + 11)
            f.player.writeRunePouch(contents); VarPlayerIntMapSetter.set(f.player, "varbit.emptyondeath_runepouch", if (empty) 1 else 0)
            val ctx = f.context(false); val result = f.preview(ctx)
            repeat(2) { assertEquals(result, f.preview(ctx)) }
            assertArrayEquals(contents.counts, f.player.readRunePouch(slots).counts)
            f.apply(ctx, result)
            assertEquals(empty, f.player.readRunePouch(slots).isEmpty(), symbol)
            assertEquals(if (empty) slots else 0, f.ground.count { RunePouchRunes.byObj(it.type) != null })
            if (!empty) assertArrayEquals(contents.counts, f.player.readRunePouch(slots).counts)
        }
    }

    @Test fun `PvP death spills every rune stack and only completion clears all four native slots`() {
        for (symbol in RunePouches.ALL) {
            val f = Fixture(); f.player.inv[0] = InvObj(symbol)
            val c = RunePouchContents.empty(4); for (rune in listOf("obj.airrune", "obj.waterrune", "obj.earthrune", "obj.firerune")) c.add(RunePouchRunes.byObj(rune.asRSCM())!!.compactId, 50)
            f.player.writeRunePouch(c)
            val ctx = f.context(true); val result = f.preview(ctx); assertFalse(f.player.readRunePouch(4).isEmpty())
            f.apply(ctx, result); assertTrue(f.player.readRunePouch(4).isEmpty())
            assertEquals(f.player.runePouchSlots() ?: if (symbol.contains("divine")) 4 else 3, f.ground.count { RunePouchRunes.byObj(it.type) != null })
            assertTrue(f.ground.filter { RunePouchRunes.byObj(it.type) != null }.all { it.count == 50 })
        }
    }

    @Test fun `looting bag actual death drops retain PvM food but deny food and potions to a PvP killer`() {
        for (pvp in listOf(false, true)) {
            val f = Fixture(); f.player.inv[0] = InvObj(LootingBags.CLOSED)
            f.player.lootingBag[0] = InvObj("obj.coins", 123); f.player.lootingBag[1] = InvObj("obj.lobster", 2); f.player.lootingBag[2] = InvObj("obj.4doseprayerrestore", 1); f.player.saveLootingBag()
            val ctx = f.context(pvp, wild = true); val result = f.preview(ctx)
            assertEquals(3, f.player.lootingBag.occupiedSpace()); f.apply(ctx, result)
            assertTrue(f.player.lootingBag.isEmpty()); assertNull(f.player.attr[LOOTING_BAG_ATTR]); assertFalse(f.player.hasLootingBag())
            assertEquals(123, f.ground.single { it.type == "obj.coins".asRSCM() }.count)
            assertEquals(if (pvp) 0 else 1, f.ground.count { it.type == "obj.lobster".asRSCM() })
            assertEquals(if (pvp) 0 else 1, f.ground.count { it.type == "obj.4doseprayerrestore".asRSCM() })
        }
    }

    @Test fun `real destroy confirmation spills public non-supplies in Wilderness and loses contents outside`() {
        for (wild in listOf(false, true)) {
            val f = Fixture(); f.player.inv[0] = InvObj(LootingBags.CLOSED)
            f.player.lootingBag[0] = InvObj("obj.coins", 123); f.player.lootingBag[1] = InvObj("obj.lobster"); f.player.lootingBag[2] = InvObj("obj.4doseprayerrestore"); f.player.saveLootingBag()
            VarPlayerIntMapSetter.set(f.player, "varbit.inside_wilderness", if (wild) 1 else 0)
            f.destroy(false); assertEquals(3, f.player.lootingBag.occupiedSpace()); assertTrue(f.ground.isEmpty())
            f.destroy(true); assertNull(f.player.inv[0]); assertTrue(f.player.lootingBag.isEmpty()); assertNull(f.player.attr[LOOTING_BAG_ATTR])
            assertEquals(if (wild) 1 else 0, f.ground.size); assertTrue(f.ground.all { it.type == "obj.coins".asRSCM() && it.isPublic })
        }
    }

    @Test fun `destroying a duplicate bag keeps shared contents in the carried or banked remaining bag`() {
        for (banked in listOf(false, true)) {
            val f = Fixture(); f.player.inv[0] = InvObj(LootingBags.CLOSED)
            if (banked) f.player.invMap.getOrPut("inv.bank")[0] = InvObj(LootingBags.OPEN) else f.player.inv[1] = InvObj(LootingBags.OPEN)
            f.player.lootingBag[0] = InvObj("obj.coins", 123); f.player.saveLootingBag(); f.destroy(true)
            assertNull(f.player.inv[0]); assertEquals(123, f.player.lootingBag[0]!!.count); assertNotNull(f.player.attr[LOOTING_BAG_ATTR]); assertTrue(f.ground.isEmpty())
        }
    }

    @Test fun `all rune and looting bag Destroy inputs reject stale items departure death logout and cancellation`() {
        for (symbol in RunePouches.ALL + LootingBags.ALL) for (mode in 0..5) {
            val f = Fixture(); f.player.inv[0] = InvObj(symbol)
            val contents = RunePouchContents.empty(4); contents.add(RunePouchRunes.byObj("obj.airrune".asRSCM())!!.compactId, 55); f.player.writeRunePouch(contents)
            f.player.lootingBag[0] = InvObj("obj.coins", 77); f.player.saveLootingBag()
            f.beginDestroy()
            when (mode) { 1 -> f.player.inv[0] = InvObj(symbol); 2 -> f.player.coords = f.player.coords.translateX(1); 3 -> f.player.pendingLogout = true; 4 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0); 5 -> f.player.loggingOut = true }
            f.confirmDestroy(mode != 0)
            assertEquals(1, f.player.inv[0]?.count); assertEquals(55, f.player.readRunePouch(4).counts[0]); assertEquals(77, f.player.lootingBag[0]?.count); assertTrue(f.ground.isEmpty())
        }
    }

    @Test fun `all rune pouch variants spill exactly once through native confirmation and banked copies keep contents`() {
        for (symbol in RunePouches.ALL) for (duplicate in 0..2) {
            val f = Fixture(); f.player.inv[0] = InvObj(symbol)
            if (duplicate == 1) f.player.inv[1] = InvObj(symbol)
            if (duplicate == 2) f.player.invMap.getOrPut("inv.bank")[0] = InvObj(symbol)
            val contents = RunePouchContents.empty(4); for ((i, rune) in listOf("obj.airrune", "obj.waterrune", "obj.earthrune", "obj.firerune").withIndex()) contents.add(RunePouchRunes.byObj(rune.asRSCM())!!.compactId, 10 + i); f.player.writeRunePouch(contents)
            f.destroy(true); f.confirmDestroy(true)
            assertTrue(f.player.inv[0] == null); assertEquals(if (duplicate == 0) 4 else 0, f.ground.size)
            assertEquals(duplicate == 0, f.player.readRunePouch(4).isEmpty())
            if (duplicate == 0) for ((i, rune) in listOf("obj.airrune", "obj.waterrune", "obj.earthrune", "obj.firerune").withIndex()) assertEquals(10 + i, f.ground.single { it.type == rune.asRSCM() }.count)
        }
    }

    @Test fun `looting bag account save and native destroy preserve item vars and clear saved contents once`() {
        val f = Fixture(); f.player.inv[0] = InvObj(LootingBags.OPEN); val item = InvObj("obj.abyssal_whip", 1, Int.MIN_VALUE)
        f.player.lootingBag[0] = item; f.player.saveLootingBag()
        val g = Fixture(); g.player.inv[0] = InvObj(LootingBags.OPEN); g.player.attr.putAllFromPersistence(f.player.attr.toPersistentMap()); g.player.restoreLootingBag(); assertEquals(item, g.player.lootingBag[0])
        VarPlayerIntMapSetter.set(g.player, "varbit.inside_wilderness", 1); g.destroy(true); g.confirmDestroy(true)
        assertEquals(1, g.ground.size); assertEquals(item.vars, g.ground.single().vars); assertTrue(g.player.lootingBag.isEmpty()); assertTrue(g.player.attr[LOOTING_BAG_ATTR] == null)
    }

    private class Fixture {
        val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); observerUUID = 1001; coords = CoordGrid(3100, 3600); currentMapClock = 100; processedMapClock = 100; statMap.setBaseLevel("stat.hitpoints", 99); statMap.setCurrentLevel("stat.hitpoints", 99) }
        val ground = mutableListOf<Obj>(); val bus = EventBus(); val hook = PouchDeathHook()
        val repo = mock(ObjRepository::class.java) { i -> if (i.method.name.startsWith("add") && i.arguments.firstOrNull() is Obj) ground.add(i.arguments[0] as Obj); RETURNS_DEFAULTS.answer(i) }
        val prices = mock(MarketPrices::class.java)
        val drops = PlayerDeathDrops(MapClock(100), repo, prices, GroundItemDropResolver(emptySet()), setOf(hook))
        val launcher: ProtectedAccessLauncher; val held: HeldInteractions
        init {
            val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache())
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) { scripts.startup() }
            val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }, getAlignment = { TextAlignment() }, getCollision = { org.rsmod.routefinder.collision.CollisionFlagMap().apply { allocateIfAbsent(3222, 3218, 0) } }))
            launcher = ProtectedAccessLauncher(factory)
            val bankScript = BankInvScript(bus, mock(WornBonuses::class.java), mock(WeaponSpeeds::class.java))
            with(LootingBagScript(repo, bankScript, prices)) { scripts.startup() }
            with(RunePouchScript(repo, launcher, bankScript)) { scripts.startup() }
            with(PouchDestroyScript()) { scripts.startup() }
            val c = HeldInteractions::class.java.declaredConstructors.single().apply { isAccessible = true }
            val dropCtor = c.parameterTypes[2].declaredConstructors.single().apply { isAccessible = true }
            val drop = dropCtor.newInstance(*dropCtor.parameterTypes.map { if (it == EventBus::class.java) bus else mock(it) }.toTypedArray())
            held = c.newInstance(bus, prices, drop, HeldEquipOp(bus)) as HeldInteractions
        }
        fun context(pvp: Boolean, wild: Boolean = pvp) = PlayerDeathContext(player, player.coords, wild, if (wild) 12 else -1, false, false, false, false, pvp, 0, if (pvp) Player().apply { observerUUID = 2002 } else null)
        fun handling(ctx: PlayerDeathContext) = PlayerDeathHandling(3, ctx.killer ?: player, 6000, 0, false, UntradeableHandling.DROP)
        fun preview(ctx: PlayerDeathContext) = drops.selectDrops(player, ctx, handling(ctx))
        fun apply(ctx: PlayerDeathContext, result: PlayerDeathDrops.DeathDropResult) { val complete = drops.prepareCompletion(ctx); drops.applyDrops(player, result, handling(ctx), ctx.coords); complete() }
        fun beginDestroy() { player.ifClose(bus); launcher.launch(player) { held.interact(this, inv, 0, HeldOp.Op5) } }
        fun confirmDestroy(confirm: Boolean) { player.activeCoroutine?.takeIf { it.isAwaiting(ResumePauseButtonInput::class) }?.resumeWith(ResumePauseButtonInput("component.confirmdestroy:universe", if (confirm) 1 else 0)) }
        fun destroy(confirm: Boolean) { beginDestroy(); confirmDestroy(confirm) }
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
