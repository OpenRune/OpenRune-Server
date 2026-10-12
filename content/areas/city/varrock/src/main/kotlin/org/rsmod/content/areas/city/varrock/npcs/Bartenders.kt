package org.rsmod.content.areas.city.varrock.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.spam
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class Bartenders @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.bluemoon_bartender") { startDialogue(it.npc) { blueMoon() } }
        onOpNpc1("npc.donkey_bartender") { startDialogue(it.npc) { dancingDonkey() } }
        onOpNpc1("npc.jollyboar_bartender") { startDialogue(it.npc) { jollyBoar() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bartender_(Blue_Moon_Inn)
    // TODO(content): [miniquest:alfred_grimhands_barcrawl] Uncle Humphry's Gutrot
    // TODO(content): [system:treasure_trails] easy cryptic clue reward
    private suspend fun Dialogue.blueMoon() {
        chatNpc(happy, "What can I do yer for?")
        val option =
            choice3(
                "A glass of your finest ale please.",
                1,
                "Can you recommend where an adventurer might make his fortune?",
                2,
                "Do you know where I can get some good equipment?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(happy, "A glass of your finest ale please.")
                chatNpc(happy, "No problemo. That'll be 2 coins.")
                if (buyBeer()) {
                    player.spam("You buy a pint of beer.")
                } else {
                    chatPlayer(sad, "Oh dear. I don't seem to have enough money.")
                }
            }
            2 -> blueMoonFortune()
            3 -> {
                chatPlayer(quiz, "Do you know where I can get some good equipment?")
                chatNpc(
                    neutral,
                    "Well, there's the sword shop across the road, or there's also all sorts of " +
                        "shops up around the market.",
                )
            }
        }
    }

    private suspend fun Dialogue.blueMoonFortune() {
        chatPlayer(quiz, "Can you recommend where an adventurer might make his fortune?")
        chatNpc(
            shifty,
            "Ooh I don't know if I should be giving away information, makes the game too easy.",
        )
        val option =
            choice3(
                "Oh ah well...",
                1,
                "Game? What are you talking about?",
                2,
                "Just a small clue?",
                3,
            )
        when (option) {
            1 -> chatPlayer(sad, "Oh ah well...")
            2 -> {
                chatPlayer(confused, "Game? What are you talking about?")
                chatNpc(
                    neutral,
                    "This world around us... is an online game... called Old School RuneScape.",
                )
                chatPlayer(
                    confused,
                    "Nope, still don't understand what you are talking about. What does " +
                        "'online' mean?",
                )
                chatNpc(
                    neutral,
                    "It's a sort of connection between magic boxes across the world, big boxes " +
                        "on people's desktops and little ones people can carry. They can talk " +
                        "to each other to play games.",
                )
                chatPlayer(angry, "I give up. You're obviously completely mad!")
            }
            3 -> {
                chatPlayer(quiz, "Just a small clue?")
                chatNpc(
                    neutral,
                    "Go and talk to the bartender at the Jolly Boar Inn, he doesn't seem to mind " +
                        "giving away clues.",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bartender_(Dancing_Donkey_Inn)
    private suspend fun Dialogue.dancingDonkey() {
        chatPlayer(happy, "Hello.")
        chatNpc(happy, "Good day to you, brave adventurer. Can I get you a refreshing beer?")
        while (true) {
            when (choice3("Yes please!", 1, "No thanks.", 2, "How much?", 3)) {
                1 -> {
                    chatPlayer(happy, "Yes please!")
                    chatNpc(happy, "Ok then, that's two gold coins please.")
                    if (buyBeer()) {
                        chatNpc(happy, "Cheers!")
                        chatPlayer(happy, "Cheers!")
                    }
                    return
                }
                2 -> {
                    chatPlayer(neutral, "No thanks.")
                    chatNpc(neutral, "Let me know if you change your mind.")
                    return
                }
                3 -> {
                    chatPlayer(quiz, "How much?")
                    chatNpc(happy, "Two gold pieces a pint. So, what do you say?")
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bartender_(Jolly_Boar_Inn)
    // TODO(content): [miniquest:alfred_grimhands_barcrawl] barcrawl drink
    private suspend fun Dialogue.jollyBoar() {
        chatNpc(happy, "Can I help you?")
        val option =
            choice3(
                "I'll have a beer please.",
                1,
                "Any hints where I can go adventuring?",
                2,
                "Heard any good gossip?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(happy, "I'll have a pint of beer please.")
                chatNpc(happy, "Ok, that'll be two coins please.")
                if (!buyBeer()) {
                    chatPlayer(sad, "Oh dear, I don't seem to have enough money.")
                }
            }
            2 -> {
                chatPlayer(quiz, "Any hints on where I can go adventuring?")
                chatNpc(neutral, "Ooh, now. Let me see...")
                chatNpc(
                    neutral,
                    "Well there is the Varrock sewers. There are tales of untold horrors coming " +
                        "out at night and stealing babies from houses.",
                )
                chatPlayer(happy, "Sounds perfect! Where's the entrance?")
                chatNpc(neutral, "It's just to the east of the palace.")
            }
            3 -> {
                chatPlayer(quiz, "Heard any gossip?")
                chatNpc(
                    neutral,
                    "I'm not that well up on the gossip out here. I've heard that the bartender " +
                        "in the Blue Moon Inn has gone a little crazy, he keeps claiming he is " +
                        "part of something called an online game.",
                )
                chatNpc(
                    neutral,
                    "What that means, I don't know. That's probably old news by now though.",
                )
            }
        }
    }

    private fun Dialogue.buyBeer(): Boolean {
        if (!player.invTakeFee(BEER_PRICE)) {
            return false
        }
        player.invAddOrDrop(objRepo, "obj.beer")
        return true
    }

    private companion object {
        const val BEER_PRICE = 2
    }
}
