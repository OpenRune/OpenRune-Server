package org.rsmod.content.areas.city.portsarim.travel

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private var Player.veosSarimVis: Int by intVarBit("varbit.veos_sarim_vis")
private var Player.veosPiscVis: Int by intVarBit("varbit.veos_pisc_vis")

private enum class VeosPort(val displayName: String, val arrival: CoordGrid) {
    PortSarim("Port Sarim", CoordGrid(3055, 3242, 1)),
    PortPiscarilius("Port Piscarilius", CoordGrid(1824, 3695, 1)),
    LandsEnd("Land's End", CoordGrid(1504, 3399, 0)),
}

class VeosScript : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerLogin {
            if (player.veosSarimVis == 0) player.veosSarimVis = VEOS_SARIM_TRAVEL
            if (player.veosPiscVis == 0) player.veosPiscVis = VEOS_PISC_TRAVEL
        }

        onOpNpc1(VEOS_SARIM) { startDialogue(it.npc) { veosAtSarim() } }
        onOpNpc3(VEOS_SARIM) { sail(VeosPort.PortPiscarilius) }
        onOpNpc4(VEOS_SARIM) { sail(VeosPort.LandsEnd) }

        onOpNpc1(VEOS_PISCARILIUS) { startDialogue(it.npc) { veosAtPiscarilius() } }
        onOpNpc3(VEOS_PISCARILIUS) { sail(VeosPort.PortSarim) }
        onOpNpc4(VEOS_PISCARILIUS) { sail(VeosPort.LandsEnd) }

        onOpLoc1("loc.sailing_sarim_veos_shipplank_off") { disembark(SARIM_QUAY) }
        onOpLoc1("loc.zeah_travel_kourend_shipplank_off") { disembark(PISCARILIUS_QUAY) }
    }

    private suspend fun Dialogue.veosAtSarim() {
        chatPlayer(happy, "Hello Veos.")
        chatNpc(quiz, "Hello there. What can I do for you?")
        val topic =
            choice3(
                "Can you take me somewhere?",
                VeosTopic.Travel,
                "Tell me more about Great Kourend.",
                VeosTopic.Kourend,
                "Nothing.",
                VeosTopic.Leave,
            )
        when (topic) {
            VeosTopic.Travel -> {
                chatPlayer(quiz, "Can you take me somewhere?")
                chooseDestination(VeosPort.PortPiscarilius, "As you wish.")
            }
            VeosTopic.Kourend -> {
                chatPlayer(quiz, "Tell me more about Great Kourend.")
                describeKourend()
                if (choice2("That's great, can you take me there please?", true, "Goodbye.", false)) {
                    chatPlayer(quiz, "That's great, can you take me there please?")
                    chooseDestination(VeosPort.PortPiscarilius, "As you wish.")
                } else {
                    chatPlayer(neutral, "Goodbye.")
                }
            }
            else -> chatPlayer(neutral, "Nothing.")
        }
    }

    private suspend fun Dialogue.veosAtPiscarilius() {
        chatNpc(quiz, "Hello again, ${player.displayName}! What can I do for you?")
        while (true) {
            val topic =
                choice4(
                    "Where am I exactly?",
                    VeosTopic.Where,
                    "Can you take me somewhere?",
                    VeosTopic.Travel,
                    "Could you tell me more about Kourend?",
                    VeosTopic.Kourend,
                    "Nothing, thanks.",
                    VeosTopic.Leave,
                )
            when (topic) {
                VeosTopic.Where -> whereAmI()
                VeosTopic.Travel -> {
                    chatPlayer(quiz, "Can you take me somewhere?")
                    return chooseDestination(VeosPort.PortSarim, LONG_VOYAGE)
                }
                VeosTopic.Kourend -> {
                    chatPlayer(quiz, "Could you tell me more about Kourend?")
                    describeKourend(fromKourend = true)
                }
                VeosTopic.Leave -> return chatPlayer(neutral, "Nothing, thanks.")
            }
        }
    }

    private suspend fun Dialogue.chooseDestination(mainland: VeosPort, farewell: String) {
        chatNpc(quiz, "Where would you like to go?")
        val dest =
            choice3(
                "Travel to ${mainland.displayName}.",
                mainland,
                "Travel to ${VeosPort.LandsEnd.displayName}.",
                VeosPort.LandsEnd,
                "Stay where you are.",
                null,
            )
        if (dest == null) {
            chatPlayer(neutral, "Actually I'd like to stay here.")
            chatNpc(neutral, "As you wish.")
            return
        }
        chatPlayer(happy, "I'd like to travel to ${dest.displayName}, please.")
        chatNpc(happy, farewell)
        access.sail(dest)
    }

    private suspend fun Dialogue.describeKourend(fromKourend: Boolean = false) {
        val opening =
            if (fromKourend) {
                "Certainly. The Kingdom of Great Kourend is made up of five main cities."
            } else {
                "Great Kourend is a magnificent kingdom comprising of five cities."
            }
        chatNpc(
            neutral,
            "$opening They are the cities of Arceuus, Lovakengj, Shayzien, Piscarilius and " +
                "Hosidius. Each city is ruled by one of the five houses of Kourend.",
        )
        chatNpc(
            neutral,
            "At one time, the kingdom as a whole was ruled over by a king or queen that the five " +
                "houses answered to.",
        )
        val king = if (fromKourend) "our" else "the"
        chatNpc(
            neutral,
            "However, since $king last King died 20 years ago, the kingdom has instead been ruled " +
                "by the Kourend Council.",
        )
    }

    private suspend fun Dialogue.whereAmI() {
        chatPlayer(quiz, "Where am I exactly?")
        chatNpc(
            neutral,
            "This is the port of Piscarilius. Home to Kourend's finest fisherman, but beware of " +
                "the thieves!",
        )
        chatPlayer(quiz, "Can you tell me about the other parts of Kourend?")
        chatNpc(
            neutral,
            "Certainly. To the west is the city of Arceuus, where they study magic at Kourend's " +
                "Grand Library.",
        )
        chatNpc(
            neutral,
            "Further west is Lovakengj, where you'll find our best miners and smithers working to " +
                "arm the Shayzien soldiers to the south west.",
        )
        chatNpc(
            neutral,
            "Finally, to the south you'll find Hosidius. They focus on agriculture to provide food " +
                "for all of Great Kourend.",
        )
        chatPlayer(happy, "Thanks.")
    }

    private suspend fun ProtectedAccess.sail(port: VeosPort) = sailVoyage(port.arrival, port.displayName)

    private suspend fun ProtectedAccess.disembark(quay: CoordGrid) {
        arriveDelay()
        telejump(quay)
    }

    private enum class VeosTopic {
        Where,
        Travel,
        Kourend,
        Leave,
    }

    private companion object {
        const val VEOS_SARIM = "npc.veos_sarim"
        const val VEOS_PISCARILIUS = "npc.veos"
        const val VEOS_SARIM_TRAVEL = 2
        const val VEOS_PISC_TRAVEL = 1
        const val LONG_VOYAGE = "As you wish, I hope you don't get seasick, it is a long voyage."

        val SARIM_QUAY = CoordGrid(3055, 3245, 0)
        val PISCARILIUS_QUAY = CoordGrid(1824, 3691, 0)
    }
}
