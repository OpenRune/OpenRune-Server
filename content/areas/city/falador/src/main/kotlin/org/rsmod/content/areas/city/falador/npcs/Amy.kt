package org.rsmod.content.areas.city.falador.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Amy (initial dialogue)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:mahogany_homes] contracts, reward shop, its ops and later dialogue
class Amy : PluginScript() {
    override fun ScriptContext.startup() {
        for (amy in AMY) {
            onOpNpc1(amy) { startDialogue(it.npc) { amy() } }
        }
    }

    private suspend fun Dialogue.amy() {
        chatNpc(happy, "Hey I'm Amy, how may I help you?")
        chatPlayer(quiz, "What do you do here?")
        chatNpc(happy, "I'm the founder of Mahogany Homes, Gielinor's first Construction company!")
        chatNpc(happy, "We've currently got branches in Falador, Varrock, Hosidius and Ardougne.")
        chatPlayer(quiz, "So people contact you for help with their homes?")
        chatNpc(
            happy,
            "Yep! They'll get in contact if they want assistance, whether it's fixing parts of " +
                "their house or requesting nicer furniture, and then we will go and help them out.",
        )
        chatPlayer(
            quiz,
            "That sounds quite enjoyable and satisfying to do, are you hiring at all?",
        )
        chatNpc(
            neutral,
            "If you have the supplies, I can tell you where one of our clients is that needs " +
                "some work doing.",
        )
        chatNpc(
            neutral,
            "I'll keep track of the work you do as well and give you some points for our " +
                "company reward shop.",
        )
        chatNpc(
            neutral,
            "Also there are assistants in Varrock, Ardougne, and Hosidius that can give you " +
                "work, if Falador is inconvienent for you.",
        )
        chatNpc(
            neutral,
            "The clients that I would offer you to go help could be in any of those cities, it " +
                "might be a bit of a journey to get there!",
        )
        val contract =
            choice2(
                "I'd like a construction contract.",
                true,
                "Thanks for the information.",
                false,
            )
        if (!contract) {
            chatPlayer(happy, "Thanks for the information.")
            return
        }
        chatPlayer(happy, "I'd like a construction contract.")
        chatNpc(quiz, "What kind of contract would you like?")
        // TODO(content): [system:mahogany_homes] contract tier choice
    }

    private companion object {
        val AMY = listOf("npc.con_contractor_1op", "npc.con_contractor_3op")
    }
}
