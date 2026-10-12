package org.rsmod.content.areas.city.rellekka.rellekka.npcs.shops

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Yrsa
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:shops] "Yrsa's Accoutrements" (inv.viking_clothes_shop has no shop
// config in the cache) for Fremenniks, from Trade and "I'd like to buy some clothes"
// TODO(content): [system:makeover] "Yrsa's Shoe Store" for Fremenniks, from Makeover and "I'd
// like to change my shoes", then one of the four random replies
class Yrsa : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.viking_clothing_shopkeeper") {
            if (QuestRequirements.hasCompleted(player, FREMENNIK_TRIALS)) {
                startDialogue(it.npc) { fremennik() }
            } else {
                startDialogue(it.npc) { outerlander() }
            }
        }
        onOpNpc3("npc.viking_clothing_shopkeeper") {
            if (QuestRequirements.hasNotCompleted(player, FREMENNIK_TRIALS)) {
                mes("Only Fremenniks may buy clothes here.")
            }
        }
        onOpNpc4("npc.viking_clothing_shopkeeper") {
            if (QuestRequirements.hasNotCompleted(player, FREMENNIK_TRIALS)) {
                mes("Only Fremenniks may change their shoes here.")
            }
        }
    }

    private suspend fun Dialogue.fremennik() {
        chatNpc(
            happy,
            "Hi. You wanted to buy some clothes? Or did you want to makeover your shoes?",
        )
        val option =
            choice3(
                "I'd like to buy some clothes",
                1,
                "I'd like to change my shoes",
                2,
                "Neither thanks",
                3,
            )
        when (option) {
            2 -> {
                chatNpc(
                    happy,
                    "It's free to change your shoes here. You can choose a colour as well as a " +
                        "style, since I'm also stocking boots imported from the elven lands.",
                )
                chatNpc(happy, "Just select the model you would like from this catalogue.")
            }
            3 -> {
                chatPlayer(neutral, "Neither thanks.")
                chatNpc(neutral, "As you wish.")
            }
        }
    }

    private suspend fun Dialogue.outerlander() {
        chatPlayer(happy, "Hello!")
        chatNpc(
            sad,
            "I'm sorry outerlander, I cannot sell you any clothes. Our customs forbid it.",
        )
    }

    private companion object {
        const val FREMENNIK_TRIALS = "quest_fremenniktrials"
    }
}
