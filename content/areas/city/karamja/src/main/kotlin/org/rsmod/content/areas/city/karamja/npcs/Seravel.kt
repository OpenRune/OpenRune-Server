package org.rsmod.content.areas.city.karamja.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Seravel
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:shilo_village] pre-quest zombie line
// TODO(content): [system:lady_of_the_waves] boarding the ship with the ticket
class Seravel @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.shiloshiptickets") { startDialogue(it.npc) { seravel() } }
    }

    private suspend fun Dialogue.seravel() {
        chatPlayer(happy, "Hello")
        chatNpc(
            happy,
            "Hello Bwana. Are you interested in buying a ticket for the 'Lady of the Waves'?",
        )
        val option =
            choice3(
                "Yes, that sounds great!",
                1,
                "No thanks.",
                2,
                "Tell me more about the ship.",
                3,
            )
        when (option) {
            1 -> buyTicket()
            2 -> noThanks()
            3 -> {
                chatNpc(
                    neutral,
                    "It's a ship that can take you to either Port Sarim or Port Khazard. The " +
                        "ship lies west of Shilo Village and south of Cairn Island. The tickets " +
                        "cost 25 coins. Would you like to purchase a ticket Bwana?",
                )
                if (choice2("Yes, that sounds great!", true, "No thanks.", false)) {
                    buyTicket()
                } else {
                    noThanks()
                }
            }
        }
    }

    private suspend fun Dialogue.buyTicket() {
        chatPlayer(happy, "Yes, that sounds great!")
        if (!player.invTakeFee(TICKET_PRICE)) {
            chatNpc(
                neutral,
                "Sorry Bwana, you don't have enough money. Come back when you have 25 coins.",
            )
            return
        }
        player.invAddOrDrop(objRepo, "obj.shiloshipticket")
        chatNpc(happy, "Great, nice doing business with you.")
    }

    private suspend fun Dialogue.noThanks() {
        chatPlayer(neutral, "No thanks.")
        chatNpc(neutral, "Fair enough Bwana, let me know if you change your mind.")
    }

    private companion object {
        const val TICKET_PRICE = 25
    }
}
