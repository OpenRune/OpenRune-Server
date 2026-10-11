package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import net.rsprot.protocol.game.incoming.buttons.If3Button
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.account.character.inv.*
import org.rsmod.api.combat.weapon.WeaponSpeeds
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.net.rsprot.handlers.If3ButtonHandler
import org.rsmod.api.player.bonus.WornBonuses
import org.rsmod.api.player.interact.*
import org.rsmod.api.player.protect.*
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.*
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
internal class FishCrateInteractionTest {
    @Test fun `item use creates cooled crates and packs ten unnoted fish of all six native species`() {
        for (fish in FishCrateScript.fishTypes) {
            val f = Fixture(); f.put(0, FishCrateScript.CAMPHOR); f.put(1, FishCrateScript.COOLER)
            f.use(1, 0); assertEquals(1, f.player.inv.physicalCount(FishCrateScript.EMPTY)); assertEquals(0, f.player.inv.physicalCount(FishCrateScript.COOLER))
            for (slot in 1..10) f.put(slot, fish.fish)
            f.use(10, 0); f.finish(2)
            assertEquals(1, f.player.inv.physicalCount(fish.full), fish.full); assertEquals(0, f.player.inv.physicalCount(fish.fish)); assertEquals(0, f.player.inv.physicalCount(FishCrateScript.EMPTY))
        }
    }

    @Test fun `ordinary Empty returns ten fish and discards the crate outside the bank`() {
        for (fish in FishCrateScript.fishTypes) {
            val f = Fixture(); f.put(0, fish.full); f.op(0)
            assertEquals(10, f.player.inv.physicalCount(fish.fish)); assertEquals(0, f.player.inv.physicalCount(fish.full)); assertEquals(0, f.player.inv.physicalCount(FishCrateScript.CAMPHOR))
        }
    }

    @Test fun `real bankside IF3 Empty returns fish and camphor crate without refunding ice cooler`() {
        for (fish in FishCrateScript.fishTypes) {
            val f = Fixture(); f.put(0, fish.full); f.bank(); f.bankEmpty(0, fish.full)
            assertEquals(10, f.player.inv.physicalCount(fish.fish), fish.full); assertEquals(1, f.player.inv.physicalCount(FishCrateScript.CAMPHOR)); assertEquals(0, f.player.inv.physicalCount(FishCrateScript.COOLER))
            f.bankEmpty(0, fish.full); assertEquals(10, f.player.inv.physicalCount(fish.fish)); assertEquals(1, f.player.inv.physicalCount(FishCrateScript.CAMPHOR))
        }
    }

    @Test fun `full inventory is unchanged and partial crates retain the exact remainder through repeated withdrawals`() {
        val fish = FishCrateScript.fishTypes.last()
        for (bank in listOf(false, true)) {
            val f = Fixture(); f.put(0, fish.full); for (slot in 1..27) f.put(slot, "obj.lobster")
            if (bank) f.bank()
            fun empty() { if (bank) f.bankEmpty(0, f.player.inv[0]!!.let { if (it.id == fish.full.asRSCM()) fish.full else fish.partial }) else f.op(0) }
            empty(); assertEquals(fish.full.asRSCM(), f.player.inv[0]!!.id)
            f.player.inv[1] = null; f.player.inv[2] = null; empty()
            assertEquals(fish.partial.asRSCM(), f.player.inv[0]!!.id); assertEquals(8, f.player.inv[0]!!.vars); assertEquals(2, f.player.inv.physicalCount(fish.fish))
            for (slot in 1..27) f.player.inv[slot] = null
            empty(); assertEquals(8, f.player.inv.physicalCount(fish.fish)); assertEquals(if (bank)1 else 0, f.player.inv.physicalCount(FishCrateScript.CAMPHOR))
        }
    }

    @Test fun `short supplies notes death logout movement and stale items never consume packing materials`() {
        val fish = FishCrateScript.fishTypes.first()
        for (mode in 0..6) {
            val f = Fixture(); f.put(0, FishCrateScript.EMPTY)
            for (slot in 1..10) f.put(slot, fish.fish)
            if (mode == 0) f.player.inv[10] = null
            if (mode == 1) f.player.inv[1] = InvObj(ServerCacheManager.getItem(ServerCacheManager.getItem(fish.fish.asRSCM())!!.certlink)!!, 10)
            f.use(0, 1)
            when (mode) { 2 -> f.player.pendingLogout = true; 3 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0); 4 -> f.player.coords = CoordGrid(3225, 3218); 5 -> f.put(0, FishCrateScript.EMPTY); 6 -> f.put(1, fish.fish) }
            f.finish(2); assertEquals(0, f.player.inv.physicalCount(fish.full)); assertEquals(1, f.player.inv.physicalCount(FishCrateScript.EMPTY))
        }
    }

    @Test fun `camphor crafting checks both tools four nails and level and only awards XP on successful completion`() {
        for (mode in 0..6) {
            val f = Fixture(); f.put(0, "obj.camphor_logs"); f.put(1, "obj.chisel"); f.put(2, "obj.hammer"); f.put(3, "obj.nails_adamant", 4)
            when (mode) { 1 -> f.player.inv[2] = null; 2 -> f.put(3, "obj.nails_adamant", 3); 3 -> f.player.statMap.setCurrentLevel("stat.crafting", 61) }
            f.use(1, 0)
            when (mode) { 4 -> f.player.pendingLogout = true; 5 -> f.put(0, "obj.camphor_logs"); 6 -> f.player.coords = CoordGrid(3225, 3218) }
            f.finish(3)
            assertEquals(if (mode == 0)1 else 0, f.player.inv.physicalCount(FishCrateScript.CAMPHOR)); assertEquals(if (mode == 0)50 else 0, f.player.statMap.getXP("stat.crafting"))
            assertEquals(if (mode == 0)0 else 1, f.player.inv.physicalCount("obj.camphor_logs"))
        }
    }

    @Test fun `partial crate item variables survive native account loading and invalid empty vars yield no free fish`() {
        val fish = FishCrateScript.fishTypes.last(); val f = Fixture()
        CharacterInventoryApplier().apply(f.player, CharacterInventoryData(listOf(CharacterInventoryData.Inventory(1, "inv.inv", "inv.inv", mutableMapOf(0 to CharacterInventoryData.Obj(fish.partial, 1, 7))))))
        f.player.inv = f.player.invMap["inv.inv"]!!
        f.op(0); assertEquals(7, f.player.inv.physicalCount(fish.fish))
        val g = Fixture(); g.player.inv[0] = InvObj(fish.partial, 1, 0); g.op(0); assertEquals(0, g.player.inv.physicalCount(fish.fish)); assertTrue(g.player.inv[0] != null)
    }
    private class Fixture {
        val bus = EventBus(); val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache())
        val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); coords = CoordGrid(3222, 3218); currentMapClock = 100; processedMapClock = 100; for (stat in listOf("stat.hitpoints", "stat.crafting")) { statMap.setBaseLevel(stat, 99); statMap.setCurrentLevel(stat, 99) } }
        val launcher: ProtectedAccessLauncher; val held: HeldInteractions
        init {
            val context = ProtectedAccessContextFactory.empty().copy(getEventBus = { bus })
            val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(context); launcher = ProtectedAccessLauncher(factory)
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) { scripts.startup() }; with(FishCrateScript(XpModifiers(emptySet()))) { scripts.startup() }
            with(BankInvScript(bus, mock(WornBonuses::class.java), mock(WeaponSpeeds::class.java))) { scripts.startup() }
            val ctor = HeldInteractions::class.java.declaredConstructors.single().apply { isAccessible = true }; held = ctor.newInstance(*ctor.parameterTypes.map { if (it == EventBus::class.java)bus else mock(it) }.toTypedArray()) as HeldInteractions
        }
        fun put(slot: Int, symbol: String, n: Int = 1) { player.inv[slot] = InvObj(symbol, n) }
        fun op(slot: Int) { player.ifClose(bus); launcher.launch(player) { held.interact(this, inv, slot, HeldOp.Op1) } }
        fun use(first: Int, second: Int) { player.ifClose(bus); launcher.launch(player) { HeldUInteractions(bus).interact(this, inv, ServerCacheManager.getItem(inv[first]!!.id)!!, first, ServerCacheManager.getItem(inv[second]!!.id)!!, second) } }
        fun finish(n: Int) { repeat(n) { player.currentMapClock++; player.processedMapClock = player.currentMapClock; player.activeCoroutine?.advance() } }
        fun bank() { launcher.launch(player) { ifOpenMainSidePair("interface.bankmain", "interface.bankside"); ifSetEvents("component.bankside:items", inv.indices, IfEvent.Op9) } }
        fun bankEmpty(slot: Int, item: String) { val ctor = If3Button::class.java.declaredConstructors.single { it.parameterCount == 5 }; If3ButtonHandler(bus, launcher).handle(player, ctor.newInstance("component.bankside:items".asRSCM(), slot, item.asRSCM(), 9, null) as If3Button) }
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
