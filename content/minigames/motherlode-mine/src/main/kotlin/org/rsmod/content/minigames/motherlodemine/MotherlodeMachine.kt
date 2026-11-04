package org.rsmod.content.minigames.motherlodemine

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.MoveRestrict
import dev.openrune.types.NpcMode
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeConstants
import org.rsmod.game.entity.Npc
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.collision.removeLoc
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.routefinder.collision.CollisionFlagMap

@Singleton
class MotherlodeMachine
@Inject
constructor(
    private val locRepo: LocRepository,
    private val npcRepo: NpcRepository,
    private val random: GameRandom,
    private val collision: CollisionFlagMap,
) {
    private val struts = mutableListOf<StrutState>()
    private val pendingVeinRespawns = mutableListOf<PendingVeinRespawn>()
    private val floaters = mutableListOf<Npc>()
    private var discovered = false
    private var roundElapsedTicks = 0
    private var channelFlowing = false
    private val flowingWater by lazy { flowingWaterLocs() }

    val isWaterFlowing: Boolean
        get() = struts.any { !it.broken }

    var flowedThisTick: Boolean = false
        private set

    fun tick() {
        if (!discovered) {
            discover()
            discovered = true
        }
        processVeinRespawns()
        roundElapsedTicks++
        if (roundElapsedTicks >= MotherlodeConstants.STRUT_ROUND_TICKS) {
            roundElapsedTicks = 0
            for (state in struts) {
                if (state.broken) {
                    continue
                }
                val breakAt = state.breaksAfterRounds ?: continue
                state.roundsSinceRepair++
                if (state.roundsSinceRepair >= breakAt) {
                    breakStrut(state)
                }
            }
        }
        flowedThisTick = isWaterFlowing
        processFloaters()
    }

    fun scheduleVeinRespawn(vein: BoundLocInfo, veinKey: String) {
        if (pendingVeinRespawns.any { it.coords == vein.coords }) {
            return
        }
        pendingVeinRespawns +=
            PendingVeinRespawn(
                coords = vein.coords,
                veinKey = veinKey,
                angle = vein.angle,
                shape = vein.shape,
                ticksRemaining = MotherlodeConstants.VEIN_RESPAWN_TICKS,
            )
    }

    private fun processVeinRespawns() {
        if (pendingVeinRespawns.isEmpty()) {
            return
        }
        val iterator = pendingVeinRespawns.iterator()
        while (iterator.hasNext()) {
            val pending = iterator.next()
            pending.ticksRemaining--
            if (pending.ticksRemaining > 0) {
                continue
            }
            iterator.remove()
            locRepo.add(pending.coords, pending.veinKey, Int.MAX_VALUE, pending.angle, pending.shape)
        }
    }

    fun isStrutBroken(strutCoords: CoordGrid): Boolean =
        struts.firstOrNull { it.strut.coords == strutCoords }?.broken ?: false

    fun spawnPayDirtFloater() {
        if (floaters.size >= MotherlodeConstants.FLOATER_CAP) {
            return
        }
        val typeId = MotherlodeConstants.NPC_PAYDIRT_FLOATER.asRSCM(RSCMType.NPC)
        val type = ServerCacheManager.getNpc(typeId) ?: return
        val npc = Npc(type, MotherlodeConstants.FLOAT_CHANNEL.first())
        npc.moveRestrict = MoveRestrict.Blocked
        npc.mode = NpcMode.None
        npcRepo.add(npc, Int.MAX_VALUE)
        floaters += npc
    }

    private fun processFloaters() {
        if (floaters.isNotEmpty()) {
            val path = MotherlodeConstants.FLOAT_CHANNEL
            val iterator = floaters.iterator()
            while (iterator.hasNext()) {
                val npc = iterator.next()
                if (!npc.isSlotAssigned) {
                    iterator.remove()
                    continue
                }
                if (!isWaterFlowing) {
                    continue
                }
                val index = path.indexOf(npc.coords)
                when {
                    index == path.lastIndex -> {
                        npcRepo.del(npc, Int.MAX_VALUE)
                        iterator.remove()
                    }
                    index >= 0 -> npc.walk(path[index + 1])
                    else -> npc.walk(path.first())
                }
            }
        }
    }

    fun repairStrut(strutCoords: CoordGrid): Boolean {
        val state = struts.firstOrNull { it.strut.coords == strutCoords } ?: return false
        if (!state.broken) {
            return false
        }
        state.broken = false
        state.roundsSinceRepair = 0
        state.breaksAfterRounds =
            random.of(
                MotherlodeConstants.STRUT_BREAK_ROUNDS.first,
                MotherlodeConstants.STRUT_BREAK_ROUNDS.last,
            )
        updateWheel(state)
        updateWater()
        return true
    }

    private fun breakStrut(state: StrutState) {
        state.broken = true
        state.breaksAfterRounds = null
        updateWheel(state)
        updateWater()
    }

    private fun discover() {
        seedMineableVeins()
        val wheelFixedId = MotherlodeConstants.WHEEL_FIXED_LOC.asRSCM(RSCMType.LOC)
        val wheelBrokenId = MotherlodeConstants.WHEEL_BROKEN_LOC.asRSCM(RSCMType.LOC)
        val wheelLocs = findLocs(wheelFixedId, wheelBrokenId).sortedWith(coordOrder)

        struts.clear()
        for (wheel in wheelLocs) {
            val broken = wheel.id == wheelBrokenId
            // The mapped struts on the east side are decorative; repairs use the west supports.
            val strut = scenery(wheel.coords.translate(-1, 1), MotherlodeConstants.STRUT_FIXED_LOC)
            val state = StrutState(
                strut = strut,
                wheel = wheel,
                foam = scenery(wheel.coords.translate(0, 3), "loc.waterfall_foam", LocAngle.North),
                broken = broken,
                roundsSinceRepair = 0,
                breaksAfterRounds = if (broken) null else random.of(MotherlodeConstants.STRUT_BREAK_ROUNDS.first, MotherlodeConstants.STRUT_BREAK_ROUNDS.last),
            )
            struts += state
            updateWheel(state)
        }
        updateWater()
    }

    private fun updateWheel(state: StrutState) {
        val strutType = if (state.broken) MotherlodeConstants.STRUT_BROKEN_LOC else MotherlodeConstants.STRUT_FIXED_LOC
        locRepo.add(state.strut.copy(entity = state.strut.entity.copy(id = strutType.asRSCM())), Int.MAX_VALUE)
        val wheelType = if (state.broken) MotherlodeConstants.WHEEL_BROKEN_LOC else MotherlodeConstants.WHEEL_FIXED_LOC
        // The broken wheel model is authored facing the opposite direction from the animated one.
        val angle = if (state.broken) LocAngle.East else LocAngle.West
        locRepo.add(state.wheel.coords, wheelType, Int.MAX_VALUE, angle, state.wheel.shape)
        if (state.broken) locRepo.del(state.foam, Int.MAX_VALUE) else addWaterVisual(state.foam)
    }

    private fun updateWater() {
        val flowing = isWaterFlowing
        if (channelFlowing == flowing) return
        channelFlowing = flowing
        for (loc in flowingWater) {
            if (flowing) addWaterVisual(loc) else locRepo.del(loc, Int.MAX_VALUE)
        }
    }

    private fun addWaterVisual(loc: LocInfo) {
        if (locRepo.add(loc, Int.MAX_VALUE)) {
            // Native foam has scenery collision, but pay-dirt must float through it in this channel.
            collision.removeLoc(loc, checkNotNull(ServerCacheManager.getObject(loc.id)))
        }
    }

    private fun flowingWaterLocs(): List<LocInfo> = buildList {
        fun overlay(x: Int, z: Int, angle: LocAngle) {
            add(scenery(CoordGrid(x, z), "loc.waterfall_overlay_1_brighter", angle, LocShape.GroundDecor))
        }
        for (z in 5660..5671) overlay(3743, z, LocAngle.East)
        for (x in 3743..3747) overlay(x, 5672, LocAngle.South)
        for (z in 5661..5672) overlay(3748, z, LocAngle.West)
        for (x in 3744..3748) overlay(x, 5660, LocAngle.North)
        add(scenery(CoordGrid(3744, 5672), "loc.waterfall_foam_small", LocAngle.East))
        add(scenery(CoordGrid(3748, 5671), "loc.waterfall_foam_small", LocAngle.South))
        add(scenery(CoordGrid(3748, 5660), "loc.waterfall_foam_small"))
        add(scenery(CoordGrid(3744, 5660), "loc.waterfall_foam_small"))
    }

    private fun scenery(coords: CoordGrid, type: String, angle: LocAngle = LocAngle.West,
        shape: LocShape = LocShape.CentrepieceStraight): LocInfo {
        val layer = if (shape == LocShape.GroundDecor) 3 else 2
        return LocInfo(layer, coords, LocEntity(type.asRSCM(), shape.id, angle.id))
    }

    // Mineable overrides must not expire back to the depleted map state.
    private fun seedMineableVeins() {
        val mineableOfDepleted =
            MotherlodeConstants.DEPLETED_VEIN_OF.entries.associate { (mineable, depleted) ->
                depleted.asRSCM(RSCMType.LOC) to mineable
            }
        val depletedLocs = findLocs(*mineableOfDepleted.keys.toIntArray())
        for (depleted in depletedLocs) {
            locRepo.change(depleted, mineableOfDepleted.getValue(depleted.id), Int.MAX_VALUE)
        }
    }

    private fun findLocs(vararg ids: Int): List<BoundLocInfo> {
        val results = mutableListOf<BoundLocInfo>()
        for (level in 0..3) {
            var x = MotherlodeConstants.SCAN_X1
            while (x <= MotherlodeConstants.SCAN_X2) {
                var z = MotherlodeConstants.SCAN_Z1
                while (z <= MotherlodeConstants.SCAN_Z2) {
                    val zone = ZoneKey.fromAbsolute(x, z, level)
                    for (loc in locRepo.findAll(zone)) {
                        if (loc.id !in ids) {
                            continue
                        }
                        val type = ServerCacheManager.getObject(loc.id) ?: continue
                        results += BoundLocInfo(loc, type)
                    }
                    z += ZoneGrid.LENGTH
                }
                x += ZoneGrid.LENGTH
            }
        }
        return results
    }

    private val coordOrder =
        compareBy<BoundLocInfo>({ it.coords.level }, { it.coords.x }, { it.coords.z })

    private class StrutState(
        val strut: LocInfo,
        val wheel: BoundLocInfo,
        val foam: LocInfo,
        var broken: Boolean,
        var roundsSinceRepair: Int,
        var breaksAfterRounds: Int?,
    )

    private class PendingVeinRespawn(
        val coords: CoordGrid,
        val veinKey: String,
        val angle: LocAngle,
        val shape: LocShape,
        var ticksRemaining: Int,
    )
}
