package org.rsmod.content.areas.city.karamja.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.mes
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Kaleb_Paramaya
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:shilo_village] pre-quest zombie line
class ParamayaInn @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.shiloinnowner") { startDialogue(it.npc) { kaleb() } }
    }

    private suspend fun Dialogue.kaleb() {
        chatNpc(happy, "Hello, Bwana, what can I do for you today?")
        while (true) {
            val option =
                choice4(
                    "Can you tell me a bit about this place?",
                    1,
                    "Have you anything for sale?",
                    2,
                    "I have a question about my Achievement Diary.",
                    3,
                    "I'm fine, thanks.",
                    4,
                )
            when (option) {
                1 -> {
                    aboutThisPlace()
                    if (choice2("Have you anything for sale?", true, "I'm fine, thanks.", false)) {
                        forSale()
                    } else {
                        chatPlayer(neutral, "I'm fine, thanks.")
                    }
                    return
                }
                2 -> {
                    forSale()
                    return
                }
                3 -> {
                    chatPlayer(quiz, "I have a question about my Achievement Diary.")
                    chatNpc(
                        neutral,
                        "Sorry, Bwana, I don't do that kind of thing anymore. You should go and " +
                            "talk to Pirate Jackie in Brimhaven instead. Can I help with " +
                            "anything else?",
                    )
                }
                4 -> {
                    chatPlayer(neutral, "I'm fine, thanks.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.aboutThisPlace() {
        chatPlayer(quiz, "Can you tell me a bit about this place?")
        chatNpc(happy, "Of course, Bwana. You look like a traveler!")
        chatPlayer(happy, "Yes, I am actually!")
        chatNpc(
            happy,
            "Well, I am a traveller myself, and I have set up this hostel for adventurers and " +
                "travellers who are weary from their journey.",
        )
        chatNpc(
            neutral,
            "There is a dormitory upstairs if you are tired. It costs 35 coins, which covers the " +
                "costs of laundry and cleaning.",
        )
    }

    private suspend fun Dialogue.forSale() {
        chatPlayer(quiz, "Have you anything for sale?")
        while (true) {
            val option =
                choice5(
                    "Buy some wine: 1 coin",
                    1,
                    "Buy some beer: 2 coins",
                    2,
                    "Buy a night's rest: 35 coins",
                    3,
                    "Buy a pack of 5 dorm tickets: 175 coins",
                    4,
                    "Cancel",
                    5,
                )
            when (option) {
                1 -> buy(WINE_PRICE, "obj.jug_wine", 1, "You buy a jug of wine for 1 coin.")
                // TODO(content): [verify:in_game] the transcript says beer gives 2 glasses
                2 ->
                    buy(
                        BEER_PRICE,
                        "obj.beer",
                        1,
                        "You purchase a frothy glass of beer for 2 coins.",
                    )
                3 ->
                    buy(
                        TICKET_PRICE,
                        TICKET,
                        1,
                        "You purchase a ticket to access the dormitory for 35 coins.",
                    )
                4 ->
                    buy(
                        TICKET_PACK_PRICE,
                        TICKET,
                        TICKET_PACK_SIZE,
                        "You purchase five tickets to access the dormitory for 175 coins.",
                    )
                5 -> {
                    chatPlayer(neutral, "I'm fine, thanks.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.buy(price: Int, obj: String, count: Int, message: String) {
        if (!player.invTakeFee(price)) {
            chatNpc(sad, "I'm afraid you do not have enough money for that, Bwana.")
            return
        }
        repeat(count) { player.invAddOrDrop(objRepo, obj) }
        player.mes(message)
    }

    private companion object {
        const val WINE_PRICE = 1
        const val BEER_PRICE = 2
        const val TICKET_PRICE = 35
        const val TICKET_PACK_PRICE = 175
        const val TICKET_PACK_SIZE = 5
        const val TICKET = "obj.paramayaticket"
    }
}
