package org.rsmod.content.areas.city.portsarim.travel

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class KaramjaFerryScript : PluginScript() {
    override fun ScriptContext.startup() {
        for (sailor in SARIM_CREW) {
            onOpNpc1(sailor) { startDialogue(it.npc) { offerTrip() } }
            onOpNpc3(sailor) { payAndSail(MUSA_POINT, MUSA_POINT_NAME) }
        }
        onOpNpc1(CUSTOMS_OFFICER) { startDialogue(it.npc) { customsOfficer() } }
        onOpNpc3(CUSTOMS_OFFICER) { payAndSail(PORT_SARIM, PORT_SARIM_NAME) }
    }

    private suspend fun Dialogue.offerTrip() {
        chatNpc(
            quiz,
            "Hello there. Do you want to go on a trip to Karamja? We can take you to Musa Point " +
                "for only $FARE coins.",
        )
        if (!choice2("Yes please.", true, "No thank you.", false)) {
            chatPlayer(neutral, "No thank you.")
            chatNpc(neutral, "Fair enough.")
            return
        }
        chatPlayer(happy, "Yes please.")
        board(MUSA_POINT, MUSA_POINT_NAME)
    }

    private suspend fun Dialogue.customsOfficer() {
        chatNpc(neutral, "Can I help you?")
        val topic =
            choice3(
                "Can I journey on this ship?",
                CustomsTopic.Journey,
                "What unusual customs do they have here?",
                CustomsTopic.Customs,
                "I'm good, thanks.",
                CustomsTopic.Leave,
            )
        when (topic) {
            CustomsTopic.Journey -> {
                chatPlayer(quiz, "Can I journey on this ship?")
                chatNpc(
                    neutral,
                    "You can, but you'll need to pay a boarding charge of $FARE coins.",
                )
                if (choice2("Okay.", true, "Oh, I'll not bother then.", false)) {
                    chatPlayer(neutral, "Okay.")
                    board(PORT_SARIM, PORT_SARIM_NAME)
                } else {
                    chatPlayer(neutral, "Oh, I'll not bother then.")
                }
            }
            CustomsTopic.Customs -> {
                chatPlayer(quiz, "What unusual customs do they have here?")
                chatNpc(neutral, "I'm not that sort of customs officer.")
            }
            CustomsTopic.Leave -> chatPlayer(neutral, "I'm good, thanks.")
        }
    }

    private suspend fun Dialogue.board(dest: CoordGrid, destName: String) {
        if (!access.payFare(FARE)) {
            chatPlayer(sad, "Oh dear, I don't seem to have enough money.")
            return
        }
        access.sailFerry(dest, destName, FARE)
    }

    private suspend fun ProtectedAccess.payAndSail(dest: CoordGrid, destName: String) {
        if (!payFare(FARE)) {
            mes("You do not have enough coins to pay passage, you need $FARE.")
            return
        }
        sailFerry(dest, destName, FARE)
    }

    private enum class CustomsTopic {
        Journey,
        Customs,
        Leave,
    }

    private companion object {
        const val FARE = 30
        const val CUSTOMS_OFFICER = "npc.customs_officer"
        const val MUSA_POINT_NAME = "Musa Point"
        const val PORT_SARIM_NAME = "Port Sarim"

        val SARIM_CREW = listOf("npc.seaman_lorris", "npc.seaman_thresnor", "npc.captain_tobias")
        val MUSA_POINT = CoordGrid(2956, 3146, 0)
        val PORT_SARIM = CoordGrid(3029, 3217, 0)
    }
}
