package org.rsmod.content.generic.npcs.crabs

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.annotations.InternalApi
import org.rsmod.api.game.process.npc.AiTimerProcessor
import org.rsmod.api.game.process.npc.hunt.NpcHuntProcessor
import org.rsmod.api.game.process.npc.hunt.NpcPlayerHuntProcessor
import org.rsmod.api.hunt.Hunt
import org.rsmod.api.npc.aggression.AggressionTolerance
import org.rsmod.api.npc.interact.AiLocInteractions
import org.rsmod.api.npc.interact.AiNpcInteractions
import org.rsmod.api.npc.interact.AiObjInteractions
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.random.DefaultGameRandom
import org.rsmod.api.registry.controller.ControllerRegistry
import org.rsmod.api.registry.loc.LocRegistry
import org.rsmod.api.registry.loc.LocRegistryNormal
import org.rsmod.api.registry.loc.LocRegistryRegion
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.player.PlayerRegistry
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.registry.zone.ZonePlayerActivityBitSet
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.route.BoundValidator
import org.rsmod.api.route.RayCastValidator
import org.rsmod.content.generic.npcs.disguise.DisguisedNpcs
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.ControllerList
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.npc.NoopNpcInfo
import org.rsmod.game.entity.npc.NpcInfoProtocol
import org.rsmod.game.map.LocZoneStorage
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.game.region.RegionListLarge
import org.rsmod.game.region.RegionListSmall
import org.rsmod.game.region.RegionListWorldEntity
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

@OptIn(InternalApi::class)
class CrabWorld {
    val events = EventBus()
    val collision = CollisionFlagMap()
    val clock = MapClock().apply { cycle = 100 }
    private val tolerance = AggressionTolerance(clock)
    private val updates = ZoneUpdateMap()
    private val zones = LocZoneStorage()
    private val activity = ZonePlayerActivityBitSet()
    private val npcList = NpcList()
    private val playerList = PlayerList()
    private val npcs = NpcRegistry(npcList, collision, events)
    private val players = PlayerRegistry(playerList, collision, activity, events)
    private val normal = LocRegistryNormal(updates, collision, zones)
    private val regions =
        RegionRegistry(
            RegionListSmall(),
            RegionListLarge(),
            RegionListWorldEntity(),
            normal,
            collision,
            zones,
            npcs,
            ControllerRegistry(clock, ControllerList()),
            activity,
        )
    private val locs =
        LocRegistry(zones, normal, LocRegistryRegion(updates, collision, zones, regions))
    private val hunt =
        Hunt(RayCastValidator(collision), players, npcs, ObjRegistry(updates), locs)
    private val random = DefaultGameRandom(1L)
    private val playerHunt = NpcPlayerHuntProcessor(random, clock, hunt, tolerance)
    private val npcHunt =
        NpcHuntProcessor(
            random,
            clock,
            hunt,
            playerList,
            npcList,
            AiPlayerInteractions(events, playerList),
            AiNpcInteractions(events),
            AiObjInteractions(events),
            AiLocInteractions(BoundValidator(collision), events),
        )
    private val aiTimers = AiTimerProcessor(events)
    private val disguises = DisguisedNpcs(hunt, tolerance, playerList, collision)
    private var nextSlot = 1

    init {
        val context = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
        with(Crabs(disguises)) { context.startup() }
    }

    fun spawn(type: String, at: CoordGrid): Npc {
        allocate(at)
        val npc = Npc(type, at)
        npc.infoProtocol =
            object : NpcInfoProtocol by NoopNpcInfo {
                override fun isActive() = true
            }
        check(npcs.add(npc) == org.rsmod.api.registry.npc.NpcRegistryResult.Add.Success)
        return npc
    }

    fun player(at: CoordGrid): Player {
        allocate(at)
        val player = Player()
        player.uuid = 7000L + nextSlot
        player.slotId = nextSlot++
        playerList[player.slotId] = player
        player.assignUid()
        player.coords = at
        player.currentMapClock = clock.cycle
        players.change(player, ZoneKey.NULL, ZoneKey.from(at))
        return player
    }

    fun move(player: Player, to: CoordGrid) {
        allocate(to)
        val from = ZoneKey.from(player.coords)
        player.coords = to
        players.change(player, from, ZoneKey.from(to))
    }

    fun tick(count: Int = 1) {
        repeat(count) {
            clock.tick()
            for (npc in npcList) {
                npc.currentMapClock = clock.cycle
                npc.processedMapClock = clock.cycle
            }
            for (npc in npcList) {
                playerHunt.process(npc)
            }
            for (npc in npcList) {
                if (npc.canProcess) {
                    npcHunt.process(npc)
                    aiTimers.process(npc)
                }
            }
        }
    }

    fun ticks(seq: String): Int =
        checkNotNull(ServerCacheManager.getAnim(seq.asRSCM(RSCMType.SEQ))).tickDuration

    private fun allocate(at: CoordGrid) {
        for (dx in -16..16 step 8) {
            for (dz in -16..16 step 8) {
                collision.allocateIfAbsent(at.x + dx, at.z + dz, at.level)
            }
        }
    }
}
