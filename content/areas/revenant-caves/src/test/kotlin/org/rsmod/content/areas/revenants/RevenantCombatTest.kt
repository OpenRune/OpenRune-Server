package org.rsmod.content.areas.revenants

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.bosses.runtime.*
import org.rsmod.api.combat.formulas.*
import org.rsmod.api.npc.access.*
import org.rsmod.api.npc.hit.modifier.StandardNpcHitModifier
import org.rsmod.api.npc.hit.processor.StandardNpcHitProcessor
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.hit.modifier.StandardPlayerHitModifier
import org.rsmod.api.player.protect.*
import org.rsmod.api.random.DefaultGameRandom
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.*
import org.rsmod.game.entity.npc.*
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitType
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.inv.*
import org.rsmod.game.queue.*
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap
@ResourceLock("ServerCacheManager")
@OptIn(org.rsmod.annotations.InternalApi::class)
internal class RevenantCombatTest {
    @Test fun `native approach handler launches magic for every ordinary species`() {
        for (species in RevenantSpecies.entries) {
            val f = Fixture(species.npcType); f.attack("attack"); f.tick(3)
            assertEquals(8, f.npc.attackRange); assertEquals(HitType.Magic, f.hits().single().type)
            assertTrue(f.hits().single().damage in 0..species.maxHit)
            verify(f.info).setSequence(f.npc.param(org.rsmod.api.config.refs.params.attack_anim).id, 0)
        }
    }

    @Test fun `normal magic hits the real health consumer and limits stacked targets to ten`() {
        val f = Fixture(RevenantSpecies.Dragon.npcType)
        for (slot in 2..12) { val p = Player(); p.worn = Inventory(ServerCacheManager.getInventory("inv.worn".asRSCM())!!, arrayOfNulls(14)); p.currentMapClock = 100; p.processedMapClock = 100; p.coords = f.player.coords; p.statMap.setBaseLevel("stat.hitpoints", 99); p.statMap.setCurrentLevel("stat.hitpoints", 99); f.players[slot] = p }
        f.attack("attack"); f.tick(3)
        assertEquals(10, f.players.count { it.queueList.contains("queue.hit") })
        val hit = f.hits().single(); val context = ProtectedAccessContextFactory.empty().copy(getEventBus = { f.bus }, getNpcList = { f.npcs }, getPlayerList = { f.players }, getRandom = { DefaultGameRandom(1) })
        val access = ProtectedAccess(f.player, GameCoroutine(), context)
        with(org.rsmod.api.player.hit.processor.StandardPlayerHitProcessor) { access.process(hit) }
        assertEquals(99 - hit.damage, f.player.statMap.getCurrentLevel("stat.hitpoints").toInt())
    }

    @Test fun `protection and equipment adapt style to ranged`() {
        val f = Fixture()
        ProtectedAccessLauncher.withProtectedAccess(f.player, ProtectedAccessContextFactory.empty()) { vars["varbit.prayer_protectfrommagic"] = 1 }
        f.attack("attack"); f.tick(3)
        assertEquals(HitType.Ranged, f.hits().single().type)
    }

    @Test fun `charged bracelet suppresses unprovoked combat and reduces real damage after retaliation`() {
        val f = Fixture(RevenantSpecies.Dragon.npcType); val slot = dev.openrune.util.Wearpos.Hands.slot
        f.player.worn[slot] = RevenantCharges.withCount(InvObj(RevenantCharges.EMPTY_BRACELET), 100)
        f.attack("attack"); f.tick(3); assertTrue(f.hits().isEmpty()); assertEquals(100, RevenantCharges.count(f.player.worn[slot]!!))
        f.npc.queueHit(f.player, 1, HitType.Melee, 0, StandardNpcHitModifier(f.bus))
        f.attack("attack"); f.tick(3); assertTrue(f.hits().single().damage in 0..7); assertEquals(99, RevenantCharges.count(f.player.worn[slot]!!))
    }

    @Test fun `heals are limited and respect half hp and fifteen tick cooldown`() {
        val f = Fixture(RevenantSpecies.Dragon.npcType); assertFalse(f.script.healIfReady(f.npc))
        var count = 0
        repeat(30) {
            f.npc.hitpoints = 1
            if (f.script.healIfReady(f.npc)) { count++; assertEquals(21, f.npc.hitpoints); assertFalse(f.script.healIfReady(f.npc)) }
            f.clock.cycle += 15
        }
        assertTrue(count in 8..25)
        f.bus.publish(NpcStateEvents.Respawn(f.npc)); f.npc.hitpoints = 1; assertTrue(f.script.healIfReady(f.npc))
    }

    @Test fun `shutdown cancels the delayed projectile effect`() {
        val f = Fixture(); f.attack("attack"); with(f.script) { f.context.shutdown() }; f.tick(3)
        assertTrue(f.hits().isEmpty()); assertFalse(f.player.isFrozen)
    }
    private class Fixture(type: String = RevenantSpecies.Imp.npcType) {
        val clock = MapClock(100); val bus = EventBus(); val collision = CollisionFlagMap(); val npcs = NpcList(); val players = PlayerList()
        val queues = WorldQueueList(); val world = mock(WorldRepository::class.java)
        val player = Player().apply {
            currentMapClock = 100; processedMapClock = 100
            slotId = 1; uuid = 1; assignUid(); coords = CoordGrid(3190, 10072)
            statMap.setBaseLevel("stat.hitpoints", 99); statMap.setCurrentLevel("stat.hitpoints", 99)
            worn = Inventory(ServerCacheManager.getInventory("inv.worn".asRSCM())!!, arrayOfNulls(14))
        }
        val repo = NpcRepository(clock, NpcRegistry(npcs, collision, bus), npcs)
        val deps = BossDeps(DefaultGameRandom(1), world, repo, mock(LocRepository::class.java), players, clock, queues, collision,
            EncounterRegistry(clock), BossExtensionRegistry(), mock(AccuracyFormulae::class.java) { true }, mock(MaxHitFormulae::class.java), StandardPlayerHitModifier(bus))
        val script = RevenantCombatScript(deps, npcs); val context = ScriptContext(bus, CheatCommandMap(), EngineQueueCache())
        val npc = Npc(ServerCacheManager.getNpc(type.asRSCM())!!, CoordGrid(3195, 10072)); val info = mock(NpcInfoProtocol::class.java)
        init {
            for (x in 3170..3220) for (z in 10050..10100) collision.set(x, z, 0, 0)
            players[1] = player; with(script) { context.startup() }; npc.infoProtocol = info; repo.add(npc, Int.MAX_VALUE)
        }
        fun attack(name: String) {
            deps.encounter(npc).forceNext(name)
            val producer = AiPlayerInteractions(bus, players)
            producer.interactAp(npc, player, InteractionOp.Op2)
            val trigger = checkNotNull(producer.apTrigger(npc, player, InteractionOp.Op2))
            val access = StandardNpcAccess(npc, GameCoroutine(), StandardNpcAccessContext({ DefaultGameRandom(1) }, { StandardNpcHitModifier(bus) }, { StandardNpcHitProcessor(players, bus, emptySet()) }))
            npc.launch { bus.publish(access, trigger) }
        }
        fun tick(count: Int) { repeat(count) {
            clock.cycle++; player.currentMapClock = clock.cycle; player.processedMapClock = clock.cycle
            val it = queues.iterator(); while (it.hasNext()) { val q = it.next(); if (--q.remainingCycles <= 0) { it.remove(); q.action() } }; it.cleanUp()
        } }
        fun hits(): List<Hit> {
            val hits = mutableListOf<Hit>(); val it = player.queueList.iterator() ?: return hits
            while (it.hasNext()) { (it.next().args as? Hit)?.let(hits::add) }; it.cleanUp(); return hits
        }
        fun clearHits() { player.queueList.removeAll("queue.hit") }
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
