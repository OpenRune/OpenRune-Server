package org.rsmod.content.areas.city.burthorpe.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Eadgar
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [verify:in_game] whether log types other than normal logs pay for the stew
class MadEadgar : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.troll_eadgar") { startDialogue(it.npc) { eadgar() } }
    }

    private suspend fun Dialogue.eadgar() {
        chatPlayer(happy, "Hi!")
        chatNpc(happy, WELCOMES[access.random.of(WELCOMES.size)])
        chatNpc(happy, "Would you care to sample our delicious home cooking?")
        val option =
            choice2(
                "Why do you live so close to the trolls?",
                1,
                "What do you have to offer?",
                2,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Why do you live so close to the trolls? Isn't it dangerous?")
                chatNpc(
                    neutral,
                    "Well, I suppose I do keep getting captured by the trolls and thrown in " +
                        "prison...But they always release me in the end, I'm far too old and " +
                        "skinny for their tastes.",
                )
                chatNpc(
                    happy,
                    "In any case, this is my home, and I'm not leaving it. And this area has the " +
                        "tastiest goats!",
                )
            }
            2 -> offer()
        }
    }

    private suspend fun Dialogue.offer() {
        chatPlayer(quiz, "What do you have to offer?")
        chatNpc(
            happy,
            "The chef's recommendation for today is mountain goat stew. I'll give some stew in " +
                "exchange for logs for my fire. They're hard to come by around here.",
        )
        val option =
            choice2("I'd like some mountain goat stew, please.", 1, "No thanks, Eadgar.", 2)
        if (option == 2) {
            chatPlayer(neutral, "No thanks, Eadgar.")
            chatNpc(neutral, "Your loss!")
            return
        }
        chatPlayer(happy, "I'd like some mountain goat stew, please.")
        if (access.invDel(player.inv, LOGS).failure) {
            chatNpc(angry, "You don't have any logs! I may be mad, but I'm not stupid!")
            return
        }
        chatNpc(happy, "Here you go! Enjoy!")
        player.mes("You get a bowl of mountain goat stew.")
        player.mes("You eat the mountain goat stew.")
        player.mes("It heals some health.")
        player.statHeal(HITPOINTS, constant = STEW_HEAL, percent = 0)
    }

    private companion object {
        const val LOGS = "obj.logs"
        const val HITPOINTS = "stat.hitpoints"
        // https://oldschool.runescape.wiki/w/Eadgar: "The mountain goat stew heals 20 Hitpoints"
        const val STEW_HEAL = 20
        val WELCOMES =
            listOf(
                "Welcome to Mad Eadgar's! Happiness in a bowl!",
                "Welcome to Mad Eadgar's! You'll never want to leave!",
                "Welcome to Mad Eadgar's! We don't serve trolls!",
                "Welcome to Mad Eadgar's! Serving all day, every day!",
            )
    }
}
