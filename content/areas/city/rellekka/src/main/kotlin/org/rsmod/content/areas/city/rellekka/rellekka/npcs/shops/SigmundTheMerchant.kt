package org.rsmod.content.areas.city.rellekka.rellekka.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.content.areas.city.rellekka.brotherOrSister
import org.rsmod.content.areas.city.rellekka.fremennikName
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Sigmund_The_Merchant
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:the_fremennik_trials] his trading task during the Trials
// TODO(content): [verify:in_game] what the Trade option does before the Trials
class SigmundTheMerchant @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.viking_sigmund") {
            if (QuestRequirements.hasCompleted(player, FREMENNIK_TRIALS)) {
                startDialogue(it.npc) { fremennik(it.npc) }
            } else {
                startDialogue(it.npc) { outerlander() }
            }
        }
        onOpNpc3("npc.viking_sigmund") {
            if (QuestRequirements.hasCompleted(player, FREMENNIK_TRIALS)) {
                player.openShop(it.npc)
            }
        }
    }

    private fun Player.openShop(npc: Npc) {
        shops.open(this, npc, "Sigmund the Merchant", "inv.viking_general_store")
    }

    private suspend fun Dialogue.fremennik(npc: Npc) {
        chatPlayer(happy, "Hello there!")
        chatNpc(
            happy,
            "Greetings again ${brotherOrSister()} ${fremennikName()}! What can I do for you " +
                "this day?",
        )
        if (choice2("Can I see your wares?", true, "Nothing thanks.", false)) {
            chatPlayer(quiz, "Can I see your wares?")
            chatNpc(happy, "Certainly, ${fremennikName()}.")
            player.openShop(npc)
        } else {
            chatPlayer(neutral, "Nothing thanks.")
            chatNpc(
                happy,
                "Well, feel free to stop by anytime that you wish ${fremennikName()}. You are " +
                    "always welcome here!",
            )
        }
    }

    private suspend fun Dialogue.outerlander() {
        chatPlayer(happy, "Hiya.")
        chatNpc(
            neutral,
            "Hello outerlander. By the laws of our tribe, I am afraid I may not speak to you " +
                "without the express permission of the chieftain.",
        )
    }

    private companion object {
        const val FREMENNIK_TRIALS = "quest_fremenniktrials"
    }
}
