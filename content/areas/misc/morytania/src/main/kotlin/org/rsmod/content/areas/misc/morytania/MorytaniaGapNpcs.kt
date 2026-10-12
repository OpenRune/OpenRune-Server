package org.rsmod.content.areas.misc.morytania

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class MorytaniaGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.ahoy_crone") { startDialogue(it.npc) { oldCrone() } }
        onOpNpc1("npc.araxxor_sister_weave") { startDialogue(it.npc) { weave() } }
        onOpNpc1("npc.slayer_tower_dungeon_ghost") { startDialogue(it.npc) { raulyn() } }
        onOpNpc1("npc.fenk_gardener") { startDialogue(it.npc) { gardenerGhost() } }
        onOpNpc1("npc.route_polmafi_ferdygris") { startDialogue(it.npc) { polmafi() } }
        onOpNpc1("npc.route_radigad_ponfit") { startDialogue(it.npc) { radigad() } }
    }

    private fun Dialogue.completed(quest: String): Boolean =
        QuestRequirements.hasCompleted(player, quest)

    private fun Player.understandsGhosts(): Boolean = GHOSTSPEAK.any { it in worn }

    // https://oldschool.runescape.wiki/w/Transcript:Old_crone
    // TODO(content): [verify:in_game] the transcript has no dialogue for having finished only one
    //  of Animal Magnetism and Ghosts Ahoy; the before-quests line is used
    // TODO(content): [quest:ghosts_ahoy] the in-quest dialogue
    // TODO(content): [quest:animal_magnetism] the in-quest dialogue
    private suspend fun Dialogue.oldCrone() {
        if (!completed(ANIMAL_MAGNETISM) || !completed(GHOSTS_AHOY)) {
            chatPlayer(neutral, "Hello, old woman.")
            chatNpc(neutral, "I'm over 100 years old, you know.")
            return
        }
        val option =
            choice2(
                "I'm here about Necrovarus.",
                1,
                "I'm here about the farmers east of here.",
                2,
            )
        if (option == 1) {
            chatPlayer(neutral, "I'm here about Necrovarus.")
            chatPlayer(happy, "Good news - the enchantment worked!")
            chatNpc(
                happy,
                "So Necrovarus has let the ghosts go free.... You have achieved many wonderful " +
                    "things, my young friend.",
            )
            return
        }
        chatPlayer(neutral, "I'm here about the farmers east of here.")
        chatPlayer(
            happy,
            "Hello there, just dropping in to thank you again. It all went perfectly. Alice and " +
                "her husband can communicate again.",
        )
        chatNpc(
            sad,
            "I can't help but shed a tear about my own family. My sisters are so vile, I refuse " +
                "to talk to them these days.",
        )
        chatPlayer(sad, "Sorry to hear that.")
        chatNpc(
            happy,
            "Never mind; we've reunited a happy family of farmers; that's enough for me.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Weave
    private suspend fun Dialogue.weave() {
        chatNpc(happy, "Greetings, stranger. How can I help you?")
        while (true) {
            val option =
                choice3(
                    "What is this place?",
                    1,
                    "Discovered anything interesting?",
                    2,
                    "Goodbye.",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What is this place?")
                    chatNpc(
                        neutral,
                        "It's a cave filled with araxytes, a rare form of arachnid. I've been " +
                            "here researching them for some time.",
                    )
                    val relative =
                        if (completed(MONKEY_MADNESS_II)) {
                            "My nephew, Steve,"
                        } else {
                            "My niece, Nieve,"
                        }
                    chatNpc(
                        neutral,
                        "$relative is one of those Slayer Masters. You know, they tell people " +
                            "what to go kill. I also look after this cave for people on " +
                            "assignments like that.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Discovered anything interesting?")
                    chatNpc(neutral, "Hmm... Not really.")
                    chatNpc(
                        neutral,
                        "However, the araxytes here are rather venomous... and when I say " +
                            "venomous, I mean venomous.",
                    )
                    chatPlayer(confused, "Is that really noteworthy?")
                    chatNpc(
                        neutral,
                        "Well, I didn't think so by itself, but lots of other people seem to " +
                            "find it very interesting.",
                    )
                    chatNpc(
                        neutral,
                        "For example, there were some particularly pale wanderers doing their " +
                            "own research in here. They didn't bother me though.",
                    )
                    chatPlayer(shifty, "Hmm...")
                }
                else -> {
                    chatPlayer(neutral, "Goodbye.")
                    chatNpc(happy, "Bye bye.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Raulyn
    private suspend fun Dialogue.raulyn() {
        if (!player.understandsGhosts()) {
            chatNpc(neutral, "Wooooo! Ooooooooooohh!")
            chatPlayer(
                confused,
                "Hmm, I wonder if there's something I could use to communicate with him.",
            )
            return
        }
        chatNpc(shocked, "Aaah! What are you doing down here!?")
        chatPlayer(neutral, "Hey, keep it down! I've come to slay the monsters down here.")
        chatNpc(neutral, "Oh, that's ok then. These vile creatures do make an awful racket.")
        chatPlayer(
            neutral,
            "You should hear it upstairs! Those Aberrant Spectres are just the worst.",
        )
        chatPlayer(quiz, "Who are you anyway?")
        chatNpc(
            neutral,
            "I'm Raulyn. Once servant to the great Viggora who built this place centuries ago.",
        )
        chatNpc(
            neutral,
            "This was, of course, before the building started to sink into the swamp...",
        )
        chatNpc(
            angry,
            "Oh, and the darn Zamorakian mages. They didn't help matters. Always making a mess.",
        )
        chatNpc(
            sad,
            "I've been here ever since. You're the first person I've spoken to in over 200 years.",
        )
        chatPlayer(quiz, "Don't you ever get lonely?")
        chatNpc(happy, "Sometimes, but it's not all that bad. I hope you stop by more often!")
        chatPlayer(happy, "Hopefully! I should be going now, speak to you later.")
        chatNpc(happy, "Goodbye stranger.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gardener_Ghost
    private suspend fun Dialogue.gardenerGhost() {
        val afterQuest = completed(CREATURE_OF_FENKENSTRAIN)
        chatPlayer(quiz, if (afterQuest) "How are you?" else "Who are you?")
        if (!player.understandsGhosts()) {
            mesbox(
                "The Headless Gardener's neck twitches at you, but the lack of any head prevents " +
                    "him from speaking.",
            )
            return
        }
        if (!afterQuest) {
            chatNpc(neutral, "Oi'm the 'eadless gardener, mate.")
            return
        }
        chatNpc(neutral, "Same as ever, mate, just getting' on with it regardless.")
        chatPlayer(confused, "Good for you...err...mate.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Polmafi_Ferdygris
    // TODO(content): [quest:in_search_of_the_myreque] the in-quest dialogue
    // TODO(content): [verify:in_game] the transcript has no dialogue once Sins of the Father is
    //  started; the Darkness of Hallowvale line is used
    private suspend fun Dialogue.polmafi() {
        val sinsStarted =
            QuestRequirements.isOnQuest(player, SINS_OF_THE_FATHER) || completed(SINS_OF_THE_FATHER)
        if (completed(A_TASTE_OF_HOPE) && !sinsStarted) {
            fullMember()
            chatPlayer(quiz, "Thanks. Speaking of Veliaf, where is he?")
            chatNpc(
                neutral,
                "He's out on business. Don't worry, he knows how to take care of himself.",
            )
            return
        }
        if (completed(DARKNESS_OF_HALLOWVALE)) {
            return fullMember()
        }
        if (completed(IN_AID_OF_THE_MYREQUE)) {
            chatNpc(
                happy,
                "Hey there. It's great of you to do all this work for Veliaf. I've heard him say " +
                    "some good things about you. Keep up the good work and I'm sure you'll be " +
                    "invited to become a full member of the Myreque!",
            )
            return
        }
        chatPlayer(happy, "Hey, how's it going?")
        chatNpc(sad, "Not so good. It's great to see you're helping us out though.")
    }

    private suspend fun Dialogue.fullMember() {
        chatNpc(
            happy,
            "Hey there. It's brilliant that you've been made into a full member of the Myreque!",
        )
        chatNpc(happy, "Keep up the good work, I'm sure Veliaf appreciates it.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Radigad_Ponfit
    // TODO(content): [quest:in_search_of_the_myreque] the in-quest dialogue
    private suspend fun Dialogue.radigad() {
        if (completed(IN_AID_OF_THE_MYREQUE)) {
            chatNpc(
                happy,
                "Thanks for doing such good work on the new hideout. Of course, we've tidied up " +
                    "a bit as you can see.",
            )
            return
        }
        chatPlayer(happy, "Hello again.")
        chatNpc(
            neutral,
            "Hello. I hear you're doing some important work for Veliaf. You'd best get to it.",
        )
    }

    private companion object {
        const val ANIMAL_MAGNETISM = "quest_animalmagnetism"
        const val GHOSTS_AHOY = "quest_ghostsahoy"
        const val MONKEY_MADNESS_II = "quest_monkeymadness2"
        const val CREATURE_OF_FENKENSTRAIN = "quest_creatureoffenkenstrain"
        const val IN_AID_OF_THE_MYREQUE = "quest_inaidofthemyreque"
        const val DARKNESS_OF_HALLOWVALE = "quest_darknessofhallowvale"
        const val A_TASTE_OF_HOPE = "quest_tasteofhope"
        const val SINS_OF_THE_FATHER = "quest_sinsofthefather"

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
