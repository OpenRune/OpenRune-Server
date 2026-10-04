package org.rsmod.content.bosses.araxxor

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.*
import org.mockito.Mockito.*
import org.rsmod.annotations.InternalApi
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.*
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.combat.formulas.MaxHitFormulae
import org.rsmod.api.instances.*
import org.rsmod.api.instances.region.InstanceAreaResolver
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.respawn.BossRespawnTimers
import org.rsmod.api.player.hit.modifier.PlayerHitModifier
import org.rsmod.api.random.GameRandom
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.route.RouteFactory
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.entity.*
import org.rsmod.game.hit.*
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.routefinder.collision.CollisionFlagMap

@ResourceLock("ServerCacheManager")
@Execution(ExecutionMode.SAME_THREAD)
@OptIn(InternalApi::class)
class AraxxorControllerTest {
    private lateinit var controller: AraxxorController
    private lateinit var instances: InstanceManager
    private lateinit var session: InstanceSession
    private lateinit var owner: Player
    private lateinit var npcs: NpcList
    private lateinit var clock: MapClock
    private lateinit var queues: WorldQueueList
    private lateinit var combat: AraxxorCombat
    private lateinit var timers: BossRespawnTimers

    @BeforeEach fun setup() {
        clock = MapClock(100)
        queues = WorldQueueList()
        owner = Player().apply {
            slotId = 1; uuid = 1; assignUid(); coords = AraxxorArena.arrival
            statMap.setCurrentLevel("stat.hitpoints", 99)
        }
        val players = PlayerList().apply { this[1] = owner }
        val spec = AraxxorArena.spec(AraxxorArena.outside)
        session = InstanceSession(InstanceId(1), mutableSetOf(), 1, AraxxorArena.KEY, spec,
            (InstanceAreaResolver().resolve(spec.area) as InstanceAreaResolver.Result.Ready).placement, InstanceAccess.Private)
        session.addOccupant(1)
        instances = mock(InstanceManager::class.java) { call ->
            if (call.method.name == "registerSessionNpc") true else RETURNS_DEFAULTS.answer(call)
        }
        `when`(instances.sessionForId(session.id)).thenReturn(session)
        `when`(instances.sessionForPlayer(owner)).thenReturn(session)
        for (tile in listOf(AraxxorArena.arrival, AraxxorArena.bossSpawn) + AraxxorArena.eggs) {
            `when`(instances.resolveCoord(session, tile)).thenReturn(tile)
        }
        npcs = NpcList()
        val collision = CollisionFlagMap()
        val repo = NpcRepository(clock, NpcRegistry(npcs, collision, mock(EventBus::class.java)), npcs)
        val registry = EncounterRegistry()
        val specBoss = boss(AraxxorAssets.BOSS) {
            val idle = ability("idle", org.rsmod.api.bosses.spec.Effect.NoOp)
            phase("combat") { rotationSelector { +then(idle) } }
        }
        for (symbol in listOf(AraxxorAssets.BOSS) + AraxyteKind.entries.map { it.spider }) {
            registry.register(symbol.asRSCM(), specBoss)
        }
        val deps = BossDeps(mock(GameRandom::class.java), mock(WorldRepository::class.java), repo,
            mock(LocRepository::class.java), players, clock, queues, collision, registry,
            BossExtensionRegistry(), mock(AccuracyFormulae::class.java), mock(MaxHitFormulae::class.java),
            mock(PlayerHitModifier::class.java))
        combat = mock(AraxxorCombat::class.java)
        timers = BossRespawnTimers(clock)
        controller = AraxxorController(instances, deps, mock(AiPlayerInteractions::class.java), combat,
            mock(NpcHitModifier::class.java), mock(RouteFactory::class.java), timers)
    }

    @Test fun `spawn is single owner only and reentry cannot duplicate actors`() {
        owner.pendingLogout = true
        controller.spawn(owner, session)
        assertFalse(npcs.any())
        owner.pendingLogout = false
        controller.spawn(owner, session)
        controller.spawn(owner, session)
        assertEquals(10, npcs.count())
        assertEquals(1, npcs.count { it.id == AraxxorAssets.BOSS.asRSCM() })
        assertTrue(npcs.all { !it.respawns })
        controller.end(session.id)
        assertFalse(npcs.any())
        assertTrue(timers.snapshot().isEmpty())
    }

    @Test fun `leaving cancels delayed damage and hatching before slots can be reused`() {
        controller.spawn(owner, session)
        val boss = bossNpc()
        `when`(combat.style(boss, owner)).thenReturn(HitType.Melee)
        `when`(combat.roll(boss, owner, HitType.Melee, null)).thenReturn(Hit(HitType.Melee, Hitmark(0), null, null, null))
        repeat(3) { controller.attack(boss, owner) }
        assertTrue(queues.isNotEmpty)
        controller.end(session.id)
        advance(4)
        assertTrue(mockingDetails(combat).invocations.none { it.method.name == "impact" })
        assertFalse(npcs.any())
    }

    @Test fun `enrage preserves drained levels and applies its boosts only once`() {
        controller.spawn(owner, session)
        val boss = bossNpc()
        boss.defenceLvl = 50
        boss.magicLvl = 70
        boss.rangedLvl = 90
        boss.hitpoints = 255
        repeat(3) { controller.tick() }
        assertEquals(85, boss.defenceLvl)
        assertEquals(98, boss.magicLvl)
        assertEquals(121, boss.rangedLvl)
        assertEquals(AraxxorAssets.ENRAGE.asRSCM(), boss.pendingSequence.id)
    }

    @Test fun `corpse reward is owner only exactly once with actual 34 tick respawn`() {
        controller.spawn(owner, session)
        val boss = bossNpc()
        boss.hitpoints = 0
        assertTrue(controller.beginDeath(boss))
        assertFalse(controller.beginDeath(boss))
        var claims = 0
        controller.finishDeath(boss) { claims++ }
        val corpse = npcs.single { it.id == AraxxorAssets.CORPSE.asRSCM() }
        assertTrue(boss.isInvisible)
        assertFalse(controller.harvest(Player(), corpse))
        assertTrue(controller.harvest(owner, corpse))
        assertFalse(controller.harvest(owner, corpse))
        assertEquals(1, claims)
        assertEquals(34, timers.snapshot().single().remainingTicks)
        clock.cycle += 33
        controller.tick()
        assertTrue(corpse.isSlotAssigned)
        clock.tick()
        controller.tick()
        assertFalse(corpse.isSlotAssigned)
        assertEquals(10, npcs.count())
        assertEquals(1020, bossNpc().hitpoints)
        assertFalse(controller.harvest(owner, corpse))
        assertEquals(1, claims)
    }

    @Test fun `logout during corpse phase removes rewards and all actors`() {
        controller.spawn(owner, session)
        val boss = bossNpc()
        boss.hitpoints = 0
        controller.beginDeath(boss)
        var claims = 0
        controller.finishDeath(boss) { claims++ }
        val corpse = npcs.single { it.id == AraxxorAssets.CORPSE.asRSCM() }
        owner.pendingLogout = true
        controller.tick()
        owner.pendingLogout = false
        assertFalse(controller.harvest(owner, corpse))
        assertEquals(0, claims)
        assertFalse(npcs.any())
    }

    private fun bossNpc() = npcs.single { it.id == AraxxorAssets.BOSS.asRSCM() }

    private fun advance(ticks: Int) = repeat(ticks) {
        clock.tick()
        val iterator = queues.iterator()
        while (iterator.hasNext()) {
            val q = iterator.next()
            if (--q.remainingCycles <= 0) { iterator.remove(); q.action() }
        }
        iterator.cleanUp()
    }

    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
