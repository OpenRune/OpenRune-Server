package org.rsmod.content.areas.city.varrock.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Apothecary
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:apothecary_potions] Potions interface (op3 and "Can you make potions for
// me?")
// TODO(content): [system:adventure_paths] Combat path voucher reward
// TODO(content): [quest:a_tail_of_two_cats] replacing the lost nurse's or doctor's hat
// TODO(content): [quest:ratcatchers] "Talk about the Ratcatchers Quest."
// TODO(content): [quest:romeo_and_juliet] quest dialogue and the after-quest gossip
// TODO(content): [quest:shades_of_mortton] trading the diary for Herblore training
class Apothecary : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.apothecary") { startDialogue(it.npc) { apothecary() } }
    }

    private suspend fun Dialogue.apothecary() {
        chatNpc(happy, "I am the Apothecary. I brew potions. Do you need anything specific?")
        while (true) {
            when (choice2("Can you make potions for me?", 1, "Talk about something else.", 2)) {
                1 -> {
                    chatPlayer(quiz, "Can you make potions for me?")
                    return
                }
                2 -> if (!somethingElse()) return
            }
        }
    }

    private suspend fun Dialogue.somethingElse(): Boolean {
        val option =
            choice4(
                "Have you got any decent gossip to share?",
                1,
                "Do you know a potion to make hair fall out?",
                2,
                "Have you got any good potions to give away?",
                3,
                "No thanks.",
                4,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Have you got any decent gossip to share?")
                chatNpc(
                    neutral,
                    "Well I hear young Romeo's having a little woman trouble but other than that " +
                        "all's quiet on the eastern front. Can I do something for you?",
                )
            }
            2 -> {
                chatPlayer(quiz, "Do you know a potion to make hair fall out?")
                chatNpc(
                    laugh,
                    "I do indeed. I gave it to my mother. That's why I now live alone.",
                )
                chatNpc(happy, "But can I do something for you?")
            }
            3 -> {
                chatPlayer(quiz, "Have you got any good potions to give away?")
                chatNpc(
                    neutral,
                    "Sorry, charity is not my strong point. Do you need anything else?",
                )
            }
            4 -> {
                chatPlayer(neutral, "No thanks.")
                return false
            }
        }
        return true
    }
}
