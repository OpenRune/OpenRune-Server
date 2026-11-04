package org.rsmod.content.minigames.motherlodemine.scripts

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.NpcMode
import jakarta.inject.Inject
import java.util.WeakHashMap
import org.rsmod.annotations.InternalApi
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.route.RouteFactory
import org.rsmod.api.script.onAiTimer
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onGameStartup
import org.rsmod.content.minigames.motherlodemine.MotherlodeRockfalls
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeConstants
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.movement.RouteRequestLoc
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MotherlodeMinerScript @Inject constructor(
    private val clock: MapClock,
    private val random: GameRandom,
    private val npcs: NpcList,
    private val locs: LocRepository,
    private val routes: RouteFactory,
    private val rockfalls: MotherlodeRockfalls,
) : PluginScript() {
    private val miners = WeakHashMap<Npc, MiningState>()

    override fun ScriptContext.startup() {
        for (miner in Miners) {
            onEvent<NpcStateEvents.Spawn>(miner.asRSCM()) { initialise(npc) }
            onAiTimer(miner) { mine(npc) }
        }
        onGameStartup { npcs.filter { it.id in minerIds }.forEach(::initialise) }
    }

    internal fun initialise(npc: Npc) {
        if (!inside(npc.spawnCoords)) return
        npc.aiTimer(1)
        npc.mode = NpcMode.None
    }

    @OptIn(InternalApi::class)
    internal fun mine(npc: Npc) {
        if (!inside(npc.spawnCoords) || !npc.isVisible || npc.isBusy ||
            npc.activeCoroutine != null || npc.hasInteraction()) return
        val state = miners.getOrPut(npc) { MiningState(nextShout = clock.cycle + random.of(100, 250)) }
        if (clock.cycle < state.resumeAt) return
        val target = state.target
        if (target == null) {
            if (npc.routeDestination.isNotEmpty()) return
            chooseTarget(npc, state)
            return
        }
        if (locs.findExact(target.coords, target.shape)?.id != target.id ||
            state.finishAt == 0 && clock.cycle >= state.giveUpAt) {
            finish(npc, state)
            return
        }
        if (npc.routeDestination.isNotEmpty()) return
        val request = RouteRequestLoc(LocInfo(target.layer, target.coords, target.entity),
            target.width, target.length, target.forceApproachFlags)
        val route = routes.create(npc.avatar, request)
        if (!route.success || route.alternative || route.isNotEmpty()) {
            finish(npc, state)
            return
        }
        npc.faceSquare(target.coords)
        if (state.finishAt == 0) {
            npc.anim(miningAnimation(npc))
            state.finishAt = clock.cycle + if (target.id in rockfallIds) random.of(1, 3) else random.of(12, 30)
            state.nextSwing = clock.cycle + 4
            if (target.id !in rockfallIds && clock.cycle >= state.nextShout) {
                npc.say("Pay-dirt!")
                state.nextShout = clock.cycle + random.of(100, 250)
            }
        } else if (clock.cycle >= state.finishAt) {
            if (target.id in rockfallIds) rockfalls.clear(target)
            finish(npc, state)
        } else if (clock.cycle >= state.nextSwing) {
            npc.anim(miningAnimation(npc))
            state.nextSwing = clock.cycle + 4
        }
    }

    private fun chooseTarget(npc: Npc, state: MiningState) {
        val nearby = nearbyLocs(npc.spawnCoords).filter {
            it.id in veinIds || it.id in rockfallIds
        }.toList()
        val clearRockfall = if (clock.cycle >= state.nextRockfallCheck) {
            state.nextRockfallCheck = clock.cycle + random.of(60, 140)
            random.randomBoolean(8)
        } else false
        val nearbyRockfalls = nearby.filter { it.id in rockfallIds && it.coords.chebyshevDistance(npc.coords) <= 3 }
        val rockfall = if (clearRockfall) chooseReachable(npc, nearbyRockfalls) else null
        val target = rockfall ?: chooseReachable(npc, nearby.filter { it.id in veinIds })
        if (target == null) {
            state.resumeAt = clock.cycle + random.of(3, 15)
            return
        }
        state.target = target.loc
        state.giveUpAt = clock.cycle + 30
        npc.walk(target.waypoints)
    }

    private fun chooseReachable(npc: Npc, choices: List<LocInfo>): MiningTarget? {
        val reachable = choices.mapNotNull { target ->
            val type = ServerCacheManager.getObject(target.id) ?: return@mapNotNull null
            val route = routes.create(npc.avatar, RouteRequestLoc(target, type))
            if (!route.success || route.alternative) return@mapNotNull null
            val waypoints = route.map { CoordGrid(it.x, it.z, it.level) }
            if (waypoints.any { !inside(it) || it.chebyshevDistance(npc.spawnCoords) > 4 }) return@mapNotNull null
            if (target.id in rockfallIds && waypoints.any { it.chebyshevDistance(npc.coords) > 2 }) return@mapNotNull null
            MiningTarget(BoundLocInfo(target, type), waypoints)
        }
        return random.pickOrNull(reachable)
    }

    private fun nearbyLocs(coords: CoordGrid): Sequence<LocInfo> = sequence {
        for (dx in -1..1) for (dz in -1..1) {
            yieldAll(locs.findAll(ZoneKey.fromAbsolute(coords.x + dx * 8, coords.z + dz * 8, coords.level))
                .filter { it.coords.chebyshevDistance(coords) <= 4 })
        }
    }

    private fun finish(npc: Npc, state: MiningState) {
        npc.resetMovement()
        npc.resetAnim()
        state.target = null
        state.finishAt = 0
        state.resumeAt = clock.cycle + random.of(3, 15)
    }

    private fun miningAnimation(npc: Npc): String =
        if (npc.type.standAnim == 101) "seq.royal_dwarf_mining" else "seq.human_mining_rune_pickaxe_wall"

    private class MiningState(
        var target: BoundLocInfo? = null,
        var giveUpAt: Int = 0,
        var finishAt: Int = 0,
        var nextSwing: Int = 0,
        var resumeAt: Int = 0,
        var nextShout: Int,
        var nextRockfallCheck: Int = 0,
    )

    private data class MiningTarget(val loc: BoundLocInfo, val waypoints: List<CoordGrid>)

    internal companion object {
        val Miners = (1..12).map { "npc.motherlode_miner_$it" }
        private val minerIds by lazy { Miners.map { it.asRSCM() }.toSet() }
        private val veinIds by lazy { MotherlodeConstants.ORE_VEINS.map { it.asRSCM() }.toSet() }
        private val rockfallIds by lazy { MotherlodeConstants.ROCKFALLS.map { it.asRSCM() }.toSet() }
        private fun inside(coords: CoordGrid): Boolean = coords.level == 0 &&
            coords.x in MotherlodeConstants.SCAN_X1..MotherlodeConstants.SCAN_X2 &&
            coords.z in MotherlodeConstants.SCAN_Z1..MotherlodeConstants.SCAN_Z2
    }
}
