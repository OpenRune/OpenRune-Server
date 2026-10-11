@file:OptIn(org.rsmod.annotations.InternalApi::class)
package org.rsmod.content.skills.runecrafting.essencepouch

import dev.openrune.ServerCacheManager
import dev.openrune.util.Wearpos
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.interact.*
import org.rsmod.api.player.protect.*
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.random.GameRandom
import org.rsmod.content.skills.runecrafting.colossalPouchDegradeThreshold
import org.rsmod.content.skills.runecrafting.mediumPouchDegradeThreshold
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
internal class EssencePouchInteractionTest {
    @Test fun `native Fill tops up partial contents instead of emptying them and Empty uses only free slots`() {
        val f = Fixture(); f.put(1, EssencePouch.PURE_ESSENCE); f.op(HeldOp.Op1); f.put(1, EssencePouch.PURE_ESSENCE); f.op(HeldOp.Op1)
        assertEquals(2, f.stored()); assertEquals(0, f.player.inv.physicalCount(EssencePouch.PURE_ESSENCE))
        for (i in 1..27)f.put(i, "obj.lobster"); f.op(HeldOp.Op3); assertEquals(2, f.stored())
        f.player.inv[1] = null; f.op(HeldOp.Op3); assertEquals(1, f.stored()); assertEquals(1, f.player.inv.physicalCount(EssencePouch.PURE_ESSENCE))
    }

    @Test fun `all native pouch tiers and accepted essence variants respect base level and exact capacities`() {
        for (tier in EssencePouch.Tier.entries) for (essence in EssencePouch.fillableEssences) { val f = Fixture(tier); f.player.worn[Wearpos.Back.slot] = InvObj("obj.skillcape_runecrafting"); for (i in 1..27) f.put(i, essence); f.op(HeldOp.Op1); assertEquals(minOf(27, tier.maxCapacity), f.stored()); assertEquals(27 - f.stored(), f.player.inv.physicalCount(essence)) }
        val f = Fixture(EssencePouch.Tier.Medium); f.player.statMap.setBaseLevel("stat.runecrafting", 24); f.player.statMap.setCurrentLevel("stat.runecrafting", 99); f.put(1, EssencePouch.PURE_ESSENCE); f.op(HeldOp.Op1); assertEquals(0, f.stored()); assertEquals(1, f.player.inv.physicalCount(EssencePouch.PURE_ESSENCE))
    }

    @Test fun `virtual altar consumption cannot access banked or absent pouch counters`() {
        val f = Fixture(); f.seed(EssencePouch.Tier.Small, 2); f.seed(EssencePouch.Tier.Large, 9); f.player.invMap.getOrPut("inv.bank")[0] = InvObj(EssencePouch.Tier.Large.intactItem)
        assertEquals(2, f.player.inv.count(EssencePouch.PURE_ESSENCE)); f.run { invDel(inv, EssencePouch.PURE_ESSENCE, 2) }; assertEquals(0, f.stored()); assertEquals(9, EssencePouch.storedAmount(f.player, EssencePouch.Tier.Large))
    }

    @Test fun `cape and eligible redwood lantern prevent further decay without repairing existing damage`() {
        for (cape in listOf("obj.skillcape_runecrafting", "obj.skillcape_max_worn", "obj.skillcape_max_dizanas", "obj.skillcape_max_assembler")) { val f = Fixture(EssencePouch.Tier.Medium); f.player.mediumPouchDegradeThreshold = 400; f.put(0, f.tier.degradedItem!!); f.player.worn[Wearpos.Back.slot] = InvObj(cape); for (i in 1..6) f.put(i, EssencePouch.PURE_ESSENCE); f.op(HeldOp.Op1); assertEquals(3, f.stored()); assertEquals(400, f.player.mediumPouchDegradeThreshold) }
        val f = Fixture(); f.player.worn[Wearpos.LeftHand.slot] = InvObj("obj.abyssal_lantern_redwood"); f.player.statMap.setBaseLevel("stat.firemaking", 89); assertFalse(EssencePouch.playerHasDegradeProtection(f.player)); f.player.statMap.setBaseLevel("stat.firemaking", 90); assertTrue(EssencePouch.playerHasDegradeProtection(f.player))
    }

    @Test fun `each regular deposited essence rolls degradation independently`() {
        val f = Fixture(EssencePouch.Tier.Medium); `when`(f.random.of(0, 3)).thenReturn(0, 1, 2, 3, 0, 1); for (i in 1..6)f.put(i, EssencePouch.PURE_ESSENCE); f.op(HeldOp.Op1)
        assertEquals(7, f.player.mediumPouchDegradeThreshold); verify(f.random, times(6)).of(0, 3)
    }

    @Test fun `colossal degradation returns all excess only when it fits and never stores beyond reduced capacity`() {
        for (full in listOf(false, true)) { val f = Fixture(EssencePouch.Tier.Colossal); f.seed(f.tier, 39); f.player.colossalPouchDegradeThreshold = 319; f.put(1, EssencePouch.PURE_ESSENCE); if (full)for (i in 2..27)f.put(i, "obj.lobster"); f.op(HeldOp.Op1); assertEquals(35, f.stored()); assertEquals(if (full)0 else 5, f.player.inv.physicalCount(EssencePouch.PURE_ESSENCE)); assertEquals(320, f.player.colossalPouchDegradeThreshold) }
        val f = Fixture(EssencePouch.Tier.Colossal); f.player.statMap.setBaseLevel("stat.runecrafting", 25); f.player.colossalPouchDegradeThreshold = 64; assertEquals(7, EssencePouch.capacity(f.player, f.tier))
    }

    @Test fun `native Dark mage Repair restores every tier including banked colossal and preserves essence`() {
        val f = Fixture(EssencePouch.Tier.Medium); f.seed(f.tier, 3); f.player.mediumPouchDegradeThreshold = 450; f.put(0, f.tier.degradedItem!!); f.seed(EssencePouch.Tier.Colossal, 25); f.player.colossalPouchDegradeThreshold = 600; f.player.invMap.getOrPut("inv.bank")[0] = InvObj(EssencePouch.Tier.Colossal.degradedItem!!)
        f.talk("npc.rcu_zammy_mage2", InteractionOp.Op3); assertEquals(3, f.stored()); assertEquals(25, EssencePouch.storedAmount(f.player, EssencePouch.Tier.Colossal)); assertEquals(0, f.player.mediumPouchDegradeThreshold); assertEquals(0, f.player.colossalPouchDegradeThreshold); assertTrue(f.player.invMap["inv.bank"]!!.contains(EssencePouch.Tier.Colossal.intactItem))
    }

    @Test fun `native abyssal needle crafting consumes all ingredients atomically after three ticks`() {
        val f = Fixture(); f.recipe(); f.use(4, 0); assertEquals(0, f.player.statMap.getXP("stat.crafting")); f.finish(); assertTrue(f.player.inv.contains(EssencePouch.Tier.Colossal.intactItem)); assertEquals(1, f.player.inv.filterNotNull().size); assertEquals(1000, f.player.statMap.getXP("stat.crafting"))
    }

    @Test fun `interrupted crafting or banked colossal never consumes any surviving ingredients or gives XP`() {
        for (mode in 0..5) { val f = Fixture(); f.recipe(); if (mode == 5)f.player.invMap.getOrPut("inv.bank")[0] = InvObj(EssencePouch.Tier.Colossal.intactItem); f.use(4, 0); when (mode) { 0 -> f.player.inv[2] = null; 1 -> f.player.inv[2] = InvObj(EssencePouch.Tier.Large.intactItem); 2 -> f.player.pendingLogout = true; 3 -> f.player.coords = f.player.coords.translateX(1); 4 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0) }; f.finish(); assertFalse(f.player.inv.contains(EssencePouch.Tier.Colossal.intactItem)); assertTrue(f.player.inv.contains("obj.abyssal_needle")); assertTrue(f.player.inv.contains(EssencePouch.Tier.Small.intactItem)); assertEquals(0, f.player.statMap.getXP("stat.crafting")) }
    }

    @Test fun `death previews preserve pouch contents and actual lost-pouch death clears only carried counters`() {
        val f = Fixture(); f.seed(f.tier, 3); f.seed(EssencePouch.Tier.Large, 9); f.player.invMap.getOrPut("inv.bank")[0] = InvObj(EssencePouch.Tier.Large.intactItem)
        val ctx = org.rsmod.api.death.PlayerDeathContext(f.player, f.player.coords, false, -1, false, false, false, false, false, 0, null)
        val hook = EssencePouchDeathHook(); val result = org.rsmod.api.death.PlayerDeathDrops.DeathDropResult(emptyList(), emptyList(), listOf(f.player.inv[0]!!), emptyList(), 0)
        repeat(2) { hook.resolve(ctx, result); assertEquals(3, f.stored()) }; val complete = hook.prepareCompletion(ctx); f.player.inv[0] = null; complete(); complete(); assertEquals(0, f.stored()); assertEquals(9, EssencePouch.storedAmount(f.player, EssencePouch.Tier.Large))
    }

    @Test fun `actual manual Drop clears carried essence only after successful ground placement`() {
        val f = Fixture(); f.seed(f.tier, 3); f.op(HeldOp.Op5); assertEquals(0, f.stored()); assertNull(f.player.inv[0]); f.put(0, f.tier.intactItem); assertEquals(0, f.stored())
        val g = Fixture(); g.seed(g.tier, 3); g.dropAccepted = false; g.op(HeldOp.Op5); assertEquals(3, g.stored()); assertNotNull(g.player.inv[0])
    }

    @Test fun `real native death selection and application clear dropped contents but preserve kept and banked tiers`() {
        for (kept in listOf(false, true)) {
            val f = Fixture(); f.seed(f.tier, 3); f.seed(EssencePouch.Tier.Large, 9); f.player.invMap.getOrPut("inv.bank")[0] = InvObj(EssencePouch.Tier.Large.intactItem)
            val ctx = org.rsmod.api.death.PlayerDeathContext(f.player, f.player.coords, false, -1, false, false, false, false, false, 0, null)
            val drops = org.rsmod.api.death.PlayerDeathDrops(org.rsmod.game.MapClock(100), mock(org.rsmod.api.repo.obj.ObjRepository::class.java), mock(org.rsmod.api.market.MarketPrices::class.java), org.rsmod.api.player.hook.GroundItemDropResolver(emptySet()), setOf(EssencePouchDeathHook()))
            val handling = org.rsmod.api.death.PlayerDeathHandling(if (kept)3 else 0, f.player, 6000, 0, false, org.rsmod.api.death.UntradeableHandling.DROP)
            val selected = drops.selectDrops(f.player, ctx, handling); repeat(2) { assertEquals(selected, drops.selectDrops(f.player, ctx, handling)); assertEquals(3, f.stored()) }
            val complete = drops.prepareCompletion(ctx); drops.applyDrops(f.player, selected, handling, f.player.coords); complete(); complete()
            assertEquals(if (kept)3 else 0, f.stored()); assertEquals(9, EssencePouch.storedAmount(f.player, EssencePouch.Tier.Large))
        }
    }
    private class Fixture(val tier: EssencePouch.Tier = EssencePouch.Tier.Small) {
        var dropAccepted = true
        val bus = EventBus(); val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache()); val random = mock(GameRandom::class.java)
        val player = Player().apply { inv = Inventory.create("inv.inv").also { it.owner = this }; worn = Inventory.create("inv.worn"); coords = CoordGrid(3000, 3000); observerUUID = 1001; currentMapClock = 100; processedMapClock = 100; for (stat in listOf("hitpoints", "runecrafting", "crafting")) { statMap.setBaseLevel("stat.$stat", 99); statMap.setCurrentLevel("stat.$stat", 99) }; inv[0] = InvObj(tier.intactItem) }
        val launcher: ProtectedAccessLauncher; val held: HeldInteractions
        init { val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }, getAlignment = { TextAlignment() }, getRandom = { random })); launcher = ProtectedAccessLauncher(factory)
            with(InvTransactionsScript(PlayerItemStorage(setOf(EssencePouchStorageHook())))) { scripts.startup() }; with(EssencePouchScript()) { scripts.startup() }; with(ColossalPouchCraftEvents()) { scripts.startup() }; with(PouchRepairEvents()) { scripts.startup() }
            val ctor = HeldInteractions::class.java.declaredConstructors.single().apply { isAccessible = true }; val dc = ctor.parameterTypes[2].declaredConstructors.single().apply { isAccessible = true }; val drop = dc.newInstance(*dc.parameterTypes.map { when { it == EventBus::class.java -> bus; it.simpleName == "GroundItemDropResolver" -> org.rsmod.api.player.hook.GroundItemDropResolver(emptySet()); it.simpleName == "ObjRepository" -> mock(it) { call -> if (call.method.name == "add" && call.method.returnType == Boolean::class.javaPrimitiveType)dropAccepted else RETURNS_DEFAULTS.answer(call) }; else -> mock(it) } }.toTypedArray()); held = ctor.newInstance(bus, mock(ctor.parameterTypes[1]), drop, mock(ctor.parameterTypes[3]))as HeldInteractions
        }
        fun put(slot: Int, item: String) { player.inv[slot] = InvObj(item) }
        fun seed(t: EssencePouch.Tier, n: Int) { EssencePouch.depositUpTo(player, t, n); EssencePouch.setStoredEssenceType(player, t, EssencePouch.PURE_ESSENCE) }
        fun stored() = EssencePouch.storedAmount(player, tier)
        fun run(block: suspend ProtectedAccess.() -> Unit) { player.ifClose(bus); launcher.launch(player, block = block) }
        fun op(op: HeldOp) = run { held.interact(this, inv, 0, op) }
        fun use(first: Int, second: Int) = run { HeldUInteractions(bus).interact(this, inv, ServerCacheManager.getItem(inv[first]!!.id)!!, first, ServerCacheManager.getItem(inv[second]!!.id)!!, second) }
        fun recipe() { for ((i, t)in EssencePouch.craftablePouchTiers.withIndex())put(i, t.intactItem); put(4, "obj.abyssal_needle") }
        fun finish() { repeat(3) { player.currentMapClock++; player.processedMapClock = player.currentMapClock; player.activeCoroutine?.advance() } }
        fun talk(symbol: String, op: InteractionOp) { val npc = Npc(symbol, player.coords.translateX(1)).apply { slotId = 2; assignUid() }; val producer = NpcInteractions(bus); producer.interact(player, npc, op); run { bus.publish(this, checkNotNull(producer.opTrigger(player, npc, op))) } }
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
