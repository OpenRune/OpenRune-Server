package org.rsmod.content.skills.agility

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.BasType
import jakarta.inject.Inject
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.agilityLvl
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.table.agility.AgilityCoursesRow
import org.rsmod.content.skills.agility.WildernessDispenser.Proceed
import org.rsmod.content.skills.agility.WildernessDispenser.exitWarningOff
import org.rsmod.content.skills.agility.WildernessDispenser.feePaid
import org.rsmod.game.entity.util.EntityFaceAngle
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The ridge between the Wilderness Agility Course and the wolf pit
 * (https://oldschool.runescape.wiki/w/Wilderness_Agility_Course). The south gate walks the player
 * north along the ridge into the course and the north gates walk them back out, one tile a tick,
 * each way paying its xp. Leaving loses the dispenser fee, the loot and the streak. A slip drops
 * the player into the wolf pit unhurt.
 */
class WildernessRidgeScript @Inject constructor(private val locRepo: LocRepository) :
    PluginScript() {
    private val course by lazy { AgilityCoursesRow.getRow("dbrow.agility_course_wilderness") }

    override fun ScriptContext.startup() {
        onOpLoc1(SOUTH_GATE) { enter() }
        onOpLoc1(NORTH_GATE_LEFT) { leave() }
        onOpLoc1(NORTH_GATE_RIGHT) { leave() }
    }

    private suspend fun ProtectedAccess.enter() {
        if (!canCross()) {
            return
        }
        arriveDelay()
        cross(SOUTH_START, NORTH_PATH, northbound = true)
    }

    private suspend fun ProtectedAccess.leave() {
        if (!canCross()) {
            return
        }
        arriveDelay()
        if (player.feePaid && !player.exitWarningOff) {
            val proceed =
                choice3(
                    "Yes.",
                    Proceed.Yes,
                    "Yes, and don't ask again.",
                    Proceed.YesAlways,
                    "No.",
                    Proceed.No,
                    title = EXIT_WARNING,
                )
            when (proceed) {
                Proceed.No -> return
                Proceed.YesAlways -> player.exitWarningOff = true
                Proceed.Yes -> Unit
            }
        }
        cross(NORTH_START, SOUTH_PATH, northbound = false)
    }

    private fun ProtectedAccess.canCross(): Boolean {
        if (player.agilityLvl >= course.level) {
            return true
        }
        // UNVERIFIED: wording guessed.
        mes("You need an Agility level of ${course.level} to attempt balancing along the ridge.")
        return false
    }

    /**
     * Walks [path] from [start], a tile a tick, then off the far end. A slip on the way leaves the
     * player in the wolf pit with no xp.
     */
    private suspend fun ProtectedAccess.cross(
        start: CoordGrid,
        path: List<CoordGrid>,
        northbound: Boolean,
    ) {
        val slips = slips()
        val previousBas = player.bas
        try {
            mes("You go through the gate and try to edge over the ridge...", ChatType.Spam)
            soundSynth(DOOR_SYNTH)
            if (coords != start) {
                step(start)
                delay(1)
            }
            for ((index, tile) in path.withIndex()) {
                step(tile)
                when (index) {
                    0 -> if (northbound) openSouthGate() else openNorthGates()
                    1 -> balance()
                }
                if (slips && tile == SLIP_TILE) {
                    delay(1)
                    slip()
                    return
                }
                delay(1)
            }
            if (northbound) {
                mes("You skillfully balance across the ridge...", ChatType.Spam)
                restore(previousBas)
                step(NORTH_END)
                openNorthGates()
            } else {
                mes("You skillfully balance along the ridge...", ChatType.Spam)
                restore(previousBas)
                delay(1)
                step(SOUTH_END)
                openSouthGate()
                WildernessDispenser.exitCourse(player)
            }
            soundSynth(DOOR_SYNTH)
            statAdvance(AGILITY, RIDGE_XP)
        } finally {
            restore(previousBas)
        }
    }

    private fun ProtectedAccess.slips(): Boolean =
        !statRandom(AGILITY, SUCCESS_LOW, SUCCESS_HIGH, 0)

    // UNVERIFIED: where a southbound slip lands; the only capture slipped heading north.
    private suspend fun ProtectedAccess.slip() {
        mes("You lose your footing and fall into the wolf pit.", ChatType.Spam)
        anim(STUMBLE)
        player.pendingFaceAngle = EntityFaceAngle(SLIP_ANGLE)
        soundSynth(STUMBLE_SYNTH, loops = 10)
        delay(1)
        exactMove(coords, WOLF_PIT, delay1 = 0, delay2 = 25, FACE_EAST, TeleportType.Exempt)
    }

    private fun ProtectedAccess.step(tile: CoordGrid) {
        teleport(tile, TeleportType.Exempt)
    }

    // UNVERIFIED: the walk anim; captures show only that the appearance changes.
    private fun ProtectedAccess.balance() {
        val seq = BALANCE.asRSCM(RSCMType.SEQ)
        player.bas = BasType(-1, seq, seq, seq, seq, seq, seq, seq)
        rebuildAppearance()
    }

    private fun ProtectedAccess.restore(previousBas: BasType?) {
        if (player.bas !== previousBas) {
            player.bas = previousBas
            rebuildAppearance()
        }
    }

    private fun openSouthGate() {
        swingOpen(SOUTH_GATE_TILE, SOUTH_DOOR_TILE, "loc.inactiveprisondoor", LocAngle[0])
    }

    private fun openNorthGates() {
        swingOpen(NORTH_LEFT_TILE, NORTH_LEFT_DOOR_TILE, "loc.inacmetalgateopenl", LocAngle[2])
        swingOpen(NORTH_RIGHT_TILE, NORTH_RIGHT_DOOR_TILE, "loc.inacmetalgateopenr", LocAngle[0])
    }

    private fun swingOpen(gate: CoordGrid, door: CoordGrid, open: String, angle: LocAngle) {
        val closed = locRepo.findExact(gate, LocShape.WallStraight) ?: return
        locRepo.add(gate, INVISIBLE_WALL, OPEN_TICKS, closed.angle, closed.shape)
        locRepo.add(door, open, OPEN_TICKS, angle, LocShape.WallStraight)
    }

    private companion object {
        const val SOUTH_GATE = "loc.balancegate52a"
        const val NORTH_GATE_LEFT = "loc.balancegate52b_left"
        const val NORTH_GATE_RIGHT = "loc.balancegate52b_right"
        const val INVISIBLE_WALL = "loc.inviswall"
        const val AGILITY = "stat.agility"
        const val BALANCE = "seq.human_walk_logbalance"
        const val STUMBLE = "seq.human_walk_logbalance_stumble"
        const val DOOR_SYNTH = "synth.door_open"
        const val STUMBLE_SYNTH = "synth.stumble_loop"
        const val RIDGE_XP = 15.0
        const val OPEN_TICKS = 2
        const val SLIP_ANGLE = 1641
        const val FACE_EAST = 1536

        // UNVERIFIED: the success curve; 1 of 11 crossings slipped at 99.
        const val SUCCESS_LOW = 200
        const val SUCCESS_HIGH = 250
        const val EXIT_WARNING =
            "You will lose your payment and any unclaimed rewards if you exit.<br>Do you still " +
                "want to proceed?"

        val SOUTH_START = CoordGrid(2998, 3916, 0)
        val SOUTH_END = SOUTH_START
        val NORTH_START = CoordGrid(2998, 3931, 0)
        val NORTH_END = NORTH_START
        val SOUTH_GATE_TILE = CoordGrid(2998, 3917, 0)
        val SOUTH_DOOR_TILE = SOUTH_START
        val NORTH_LEFT_TILE = NORTH_START
        val NORTH_LEFT_DOOR_TILE = CoordGrid(2998, 3930, 0)
        val NORTH_RIGHT_TILE = CoordGrid(2997, 3931, 0)
        val NORTH_RIGHT_DOOR_TILE = CoordGrid(2997, 3930, 0)

        // UNVERIFIED: that every slip happens on this tile; the only capture slipped here.
        val SLIP_TILE = CoordGrid(2998, 3924, 0)
        val WOLF_PIT = CoordGrid(3001, 3923, 0)
        val NORTH_PATH = (3917..3930).map { CoordGrid(2998, it, 0) }
        val SOUTH_PATH = (3930 downTo 3918).map { CoordGrid(2998, it, 0) }
    }
}
