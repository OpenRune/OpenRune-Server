package org.rsmod.content.areas.misc.islands.corsaircove.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class CorsairCoveTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.corscurs_thief_sick") { startDialogue(it.npc) { sickCorsair() } }
        onOpNpc1("npc.corscurs_cook_sick") { startDialogue(it.npc) { sickCorsair() } }
        onOpNpc1("npc.corscurs_navigator_sick") { startDialogue(it.npc) { sickCorsair() } }
        onOpNpc1("npc.corscurs_cabinboy_sick") { startDialogue(it.npc) { sickCorsair() } }
        onOpNpc1("npc.corscurs_resource_meditate") { startDialogue(it.npc) { altarkiz() } }
        onOpNpc1("npc.corscurs_resource_guard") { startDialogue(it.npc) { haris() } }
        onOpNpc1("npc.ogress_tess") { startDialogue(it.npc) { chiefTess() } }
        onOpNpc1("npc.corscurs_lord_marshal") { startDialogue(it.npc) { lordMarshalBrogan() } }
        onOpNpc1("npc.corscurs_spa_owner") { startDialogue(it.npc) { madameCaldarium() } }
        onOpNpc1("npc.corscurs_banker_multi") { startDialogue(it.npc) { yusuf() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Arsen_the_Thief (before The Corsair Curse)
    // https://oldschool.runescape.wiki/w/Transcript:Gnocci_the_Cook
    // https://oldschool.runescape.wiki/w/Transcript:Ithoi_the_Navigator
    // https://oldschool.runescape.wiki/w/Transcript:Cabin_Boy_Colin
    // TODO(content): [quest:the_corsair_curse] quest and after-quest dialogue, and Colin's ship to
    // Rimmington after the quest
    private suspend fun Dialogue.sickCorsair() {
        chatNpc(sad, "Urgh...")
        mesbox("He looks too sick to chat.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Altarkiz
    private suspend fun Dialogue.altarkiz() {
        chatPlayer(happy, "Hello!")
        mesbox("He seems to be in deep concentration.")
        chatPlayer(neutral, "...I'll leave you to it.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Haris
    // TODO(content): [quest:dragon_slayer_i] the tale of Elvarg that grants entry to the resource
    // area after the quest
    private suspend fun Dialogue.haris() {
        chatNpc(happy, "Welcome, Effendi!")
        chatPlayer(happy, "Hello!")
        val option =
            choice3("Can I go in?", 1, "Who are you?", 2, "Bye!", 3, title = "Select an Option")
        when (option) {
            1 -> harisEntry()
            2 -> {
                chatPlayer(quiz, "Who are you?")
                chatNpc(
                    happy,
                    "Me? I am Haris! Owner of the area north of here, it is has great " +
                        "resources, I assure you.",
                )
                chatPlayer(quiz, "You own the land? Who did you buy it from?")
                chatNpc(laugh, "Buy? Oh no no no. Do you see this sword, Effendi?")
                chatPlayer(neutral, "I do.")
                chatNpc(laugh, "Well then, that is why it is mine! Hahaha.")
                chatPlayer(quiz, "Oh, so do I pay a toll? 10 coins should do, right?")
                chatNpc(
                    neutral,
                    "Not quite, while gold can get you to many places, I seek stories of " +
                        "heroics, for my land is only for the worthy.",
                )
                chatPlayer(quiz, "Is the story behind your sword one of heroics?")
                chatNpc(laugh, "I promise you, the monkey I took it from was big! Hahaha!")
            }
            3 -> {
                chatPlayer(happy, "Bye!")
                chatNpc(happy, "Farewell, Effendi!")
            }
        }
    }

    private suspend fun Dialogue.harisEntry() {
        chatPlayer(quiz, "Can I go in?")
        chatNpc(
            happy,
            "Come now Effendi, to pass you must tell me a story, one of grand heroics!",
        )
        if (vars[RESOURCE_ENTRY] != 0) {
            chatNpc(happy, "Of course Effendi! You have proved yourself worthy!")
            return
        }
        mesbox(
            "You tell the tale of how you bested a goblin, it lacks heroics and sounds more like " +
                "senseless murder.",
        )
        chatPlayer(
            neutral,
            "...And then I pulled the goblin mail from the corpse, but it didn't fit, so I " +
                "dropped it.",
        )
        chatNpc(
            angry,
            "You thought that was a story of heroics?! Come back when you have something worth " +
                "telling.",
        )
        mesbox(
            "To gain entrance to the resource area, you must first complete the quest 'Dragon " +
                "Slayer'.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Chief_Tess
    private suspend fun Dialogue.chiefTess() {
        chatNpc(angry, "What you want, skinny creature?")
        if (choice2("What is this place?", true, "I'll leave you alone, sorry.", false)) {
            chatPlayer(quiz, "What is this place?")
            chatNpc(
                neutral,
                "Dis our home. It comfy, though we thinking of maybe getting more space up on " +
                    "land, above. But silly Corsair men live up there an' think dey own whole " +
                    "place.",
            )
            chatNpc(shifty, "Maybe one day dat gonna change. Tess got plans.")
            chatPlayer(neutral, "Thanks.")
        } else {
            chatPlayer(worried, "I'll leave you alone, sorry.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lord_Marshal_Brogan
    private suspend fun Dialogue.lordMarshalBrogan() {
        chatPlayer(happy, "Good day!")
        chatNpc(happy, "Isn't it just!")
        chatPlayer(quiz, "What brings you down here?")
        chatNpc(
            neutral,
            "This and that... But mostly, I'm looking for a good place to mobilise my army.",
        )
        chatPlayer(confused, "That's awfully specific, you sure this is a good place for it?")
        chatNpc(happy, "I don't see why not!")
        chatPlayer(neutral, "I'm not sure there's enough land, to be honest.")
        chatNpc(
            happy,
            "Tish tosh! I'm importing sand from Al Kharid, there'll be a base camp here in no " +
                "time, do not fret!",
        )
        chatPlayer(neutral, "Oh. Okay then.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Madame_Caldarium
    // TODO(content): [system:treasure_trails] medium clue challenge scroll
    private suspend fun Dialogue.madameCaldarium() {
        chatPlayer(happy, "Hello!")
        chatNpc(happy, "Good morrow, adventurer.")
        when (choice3("I guess so.", 1, "Who are you?", 2, "Goodbye!", 3)) {
            1 -> {
                chatPlayer(neutral, "I guess so.")
                chatNpc(
                    confused,
                    "You guess? The sun is shining, the sea breeze is salty, what's not to enjoy?",
                )
                chatPlayer(
                    bored,
                    "I don't know... The sand? It's coarse and rough and irritating, and it gets " +
                        "everywhere.",
                )
                chatNpc(
                    happy,
                    "Hmm, well my spa isn't quite ready yet, but when it is, I'm sure that'll " +
                        "solve all of your sand related issues!",
                )
                chatPlayer(neutral, "Okay.")
            }
            2 -> caldariumWho()
            3 -> {
                chatPlayer(happy, "Goodbye!")
                chatNpc(happy, "Farewell, adventurer.")
            }
        }
    }

    private suspend fun Dialogue.caldariumWho() {
        chatPlayer(quiz, "Who are you?")
        chatNpc(
            happy,
            "I am Madame Caldarium! A pirate's life was for me, but lately I've been looking to " +
                "expand my business, I'm getting old, ya see.",
        )
        chatPlayer(
            confused,
            "A spa business? Do you really think these Corsairs are the washing type?",
        )
        chatNpc(laugh, "Ha! That's what the Rum is for!")
        chatNpc(
            angry,
            "I'm more concerned of their laziness, 'Can't work today Madame, I'm cursed!'. Bunch " +
                "of idiots, the lot of 'em.",
        )
        chatPlayer(
            neutral,
            "Surely you never expected this lot to be any different... They're either stealing, " +
                "killing or both!",
        )
        chatNpc(
            shifty,
            "Yes... I have plans adventurer, the local cave dwelling population are... Let's " +
                "say, sorting something out.",
        )
        chatPlayer(neutral, "Interesting, I'm sure that'll be an entertaining turn of events.")
        chatNpc(shifty, "We'll see...")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Yusuf (before The Corsair Curse)
    // TODO(content): [quest:the_corsair_curse] the bank, PIN and collection options after the
    // quest
    private suspend fun Dialogue.yusuf() {
        chatNpc(angry, "Bank's closed. Go away.")
    }

    private companion object {
        const val RESOURCE_ENTRY = "varbit.corsair_cove_resource_entry"
    }
}
