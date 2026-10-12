package org.rsmod.content.areas.zeah.wintertodt.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class WintertodtCamp : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.wint_shay_guard") { startDialogue(it.npc) { captainKalt() } }
        onOpNpc1("npc.wint_wcguild_rep") { startDialogue(it.npc) { esther() } }
        onOpNpc1("npc.wint_pisc_navigator") { startDialogue(it.npc) { ish() } }
        onOpNpc1("npc.wint_master_smith") { startDialogue(it.npc) { undor() } }
        onOpNpc1("npc.wint_soldier") { startDialogue(it.npc) { winterSoldier() } }
        onOpNpc1("npc.wint_brewma") { startDialogue(it.npc) { brewma() } }
        onOpNpc1("npc.wint_cluehunter") { startDialogue(it.npc) { ed() } }
        onOpNpc1("npc.fh_rakkar") { startDialogue(it.npc) { rakkar() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Captain_Kalt
    // TODO(content): [boss:wintertodt] "Tell me how I'm doing." with subdued counts and scores,
    //  and the Check Scores op
    private suspend fun Dialogue.captainKalt() {
        chatNpc(neutral, "Hello there.")
        while (true) {
            val option =
                choice4(
                    "Tell me about the Wintertodt.",
                    1,
                    "Tell me about yourself.",
                    2,
                    "Tell me how I'm doing.",
                    3,
                    "See you later.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Tell me about the Wintertodt.")
                    chatNpc(neutral, "Ignisia claims it's some kind of evil spirit.")
                    chatPlayer(quiz, "You don't believe her?")
                    chatNpc(
                        laugh,
                        "Nah, I believe in things I can stab and kill. This is all just a storm " +
                            "in a teacup. I mean, how can wind and snow be evil? My toad is more " +
                            "of a threat!",
                    )
                    chatPlayer(confused, "Your... toad?")
                    chatNpc(
                        happy,
                        "Yeah, just a bit of fun. I have a pet - he's around here somewhere, " +
                            "assuming that troublesome cat hasn't eaten him.",
                    )
                    chatPlayer(confused, "That's a bit weird, but okay.")
                    chatNpc(neutral, "No weirder than being scared of snow.")
                }
                2 -> {
                    chatPlayer(quiz, "Tell me about yourself.")
                    chatNpc(happy, "I'm Captain Kalt, proud member of the Shayzien Army.")
                    chatPlayer(quiz, "How long have you been a soldier?")
                    chatNpc(
                        neutral,
                        "Man and boy I've worn the uniform. Earned Captain a few years back and " +
                            "never looked back. Not too happy about babysitting delusional " +
                            "wizards though. But orders is orders.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "Tell me how I'm doing.")
                    chatNpc(
                        neutral,
                        "Go play in the snow and come back when you've done something.",
                    )
                    return
                }
                else -> {
                    chatPlayer(neutral, "See you later.")
                    chatNpc(neutral, "Goodbye.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Esther
    private suspend fun Dialogue.esther() {
        chatNpc(quiz, "Are you here to fight the cold?")
        chatPlayer(confused, "Umm... I guess?")
        chatNpc(happy, "Great! Ask me anything you like about the Bruma tree, I'm an expert.")
        while (true) {
            val option =
                choice4(
                    "Tell me about the Wintertodt.",
                    1,
                    "Tell me about the Bruma tree.",
                    2,
                    "Tell me about yourself.",
                    3,
                    "See you later.",
                    4,
                )
            when (option) {
                1 -> estherWintertodt()
                2 -> brumaTree()
                3 -> {
                    chatPlayer(quiz, "Tell me about yourself.")
                    chatNpc(
                        neutral,
                        "Me? I'm a member of the Woodcutting Guild. More of a researcher " +
                            "though... I'm currently tasked with growing a new Bruma tree.",
                    )
                    chatPlayer(quiz, "How's that going?")
                    chatNpc(
                        sad,
                        "Poorly. I believe the Bruma tree can only be grown in a freezing cold " +
                            "climate.",
                    )
                    chatPlayer(confused, "Well, isn't that here?")
                    chatNpc(
                        neutral,
                        "It's just an idea really. For all I know, it could be magic way beyond " +
                            "me, beyond even the Pyromancers.",
                    )
                }
                else -> {
                    chatPlayer(neutral, "See you later.")
                    chatNpc(happy, "Goodbye!")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.estherWintertodt() {
        chatPlayer(quiz, "Tell me about the Wintertodt.")
        chatNpc(
            neutral,
            "Some would say that ice preserves while fire destroys, but no amount of fire is " +
                "destroying the Wintertodt. That's a cold that will freeze the air in your lungs.",
        )
        chatNpc(
            neutral,
            "Even the heat generated from burning the Bruma roots only weakens it, and that " +
                "doesn't last very long nowadays.",
        )
        chatPlayer(neutral, "Looks like an angry snow storm to me...")
        chatNpc(
            neutral,
            "What you see in that pit is not the Wintertodt at full strength. It never will be " +
                "while the Bruma tree still burns and the Doors of Dinh remain closed.",
        )
    }

    private suspend fun Dialogue.brumaTree() {
        chatPlayer(quiz, "Tell me about the Bruma tree.")
        chatNpc(
            neutral,
            "The Bruma tree is what keeps the Wintertodt weak. The prison was built here by Dinh " +
                "because of its usefulness against the Wintertodt.",
        )
        chatNpc(
            neutral,
            "Some legends say that Xeric is the cause for the Doors weakening, or maybe he made " +
                "the Wintertodt stronger?",
        )
        chatPlayer(quiz, "Is Xeric really such a bad guy?")
        chatNpc(
            neutral,
            "It's a known fact that he was interested in the Bruma tree. For what purpose... " +
                "there are only rumours.",
        )
        chatNpc(
            happy,
            "Regardless, the Bruma tree definitely contains powerful magic. I've been studying " +
                "the sap for years and it has truly astounding properties.",
        )
        chatNpc(happy, "There's not many fires that burn green now, are there?")
        chatPlayer(quiz, "What about gnomish fire lighters?")
        chatNpc(silent, "...")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ish_the_Navigator
    private suspend fun Dialogue.ish() {
        chatNpc(worried, "Be on your guard friend.")
        while (true) {
            val option =
                choice4(
                    "Tell me about the Wintertodt.",
                    1,
                    "Tell me about yourself.",
                    2,
                    "What do you think of the Pyromancers?",
                    3,
                    "I have to go.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Tell me about the Wintertodt.")
                    chatNpc(
                        neutral,
                        "The Wintertodt terrorised Great Kourend many centuries ago. It " +
                            "destroyed much of this northern land over many years before it was " +
                            "finally driven back into this prison.",
                    )
                    chatPlayer(quiz, "Why did it attack Kourend?")
                    chatNpc(neutral, "No one knows for sure but I have a theory...")
                    chatPlayer(quiz, "Which is?")
                    chatNpc(
                        shifty,
                        "The Dark Altar. It felt the altar's power and wanted to consume it for " +
                            "itself. It wouldn't be the first time...",
                    )
                    chatPlayer(neutral, "Interesting...")
                }
                2 -> {
                    chatPlayer(quiz, "Tell me about yourself.")
                    chatNpc(neutral, "I'm Ish, one of the navigators of Piscarilius.")
                    chatPlayer(quiz, "What brings a navigator up here?")
                    chatNpc(
                        shifty,
                        "Lady Piscarilius has sent me here to keep an eye on the situation. She " +
                            "doesn't trust the other houses and believes something sinister is " +
                            "going on.",
                    )
                    chatPlayer(quiz, "Like what?")
                    chatNpc(neutral, "I don't know, but I intend to find out.")
                }
                3 -> {
                    chatPlayer(quiz, "What do you think of the Pyromancers?")
                    chatNpc(
                        worried,
                        "I've heard stories about them and the things they have done over the " +
                            "years. Be careful, they can't be trusted.",
                    )
                    chatPlayer(neutral, "You seem a bit paranoid.")
                    chatNpc(
                        angry,
                        "Better to be paranoid than a fool. Anyone with any sense knows that the " +
                            "Pyromancers are trouble. They say even Lord Arceuus doesn't trust " +
                            "them.",
                    )
                }
                else -> {
                    chatPlayer(neutral, "I have to go.")
                    chatNpc(neutral, "Stay vigilant friend.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Undor
    // TODO(content): [quest:the_forsaken_tower] his quest dialogue
    private suspend fun Dialogue.undor() {
        val title = if (access.isBodyTypeA()) "laddie" else "lass"
        chatNpc(happy, "Hello there $title.")
        while (true) {
            val option =
                choice4(
                    "Tell me about the Wintertodt.",
                    1,
                    "Tell me about the Doors of Dinh.",
                    2,
                    "Tell me about yourself.",
                    3,
                    "Goodbye.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Tell me about the Wintertodt.")
                    chatNpc(neutral, "They say it's some kind of powerful fire spirit.")
                    chatPlayer(confused, "Don't you mean ice?")
                    chatNpc(neutral, "Aye that as well.")
                    chatNpc(
                        neutral,
                        "What it is doesn't bother me, just as long as we keep it behind the " +
                            "Doors.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Tell me about the Doors of Dinh.")
                    chatNpc(
                        neutral,
                        "The Doors were built by Dinh to contain the Wintertodt. This was over a " +
                            "thousand years ago and they still stand today.",
                    )
                    chatPlayer(quiz, "Who was Dinh?")
                    chatNpc(
                        neutral,
                        "Dinh was the greatest smith that the world has ever seen. I am the " +
                            "finest smith around today and even I cannot match his abilities.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "Tell me about yourself.")
                    chatNpc(
                        happy,
                        "The name's Undor. You'll struggle to find a dwarf in this land who " +
                            "knows more about metalwork than me.",
                    )
                    chatNpc(neutral, "One day I hope to be as good as the legendary Dinh himself.")
                    chatPlayer(quiz, "What are you doing here?")
                    if (QuestRequirements.hasCompleted(player, FORSAKEN_TOWER)) {
                        chatNpc(
                            neutral,
                            "I'm just here to keep an eye on things. Even though the Doors are " +
                                "fixed now, it will take many years for things to get back to " +
                                "normal.",
                        )
                    } else {
                        chatNpc(
                            neutral,
                            "They say the doors are failing and the Wintertodt is trying to " +
                                "escape. I'm here to prove to them that the Doors are fine.",
                        )
                        chatPlayer(quiz, "And are they?")
                        chatNpc(angry, "Of course they are! They were made by Dinh himself.")
                    }
                }
                else -> {
                    chatPlayer(neutral, "Goodbye.")
                    chatNpc(happy, "Until next time $title.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Winter_Soldier
    private suspend fun Dialogue.winterSoldier() {
        when (access.random.of(4)) {
            0 -> chatNpc(sad, "This could be worse I guess. Oh wait... No it can't.")
            1 -> chatNpc(sad, "I'm hungry...")
            2 ->
                chatNpc(
                    bored,
                    "Sometimes I wish this thing would just escape. At least we'd have something " +
                        "to do then.",
                )
            else -> chatNpc(sad, "It's so cold...")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Brew'ma
    // TODO(content): [boss:wintertodt] mixing Rejuvenation potions when items are used on her
    private suspend fun Dialogue.brewma() {
        chatPlayer(quiz, "Who are you?")
        chatNpc(
            neutral,
            "My name is Brew'ma. I'm here to assist adventurers like you with potion making.",
        )
        chatNpc(
            neutral,
            "If you hand me a herb from the Bruma tree and one of the unfinished potions from " +
                "the crate, I can combine the two if you lack the... talent to do so yourself.",
        )
        chatPlayer(happy, "Thanks!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ed (Unmarked Grave)
    private suspend fun Dialogue.ed() {
        if (RAEDWALD_HELM in player.worn) {
            chatNpc(shocked, "That helmet! Where did you get it?")
            chatPlayer(neutral, "I found it.")
            chatNpc(quiz, "Where?")
            chatPlayer(
                laugh,
                "I can't tell you that. It would ruin the fun. You'll have to work it out for " +
                    "yourself.",
            )
            chatNpc(angry, "Arhhh!")
            return
        }
        chatPlayer(happy, "Hello.")
        chatNpc(neutral, "Hello friend. What are you doing here?")
        while (true) {
            val option =
                choice4(
                    "What are you doing here?",
                    1,
                    "That's a nice outfit.",
                    2,
                    "Hey your hair is on fire!",
                    3,
                    "Goodbye.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What are you doing here?")
                    chatNpc(neutral, "I'm looking for a helmet. I know it's buried somewhere.")
                    chatPlayer(quiz, "And you think that somewhere is here?")
                    chatNpc(
                        sad,
                        "I don't know. I've tried everywhere else so it might be.",
                    )
                    chatPlayer(neutral, "Maybe you're missing something important.")
                    chatNpc(sad, "Maybe you're right. I just don't know anymore.")
                }
                2 -> {
                    chatPlayer(happy, "That's a nice outfit.")
                    chatNpc(sad, "Thank you. I just wish I had the helmet to go with it.")
                }
                3 -> {
                    chatPlayer(shocked, "Hey your hair is on fire!")
                    chatNpc(angry, "Get out of it!")
                }
                else -> {
                    chatPlayer(neutral, "Goodbye.")
                    chatNpc(neutral, "See you later.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Rakkar
    private suspend fun Dialogue.rakkar() {
        if (MYRE_SNELM in player.worn) {
            chatNpc(
                neutral,
                "Beyond the reef, within the deep, the Kayzertief remains asleep, I know, I " +
                    "know, oh oh oh.",
            )
            chatPlayer(confused, "The what?")
            chatNpc(
                neutral,
                "They whisper of a dream to climb across the moon supreme, I know, I know, oh oh " +
                    "oh",
            )
            chatPlayer(confused, "...I don't understand you.")
            chatNpc(
                neutral,
                "To visit their watery hall, answer their call, embrace the fall and save us all!",
            )
            chatPlayer(worried, "... I'll be going now.")
            return
        }
        chatNpc(neutral, "Blub bop, blub bop")
        chatPlayer(worried, "Are you ok?")
        chatNpc(neutral, "Splish splosh, splish splosh")
        chatPlayer(confused, "...I don't understand you.")
        chatNpc(neutral, "Wubba lubba, dub dub")
        chatPlayer(confused, "...")
    }

    private companion object {
        const val RAEDWALD_HELM = "obj.raedwald_helm"
        const val FORSAKEN_TOWER = "quest_forsakentower"
        const val MYRE_SNELM = "obj.snelm_round_swamp"
    }
}
