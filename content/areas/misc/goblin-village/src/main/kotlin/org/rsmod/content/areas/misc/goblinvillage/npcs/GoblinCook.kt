package org.rsmod.content.areas.misc.goblinvillage.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Mudknuckles, in the cave below Goblin Village, inventing explosive cooking. */
class GoblinCook : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.goblin_cook_charred") { startDialogue(it.npc) { fastFood() } }
    }

    private suspend fun Dialogue.fastFood() {
        chatNpc(
            happy,
            "You the human who helped me. Mudknuckles is very grateful, will be famous real " +
                "soon now. Is just collecting results. And so many results! On walls, on " +
                "ceiling, everywhere!",
        )
        chatNpc(
            happy,
            "I thinking of calling it fast food, 'cos of how fast it goes from raw to burnt, see?",
        )
        when (
            choice3(
                "That sounds like a great idea.",
                1,
                "Do you think that will sell?",
                2,
                "I'll leave you to your experimentation then.",
                3,
            )
        ) {
            1 -> greatIdea()
            2 -> willItSell()
            else -> chatPlayer(neutral, "I'll leave you to your experimentation then.")
        }
    }

    private suspend fun Dialogue.greatIdea() {
        chatPlayer(happy, "That sounds like a great idea.")
        chatNpc(
            quiz,
            "You think so? Everyone else think Mudknuckles crazy. Seem think explosive cooking " +
                "might be dangerous or summat.",
        )
        chatNpc(quiz, "Can't see why. Not like you gonna get food poisoning. Food is very well cooked.")
        chatPlayer(confused, "Er... yes... I'm sure food poisoning was what they were worried about.")
    }

    private suspend fun Dialogue.willItSell() {
        chatPlayer(quiz, "Do you think that will sell? Like you said, it's burnt.")
        chatNpc(
            neutral,
            "So maybe some imperfections, but fast food will sell. People buy anything when in " +
                "hurry, even if does taste like sawdust.",
        )
    }
}
