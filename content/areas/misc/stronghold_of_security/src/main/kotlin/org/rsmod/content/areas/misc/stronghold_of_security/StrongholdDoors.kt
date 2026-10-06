package org.rsmod.content.areas.misc.stronghold_of_security

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.MesAnimType
import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
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
        soundSynth(DoorSound)
        telejump(door.otherSide(player.coords))
    }

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

    internal companion object {
        const val DoorSound = "synth.door_open"
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

/** Only the second door of a pair asks, so leaving a room of monsters is never held up. */
internal fun LocRepository.standsBetweenDoors(
    floor: StrongholdFloor,
    door: LocInfo,
    onLocSide: Boolean,
): Boolean {
    val here = if (onLocSide) door.coords else door.acrossTile()
    val there = if (onLocSide) door.acrossTile() else door.coords
    val doorIds = setOf(floor.face, floor.mirror).map { it.asRSCM(RSCMType.LOC) }.toSet()
    return (1..StrongholdDoors.MaxVestibuleDepth).any { depth ->
        val tile = here.translate((here.x - there.x) * depth, (here.z - there.z) * depth)
        findAll(tile).any { it.id in doorIds }
    }
}

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

internal fun LocInfo.otherSide(from: CoordGrid): CoordGrid =
    if (isOnLocSide(from)) acrossTile() else coords
