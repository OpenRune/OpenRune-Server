package org.rsmod.content.quest.area.alkharid.princealirescue

import dev.openrune.types.MesAnimType
import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc4
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.COINS
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.STAGE_ALI_ESCAPED
import org.rsmod.content.quest.manager.menu
import org.rsmod.game.entity.Npc
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal const val TOLL_GATE_OPEN_TICKS = 8

class AlKharidTollGate
@Inject
constructor(private val princeAli: PrinceAliRescueQuest, private val locRepo: LocRepository) :
    PluginScript() {

    override fun ScriptContext.startup() {
        bindLeaf(LEFT_GATE)
        bindLeaf(RIGHT_GATE)
        onOpNpc1(GUARD_1) { talkToGuard(it.npc) }
        onOpNpc1(GUARD_2) { talkToGuard(it.npc) }
    }

    private fun ScriptContext.bindLeaf(leaf: String) {
        onOpLoc1(leaf) { open() }
        onOpLoc4(leaf) { payToll() }
    }

    private suspend fun ProtectedAccess.open() {
        arriveDelay()
        if (isLeaving() || friendOfAlKharid()) {
            passThrough()
            return
        }
        var pay = false
        startDialogue { pay = askToll() }
        if (pay) {
            payAndPass()
        }
    }

    private suspend fun ProtectedAccess.payToll() {
        arriveDelay()
        if (isLeaving() || friendOfAlKharid()) {
            passThrough()
            return
        }
        payAndPass()
    }

    private suspend fun ProtectedAccess.talkToGuard(guard: Npc) {
        if (isLeaving()) {
            passThrough()
            return
        }
        if (friendOfAlKharid()) {
            startDialogue(guard) {
                chatPlayer(neutral, "Can I come through this gate?")
                chatNpc(neutral, "You may pass for free, you are a friend of Al-Kharid.")
            }
            passThrough()
            return
        }
        var pay = false
        startDialogue(guard) { pay = askToll() }
        if (pay) {
            payAndPass()
        }
    }

    private suspend fun Dialogue.askToll(): Boolean {
        chatPlayer(neutral, "Can I come through this gate?")
        speak(neutral, "You must pay a toll of 10 gold coins to pass.")
        return when (
            menu(
                "Yes, okay." to Toll.Pay,
                "Who does my money go to?" to Toll.Who,
                "No thank you, I'll walk around." to Toll.Decline,
            )
        ) {
            Toll.Pay -> {
                chatPlayer(neutral, "Yes, okay.")
                true
            }
            Toll.Who -> {
                chatPlayer(quiz, "Who does my money go to?")
                speak(neutral, "The money goes to the city of Al-Kharid.")
                false
            }
            Toll.Decline -> {
                chatPlayer(neutral, "No thank you, I'll walk around.")
                speak(neutral, "Ok suit yourself.")
                false
            }
        }
    }

    private suspend fun Dialogue.speak(mesanim: MesAnimType, text: String) {
        if (npc != null) {
            chatNpc(mesanim, text)
        } else {
            chatNpcSpecific("Border Guard", GUARD_1, mesanim, text)
        }
    }

    private suspend fun ProtectedAccess.payAndPass() {
        if (invDel(inv, COINS, TOLL_PRICE).failure) {
            startDialogue {
                chatPlayer(sad, "Oh dear, I don't actually seem to have enough money.")
            }
            return
        }
        passThrough()
    }

    private suspend fun ProtectedAccess.passThrough() {
        val eastbound = coords.x < GATE_X
        val z = coords.z.coerceIn(GATE_MIN_Z, GATE_MAX_Z)
        swingOpen()
        playerWalkWithMinDelay(CoordGrid(if (eastbound) GATE_X else GATE_X - 1, z, 0))
    }
    private fun ProtectedAccess.swingOpen() {
        for (z in GATE_MIN_Z..GATE_MAX_Z) {
            locRepo.findExact(CoordGrid(GATE_X, z, 0), LocShape.WallStraight)?.let {
                locRepo.del(it, TOLL_GATE_OPEN_TICKS)
            }
        }
        locRepo.add(
            CoordGrid(GATE_X - 1, GATE_MIN_Z, 0),
            OPEN_LEFT,
            TOLL_GATE_OPEN_TICKS,
            LocAngle.South,
            LocShape.WallStraight,
        )
        locRepo.add(
            CoordGrid(GATE_X - 1, GATE_MAX_Z, 0),
            OPEN_RIGHT,
            TOLL_GATE_OPEN_TICKS,
            LocAngle.North,
            LocShape.WallStraight,
        )
        soundSynth("synth.iron_door_open")
    }

    private fun ProtectedAccess.isLeaving(): Boolean = coords.x >= GATE_X

    private fun ProtectedAccess.friendOfAlKharid(): Boolean =
        princeAli.stage(player) >= STAGE_ALI_ESCAPED

    private enum class Toll {
        Pay,
        Who,
        Decline,
    }

    private companion object {
        const val LEFT_GATE = "loc.kharidmetalgateclosedl"
        const val RIGHT_GATE = "loc.kharidmetalgateclosedr"
        const val OPEN_LEFT = "loc.inacmetalgateopenl"
        const val OPEN_RIGHT = "loc.inacmetalgateopenr"
        const val GUARD_1 = "npc.borderguard1"
        const val GUARD_2 = "npc.borderguard2"

        const val GATE_X = 3268
        const val GATE_MIN_Z = 3227
        const val GATE_MAX_Z = 3228
        const val TOLL_PRICE = 10
    }
}
