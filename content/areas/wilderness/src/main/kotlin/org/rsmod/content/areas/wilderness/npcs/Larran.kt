package org.rsmod.content.areas.wilderness.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class Larran : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.slayer_larran") { startDialogue(it.npc) { larran() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Larran
    private suspend fun Dialogue.larran() {
        if (GHOSTSPEAK.none { it in player.worn }) {
            chatNpc(neutral, "Woooo wooo wooooo woooo")
            return
        }
        chatNpc(neutral, "Do you want something, mortal?")
        while (true) {
            val hasKey = LARRANS_KEY in player.inv
            val choice =
                if (hasKey) {
                    choice4(
                        "Who are you and why are you here?",
                        1,
                        "I've got your key.",
                        2,
                        "Can you give me anything?",
                        3,
                        "I'll leave you to it.",
                        4,
                    )
                } else {
                    choice3(
                        "Who are you and why are you here?",
                        1,
                        "Can you give me anything?",
                        3,
                        "I'll leave you to it.",
                        4,
                    )
                }
            when (choice) {
                1 -> whoAreYou()
                2 -> gotYourKey()
                3 -> {
                    giveMeAnything()
                    return
                }
                else -> {
                    chatPlayer(neutral, "I'll leave you to it.")
                    chatNpc(neutral, "I will be here if you return.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.whoAreYou() {
        chatPlayer(quiz, "Who are you and why are you here?")
        chatNpc(
            neutral,
            "Once I was a merchant. I worked hard and became wealthy, very wealthy. I was brave " +
                "too, storing my possessions in two great chests here, where few would dare to " +
                "interfere with them.",
        )
        chatNpc(
            sad,
            "But I was not strong enough. One day a witch allied with the pirates of the north; " +
                "between their strength and her magic, they killed me. The pirates carried one " +
                "of my chests away to their ship.",
        )
        chatNpc(
            neutral,
            "I keep vigil here now, as it interests me to watch adventurers come to raid this " +
                "little chest, whenever they can find one of my keys.",
        )
        chatNpc(
            neutral,
            "My bigger chest was taken north to the pirates' ship - a brave adventurer may like " +
                "to raid that one instead, if they can handle the perils of the north. I kept " +
                "more wealth in that one.",
        )
    }

    private suspend fun Dialogue.gotYourKey() {
        chatPlayer(happy, "I've got your key.")
        chatNpc(
            neutral,
            "Well done, adventurer. I have two chests, containing my wealth from the days when I " +
                "was alive. The smaller one is here, and the bigger one was taken far to the " +
                "north, to the pirates' ship.",
        )
        chatNpc(
            neutral,
            "You can use the key to open either of them. The bigger one, in the pirates' ship, " +
                "contains more wealth than this small one.",
        )
    }

    private suspend fun Dialogue.giveMeAnything() {
        chatPlayer(quiz, "Can you give me anything?")
        chatNpc(
            neutral,
            "If you want my worldly goods, open my chests with my keys. Adventurers occasionally " +
                "find my keys while slaying monsters in the Wilderness, on assignment from the " +
                "witch Krystilia.",
        )
        chatNpc(
            neutral,
            "I do not know why Krystilia sends adventurers to find my keys instead of looting " +
                "the chest herself. Perhaps it amuses her to send people into this land to risk " +
                "their lives.",
        )
    }

    private companion object {
        const val LARRANS_KEY = "obj.slayer_wilderness_key"
        val GHOSTSPEAK =
            listOf(
                "obj.amulet_of_ghostspeak",
                "obj.amulet_of_ghostspeak_enchanted",
                "obj.morytania_legs_medium",
                "obj.morytania_legs_hard",
                "obj.morytania_legs_elite",
            )
    }
}
