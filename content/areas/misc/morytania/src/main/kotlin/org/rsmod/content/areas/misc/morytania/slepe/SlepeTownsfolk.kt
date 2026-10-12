package org.rsmod.content.areas.misc.morytania.slepe

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class SlepeTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.slepe_lector") { startDialogue(it.npc) { lectorGura() } }
        onOpNpc1("npc.slepe_sister1") { startDialogue(it.npc) { sisterSevi() } }
        onOpNpc1("npc.slepe_sister2") { startDialogue(it.npc) { sisterToen() } }
        onOpNpc1("npc.slepe_sister3") { startDialogue(it.npc) { sisterYram() } }
        onOpNpc1("npc.slepe_posh_man1") { startDialogue(it.npc) { drinker() } }
        onOpNpc1("npc.slepe_posh_man2") { startDialogue(it.npc) { drinker() } }
        onOpNpc1("npc.slepe_posh_man3") { startDialogue(it.npc) { drinker() } }
        for (resident in RESIDENTS) {
            onOpNpc1(resident) { startDialogue(it.npc) { resident() } }
        }
        for (child in CHILDREN) {
            onOpNpc1(child) { startDialogue(it.npc) { child() } }
        }
        onOpNpc1("npc.slepe_monk_vis") { startDialogue(it.npc) { paintedOne() } }
        onOpNpc1("npc.slepe_damien_vis") { startDialogue(it.npc) { damien() } }
        onOpNpc1("npc.slepe_crombwick_vis") { startDialogue(it.npc) { lordCrombwick() } }
        onOpNpc1("npc.slepe_boatman_return") { startDialogue(it.npc) { andras() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lector_Gura
    private suspend fun Dialogue.lectorGura() {
        chatPlayer(happy, "Hello!")
        chatNpc(angry, "May the chaos rot your soul. Who are you?")
        val option = choice3("Who are you?", 1, "What do you do here?", 2, "Bye!", 3)
        when (option) {
            1 -> {
                chatPlayer(quiz, "Who are you?")
                chatNpc(neutral, "My name is Gura.")
                chatPlayer(happy, "Nice to meet you Gura, my name is ${player.displayName}.")
                chatNpc(quiz, "Did you want something?")
                chatPlayer(quiz, "What have you got?")
                chatNpc(confused, "No, I mean... Nevermind.")
            }
            2 -> {
                chatPlayer(quiz, "What do you do here?")
                chatNpc(neutral, "I speak the teachings of Zamorak.")
                chatPlayer(quiz, "So, fire and brimstone?")
                chatNpc(neutral, "Amongst other things.")
                chatPlayer(quiz, "Like?")
                chatNpc(
                    neutral,
                    "Well, traveller, if you hadn't already noticed, a foul plague has beset " +
                        "this town. Not only that, but there is talk of a great evil stirring " +
                        "below...",
                )
                chatPlayer(
                    worried,
                    "I see... So is the plague contagious? Because I could be elsewhere, now you " +
                        "mention it.",
                )
                chatNpc(
                    neutral,
                    "Well, we're taking precautions, but you should be fine. Providing you don't " +
                        "stick your nose where it doesn't belong, that is.",
                )
                chatPlayer(happy, "Keep my nose to myself, gotcha!")
            }
            3 -> {
                chatPlayer(happy, "Bye!")
                chatNpc(neutral, "May the flames guide you.")
                chatPlayer(confused, "I'm... sure they will.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sister_Sevi
    private suspend fun Dialogue.sisterSevi() {
        sister(
            preQuestOption = "What's wrong with that old man?",
            preQuest = {
                chatPlayer(quiz, "What's wrong with that old man?")
                chatNpc(
                    sad,
                    "He's not looking great, is he? We fear he is a victim of the Sleeper Plague.",
                )
                chatPlayer(quiz, "Sleeper Plague?")
                chatNpc(
                    neutral,
                    "Many years ago, this village was beset by a terrible plague. Those infected " +
                        "fell into a deep sleep, never to wake. Now we fear the plague has " +
                        "returned.",
                )
                chatPlayer(quiz, "Do you any theories on why it's come back?")
                chatNpc(
                    neutral,
                    "Well, there's the strange man being accused out in the centre, but " +
                        "personally I think he's innocent.",
                )
                chatPlayer(quiz, "Hmm, so how long has the old man been asleep?")
                chatNpc(
                    neutral,
                    "Oh, I can't recall exactly, but at least a few weeks. He has spoken a few " +
                        "times though, in his sleep.",
                )
                chatPlayer(quiz, "What did he say?")
                chatNpc(
                    confused,
                    "It was nonsense, but I recall the words 'Inyetat' and 'Coshmar', whatever " +
                        "those means.",
                )
                chatPlayer(confused, "How peculiar.")
            },
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sister_Toen
    private suspend fun Dialogue.sisterToen() {
        sister(
            preQuestOption = "What's wrong with that woman?",
            preQuest = {
                chatPlayer(quiz, "What's wrong with that woman?")
                chatNpc(
                    sad,
                    "Oh, she's Lord Crombwick's wife and the first victim of the Sleeper Plague.",
                )
                chatPlayer(quiz, "Lord Crombwick?")
                chatNpc(neutral, "The ruler of this place. He lives in his manor, to the north.")
                chatPlayer(
                    quiz,
                    "Where's the Lord? Surely he should be down here with his wife?",
                )
                chatNpc(neutral, "Still in his manor. He hasn't left since she fell ill.")
                chatPlayer(quiz, "Why?")
                chatNpc(
                    worried,
                    "No one knows. The place looks like a mess and some say they've seen " +
                        "vampyres lurking.",
                )
                chatPlayer(confused, "How mysterious.")
                chatNpc(neutral, "Indeed.")
            },
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sister_Yram
    private suspend fun Dialogue.sisterYram() {
        sister(
            preQuestOption = "What's going on here?",
            preQuest = {
                chatPlayer(quiz, "What's going on here?")
                chatNpc(neutral, "With this strange tattooed man? He is on trial.")
                chatPlayer(quiz, "For what crime?")
                chatNpc(
                    neutral,
                    "It is suspected he might have caused the return of the Sleeper Plague.",
                )
                chatPlayer(quiz, "Sleeper Plague?")
                chatNpc(
                    neutral,
                    "Yes. A long time ago, Slepe was beset by an illness called the Sleeper " +
                        "Plague. Those infected fell into a deep sleep, never to wake.",
                )
                chatPlayer(quiz, "But now it's back?")
                chatNpc(
                    neutral,
                    "It would appear so. This man showed up in town around the same time the " +
                        "first victim fell ill. We fear this is no coincidence.",
                )
                chatPlayer(neutral, "That doesn't make him guilty though.")
                chatNpc(neutral, "No. That is for the trial to determine.")
            },
            postQuestOption = "What happened to the tattooed man?",
            postQuest = {
                chatPlayer(quiz, "What happened to the tattooed man?")
                chatNpc(
                    neutral,
                    "His trial has concluded. The citizens have passed their judgement.",
                )
                chatPlayer(quiz, "And?")
                chatNpc(
                    neutral,
                    "The accused was found guilty of causing the return of the Sleeper Plague. " +
                        "He has now been executed.",
                )
                chatPlayer(shocked, "Executed? That's pretty dark.")
                chatNpc(
                    neutral,
                    "Morytania is a dark place, traveller. If you wished for light, you should " +
                        "have picked a better holiday destination.",
                )
                chatPlayer(quiz, "So that's it?")
                chatNpc(
                    neutral,
                    "Since the execution, we have had no new occurrences of the Sleeper Plague. " +
                        "So yes, that's it.",
                )
            },
        )
    }

    // TODO(content): [verify:in_game] whether the mask answer returns to the options
    private suspend fun Dialogue.sister(
        preQuestOption: String,
        preQuest: suspend Dialogue.() -> Unit,
        postQuestOption: String = "How are things with the Sleeper Plague?",
        postQuest: suspend Dialogue.() -> Unit = { sleeperPlague() },
    ) {
        chatPlayer(happy, "Hello!")
        chatNpc(neutral, "Greetings, traveller.")
        val questDone = QuestRequirements.hasCompleted(player, SINS_OF_THE_FATHER)
        while (true) {
            val option =
                choice4(
                    "What's with the mask?",
                    1,
                    "Tell me more about the Sisterhood.",
                    2,
                    if (questDone) postQuestOption else preQuestOption,
                    3,
                    "Bye!",
                    4,
                )
            when (option) {
                1 -> sisterhoodMask()
                2 -> sisterhoodHistory()
                3 -> if (questDone) postQuest() else preQuest()
                4 -> {
                    chatPlayer(happy, "Bye!")
                    chatNpc(neutral, "Be safe, traveller.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.sisterhoodMask() {
        chatPlayer(quiz, "What's with the mask?")
        chatNpc(
            neutral,
            "These are the masks of the Sisterhood. While symbolic, they also have practical " +
                "uses.",
        )
        chatPlayer(quiz, "Oh really? What like?")
        chatNpc(
            neutral,
            "Herbs can be stored within the beak, masking the smell of death and preventing us " +
                "from becoming infected.",
        )
        chatPlayer(happy, "That sounds handy. Can I have one?")
        chatNpc(angry, "You ask too many questions.")
        chatPlayer(quiz, "Is that a yes?")
        chatNpc(
            neutral,
            "It's a no. To wear a mask of the Sisterhood, you need to join the Sisterhood.",
        )
        chatPlayer(quiz, "What is the Sisterhood?")
        chatNpc(neutral, "An ancient order, born right here in Slepe.")
        chatPlayer(happy, "Well sign me up!")
        chatNpc(
            neutral,
            "You lack the qualities we're currently looking for. Perhaps another time.",
        )
    }

    private suspend fun Dialogue.sisterhoodHistory() {
        chatPlayer(quiz, "Tell me more about the Sisterhood.")
        chatNpc(
            neutral,
            "Many years ago, the Sisterhood were a group of healers dedicated to Saradomin.",
        )
        chatNpc(
            neutral,
            "However, our leader, a justiciar known as Phosani, saw the truth in Saradomin's " +
                "teachings and turned away from his false light.",
        )
        chatNpc(
            neutral,
            "Phosani embraced the glory of Zamorak and most of our order did the same. After " +
                "that, she became known as the Inquisitor, though our order refers to her as " +
                "'Mother'.",
        )
        chatNpc(
            neutral,
            "After the order reached enlightenment through Zamorak, we made our home in an " +
                "abandoned hermitage. Over time, that hermitage grew into the town you see " +
                "today.",
        )
        chatPlayer(quiz, "What happened to those that refused to turn away from Saradomin?")
        chatNpc(neutral, "They were shown the error of their ways through fire and blood.")
        chatPlayer(shifty, "How wonderful.")
    }

    private suspend fun Dialogue.sleeperPlague() {
        chatPlayer(quiz, "How are things with the Sleeper Plague?")
        chatNpc(
            neutral,
            "The one responsible was found out and executed. We haven't had any new cases since.",
        )
        chatPlayer(quiz, "I see... And what of those still ill?")
        chatNpc(
            neutral,
            "Well now that the cause has been dealt with, we can focus more heavily on a cure.",
        )
        chatPlayer(quiz, "And how long will that take?")
        chatNpc(neutral, "Well that depends...")
        chatPlayer(quiz, "On what?")
        chatNpc(shifty, "On how things progress... down below.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lahsram
    // https://oldschool.runescape.wiki/w/Transcript:Erodoeht
    // https://oldschool.runescape.wiki/w/Transcript:Yenrab
    private suspend fun Dialogue.drinker() {
        chatPlayer(happy, "Hello there.")
        chatNpc(angry, "Clear off! Let me drink in peace!")
        chatPlayer(neutral, "Okay then.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Kurt
    // https://oldschool.runescape.wiki/w/Transcript:Don
    // https://oldschool.runescape.wiki/w/Transcript:Debra
    // https://oldschool.runescape.wiki/w/Transcript:Tanya_(Slepe)
    private suspend fun Dialogue.resident() {
        if (QuestRequirements.hasCompleted(player, SINS_OF_THE_FATHER)) {
            chatNpc(angry, "Whatever it is you're selling, we ain't interested!")
            chatPlayer(confused, "But I'm not...")
            chatNpc(angry, "Did you not hear me? Clear off!")
            return
        }
        chatPlayer(happy, "Hello.")
        chatNpc(worried, "Not infected are you?")
        chatPlayer(confused, "I don't think so.")
        chatNpc(
            angry,
            "You don't seem very sure. We don't want any infected near the children. Be off " +
                "with you!",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Child_(Slepe)
    private suspend fun Dialogue.child() {
        chatPlayer(happy, "Hello.")
        chatNpc(neutral, "Mother says I shouldn't speak with strangers.")
        chatPlayer(neutral, "Fair enough.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Painted_One
    private suspend fun Dialogue.paintedOne() {
        chatPlayer(quiz, "What's going on?")
        chatNpc(silent, "...")
        chatPlayer(neutral, "Speaking would certainly help your case.")
        chatNpc(silent, "...")
        chatPlayer(neutral, "Well, okay then.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Damien_Leucurte
    private suspend fun Dialogue.damien() {
        chatPlayer(quiz, "You seem to really enjoy that painting.")
        chatNpc(silent, "...")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lord_Crombwick
    private suspend fun Dialogue.lordCrombwick() {
        chatPlayer(quiz, "Are you alright?")
        chatNpc(silent, "...")
        chatNpcSpecific(DAMIEN_NAME, DAMIEN, neutral, "You may speak.")
        chatNpc(worried, "Oh, yes yes, I'm fine.")
        chatPlayer(quiz, "You look a little beat up.")
        chatNpc(worried, "These scratches? Don't worry, just a disagreement with a thorn bush.")
        chatPlayer(shifty, "Sure...")
        chatNpcSpecific(DAMIEN_NAME, DAMIEN, angry, "Is there something you wanted, bloodbag?")
        chatPlayer(neutral, "Just looking around.")
        chatNpcSpecific(
            DAMIEN_NAME,
            DAMIEN,
            angry,
            "Well, I'd prefer you did it elsewhere, Crombwick is busy entertaining his guests.",
        )
        chatPlayer(shifty, "Strange bunch of guests...")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Andras (Slepe)
    // TODO(content): [system:ship_travel] sailing back to Port Phasmatys
    private suspend fun Dialogue.andras() {
        chatNpc(neutral, "Hello there. Want to return to Port Phasmatys?")
        val option =
            choice3(
                "Yes please.",
                1,
                "Is there anything to do around here?",
                2,
                "No thank you.",
                3,
            )
        when (option) {
            1 -> chatPlayer(happy, "Yes please.")
            2 -> {
                chatPlayer(quiz, "Is there anything to do around here?")
                chatNpc(
                    neutral,
                    "Well you have Slepe just north of here. It's mostly a quiet place, though " +
                        "there's been rumours recently of a great evil making its home beneath " +
                        "the village.",
                )
                chatNpc(
                    neutral,
                    "There's also Ver Sinhaza. You can find it south of here. There's a gap in " +
                        "the northern wall that you can get through.",
                )
                chatPlayer(quiz, "What's Ver Sinhaza?")
                chatNpc(
                    neutral,
                    "It's a large vampyric castle, home to the Theatre of Blood. The vampyres " +
                        "claim that the Theatre is one of the greatest combat challenges " +
                        "around. Definitely worth a look even if you don't want to participate.",
                )
                chatNpc(quiz, "Anyway, did you want to return to Port Phasmatys?")
                if (choice2("Yes please.", true, "No thank you.", false)) {
                    chatPlayer(happy, "Yes please.")
                } else {
                    chatPlayer(neutral, "No thank you.")
                }
            }
            3 -> chatPlayer(neutral, "No thank you.")
        }
    }

    private companion object {
        const val SINS_OF_THE_FATHER = "quest_sinsofthefather"
        const val DAMIEN = "npc.slepe_damien_vis"
        const val DAMIEN_NAME = "Damien Leucurte"

        val RESIDENTS =
            listOf(
                "npc.slepe_poor_man1_vis",
                "npc.slepe_poor_man2_vis",
                "npc.slepe_poor_woman1_vis",
                "npc.slepe_poor_woman2_vis",
            )

        val CHILDREN =
            listOf(
                "npc.slepe_child_female_1_vis",
                "npc.slepe_child_female_2_vis",
                "npc.slepe_child_female_3_vis",
                "npc.slepe_child_female_4_vis",
                "npc.slepe_child_male_2_vis",
                "npc.slepe_child_male_3_vis",
                "npc.slepe_child_male_4_vis",
            )
    }
}
