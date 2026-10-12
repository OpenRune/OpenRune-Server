package org.rsmod.content.areas.zeah.arceuus.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.areas.zeah.kourend.kourendRumour
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class ArceuusTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.arceuus_wizard_visitor") { startDialogue(it.npc) { wizard() } }
        onOpNpc1("npc.arceuus_towerguardian") { startDialogue(it.npc) { towerGuardian() } }
        for (mage in TOWER_MAGES) {
            onOpNpc1(mage) { startDialogue(it.npc) { towerMage() } }
        }
        onOpNpc1("npc.shayzien_northern_guardian") { startDialogue(it.npc) { lookOut() } }
        onOpNpc1("npc.arceuus_bloodguardian") { startDialogue(it.npc) { okina() } }
        onOpNpc1("npc.arceuus_soulguardian") { startDialogue(it.npc) { aretha() } }
        onOpNpc1("npc.arceuus_essencebuyer") { startDialogue(it.npc) { clerris() } }
        onOpNpc1("npc.arceuus_cleric") { startDialogue(it.npc) { mofina() } }
        onOpNpc1("npc.arceuus_fairyring_curator") { startDialogue(it.npc) { trossa() } }
        onOpNpc1("npc.arceuus_petbat_owner") { startDialogue(it.npc) { freald() } }
        onOpNpc1("npc.arceuus_researcher") { startDialogue(it.npc) { ouditor() } }
        onOpNpc1("npc.arceuus_bartender") { startDialogue(it.npc) { rassain() } }
        onOpNpc1("npc.trobin_arceuus") { startDialogue(it.npc) { lordTrobin() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Wizard_(Arceuus)
    private suspend fun Dialogue.wizard() {
        when (access.random.of(3)) {
            0 -> {
                chatPlayer(quiz, "What are you doing here?")
                chatNpc(
                    neutral,
                    "I've heard there is a powerful magic presence in these lands, I'm gathering " +
                        "notes to take back with me to the Wizards' Tower.",
                )
            }
            1 -> {
                chatPlayer(quiz, "What are you doing?")
                chatNpc(
                    neutral,
                    "When we heard of a magical presence in the Western lands, the Wizards' " +
                        "Tower had no choice but to send out a research party.",
                )
                chatPlayer(quiz, "Learned anything interesting?")
                chatNpc(
                    neutral,
                    "Some of these texts describe a 'Dark Altar', one can only imagine what " +
                        "power it possesses.",
                )
            }
            else -> {
                chatPlayer(quiz, "Learned anything interesting?")
                chatNpc(
                    happy,
                    "The beings here seem to have transcended beyond the corporeal realm, truly " +
                        "fascinating!",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Tower_Mage
    private suspend fun Dialogue.towerGuardian() {
        if (QuestRequirements.hasCompleted(player, ASCENT_OF_ARCEUUS)) {
            chatNpc(
                neutral,
                "You have been granted permission to enter the Tower of Magic. Move along.",
            )
            return
        }
        chatNpc(
            neutral,
            "The Tower of Magic is off limits. Lord Arceuus does not wish his experiments to be " +
                "disturbed.",
        )
    }

    private suspend fun Dialogue.towerMage() {
        when (access.random.of(4)) {
            0 -> chatNpc(neutral, "I can't talk I'm afraid. I need to time this experiment.")
            1 -> chatNpc(worried, "Please be careful near the apparatus.")
            2 -> chatNpc(confused, "Hmmm... Maybe eye of newt will work...")
            else -> chatNpc(happy, "It's going to be interesting to see the results of this test.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Look-out
    private suspend fun Dialogue.lookOut() {
        chatNpc(angry, "What do you want?")
        var option = lookOutMenu()
        while (true) {
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What are you guarding?")
                    chatNpc(
                        neutral,
                        "This path leads to the Doors of Dinh. You should be careful if you wish " +
                            "to travel any further.",
                    )
                    val doors =
                        choice2(
                            "Tell me about the Doors of Dinh.",
                            true,
                            "Okay, thanks for the warning.",
                            false,
                        )
                    if (doors) {
                        chatPlayer(quiz, "Tell me about the Doors of Dinh.")
                        chatNpc(
                            neutral,
                            "They're to keep the Wintertodt at bay. No-one knows what it really " +
                                "is, but centuries ago it came out of the northern wastes to " +
                                "terrorise the kingdom.",
                        )
                        chatNpc(
                            neutral,
                            "The Doors were created by Dinh, a master smither, to imprison the " +
                                "Wintertodt.",
                        )
                        chatNpc(
                            neutral,
                            "Now we keep a watch on the pass, so that the armies can be summoned " +
                                "if the Wintertodt ever escapes.",
                        )
                        chatPlayer(neutral, "Okay, thanks for the warning.")
                        return
                    }
                    chatPlayer(neutral, "Okay, thanks for the warning.")
                    option = lookOutMenu()
                }
                2 -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(
                        sad,
                        "I'm the duty look-out on the northern pass. It's the worst assignment " +
                            "there is.",
                    )
                    chatNpc(
                        angry,
                        "As if the cold weren't bad enough, the snooty Arceuus magi always look " +
                            "down their noses at the Shayzien armies, just because we use muscle " +
                            "and steel rather than fancy magicks.",
                    )
                    option = choice2("What are you guarding?", 1, "I'll leave you alone now.", 3)
                }
                else -> {
                    chatPlayer(neutral, "I'll leave you alone now.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.lookOutMenu(): Int =
        choice3("What are you guarding?", 1, "Who are you?", 2, "I'll leave you alone now.", 3)

    // https://oldschool.runescape.wiki/w/Transcript:Okina
    private suspend fun Dialogue.okina() {
        chatNpc(neutral, "So much blood flows through these lands.")
        while (true) {
            val option =
                choice3(
                    "Where does the blood come from?",
                    1,
                    "Why are you here?",
                    2,
                    "I'll leave you to enjoy the view.",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Where does the blood come from?")
                    chatNpc(
                        neutral,
                        "Throughout the generations, countless people of this land have perished " +
                            "in times of strife, with their bodies left to rot where they fell.",
                    )
                    chatNpc(
                        neutral,
                        "Some were slain by the deadly creatures that roam the land; others " +
                            "perished at the hands of their fellow citizens during the wars of " +
                            "this land.",
                    )
                    chatNpc(
                        neutral,
                        "Their blood flowed into the soil, pooling and percolating unseen in the " +
                            "dark, as the power of the Altar drew it to this sacred place to " +
                            "form the River.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Why are you here?")
                    chatNpc(
                        neutral,
                        "The blood is the blood of Kourend's citizens. It is fitting that we " +
                            "honour their memory.",
                    )
                    chatNpc(happy, "Also, it smells so good.")
                }
                else -> {
                    chatPlayer(neutral, "I'll leave you to enjoy the view.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Aretha
    // TODO(content): [miniquest:bear_your_soul] revealing the soul bearer's grave, and "About the
    //  soul bearer again..." once it has been found
    // TODO(content): [system:treasure_trails] medium challenge scroll
    private suspend fun Dialogue.aretha() {
        chatNpc(neutral, "Respect the souls, traveller.")
        while (true) {
            val option =
                choice4(
                    "Why are all these souls here?",
                    1,
                    "Why are you here?",
                    2,
                    "I've been reading your book...",
                    3,
                    "Okay, I'm respecting the souls.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Why are all these souls here?")
                    chatNpc(
                        neutral,
                        "A corporeal body is a mere flesh-temple in which the soul dwells. As " +
                            "the body fails, the soul loses its home. Without guidance, it would " +
                            "drift into oblivion.",
                    )
                    chatNpc(
                        neutral,
                        "However, the Soul Altar provides a beacon, drawing the souls to this " +
                            "place where they may join the River.",
                    )
                    chatNpc(
                        neutral,
                        "The River leads the souls through unchartable realms, transcending the " +
                            "plane of the world as you see it, but the three-headed Guardian " +
                            "keeps them safe.",
                    )
                    chatNpc(
                        neutral,
                        "The souls are the dead of Kourend - ensure you respect the souls.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Why are you here?")
                    chatNpc(
                        neutral,
                        "I am here to salute the dead of Kourend whose souls are joining the " +
                            "River. Ensure you respect the souls.",
                    )
                }
                3 -> {
                    soulBearer()
                    return
                }
                else -> {
                    chatPlayer(neutral, "Okay, I'm respecting the souls.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.soulBearer() {
        chatPlayer(quiz, "I've been reading your book: 'The Journey of Souls'")
        chatNpc(
            neutral,
            "Let me guess, traveller... did you come to ask me where the soul bearer was buried? " +
                "Every mortal who reads my book seems to want to get their hands on it.",
        )
        val yes =
            choice2("Yes please.", true, "No, I don't want your smelly soul bearer.", false)
        if (!yes) {
            chatPlayer(angry, "No, I don't want your smelly soul bearer.")
            chatNpc(
                angry,
                "Hmmph! So what do you want? I trust you will respect the souls while you are in " +
                    "this hallowed place.",
            )
            return
        }
        chatNpc(
            neutral,
            "Mortals are so predictable. This is the kind of thinking that led that adventurer " +
                "to break into the Guardian's cave and damage the soul bearer in the first place.",
        )
        chatNpc(
            neutral,
            "It occurs to me, though, that even if you dig it up, the artefact is damaged. " +
                "Although the Elders of Arceuus never had the knowledge to repair it, there is " +
                "one who may be able to help.",
        )
        chatNpc(
            neutral,
            "I speak of the Key Master, he who watches over the cave of the three-headed " +
                "Guardian of the River. He has been there for countless years, and will have " +
                "absorbed power from the River in that time.",
        )
        chatNpc(
            neutral,
            "Now, we buried the artefact in the crypt of the Arceuus church. As I have revealed " +
                "this secret to you, you will no longer be affected by the spells that prevent " +
                "unwary explorers from stumbling across it.",
        )
        chatPlayer(happy, "Thanks! I'll be off now.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Clerris
    // TODO(content): [system:treasure_trails] medium challenge scroll
    private suspend fun Dialogue.clerris() {
        chatNpc(neutral, "Good day.")
        chatPlayer(quiz, "Hey there. What brings you here?")
        chatNpc(
            neutral,
            "This is the dense essence mine. The essence we gather here is used across Arceuus, " +
                "and indeed all of Kourend. If you wish, you are welcome to mine some yourself.",
        )
        chatPlayer(happy, "I might do that. Thanks.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Mofina
    private suspend fun Dialogue.mofina() {
        chatNpc(neutral, "Do you seek guidance, traveller?")
        while (true) {
            val option =
                choice3(
                    "Sure, gimme your best advice.",
                    1,
                    "What god do you worship?",
                    2,
                    "No, I'm fine.",
                    3,
                )
            when (option) {
                1 -> guidance()
                2 -> {
                    chatPlayer(quiz, "What god do you worship?")
                    chatNpc(
                        neutral,
                        "You sound like the Saradominist peasants from Hosidius, always assuming " +
                            "they must serve one god above all others.",
                    )
                    chatNpc(
                        neutral,
                        "While the gods are useful for tedious matters such as granting prayers " +
                            "during combat, we of Arceuus have long moved beyond the need to pay " +
                            "them homage.",
                    )
                    chatNpc(
                        neutral,
                        "This is a place of meditation, where a novice may contemplate the " +
                            "mysteries of the Dark Altar - without venturing too close to its " +
                            "power.",
                    )
                    chatNpc(
                        neutral,
                        "Saradomin is popular with many uneducated citizens of Kourend, but you " +
                            "may use our church to worship any god you prefer. That would be an " +
                            "ecumenical matter.",
                    )
                    chatNpc(quiz, "Now, do you require anything of me?")
                }
                else -> {
                    chatPlayer(neutral, "No, I'm fine.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.guidance() {
        chatPlayer(happy, "Sure, gimme your best advice.")
        chatNpc(bored, "*sigh*")
        chatNpc(
            neutral,
            "You flesh-temples always assume you can receive guidance merely by asking someone " +
                "for it. But in this land there is a power that can teach us far more than any " +
                "mere teacher could.",
        )
        chatNpc(
            neutral,
            "I speak of the Dark Altar. For generations our wisest scholars have studied it, yet " +
                "still it has capacities beyond our comprehension.",
        )
        chatNpc(
            neutral,
            "The Dark Altar can distort the dimensions, warping the fabric of reality as you " +
                "perceive it, and even bridging the gap between life and death.",
        )
        chatNpc(
            neutral,
            "But you are still a flesh-temple; you require a corporeal body to house your soul, " +
                "and you perform disgusting bodily functions to maintain it.",
        )
        chatNpc(
            neutral,
            "We of Arceuus have learnt, through the power of the Dark Altar, to move beyond the " +
                "need for such a flesh-temple to house our souls.",
        )
        chatNpc(
            neutral,
            "In the crypt below us, we freed our souls from the flesh-temples that had " +
                "imprisoned them, and we took the form you see now.",
        )
        chatNpc(
            neutral,
            "Perhaps, if you allow the Dark Altar to work on your mind, one day you too will be " +
                "able to ascend to our higher form.",
        )
        val thanks =
            choice2(
                "Thank you for explaining that.",
                true,
                "No thanks, I like having a body. It's fun.",
                false,
            )
        if (thanks) {
            chatPlayer(happy, "Thank you for explaining that.")
            chatNpc(
                neutral,
                "I fear you are far from comprehending it. Now, is there anything I can do for " +
                    "you?",
            )
        } else {
            chatPlayer(laugh, "No thanks, I like having a body. It's fun.")
            chatNpc(
                angry,
                "How disgusting - a flesh-temple that worships itself. Now, do you want anything?",
            )
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Trossa
    private suspend fun Dialogue.trossa() {
        chatNpc(neutral, "Can I help you, traveller?")
        if (!choice2("Why do you stand here?", true, "I'm fine, thanks.", false)) {
            chatPlayer(neutral, "I'm fine, thanks.")
            return
        }
        chatPlayer(quiz, "Why do you stand here?")
        chatNpc(
            neutral,
            "This is the northernmost reach of Kourend. Beyond these cliffs you will find the " +
                "Doors of Dinh and the prison of the Wintertodt.",
        )
        chatNpc(
            worried,
            "The Pyromancers do what they can to keep the Wintertodt contained but I fear it " +
                "will not be enough.",
        )
        chatNpc(
            neutral,
            "I come here to keep watch, whenever my time permits, so that the city may be warned " +
                "if the Wintertodt escapes.",
        )
        chatPlayer(neutral, "Interesting... Thanks.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Freald
    // TODO(content): [system:npc_overhead] Batt Mellamy's "Squeak!" overhead
    private suspend fun Dialogue.freald() {
        chatNpc(neutral, "Greetings traveller. What brings you to my residence?")
        chatPlayer(confused, "You keep a pet bat? That's a bit odd.")
        chatNpc(
            neutral,
            "Yes traveller, this is not out of the ordinary for us. The bats respond well to the " +
                "powers of the dark altar after being drawn here by the river of blood.",
        )
        chatPlayer(laugh, "Well I hope he keeps you 'mused.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ouditor
    private suspend fun Dialogue.ouditor() {
        chatNpc(
            neutral,
            "Please feel free to look around, just try to keep your hands off the apparatus!",
        )
        chatPlayer(quiz, "What are you doing?")
        chatNpc(
            angry,
            "I'm trying to carry out very important research, but these so-called 'wizards' from " +
                "the eastern lands keep meddling with my equipment.",
        )
        chatPlayer(neutral, "I'll let you get back to your work.")
        chatNpc(neutral, "Farewell, traveller.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Rassain
    private suspend fun Dialogue.rassain() {
        chatNpc(neutral, "What can I do for you, traveller?")
        while (true) {
            val option =
                choice4(
                    "Can I have a beer, please?",
                    1,
                    "What's with the furniture in this place?",
                    2,
                    "Have you heard any rumours here?",
                    3,
                    "Nothing.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Can I have a beer, please?")
                    chatNpc(neutral, "I am afraid you cannot, traveller. I serve only spirits.")
                    chatNpc(quiz, "Now, can I offer you anything else?")
                }
                2 -> {
                    chatPlayer(quiz, "What's with the furniture in this place?")
                    chatNpc(
                        neutral,
                        "Perhaps you refer to the lack of barstools and chairs. My customers " +
                            "have moved well beyond such trivial needs. However there are beds " +
                            "upstairs for the benefit of fleshy travellers.",
                    )
                    chatNpc(quiz, "Now, can I offer you anything?")
                }
                3 -> {
                    chatPlayer(quiz, "Have you heard any rumours here?")
                    kourendRumour()
                    return
                }
                else -> {
                    chatPlayer(neutral, "Nothing.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lord_Trobin_Arceuus
    // TODO(content): [quest:the_ascent_of_arceuus] he is only reachable in the Tower of Magic
    //  after the quest
    // TODO(content): [quest:a_kingdom_divided] "Thank you for helping our people. Please accept
    //  these gifts" for players who lost the memoir page
    private suspend fun Dialogue.lordTrobin() {
        chatNpc(neutral, "Good day friend.")
        while (true) {
            val option =
                choice5(
                    "Tell me about yourself.",
                    1,
                    "Tell me about Arceuus.",
                    2,
                    "Tell me about the Ascent of Arceuus.",
                    3,
                    "Tell me about your plans.",
                    4,
                    "Goodbye.",
                    5,
                )
            when (option) {
                1 -> trobinHimself()
                2 -> trobinArceuus()
                3 -> trobinAscent()
                4 -> trobinPlans()
                else -> {
                    chatPlayer(neutral, "Goodbye.")
                    chatNpc(neutral, "Farewell.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.trobinPlans() {
        chatPlayer(quiz, "Tell me about your plans.")
        if (QuestRequirements.hasCompleted(player, KINGDOM_DIVIDED)) {
            chatNpc(
                neutral,
                "Now that we know Xamphur was responsible for the device that was absorbing " +
                    "power from the Dark Altar, we're hoping we can use it to help locate him.",
            )
            chatNpc(
                sad,
                "Alas, so far we've had no luck. We're also scanning for energy that matches " +
                    "what we observed during that earthquake he caused, but again, nothing has " +
                    "been discovered so far.",
            )
            chatNpc(
                neutral,
                "It seems this work will take time. I assure you though, we will find him and " +
                    "his master.",
            )
            chatPlayer(quiz, "What about that Pyromancer?")
            chatNpc(
                neutral,
                "We've begun an investigation into their order, but we have little to go on. " +
                    "Hopefully we find a good lead soon.",
            )
            chatPlayer(neutral, "Well just let me know if you need my help.")
            chatNpc(happy, "I will, thank you.")
            return
        }
        chatNpc(
            neutral,
            "I had one of my people recover the remains of that device. The one that was " +
                "absorbing the Dark Altars power.",
        )
        chatNpc(
            neutral,
            "We're performing tests to see if we can learn more about it. We haven't made any " +
                "meaningful progress yet though.",
        )
        chatNpc(
            neutral,
            "Once we've learnt more about it, we'll be in a better position to determine our " +
                "next move.",
        )
        chatNpc(
            worried,
            "I admit, the idea of someone sabotaging the Altar is more than a bit concerning. I " +
                "hope we can make some progress soon.",
        )
    }

    private suspend fun Dialogue.trobinHimself() {
        chatPlayer(quiz, "Tell me about yourself.")
        chatNpc(
            neutral,
            "I am Trobin Arceuus, lord of Arceuus. It's a position I've held for a thousand " +
                "years, ever since the Ascent of Arceuus.",
        )
        chatPlayer(quiz, "That's a long time. I guess you've seen quite a bit.")
        chatNpc(
            neutral,
            "Indeed. I've been present at some of the most notable moments in Kourend's history.",
        )
        chatNpc(
            neutral,
            "I was there when King Agata IV was deposed, I was there when Kharedst the Builder " +
                "opened Kourend Castle and I was there when Emperor Imafore was executed.",
        )
        chatNpc(
            neutral,
            "I tend to keep out of public affairs these days though. My presence is often more " +
                "of a distraction than a benefit. It also means I can focus on my work in peace.",
        )
        chatPlayer(quiz, "Your work?")
        chatNpc(
            neutral,
            "The freeing of souls. There are many souls that find themselves trapped here after " +
                "death. My people and I do our best to release them.",
        )
    }

    private suspend fun Dialogue.trobinArceuus() {
        chatPlayer(quiz, "Tell me about Arceuus.")
        chatNpc(
            neutral,
            "If you find something magical in Kourend, chances are it came from Arceuus. Our " +
                "people have lived for a thousand years. The knowledge we've gained in that time " +
                "has resulted in many a magical discovery.",
        )
        chatNpc(
            neutral,
            "Most of our discoveries start with the Dark Altar. It is the source of our " +
                "immortality and our people dedicate their lives to studying it.",
        )
        chatNpc(
            neutral,
            "Our people also serves an important role in the wider world. We work to free " +
                "trapped souls so that they may pass on to the next world. It is a crucial task " +
                "that the Dark Altar helps us to perform.",
        )
        chatPlayer(quiz, "Was the Dark Altar always here?")
        chatNpc(
            neutral,
            "Impossible to say. The Dark Altar has been mentioned in records dating back to the " +
                "earliest days of Kourend. Before then, who knows.",
        )
        chatPlayer(quiz, "You weren't around back then?")
        chatNpc(
            neutral,
            "No. The Ascent of Arceuus didn't occur until after Xeric's Age of Strife, hundreds " +
                "of years after Kourend was formed.",
        )
    }

    private suspend fun Dialogue.trobinAscent() {
        chatPlayer(quiz, "Tell me about the Ascent of Arceuus.")
        chatNpc(
            neutral,
            "It was a thousand years ago, during the time of King Byrne II. I went to my people " +
                "and shared the secrets of immortality with them.",
        )
        chatNpc(
            neutral,
            "Not all of them accepted this gift of course, as was their right. Those that did, " +
                "were granted new bodies to house their souls. These bodies are sustained by the " +
                "Dark Altar.",
        )
        chatPlayer(quiz, "It wasn't you that discovered this secret though was it?")
        chatNpc(
            neutral,
            "No. It was granted to us by the Tasakaal. They are ancient beings, ones that make " +
                "us seem like mere children.",
        )
        chatNpc(
            neutral,
            "Their age and experience have granted them knowledge over life and death that we " +
                "can only dream of.",
        )
        chatPlayer(
            neutral,
            "The Tasakaal don't like you very much. They called you a liar and claimed you " +
                "misused the knowledge.",
        )
        chatNpc(
            neutral,
            "The events that led to the Ascent of Arceuus are more complicated than even the " +
                "Tasakaal know. But that is a story for another day.",
        )
    }

    private companion object {
        const val ASCENT_OF_ARCEUUS = "quest_ascentofarceuus"
        const val KINGDOM_DIVIDED = "quest_kingdomdivided"
        val TOWER_MAGES =
            listOf(
                "npc.arcquest_towermage1",
                "npc.arcquest_towermage2",
                "npc.arcquest_towermage3",
                "npc.arcquest_towermage4",
                "npc.arcquest_towermage5",
            )
    }
}
