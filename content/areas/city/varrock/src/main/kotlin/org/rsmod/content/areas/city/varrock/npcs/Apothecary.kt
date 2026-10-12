package org.rsmod.content.areas.city.varrock.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.content.quest.manager.menu
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Apothecary
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:apothecary_potions] Potions interface (op3 and "Can you make potions for
// me?")
// TODO(content): [system:adventure_paths] Combat path voucher reward
// TODO(content): [quest:romeo_and_juliet] "Talk about Romeo & Juliet." quest dialogue
// TODO(content): [quest:shades_of_mortton] trading the diary for Herblore training
class Apothecary : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.apothecary") { startDialogue(it.npc) { apothecary() } }
    }

    private suspend fun Dialogue.apothecary() {
        chatNpc(happy, "I am the Apothecary. I brew potions. Do you need anything specific?")
        while (true) {
            val options =
                buildList {
                    add("Can you make potions for me?" to 1)
                    if (hasLostHat()) {
                        add("Talk about A Tail of Two Cats." to 3)
                    }
                    add("Talk about something else." to 2)
                }
            when (menu(options)) {
                1 -> {
                    chatPlayer(quiz, "Can you make potions for me?")
                    return
                }
                2 -> if (!somethingElse()) return
                3 -> {
                    replaceHat()
                    return
                }
            }
        }
    }

    private fun Dialogue.hasLostHat(): Boolean =
        QuestRequirements.hasCompleted(player, TAIL_OF_TWO_CATS) &&
            HATS.none { it in player.inv || it in player.worn || it in access.bank }

    // https://oldschool.runescape.wiki/w/Transcript:Apothecary (lost A Tail of Two Cats hat)
    private suspend fun Dialogue.replaceHat() {
        chatPlayer(sad, "Hello Apothecary, I've lost that hat you gave me.")
        chatNpc(happy, "No problem, I get plenty of these from Gielinor Hospital Supplies.")
        val (hat, name) =
            choice2(
                "Nurse's Hat",
                NURSES_HAT to "Nurse's Hat",
                "Doctor's Hat",
                DOCTORS_HAT to "Doctor's Hat",
                title = "Choose a hat:",
            )
        // TODO(content): [verify:in_game] full-inventory behaviour (not in the transcript)
        if (access.invAdd(access.inv, hat).failure) {
            access.mes("You don't have enough inventory space.")
            return
        }
        objbox(hat, "The Apothecary has given you a $name.")
    }

    private suspend fun Dialogue.somethingElse(): Boolean {
        val options =
            buildList {
                if (QuestRequirements.hasCompleted(player, RATCATCHERS)) {
                    add("Talk about the Ratcatchers Quest." to 5)
                }
                add("Have you got any decent gossip to share?" to 1)
                add("Do you know a potion to make hair fall out?" to 2)
                add("Have you got any good potions to give away?" to 3)
                add("No thanks." to 4)
            }
        when (menu(options)) {
            1 -> {
                chatPlayer(quiz, "Have you got any decent gossip to share?")
                if (QuestRequirements.hasCompleted(player, ROMEO_AND_JULIET)) {
                    chatNpc(sad, "Sad about that affair with young Romeo and Juliet...")
                    chatNpc(
                        neutral,
                        "I hear every time Romeo sees Juliet now he runs away screaming " +
                            "something about ghosts and Juliet's cousin?",
                    )
                    chatNpc(neutral, "Always did think he was a bit of a strange one...")
                    chatNpc(
                        happy,
                        "Anyway! Life goes on and so does business! Can I do something for you?",
                    )
                } else {
                    chatNpc(
                        neutral,
                        "Well I hear young Romeo's having a little woman trouble but other than " +
                            "that all's quiet on the eastern front. Can I do something for you?",
                    )
                }
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
            5 -> {
                chatPlayer(
                    quiz,
                    "You wouldn't be able to make any other cat potions, would you?",
                )
                chatNpc(
                    neutral,
                    "I have no plans to branch out into cat potions at the moment, but if there " +
                        "was sufficient demand for them I might change my mind.",
                )
                return false
            }
        }
        return true
    }

    private companion object {
        const val RATCATCHERS = "quest_ratcatchers"
        const val ROMEO_AND_JULIET = "quest_romeoandjuliet"
        const val TAIL_OF_TWO_CATS = "quest_tailoftwocats"
        const val NURSES_HAT = "obj.twocats_nurses_hat"
        const val DOCTORS_HAT = "obj.twocats_doctors_hat"
        val HATS = listOf(NURSES_HAT, DOCTORS_HAT)
    }
}
