package org.rsmod.content.areas.wilderness.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class BanditCampShops @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.noterazzo") { startDialogue(it.npc) { noterazzo(it.npc) } }
        onOpNpc3("npc.noterazzo") { player.openDutyFree(it.npc) }
        onOpNpc1("npc.fat_tony") { startDialogue(it.npc) { fatTony(it.npc) } }
        onOpNpc3("npc.fat_tony") { player.openPizzaBases(it.npc) }
    }

    private fun Player.openDutyFree(npc: Npc) {
        shops.open(this, npc, "Bandit Duty Free", "inv.taxfree")
    }

    private fun Player.openPizzaBases(npc: Npc) {
        shops.open(this, npc, "Tony's Pizza Bases", "inv.pizzabaseshop")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Noterazzo
    private suspend fun Dialogue.noterazzo(npc: Npc) {
        chatNpc(happy, "Hey wanna trade? I'll give the best deals you can find.")
        val option =
            choice3(
                "Yes please.",
                1,
                "No thanks.",
                2,
                "How can you afford to give such good deals?",
                3,
            )
        when (option) {
            1 -> player.openDutyFree(npc)
            2 -> chatPlayer(neutral, "No thanks.")
            3 -> {
                chatPlayer(quiz, "How can you afford to give such good deals?")
                chatNpc(
                    neutral,
                    "The general stores in Asgarnia and Misthalin are heavily taxed. It really " +
                        "makes it hard for them to run an effective business. For some reason " +
                        "taxmen don't visit my store.",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Fat_Tony
    private suspend fun Dialogue.fatTony(npc: Npc) {
        chatNpc(angry, "Go away I'm very busy!")
        val option =
            choice3(
                "Sorry to disturb you.",
                1,
                "What are you busy doing?",
                2,
                "Have you anything to sell?",
                3,
            )
        when (option) {
            1 -> chatPlayer(sad, "Sorry to disturb you.")
            2 -> fatTonyBusy(npc)
            3 -> {
                chatPlayer(quiz, "Have you anything to sell?")
                chatNpc(neutral, "Well I guess I can sell you some half made pizzas.")
                player.openPizzaBases(npc)
            }
        }
    }

    private suspend fun Dialogue.fatTonyBusy(npc: Npc) {
        chatPlayer(quiz, "What are you busy doing?")
        chatNpc(neutral, "I'm cooking pizzas for the people in this camp.")
        chatNpc(angry, "Not that these louts appreciate my gourmet cooking!")
        while (true) {
            val option =
                choice3(
                    "So what is a gourmet chef doing cooking for bandits?",
                    1,
                    "Can I have some pizza too?",
                    2,
                    "Okay, I'll leave you to it.",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "So what is a gourmet chef doing cooking for bandits?")
                    chatNpc(
                        angry,
                        "Well I'm an outlaw. I was accused of giving the king food poisoning! " +
                            "The thought of it! I think he just drank too much wine that night.",
                    )
                    chatNpc(
                        sad,
                        "I had to flee the kingdom of Misthalin! The bandits give me refuge here " +
                            "as long as I cook for them.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Can I have some pizza too?")
                    chatNpc(
                        neutral,
                        "Well this pizza is really meant to be for the bandits. I guess I could " +
                            "sell you some pizza bases though",
                    )
                    val buy =
                        choice2("Yes, Okay.", 1, "Oh if I have to pay I don't want any.", 2)
                    if (buy == 1) {
                        chatPlayer(happy, "Yes, Okay.")
                        player.openPizzaBases(npc)
                    } else {
                        chatPlayer(neutral, "Oh if I have to pay I don't want any.")
                    }
                    return
                }
                else -> {
                    chatPlayer(neutral, "Okay, I'll leave you to it.")
                    return
                }
            }
        }
    }
}
