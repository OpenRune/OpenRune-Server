package org.rsmod.content.areas.city.canifis.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Boris
// https://oldschool.runescape.wiki/w/Transcript:Alexis
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class CanifisCitizens : PluginScript() {
    override fun ScriptContext.startup() {
        for (citizen in CITIZENS) {
            onOpNpc1(citizen) { startDialogue(it.npc) { citizen() } }
        }
    }

    private suspend fun Dialogue.citizen() {
        if (player.wearsRingOfCharos()) {
            friendly()
        } else {
            hostile()
        }
    }

    private suspend fun Dialogue.hostile() {
        when (access.random.of(10)) {
            0 ->
                chatNpc(
                    angry,
                    "If I were as ugly as you I would not dare to show my face in public!",
                )
            1 -> chatNpc(angry, "Out of my way, punk.")
            2 -> {
                chatNpc(shifty, "Hmm... you smell strange...")
                chatPlayer(confused, "Strange how?")
                chatNpc(shifty, "Like a human!")
                chatPlayer(worried, "Oh! Er... I just ate one is why!")
            }
            3 -> chatNpc(angry, "Leave me alone.")
            4 -> chatNpc(verymad, "Don't talk to me again if you value your life!")
            5 -> chatNpc(angry, "Get lost!")
            6 ->
                chatNpc(angry, "I don't have anything to give you so leave me alone, mendicant.")
            7 -> chatNpc(angry, "Have you no manners?")
            8 -> chatNpc(bored, "I don't have time for this right now.")
            else ->
                chatNpc(
                    angry,
                    "I have no interest in talking to a pathetic meat bag like yourself.",
                )
        }
    }

    private suspend fun Dialogue.friendly() {
        when (access.random.of(10)) {
            0 -> chatNpc(happy, "I bet you have wonderful paws.")
            1 -> chatNpc(sad, "A very miserable day, altogether... enjoy it while it lasts.")
            2 -> chatNpc(happy, "If you catch anyone promise me you'll share.")
            3 -> chatNpc(shifty, "I haven't smelt you around here before...")
            4 -> chatNpc(shifty, "You smell familiar...")
            5 -> chatNpc(quiz, "Seen any humans around here? I'm v-e-r-y hungry.")
            6 -> chatNpc(happy, "You look to me like someone with a healthy taste for blood.")
            7 -> chatNpc(happy, "Good day to you, my friend.")
            8 -> chatNpc(happy, "Fancy going up to the castle for a bit of a snack?")
            else ->
                chatNpc(neutral, "Give me a moment, I have a bit of someone stuck in my teeth...")
        }
    }

    private companion object {
        val CITIZENS =
            (1..8).map { "npc.canafis_man$it" } + (1..12).map { "npc.canafis_woman$it" }
    }
}
