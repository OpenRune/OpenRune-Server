package org.rsmod.content.areas.city.keldagrim.dorgeshkaan.npcs

import dev.openrune.types.MesAnimType
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:dorgesh_kaan_market] "May I have permission to sell food in the market?"
class DorgeshKaanCouncil : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(UR_TAG) { startDialogue(it.npc) { urTag() } }
        onOpNpc1(ALVIJAR) { startDialogue(it.npc) { alvijar() } }
        onOpNpc1(UR_LUN) { startDialogue(it.npc) { guardColours(UR_LUN) } }
        onOpNpc1(UR_PEL) { startDialogue(it.npc) { guardColours(UR_PEL) } }
        onOpNpc1(UR_TAAL) { startDialogue(it.npc) { surfaceDwellers(UR_TAAL) } }
        onOpNpc1(UR_ZEK) { startDialogue(it.npc) { surfaceDwellers(UR_ZEK) } }
        onOpNpc1("npc.dorgesh_female_council_2") { startDialogue(it.npc) { urMeg() } }
        onOpNpc1("npc.dorgesh_male_council_2") { startDialogue(it.npc) { urVass() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ur-tag (before Another Slice of H.A.M.)
    // TODO(content): [quest:another_slice_of_ham] after-quest greeting and options
    private suspend fun Dialogue.urTag() {
        trainLinkArgument(UR_TAG)
        while (true) {
            val option =
                choice3(
                    "What are you arguing about?",
                    1,
                    "What can I do in the city?",
                    2,
                    "Why did you decide to open the city?",
                    3,
                )
            when (option) {
                1 -> {
                    trainLink(UR_TAG)
                    return
                }
                2 -> {
                    chatPlayer(quiz, "What can I do in the city?")
                    chatNpc(
                        happy,
                        "Why don't you visit the market? I hear there is quite a craze for " +
                            "surface foods at the moment, so you might be able to sell something " +
                            "there.",
                    )
                    chatNpc(
                        neutral,
                        "You might also want to visit the caves to the south of the city. There " +
                            "are many monsters to fight there, and Zanik apparently found ways " +
                            "to cross over their heads!",
                    )
                }
                else -> {
                    chatPlayer(quiz, "Why did you decide to open the city?")
                    chatNpc(
                        happy,
                        "For trade, and for fostering goodwill between races! I am sure that it " +
                            "will benefit both our races.",
                    )
                }
            }
            trainLinkArgument(UR_TAG)
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ambassador_Alvijar (before the quest)
    // TODO(content): [quest:another_slice_of_ham] random lines after the quest
    private suspend fun Dialogue.alvijar() {
        trainLinkArgument(ALVIJAR)
        trainLink(ALVIJAR)
    }

    private suspend fun Dialogue.trainLinkArgument(talkedTo: String) {
        speak(
            talkedTo,
            ALVIJAR,
            angry,
            "Headman Ur-tag, I cannot stress enough how important it is that this train link " +
                "be completed on time. The mining companies have taken out considerable loans...",
        )
        speak(
            talkedTo,
            UR_TAG,
            neutral,
            "I understand your concerns, Ambassador, but you must realise that the discoveries " +
                "must change the whole schedule. To remove them too hurriedly would be a crime " +
                "against knowledge.",
        )
    }

    // TODO(content): [quest:another_slice_of_ham] requirements check and the quest start offer
    private suspend fun Dialogue.trainLink(talkedTo: String) {
        chatPlayer(quiz, "What are you talking about?")
        speak(
            talkedTo,
            UR_TAG,
            neutral,
            "It's the new underground train link being built between our city and the dwarven " +
                "city of Keldagrim.",
        )
        speak(
            talkedTo,
            ALVIJAR,
            happy,
            "It will be the pinnacle of dwarven engineering! A great leap forward from the old " +
                "steam-powered minecarts! A monument to the power and ingenuity of the " +
                "Consortium!",
        )
        speak(
            talkedTo,
            ALVIJAR,
            angry,
            "And it would be finished by now, except that they found some old goblin bones they " +
                "want to dig up.",
        )
        speak(
            talkedTo,
            UR_TAG,
            neutral,
            "We've unearthed some very important artefacts that were buried during the " +
                "Dorgeshuun Civil War. They could shed important light on our history.",
        )
        speak(
            talkedTo,
            UR_TAG,
            quiz,
            "${player.displayName}, do you have any experience with archaeology?",
        )
        chatPlayer(happy, "I passed my archaeology exam at the Digsite!")
        speak(
            talkedTo,
            UR_TAG,
            happy,
            "Excellent! We have not had a major archaeological project for generations, so if " +
                "you could give us some help we would appreciate it.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ur-lun
    // TODO(content): [verify:in_game] Ur-pel's greeting split from the player's question
    private suspend fun Dialogue.guardColours(talkedTo: String) {
        speak(
            talkedTo,
            UR_LUN,
            neutral,
            "At the next council meeting, I'm going to recommend that we change the guards' " +
                "uniform to be red.",
        )
        speak(talkedTo, UR_PEL, quiz, "Why?")
        speak(
            talkedTo,
            UR_LUN,
            neutral,
            "We have surface-dwellers coming down into our mines and our city now, and remember " +
                "that they don't have very good eyesight.",
        )
        speak(
            talkedTo,
            UR_LUN,
            neutral,
            "I worry that they can't see our guards very clearly in the dark. It is important " +
                "for a guard to be seen.",
        )
        speak(
            talkedTo,
            UR_PEL,
            neutral,
            "You have a point. But I don't think red would be the best colour. It is the colour " +
                "of blood, so it might give the wrong impression.",
        )
        speak(talkedTo, UR_LUN, quiz, "What colour do you suggest, then?")
        speak(
            talkedTo,
            UR_PEL,
            neutral,
            "I suggest green. I have heard that the plants that grow on the surface are mostly " +
                "green, so surface-dwellers might find that colour soothing.",
        )
        speak(
            talkedTo,
            UR_LUN,
            angry,
            "No, I disagree. Guards should not be soothing, they should be seen as formidable. " +
                "Red will give the correct impression.",
        )
        speak(
            talkedTo,
            UR_PEL,
            happy,
            "Oh, hello surface-dweller. We were just discussing some council business.",
        )
        chatPlayer(quiz, "Do you want me to pick an armour colour for you?")
        speak(
            talkedTo,
            UR_LUN,
            neutral,
            "No, thank you. We don't let any one single goblin make decisions, and certainly " +
                "not an outsider!",
        )
        speak(
            talkedTo,
            UR_PEL,
            quiz,
            "But we could do with an opinion. What colour do you think is best?",
        )
        val option =
            choice4(
                "Red",
                1,
                "Green",
                2,
                "I think the guards are fine as they are",
                3,
                "I think I've been here before...",
                4,
            )
        when (option) {
            1 -> {
                chatPlayer(neutral, "Red.")
                speak(
                    talkedTo,
                    UR_LUN,
                    happy,
                    "Thank you, surface-dweller. That's what I'll be recommending in the next " +
                        "meeting.",
                )
            }
            2 -> {
                chatPlayer(neutral, "Green.")
                speak(
                    talkedTo,
                    UR_PEL,
                    happy,
                    "Thank you, surface-dweller. That's what I'll be recommending in the next " +
                        "meeting.",
                )
            }
            3 -> {
                chatPlayer(neutral, "I think the guards are fine as they are.")
                speak(
                    talkedTo,
                    UR_LUN,
                    neutral,
                    "Perhaps you're right. Not making any change would certainly be the easiest " +
                        "option!",
                )
            }
            else -> {
                chatPlayer(confused, "I think I've been here before...")
                speak(talkedTo, UR_LUN, quiz, "Oh? How so?")
                chatPlayer(
                    neutral,
                    "This is a lot like a conversation I had with some goblins on the surface.",
                )
                speak(talkedTo, UR_PEL, quiz, "Really? Which armour colour did they pick?")
                chatPlayer(
                    laugh,
                    "They ended up with the same armour colour that they started with!",
                )
                speak(
                    talkedTo,
                    UR_LUN,
                    neutral,
                    "You know, I think that's probably what we'll end up doing.",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ur-zek
    private suspend fun Dialogue.surfaceDwellers(talkedTo: String) {
        speak(
            talkedTo,
            UR_ZEK,
            angry,
            "I think that Ur-tag is trying to move too quickly. First, he opens the city to " +
                "humans, and now he wants to invite these dwarves into the city. I think we " +
                "should exercise more caution in dealing with surface-dwellers.",
        )
        speak(
            talkedTo,
            UR_TAAL,
            neutral,
            "The dwarves are not surface dwellers. They live in an underground city, like us!",
        )
        speak(
            talkedTo,
            UR_ZEK,
            angry,
            "Why does that mean we can trust them? Are you forgetting the machine that nearly " +
                "destroyed us was made by dwarves?",
        )
        speak(talkedTo, UR_TAAL, neutral, "You shouldn't judge all dwarves because of that!")
        speak(
            talkedTo,
            UR_ZEK,
            angry,
            "I know that! And I know not all humans want to kill us, but I still never thought " +
                "we should open the city.",
        )
        speak(
            talkedTo,
            UR_TAAL,
            happy,
            "You can't deny it's done some good. Think of how happy everyone is trying surface " +
                "foods in the market!",
        )
        speak(
            talkedTo,
            UR_ZEK,
            neutral,
            "I'm not denying that, I just don't think it's worth the risk. Our guards have " +
                "their work cut out as it is, protecting us from renegade humans, without " +
                "having to worry about dwarves as well.",
        )
        speak(
            talkedTo,
            UR_TAAL,
            neutral,
            "Please don't be offended by my friend's attitude, surface-dweller. I'm sure you " +
                "understand why we need to be cautious.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ur-meg
    // TODO(content): [verify:in_game] the options of Ur-meg's first menu
    private suspend fun Dialogue.urMeg() {
        chatNpc(worried, "Oh dear...")
        chatPlayer(quiz, "What's the matter?")
        chatNpc(
            worried,
            "I'm worried. I think the Council is neglecting a big issue. I tried to bring it up " +
                "at the last meeting, but they didn't pay attention.",
        )
        chatPlayer(quiz, "What issue?")
        chatNpc(
            worried,
            "I'm very happy for you humans to come down to the city, but what about...them?",
        )
        chatPlayer(confused, "What about who?")
        chatNpc(worried, "Them! *whispers* The G-O-D-S.")
        chatPlayer(confused, "The gods?")
        chatNpc(shocked, "Don't say it! What if they hear?")
        var asked = 0
        while (true) {
            val topics = UR_MEG_TOPICS.filterIndexed { index, _ -> index + 1 != asked }
            val option =
                if (asked == 0) {
                    choice4(topics[0], 1, topics[1], 2, topics[2], 3, "Goodbye.", 0)
                } else {
                    choice3(
                        topics[0],
                        UR_MEG_TOPICS.indexOf(topics[0]) + 1,
                        topics[1],
                        UR_MEG_TOPICS.indexOf(topics[1]) + 1,
                        "Goodbye.",
                        0,
                    )
                }
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Why are you scared of the gods?")
                    chatNpc(worried, "Because they're so powerful, of course!")
                    chatNpc(
                        worried,
                        "In the time of the great war, they forced all the goblin tribes to " +
                            "fight one another. Our ancestors weren't powerful enough to stand " +
                            "up to them.",
                    )
                    chatNpc(
                        worried,
                        "We've hidden down here for this long, but now the way to the surface " +
                            "is open, what if they find us again?",
                    )
                }
                2 -> {
                    chatPlayer(happy, "You don't need to worry!")
                    chatNpc(quiz, "I don't?")
                    chatPlayer(
                        neutral,
                        "The God Wars are over! The gods don't intervene directly in the " +
                            "affairs of mortals any more.",
                    )
                    chatNpc(quiz, "Are you sure?")
                    chatPlayer(happy, "Yes!")
                    chatNpc(
                        worried,
                        "I do hope you're right! I still can't help being worried, though.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "Don't you want to worship the god of the goblins?")
                    chatNpc(
                        angry,
                        "No! How could you even suggest that? We're not going to worship some " +
                            "supernatural being just because it's bigger and more powerful " +
                            "than us.",
                    )
                    chatNpc(
                        neutral,
                        "It may seem to you that we're cowardly, hiding here from the gods all " +
                            "this time, but I know that every Dorgeshuun would rather die than " +
                            "submit to the rule of any god.",
                    )
                }
                else -> {
                    chatPlayer(neutral, "Goodbye.")
                    chatNpc(neutral, "Goodbye.")
                    return
                }
            }
            asked = option
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ur-vass
    private suspend fun Dialogue.urVass() {
        chatNpc(neutral, "Ah. ${player.displayName}, I presume?")
        chatPlayer(happy, "That's me.")
        chatNpc(
            happy,
            "Pleased to meet you. On behalf of the Dorgeshuun Council, I should say how grateful " +
                "we are that you saved our city.",
        )
    }

    private suspend fun Dialogue.speak(
        talkedTo: String,
        speaker: String,
        mesanim: MesAnimType,
        text: String,
    ) {
        if (speaker == talkedTo) {
            chatNpc(mesanim, text)
        } else {
            chatNpcSpecific(NAMES.getValue(speaker), speaker, mesanim, text)
        }
    }

    private companion object {
        const val UR_TAG = "npc.dorgesh_urtaq"
        const val ALVIJAR = "npc.slice_dwarf_alvijar_there"
        const val UR_LUN = "npc.dorgesh_female_council_3"
        const val UR_PEL = "npc.dorgesh_female_council_4"
        const val UR_TAAL = "npc.dorgesh_female_council_1"
        const val UR_ZEK = "npc.dorgesh_male_council_1"

        val NAMES =
            mapOf(
                UR_TAG to "Ur-tag",
                ALVIJAR to "Ambassador Alvijar",
                UR_LUN to "Ur-lun",
                UR_PEL to "Ur-pel",
                UR_TAAL to "Ur-taal",
                UR_ZEK to "Ur-zek",
            )

        val UR_MEG_TOPICS =
            listOf(
                "Why are you scared of the gods?",
                "You don't need to worry!",
                "Don't you want to worship the god of the goblins?",
            )
    }
}
