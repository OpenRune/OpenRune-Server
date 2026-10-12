package org.rsmod.content.areas.city.lumbridge.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [verify:in_game] which pieces count as "full H.A.M. robes" (hood, shirt and robe
// are assumed)
class HamHideout : PluginScript() {
    override fun ScriptContext.startup() {
        for (guard in GUARDS) {
            onOpNpc1(guard) { startDialogue(it.npc) { guard() } }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:H.A.M._Guard
    private suspend fun Dialogue.guard() {
        if (ROBES.all { it in player.worn }) {
            val address = if (access.isBodyTypeA()) "brother" else "sister"
            chatNpc(
                happy,
                "Hello, $address, you fit in much better now that you're correctly attired. " +
                    "Before you stuck out like a sore thumb! Keep your eyes peeled though; I've " +
                    "heard there are thieves amongst us!",
            )
        }
        while (true) {
            val option =
                choice5(
                    "What are all you people doing here?",
                    1,
                    "Who are you and what do you do here?",
                    2,
                    "What do you think you're going to achieve?",
                    3,
                    "Where did all you people come from?",
                    4,
                    "Ok, thanks.",
                    5,
                )
            when (option) {
                1 -> doingHere()
                2 -> whoAreYou()
                3 -> achieve()
                4 -> return comeFrom()
                else -> return chatPlayer(neutral, "Ok, thanks.")
            }
        }
    }

    private suspend fun Dialogue.doingHere() {
        chatPlayer(quiz, "What are all you people doing here?")
        when (access.random.of(3)) {
            0 ->
                chatNpc(
                    neutral,
                    "Many of us disagree with the king about what freedoms the local monster " +
                        "population should have. We're taking a stand and mobilising our forces " +
                        "against the monstrous hordes.",
                )
            1 ->
                chatNpc(
                    happy,
                    "I'm totally in awe of Johanhus, he really knows what's what. I know he " +
                        "keeps going on about monsters and it's clear there are too many of " +
                        "them, so hey, I agree with whatever Johanhus says.",
                )
            else ->
                chatNpc(
                    confused,
                    "We're against the monsters..like Johanhus says...we don't like them...you " +
                        "know.",
                )
        }
    }

    private suspend fun Dialogue.whoAreYou() {
        chatPlayer(quiz, "Who are you and what do you do here?")
        chatNpc(
            neutral,
            "I'm a strong believer in the non-monsters policy...we should really get rid of " +
                "them...and if that means I have to live in a cave like a monster, so be it!",
        )
    }

    private suspend fun Dialogue.achieve() {
        chatPlayer(quiz, "What do you think you're going to achieve?")
        chatNpc(
            neutral,
            "We want a world without monsters, to live in safety and without fear of being " +
                "attacked by these ferocious beasts.",
        )
        chatPlayer(
            neutral,
            "But there aren't that many ferocious beasts in the towns and cities.",
        )
        chatNpc(
            angry,
            "That's not enough, we want to get rid of them totally, we want to enjoy the " +
                "surrounding lands and not worry about our children playing in caves and so on.",
        )
    }

    private suspend fun Dialogue.comeFrom() {
        chatPlayer(quiz, "Where did all you people come from?")
        chatNpc(
            neutral,
            "Most of us came from small towns that had been attacked by monsters. We all got fed " +
                "up with it and so decided to join this movement. We're hoping to return to the " +
                "towns and cities when we've cleaned up the areas",
        )
        chatNpc(neutral, "that these monsters live in.")
    }

    private companion object {
        val GUARDS =
            listOf(
                "npc.favour_guard_male_no_beard",
                "npc.favour_guard_male_bearded",
                "npc.favour_guard_long_beard",
            )

        val ROBES = listOf("obj.ham_hood", "obj.ham_shirt", "obj.ham_robe")
    }
}
