package org.rsmod.content.bosses.corp

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.varp.baseVar
import dev.openrune.types.varp.bits
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.annotations.InternalApi
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.*
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.combat.formulas.MaxHitFormulae
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.hit.modifier.StandardPlayerHitModifier
import org.rsmod.api.player.hit.processor.InstantPlayerHitProcessor
import org.rsmod.api.random.GameRandom
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.entity.*
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

@ResourceLock("ServerCacheManager")
@OptIn(InternalApi::class)
class CorpControllerTest {
    private lateinit var controller: CorpController
    private lateinit var boss: Npc
    private lateinit var player: Player
    private lateinit var clock: MapClock
    private lateinit var queues: WorldQueueList
    private lateinit var npcs: NpcList
    private lateinit var hits: CorpHits
    private val impacts = mutableListOf<Hit>()
    private var roll = 50

    @BeforeEach fun setup() {
        impacts.clear(); clock = MapClock(100); queues = WorldQueueList(); npcs = NpcList()
        player = Player().apply {
            slotId = 1; uuid = 1; assignUid(); coords = CoordGrid(2990, 4254, 2)
            statMap.setCurrentLevel("stat.hitpoints", 99)
            worn = Inventory(ServerCacheManager.getInventory("inv.worn".asRSCM())!!, arrayOfNulls(14))
        }
        val players = PlayerList().apply { this[1] = player }
        val collision = CollisionFlagMap()
        val repo = NpcRepository(clock, NpcRegistry(npcs, collision, mock(EventBus::class.java)), npcs)
        val random = mock(GameRandom::class.java) { call ->
            val range = call.arguments.firstOrNull() as? IntRange
            when (range) { 0..99 -> roll; 0..7 -> 0; else -> range?.last ?: 0 }
        }
        val registry = EncounterRegistry().apply { register(CorpRules.BOSS.asRSCM(), boss(CorpRules.BOSS) {
            val idle = ability("idle", Effect.NoOp); phase("combat") { rotationSelector { +then(idle) } }
        }) }
        val modifier = StandardPlayerHitModifier(mock(EventBus::class.java))
        hits = CorpHits(modifier, InstantPlayerHitProcessor { impacts += it; statMap.setCurrentLevel("stat.hitpoints", ((statMap.getCurrentLevel("stat.hitpoints").toInt() and 255) - it.damage).coerceAtLeast(0).toByte()) })
        val deps = BossDeps(random, mock(WorldRepository::class.java), repo, mock(LocRepository::class.java), players,
            clock, queues, collision, registry, BossExtensionRegistry(), mock(AccuracyFormulae::class.java) { true }, mock(MaxHitFormulae::class.java), modifier)
        controller = CorpController(deps, hits, mock(AiPlayerInteractions::class.java))
        boss = Npc(ServerCacheManager.getNpc(CorpRules.BOSS.asRSCM())!!, CoordGrid(2993, 4254, 2))
        repo.add(boss, Int.MAX_VALUE); controller.created(boss)
    }
    private fun advance(ticks: Int) { repeat(ticks) {
        clock.tick(); val iterator = queues.iterator()
        while (iterator.hasNext()) { val q = iterator.next(); if (--q.remainingCycles <= 0) { iterator.remove(); q.action() } }
        iterator.cleanUp(); controller.tick()
    } }

    @Test fun `normal magic reaches native impact without zero-delay queues`() {
        roll = 0; controller.attack(boss, player); advance(2)
        assertEquals(65, impacts.single().damage)
    }

    @Test fun `prayers block melee and exactly one third of magic but never stomp`() {
        for (prayer in listOf("varbit.prayer_protectfrommagic", "varbit.prayer_protectfrommelee")) {
            val bit = ServerCacheManager.getVarbit(prayer.asRSCM())!!
            player.vars.backing[bit.baseVar.id] = player.vars.backing[bit.baseVar.id] or (1 shl bit.bits.first)
        }
        assertEquals(43, hits.snapshot(boss, player, HitType.Magic, 65).damage)
        assertEquals(36, hits.snapshot(boss, player, HitType.Magic, 55).damage)
        assertEquals(0, hits.snapshot(boss, player, HitType.Melee, 33).damage)
        assertEquals(51, hits.snapshot(boss, player, HitType.Typeless, 51).damage)
        assertEquals(33, CorpRules.meleeMax(320)); assertEquals(1, CorpRules.meleeMax(0))
    }

    @Test fun `leaving cancels airborne damage`() {
        roll = 0; controller.attack(boss, player); player.coords = CorpRules.LOBBY; advance(3)
        assertTrue(impacts.isEmpty())
    }

    @Test fun `stomp only hits players beneath boss`() {
        player.coords = boss.coords; advance(7)
        assertEquals(51, impacts.single().damage)
    }

    @Test fun `stat drains recover slowly while empty room heals hitpoints`() {
        player.coords = CorpRules.LOBBY; boss.hitpoints = 1000; boss.defenceLvl = 10
        advance(20); assertEquals(11, boss.defenceLvl); assertEquals(1050, boss.hitpoints)
        advance(280); assertEquals(2000, boss.hitpoints)
    }

    @Test fun `draining magic heals half the real damage`() {
        boss.hitpoints = 1500; roll = 34; controller.attack(boss, player); advance(2)
        assertEquals(55, impacts.single().damage)
        assertEquals(1527, boss.hitpoints)
    }

    @Test fun `core drains nearby player and heals boss`() {
        boss.hitpoints = 900; roll = 0; controller.attack(boss, player); advance(2)
        assertTrue(impacts.any { it.type == HitType.Typeless && it.damage == 13 })
        assertEquals(913, boss.hitpoints)
    }

    @Test fun `poisoned core resumes hopping immediately when player steps away`() {
        boss.hitpoints = 900; roll = 0; controller.attack(boss, player)
        val core = npcs.single { it.type.id == CorpRules.CORE.asRSCM() }
        core.vars["varn.poison_severity"] = 4; advance(2)
        player.coords = player.coords.translate(-4, 0); advance(1)
        assertEquals(player.coords, core.coords)
        assertTrue(core.hidden)
    }

    @Test fun `low hp summons only one core and boss death removes it`() {
        boss.hitpoints = 900; repeat(3) { controller.attack(boss, player) }
        assertEquals(1, npcs.count { it.type.id == CorpRules.CORE.asRSCM() })
        controller.dying(boss); advance(3)
        assertEquals(0, npcs.count { it.type.id == CorpRules.CORE.asRSCM() })
        assertTrue(impacts.isEmpty())
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
