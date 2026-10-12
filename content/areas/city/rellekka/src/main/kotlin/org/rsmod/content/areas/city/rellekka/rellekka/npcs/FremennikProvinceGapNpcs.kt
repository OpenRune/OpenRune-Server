package org.rsmod.content.areas.city.rellekka.rellekka.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class FremennikProvinceGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.mdaughter_svidi") { startDialogue(it.npc) { svidi() } }
        onOpNpc1("npc.viking_lalli_troll") { startDialogue(it.npc) { lalli() } }
        onOpNpc1("npc.aluft_gnome_explorer_mountain") { startDialogue(it.npc) { brambickle() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Svidi (before Mountain Daughter)
    // TODO(content): [quest:mountain_daughter] after-quest liaison dialogue
    private suspend fun Dialogue.svidi() {
        chatPlayer(quiz, "Hello. Where are you going?")
        chatNpc(angry, "None of your business! Leave me alone!")
        chatPlayer(neutral, "It just seemed as if you were walking in circles.")
        chatNpc(angry, "No I'm not! Now leave me alone!")
        chatPlayer(worried, "Okay, okay.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lalli (before The Fremennik Trials)
    // TODO(content): [quest:the_fremennik_trials] stone soup and the golden wool sale after it
    private suspend fun Dialogue.lalli() {
        chatPlayer(happy, "Hello there.")
        chatNpc(
            angry,
            "Bah! Puny humans always try steal Lallis' golden apples! You go away now!",
        )
        chatPlayer(confused, "Uh.... okay...")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Brambickle
    // TODO(content): [system:treasure_trails] hard clue puzzle box dialogue
    // TODO(content): [system:gnome_restaurant] delivery dialogue
    private suspend fun Dialogue.brambickle() {
        val hasMintCake = "obj.aluft_gnome_mint_cake" in player.inv
        val hasSeedPod = "obj.aluft_seed_pod" in player.inv
        val choice =
            when {
                hasMintCake && hasSeedPod ->
                    choice3(
                        "You're a long way from home, what are you doing here?",
                        1,
                        "What does this mint cake do?",
                        2,
                        "What does this seed pod do?",
                        3,
                    )
                hasMintCake ->
                    choice2(
                        "You're a long way from home, what are you doing here?",
                        1,
                        "What does this mint cake do?",
                        2,
                    )
                hasSeedPod ->
                    choice2(
                        "You're a long way from home, what are you doing here?",
                        1,
                        "What does this seed pod do?",
                        3,
                    )
                // TODO(content): [verify:in_game] whether a one-option menu is shown here
                else -> 1
            }
        when (choice) {
            1 -> {
                chatPlayer(quiz, "You're a long way from home, what are you doing here?")
                chatNpc(
                    happy,
                    "I'm on an expedition to try and extend the bounds of gnome knowledge. King " +
                        "Narnode has sent out a number of gnome explorers to map the furthest " +
                        "reaches of Gielinor.",
                )
                chatPlayer(neutral, "Seems like a hard job.")
                chatNpc(
                    happy,
                    "Oh no, I enjoy it - I've always had itchy feet ever since I was a little " +
                        "gnome.",
                )
                chatPlayer(
                    neutral,
                    "Still, you seem to have drawn the short straw here - there's nothing but " +
                        "snow...",
                )
                chatNpc(
                    happy,
                    "Oh, I like it - very peaceful. Avoiding those Trolls can be tricky though.",
                )
                chatPlayer(neutral, "Tell me about it...")
            }
            2 -> {
                chatPlayer(quiz, "What does this mint cake do?")
                chatNpc(
                    happy,
                    "Gnomish mint cake is a great source of energy for the gnome traveller. It " +
                        "will give you that extra bit of zip when you are running out of steam.",
                )
                chatPlayer(happy, "Ooh, that might come in very handy. Do you have any spare?")
                chatNpc(neutral, "I need all the mint cake I have, I'm afraid.")
            }
            else -> {
                chatPlayer(quiz, "What does this seed pod do?")
                chatNpc(
                    happy,
                    "Ah, the seed pod of the Grand Tree is a fascinating spirit artefact. It is " +
                        "a very useful thing, though one must treat it with respect, since the " +
                        "Grand Tree has a connection with all of its parts, however distant they",
                )
                chatNpc(happy, "may be. How do you think the Spirit Trees work?")
                chatNpc(
                    happy,
                    "If you launch its seeds in a far flung location, you will be helping the " +
                        "tree survive, so the grand tree will help a gnome glider pilot find " +
                        "you, using the seed burst itself (which marks the sky) and a little " +
                        "magic, and bring you",
                )
                chatNpc(
                    happy,
                    "back home as an act of gratitude. Budding farmers will also find their " +
                        "link with plants to be just a little bit better than before.",
                )
                chatNpc(
                    neutral,
                    "However, it takes time for a pilot to find you, and if you are in a real " +
                        "hurry, and I mean a real emergency, you can just squash the seed pod. " +
                        "The Grand Tree doesn't like this, but will pull you back",
                )
                chatNpc(
                    neutral,
                    "home straight away. Don't do this if you need to do anything with plants " +
                        "straight away, though - your fingers will be considerably less green " +
                        "for a while after using the seed pod this way, and the journey is not",
                )
                chatNpc(neutral, "very pleasant...")
                chatPlayer(happy, "Wow, that's very interesting. Thanks.")
            }
        }
    }
}
