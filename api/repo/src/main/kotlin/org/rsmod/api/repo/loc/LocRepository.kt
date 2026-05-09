package org.rsmod.api.repo.loc

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ObjectServerType
import jakarta.inject.Inject
import org.rsmod.api.registry.loc.LocRegistry
import org.rsmod.api.registry.loc.LocRegistryResult
import org.rsmod.api.registry.loc.isSuccess
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.game.MapClock
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.routefinder.loc.LocLayerConstants

public class LocRepository
@Inject
constructor(
    private val mapClock: MapClock,
    private val locReg: LocRegistry,
    private val regionReg: RegionRegistry,
) {
    private val addDurations = ArrayDeque<LocCycleDuration>()
    private val delDurations = ArrayDeque<LocCycleDuration>()

    /**
     * Spawns [loc] and schedules its removal after [duration] map cycles when the spawn is
     * timer-driven.
     *
     * @param onDespawn Invoked on the first [MapClock] tick **after** this loc is removed when the
     *   timer elapses, and only if the removal actually runs (region validator still valid). Never
     *   runs when [duration] is [Int.MAX_VALUE], when the add result is not a timed spawn, or when
     *   the registry add fails.
     */
    public fun add(loc: LocInfo, duration: Int, onDespawn: (() -> Unit)? = null): Boolean {
        val add = locReg.add(loc)

        if (!add.isSuccess()) {
            return false
        }

        // Replacing a timed spawn with a different loc id (same tile/layer) used to leave the old
        // despawn entry in [addDurations] when the new [locReg.add] returned [NormalMapLoc], so the
        // stale timer could never remove the visible loc and no new timer was scheduled.
        clearTimedDespawnAt(loc.coords, loc.layer)

        if (add.shouldDespawn() && duration != Int.MAX_VALUE) {
            val revertCycle = mapClock + duration
            val validator = add.regionValidator()
            val locDuration = LocCycleDuration(loc, revertCycle, validator, onDespawn)
            delDurations.removeExisting(loc)
            addDurations.add(locDuration)
        }

        return true
    }

    @Deprecated("rather than passing the type we should be migrating to using the rscm name.")
    public fun add(
        coords: CoordGrid,
        type: ObjectServerType,
        duration: Int,
        angle: LocAngle,
        shape: LocShape,
        onDespawn: (() -> Unit)? = null,
    ): LocInfo {
        val layer = LocLayerConstants.of(shape.id)
        val entity = LocEntity(type.id, shape.id, angle.id)
        val loc = LocInfo(layer, coords, entity)
        add(loc, duration, onDespawn)
        return loc
    }

    /**
     * @param onDespawn See [add].
     */
    public fun add(
        coords: CoordGrid,
        internal: String,
        duration: Int,
        angle: LocAngle,
        shape: LocShape,
        onDespawn: (() -> Unit)? = null,
    ): LocInfo {
        val layer = LocLayerConstants.of(shape.id)
        val entity = LocEntity(internal.asRSCM(RSCMType.LOC), shape.id, angle.id)
        val loc = LocInfo(layer, coords, entity)
        add(loc, duration, onDespawn)
        return loc
    }

    public fun del(loc: LocInfo, duration: Int): Boolean {
        val delete = locReg.del(loc)

        if (!delete.isSuccess()) {
            return false
        }

        if (delete.canRespawn() && duration != Int.MAX_VALUE) {
            val revertCycle = mapClock + duration
            val validator = delete.regionValidator()
            val locDuration = LocCycleDuration(loc, revertCycle, validator)
            addDurations.removeExisting(loc)
            delDurations.removeExisting(loc)
            delDurations.add(locDuration)
        }

        return true
    }

    public fun del(bound: BoundLocInfo, duration: Int): Boolean {
        val loc = LocInfo(bound.layer, bound.coords, bound.entity)
        return del(loc, duration)
    }

    public fun change(from: LocInfo, into: ObjectServerType, duration: Int) {
        add(from.coords, into, duration, from.angle, from.shape)
    }

    public fun change(from: BoundLocInfo, into: ObjectServerType, duration: Int) {
        add(from.coords, into, duration, from.angle, from.shape)
    }

    public fun change(from: BoundLocInfo, internal: String, duration: Int) {
        add(from.coords, internal, duration, from.angle, from.shape)
    }

    private fun ArrayDeque<LocCycleDuration>.removeExisting(loc: LocInfo) {
        val iterator = iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (next.loc.coords == loc.coords && next.loc.entity == loc.entity) {
                iterator.remove()
                break
            }
        }
    }

    private fun clearTimedDespawnAt(coords: CoordGrid, layer: Int) {
        val iterator = addDurations.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (next.loc.coords == coords && next.loc.layer == layer) {
                iterator.remove()
            }
        }
    }

    public fun findAll(zone: ZoneKey): Sequence<LocInfo> = locReg.findAll(zone)

    public fun findAll(coords: CoordGrid): Sequence<LocInfo> =
        findAll(ZoneKey.from(coords)).filter { it.coords == coords }


    public fun findLoc(coords: CoordGrid, type: String): Boolean =
        locReg.findType(coords, type.asRSCM(RSCMType.LOC)) != null

    public fun findExact(coords: CoordGrid, type: ObjectServerType): LocInfo? =
        locReg.findType(coords, type.id)

    public fun findExact(coords: CoordGrid, shape: LocShape): LocInfo? =
        locReg.findShape(coords, shape.id)

    public fun findExact(coords: CoordGrid, content: String, shape: LocShape): LocInfo? {
        val loc = locReg.findShape(coords, shape.id) ?: return null
        return loc.takeIf { ServerCacheManager.getObject(it.id)?.contentGroup == content.asRSCM(RSCMType.CONTENT) }
    }

    public fun findExact(
        coords: CoordGrid,
        content: String,
        type: ObjectServerType,
    ): LocInfo? {
        val loc = locReg.findType(coords, type.id) ?: return null
        return loc.takeIf { ServerCacheManager.getObject(it.id)?.contentGroup == content.asRSCM(RSCMType.CONTENT) }
    }

    internal fun processDurations() {
        if (delDurations.isNotEmpty()) {
            processDelDurations()
        }
        if (addDurations.isNotEmpty()) {
            processAddDurations()
        }
    }

    private fun processDelDurations() {
        val iterator = delDurations.iterator()
        while (iterator.hasNext()) {
            val duration = iterator.next()
            if (!duration.shouldTrigger()) {
                continue
            }
            if (duration.isValid()) {
                locReg.add(duration.loc)
                duration.onTrigger?.invoke()
            }
            iterator.remove()
        }
    }

    private fun processAddDurations() {
        val iterator = addDurations.iterator()
        while (iterator.hasNext()) {
            val duration = iterator.next()
            if (!duration.shouldTrigger()) {
                continue
            }
            if (duration.isValid()) {
                locReg.del(duration.loc)
                duration.onTrigger?.invoke()
            }
            iterator.remove()
        }
    }

    private fun LocCycleDuration.shouldTrigger(): Boolean = mapClock >= triggerCycle

    private fun LocCycleDuration.isValid(): Boolean {
        val validator = regionValidator ?: return true
        val (slot, uid) = validator
        return regionReg.isValid(slot, uid)
    }

    /**
     * @param onTrigger For [addDurations], runs after the spawned loc is deleted when the timer
     *   fires. For [delDurations], runs after the map loc is restored when the timer fires. Only
     *   invoked when [isValid] is true.
     */
    private data class LocCycleDuration(
        val loc: LocInfo,
        val triggerCycle: Int,
        val regionValidator: RegionValidator?,
        val onTrigger: (() -> Unit)? = null,
    )

    private data class RegionValidator(val slot: Int, val uid: Int)

    private companion object {
        private fun LocRegistryResult.Add.Success.shouldDespawn(): Boolean =
            this is LocRegistryResult.Add.NormalSpawned ||
                this is LocRegistryResult.Add.RegionSpawned

        private fun LocRegistryResult.Delete.Success.canRespawn(): Boolean =
            this is LocRegistryResult.Delete.NormalMapLoc ||
                this is LocRegistryResult.Delete.RegionMapLoc

        private fun LocRegistryResult.Add.Success.regionValidator(): RegionValidator? =
            if (this is LocRegistryResult.Add.RegionSuccess) {
                RegionValidator(regionSlot, regionUid)
            } else {
                null
            }

        private fun LocRegistryResult.Delete.Success.regionValidator(): RegionValidator? =
            if (this is LocRegistryResult.Delete.RegionSuccess) {
                RegionValidator(regionSlot, regionUid)
            } else {
                null
            }
    }
}
