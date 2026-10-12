package org.rsmod.content.areas.misc.lobbies.tempoross.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:met_flags] Koanee, Lauretta, Timallus, Taiman and the sailor have
//  longer dialogue after the player heard Captain Pudi's introduction with 35 Fishing
// TODO(content): [system:tempoross] using eggs, damp eggs, salmon and the spooky egg on the
//  Unkah talkers
class UnkahTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.tempoross_lobby_announcer") { startDialogue(it.npc) { captainDudi() } }
        onOpNpc1("npc.tempoross_ferry_from_lobby_npc") { startDialogue(it.npc) { nathwood() } }
        onOpNpc1("npc.tempoross_lobby_character") { startDialogue(it.npc) { sailor() } }
        onOpNpc1("npc.tempoross_koanee") { startDialogue(it.npc) { koanee() } }
        onOpNpc1("npc.tempoross_lauretta") { startDialogue(it.npc) { lauretta() } }
        onOpNpc1("npc.tempoross_timallus") { startDialogue(it.npc) { timallus() } }
        onOpNpc1("npc.tempoross_taiman") { startDialogue(it.npc) { taiman() } }
        onOpNpc1("npc.tempoross_hut_character") { startDialogue(it.npc) { retiredSailor() } }
        onOpNpc1("npc.tempoross_ishmael") { startDialogue(it.npc) { ishmael() } }
        for (angler in SPIRIT_ANGLERS) {
            onOpNpc1(angler) { startDialogue(it.npc) { spiritAngler() } }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Captain_Dudi (at Ruins of Unkah)
    // TODO(content): [system:tempoross] boarding overheads and Tempoross Cove dialogue
    private suspend fun Dialogue.captainDudi() {
        chatNpc(happy, "Prepare yeself, we'll be off soon!")
        chatPlayer(happy, "Aye aye!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ferryman_Nathwood
    // TODO(content): [system:tempoross] ferry to Al Kharid on "Yes" and the Ferry option
    private suspend fun Dialogue.nathwood() {
        chatPlayer(quiz, "Hello, where does your ferry go?")
        chatNpc(neutral, "I can ferry ye up t' Al Kharid, if ye want t' be off?")
        if (!choice2("Yes", true, "No", false)) {
            chatNpc(happy, "Want t' continue the fight against Tempoross, eh? Good stuff!")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sailor_(Ruins_of_Unkah)
    private suspend fun Dialogue.sailor() {
        chatNpc(
            happy,
            "Ahoy there! Ye best talk t' the Captain, she'll get ye up t' speed on the moist " +
                "beastie ye'll be facin'!",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Koanee
    private suspend fun Dialogue.koanee() {
        chatPlayer(happy, "Hey, how's it go...")
        chatNpc(
            angry,
            "Sorry my friend, the Skipper has been grindin' me up the wrong way lately. If ye " +
                "need anythin', talk t' Cap'ain Pudi over by the dock.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lauretta
    private suspend fun Dialogue.lauretta() {
        chatPlayer(quiz, "Hey, what are yo...")
        chatNpc(
            neutral,
            "Sorry, can't talk to ye right now. Busy with paperwork. If ye need anythin', talk " +
                "t' Captain Pudi over by the dock.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Timallus
    private suspend fun Dialogue.timallus() {
        chatPlayer(happy, "Hey, how's it go...")
        chatNpc(
            neutral,
            "Apples, trout. Maybe some spi... Oh sorry matey, busy getting the grub together for " +
                "our next voyage. If ye need anything, talk t' Captain Pudi over by the dock.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Taiman
    private suspend fun Dialogue.taiman() {
        chatPlayer(happy, "Hi, my name is ${player.displayName}. What's wi...")
        chatNpc(
            sad,
            "I not be in the mood t' talk, sorry. If ye need anythin', talk to Cap'ain Pudi over " +
                "by the dock.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Retired_sailor
    // TODO(content): [system:tempoross] the soaked pages exchange (option and Exchange op) and
    //  using reward items on him
    private suspend fun Dialogue.retiredSailor() {
        val title = if (access.isBodyTypeA()) "man" else "woman"
        chatNpc(neutral, "Do you have something you'd like to trade, young $title?")
        while (true) {
            val option =
                choice3("What kind of trade can you offer?", 1, "What is this place?", 2, "No.", 3)
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What kind of trade can you offer?")
                    chatNpc(
                        neutral,
                        "Well, we find a lot of soaked pages in our nets. If you've got " +
                            "something we really want, I could swap it for 25 soaked pages.",
                    )
                    return
                }
                2 -> {
                    chatPlayer(quiz, "What is this place?")
                    chatNpc(
                        neutral,
                        "Just a little place where we Spirit Anglers can store things, young " +
                            "$title. I used to be the chief mate on one of our ships, but now I " +
                            "just watch over the stores from the land.",
                    )
                    chatNpc(neutral, "Maybe you'd like to do a trade, though, while you're here?")
                }
                3 -> {
                    chatPlayer(neutral, "No.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ishmael
    // TODO(content): [system:shops] no server inv for Ishmael's Fish He Sells (Trade op and the
    //  stock option)
    private suspend fun Dialogue.ishmael() {
        chatPlayer(quiz, "Hello, what do you do around here?")
        chatNpc(
            happy,
            "The name's Ishmael. I sell fishing supplies, along with some of the local catch.",
        )
        while (true) {
            val option =
                choice3(
                    "Can I take a look at what you have in stock?",
                    1,
                    "Why did you set up shop around here? Seems a little run down.",
                    2,
                    "Goodbye.",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Can I take a look at what you have in stock?")
                    chatNpc(happy, "Aye.")
                    return
                }
                2 -> {
                    chatPlayer(
                        quiz,
                        "Why did you set up shop around here? It seems a little run down.",
                    )
                    chatNpc(
                        neutral,
                        "Argh... run down it is! But since Tempoross showed up, seadogs from all " +
                            "across Gielinor have been coming to wrangle the beast. It only made " +
                            "sense to sell off our spare resources.",
                    )
                }
                3 -> {
                    chatPlayer(neutral, "I think that's everything. Goodbye.")
                    chatNpc(happy, "Safe travels.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Spirit_Angler
    private suspend fun Dialogue.spiritAngler() {
        chatNpc(neutral, "Hello.")
        chatPlayer(quiz, "I was wondering if you could answer a few questions?")
        chatNpc(
            neutral,
            "Sorry, I'm quite busy preparing to face Tempoross. Captain Pudi by the dock can " +
                "answer any questions you might have.",
        )
        chatPlayer(neutral, "Okay, thanks.")
    }

    private companion object {
        val SPIRIT_ANGLERS =
            listOf(
                "npc.tempoross_lobby_spirit_angler_man_1",
                "npc.tempoross_lobby_spirit_angler_man_2",
                "npc.tempoross_lobby_spirit_angler_woman_1",
                "npc.tempoross_lobby_spirit_angler_woman_2",
            )
    }
}
