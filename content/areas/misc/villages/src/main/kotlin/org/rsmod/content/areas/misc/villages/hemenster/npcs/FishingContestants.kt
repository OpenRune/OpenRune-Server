package org.rsmod.content.areas.misc.villages.hemenster.npcs

import dev.openrune.types.ItemServerType
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpcU
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:fishing_contest] contest dialogue for Bonzo, Morris and the contestants
class FishingContestants : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.bigdave") { startDialogue(it.npc) { bigDave() } }
        onOpNpc1("npc.joshua") { startDialogue(it.npc) { joshua() } }
        onOpNpc1("npc.sinister_stranger") { startDialogue(it.npc) { sinisterStranger() } }
        onOpNpcU("npc.sinister_stranger") { useOnStranger(it.npc, it.objType) }
        onOpNpc1("npc.morris") { startDialogue(it.npc) { morris() } }
        onOpNpc1("npc.bonzo") { startDialogue(it.npc) { bonzo() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Big_Dave
    private suspend fun Dialogue.bigDave() {
        chatNpc(happy, "Hey guy! Always nice to see a fresh face!")
        val option =
            choice3(
                "So you're the champ?",
                1,
                "Can I fish here instead of you?",
                2,
                "Do you have any tips for me?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "So you're the champ?")
                chatNpc(
                    happy,
                    "That's right, guy! Ain't nobody better at fishing round here than me! " +
                        "That's for sure!",
                )
            }
            2 -> {
                chatPlayer(quiz, "Can I fish here instead of you?")
                chatNpc(neutral, "Sorry guy! This is my lucky spot!")
            }
            3 -> {
                chatPlayer(quiz, "Do you have any tips for me?")
                chatNpc(
                    angry,
                    "Why would I help you? I wanna stay the best! I'm not givin' away my secrets " +
                        "like old Grampa Jack does!",
                )
                chatPlayer(quiz, "Who's Grampa Jack?")
                chatNpc(
                    neutral,
                    "You really have no clue do you! He won this competition four years in a " +
                        "row! He lives in the house just outside the gate.",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Joshua
    private suspend fun Dialogue.joshua() {
        chatNpc(angry, "Yeah? What you want?")
        val option =
            choice3(
                "Um... nothing really...",
                1,
                "Can I fish here instead of you?",
                2,
                "Do you have any tips for me?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(confused, "Um... nothing really...")
                chatNpc(angry, "Quit bugging me then! I got me some fish to catch!")
            }
            2 -> {
                chatPlayer(quiz, "Can I fish here instead of you?")
                chatNpc(neutral, "Nuh uh dude. Less talk, more fishing!")
            }
            3 -> {
                chatNpc(
                    angry,
                    "Dude! Why should I help you? You like, might beat me! I'm not giving away my " +
                        "secrets like that dude Grampa Jack does!",
                )
                chatPlayer(quiz, "Who's Grampa Jack?")
                chatNpc(
                    neutral,
                    "Who's Grampa Jack ${pronoun()}! He won this competition four years in a " +
                        "row! He lives in the house just outside the gate.",
                )
            }
        }
    }

    private fun Dialogue.pronoun(): String =
        when (player.appearance.subjectPronoun()) {
            "He" -> "he says"
            "She" -> "she says"
            else -> "they say"
        }

    // https://oldschool.runescape.wiki/w/Transcript:Sinister_Stranger
    private suspend fun Dialogue.sinisterStranger() {
        chatNpc(neutral, "...")
        val option = choice3("...?", 1, "Who are you?", 2, "So... you like fishing?", 3)
        when (option) {
            1 -> {
                chatPlayer(confused, "...?")
                chatNpc(neutral, "...")
                chatPlayer(confused, ".....?")
                chatNpc(neutral, "......")
            }
            2 -> whoAreYou()
            3 -> likeFishing()
        }
    }

    private suspend fun Dialogue.whoAreYou() {
        chatPlayer(quiz, "Who are you?")
        chatNpc(
            neutral,
            "My name iz Vlad. I come from far avay, vere the sun iz not so bright.",
        )
        while (true) {
            val option =
                choice3(
                    "You're a vampyre aren't you?",
                    1,
                    "Is it nice there?",
                    2,
                    "So you like fishing?",
                    3,
                )
            when (option) {
                1 -> {
                    vampyre()
                    return
                }
                2 -> {
                    chatPlayer(quiz, "Is it nice there?")
                    chatNpc(happy, "It is vonderful! Ze beautiful und ze nights are long!")
                }
                3 -> {
                    likeFishing()
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.vampyre() {
        chatPlayer(quiz, "You're a vampyre aren't you?")
        chatNpc(
            angry,
            "Just because I can't stand ze smell ov garlic und I don't like bright sunlight " +
                "doesn't necessarily mean I'm ein vampyre!",
        )
    }

    private suspend fun Dialogue.likeFishing() {
        chatPlayer(quiz, "So... you like fishing?")
        chatNpc(
            neutral,
            "My doctor told me to take up ein velaxing hobby. Vhen I am stressed I tend to get " +
                "ein little...",
        )
        chatNpc(shifty, "..thirsty.")
        val option =
            choice3(
                "You're a vampyre aren't you?",
                1,
                "If you get thirsty you should drink something.",
                2,
                "Well, good luck with the fishing.",
                3,
            )
        when (option) {
            1 -> vampyre()
            2 -> {
                chatPlayer(neutral, "If you get thirsty you should drink something.")
                chatNpc(shifty, "I tsink I may do zat soon...")
            }
            3 -> {
                chatPlayer(happy, "Well, good luck with the fishing.")
                chatNpc(neutral, "Luck haz notsing to do vith it it iz all in ze technique.")
            }
        }
    }

    private suspend fun ProtectedAccess.useOnStranger(npc: Npc, obj: ItemServerType) {
        if (obj.internalName != GARLIC) {
            mes("Nothing interesting happens.")
            return
        }
        startDialogue(npc) {
            chatNpc(
                angry,
                "Uurrggh! Get zat horrible tsing avay from me! How do people like to eat zat " +
                    "stuff? I can't stand even to be near it for tsen zeconds!",
            )
            chatPlayer(worried, "Umm... sorry")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Morris
    // TODO(content): [quest:fishing_contest] competition pass check at the gate
    // TODO(content): [quest:land_of_the_goblins] "I need to catch a Hemenster Whitefish."
    private suspend fun Dialogue.morris() {
        chatPlayer(quiz, "What are you sitting around here for?")
        chatNpc(
            neutral,
            "I'm making sure only those with a competition pass enter the fishing contest.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bonzo (after Fishing Contest)
    private suspend fun Dialogue.bonzo() {
        chatNpc(happy, "Hello champ!")
        chatNpc(quiz, "So any hints on how to fish?")
        chatPlayer(shifty, "I think I'll keep them to myself.")
    }

    private companion object {
        const val GARLIC = "obj.garlic"
    }
}
