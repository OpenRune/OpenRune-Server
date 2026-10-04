package org.rsmod.content.bosses.kraken

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.or2.central.account.Rights
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.*
import org.mockito.Mockito.*
import org.rsmod.annotations.InternalApi
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.*
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.combat.formulas.MaxHitFormulae
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.respawn.BossRespawnTimers
import org.rsmod.api.player.hit.modifier.PlayerHitModifier
import org.rsmod.api.player.interact.NpcInteractions
import org.rsmod.api.random.GameRandom
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.table.slayer.SlayerTaskRow
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.entity.*
import org.rsmod.game.hit.HitType
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

@ResourceLock("ServerCacheManager")
@Execution(ExecutionMode.SAME_THREAD)
@OptIn(InternalApi::class)
class KrakenControllerTest {
    private lateinit var controller: KrakenController
    private lateinit var owner: Player
    private lateinit var npcs: NpcList
    private lateinit var repo: NpcRepository
    private lateinit var clock: MapClock
    private lateinit var bossPool: Npc
    private lateinit var tentacles: List<Npc>
    private lateinit var queues: WorldQueueList
    private lateinit var playerInteractions: NpcInteractions
    private lateinit var hitModifier: PlayerHitModifier

    @BeforeEach fun setup() {
        clock = MapClock(100)
        owner = player(1)
        npcs = NpcList()
        val collision = CollisionFlagMap()
        repo = NpcRepository(clock, NpcRegistry(npcs, collision, mock(EventBus::class.java)), npcs)
        val registry = EncounterRegistry()
        queues = WorldQueueList()
        playerInteractions = mock(NpcInteractions::class.java)
        hitModifier = mock(PlayerHitModifier::class.java)
        for (kind in KrakenKind.entries) registry.register(kind.activeId, boss(kind.active) {
            val idle = ability("idle", Effect.NoOp)
            phase("combat") { rotationSelector { +then(idle) } }
        })
        val deps = BossDeps(mock(GameRandom::class.java), mock(WorldRepository::class.java), repo,
            mock(LocRepository::class.java), PlayerList().apply { this[1] = owner }, clock,
            queues, collision, registry, BossExtensionRegistry(), mock(AccuracyFormulae::class.java),
            mock(MaxHitFormulae::class.java), hitModifier)
        controller = KrakenController(deps, mock(AiPlayerInteractions::class.java), playerInteractions, BossRespawnTimers(clock))
        bossPool = pool(KrakenKind.BOSS, CoordGrid(2278, 10034))
        tentacles = listOf(2275 to 10034, 2275 to 10038, 2284 to 10034, 2284 to 10038)
            .map { (x, z) -> pool(KrakenKind.TENTACLE, CoordGrid(x, z)) }
    }

    private fun player(slot: Int) = Player().apply {
        slotId = slot; uuid = slot.toLong(); assignUid(); coords = CoordGrid(2280, 10030)
        statMap.setCurrentLevel("stat.hitpoints", 99)
        statMap.setCurrentLevel("stat.slayer", 87)
        statMap.setBaseLevel("stat.slayer", 87)
        vars.backing["varp.slayer_target".asRSCM()] = SlayerTaskRow.all().first { it.nameLowercase == "cave kraken" }.id
        vars.backing["varp.slayer_count".asRSCM()] = 20
    }
    private fun pool(kind: KrakenKind, coords: CoordGrid) = Npc(ServerCacheManager.getNpc(kind.poolId)!!, coords).also {
        repo.add(it, Int.MAX_VALUE); controller.created(it)
    }
    private fun active(kind: KrakenKind) = npcs.filter { it.type.id == kind.activeId }

    @Test fun `central pool requires four awakened tentacles`() {
        assertFalse(controller.disturb(owner, bossPool))
        tentacles.take(3).forEach { assertTrue(controller.disturb(owner, it)) }
        assertFalse(controller.disturb(owner, bossPool))
        assertTrue(controller.disturb(owner, tentacles.last()))
        assertTrue(controller.disturb(owner, bossPool))
        assertEquals(4, active(KrakenKind.TENTACLE).size)
        assertEquals(255, active(KrakenKind.BOSS).single().hitpoints)
        assertTrue(bossPool.hidden)
        assertTrue(active(KrakenKind.BOSS).single().movementLocked)
    }

    @Test fun `explosive awakens all actors exactly once and locks other players out`() {
        assertTrue(controller.disturb(owner, bossPool, true))
        assertFalse(controller.disturb(owner, bossPool, true))
        assertFalse(controller.disturb(player(2), bossPool, true))
        assertEquals(4, active(KrakenKind.TENTACLE).size)
        assertEquals(1, active(KrakenKind.BOSS).size)
    }

    @Test fun `cave kraken uses real model stats and independently resets`() {
        val cave = pool(KrakenKind.CAVE, CoordGrid(2270, 10030))
        assertTrue(controller.disturb(owner, cave))
        val npc = active(KrakenKind.CAVE).single()
        assertEquals(125, npc.hitpoints)
        assertEquals(120, npc.magicLvl)
        controller.finishDeath(controller.beginDeath(npc)!!)
        assertTrue(active(KrakenKind.CAVE).isEmpty())
        assertEquals(125, cave.lifecycleRevealCycle)
        assertFalse(bossPool.hidden)
    }

    @Test fun `boss death removes tentacles and schedules all pools for next fight`() {
        controller.disturb(owner, bossPool, true)
        val npc = active(KrakenKind.BOSS).single()
        controller.finishDeath(controller.beginDeath(npc)!!)
        assertTrue(active(KrakenKind.BOSS).isEmpty())
        assertTrue(active(KrakenKind.TENTACLE).isEmpty())
        assertEquals(134, bossPool.lifecycleRevealCycle)
        assertTrue(tentacles.all { it.lifecycleRevealCycle == 134 })
        assertFalse(controller.disturb(owner, bossPool, true))
    }

    @Test fun `logout and leaving abort encounter without loot or stuck pools`() {
        controller.disturb(owner, bossPool, true)
        owner.pendingLogout = true
        controller.tick()
        assertTrue(active(KrakenKind.BOSS).isEmpty())
        assertTrue(active(KrakenKind.TENTACLE).isEmpty())
        assertEquals(101, bossPool.lifecycleRevealCycle)
    }

    @Test fun `task and slayer requirements apply before awakening`() {
        owner.vars.backing["varp.slayer_count".asRSCM()] = 0
        assertFalse(controller.disturb(owner, bossPool, true))
        owner.vars.backing["varp.slayer_count".asRSCM()] = 20
        owner.statMap.setCurrentLevel("stat.slayer", 86)
        assertFalse(controller.disturb(owner, bossPool, true))
        assertFalse(bossPool.hidden)
        assertTrue(active(KrakenKind.BOSS).isEmpty())
    }

    @Test fun `fishing explosives cannot surface regular cave krakens`() {
        val cave = pool(KrakenKind.CAVE, CoordGrid(2270, 10030))
        assertFalse(controller.disturb(owner, cave, true))
        assertFalse(cave.hidden)
        assertTrue(active(KrakenKind.CAVE).isEmpty())
    }

    @Test fun `leaving cancels pending projectiles and delayed auto attack`() {
        controller.disturb(owner, bossPool, true)
        val boss = active(KrakenKind.BOSS).single()
        controller.attack(boss, owner)
        assertTrue(queues.isNotEmpty)
        owner.coords = CoordGrid(3222, 3218)
        controller.tick()
        repeat(KrakenKind.BOSS.spawnTicks + 3) {
            clock.tick()
            val iterator = queues.iterator()
            while (iterator.hasNext()) {
                val queue = iterator.next()
                if (--queue.remainingCycles <= 0) { iterator.remove(); queue.action() }
            }
            iterator.cleanUp()
        }
        verifyNoInteractions(hitModifier, playerInteractions)
        assertEquals(101, bossPool.lifecycleRevealCycle)
    }

    @Test fun `admin testing preserves slayer task and expires on leaving`() {
        owner.vars.backing["varp.slayer_count".asRSCM()] = 0
        controller.setTesting(owner, true)
        assertFalse(controller.allowed(owner))
        owner.modLevel = Rights.ADMINISTRATOR
        controller.setTesting(owner, true)
        assertTrue(controller.allowed(owner))
        assertEquals(0, owner.vars["varp.slayer_count"])
        owner.coords = CoordGrid(3222, 3218)
        controller.tick()
        owner.coords = CoordGrid(2280, 10030)
        assertFalse(controller.allowed(owner))
    }

    @Test fun `ranged is reduced and melee cannot damage underwater actors`() {
        for (kind in KrakenKind.entries) {
            assertEquals(10, KrakenRules.damage(kind, HitType.Ranged, 70))
            assertEquals(0, KrakenRules.damage(kind, HitType.Melee, 70))
            assertEquals(70, KrakenRules.damage(kind, HitType.Magic, 70))
        }
    }

    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
