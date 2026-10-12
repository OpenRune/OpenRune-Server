package org.rsmod.content.areas.city.varrock.npcs.shops

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:speedrunning] Speedrunning Reward Shop (Speedrun Points) and Claim op
// TODO(content): [system:events] Events Reward Shop (Event Points)
class RewardShops : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.speedrunning_shop") { startDialogue(it.npc) { eliza() } }
        onOpNpc1("npc.events_shop") { startDialogue(it.npc) { nevet() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Eliza
    private suspend fun Dialogue.eliza() {
        chatNpc(happy, "Hey there. Looking to buy something?")
        if (vars[ELIZA_MET] == 0) {
            elizaSelling()
            return
        }
        val option =
            choice4(
                "Yes please.",
                1,
                "What are you selling?",
                2,
                "I'd like to claim some trophies.",
                3,
                "I'll pass.",
                4,
            )
        when (option) {
            1 -> chatPlayer(happy, "Yes please.")
            2 -> elizaSelling()
            3 -> elizaTrophies()
            4 -> elizaPass()
        }
    }

    private suspend fun Dialogue.elizaSelling() {
        chatPlayer(quiz, "What are you selling?")
        chatNpc(
            happy,
            "A selection of fun items for the best of adventurers! That said, be aware that I " +
                "have little interest in the usual currencies...",
        )
        chatPlayer(neutral, "Right...")
        mesbox(
            "You can only purchase items from the Speedrunning Rewards Shop using Speedrun " +
                "Points. Speedrun Points can only be earned on Speedrunning Worlds."
        )
        vars[ELIZA_MET] = 1
        chatNpc(happy, "So, looking to buy?")
        val option =
            choice3("Yes please.", 1, "I'd like to claim some trophies.", 2, "I'll pass.", 3)
        when (option) {
            1 -> chatPlayer(happy, "Yes please.")
            2 -> elizaTrophies()
            3 -> elizaPass()
        }
    }

    private suspend fun Dialogue.elizaTrophies() {
        chatPlayer(quiz, "I'd like to claim some trophies.")
        chatNpc(neutral, "I'm afraid you don't have any trophies to claim.")
        mesbox(
            "A Speedrun Trophy can only be claimed on your main profile if you have earned that " +
                "trophy for every quest available on Speedrunning Worlds."
        )
    }

    private suspend fun Dialogue.elizaPass() {
        chatPlayer(neutral, "I'll pass.")
        chatNpc(neutral, "Fair enough.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Nevet
    private suspend fun Dialogue.nevet() {
        chatNpc(happy, "Hello! Looking to buy?")
        if (vars[NEVET_MET] == 0) {
            nevetSelling()
            return
        }
        when (choice3("Yes please.", 1, "What are you selling?", 2, "I'll pass.", 3)) {
            1 -> chatPlayer(happy, "Yes please.")
            2 -> nevetSelling()
            3 -> nevetPass()
        }
    }

    private suspend fun Dialogue.nevetSelling() {
        chatPlayer(quiz, "What are you selling?")
        chatNpc(
            happy,
            "A selection of items for those who partake in variety! That said, be aware that I " +
                "have little interest in coins...",
        )
        chatPlayer(neutral, "Right...")
        mesbox(
            "You can only purchase items from the Events Rewards Shop using Event Points. Event " +
                "Points can only be earned within limited-time Events on separate worlds."
        )
        vars[NEVET_MET] = 1
        chatNpc(happy, "So, looking to buy?")
        when (choice2("Yes please.", 1, "I'll pass.", 2)) {
            1 -> chatPlayer(happy, "Yes please.")
            2 -> nevetPass()
        }
    }

    private suspend fun Dialogue.nevetPass() {
        chatPlayer(neutral, "I'll pass.")
        chatNpc(neutral, "Fair enough.")
    }

    private companion object {
        const val ELIZA_MET = "varbit.speedrunning_shop_owner_met"
        const val NEVET_MET = "varbit.events_shop_owner_met"
    }
}
