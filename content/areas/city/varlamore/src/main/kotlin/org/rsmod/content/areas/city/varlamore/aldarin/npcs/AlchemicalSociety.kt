package org.rsmod.content.areas.city.varlamore.aldarin.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:mastering_mixology] the lines for members of the Alchemical Society
//  (Alaxxi's society goals, Meztlan's potion tips) and Clanila's lab entrance check
// TODO(content): [verify:in_game] the closing option reads "Thanks! I'd better be going now."
//  once a question has been asked
class AlchemicalSociety : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.mm_lab_tech_01") { startDialogue(it.npc) { meztlan() } }
        onOpNpc1("npc.mm_lab_tech_02") { startDialogue(it.npc) { clanila() } }
        onOpNpc1("npc.mm_lab_tech_04") { startDialogue(it.npc) { juatil() } }
        onOpNpc1("npc.mm_lab_tech_05") { startDialogue(it.npc) { alaxxi() } }
        onOpNpc1("npc.mm_lab_tech_06") { startDialogue(it.npc) { kali() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Alaxxi
    private suspend fun Dialogue.alaxxi() {
        chatNpc(happy, "Nilsal, iknami! Do you need help with anything?")
        var leave = NOTHING
        while (true) {
            when (choice2("What do you do around here?", 1, leave, 2, title = TITLE)) {
                1 -> {
                    chatPlayer(quiz, "What do you do around here?")
                    chatNpc(
                        happy,
                        "I spend a lot of my time researching new product formulas for the lab. " +
                            "Just over here we have a batch of something new that I've dubbed " +
                            "'Earworm'.",
                    )
                    chatPlayer(confused, "Ear... Worm...?")
                    chatNpc(
                        happy,
                        "Indeed! Its effects produce musical auditory hallucinations! Soon, " +
                            "people will be able to listen to music while on the go. It'll sell " +
                            "like hotcakes!",
                    )
                    chatPlayer(
                        neutral,
                        "I feel like there has to be a better solution to that, but good luck " +
                            "anyway!",
                    )
                    leave = THANKS
                }
                else -> return leaving(leave)
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Clanila
    private suspend fun Dialogue.clanila() {
        chatNpc(happy, "Welcome to the Alchemical Society. What can I help you with?")
        var leave = NOTHING
        while (true) {
            val option =
                choice4(
                    "What is this place?",
                    1,
                    "What do you do around here?",
                    2,
                    "Can I buy some potions?",
                    3,
                    leave,
                    4,
                    title = TITLE,
                )
            when (option) {
                1 -> clanilaThisPlace()
                2 -> {
                    chatPlayer(quiz, "What do you do around here?")
                    chatNpc(
                        neutral,
                        "When I'm not working in the lab, I'm normally somewhere around here " +
                            "researching or administrating our orders. There's an awful lot of " +
                            "paperwork to get through!",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "Can I buy some potions?")
                    chatNpc(
                        neutral,
                        "We're not really a shop, so our products are not available to buy " +
                            "directly. Everything we make gets shipped wholesale around the " +
                            "kingdom.",
                    )
                    chatNpc(
                        neutral,
                        "You look like the adventuring type anyway. I can't imagine our products " +
                            "would be of much use to you.",
                    )
                }
                else -> return leaving(leave)
            }
            leave = THANKS
        }
    }

    private suspend fun Dialogue.clanilaThisPlace() {
        chatPlayer(quiz, "What is this place?")
        chatNpc(
            happy,
            "This is the main facility of the Alchemical Society. We're an organisation who fuse " +
                "the forgotten teachings of the Old Ones with traditional Gielinorian Herblore " +
                "practices.",
        )
        chatNpc(
            happy,
            "The lab under our feet allows us to fund our research by producing useful products " +
                "for the kingdom.",
        )
        chatPlayer(quiz, "So you do things differently to the druids of the mainland?")
        chatNpc(
            neutral,
            "In a way, but we very much respect Herblore as a profession and recognise that " +
                "whatever your druids do definitely works. Our goal is to advance the study of " +
                "Herblore, not replace it.",
        )
        chatNpc(
            neutral,
            "Anyway, I'm a little busy at the moment, but I'm sure some of the other lab " +
                "assistants would love to tell you more!",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Juatil
    private suspend fun Dialogue.juatil() {
        chatNpc(neutral, "Hello there. What do you need?")
        var leave = NOTHING
        while (true) {
            val option =
                choice3(
                    "What do you do around here?",
                    1,
                    "You alchemists sure wear a lot of safety gear.",
                    2,
                    leave,
                    3,
                    title = TITLE,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What do you do around here?")
                    chatNpc(
                        neutral,
                        "I mostly look after the lab equipment and make sure things run as they " +
                            "should.",
                    )
                    chatNpc(
                        sad,
                        "These days, I'm not much into the production side of things... All " +
                            "that moving up and down, pulling levers, carrying vials... These " +
                            "old bones ain't what they used to be.",
                    )
                }
                2 -> {
                    chatPlayer(happy, "You alchemists sure wear a lot of safety gear.")
                    chatNpc(
                        happy,
                        "Of course! Some of the mixtures down here can get pretty nasty in their " +
                            "raw form. Safety first!",
                    )
                    chatNpc(
                        shifty,
                        "Although between you and me... I'm not sure if anything this lab " +
                            "produces can be considered truly safe for human consumption.",
                    )
                    chatPlayer(shocked, "Yikes! I guess I'll not be trying anything then!")
                }
                else -> return leaving(leave)
            }
            leave = THANKS
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Kali
    private suspend fun Dialogue.kali() {
        chatPlayer(happy, "Hello there.")
        chatNpc(neutral, "I'm on duty, so you'll need to make this quick.")
        var leave = NOTHING
        while (true) {
            val option =
                choice3(
                    "What do you do around here?",
                    1,
                    "What's your favourite potion?",
                    2,
                    leave,
                    3,
                    title = TITLE,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What do you do around here?")
                    chatNpc(
                        neutral,
                        "I typically hang out in the lab and keep an eye the newbies. Someone " +
                            "needs to make sure no one breaks anything or loses an eye.",
                    )
                    chatNpc(
                        angry,
                        "Last week we had someone trying to churn butter in the agitator. They " +
                            "weren't around for long. I really wish they'd start properly " +
                            "vetting new society members.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "What's your favourite potion?")
                    chatNpc(neutral, "For me, its the Anti-leech lotion. I despise leeches.")
                    chatPlayer(
                        confused,
                        "Oh, that's surprising. I don't think I've seen many around here...",
                    )
                    chatNpc(
                        neutral,
                        "You've obviously never been swimming anywhere in the Tlati Rainforest...",
                    )
                }
                else -> return leaving(leave)
            }
            leave = THANKS
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Meztlan
    private suspend fun Dialogue.meztlan() {
        chatNpc(neutral, "Oh... Nilsal. Unusual to see someone up here. What can I do for you?")
        while (true) {
            val option =
                choice3(
                    "What do you do around here?",
                    1,
                    "Do you have any tips for potion production?",
                    2,
                    THANKS,
                    3,
                    title = TITLE,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What do you do around here?")
                    chatNpc(
                        sad,
                        "I used to work downstairs... but I started using the lab after hours " +
                            "for my own projects and the supervisor found out. Now I've been " +
                            "assigned to rooftop duty to look after the plants.",
                    )
                    chatPlayer(neutral, "Sorry to hear that. At least the view is good!")
                }
                2 -> {
                    chatPlayer(quiz, "Do you have any tips for potion production?")
                    chatNpc(
                        neutral,
                        "I do, but only for fellow members of the Alchemical Society. Perhaps " +
                            "you should join up if you're interested in Herblore.",
                    )
                    chatPlayer(neutral, "Interesting... Perhaps I should.")
                }
                else -> return leaving(THANKS)
            }
        }
    }

    private suspend fun Dialogue.leaving(text: String) {
        chatPlayer(neutral, text)
    }

    private companion object {
        const val TITLE = "Select an option"
        const val NOTHING = "Nothing. I'll be going now."
        const val THANKS = "Thanks! I'd better be going now."
    }
}
