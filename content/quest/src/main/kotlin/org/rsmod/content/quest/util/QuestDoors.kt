package org.rsmod.content.quest.util

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.content.generic.locs.doors.DoorTranslations
import org.rsmod.content.generic.locs.gate.GateTranslations
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocInfo
import org.rsmod.map.CoordGrid

/**
 * Opens and closes quest doors and gates that carry no generic door content group. Opened
 * locs revert on their own after [DURATION] cycles.
 */
class QuestDoors @Inject constructor(private val locRepo: LocRepository) {

    /**
     * Swings a single door open. A door normally swings onto the tile inside the room; with
     * [outward] it swings the other way around the same hinge, onto the doorway tile itself.
     */
    fun open(
        access: ProtectedAccess,
        closed: BoundLocInfo,
        opened: String,
        sound: String = DOOR_OPEN,
        outward: Boolean = false,
    ) {
        access.soundSynth(sound)
        val coords =
            if (outward) closed.coords
            else DoorTranslations.translateOpen(closed.coords, closed.shape, closed.angle)
        locRepo.del(closed, DURATION)
        locRepo.add(coords, opened, DURATION, closed.turnAngle(1), closed.shape)
    }

    /**
     * Opens both leaves of a double door. [left] and [right] are the closed leaves; the open forms
     * are named explicitly because the quest doors carry no `next_loc_stage` param.
     */
    fun openDouble(
        access: ProtectedAccess,
        left: LocInfo?,
        leftOpened: String,
        right: LocInfo?,
        rightOpened: String,
        sound: String = DOOR_OPEN,
    ) {
        access.soundSynth(sound)
        left?.let {
            val coords = DoorTranslations.translateOpen(it.coords, it.shape, it.angle)
            locRepo.del(it, DURATION)
            locRepo.add(coords, leftOpened, DURATION, it.angle.turn(3), it.shape)
        }
        right?.let {
            val coords = DoorTranslations.translateOpen(it.coords, it.shape, it.angle)
            locRepo.del(it, DURATION)
            locRepo.add(coords, rightOpened, DURATION, it.angle.turn(1), it.shape)
        }
    }

    /**
     * Opens a gate pair. Each leaf swings on its own post, the way the metal gates do, unless
     * [symmetric] is false, in which case both fold onto the left post like a picket gate.
     */
    fun openGate(
        access: ProtectedAccess,
        left: LocInfo?,
        leftOpened: String,
        right: LocInfo?,
        rightOpened: String,
        sound: String = GATE_OPEN,
        symmetric: Boolean = true,
    ) {
        access.soundSynth(sound)
        left?.let {
            val coords = it.coords + GateTranslations.leftGateOpen(it.shape, it.angle)
            locRepo.del(it, DURATION)
            locRepo.add(coords, leftOpened, DURATION, it.angle.turn(3), it.shape)
        }
        right?.let {
            val coords =
                if (symmetric) it.coords + GateTranslations.leftGateOpen(it.shape, it.angle)
                else it.coords + GateTranslations.rightGateOpen(it.shape, it.angle)
            val angle = if (symmetric) it.angle.turn(1) else it.angle.turn(3)
            locRepo.del(it, DURATION)
            locRepo.add(coords, rightOpened, DURATION, angle, it.shape)
        }
    }

    /** The right half of a gate, found from the closed left half [left]. */
    fun rightOfGate(left: BoundLocInfo, rightType: String): LocInfo? =
        find(left.coords + GateTranslations.leftGateRightPair(left.shape, left.angle), rightType)

    /** The left half of a gate, found from the closed right half [right]. */
    fun leftOfGate(right: BoundLocInfo, leftType: String): LocInfo? =
        find(right.coords - GateTranslations.leftGateRightPair(right.shape, right.angle), leftType)

    /** The right leaf paired with the closed left leaf [left]. */
    fun rightLeafOf(left: BoundLocInfo, rightType: String): LocInfo? =
        find(DoorTranslations.translateClose(left.coords, left.shape, left.angle), rightType)

    /** The left leaf paired with the closed right leaf [right]. */
    fun leftLeafOf(right: BoundLocInfo, leftType: String): LocInfo? =
        find(
            DoorTranslations.translateCloseOpposite(right.coords, right.shape, right.angle),
            leftType,
        )

    fun find(coords: CoordGrid, type: String): LocInfo? =
        locRepo.findAll(coords).firstOrNull { it.id == type.asRSCM(RSCMType.LOC) }

    fun asInfo(loc: BoundLocInfo): LocInfo = LocInfo(loc.layer, loc.coords, loc.entity)

    companion object {
        const val DURATION = 100
        const val DOOR_OPEN = "synth.door_open"
        const val GATE_OPEN = "synth.picketgate_open"
    }
}
