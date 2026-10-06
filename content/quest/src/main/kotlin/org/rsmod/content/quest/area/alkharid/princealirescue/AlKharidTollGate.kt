package org.rsmod.content.quest.area.alkharid.princealirescue

import dev.openrune.types.MesAnimType
import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc4
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.Coins
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.StageAliEscaped
import org.rsmod.content.quest.manager.menu
import org.rsmod.game.entity.Npc
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal const val TollGateOpenTicks = 8

/**
 * The toll gate between Lumbridge and Al Kharid. Going east costs 10 coins until Prince Ali is out
 * of the jail, after which the gate just opens. Going west is always free and never speaks. The gate
 * is a pair of multilocs keyed on the quest varp: before the quest ends they show Open and
 * Pay-toll, from the escape onwards only Open.
 *
 * The closed leaves block the doorway, so both are swapped for open gates and stay that way for
 * [TollGateOpenTicks], which is long enough for the player to finish the step from either side.
 */
class AlKharidTollGate
@Inject
constructor(private val princeAli: PrinceAliRescueQuest, private val locRepo: LocRepository) :
    PluginScript() {

    override fun ScriptContext.startup() {
        for (leaf in listOf(LeftGate, RightGate)) {
            onOpLoc1(leaf) { open() }
            onOpLoc4(leaf) { payToll() }
        }
        for (guard in listOf(Guard1, Guard2)) {
            onOpNpc1(guard) { talkToGuard(it.npc) }
        }
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

    /** Runs the toll conversation and returns true when the player agreed to pay. */
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
            chatNpcSpecific("Border Guard", Guard1, mesanim, text)
        }
    }

    private suspend fun ProtectedAccess.payAndPass() {
        if (invDel(inv, Coins, TollPrice).failure) {
            startDialogue {
                chatPlayer(sad, "Oh dear, I don't actually seem to have enough money.")
            }
            return
        }
        passThrough()
    }

    private suspend fun ProtectedAccess.passThrough() {
        val eastbound = coords.x < GateX
        val z = coords.z.coerceIn(GateMinZ, GateMaxZ)
        swingOpen()
        playerWalkWithMinDelay(CoordGrid(if (eastbound) GateX else GateX - 1, z, 0))
    }
    private fun ProtectedAccess.swingOpen() {
        for (z in GateMinZ..GateMaxZ) {
            locRepo.findExact(CoordGrid(GateX, z, 0), LocShape.WallStraight)?.let {
                locRepo.del(it, TollGateOpenTicks)
            }
        }
        locRepo.add(
            CoordGrid(GateX - 1, GateMinZ, 0),
            OpenLeft,
            TollGateOpenTicks,
            LocAngle.South,
            LocShape.WallStraight,
        )
        locRepo.add(
            CoordGrid(GateX - 1, GateMaxZ, 0),
            OpenRight,
            TollGateOpenTicks,
            LocAngle.North,
            LocShape.WallStraight,
        )
        soundSynth("synth.iron_door_open")
    }

    private fun ProtectedAccess.isLeaving(): Boolean = coords.x >= GateX

    private fun ProtectedAccess.friendOfAlKharid(): Boolean =
        princeAli.stage(player) >= StageAliEscaped

    private enum class Toll {
        Pay,
        Who,
        Decline,
    }

    private companion object {
        const val LeftGate = "loc.kharidmetalgateclosedl"
        const val RightGate = "loc.kharidmetalgateclosedr"
        const val OpenLeft = "loc.inacmetalgateopenl"
        const val OpenRight = "loc.inacmetalgateopenr"
        const val Guard1 = "npc.borderguard1"
        const val Guard2 = "npc.borderguard2"

        const val GateX = 3268
        const val GateMinZ = 3227
        const val GateMaxZ = 3228
        const val TollPrice = 10
    }
}
