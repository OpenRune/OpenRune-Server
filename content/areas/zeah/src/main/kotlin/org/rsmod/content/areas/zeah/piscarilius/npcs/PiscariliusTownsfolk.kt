package org.rsmod.content.areas.zeah.piscarilius.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class PiscariliusTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        for (type in CUT_OFF_PASSENGERS) {
            onOpNpc1(type) { startDialogue(it.npc) { passenger(cutOff = true) } }
        }
        for (type in PASSENGERS) {
            onOpNpc1(type) { startDialogue(it.npc) { passenger(cutOff = false) } }
        }
        onOpNpc1("npc.zeah_dock_passenger_01") { it.npc.say(MARK_OVERHEAD) }
        onOpNpc1("npc.zeah_dock_passenger_02") { startDialogue(it.npc) { cheryl() } }
        onOpNpc1("npc.zeah_dock_passenger_03") { startDialogue(it.npc) { charles() } }
        onOpNpc1("npc.zeah_dock_passenger_04") { startDialogue(it.npc) { sarah() } }
        onOpNpc1("npc.zeah_dock_passenger_11") { startDialogue(it.npc) { rossAndBen() } }
        onOpNpc1("npc.zeah_dock_passenger_12") { startDialogue(it.npc) { dobwinkle() } }
        onOpNpc1("npc.zeah_dock_captain_janaway") { startDialogue(it.npc) { captainJanaway() } }
        onOpNpc1("npc.zeah_dock_parking_attendant") { startDialogue(it.npc) { portOfficial() } }
        onOpNpc1("npc.zeah_dock_worker") { startDialogue(it.npc) { portWorker() } }
        onOpNpc1("npc.zeah_dock_dark_wizard") { startDialogue(it.npc) { darkWizard() } }
        onOpNpc1("npc.piscarilius_dockworker_repair") { startDialogue(it.npc) { dockWorker() } }
        onOpNpc1("npc.piscarilius_dockworker_deliver") { startDialogue(it.npc) { dockWorker() } }
        onOpNpc1("npc.piscarilius_dockmaster") { startDialogue(it.npc) { dockmaster() } }
        onOpNpc1("npc.piscarilius_fisherman") { startDialogue(it.npc) { fisherman() } }
        onOpNpc1("npc.piscarilius_grub_hunter") { startDialogue(it.npc) { nicholas() } }
        for (citizen in POOR_CITIZENS) {
            onOpNpc1(citizen) { startDialogue(it.npc) { poorCitizen() } }
        }
        for (thief in WARRENS_THIEVES) {
            onOpNpc1(thief) { startDialogue(it.npc) { warrensThief() } }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Alexander
    // https://oldschool.runescape.wiki/w/Transcript:Darren_(Piscarilius)
    // TODO(content): [verify:in_game] whether "what a time to be" is really cut off for Alexander,
    //  Andrea, Lorraine and Simon, as their transcripts show
    private suspend fun Dialogue.passenger(cutOff: Boolean) {
        when (access.random.of(5)) {
            0 -> chatNpc(happy, "I can't believe we've finally arrived, I can't wait to explore!")
            1 -> chatNpc(happy, "So much to see, I don't know where to start!")
            2 -> {
                val end = if (cutOff) "be" else "be alive!"
                chatNpc(happy, "A whole new continent to explore... what a time to $end")
            }
            3 -> {
                chatNpc(quiz, "Are you the tour guide?")
                chatPlayer(confused, "Nope, I'm just as lost as you.")
            }
            else ->
                chatNpc(
                    worried,
                    "I overheard someone mention there are pirates and thieves here, be careful!",
                )
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Cheryl
    private suspend fun Dialogue.cheryl() {
        chatPlayer(quiz, "Hello, what are you doing here?")
        chatNpc(
            happy,
            "Hi there, my husband and I are adventurers, Captain Janaway brought us here.",
        )
        chatPlayer(quiz, "What brings you to Great Kourend?")
        chatNpc(
            happy,
            "New lands to explore of course! Isn't it exciting? A whole new world that we know " +
                "nothing about just waiting for us to discover it!",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Charles
    // TODO(content): [system:treasure_trails] easy emote clue reward
    private suspend fun Dialogue.charles() {
        chatNpc(worried, "Have you seen my wife Sarah? She's always wandering off...")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sarah_(Piscarilius)
    private suspend fun Dialogue.sarah() {
        chatNpc(
            worried,
            "I can't find my husband, we got seperated as we disembarked the ship.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ross_and_Ben
    private suspend fun Dialogue.rossAndBen() {
        chatNpcSpecific("Ben", ROSS_AND_BEN, sad, "Daddy I'm tired.")
        chatNpcSpecific(
            "Ross",
            ROSS_AND_BEN,
            neutral,
            "I know son, it was a long journey. I just need to work out where we're going.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Dobwinkle
    private suspend fun Dialogue.dobwinkle() {
        chatNpc(
            bored,
            "Such a long voyage... maybe one day there will be a glider service out here.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Captain_Janaway
    private suspend fun Dialogue.captainJanaway() {
        chatNpc(neutral, "Sorry I can't take any more passengers today.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Port_Official
    private suspend fun Dialogue.portOfficial() {
        chatPlayer(quiz, "Hello! What are you doing here?")
        chatNpc(
            neutral,
            "Hello, I'm keeping this berth free for the return of Lady Piscarilius.",
        )
        chatPlayer(quiz, "Lady Piscarilius is out at sea?")
        chatNpc(
            neutral,
            "Indeed, As a keen seafarer Lady Piscarilius often leads voyages herself.",
        )
        chatPlayer(quiz, "Is she expected to return soon?")
        chatNpc(
            neutral,
            "Lady Piscarilius did not reveal how long she may be at sea for nor where she " +
                "intended to explore.",
        )
        chatPlayer(neutral, "Ok, thanks.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Port_worker
    private suspend fun Dialogue.portWorker() {
        chatNpc(angry, "Go away, I'm busy!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Dark_wizard_(Great_Kourend)
    private suspend fun Dialogue.darkWizard() {
        chatPlayer(quiz, "Hello! What are you doing here?")
        chatNpc(
            neutral,
            "Hello, I am here to investigate rumours of a dark magical presence in these lands.",
        )
        chatPlayer(quiz, "What rumours have you heard exactly?")
        chatNpc(
            neutral,
            "I've heard there are hidden ruins in the mountains here with the power to " +
                "re-animate beings from the dead. Zandar Horfyre sent me to study them.",
        )
        chatPlayer(quiz, "Do you know where in the mountains these ruins are?")
        chatNpc(
            neutral,
            "Sadly no but I will find them, I will begin looking for information in Arceeus, " +
                "they seem like my kind of people.",
        )
        chatPlayer(neutral, "If you say so, good luck.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Dock_worker
    // TODO(content): [system:npc_overhead] fishing crane repair shouts
    private suspend fun Dialogue.dockWorker() {
        chatNpc(
            neutral,
            "We could always do with an extra pair of hands on the dock. See Frankie, the fresh " +
                "fish trader, if you're looking for work.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Dockmaster
    // TODO(content): [system:treasure_trails] medium challenge scroll
    private suspend fun Dialogue.dockmaster() {
        val title = if (access.isBodyTypeA()) "sir" else "m'lady"
        chatNpc(neutral, "What can I do for you, $title?")
        val ask =
            choice2(
                "What do you do around here?",
                true,
                "Sorry, I was just looking around.",
                false,
            )
        if (!ask) {
            chatPlayer(neutral, "Sorry, I was just looking around.")
            chatNpc(neutral, "Fair enough.")
            return
        }
        chatPlayer(quiz, "What do you do around here?")
        chatNpc(neutral, "It's my job to keep track of operations on the docks.")
        val help =
            choice2(
                "Is there anything I can help out with?",
                true,
                "I'll let you get back to work.",
                false,
            )
        if (!help) {
            chatPlayer(neutral, "I'll let you get back to work.")
            return
        }
        chatPlayer(quiz, "Is there anything I can help out with?")
        if (player.statBase("stat.crafting") < CRANE_CRAFTING_LEVEL) {
            chatNpc(
                neutral,
                "Thanks for the offer, but I'm afraid you need a higher crafting level before " +
                    "you can help me.",
            )
            mesbox("You need a crafting level of 30 to assist the Piscarilius dockmaster.")
            return
        }
        chatNpc(
            neutral,
            "You look like you know a thing or two about crafting, perhaps you could help us " +
                "maintain the fishing cranes around the docks.",
        )
        chatNpc(
            neutral,
            "You'll need a hammer, some nails and a few wooden planks, 3 planks per broken crane " +
                "should be enough.",
        )
        chatNpc(
            neutral,
            "I can't offer you any payment at the moment, but hard work doesn't go unnoticed " +
                "around these parts.",
        )
        chatPlayer(happy, "Sure, I'll see what I can do.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Fisherman_(Port_Piscarilius)
    private suspend fun Dialogue.fisherman() {
        chatNpc(angry, "What do you want?")
        val what =
            choice2(
                "What are you doing out here?",
                true,
                "I'll let you get back to it.",
                false,
            )
        if (!what) {
            chatPlayer(neutral, "I'll let you get back to it.")
            return
        }
        chatPlayer(quiz, "What are you doing out here?")
        chatNpc(happy, "Catching anglerfish! This is the only place you can find them!")
        val how =
            choice2(
                "How can I catch anglerfish?",
                true,
                "I'll let you get back to it.",
                false,
            )
        if (!how) {
            chatPlayer(neutral, "I'll let you get back to it.")
            return
        }
        chatPlayer(quiz, "How can I catch anglerfish?")
        if (player.statBase("stat.fishing") < ANGLERFISH_FISHING_LEVEL) {
            chatNpc(
                neutral,
                "I have heard good things about you, outsider, however, you will need to train " +
                    "your fishing skill before catching these.",
            )
            return
        }
        chatNpc(
            neutral,
            "You will need a fishing rod and some sandworms to use as bait. If you need " +
                "supplies, try Tynan's shoreside store.",
        )
        chatPlayer(happy, "Thanks. I'll let you get back to fishing.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Nicholas
    // TODO(content): [system:npc_overhead] grub hunting shouts
    // TODO(content): [system:treasure_trails] medium challenge scroll
    private suspend fun Dialogue.nicholas() {
        val title = if (access.isBodyTypeA()) "mister" else "madam"
        chatNpc(
            neutral,
            "Sorry $title, you'll have to make it quick - I'm busy hunting these wriggly buggers!",
        )
        val help =
            choice2(
                "Is there anything I can do to help?",
                true,
                "I'll let you get back to work.",
                false,
            )
        if (!help) {
            chatPlayer(neutral, "I'll let you get back to work.")
            return
        }
        chatPlayer(quiz, "Is there anything I can do to help?")
        val address = if (access.isBodyTypeA()) "sir" else "m'lady"
        chatNpc(
            neutral,
            "You'll have to speak to Tynan about that, $address, you can find him in the fishing " +
                "store just south of here.",
        )
        chatPlayer(neutral, "Thanks. I'll let you get back to work.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Poor_looking_man
    // https://oldschool.runescape.wiki/w/Transcript:Poor_looking_woman
    private suspend fun Dialogue.poorCitizen() {
        chatPlayer(quiz, "Hey there, is this where you live?")
        chatNpc(
            angry,
            "Yes it is! Don't look down your nose at me. I have to put up with an awful lot to " +
                "earn my living, life is a daily struggle.",
        )
        chatPlayer(neutral, "Sorry, I am just curious. Why is life so difficult here?")
        when (access.random.of(5)) {
            0 ->
                chatNpc(
                    sad,
                    "Just look at the place, rats, flies and all sorts just swarming the area. " +
                        "Doesn't help that I stink but ain't no one who likes using those tin " +
                        "baths right in public view.",
                )
            1 ->
                chatNpc(
                    sad,
                    "It is difficult to make ends meet. There are just so many skilled " +
                        "fishermen here that the competition is too much! Leaves no room for " +
                        "someone like me.",
                )
            2 ->
                chatNpc(
                    angry,
                    "Thieves! Thieves everywhere. Come to think of it, they always look " +
                        "remarkably like you! Adventurers running about stealing our things, get " +
                        "out of my sight.",
                )
            3 -> {
                chatNpc(
                    angry,
                    "Look, this is none of your business, I'd appreciate it if you left me to " +
                        "it. I don't want to see another fancy adventurer again!",
                )
                chatPlayer(worried, "Okay okay! I'll be off now.")
            }
            else -> {
                chatNpc(
                    worried,
                    "Gang crime is everywhere! I see them sometimes... it makes me very uneasy. " +
                        "It becomes difficult to sleep at night, never mind the draft in here.",
                )
                chatPlayer(
                    neutral,
                    "I've heard about those gangs! The Shayzien soldiers do a lot of work to put " +
                        "an end to it.",
                )
                chatNpc(
                    angry,
                    "Don't talk to me about those soldiers, all they ever do is blitz onto these " +
                        "docks with not a care in the world for us folk.",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Thief
    private suspend fun Dialogue.warrensThief() {
        when (access.random.of(6)) {
            0 -> chatNpc(bored, "Another lovely day in this dung heap.")
            1 -> chatNpc(shifty, "Watch yourself, I don't trust you.")
            2 ->
                chatNpc(
                    shifty,
                    "The thieves down here are just small time. The real thieves are the ones " +
                        "who rule.",
                )
            3 -> chatNpc(shifty, "We're all liars here.")
            4 -> chatNpc(neutral, "Ain't no honour down here. Never assume otherwise.")
            else -> chatNpc(happy, "I made a good score the other day.")
        }
    }

    private companion object {
        const val CRANE_CRAFTING_LEVEL = 30
        const val ANGLERFISH_FISHING_LEVEL = 82
        const val ROSS_AND_BEN = "npc.zeah_dock_passenger_11"
        const val MARK_OVERHEAD = "Cheryl where is our crate? We can't leave without our luggage!"
        val CUT_OFF_PASSENGERS =
            listOf(
                "npc.zeah_dock_passenger_13",
                "npc.zeah_dock_passenger_08",
                "npc.zeah_dock_passenger_10",
                "npc.zeah_dock_passenger_07",
            )
        val PASSENGERS =
            listOf(
                "npc.zeah_dock_passenger_05",
                "npc.zeah_dock_passenger_09",
                "npc.zeah_dock_passenger_06",
            )
        val POOR_CITIZENS =
            listOf(
                "npc.piscarilius_poor_citizen",
                "npc.piscarilius_poor_citizen_2",
                "npc.piscarilius_poor_citizen_female",
                "npc.piscarilius_poor_citizen_female_2",
                "npc.piscarilius_poor_citizen_female_3",
            )
        val WARRENS_THIEVES =
            listOf("npc.warrens_thief_1", "npc.warrens_thief_2", "npc.warrens_thief_stall")
    }
}
