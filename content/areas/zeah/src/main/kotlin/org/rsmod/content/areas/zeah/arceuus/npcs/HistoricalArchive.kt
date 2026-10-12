package org.rsmod.content.areas.zeah.arceuus.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class HistoricalArchive : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.raidquest_library_horologist") { startDialogue(it.npc) { imerominia() } }
        onOpNpc1("npc.raidquest_library_scholar") { startDialogue(it.npc) { logios() } }
        onOpNpc1("npc.raidquest_library_traphunter") { startDialogue(it.npc) { pagida() } }
        onOpNpc1("npc.raidquest_library_xeric_expert") { startDialogue(it.npc) { ektheme() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Imerominia
    // TODO(content): [quest:dragon_slayer_ii] "Have you ever heard of Camorra?"
    private suspend fun Dialogue.imerominia() {
        chatPlayer(quiz, "Hello. What do you do here?")
        chatNpc(happy, "Hello. My name is Imerominia and I'm the library horologist.")
        if (!choice2("Horologist? What does that mean?", true, "Goodbye.", false)) {
            chatPlayer(neutral, "Goodbye.")
            return
        }
        chatPlayer(quiz, "Horologist? What does that mean?")
        chatNpc(
            neutral,
            "Horology is the study and measurement of time. Specifically, I study how time and " +
                "dates have been historically recorded.",
        )
        chatNpc(
            neutral,
            "For example, the date convention in Great Kourend is to state the number of days " +
                "and decades since the signing of the Royal Accord of Twill.",
        )
        chatPlayer(confused, "Days and decades? That's a bit confusing.")
        chatNpc(
            neutral,
            "Yes, it's an interesting convention. Truth be told, most in the kingdom prefer to " +
                "use the same date format as you'll find used elsewhere.",
        )
        chatNpc(
            neutral,
            "However, as this is the Historical Archive, we stick to the dates used historically " +
                "within Kourend.",
        )
        while (true) {
            val option =
                choice4(
                    "Tell me more about the Royal Accord of Twill.",
                    1,
                    "So what is today's date?",
                    2,
                    "What about the time before the Accord was signed?",
                    3,
                    "Goodbye.",
                    4,
                )
            when (option) {
                1 -> {
                    royalAccord()
                    return
                }
                2 -> {
                    todaysDate()
                    return
                }
                3 -> {
                    chatPlayer(quiz, "What about the time before the Accord was signed?")
                    chatNpc(
                        neutral,
                        "Very little is known about those ancient times, but we refer to them as " +
                            "decades 'Before Accord', or 'BA' for short.",
                    )
                }
                4 -> {
                    chatPlayer(neutral, "Goodbye.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.royalAccord() {
        chatPlayer(quiz, "Tell me more about the Royal Accord of Twill.")
        chatNpc(
            neutral,
            "The Royal Accord of Twill was signed by the rulers of each of the houses 139 " +
                "decades ago, following the great famine.",
        )
        chatNpc(
            neutral,
            "The Accord declared that each house will, in turn, rule the Kingdom of Great " +
                "Kourend for one decade in rotation, based on lots drawn.",
        )
        if (QuestRequirements.hasCompleted(player, KINGDOM_DIVIDED)) {
            chatNpc(
                neutral,
                "The Royal Accord of Twill no longer plays much part in the rulership of " +
                    "Kourend. It was breached in the 13th decade when King Shayzien III seized " +
                    "control of the kingdom. It hasn't been honoured since.",
            )
            chatNpc(
                neutral,
                "That said, it was the basis for Rose's Accord, which sets out how the kingdom " +
                    "is currently ruled today. As decreed in Rose's Accord, the Hosidius family " +
                    "are now the royal family of Kourend.",
            )
            chatNpc(
                neutral,
                "We have the original Royal Accord of Twill safely stored here. Although it's " +
                    "not available for public viewing, there are copies available within the " +
                    "Grand Library. Is there anything else I can help you with?",
            )
            if (choice2("So what is today's date?", true, "No thanks.", false)) {
                todaysDate()
            } else {
                chatPlayer(neutral, "No thanks.")
            }
            return
        }
        chatNpc(
            neutral,
            "You may find a book in the Grand Library with further information. Is there " +
                "anything else I can help you with?",
        )
        val option =
            choice3(
                "Is the Royal Accord of Twill still honoured today?",
                1,
                "So what is today's date?",
                2,
                "No thanks.",
                3,
            )
        when (option) {
            1 -> accordHonoured()
            2 -> todaysDate()
            3 -> chatPlayer(neutral, "No thanks.")
        }
    }

    private suspend fun Dialogue.accordHonoured() {
        chatPlayer(quiz, "Is the Royal Accord of Twill honoured today?")
        chatNpc(
            neutral,
            "Our kingdom has advanced somewhat over the many decades since the Accord. Though " +
                "some parts of the Accord remain intact, the Kourend Council now rules Great " +
                "Kourend.",
        )
        chatPlayer(quiz, "Was the Accord ever breached?")
        chatNpc(
            neutral,
            "Indeed it was. In the 13th decade, King Shayzien III seized control of Kourend " +
                "using the military under his command.",
        )
        chatNpc(
            neutral,
            "The full set of laws within the Accord haven't been followed since. However, some " +
                "of them were reinstated in the 47th decade, by King Byrne I.",
        )
        chatPlayer(quiz, "Fascinating. What happened during that time?")
        chatNpc(
            neutral,
            "During the 20th decade, King Shayzien VI married the daughter of Lord Hosidius. " +
                "Four decades later their son was crowned king.",
        )
        chatNpc(
            neutral,
            "He took the name King Hosidius IV in an attempt to distance the family from the " +
                "breaking of the Royal Accord of Twill.",
        )
        chatNpc(
            neutral,
            "King Hosidius IV followed by his son, King Hosidius V ruled Kourend until the early " +
                "days of the 33rd decade.",
        )
        chatNpc(
            neutral,
            "It was then that King Shayzien VII took control of the kingdom. His rule was a " +
                "notably violent one.",
        )
        chatNpc(
            neutral,
            "King Shayzien VII was killed some five decades later during the Battle of the " +
                "Concourse. He was succeeded by the dark mage, Xeric.",
        )
        chatNpc(
            neutral,
            "Xeric ruled Kourend for more than ten dark decades using an army of lizardmen. This " +
                "time period was known as the Age of Strife.",
        )
        chatNpc(
            neutral,
            "Xeric was eventually killed during the 47th decade and succeeded by King Byrne I.",
        )
        val option =
            choice3(
                "Can you tell me more about Xeric?",
                1,
                "So what is today's date?",
                2,
                "Goodbye.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Can you tell me more about Xeric?")
                chatNpc(
                    neutral,
                    "Ah, I do not have much more information on him. You should speak to " +
                        "Ektheme, our Xerician expert.",
                )
                chatPlayer(happy, "Thanks.")
            }
            2 -> todaysDate()
            3 -> chatPlayer(neutral, "Goodbye.")
        }
    }

    private suspend fun Dialogue.todaysDate() {
        chatPlayer(quiz, "So what is today's date?")
        chatNpc(
            neutral,
            "I'm afraid I lose track of which day it is... but we are in the 141st decade.",
        )
        chatPlayer(quiz, "So that means the Accord was signed almost 1,410 years ago?")
        chatNpc(
            happy,
            "That is correct. You seem to have understood our time and date format well.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Logios
    private suspend fun Dialogue.logios() {
        chatPlayer(quiz, "Hello. What are you doing here?")
        chatNpc(neutral, "I'm studying the Invasion of the Wintertodt.")
        while (true) {
            val option =
                choice3(
                    "Tell me more about the Wintertodt.",
                    1,
                    "Tell me more about the invasion.",
                    2,
                    "Goodbye.",
                    3,
                )
            when (option) {
                1 -> if (!wintertodt()) return
                2 -> invasion()
                3 -> {
                    chatPlayer(neutral, "Goodbye.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.wintertodt(): Boolean {
        chatPlayer(quiz, "Tell me more about the Wintertodt.")
        chatNpc(
            neutral,
            "The Wintertodt is a fiercely cold being, capable of freezing entire areas with its " +
                "mere presence. We do not know what exactly the Wintertodt is, nor where it came " +
                "from.",
        )
        chatNpc(
            neutral,
            "When the Wintertodt first attacked, it was feared that all of Kourend would be " +
                "lost. Fortunately, the Order of the Sacred Flame and Dinh the Master Smith were " +
                "able to stop it.",
        )
        val option =
            choice3(
                "How did they stop the Wintertodt?",
                1,
                "Tell me more about the invasion.",
                2,
                "Goodbye.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "How did they stop the Wintertodt?")
                chatNpc(
                    neutral,
                    "Dinh was a great smith and engineer. He uncovered the power of the Bruma " +
                        "tree. Using bark from the tree and various metal compounds, Dinh forged " +
                        "the doors of the prison that hold the Wintertodt.",
                )
                chatNpc(
                    neutral,
                    "In addition, the Order of the Sacred Flame utilised the Bruma tree in " +
                        "firemaking. They heated the environment to subdue Wintertodt.",
                )
            }
            2 -> invasion()
            3 -> {
                chatPlayer(neutral, "Goodbye.")
                return false
            }
        }
        return true
    }

    private suspend fun Dialogue.invasion() {
        chatPlayer(quiz, "Tell me more about the invasion.")
        chatNpc(
            neutral,
            "More than 137 decades ago, seemingly from nowhere, the Wintertodt began terrorising " +
                "the lands north of Kourend, freezing everything within its path.",
        )
        chatNpc(
            neutral,
            "After five decades of this terror, the people of Kourend were able to quell the " +
                "beast, containing it in an icy prison.",
        )
        chatNpc(
            neutral,
            "The Wintertodt remained subdued until very recently, when it began to threaten " +
                "Kourend once more.",
        )
        chatNpc(
            neutral,
            "Fortunately, the Order of the Sacred Flame is able to contain the Wintertodt and " +
                "prevent another attack.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Pagida
    // TODO(content): [quest:tale_of_the_righteous] "I have a question about King Shayzien VII."
    // and the prison teleport during the quest
    private suspend fun Dialogue.pagida() {
        chatNpc(happy, "Hello there, what can I do for you?")
        chatPlayer(quiz, "I don't know, what can you do for me?")
        chatNpc(neutral, "Well, I am somewhat of an expert in the field of magical traps.")
        chatPlayer(
            confused,
            "Magical traps? What does that have to do with the Historical Archive?",
        )
        chatNpc(
            neutral,
            "A good question. The Archive is actually part of the Tower of Magic, and was " +
                "repurposed many decades ago.",
        )
        chatNpc(
            neutral,
            "Unfortunately, we don't possess full understanding of the Tower of Magic and its " +
                "many enchantments, so we often discover remnants of traps.",
        )
        chatNpc(
            neutral,
            "The task has fallen to me to investigate and study what we can of these " +
                "enchantments in the hopes of understanding their magical power.",
        )
        while (true) {
            val option =
                choice4(
                    "What exactly is the Tower of Magic?",
                    1,
                    "Tell me about the history of the Tower",
                    2,
                    "This room is part of the Tower?",
                    3,
                    "Goodbye.",
                    4,
                )
            when (option) {
                1 -> towerOfMagic()
                2 -> towerHistory()
                3 -> archiveRoom()
                4 -> {
                    chatPlayer(neutral, "Goodbye.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.towerOfMagic() {
        chatPlayer(quiz, "What exactly is the Tower of Magic?")
        chatNpc(
            neutral,
            "The Tower of Magic is a uniquely enchanted building located just west of the " +
                "library, though many levels and rooms are hidden from sight.",
        )
        chatNpc(
            neutral,
            "I have dedicated my life to studying the powerful enchantments that conceal the " +
                "many levels of the Tower, though it proves difficult at times.",
        )
        chatNpc(quiz, "Is there anything else I can help you with?")
    }

    private suspend fun Dialogue.towerHistory() {
        chatPlayer(quiz, "Tell me about the history of the Tower.")
        chatNpc(
            neutral,
            "The Tower was constructed by King Rada III. We don't know exactly when, since dates " +
                "were not recorded before the Twill Accord.",
        )
        chatNpc(
            neutral,
            "Following the rebellion of the five houses against King Rada III, the Council of " +
                "Elders was formed. They used the Tower as a meeting place for the Council.",
        )
        chatNpc(
            neutral,
            "Since then, it has mostly been used as a residence by the various rulers of the " +
                "kingdom, with each ruler using the Tower however they saw fit.",
        )
        chatNpc(
            neutral,
            "Eventually, after Kourend Castle was built, ownership of the Tower was passed over " +
                "to Lord Arceuus.",
        )
        chatNpc(quiz, "Is there anything else I can help you with?")
    }

    private suspend fun Dialogue.archiveRoom() {
        chatPlayer(quiz, "This room is part of the Tower?")
        chatNpc(
            neutral,
            "Indeed. During the invasion of the Wintertodt, it was feared that all of Kourend " +
                "might be lost to the cold. In order to preserve the history of the kingdom, " +
                "this archive was created during the fifth decade.",
        )
        chatNpc(
            neutral,
            "Unfortunately, as it is a former chamber of the Tower, and we do not fully " +
                "understand the magic that conceals it, we often discover strange enchantments " +
                "that we believe to be old traps.",
        )
        chatNpc(
            worried,
            "Just recently we lost a scholar when she accidentally activated a portal and fell " +
                "through it.",
        )
        chatPlayer(worried, "Was she okay?")
        chatNpc(
            neutral,
            "Yes, she turned up a few days later, a little confused but physically fine. It " +
                "happens every now and then, but most people turn up eventually.",
        )
        chatPlayer(worried, "I guess I'd better be careful then.")
        chatNpc(
            neutral,
            "Just be cautious of walking into walls. Is there anything else I can help you with?",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ektheme
    private suspend fun Dialogue.ektheme() {
        chatPlayer(quiz, "Hello. Who are you?")
        chatNpc(happy, "Hi there. I'm Ektheme, an expert on the Xerician era.")
        while (true) {
            val option =
                choice4(
                    "Who was Xeric?",
                    1,
                    "How did Xeric have so much power?",
                    2,
                    "What happened to Xeric?",
                    3,
                    "Goodbye.",
                    4,
                )
            val keepTalking =
                when (option) {
                    1 -> {
                        whoWasXeric()
                        true
                    }
                    2 -> xericsPower()
                    3 -> xericsFate()
                    else -> {
                        chatPlayer(neutral, "Goodbye.")
                        false
                    }
                }
            if (!keepTalking) {
                return
            }
        }
    }

    private suspend fun Dialogue.whoWasXeric() {
        chatPlayer(quiz, "Who was Xeric?")
        chatNpc(
            neutral,
            "Xeric was a dark mage who ruled Great Kourend for at least ten decades. A time " +
                "period that is sometimes known as the Age of Strife.",
        )
        chatPlayer(quiz, "At least? Do you not know how long he was in power for?")
        chatNpc(
            neutral,
            "We do have records of course. However, while Xeric was indeed ruler of the kingdom " +
                "from the 37th to the 47th decades, some believe that he actually ruled for " +
                "longer.",
        )
        chatPlayer(quiz, "How come?")
        chatNpc(
            neutral,
            "It is often speculated that Xeric and his predecessor, King Shayzien VII, were in " +
                "fact the same person.",
        )
        chatPlayer(confused, "How could that be?")
        chatNpc(neutral, "Like Xeric, King Shayzien VII was fascinated with the Dark Altar.")
        chatNpc(
            neutral,
            "The King was killed at the Battle of the Concourse during the 37th decade. We have " +
                "many accounts of this, yet his body was never recovered.",
        )
        chatNpc(
            neutral,
            "Just days after his death, we have the first recorded mention of Xeric, an " +
                "incredibly powerful mage who resembled the King.",
        )
        chatNpc(
            neutral,
            "It is written that Xeric killed the rulers of Kourend's five cities and claimed the " +
                "kingdom as his own.",
        )
        chatPlayer(quiz, "So King Shayzien VII was resurrected as Xeric using the Dark Altar?")
        chatNpc(
            neutral,
            "It's possible. I am no expert on necromancy but we know the Dark Altar has the " +
                "power to resurrect beasts. We also know that it can extend life. It's how the " +
                "people of Arceuus are immortal.",
        )
        chatNpc(neutral, "King Shayzien VII may have found a way to combine these powers.")
    }

    private suspend fun Dialogue.xericsPower(): Boolean {
        chatPlayer(quiz, "How did Xeric have so much power?")
        chatNpc(
            neutral,
            "Xeric drew much of his power from the Dark Altar, but he asserted his authority as " +
                "ruler through his personal army of creatures, the lizardmen.",
        )
        if (QuestRequirements.hasCompleted(player, TALE_OF_THE_RIGHTEOUS)) {
            chatNpc(
                neutral,
                "Originally, we believed that Xeric created the lizardmen. However, recent " +
                    "discovieres suggest they were actually created during the time of King " +
                    "Shayzien VII.",
            )
            chatPlayer(quiz, "Do you know why Xeric needed the lizardmen?")
        } else {
            chatPlayer(shocked, "Xeric created the lizardmen!?")
            chatNpc(
                sad,
                "Sadly so, though we're not exactly sure how he did so. Xeric used the " +
                    "citizens of Kourend for many experiments, as well as for his own sadistic " +
                    "pleasures.",
            )
            chatNpc(
                neutral,
                "We have little written record of the experiments, but we believe they might " +
                    "have led to the creation of the lizardmen.",
            )
            chatPlayer(quiz, "Do you know why he created the lizardmen?")
        }
        chatNpc(
            neutral,
            "Xeric was undoubtedly powerful, but he couldn't rule the people singlehandedly, nor " +
                "could he rely on human informants. He needed an army he could control.",
        )
        chatNpc(
            neutral,
            "Lizardmen are loyal creatures and would do as he commanded without question. With " +
                "them at his disposal, the people were powerless to resist his authority.",
        )
        val option =
            choice3(
                "What happened to the lizardmen after Xeric's reign?",
                1,
                "What happened to Xeric?",
                2,
                "Goodbye.",
                3,
            )
        return when (option) {
            1 -> {
                lizardmenAfterXeric()
                true
            }
            2 -> xericsFate()
            else -> {
                chatPlayer(neutral, "Goodbye.")
                false
            }
        }
    }

    private suspend fun Dialogue.lizardmenAfterXeric() {
        chatPlayer(quiz, "What happened to the lizardmen after Xeric's reign?")
        chatNpc(
            neutral,
            "They remain loyal to Xeric. Even to this day they continue to attack the kingdom, " +
                "as you may have seen.",
        )
        chatNpc(neutral, "Fortunately, the Shayzien Army keeps them under control.")
        chatPlayer(
            confused,
            "Why do they keep fighting? Surely after a thousand years they must know Xeric is " +
                "gone?",
        )
        chatNpc(
            neutral,
            "Without examining their minds, we will never know for sure. However, we assume " +
                "Xeric genetically altered them to enforce that their duty is to serve him.",
        )
        chatPlayer(quiz, "But if they are serving Xeric, why do they attack the kingdom?")
        chatNpc(neutral, "A good question. One that I'm afraid I do not know the answer to.")
        chatNpc(quiz, "Is there anything else you would like to know?")
    }

    private suspend fun Dialogue.xericsFate(): Boolean {
        chatPlayer(quiz, "What happened to Xeric?")
        chatNpc(
            neutral,
            "During the 47th decade, after ten decades of oppression, the people revolted " +
                "against Xeric and his army of lizardmen.",
        )
        chatPlayer(quiz, "Why did the people suffer for so long?")
        chatNpc(
            neutral,
            "The events were triggered through the courage of one man, Byrne. He was not a man " +
                "of particular stature of nobility, though he did proceed to become king.",
        )
        chatPlayer(quiz, "What happened?")
        chatNpc(
            neutral,
            "'Twas the 312th day of decade 47. Byrne, a farmer by trade, is said to have " +
                "witnessed a lizardman guard murder a child whilst working his farm.",
        )
        chatNpc(
            angry,
            "Outraged at what he had just seen, Byrne grabbed his pitchfork and sprinted towards " +
                "the guard.",
        )
        chatNpc(
            angry,
            "Blinded by fury, Byrne attacked the guard, slaughtering the lizardman in plain " +
                "sight of all around.",
        )
        chatNpc(
            neutral,
            "Legend has it Byrne then gave a galvanizing speech, standing over the butchered " +
                "guard with his bloodied pitchfork held aloft in the air, urging the people to " +
                "fight back.",
        )
        chatPlayer(quiz, "Do you know what he said?")
        chatNpc(
            sad,
            "Sadly no. While we have many witness accounts, they all differ, so we can not " +
                "verify it at all.",
        )
        chatPlayer(quiz, "Ah, so what happened next?")
        chatNpc(
            neutral,
            "Whatever Byrne said, the people responded. Together, the citizens of Kourend " +
                "revolted, grabbing whatever they could use as a weapon. They fought the " +
                "lizardmen.",
        )
        chatNpc(
            neutral,
            "The lizardmen, while strong and powerful, were simply outnumbered. In a matter of " +
                "days, they had been driven back into the Kebos Lowlands.",
        )
        chatPlayer(quiz, "So what happened to Xeric?")
        chatNpc(
            neutral,
            "During the battle, the temple surrounding the Dark Altar was destroyed, weakening " +
                "Xeric's power. We believe he fled to Quidamortem.",
        )
        chatNpc(
            neutral,
            "Some say Xeric was actually killed during the revolt, but we have no record of his " +
                "remains being discovered.",
        )
        chatPlayer(quiz, "So no one knows what really happened to him?")
        if (QuestRequirements.hasCompleted(player, TALE_OF_THE_RIGHTEOUS)) {
            chatNpc(
                neutral,
                "Xeric was incredibly powerful, but without the Dark Altar, he would have soon " +
                    "become mortal and perished.",
            )
            chatNpc(
                neutral,
                "However, recent discoveries suggest that he may have found a way to amplify the " +
                    "power of the altar. This may have allowed him to live far longer than " +
                    "previously believed.",
            )
            chatPlayer(confused, "Hmmm, interesting.")
            chatNpc(neutral, "Is there anything else I can help you with?")
        } else {
            chatNpc(
                neutral,
                "Xeric was incredibly powerful but we can reasonably assume that he soon " +
                    "perished. Without the power of the Dark Altar, Xeric would have become " +
                    "mortal.",
            )
            chatPlayer(happy, "Good riddance by the sounds of it.")
            chatNpc(neutral, "Indeed. Is there anything else I can help you with?")
        }
        val option =
            choice3(
                "What happened to the lizardmen after Xeric's reign?",
                1,
                "How did Xeric have so much power?",
                2,
                "No thanks.",
                3,
            )
        return when (option) {
            1 -> {
                lizardmenAfterXeric()
                true
            }
            2 -> xericsPower()
            else -> {
                chatPlayer(neutral, "No thanks.")
                false
            }
        }
    }

    private companion object {
        const val KINGDOM_DIVIDED = "quest_kingdomdivided"
        const val TALE_OF_THE_RIGHTEOUS = "quest_taleoftherighteous"
    }
}
