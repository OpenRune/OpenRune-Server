package org.rsmod.api.instances.timer

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.instances.InstanceAccess
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceId
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.instances.InstanceSettings
import org.rsmod.api.instances.region.InstanceAreaResolver
import org.rsmod.api.registry.controller.ControllerRegistry
import org.rsmod.api.registry.loc.LocRegistryNormal
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.registry.zone.ZonePlayerActivityBitSet
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.entity.ControllerList
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.map.LocZoneStorage
import org.rsmod.game.region.RegionListLarge
import org.rsmod.game.region.RegionListSmall
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
@OptIn(InternalApi::class)
class InstanceBossRespawnTest {
    @Test
    fun `timer follows actual native despawn deadline rather than death animation`() {
        val f = Fixture()
        val boss = f.spawn("npc.king_dragon")
        val session = f.session(boss)
        boss.hitpoints = 0
        assertNull(f.next(session))

        f.npcRepo.despawn(boss, 25)
        assertEquals(InstanceBossRespawn(boss.visType.name, 125), f.next(session))
        assertEquals(125, boss.lifecycleRespawnCycle)
        assertEquals(125, f.next(session, 124)?.deadlineTick)
        assertNull(f.next(session, 125))

        f.registry.respawn(boss)
        assertFalse(boss.hidden)
        assertNull(f.next(session))
    }

    @Test
    fun `hidden phases minions and custom nonrespawning bosses do not start native timers`() {
        val f = Fixture()
        val boss = f.spawn("npc.king_dragon")
        val custom = f.spawn("npc.snakeboss_boss_ranged")
        val minion = f.spawn("npc.snakeboss_minion_melee")
        val session = f.session(boss, custom)

        f.npcRepo.hide(boss, 20)
        f.npcRepo.despawn(minion, 10)
        f.npcRepo.despawn(custom, 30)
        custom.respawns = false
        assertNull(f.next(session))
    }

    @Test
    fun `earliest dead main boss wins and equal deadlines use stable slot order`() {
        val f = Fixture()
        val first = f.spawn("npc.king_dragon")
        val second = f.spawn("npc.godwars_bandos_avatar")
        val session = f.session(first, second)
        f.npcRepo.despawn(first, 30)
        f.npcRepo.despawn(second, 20)
        assertEquals(second.visType.name, f.next(session)?.bossName)

        second.lifecycleRespawnCycle = first.lifecycleRespawnCycle
        assertEquals(first.visType.name, InstanceBossRespawn.next(session, listOf(second, first), 100)?.bossName)
    }

    @Test
    fun `expired and grace instances suppress pending boss timers`() {
        val f = Fixture()
        val boss = f.spawn("npc.king_dragon")
        val session = f.session(boss)
        f.npcRepo.despawn(boss, 20)
        assertTrue(f.next(session) != null)
        session.enterGrace(100)
        assertNull(f.next(session))
        session.resetState()
        session.markArenaExpired()
        assertNull(f.next(session))
    }

    @Test
    fun `no declared main bosses means no timer`() {
        val f = Fixture()
        val boss = f.spawn("npc.king_dragon")
        f.npcRepo.despawn(boss, 20)
        assertNull(f.next(f.session()))
    }

    @Test
    fun `permanently deleted NPC never keeps a respawn timer`() {
        val f = Fixture()
        val boss = f.spawn("npc.king_dragon")
        val session = f.session(boss)
        f.npcRepo.despawn(boss, 20)
        f.npcRepo.del(boss, Int.MAX_VALUE)
        assertNull(InstanceBossRespawn.next(session, listOf(boss), 100))
    }

    @Test
    fun `identity membership survives deletion for kill hooks but never transfers to reused slots`() {
        val f = Fixture()
        val boss = f.spawn("npc.king_dragon")
        val originalSlot = boss.slotId
        val firstId = InstanceId(1)
        val secondId = InstanceId(2)
        f.manager.attachNpc(firstId, boss)
        f.npcRepo.del(boss, Int.MAX_VALUE)
        assertEquals(firstId, f.manager.instanceForNpc(boss))

        val replacement = Npc("npc.king_dragon", boss.spawnCoords).apply {
            slotId = originalSlot
            assignUid()
        }
        assertNull(f.manager.instanceForNpc(replacement))
        f.manager.attachNpc(secondId, replacement)
        f.manager.detachNpc(secondId, boss)
        assertEquals(firstId, f.manager.instanceForNpc(boss))

        f.manager.detachNpc(firstId, boss)
        assertNull(f.manager.instanceForNpc(boss))
        assertTrue(f.manager.npcsForInstance(firstId).isEmpty())
        assertEquals(secondId, f.manager.instanceForNpc(replacement))
        assertTrue(replacement.isSlotAssigned)
    }

    @Test
    fun `native despawn and transmog preserve instance membership until explicit removal`() {
        val f = Fixture()
        val boss = f.spawn("npc.snakeboss_boss_ranged")
        val alternate = f.spawn("npc.snakeboss_boss_magic")
        val id = InstanceId(1)
        f.manager.attachNpc(id, boss)
        boss.transmog(alternate.type, Int.MAX_VALUE)
        boss.assignUid()
        f.npcRepo.despawn(boss, 20)
        assertEquals(id, f.manager.instanceForNpc(boss))
        assertEquals(listOf(boss), f.manager.npcsForInstance(id))
        f.registry.respawn(boss)
        assertEquals(id, f.manager.instanceForNpc(boss))
    }

    private class Fixture {
        val clock = MapClock(100)
        private val events = EventBus()
        private val collision = CollisionFlagMap().apply {
            for (x in 3200..3240 step 8) for (z in 3200..3240 step 8) {
                allocateIfAbsent(x, z, 0)
            }
        }
        private val npcs = NpcList()
        val registry = NpcRegistry(npcs, collision, events)
        val npcRepo = NpcRepository(clock, registry, npcs)
        private val locZones = LocZoneStorage()
        private val regions = RegionRegistry(
            RegionListSmall(),
            RegionListLarge(),
            LocRegistryNormal(ZoneUpdateMap(), collision, locZones),
            collision,
            locZones,
            registry,
            ControllerRegistry(clock, ControllerList()),
            ZonePlayerActivityBitSet(),
        )
        private val resolver = InstanceAreaResolver()
        val manager = InstanceManager(
            RegionRepository(regions), npcRepo, PlayerList(), events, resolver, clock, collision,
        )

        fun spawn(type: String): Npc = Npc(type, CoordGrid(3210, 3210)).also {
            npcRepo.add(it, Int.MAX_VALUE)
        }

        fun session(vararg bosses: Npc): InstanceSession {
            val area = InstanceArea.copyRegions(listOf(9033))
            val spec = InstanceSettings(bossNpc = bosses.map { it.type }).withArea(area, -1)
            val placement = (resolver.resolve(area) as InstanceAreaResolver.Result.Ready).placement
            return InstanceSession(InstanceId(1), mutableSetOf(), 1L, "test", spec, placement, InstanceAccess.Private)
        }

        fun next(session: InstanceSession, tick: Int = clock.cycle): InstanceBossRespawn? =
            InstanceBossRespawn.next(session, npcs.toList(), tick)
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}
