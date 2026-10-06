package org.rsmod.content.areas.misc.stronghold_of_security

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.MesAnimType
import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.forcedWalk
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.generic.locs.doors.DoorTranslations
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class StrongholdDoors @Inject constructor(private val locRepo: LocRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        for (floor in StrongholdFloor.entries) {
            onOpLoc1(floor.face) { useDoor(floor, it.loc.toLocInfo()) }
            onOpLoc1(floor.mirror) { useDoor(floor, it.loc.toLocInfo()) }
        }
    }

    private suspend fun ProtectedAccess.useDoor(floor: StrongholdFloor, door: LocInfo) {
        if (asksQuestion(floor, door) && !passesQuestion(floor)) {
            return
        }
        val route = crossingRoute(player.coords, door)
        val tiles = crossingTiles(player.coords, route)
        openLeaves(floor, door, maxOf(MinOpenTicks, tiles + OpenTailTicks))
        soundSynth(DoorSound)
        if (route.isNotEmpty()) {
            forcedWalk(route, crossTiles = tiles)
        }
    }

    /**
     * A door only questions someone leaving the empty space between a pair of doors, so opening it
     * from a room of monsters is always immediate. That space is the side with another door of the
     * floor a few tiles behind it.
     */
    private fun ProtectedAccess.asksQuestion(floor: StrongholdFloor, door: LocInfo): Boolean {
        if (player.doorsStayQuiet(floor)) {
            return false
        }
        if (!locRepo.standsBetweenDoors(floor, door, door.isOnLocSide(player.coords))) {
            return false
        }
        return random.of(maxExclusive = QuestionSkipOdds) != 0
    }

    private suspend fun ProtectedAccess.passesQuestion(floor: StrongholdFloor): Boolean {
        val question = random.pick(SecurityQuestions.all)
        var passed = false
        startDialogue {
            say(floor, neutral, "${SecurityQuestions.PREAMBLE} ${question.text}")
            val answer = question.answers[answerMenu(question)]
            say(floor, if (answer.correct) happy else angry, answer.response)
            passed = answer.correct
        }
        return passed
    }

    private suspend fun Dialogue.answerMenu(question: SecurityQuestion): Int {
        val a = question.answers
        return when (a.size) {
            2 -> choice2(a[0].text, 0, a[1].text, 1)
            else -> choice3(a[0].text, 0, a[1].text, 1, a[2].text, 2)
        }
    }

    private suspend fun Dialogue.say(
        floor: StrongholdFloor,
        mesanim: MesAnimType,
        text: String,
    ) {
        for (part in text.splitForChatbox()) {
            chatNpcSpecific(floor.doorTitle, floor.doorNpc, mesanim, part)
        }
    }

    /**
     * Swaps both leaves of the doorway for their open forms. The map places the face and mirror on
     * either side of a doorway, so the leaf whose partner stands where the left leaf would close
     * to is the left one, and each swings out on its own side like the generic double doors.
     */
    private fun openLeaves(floor: StrongholdFloor, clicked: LocInfo, ticks: Int) {
        val faceId = floor.face.asRSCM(RSCMType.LOC)
        val partnerId = (if (clicked.id == faceId) floor.mirror else floor.face).asRSCM(RSCMType.LOC)
        fun partnerAt(coords: CoordGrid) =
            locRepo.findExact(coords, clicked.shape)?.takeIf { it.id == partnerId }

        val toRight = partnerAt(DoorTranslations.translateClose(clicked.coords, clicked.shape, clicked.angle))
        val toLeft =
            partnerAt(DoorTranslations.translateCloseOpposite(clicked.coords, clicked.shape, clicked.angle))
        val left = if (toLeft != null) toLeft else clicked
        val right = if (toLeft != null) clicked else toRight
        for ((leaf, rotations) in listOf(left to 3, right to 1)) {
            if (leaf == null) continue
            val open = if (leaf.id == faceId) floor.faceOpen else floor.mirrorOpen
            swap(leaf, open, leaf.turnAngle(rotations), ticks)
        }
    }

    private fun swap(closed: LocInfo, open: String, angle: LocAngle, ticks: Int) {
        val at = DoorTranslations.translateOpen(closed.coords, closed.shape, closed.angle)
        locRepo.del(closed, ticks)
        locRepo.add(at, open, ticks, angle, closed.shape)
    }

    internal companion object {
        const val DoorSound = "synth.door_open"
        const val MinOpenTicks = 4
        const val OpenTailTicks = 2
        const val MaxVestibuleDepth = 5
        const val QuestionSkipOdds = 4

        private const val ChatboxLimit = 200

        fun String.splitForChatbox(limit: Int = ChatboxLimit): List<String> {
            val parts = mutableListOf<String>()
            var current = StringBuilder()
            for (word in split(' ')) {
                if (current.isNotEmpty() && current.length + 1 + word.length > limit) {
                    parts += current.toString()
                    current = StringBuilder()
                }
                if (current.isNotEmpty()) current.append(' ')
                current.append(word)
            }
            if (current.isNotEmpty()) parts += current.toString()
            return parts
        }
    }
}

/**
 * Whether someone standing on [onLocSide] of [door] is in the empty space between a pair of doors,
 * which is the case when another door of the floor stands a few tiles behind them.
 */
internal fun LocRepository.standsBetweenDoors(
    floor: StrongholdFloor,
    door: LocInfo,
    onLocSide: Boolean,
): Boolean {
    val here = if (onLocSide) door.coords else door.acrossTile()
    val there = if (onLocSide) door.acrossTile() else door.coords
    val doorIds = floor.doorIds()
    return (1..StrongholdDoors.MaxVestibuleDepth).any { depth ->
        val tile = here.translate((here.x - there.x) * depth, (here.z - there.z) * depth)
        findAll(tile).any { it.id in doorIds }
    }
}

internal fun StrongholdFloor.doorIds(): Set<Int> =
    listOf(face, mirror, faceOpen, mirrorOpen).map { it.asRSCM(RSCMType.LOC) }.toSet()

internal fun BoundLocInfo.toLocInfo(): LocInfo = LocInfo(layer, coords, entity)

internal fun LocInfo.acrossTile(): CoordGrid =
    when (angle) {
        LocAngle.West -> coords.translateX(-1)
        LocAngle.North -> coords.translateZ(1)
        LocAngle.East -> coords.translateX(1)
        LocAngle.South -> coords.translateZ(-1)
    }

internal fun LocInfo.isOnLocSide(tile: CoordGrid): Boolean =
    when (angle) {
        LocAngle.West -> tile.x >= coords.x
        LocAngle.North -> tile.z <= coords.z
        LocAngle.East -> tile.x <= coords.x
        LocAngle.South -> tile.z >= coords.z
    }

internal fun crossingRoute(from: CoordGrid, wall: LocInfo): List<CoordGrid> {
    val near = if (wall.isOnLocSide(from)) wall.coords else wall.acrossTile()
    val far = if (near == wall.coords) wall.acrossTile() else wall.coords
    return listOf(near, far).dropWhile { it == from }
}

internal fun crossingTiles(from: CoordGrid, route: List<CoordGrid>): Int {
    var tiles = 0
    var previous = from
    for (waypoint in route) {
        tiles += previous.chebyshevDistance(waypoint)
        previous = waypoint
    }
    return maxOf(1, tiles)
}
