package org.rsmod.content.areas.misc.villages.barbarianoutpost.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:barbarian_assault] tutorial, waves, honour points and the reward shop
class BarbarianAssaultLobby : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.barbassault_barbarian_captain") { startDialogue(it.npc) { captainCain() } }
        onOpNpc1("npc.barbassault_barbarian_commander") {
            startDialogue(it.npc) { commanderConnad() }
        }
        onOpNpc1("npc.barbassault_barbarian_army_02") { startDialogue(it.npc) { privatePaldo() } }
        onOpNpc1("npc.barbassault_barbarian_army_03") {
            startDialogue(it.npc) { privatePendron() }
        }
        onOpNpc1("npc.barbassault_barbarian_army_04") {
            startDialogue(it.npc) { privatePierreb() }
        }
        onOpNpc1("npc.barbassault_barbarian_army_05") {
            startDialogue(it.npc) { privatePaldon() }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Captain_Cain (before starting the tutorial)
    private suspend fun Dialogue.captainCain() {
        chatPlayer(quiz, "Hello. What's this place?")
        chatNpc(
            angry,
            "Why you wretched Son-of-a-Big-Nosed-Warty-Smelly- Goblin! How dare you speak to a " +
                "commanding officer like that?",
        )
        chatPlayer(shocked, "I... err...")
        chatPlayer(confused, "What?")
        chatNpc(angry, "Stand straight! Chest out, shoulders back!")
        chatPlayer(worried, "Okay, okay.")
        chatNpc(angry, "You'll address me as captain!")
        chatPlayer(worried, "Yea... yes, captain!")
        chatNpc(
            neutral,
            "Better. Now what is a low-life, pathetic excuse for a human being doing sniffing " +
                "around my arena?",
        )
        chatPlayer(neutral, "Exploring, captain. Looked like a chance for combat, sir!")
        chatNpc(
            neutral,
            "Very well, make the most of this because I don't suffer idiots easily! This is the " +
                "Barbarian Assault arena. You want to sign up with us?",
        )
        val option =
            choice3(
                "Sir, yes sir!",
                1,
                "Let's skip the training, I'm an expert already.",
                2,
                "Nah, sounds boring.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(happy, "Sir, yes sir!")
                chatNpc(neutral, "Then listen closely.")
            }
            2 -> {
                chatPlayer(neutral, "Let's skip the training, I'm an expert already.")
                chatNpc(
                    neutral,
                    "Really? You don't look like one. Very well, upon your own head be it, I'll " +
                        "let you skip the training. Come back and speak to me if you find you're " +
                        "not the know-it-all you believe yourself to be.",
                )
            }
            3 -> boring()
        }
    }

    private suspend fun Dialogue.boring() {
        chatPlayer(bored, "Nah, sounds boring.")
        chatNpc(
            angry,
            "'Boring'? You'll get nowhere with that kind of attitude! Drop and give me ten!",
        )
        val option =
            choice3(
                "Maybe next time, I need to go wash my hair.",
                1,
                "Sir, yes sir!",
                2,
                "But I already did...",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(neutral, "Maybe next time, I need to go wash my hair.")
                chatNpc(angry, "You'll do as I say!")
                chatPlayer(angry, "Arrrghhh. Okay, okay!")
            }
            2 -> {
                chatPlayer(happy, "Sir, yes sir!")
                chatNpc(neutral, "Good to see you've learned some respect, make it five")
                chatPlayer(happy, "Yes sir!")
            }
            3 -> {
                chatPlayer(neutral, "But I already did!")
                chatNpc(confused, "You did?")
                chatPlayer(happy, "Sir, yes sir!")
                chatNpc(angry, "I don't trust you in the slightest. Do as I tell you!")
            }
        }
        access.anim(PUSH_UPS)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Commander_Connad (before speaking to Cain)
    private suspend fun Dialogue.commanderConnad() {
        chatPlayer(quiz, "Hello?")
        chatNpc(
            neutral,
            "Sorry soldier, you don't have permission to speak with me yet. Go and talk to the " +
                "Captain.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Private_Paldo
    private suspend fun Dialogue.privatePaldo() {
        chatPlayer(happy, "Hi, soldier.")
        chatNpc(happy, "Why, hello there!")
        chatPlayer(happy, "You seem like a jolly chap.")
        chatNpc(laugh, "And why not? 'Tis a greeeaat day!")
        chatPlayer(neutral, "Well, I suppose it is.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Private_Pendron
    private suspend fun Dialogue.privatePendron() {
        chatPlayer(happy, "Hi there.")
        chatNpc(worried, "Don't suppose you've seen a battleaxe around here?")
        chatPlayer(neutral, "A battleaxe? Nope, afraid not.")
        chatNpc(worried, "The captain is going to kill me if he finds out I've lost my weapon.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Private_Pierreb
    private suspend fun Dialogue.privatePierreb() {
        chatPlayer(quiz, "Hello. So you're just a private?")
        chatNpc(angry, "Show some respect! It's more than you'll achieve.")
        chatPlayer(happy, "I beg to differ. I'm in perfect shape!")
        chatNpc(angry, "Prove it!")
        chatPlayer(quiz, "How?")
        chatNpc(angry, "Give me five star-jumps!")
        access.anim(STAR_JUMPS)
        chatNpc(angry, "Five sit-ups.")
        access.anim(SIT_UPS)
        chatNpc(angry, "Run on the spot!")
        access.anim(RUN_ON_THE_SPOT)
        chatNpc(
            neutral,
            "Okay. Maybe you have what it takes. Best you speak with the captain.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Private_Paldon
    private suspend fun Dialogue.privatePaldon() {
        chatPlayer(happy, "Hi.")
        chatNpc(shifty, "Shhh. Don't talk to me.")
        chatPlayer(confused, "What? Why?")
        chatNpc(
            worried,
            "If I'm seen slacking off by talking to you, I'll be in deep trouble!",
        )
        chatPlayer(sad, "Oh... Sorry.")
    }

    private companion object {
        // TODO(content): [verify:in_game] emote timing during the private's and captain's orders
        const val PUSH_UPS = "seq.emote_pushups_5"
        const val STAR_JUMPS = "seq.emote_starjump_5"
        const val SIT_UPS = "seq.emote_situps_5"
        const val RUN_ON_THE_SPOT = "seq.emote_run_on_spot"
    }
}
