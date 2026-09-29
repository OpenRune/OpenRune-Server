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
        onOpNpc3(CUSTOMS_OFFICER) {
            if (inv.count(KARAMJA_RUM) > 0) {
                startDialogue(it.npc) { confiscateRum() }
                return@onOpNpc3
            }
            payAndSail(PORT_SARIM, PORT_SARIM_NAME)
        }
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
                chatNpc(neutral, "You need to be searched before you can board.")
                customsSearch()
            }
            CustomsTopic.Customs -> {
                chatPlayer(quiz, "What unusual customs do they have here?")
                chatNpc(neutral, "I'm not that sort of customs officer.")
            }
            CustomsTopic.Leave -> chatPlayer(neutral, "I'm good, thanks.")
        }
    }

    private suspend fun Dialogue.customsSearch() {
        while (true) {
            val choice =
                choice3(
                    "Why?",
                    1,
                    "Search away. I have nothing to hide.",
                    2,
                    "You're not putting your hands on my things!",
                    3,
                )
            when (choice) {
                1 -> {
                    chatPlayer(quiz, "Why?")
                    chatNpc(
                        neutral,
                        "Because Asgarnia has banned the import of intoxicating spirits.",
                    )
                }
                2 -> {
                    chatPlayer(neutral, "Search away. I have nothing to hide.")
                    if (player.inv.count(KARAMJA_RUM) > 0) {
                        confiscateRum()
                        return
                    }
                    chatNpc(
                        neutral,
                        "Well you've got some odd stuff, but it's all legal. Now you need to pay a " +
                            "boarding charge of $FARE coins.",
                    )
                    if (choice2("Okay.", true, "Oh, I'll not bother then.", false)) {
                        chatPlayer(neutral, "Okay.")
                        board(PORT_SARIM, PORT_SARIM_NAME)
                    } else {
                        chatPlayer(neutral, "Oh, I'll not bother then.")
                    }
                    return
                }
                else -> {
                    chatPlayer(angry, "You're not putting your hands on my things!")
                    chatNpc(neutral, "You're not getting on this ship then.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.confiscateRum() {
        chatNpc(angry, "Aha, trying to smuggle rum are we?")
        chatPlayer(shifty, "Umm... it's for personal use?")
        access.invDel(access.inv, KARAMJA_RUM, player.inv.count(KARAMJA_RUM))
        access.mes("The customs officer confiscates your rum.")
        access.mes("You will need to find some way to smuggle it off the island...")
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
        const val KARAMJA_RUM = "obj.karamja_rum"
        const val MUSA_POINT_NAME = "Musa Point"
        const val PORT_SARIM_NAME = "Port Sarim"

        val SARIM_CREW = listOf("npc.seaman_lorris", "npc.seaman_thresnor", "npc.captain_tobias")
        val MUSA_POINT = CoordGrid(2956, 3146, 0)
        val PORT_SARIM = CoordGrid(3029, 3217, 0)
    }
}
