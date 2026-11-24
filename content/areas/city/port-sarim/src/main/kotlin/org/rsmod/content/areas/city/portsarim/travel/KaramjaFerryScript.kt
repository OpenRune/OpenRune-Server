package org.rsmod.content.areas.city.portsarim.travel

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.quest.area.portsarim.piratestreasure.PiratesTreasureQuest
import org.rsmod.content.quest.area.portsarim.piratestreasure.confiscateRum
import org.rsmod.content.quest.manager.menu
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class KaramjaFerryScript @Inject constructor(private val piratesTreasure: PiratesTreasureQuest) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (sailor in SARIM_CREW) {
            onOpNpc1(sailor) { startDialogue(it.npc) { offerTrip() } }
            onOpNpc3(sailor) { payAndSail(MUSA_POINT, MUSA_POINT_NAME) }
        }
        onOpNpc1(CUSTOMS_OFFICER) { startDialogue(it.npc) { customsOfficer() } }
        onOpNpc3(CUSTOMS_OFFICER) { payCustomsAndSail() }
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
        val journey =
            choice2(
                "Can I journey on this ship?",
                true,
                "Does Karamja have unusual customs then?",
                false,
            )
        if (!journey) {
            chatPlayer(quiz, "Does Karamja have any unusual customs then?")
            chatNpc(neutral, "I'm not that sort of customs officer.")
            return
        }
        chatPlayer(quiz, "Can I journey on this ship?")
        if (piratesTreasure.stage(player) >= PiratesTreasureQuest.STAGE_COMPLETE) {
            plantationWorkerSearch()
        } else {
            customsSearch()
        }
    }

    private suspend fun Dialogue.customsSearch() {
        chatNpc(neutral, "You need to be searched before you can board.")
        while (true) {
            val topic =
                menu(
                    "Why?" to SearchTopic.Why,
                    "Search away, I have nothing to hide." to SearchTopic.Search,
                    "You're not putting your hands on my things!" to SearchTopic.Refuse,
                )
            when (topic) {
                SearchTopic.Why -> {
                    chatPlayer(quiz, "Why?")
                    chatNpc(neutral, "Because Asgarnia has banned the import of intoxicating spirits.")
                }
                SearchTopic.Search -> {
                    chatPlayer(neutral, "Search away, I have nothing to hide.")
                    if (access.inv.count(RUM) > 0) {
                        chatNpc(angry, "Aha, trying to smuggle rum are we?")
                        chatPlayer(shifty, "Umm... it's for personal use?")
                        confiscateRum(access)
                        access.mes("The customs officer confiscates your rum.")
                        access.mes("You will need to find some way to smuggle it off the island...")
                        return
                    }
                    chatNpc(
                        neutral,
                        "Well you've got some odd stuff, but it's all legal. Now you need to pay a " +
                            "boarding charge of $FARE coins.",
                    )
                    return offerBoarding()
                }
                SearchTopic.Refuse -> {
                    chatPlayer(angry, "You're not putting your hands on my things!")
                    chatNpc(neutral, "You're not getting on this ship then.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.plantationWorkerSearch() {
        if (access.inv.count(RUM) > 0) {
            chatNpc(neutral, "Spot inspection. You don't mind do you?")
            chatNpc(angry, "Aha, trying to smuggle rum eh?")
            chatPlayer(shifty, "Umm... it's for personal use?")
            confiscateRum(access)
            access.mes("The customs officer confiscates your rum.")
            return
        }
        chatNpc(happy, "Hey, I know you, you work at the plantation.")
        chatNpc(
            neutral,
            "I don't think you'll try smuggling anything, you just need to pay a boarding charge " +
                "of $FARE coins.",
        )
        offerBoarding()
    }

    private suspend fun Dialogue.offerBoarding() {
        if (choice2("Ok.", true, "Oh, I'll not bother then.", false)) {
            chatPlayer(neutral, "Ok.")
            board(PORT_SARIM, PORT_SARIM_NAME)
        } else {
            chatPlayer(neutral, "Oh, I'll not bother then.")
        }
    }

    private suspend fun Dialogue.board(dest: CoordGrid, destName: String) {
        if (!access.payFare(FARE)) {
            chatPlayer(sad, "Oh dear, I don't seem to have enough money.")
            return
        }
        access.sailFerry(dest, destName, FARE)
    }

    private suspend fun ProtectedAccess.payCustomsAndSail() {
        if (confiscateRum(this) > 0) {
            mes("The customs officer confiscates your rum.")
            return
        }
        payAndSail(PORT_SARIM, PORT_SARIM_NAME)
    }

    private suspend fun ProtectedAccess.payAndSail(dest: CoordGrid, destName: String) {
        if (!payFare(FARE)) {
            mes("You do not have enough coins to pay passage, you need $FARE.")
            return
        }
        sailFerry(dest, destName, FARE)
    }

    private enum class SearchTopic {
        Why,
        Search,
        Refuse,
    }

    private companion object {
        const val FARE = 30
        const val CUSTOMS_OFFICER = "npc.customs_officer"
        const val RUM = "obj.karamja_rum"
        const val MUSA_POINT_NAME = "Musa Point"
        const val PORT_SARIM_NAME = "Port Sarim"

        val SARIM_CREW = listOf("npc.seaman_lorris", "npc.seaman_thresnor", "npc.captain_tobias")
        val MUSA_POINT = CoordGrid(2956, 3146, 0)
        val PORT_SARIM = CoordGrid(3029, 3217, 0)
    }
}
