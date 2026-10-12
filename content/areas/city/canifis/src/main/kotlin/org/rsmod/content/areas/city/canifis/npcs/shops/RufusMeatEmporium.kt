package org.rsmod.content.areas.city.canifis.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Rufus
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:ironman_shops] inv.werewolfstore1_iron, _uim and _gim stock variants
class RufusMeatEmporium @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(RUFUS) { startDialogue(it.npc) { rufus(it.npc) } }
        onOpNpc3(RUFUS) { player.openShop(it.npc) }
    }

    private fun Player.openShop(npc: Npc) {
        shops.open(this, npc, "Rufus' Meat Emporium", "inv.werewolfstore1")
    }

    private suspend fun Dialogue.rufus(npc: Npc) {
        chatPlayer(happy, "Hi!")
        chatNpc(
            happy,
            "Grrreetings frrriend! Welcome to my worrrld famous food emporrrium! All my meats " +
                "are so frrresh you'd swear you killed them yourrrself!",
        )
        val option =
            choice3(
                "Why do you only sell meats?",
                1,
                "Do you sell cooked food?",
                2,
                "Can I buy some food?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Why do you only sell meats?")
                chatNpc(
                    confused,
                    "What? Why, what else would you want to eat? What kind of lycanthrrrope are " +
                        "you anyway?",
                )
                chatPlayer(shifty, "...A vegetarian one?")
                chatNpc(confused, "Vegetarrrian...?")
                chatPlayer(neutral, "Never mind.")
            }
            2 -> {
                chatPlayer(quiz, "Do you sell cooked food?")
                chatNpc(
                    confused,
                    "Cooked food? Who would want that? You lose all the flavourrr of the meat " +
                        "when you can't taste the blood!",
                )
            }
            3 -> {
                chatPlayer(quiz, "Can I buy some food?")
                chatNpc(happy, "Cerrrtainly!")
                player.openShop(npc)
            }
        }
    }

    private companion object {
        const val RUFUS = "npc.werewolfshopkeeper1"
    }
}
