package org.rsmod.content.areas.city.portsarim.travel

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.content.quest.area.lumbridge.xmarksthespot.npcs.Veos
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private var Player.metVeosInKourend: Boolean by boolVarBit("varbit.cluequest_veos_already_met")
private var Player.veosPiscVis: Int by intVarBit("varbit.veos_pisc_vis")

private enum class VeosPort(val displayName: String, val arrival: CoordGrid) {
    PortSarim("Port Sarim", CoordGrid(3055, 3242, 1)),
    PortPiscarilius("Port Piscarilius", CoordGrid(1824, 3695, 1)),
    LandsEnd("Land's End", CoordGrid(1504, 3399, 0)),
}

class VeosScript @Inject constructor(private val veos: Veos) : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerLogin {
            if (player.veosPiscVis == 0) player.veosPiscVis = VEOS_PISC_TRAVEL
        }

        onOpNpc1(VEOS_SARIM) {
            startDialogue(it.npc) { if (!with(veos) { veosSarimQuest() }) veosAtSarim() }
        }
        onOpNpc3(VEOS_SARIM) { sail(VeosPort.PortPiscarilius) }
        onOpNpc4(VEOS_SARIM) { sail(VeosPort.LandsEnd) }

        onOpNpc1(VEOS_PISCARILIUS) {
            player.metVeosInKourend = true
            startDialogue(it.npc) { veosAtPiscarilius() }
        }
        onOpNpc3(VEOS_PISCARILIUS) { sail(VeosPort.PortSarim) }
        onOpNpc4(VEOS_PISCARILIUS) { sail(VeosPort.LandsEnd) }

        onOpLoc1("loc.sailing_sarim_veos_shipplank_off") { disembark(SARIM_QUAY) }
        onOpLoc1("loc.zeah_travel_kourend_shipplank_off") { disembark(PISCARILIUS_QUAY) }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Veos
    private suspend fun Dialogue.veosAtSarim() {
        if (!player.metVeosInKourend) {
            return veosFirstMeeting()
        }
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
                chatNpc(quiz, "Where would you like to go?")
                val dest =
                    choice3(
                        "I'd like to travel to Port Piscarilius, please.",
                        VeosPort.PortPiscarilius,
                        "I'd like to travel to Land's End, please.",
                        VeosPort.LandsEnd,
                        "Actually I'd like to stay here.",
                        null,
                    )
                travelTo(dest)
            }
            VeosTopic.Kourend -> greatKourend()
            else -> chatPlayer(neutral, "Nothing.")
        }
    }

    private suspend fun Dialogue.veosFirstMeeting() {
        chatPlayer(quiz, "Hello, who are you?")
        chatNpc(
            neutral,
            "I am Veos and I've just arrived in Port Sarim from the distant Kingdom of Great " +
                "Kourend.",
        )
        val topic =
            choice3(
                "Where's that?",
                VeosTopic.Where,
                "That's great, can you take me there please?",
                VeosTopic.Travel,
                "Goodbye.",
                VeosTopic.Leave,
            )
        when (topic) {
            VeosTopic.Where -> {
                chatPlayer(quiz, "Where's that?")
                chatNpc(neutral, "Across the sea, to the far west.")
                val next =
                    choice3(
                        "That's great, can you take me there please?",
                        VeosTopic.Travel,
                        "Tell me more about Great Kourend.",
                        VeosTopic.Kourend,
                        "Goodbye.",
                        VeosTopic.Leave,
                    )
                when (next) {
                    VeosTopic.Travel -> takeMeThere()
                    VeosTopic.Kourend -> greatKourend()
                    else -> chatPlayer(neutral, "Goodbye.")
                }
            }
            VeosTopic.Travel -> takeMeThere()
            else -> chatPlayer(neutral, "Goodbye.")
        }
    }

    private suspend fun Dialogue.greatKourend() {
        chatPlayer(quiz, "Tell me more about Great Kourend.")
        describeKourend()
        if (choice2("That's great, can you take me there please?", true, "Goodbye.", false)) {
            takeMeThere()
        } else {
            chatPlayer(neutral, "Goodbye.")
        }
    }

    private suspend fun Dialogue.takeMeThere() {
        chatPlayer(quiz, "That's great, can you take me there please?")
        chatNpc(happy, "Certainly! I hope you don't get seasick, it is a long voyage.")
        player.metVeosInKourend = true
        access.sail(VeosPort.PortPiscarilius)
    }

    private suspend fun Dialogue.travelTo(dest: VeosPort?) {
        if (dest == null) {
            chatPlayer(neutral, "Actually I'd like to stay here.")
            chatNpc(neutral, "As you wish.")
            return
        }
        chatPlayer(happy, "I'd like to travel to ${dest.displayName}, please.")
        chatNpc(happy, LONG_VOYAGE)
        access.sail(dest)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Veos
    // TODO(content): [quest:client_of_kourend] "Let's talk about your client..." and "I've lost
    // something you've given me."
    private suspend fun Dialogue.veosAtPiscarilius() {
        chatNpc(quiz, "Hello again, ${player.displayName}! What can I do for you?")
        var topic =
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
        while (true) {
            topic =
                when (topic) {
                    VeosTopic.Where -> {
                        whereAmI()
                        choice3(
                            "Can you take me somewhere?",
                            VeosTopic.Travel,
                            "Could you tell me more about Kourend?",
                            VeosTopic.Kourend,
                            "Goodbye.",
                            VeosTopic.Goodbye,
                        )
                    }
                    VeosTopic.Travel -> {
                        chatPlayer(quiz, "Can you take me somewhere?")
                        chatNpc(quiz, "Where would you like to go?")
                        val dest =
                            choice3(
                                "Travel to Port Sarim.",
                                VeosPort.PortSarim,
                                "Travel to Land's End.",
                                VeosPort.LandsEnd,
                                "Stay where you are.",
                                null,
                            )
                        return travelTo(dest)
                    }
                    VeosTopic.Kourend -> {
                        chatPlayer(quiz, "Could you tell me more about Kourend?")
                        describeKourend(fromKourend = true)
                        choice3(
                            "Where am I exactly?",
                            VeosTopic.Where,
                            "Can you take me somewhere?",
                            VeosTopic.Travel,
                            "Goodbye.",
                            VeosTopic.Goodbye,
                        )
                    }
                    VeosTopic.Goodbye -> return chatPlayer(neutral, "Goodbye.")
                    VeosTopic.Leave -> return chatPlayer(neutral, "Nothing, thanks.")
                }
        }
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
        Goodbye,
        Leave,
    }

    private companion object {
        const val VEOS_SARIM = "npc.veos_sarim"
        const val VEOS_PISCARILIUS = "npc.veos"
        const val VEOS_PISC_TRAVEL = 1
        const val LONG_VOYAGE = "As you wish, I hope you don't get seasick, it is a long voyage."

        val SARIM_QUAY = CoordGrid(3055, 3245, 0)
        val PISCARILIUS_QUAY = CoordGrid(1824, 3691, 0)
    }
}
