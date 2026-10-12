package org.rsmod.content.areas.city.alkharid.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.fishingLvl
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class AlKharidTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.osman") { startDialogue(it.npc) { osman() } }
        onOpNpc1("npc.arabian_sorceress") { startDialogue(it.npc) { sorceress() } }
        onOpNpc1("npc.arabian_sorceress_apprentice") { startDialogue(it.npc) { apprentice() } }
        onOpNpc4("npc.arabian_sorceress_apprentice") {
            startDialogue(it.npc) { apprenticeTooBusy() }
        }
        onOpNpc1("npc.ali_the_farmer") { startDialogue(it.npc) { aliTheFarmer() } }
        onOpNpc1("npc.ali_the_guard") { startDialogue(it.npc) { aliTheGuard() } }
        onOpNpc1("npc.ali_the_smith") { startDialogue(it.npc) { aliTheSmith() } }
        onOpNpc1("npc.ali_the_tailor") { startDialogue(it.npc) { aliTheTailor() } }
        onOpNpc1("npc.shantay_guard") { startDialogue(it.npc) { shantayGuard() } }
        onOpNpc1("npc.shantay_guard_still") { startDialogue(it.npc) { shantayGateGuard() } }
        onOpNpc3("npc.pilot_al_kharid_base") { startDialogue(it.npc) { captainDalbur() } }
        onOpNpc1("npc.tempoross_ferry_to_lobby_npc") { startDialogue(it.npc) { sathwood() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Osman (before Prince Ali Rescue)
    // TODO(content): [quest:prince_ali_rescue] in-progress and post-quest dialogue
    // TODO(content): [quest:contact] Sophanem and Keris dagger dialogue
    // TODO(content): [system:sorceress_garden] "I'd like to talk about sq'irks."
    private suspend fun Dialogue.osman() {
        chatNpc(neutral, "Hello. I am Osman. What can I assist you with?")
        val option =
            choice2(
                "You don't seem very tough. Who are you?",
                1,
                "Nothing. I'm just being nosy.",
                2,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "You don't seem very tough. Who are you?")
                chatNpc(neutral, "I work for Al Kharid's Emir. That is all you need to know.")
            }
            2 -> {
                chatPlayer(shifty, "Nothing. I'm just being nosy.")
                chatNpc(
                    neutral,
                    "That bothers me not. The secrets of Al Kharid protect themselves.",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sorceress
    // TODO(content): [system:sorceress_garden] teleport the player outside the house when ejected
    // TODO(content): [quest:beneath_cursed_sands] "Selim." instead of "Osman." after the quest
    private suspend fun Dialogue.sorceress() {
        chatNpc(angry, "Who are you and what do you want?")
        val option =
            choice4(
                "None of your business!",
                1,
                "I'm here to kill you!",
                2,
                "Can I have some sq'irks please?",
                3,
                "I'm just passing by.",
                4,
            )
        when (option) {
            1 -> {
                chatPlayer(angry, "None of your business!")
                chatPlayer(angry, "I go where I like and do what I like.")
                chatNpc(angry, "Not in my house. Be gone!")
                chatNpc(angry, "Be gone, intruder!")
            }
            2 -> {
                chatPlayer(angry, "I'm here to kill you!")
                chatNpc(laugh, "I think not!")
                chatNpc(angry, "Be gone, intruder!")
            }
            3 -> {
                chatPlayer(quiz, "Can I have some sq'irks please?")
                chatNpc(neutral, "What do you want them for?")
                chatPlayer(neutral, "Someone asked me to bring them some.")
                chatNpc(quiz, "Who?")
                chatPlayer(neutral, "Osman.")
                chatNpc(
                    angry,
                    "In that case I'm sorry, but you can't. I have had a falling out with him " +
                        "recently and would rather not oblige him.",
                )
            }
            4 -> chatPlayer(neutral, "I'm just passing by.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Apprentice
    // TODO(content): [system:sorceress_garden] teleport once the player has asked Osman about it
    private suspend fun Dialogue.apprentice() {
        chatPlayer(quiz, "Hello. What are you doing?")
        chatNpc(
            sad,
            "Cleaning, cleaning, always cleaning. This apprenticeship isn't all that it was " +
                "cracked up to be.",
        )
        chatPlayer(quiz, "Whose apprentice are you?")
        chatNpc(
            sad,
            "Oh, Aadeela, the sorceress upstairs, said she'd teach me magic, she did. And here I " +
                "am scrubbing floors without a spell to help me.",
        )
        val option =
            choice2(
                "I could cast a Water Blast or a Wind Blast spell?",
                1,
                "Surely there must be upsides to the task?",
                2,
            )
        when (option) {
            1 -> {
                chatPlayer(
                    happy,
                    "I could cast a Water Blast or a Wind Blast spell to hurry things along if " +
                        "you'd like?",
                )
                chatNpc(
                    worried,
                    "No, no, she'd kill me or worse if she knew I was using Magic to do chores. " +
                        "Her last apprentice - well I'd rather not say.",
                )
                chatPlayer(quiz, "Oh go on, what happened to them?")
                chatNpc(worried, "They say she turned them into little spiders.")
                chatPlayer(sad, "Oh, that's too bad. I had better leave you to it.")
            }
            2 -> apprenticeUpsides()
        }
    }

    private suspend fun Dialogue.apprenticeUpsides() {
        chatPlayer(quiz, "Surely there must be upsides to the task?")
        chatNpc(
            sad,
            "Nope. Clean this, clean that. When I'm finished cleaning here I have to go help out " +
                "in the garden.",
        )
        chatPlayer(quiz, "What garden?")
        chatNpc(worried, "Oh, I shouldn't have told you.")
        val option =
            choice2(
                "You're right, you shouldn't have.",
                1,
                "Oh, you can talk to me. I can see you're having a bad day.",
                2,
            )
        when (option) {
            1 -> {
                chatPlayer(neutral, "You're right, you shouldn't have.")
                chatNpc(
                    neutral,
                    "Well, if you don't mind then, I'm busy and have to get back to work.",
                )
                chatPlayer(neutral, "Don't let me keep you.")
            }
            2 -> {
                chatPlayer(happy, "Oh, you can talk to me. I can see you're having a bad day.")
                chatNpc(sad, "You know you're right. Nobody listens to me.")
                apprenticeListen()
            }
        }
    }

    private suspend fun Dialogue.apprenticeListen() {
        val option =
            choice2(
                "I can't blame them, all you do is whine.",
                1,
                "A sympathetic ear can do wonders.",
                2,
            )
        if (option == 1) {
            chatPlayer(angry, "I can't blame them, all you do is whine.")
            mesbox("The apprentice is clearly hurt and ignores you.")
            return
        }
        chatPlayer(happy, "A sympathetic ear can do wonders.")
        chatNpc(
            happy,
            "Yes, if I just let my frustrations out, I'd feel a lot better. Now what was I " +
                "saying?",
        )
        val next =
            choice3(
                "I don't know. You were whining about something or other.",
                1,
                "To tell you the truth, I wasn't really listening.",
                2,
                "You mentioned something about the garden.",
                3,
            )
        when (next) {
            1 -> {
                chatPlayer(neutral, "I don't know. You were whining about something or other.")
                chatPlayer(neutral, "To tell you the truth, I wasn't really listening.")
                mesbox("The apprentice is clearly hurt and ignores you.")
            }
            2 -> {
                chatPlayer(neutral, "To tell you the truth, I wasn't really listening.")
                mesbox("The apprentice is clearly hurt and ignores you.")
            }
            3 -> apprenticeGarden()
        }
    }

    private suspend fun Dialogue.apprenticeGarden() {
        chatPlayer(quiz, "You mentioned something about the garden.")
        chatNpc(sad, "Oh yeah, that dreadful garden of hers.")
        chatPlayer(quiz, "Where is it?")
        chatNpc(neutral, "Oh, it's nowhere.")
        chatPlayer(confused, "What do you mean?")
        chatNpc(
            neutral,
            "Well it's here, but not really. You see the sorceress is trying out some new type " +
                "of compression magic.",
        )
        chatPlayer(happy, "Oh, that sounds interesting - so how does it work?")
        chatNpc(
            neutral,
            "It would take too long to explain and, to be honest, I don't really understand how " +
                "it works.",
        )
        chatPlayer(quiz, "Fair enough, but tell me, how do you get to the garden?")
        chatNpc(happy, "By magic! The sorceress did teach me one spell.")
        val option =
            choice2(
                "Wow, cast the spell on me. It will be good Magic training for you.",
                1,
                "Oh, that's nice. Well it's been great talking to you.",
                2,
            )
        when (option) {
            1 -> {
                chatPlayer(
                    happy,
                    "Wow, cast the spell on me. It will be good Magic training for you.",
                )
                apprenticeTooBusy()
            }
            2 -> chatPlayer(happy, "Oh, that's nice. Well it's been great talking to you.")
        }
    }

    private suspend fun Dialogue.apprenticeTooBusy() {
        chatNpc(neutral, "I can't do that now, I'm far too busy sweeping.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ali_the_Farmer
    // TODO(content): [miniquest:rogue_trader] Ali Morrisane and threatened-shopkeeper options
    private suspend fun Dialogue.aliTheFarmer() {
        chatPlayer(happy, "Hello there!")
        chatNpc(neutral, "Oh, er, hello. What do you want?")
        val option =
            choice2("What can you tell me about Al Kharid?", 1, "So, what do you do here?", 2)
        when (option) {
            1 -> {
                chatPlayer(quiz, "What can you tell me about Al Kharid?")
                chatNpc(
                    neutral,
                    "There's not much farming land around here. Only that little patch outside.",
                )
            }
            2 -> {
                chatPlayer(quiz, "So, what do you do here?")
                chatNpc(
                    happy,
                    "I'm going to set up a shop selling farming implements. That patch out there " +
                        "may be small, but it's all we've got.",
                )
            }
        }
        chatPlayer(quiz, "Can you give me any advice on farming here in the desert?")
        chatNpc(neutral, "Like I said, I only know about cactuses...")
        chatPlayer(neutral, "Just tell me about cactuses, then.")
        chatNpc(neutral, "First you have to weed the patch using a rake.")
        chatPlayer(quiz, "Can you give me any other advice?")
        chatNpc(neutral, "Not really. I've not done much farming recently.")
        chatPlayer(quiz, "Well, can you at least sell me any gardening tools or seeds?")
        chatNpc(sad, "Sorry. They haven't been delivered yet.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ali_the_Guard
    // TODO(content): [miniquest:rogue_trader] Ali Morrisane and threatened-shopkeeper options
    private suspend fun Dialogue.aliTheGuard() {
        chatPlayer(happy, "Hello there!")
        chatNpc(angry, "I'm working. What do you have to say that's so urgent?")
        val option =
            choice2("What can you tell me about Al Kharid?", 1, "So, what do you do here?", 2)
        when (option) {
            1 -> {
                chatPlayer(quiz, "What can you tell me about Al Kharid?")
                chatNpc(neutral, "There's a lot of space here. More open space than back home.")
                chatPlayer(quiz, "So where is back home?")
                chatNpc(
                    neutral,
                    "Pollnivneach. It's a town to the south of the Shantay Pass.",
                )
            }
            2 -> {
                chatPlayer(quiz, "So, what do you do here?")
                chatNpc(
                    neutral,
                    "I'm on guard duty. Making sure nobody tries to steal anything from the " +
                        "house and tents in the middle of town.",
                )
                chatPlayer(quiz, "Why are you only guarding those buildings?")
                chatNpc(neutral, "That's all I've been hired to guard.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ali_the_Smith
    // TODO(content): [miniquest:rogue_trader] Ali Morrisane and threatened-shopkeeper options
    private suspend fun Dialogue.aliTheSmith() {
        chatPlayer(happy, "Hello there!")
        chatNpc(quiz, "You seem rather cheerful. Is there anything you're after?")
        val option =
            choice2("What can you tell me about Al Kharid?", 1, "So, what do you do here?", 2)
        when (option) {
            1 -> {
                chatPlayer(quiz, "What can you tell me about Al Kharid?")
                chatNpc(neutral, "Well, it's hot and full of sand.")
                chatPlayer(quiz, "Anything else?")
                chatNpc(neutral, "Don't ask me, I'm not from around here.")
                chatPlayer(quiz, "So where are you from?")
                chatNpc(
                    neutral,
                    "A town to the south of the Shantay Pass, called Pollnivneach.",
                )
            }
            2 -> {
                chatPlayer(quiz, "So, what do you do here?")
                chatNpc(
                    neutral,
                    "Not very much, at the moment. I came from further south to set up a " +
                        "smithy here...",
                )
                chatNpc(
                    sad,
                    "...but we still haven't set up the shops, so we have no customers yet.",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ali_the_Tailor
    // TODO(content): [miniquest:rogue_trader] Ali Morrisane and threatened-shopkeeper options
    private suspend fun Dialogue.aliTheTailor() {
        chatPlayer(happy, "Hello there!")
        chatNpc(neutral, "I'm a little busy at the moment. What is it?")
        val option =
            choice2("What can you tell me about Al Kharid?", 1, "So, what do you do here?", 2)
        when (option) {
            1 -> {
                chatPlayer(quiz, "What can you tell me about Al Kharid?")
                chatNpc(happy, "Oh, it has wonderful weather.")
                chatPlayer(confused, "Wonderful? It's hot and dry all the time!")
                chatNpc(neutral, "Not quite as hot as back home.")
                chatPlayer(quiz, "Where's home, then?")
                chatNpc(
                    neutral,
                    "Oh, it's a town to the south of the pass, called Pollnivneach.",
                )
            }
            2 -> {
                chatPlayer(quiz, "So, what do you do here?")
                chatNpc(
                    sad,
                    "If I had cloth, patterns and customers, I'd be a tailor. As it is, I'm a " +
                        "tailor with nothing to do.",
                )
                chatNpc(
                    angry,
                    "The silk merchant won't even sell me any silks, because he doesn't trust me!",
                )
                chatPlayer(quiz, "Anything I can do to help?")
                chatNpc(shifty, "No, no, it's all being dealt with.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Shantay_Guard (attackable guard)
    private suspend fun Dialogue.shantayGuard() {
        chatNpc(
            angry,
            "Go talk to Shantay. I'm on duty and I don't have time to talk to the likes of you!",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Shantay_Guard (guard at the gate)
    private suspend fun Dialogue.shantayGateGuard() {
        chatNpc(happy, "Hello there! What can I do for you?")
        when (choice2("I'd like to go into the desert please.", 1, "Nothing thanks.", 2)) {
            1 -> {
                chatPlayer(quiz, "I'd like to go into the desert please.")
                chatNpc(
                    neutral,
                    "You'll need a Shantay pass to go through the gate into the desert. See " +
                        "Shantay, he'll sell you one for a very reasonable price.",
                )
            }
            2 -> {
                chatPlayer(neutral, "Nothing thanks.")
                chatNpc(happy, "Okay then, have a nice day.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Captain_Dalbur (before The Grand Tree)
    // TODO(content): [quest:the_grand_tree] glider travel and post-quest options
    // TODO(content): [system:treasure_trails] Crack the Clue II
    private suspend fun Dialogue.captainDalbur() {
        chatNpc(happy, "Welcome to Gnome Air!")
        while (true) {
            val option =
                choice4(
                    "What's Gnome Air?",
                    1,
                    "Where can you take me?",
                    2,
                    "How much for one-way to Varrock?",
                    3,
                    "I'll leave you to it.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What's Gnome Air?")
                    chatNpc(happy, "Gnome Air is the finest airline in Gielinor!")
                    chatNpc(happy, "Well...it's the only real airline in Gielinor.")
                    chatNpc(laugh, "Ha!")
                    chatPlayer(quiz, "What do you mean by real airline?")
                    chatNpc(
                        laugh,
                        "Well there's a dodgy magic carpet operation in the desert, I doubt it " +
                            "will ever take off... Ha!",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Where can you take me?")
                    chatNpc(neutral, "Glough has ordered that I only take gnomes on Gnome Air.")
                }
                3 -> {
                    chatPlayer(quiz, "How much for one-way to Varrock?")
                    chatNpc(neutral, "I can't take you anywhere.")
                    chatPlayer(confused, "How come?")
                    chatNpc(
                        neutral,
                        "I would, but Glough has told me not to take humans on Gnome Air.",
                    )
                }
                4 -> {
                    chatPlayer(neutral, "I'll leave you to it.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ferryman_Sathwood (not met Captain Pudi)
    // TODO(content): [system:tempoross] ferry to the Ruins of Unkah and the Captain Pudi branch
    private suspend fun Dialogue.sathwood() {
        chatPlayer(happy, "Hello there.")
        chatNpc(happy, "Ahoy!")
        when (choice2("What are you doing here?", 1, "Where does your ferry go?", 2)) {
            1 -> {
                chatPlayer(quiz, "What are you doing here?")
                chatNpc(
                    happy,
                    "I ferry skilled sailors and fisherfolk south t' the Spirit Anglers' base in " +
                        "the Ruins of Unkah.",
                )
                chatPlayer(quiz, "The Spirit Anglers? Who are they?")
                chatNpc(
                    neutral,
                    "Why, they're a group of sea-faring folk researchin' and fightin' against " +
                        "Tempoross, the crazed spirit that's terrorising the seas! I'm surprised " +
                        "you haven't heard!",
                )
                chatPlayer(happy, "That sounds important! Can I help?")
                if (player.fishingLvl < TEMPOROSS_FISHING_LEVEL) {
                    chatNpc(
                        neutral,
                        "I can ferry ye down t' the Spirit Anglers, though they're looking for " +
                            "people with more fishing experience t' face Tempoross.",
                    )
                    mesbox("You need a Fishing level of 35 to fight Tempoross.")
                } else {
                    chatNpc(
                        happy,
                        "Of course! I can ferry ye down there when ye be ready. Ye'll want t' " +
                            "speak to Captain Pudi when ye arrive!",
                    )
                }
                chatPlayer(quiz, "Do I have to pay for the ferry?")
                chatNpc(laugh, "'Course not. Don't even 'ave t' fix a price!")
            }
            2 -> {
                chatPlayer(quiz, "Where does your ferry go?")
                chatNpc(neutral, "South t' the Spirit Anglers' base in the Ruins of Unkah.")
                chatNpc(happy, "Would ye like t' board?")
            }
        }
    }

    private companion object {
        const val TEMPOROSS_FISHING_LEVEL = 35
    }
}
