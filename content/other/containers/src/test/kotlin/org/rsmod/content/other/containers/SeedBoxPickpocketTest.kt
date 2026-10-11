package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.events.skilling.PickpocketSuccessEvent
import org.rsmod.api.player.interact.NpcInteractions
import org.rsmod.api.player.protect.*
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onEvent
import org.rsmod.content.skills.thieving.ThievingScript
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.*
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.inv.*
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@OptIn(org.rsmod.annotations.InternalApi::class)
@ResourceLock("ServerCacheManager")
internal class SeedBoxPickpocketTest {
    @Test fun `actual Master Farmer NPC input stores seed loot with a full inventory for both farmer aliases`() {
        for (npc in listOf("npc.master_farmer_1", "npc.martin_the_master_farmer")) {
            val f = Fixture(npc); f.pick(); f.advance()
            assertEquals(1, f.count()); assertEquals(0, f.player.inv.physicalCount("obj.potato_seed")); assertEquals(28, f.player.inv.occupiedSpace()); assertEquals(43, f.player.statMap.getXP("stat.thieving")); assertEquals(1, f.rewards.size)
        }
    }

    @Test fun `closed full and seventh-seed boxes refuse rewards while an existing physical stack still fits`() {
        for (mode in 0..3) {
            val f = Fixture()
            when (mode) { 0 -> f.player.inv[0] = InvObj("obj.seed_box"); 1 -> f.store.update(f.player, SeedBox.spec, f.seed, Int.MAX_VALUE); 2 -> for (item in listOf("obj.guam_seed", "obj.ranarr_seed", "obj.watermelon_seed", "obj.onion_seed", "obj.seaweed_seed", "obj.mushroom_seed")) f.store.update(f.player, SeedBox.spec, item.asRSCM(), 1); 3 -> { f.player.inv[0] = InvObj("obj.seed_box"); f.player.inv[1] = InvObj("obj.potato_seed", 3) } }
            f.pick(); f.advance(); assertEquals(if (mode == 3) 43 else 0, f.player.statMap.getXP("stat.thieving")); assertEquals(if (mode == 3) 1 else 0, f.rewards.size)
            if (mode == 3) assertEquals(4, f.player.inv.physicalCount("obj.potato_seed")) else assertEquals(if (mode == 1) Int.MAX_VALUE else 0, f.count())
        }
    }

    @Test fun `delayed NPC pickpocket revalidates ownership capacity death logout and level before credit`() {
        for (mode in 0..5) {
            val f = Fixture(); f.pick()
            when (mode) { 0 -> f.player.inv[0] = InvObj("obj.lobster"); 1 -> f.store.update(f.player, SeedBox.spec, f.seed, Int.MAX_VALUE); 2 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0); 3 -> f.player.pendingLogout = true; 4 -> f.player.loggingOut = true; 5 -> f.player.statMap.setCurrentLevel("stat.thieving", 1) }
            f.advance(); assertEquals(if (mode == 1) Int.MAX_VALUE else 0, f.count()); assertEquals(0, f.player.statMap.getXP("stat.thieving")); assertTrue(f.rewards.isEmpty())
        }
    }
    private class Fixture(symbol: String = "npc.master_farmer_1") {
        val bus = EventBus(); val store = ContainerStore(); val seed = "obj.potato_seed".asRSCM()
        val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); inv[0] = InvObj("obj.seed_box_open"); for (slot in 1..27) inv[slot] = InvObj("obj.lobster"); coords = CoordGrid(3079, 3250); currentMapClock = 100; processedMapClock = 100; for (stat in listOf("hitpoints", "thieving")) { statMap.setBaseLevel("stat.$stat", 99); statMap.setCurrentLevel("stat.$stat", 99) } }
        val npc = Npc(symbol, player.coords.translateX(1)).apply { slotId = 2; assignUid() }
        val rewards = mutableListOf<PickpocketSuccessEvent>(); val launcher: ProtectedAccessLauncher
        init {
            val random = object : GameRandom { override fun of(maxExclusive: Int) = 0; override fun of(minInclusive: Int, maxInclusive: Int) = minInclusive; override fun randomDouble() = 0.0 }
            val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }, getRandom = { random })); launcher = ProtectedAccessLauncher(factory)
            val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache())
            with(InvTransactionsScript(PlayerItemStorage(setOf(ContainerStorageHook(store))))) { scripts.startup() }
            with(ThievingScript(mock(LocRepository::class.java), mock(NpcRepository::class.java), mock(AiPlayerInteractions::class.java))) { scripts.startup() }
            scripts.onEvent<PickpocketSuccessEvent> { assertTrue(player.statMap.getXP("stat.thieving") > 0); rewards += this }
        }
        fun count() = store.count(player, SeedBox.spec, seed)
        fun pick() { val producer = NpcInteractions(bus); producer.interact(player, npc, InteractionOp.Op3); launcher.launch(player) { assertTrue(bus.publish(this, checkNotNull(producer.opTrigger(player, npc, InteractionOp.Op3)))) } }
        fun advance() { repeat(2) { player.currentMapClock++; player.processedMapClock = player.currentMapClock; player.activeCoroutine?.advance() } }
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
