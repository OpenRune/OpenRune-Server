package org.rsmod.content.minigames.gauntlet

import dev.openrune.ServerCacheManager
import kotlin.random.Random
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.annotations.InternalApi
import org.rsmod.api.bosses.runtime.*
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.combat.formulas.MaxHitFormulae
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.access.StandardNpcAccessContextFactory
import org.rsmod.api.npc.hit.modifier.StandardNpcHitModifier
import org.rsmod.api.npc.hit.processor.StandardNpcHitProcessor
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.player.hit.modifier.StandardPlayerHitModifier
import org.rsmod.api.player.hit.DeferredPlayerHit
import org.rsmod.api.player.hit.modify
import org.rsmod.api.player.hit.queueImpactHit
import org.rsmod.api.player.hit.queueHit as queuePlayerHit
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.content.minigames.gauntlet.hunllef.*
import org.rsmod.content.minigames.gauntlet.layout.GauntletLayout
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.game.MapClock
import org.rsmod.game.entity.*
import org.rsmod.game.hit.HitType
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

@ResourceLock("ServerCacheManager")
@OptIn(InternalApi::class)
class HunllefMechanicsTest {
    @Test fun `sixth off prayer hit lands before native protection changes`() {
        val f = Fight()
        repeat(5) { assertEquals(20, f.playerAttack(HitType.Ranged, 20).damage) }
        assertEquals(HunllefVarns.PROTECT_MELEE, f.boss.vars[HunllefVarns.PROTECT])
        assertEquals(20, f.playerAttack(HitType.Ranged, 20).damage)
        assertEquals(HunllefVarns.PROTECT_RANGED, f.boss.vars[HunllefVarns.PROTECT])
        assertEquals(1, f.boss.vars["varn.immune_ranged"])
        assertEquals(0, f.boss.vars["varn.immune_melee"])
        assertEquals(0, f.playerAttack(HitType.Ranged, 20).damage)
        assertEquals(480, f.boss.hitpoints)
        assertEquals(120, f.run.bossDamage)
    }

    @Test fun `zero damage misses count but blocked attacks do not`() {
        val f = Fight()
        repeat(8) { f.playerAttack(HitType.Melee, 20) }
        assertEquals(0, f.boss.vars[HunllefVarns.OFF_PRAYER])
        repeat(6) { f.playerAttack(HitType.Magic, 0) }
        assertEquals(HunllefVarns.PROTECT_MAGIC, f.boss.vars[HunllefVarns.PROTECT])
        assertEquals(600, f.boss.hitpoints)
        assertEquals(0, f.run.bossDamage)
    }

    @Test fun `native player modifier uses exact prayed armour damage caps`() {
        for (corrupted in listOf(false, true)) for (tier in 0..3) {
            val f = Fight(corrupted)
            VarPlayerIntMapSetter.set(f.owner, "varbit.prayer_protectfrommissiles", 1)
            if (tier > 0) for ((slot, piece) in listOf(0 to "helmet", 4 to "chestplate", 7 to "platelegs")) {
                f.owner.worn[slot] = org.rsmod.game.inv.InvObj(gauntletObj("${piece}_t$tier", corrupted))
            }
            val cap = HunllefDamage.maxHit(corrupted, tier * 3, prayed = true)
            val damages = (0 until 200).map { f.owner.queuePlayerHit(f.boss, 1, HitType.Ranged, 1, f.playerModifier).damage }
            assertTrue(damages.all { it in 0..cap })
            assertTrue(damages.any { it > 0 }, "Prayer penetration must retain damage")
        }
    }

    @Test fun `late normal attacks cannot hit a player who left the owned run`() {
        val f = Fight()
        `when`(f.runs.runFor(f.owner)).thenReturn(null)
        assertEquals(0, f.owner.queuePlayerHit(f.boss, 1, HitType.Magic, 1, f.playerModifier).damage)
    }

    @Test fun `armour damage tables match all complete tiers in both modes`() {
        assertEquals(listOf(12, 10, 8, 6), (0..3).map { HunllefDamage.maxHit(false, it * 3, true) })
        assertEquals(listOf(16, 13, 10, 8), (0..3).map { HunllefDamage.maxHit(true, it * 3, true) })
        assertEquals(listOf(51, 41, 34, 26), (0..3).map { HunllefDamage.maxHit(false, it * 3, false) })
        assertEquals(listOf(68, 55, 45, 35), (0..3).map { HunllefDamage.maxHit(true, it * 3, false) })
    }

    @Test fun `floor patterns stay inside the arena and accelerate across stages`() {
        for (stage in HunllefStage.entries) {
            assertTrue(HunllefPatterns.patterns(stage).all { it.isNotEmpty() && it.all { tile -> tile.x in 0..11 && tile.z in 0..11 } })
        }
        for (corrupted in listOf(false, true)) {
            val timings = HunllefStage.entries.map { HunllefPatterns.timing(corrupted, it, 0.5).cycle }
            assertTrue(timings.zipWithNext().all { (a, b) -> a > b })
        }
    }

    @Test fun `four offensive casts switch style and stomps do not count`() {
        val f = Fight()
        repeat(3) { f.deps.runAbility(f.boss, f.owner, "ranged") }
        assertEquals(3, f.boss.vars[HunllefVarns.CYCLE])
        f.deps.runAbility(f.boss, f.owner, "stomp")
        assertEquals(3, f.boss.vars[HunllefVarns.CYCLE])
        f.deps.runAbility(f.boss, f.owner, "ranged")
        assertEquals(0, f.boss.vars[HunllefVarns.CYCLE])
        assertEquals(HunllefVarns.MAGIC, f.boss.vars[HunllefVarns.STYLE])
    }

    @Test fun `queued projectile retains prayed damage after a protection model change`() {
        val f = Fight()
        VarPlayerIntMapSetter.set(f.owner, "varbit.prayer_protectfrommissiles", 1)
        f.owner.queueImpactHit(f.boss, 2, HitType.Ranged, 1, f.playerModifier)
        val deferred = f.owner.queueList.iterator()!!.next().args as DeferredPlayerHit
        val launchedUid = deferred.builder.sourceUid
        repeat(6) { f.playerAttack(HitType.Ranged, 0) }
        assertNotEquals(launchedUid, f.boss.uid.packed)
        val damages = (0 until 100).map {
            deferred.builder.damage = 1
            deferred.modifier.modify(deferred.builder, f.owner)
            deferred.builder.damage
        }
        assertTrue(damages.all { it in 0..12 })
        assertTrue(damages.any { it > 1 }, "The sentinel must be replaced on the impact tick")
        assertEquals(100, deferred.builder.penetration)
    }

    @Test fun `queued projectile is cancelled when its owned actor disappears`() {
        val f = Fight()
        f.owner.queueImpactHit(f.boss, 2, HitType.Magic, 1, f.playerModifier)
        val deferred = f.owner.queueList.iterator()!!.next().args as DeferredPlayerHit
        f.repo.del(f.boss, Int.MAX_VALUE)
        deferred.modifier.modify(deferred.builder, f.owner)
        assertEquals(0, deferred.builder.damage)
    }

    private class Fight(corrupted: Boolean = false) {
        val fixture = GauntletTestFixture()
        val owner = fixture.player.apply { slotId = 1; uuid = 1; assignUid(); coords = CoordGrid(1000, 1000, 1) }
        val players = PlayerList().apply { this[1] = owner }
        val clock = MapClock(100)
        val collision = CollisionFlagMap()
        val npcs = NpcList()
        val repo = NpcRepository(clock, NpcRegistry(npcs, collision, fixture.events), npcs)
        val runs = mock(GauntletRuns::class.java)
        val mode = if (corrupted) GauntletMode.CORRUPTED else GauntletMode.NORMAL
        val run = GauntletRun(GauntletLayout.generate(Random(9)), mode, emptyMap()).apply { fighter = owner }
        val playerModifier = StandardPlayerHitModifier(fixture.events)
        val modifier = StandardNpcHitModifier(fixture.events)
        val processor = StandardNpcHitProcessor(players, fixture.events, emptySet())
        val random = mock(GameRandom::class.java)
        val deps = BossDeps(random, WorldRepository(mock(ZoneUpdateMap::class.java)), repo, mock(LocRepository::class.java), players, clock,
            WorldQueueList(), collision, EncounterRegistry(), BossExtensionRegistry(), mock(AccuracyFormulae::class.java), mock(MaxHitFormulae::class.java), playerModifier)
        val specs = HunllefSpecs()
        val boss = Npc("npc.crystal_hunllef_melee${if (corrupted) "_hm" else ""}", CoordGrid(1000, 1001, 1))
        val npcAccess = StandardNpcAccess(boss, GameCoroutine("hunllef-impact"), StandardNpcAccessContextFactory(random, modifier, processor).create())
        init {
            run.hunllef = boss
            fixture.register(Hunllef(deps, specs, mock(HunllefFloor::class.java), mock(HunllefTornadoes::class.java), runs, mock(GauntletRewards::class.java), mock(ProtectedAccessLauncher::class.java), npcs))
            repo.add(boss, Int.MAX_VALUE)
            deps.startEncounter(boss, specs.of(corrupted))
            boss.vars["varn.immune_melee"] = 1
            `when`(runs.runFor(boss)).thenReturn(run)
            `when`(runs.runFor(owner)).thenReturn(run)
        }
        fun playerAttack(type: HitType, damage: Int): org.rsmod.game.hit.Hit {
            val hit = boss.queueHit(owner, 1, type, damage, modifier)
            with(processor) { npcAccess.process(hit) }
            return hit
        }
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
