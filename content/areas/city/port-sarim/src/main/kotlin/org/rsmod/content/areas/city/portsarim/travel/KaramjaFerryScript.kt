package org.rsmod.content.areas.city.portsarim.travel

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:pandemonium] the Pandemonium stop after the quest
// TODO(content): [diary:karamja] half fares with Karamja gloves
// TODO(content): [system:ring_of_charos] the charm options
class KaramjaFerryScript : PluginScript() {
    override fun ScriptContext.startup() {
        for (sailor in SARIM_CREW) {
            onOpNpc1(sailor) { startDialogue(it.npc) { offerTrip() } }
            onOpNpc3(sailor) { payAndSail(MUSA_POINT, MUSA_POINT_NAME) }
        }
        onOpNpc1(CUSTOMS_OFFICER) { startDialogue(it.npc) { customsOfficer() } }
        onOpNpc3(CUSTOMS_OFFICER) { payAndSail(PORT_SARIM, PORT_SARIM_NAME) }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Captain_Tobias
    // https://oldschool.runescape.wiki/w/Transcript:Seaman_Lorris
    // https://oldschool.runescape.wiki/w/Transcript:Seaman_Thresnor
    private suspend fun Dialogue.offerTrip() {
        chatNpc(
            quiz,
            "Hello there. Do you want to go on a trip to Karamja? We can take you to Musa Point " +
                "for only $FARE coins.",
        )
        if (!choice2("Yes please.", true, "No, thank you.", false)) {
            chatPlayer(neutral, "No, thank you.")
            return
        }
        if (access.inv.count(COINS) < FARE) {
            chatPlayer(sad, NOT_ENOUGH_MONEY)
            return
        }
        chatPlayer(happy, "Yes please.")
        if (access.payFare(FARE)) {
            access.sailFerry(MUSA_POINT, MUSA_POINT_NAME, FARE)
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Customs_officer
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
        if (QuestRequirements.hasCompleted(player, PIRATES_TREASURE)) {
            if (carriesRum()) {
                chatNpc(neutral, "Spot inspection. You don't mind do you?")
                chatNpc(angry, "Aha, trying to smuggle rum eh?")
                confiscateRum(smuggleHint = false)
                return
            }
            chatNpc(happy, "Hey, I know you, you work at the plantation.")
            chatNpc(
                neutral,
                "I don't think you'll try smuggling anything, you just need to pay a boarding " +
                    "charge of $FARE coins.",
            )
            return boardingCharge()
        }
        chatNpc(neutral, "You need to be searched before you can board.")
        while (true) {
            val answer =
                choice3(
                    "Why?",
                    CustomsTopic.Why,
                    "Search away, I have nothing to hide.",
                    CustomsTopic.Search,
                    "You're not putting your hands on my things!",
                    CustomsTopic.Refuse,
                )
            when (answer) {
                CustomsTopic.Why -> {
                    chatPlayer(quiz, "Why?")
                    chatNpc(
                        neutral,
                        "Because Asgarnia has banned the import of intoxicating spirits.",
                    )
                }
                CustomsTopic.Search -> {
                    chatPlayer(neutral, "Search away, I have nothing to hide.")
                    if (carriesRum()) {
                        chatNpc(angry, "Aha, trying to smuggle rum are we?")
                        confiscateRum(smuggleHint = true)
                        return
                    }
                    chatNpc(
                        neutral,
                        "Well you've got some odd stuff, but it's all legal. Now you need to pay " +
                            "a boarding charge of $FARE coins.",
                    )
                    return boardingCharge()
                }
                CustomsTopic.Refuse -> {
                    chatPlayer(angry, "You're not putting your hands on my things!")
                    chatNpc(neutral, "You're not getting on this ship then.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.boardingCharge() {
        if (!choice2("Ok.", true, "Oh, I'll not bother then.", false)) {
            chatPlayer(neutral, "Oh, I'll not bother then.")
            return
        }
        chatPlayer(neutral, "Ok.")
        if (!access.payFare(FARE)) {
            chatPlayer(sad, NOT_ENOUGH_MONEY)
            return
        }
        access.sailFerry(PORT_SARIM, PORT_SARIM_NAME, FARE)
    }

    private suspend fun Dialogue.confiscateRum(smuggleHint: Boolean) {
        chatPlayer(shifty, "Umm... it's for personal use?")
        access.invDel(access.inv, KARAMJA_RUM, access.inv.count(KARAMJA_RUM))
        access.mes("The customs officer confiscates your rum.")
        if (smuggleHint) {
            access.mes("You will need to find some way to smuggle it off the island...")
        }
    }

    private fun Dialogue.carriesRum(): Boolean = access.inv.count(KARAMJA_RUM) > 0

    private suspend fun ProtectedAccess.payAndSail(dest: CoordGrid, destName: String) {
        if (!payFare(FARE)) {
            mes("You do not have enough coins to pay passage, you need $FARE.")
            return
        }
        sailFerry(dest, destName, FARE)
    }

    private enum class CustomsTopic {
        Why,
        Search,
        Refuse,
    }

    private companion object {
        const val FARE = 30
        const val COINS = "obj.coins"
        const val KARAMJA_RUM = "obj.karamja_rum"
        const val PIRATES_TREASURE = "quest_piratestreasure"
        const val NOT_ENOUGH_MONEY = "Oh dear, I don't seem to have enough money."
        const val CUSTOMS_OFFICER = "npc.customs_officer"
        const val MUSA_POINT_NAME = "Musa Point"
        const val PORT_SARIM_NAME = "Port Sarim"

        val SARIM_CREW = listOf("npc.seaman_lorris", "npc.seaman_thresnor", "npc.captain_tobias")
        val MUSA_POINT = CoordGrid(2956, 3146, 0)
        val PORT_SARIM = CoordGrid(3029, 3217, 0)
    }
}
