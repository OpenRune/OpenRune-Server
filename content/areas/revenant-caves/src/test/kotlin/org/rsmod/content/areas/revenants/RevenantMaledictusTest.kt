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
internal class RevenantMaledictusTest {
    @Test fun `world spawn finds room for the full model and despawns after inactivity`() {
        val f = Fixture(); val boss = f.controller.spawn(f.player.coords)
        Assertions.assertNotNull(boss); assertSame(boss, f.controller.active); assertEquals(1250, boss!!.hitpoints)
        Assertions.assertNull(f.controller.spawn(f.player.coords)); f.tick(1005)
        Assertions.assertNull(f.controller.active); assertFalse(boss.isSlotAssigned)
        with(f.script) { f.context.shutdown() }
    }

    @Test fun `participant rewards go to contributors even outside the cave but not the MVP twice`() {
        val f = Fixture(); val other = f.addPlayer(); f.npc.recordDamage(f.player, 10); f.npc.recordDamage(other, 5); other.coords = CoordGrid(3200, 3200)
        assertSame(f.player, f.npc.findHero(f.players))
        f.controller.onKill(org.rsmod.api.death.NpcDeathKillContext(f.player, f.npc, 0))
        val calls = mockingDetails(f.objects).invocations.filter { it.method.name.startsWith("add") }
        assertEquals(2, calls.size); assertTrue(calls.all { it.arguments[1] == other.coords.packed && it.arguments[3] === other })
        assertEquals(setOf(1, 2), calls.map { it.arguments[4] }.toSet())
    }

    @Test fun `Maledictus attacks players in PvP while ordinary revenants disengage`() {
        val f = Fixture(); ProtectedAccessLauncher.withProtectedAccess(f.player, ProtectedAccessContextFactory.empty()) { vars["varp.lastcombat_pvp"] = 100 }
        f.attack("attack"); f.tick(3); assertEquals(1, f.hits().size)
    }

    @Test fun `normal attack damages all players in range and protection halves rather than blocks`() {
        val f = Fixture(); val second = f.addPlayer()
        ProtectedAccessLauncher.withProtectedAccess(second, ProtectedAccessContextFactory.empty()) { vars["varbit.prayer_protectfrommissiles"] = 1 }
        f.attack("attack"); f.tick(3)
        val first = f.hits().single(); val other = f.hits(second).single()
        assertEquals(HitType.Ranged, first.type); assertTrue(first.damage in 0..30); assertTrue(other.damage in 0..15)
        assertEquals(2, mockingDetails(f.world).invocations.count { it.method.name.startsWith("projAnim") })
    }

    @Test fun `fourth attack is a tile targeted ice spell and moving away avoids it`() {
        for (dodge in listOf(false, true)) {
            val f = Fixture()
            repeat(3) { f.attack("attack"); f.tick(5); f.clearHits() }
            f.attack("attack"); if (dodge)f.player.coords = f.player.coords.translateX(3); f.tick(3)
            verify(f.info).setSequence("seq.npc_revenant_superior_attack_stun".asRSCM(), 0)
            assertEquals(!dodge, f.player.isFrozen); assertEquals(if (dodge)0 else 1, f.hits().size)
        }
    }

    @Test fun `blood heal uses capped actual damage and runs only once across hit copies`() {
        val f = Fixture(); repeat(7) { f.attack("attack"); f.tick(5); f.clearHits() }
        f.npc.hitpoints = 100; f.attack("attack"); f.tick(3)
        val hit = f.hits().single(); assertEquals(HitType.Magic, hit.type)
        verify(f.info).setSequence("seq.npc_revenant_superior_attack_heal".asRSCM(), 0)
        hit.impactEffects.complete(6); assertEquals(103, f.npc.hitpoints)
        hit.copy().impactEffects.complete(20); assertEquals(103, f.npc.hitpoints)
    }

    @Test fun `twelfth attack is typeless with a five by five damage falloff`() {
        val f = Fixture(); repeat(11) { f.attack("attack"); f.tick(5); f.clearHits() }
        val edge = f.addPlayer(); edge.coords = f.player.coords.translateX(2)
        f.attack("attack"); f.tick(3)
        assertEquals(HitType.Typeless, f.hits().single().type); assertEquals(25, f.hits().single().damage); assertEquals(9, f.hits(edge).single().damage)
    }

    @Test fun `shutdown prevents pending blood healing and projectiles`() {
        val f = Fixture(); f.attack("attack"); with(f.script) { f.context.shutdown() }; f.tick(3); assertTrue(f.hits().isEmpty())
    }
    private class Fixture(type: String = RevenantSpecies.MALEDICTUS) {
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
        val objects = mock(org.rsmod.api.repo.obj.ObjRepository::class.java)
        val controller = RevenantMaledictusController(deps, mock(org.rsmod.api.area.checker.AreaChecker::class.java) { true }, objects)
        val script = RevenantMaledictus(deps, controller); val context = ScriptContext(bus, CheatCommandMap(), EngineQueueCache())
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
            // Match NpcPostTickProcess: an animation request lasts for this update tick.
            npc.pendingSequence = org.rsmod.game.seq.EntitySeq.NULL
            val it = queues.iterator(); while (it.hasNext()) { val q = it.next(); if (--q.remainingCycles <= 0) { it.remove(); q.action() } }; it.cleanUp()
        } }
        fun hits(target: Player = player): List<Hit> {
            val hits = mutableListOf<Hit>(); val it = target.queueList.iterator() ?: return hits
            while (it.hasNext()) { (it.next().args as? Hit)?.let(hits::add) }; it.cleanUp(); return hits
        }
        fun addPlayer(): Player = Player().apply {
            slotId = 2; uuid = 2; assignUid(); currentMapClock = clock.cycle; processedMapClock = clock.cycle; coords = player.coords
            statMap.setBaseLevel("stat.hitpoints", 99); statMap.setCurrentLevel("stat.hitpoints", 99)
            worn = Inventory(ServerCacheManager.getInventory("inv.worn".asRSCM())!!, arrayOfNulls(14)); players[2] = this
        }
        fun clearHits() { player.queueList.removeAll("queue.hit") }
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
