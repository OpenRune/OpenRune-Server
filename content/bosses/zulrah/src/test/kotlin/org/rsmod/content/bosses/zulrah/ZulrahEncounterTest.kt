package org.rsmod.content.bosses.zulrah

import com.google.inject.AbstractModule
import com.google.inject.Guice
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossExtensionRegistry
import org.rsmod.api.bosses.runtime.EncounterRegistry
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.combat.formulas.MaxHitFormulae
import org.rsmod.api.combat.weapon.scripts.WeaponAttackStylesScript
import org.rsmod.api.combat.weapon.styles.AttackStyles
import org.rsmod.api.config.refs.params
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.instances.InstanceAccess
import org.rsmod.api.instances.InstanceId
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.instances.InstanceSpec
import org.rsmod.api.instances.region.InstanceAreaResolver
import org.rsmod.api.instances.ui.BossCountdown
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.hit.modifier.NoopPlayerHitModifier
import org.rsmod.api.random.GameRandom
import org.rsmod.api.registry.loc.LocRegistry
import org.rsmod.api.registry.loc.LocRegistryNormal
import org.rsmod.api.registry.loc.LocRegistryRegion
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.route.RayCastValidator
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.Inventory
import org.rsmod.game.map.LocZoneStorage
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.game.queue.QueueCategory
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.game.region.Region
import org.rsmod.game.region.zone.RegionZoneCopy
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag
import sun.misc.Unsafe

@OptIn(InternalApi::class)
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class ZulrahEncounterTest {
    @Test
    fun `all forms share native kill count and health bar metadata`() {
        for (symbol in ZulrahCombatScript.BOSS_TYPES) {
            val npc = ZulrahEncounterManager.type(symbol)
            assertEquals("varp.total_snakeboss_kills".asRSCM(RSCMType.VARP), npc.param(params.killcount_varp).id)
            assertEquals(1, npc.param(params.boss_hp_bar_mode))
            assertEquals(500, npc.hitpoints)
        }
    }

    @Test
    fun `joining twice creates one mapped boss without an engine respawn`() {
        val f = Fixture()
        f.start()
        val boss = f.boss()
        f.enter()
        assertEquals(1, f.npcs.count())
        assertEquals(CoordGrid(3226, 3265), boss.coords)
        assertEquals(500, boss.hitpoints)
        assertFalse(boss.respawns)
        assertTrue(f.encounters.validate(f.player, boss) is NpcAttackValidateResult.Deny)
        f.advance(4)
        assertSame(NpcAttackValidateResult.BypassSingleWayPvnRestriction, f.encounters.validate(f.player, boss))
    }

    @Test
    fun `entry remains empty for five seconds and duplicate joins do not reset the countdown`() {
        val f = Fixture()
        f.enter()
        assertEquals(0, f.npcs.count())
        f.advance(4)
        f.enter()
        f.advance(4)
        assertEquals(0, f.npcs.count())
        f.advance(1)
        assertEquals(1, f.npcs.count())
        assertEquals(500, f.boss().hitpoints)
        assertTrue(f.encounters.validate(f.player, f.boss()) is NpcAttackValidateResult.Deny)
    }

    @Test
    fun `leaving or logging out before first spawn cancels the waiting encounter`() {
        for (logout in listOf(false, true)) {
            val f = Fixture()
            f.enter()
            f.advance(3)
            if (logout) f.player.pendingLogout = true else f.encounters.stop(f.session.id)
            f.advance(30)
            assertEquals(0, f.npcs.count())
            assertTrue(f.instances.npcsForInstance(f.session.id).isEmpty())
        }
    }

    @Test
    fun `native hide reveal and form change preserve health debuffs and damage credit`() {
        val f = Fixture()
        f.start()
        val boss = f.boss()
        val firstUid = boss.uid
        boss.hitpoints = 287
        boss.defenceLvl = 260
        boss.damageContributions.record(f.player, 213)
        boss.heroPoints(f.player, 213)
        boss.queueHit(f.player, 40, HitType.Ranged, 10, NpcHitModifier { })
        f.advance(30)
        assertTrue(boss.hidden)
        assertFalse("queue.hit" in boss.queueList)
        assertTrue(f.encounters.validate(f.player, boss) is NpcAttackValidateResult.Deny)
        f.advance(3)
        assertFalse(boss.hidden)
        assertEquals("npc.snakeboss_boss_melee", boss.visType.internalName)
        assertNotEquals(firstUid, boss.uid)
        assertEquals(287, boss.hitpoints)
        assertEquals(260, boss.defenceLvl)
        assertEquals(213, boss.damageContributions.damageBy(f.player))
        assertSame(f.player, boss.findHero(f.players))
        assertEquals(1, f.npcs.count())
    }

    @Test
    fun `leaving during dive cancels pending hide and does not resurrect the boss`() {
        val f = Fixture()
        f.start()
        f.advance(28)
        f.encounters.stop(f.session.id)
        f.encounters.stop(f.session.id)
        f.advance(40)
        assertEquals(0, f.npcs.count())
    }

    @Test
    fun `logout tears down the encounter without waiting for instance expiry`() {
        val f = Fixture()
        f.start()
        f.player.pendingLogout = true
        f.advance(1)
        assertEquals(0, f.npcs.count())
    }

    @Test
    fun `leaving cancels the actual pending tail hit while preserving unrelated damage`() {
        val f = Fixture()
        f.start()
        f.advance(40)
        assertEquals(1, f.pendingHits(HitType.Typeless))
        val unrelated = Any()
        f.player.queueList.add("queue.hit", QueueCategory.Strong, 10, unrelated)
        f.encounters.stop(f.session.id)
        assertEquals(1, f.player.queueList.count("queue.hit"))
        assertEquals(1, f.player.queueList.removeIf { it.args === unrelated })
        assertEquals(0, f.player.queueList.count("queue.hit"))
    }

    @Test
    fun `moving away from the telegraphed tail avoids damage and stun`() {
        val f = Fixture()
        f.start()
        f.advance(37)
        val targetedTile = f.player.coords
        assertTrue(f.boss().isFacingLocked)
        assertEquals(targetedTile, f.boss().faceLockSquare)
        f.player.coords = targetedTile.translate(3, 0)
        f.advance(3)
        assertEquals(0, f.pendingHits(HitType.Typeless))
        assertFalse(f.player.frozen)
    }

    @Test
    fun `opening clouds are followed by regular attacks until the dive begins`() {
        val f = Fixture()
        f.start()
        val boss = f.boss()
        // Water blocks walking and cloud placement, but must allow the boss to fire projectiles.
        for (dx in 0 until boss.size) for (dz in 0 until boss.size) {
            val tile = boss.coords.translate(dx, dz)
            f.collision.remove(tile.x, tile.z, tile.level, CollisionFlag.LOC)
            f.collision.add(tile.x, tile.z, tile.level, CollisionFlag.BLOCK_WALK)
        }
        assertTrue(RayCastValidator(f.collision).hasLineOfSight(boss.coords, f.player.coords, boss.size, boss.size))
        f.advance(15)
        assertEquals(0, f.pendingHits(HitType.Ranged))
        f.advance(1)
        assertEquals(1, f.pendingHits(HitType.Ranged))
        f.advance(2)
        assertEquals(1, f.pendingHits(HitType.Ranged))
        f.advance(1)
        assertEquals(2, f.pendingHits(HitType.Ranged))
        f.advance(6)
        assertEquals(4, f.pendingHits(HitType.Ranged))
        f.advance(3)
        assertTrue(f.encounters.validate(f.player, f.boss()) is NpcAttackValidateResult.Deny)
        assertEquals(4, f.pendingHits(HitType.Ranged))
        f.advance(2)
        assertTrue(f.boss().hidden)
        assertEquals(4, f.pendingHits(HitType.Ranged))
    }

    @Test
    fun `real cloud spawn damages its footprint and exit removes both loc and pending damage`() {
        val f = Fixture()
        val center = CoordGrid(3233, 3261)
        for (dx in -1..1) for (dz in -1..1) {
            f.collision.remove(center.x + dx, center.z + dz, 0, CollisionFlag.LOC)
        }
        f.start()
        f.advance(6)
        val origin = center.translate(-1, -1)
        assertTrue(f.locRepo.findLoc(origin, "loc.snakeboss_poisoncloud"))
        f.player.coords = center
        f.advance(1)
        assertEquals(1, f.player.queueList.count("queue.hit"))
        f.encounters.stop(f.session.id)
        assertFalse(f.locRepo.findLoc(origin, "loc.snakeboss_poisoncloud"))
        assertEquals(0, f.player.queueList.count("queue.hit"))
    }

    @Test
    fun `other players cannot damage another owners encounter`() {
        val f = Fixture()
        f.start()
        f.advance(4)
        val other = Player().apply { uuid = 2L }
        assertTrue(f.encounters.validate(other, f.boss()) is NpcAttackValidateResult.Deny)
    }

    @Test
    fun `death returns the mapped platform loot tile once and stops further phases`() {
        val f = Fixture()
        f.start()
        val boss = f.boss()
        boss.hitpoints = 0
        assertEquals(CoordGrid(3228, 3261), f.encounters.beginDeath(boss))
        assertNull(f.encounters.beginDeath(boss))
        f.advance(100)
        assertEquals("npc.snakeboss_boss_ranged", boss.visType.internalName)
        assertEquals(0, boss.hitpoints)
    }

    internal class Fixture {
        val clock = MapClock(100)
        val events = EventBus()
        val player = Player().apply {
            uuid = 1L
            observerUUID = 1L
            slotId = 1
            assignUid()
            coords = CoordGrid(3228, 3261)
            previousCoords = coords
            currentMapClock = clock.cycle
            processedMapClock = clock.cycle
            inv = Inventory.create("inv.inv")
            worn = Inventory.create("inv.worn")
            for (stat in listOf("stat.defence", "stat.magic", "stat.hitpoints")) {
                statMap.setBaseLevel(stat, 99.toByte())
                statMap.setCurrentLevel(stat, 99.toByte())
            }
        }
        val players = PlayerList().apply { this[player.slotId] = player }
        val collision = CollisionFlagMap().apply {
            for (x in 3192..3344) for (z in 3192..3344) {
                allocateIfAbsent(x, z, 0)
                add(x, z, 0, CollisionFlag.LOC)
            }
        }
        val npcs = NpcList()
        val npcRegistry = NpcRegistry(npcs, collision, events)
        val npcRepo = NpcRepository(clock, npcRegistry, npcs)
        val zoneUpdates = ZoneUpdateMap()
        val locStorage = LocZoneStorage()
        val normalLocs = LocRegistryNormal(zoneUpdates, collision, locStorage)
        val locRepo = LocRepository(
            clock,
            LocRegistry(locStorage, normalLocs, unused(LocRegistryRegion::class.java)),
            unused(RegionRegistry::class.java),
        )
        val resolver = InstanceAreaResolver()
        val instances = InstanceManager(unused(RegionRepository::class.java), npcRepo, players, events, resolver, clock, collision)
        val session: InstanceSession
        val encounters: ZulrahEncounterManager

        init {
            val area = ZulrahInstance.ARENA
            val placement = (resolver.resolve(area) as InstanceAreaResolver.Result.Ready).placement
            val spec = InstanceSpec(0, 1, 2000, 100, true, area, -1)
            session = InstanceSession(InstanceId(1), mutableSetOf(), 1L, "zulrah", spec, placement, InstanceAccess.Private)
            session.addOccupant(1L)
            mapField<InstanceId, InstanceSession>(instances, "sessions")[session.id] = session
            mapField<Long, InstanceId>(instances, "playerIndex")[1L] = session.id
            val region = Region(CoordGrid(3200, 3200), CoordGrid(3328, 3328), 1, 1)
            for (x in 280..287) for (z in 376..391) {
                region.registerZone(RegionZoneCopy(ZoneKey(x, z, 0), 0, null), ZoneKey(400 + x - 280, 400 + z - 376, 0))
            }
            mapField<InstanceId, Region>(instances, "regions")[session.id] = region
            val deps = BossDeps(
                FixedRandom, WorldRepository(zoneUpdates), npcRepo, players, clock,
                WorldQueueList(), collision, EncounterRegistry(), BossExtensionRegistry(),
                accuracy(), unused(MaxHitFormulae::class.java), NoopPlayerHitModifier,
            )
            encounters = ZulrahEncounterManager(deps, instances, locRepo, AiPlayerInteractions(events, players), BossCountdown(events))
            events.subscribeUnbound(NpcStateEvents.Delete::class.java) { encounters.onNpcDeleted(npc) }
        }

        private fun accuracy(): AccuracyFormulae {
            val styles = AttackStyles()
            with(WeaponAttackStylesScript(styles)) {
                ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup()
            }
            val injector = Guice.createInjector(object : AbstractModule() {
                override fun configure() {
                    bind(GameRandom::class.java).toInstance(FixedRandom)
                    bind(AttackStyles::class.java).toInstance(styles)
                }
            })
            return injector.getInstance(AccuracyFormulae::class.java)
        }

        fun enter() = encounters.start(player, session)

        fun start() {
            enter()
            advance(9)
        }

        fun boss(): Npc = npcs.single().also { assertNotNull(it) }

        fun pendingHits(type: HitType): Int {
            val iterator = player.queueList.iterator() ?: return 0
            var count = 0
            while (iterator.hasNext()) {
                val hit = iterator.next().args as? org.rsmod.game.hit.Hit
                if (hit?.type == type) count++
            }
            return count
        }

        fun advance(ticks: Int) {
            repeat(ticks) {
                clock.tick()
                player.currentMapClock = clock.cycle
                player.processedMapClock = clock.cycle
                for (npc in npcs.toList()) {
                    npc.currentMapClock = clock.cycle
                    npc.processedMapClock = clock.cycle
                    if (npc.hidden && npc.lifecycleRevealCycle > 0 && npc.lifecycleRevealCycle <= clock.cycle) {
                        npcRegistry.reveal(npc)
                        npc.lifecycleRevealCycle = 0
                    }
                }
                encounters.tick()
            }
        }
    }

    private object FixedRandom : GameRandom {
        override fun of(maxExclusive: Int): Int = 0
        override fun of(minInclusive: Int, maxInclusive: Int): Int = minInclusive
        override fun randomDouble(): Double = 0.0
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun loadCache() {
            ServerCacheManager.init(240).close()
        }

        @Suppress("UNCHECKED_CAST")
        private fun <K, V> mapField(target: Any, name: String): MutableMap<K, V> =
            target.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(target) as MutableMap<K, V>

        private fun <T> unused(type: Class<T>): T {
            val field = Unsafe::class.java.getDeclaredField("theUnsafe").apply { isAccessible = true }
            return type.cast((field.get(null) as Unsafe).allocateInstance(type))
        }
    }
}
