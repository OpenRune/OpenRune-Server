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

class QuestDoors @Inject constructor(private val locRepo: LocRepository) {

    /** [outward] swings the door the other way around its hinge, onto the doorway tile. */
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

    /** The open forms are named because quest doors carry no `next_loc_stage` param. */
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

    /** Unless [symmetric], both leaves fold onto the left post like a picket gate. */
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

    fun rightOfGate(left: BoundLocInfo, rightType: String): LocInfo? =
        find(left.coords + GateTranslations.leftGateRightPair(left.shape, left.angle), rightType)

    fun leftOfGate(right: BoundLocInfo, leftType: String): LocInfo? =
        find(right.coords - GateTranslations.leftGateRightPair(right.shape, right.angle), leftType)

    fun rightLeafOf(left: BoundLocInfo, rightType: String): LocInfo? =
        find(DoorTranslations.translateClose(left.coords, left.shape, left.angle), rightType)

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
