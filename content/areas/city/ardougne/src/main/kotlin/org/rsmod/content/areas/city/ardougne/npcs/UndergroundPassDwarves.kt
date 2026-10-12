package org.rsmod.content.areas.city.ardougne.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class UndergroundPassDwarves @Inject constructor(private val objRepo: ObjRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.upassdwarf1") { startDialogue(it.npc) { niloof() } }
        onOpNpc1("npc.upassdwarf2") { startDialogue(it.npc) { klank() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Niloof
    // TODO(content): [quest:song_of_the_elves] "Lord Iorwerth is no longer an issue" after the
    // quest
    private suspend fun Dialogue.niloof() {
        chatPlayer(happy, "Hi Niloof.")
        chatNpc(
            happy,
            "Traveller, thank the stars you're still around! I thought your time had come.",
        )
        chatPlayer(
            happy,
            "I've still a few years in me yet. So what are you still doing down here?",
        )
        chatNpc(
            neutral,
            "Well with Iban and his lot gone, we've been left with a great cavern filled with " +
                "resources. We thought we'd actually do what we came here for and get mining.",
        )
        chatPlayer(happy, "Fair enough. Well you take care of yourselves.")
        chatNpc(happy, "And you.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Klank
    private suspend fun Dialogue.klank() {
        chatPlayer(happy, "Hello Klank.")
        chatNpc(happy, "Hello again adventurer, so you're still around?")
        chatPlayer(happy, "Still here!")
        when (choice2("Have you anymore gauntlets?", 1, "Take care Klank.", 2)) {
            1 -> {
                chatPlayer(quiz, "Have you any more gauntlets?")
                chatNpc(
                    neutral,
                    "Well..yes, but they're not cheap to make. I'll have to sell you a pair...",
                )
                chatPlayer(quiz, "How much?")
                chatNpc(neutral, "5,000 coins.")
                val option =
                    choice2("5,000, you must be joking!", 1, "Okay then, I'll take a pair.", 2)
                if (option == 1) {
                    chatPlayer(shocked, "5,000, you must be joking!")
                    chatNpc(neutral, "We don't joke down here friend...")
                    return
                }
                chatPlayer(happy, "Okay then, I'll take a pair.")
                if (!player.invTakeFee(GAUNTLETS_PRICE)) {
                    chatPlayer(sad, "Oh dear, I haven't got enough money...")
                    chatNpc(neutral, "Sorry, I can't sell them any cheaper than that.")
                    return
                }
                player.invAddOrDrop(objRepo, GAUNTLETS)
                objbox(GAUNTLETS, "Klank sells you some gauntlets.")
                chatNpc(happy, "There you go... I hope they help.")
                chatPlayer(happy, "I'll see you around Klank.")
            }
            2 -> {
                chatPlayer(happy, "Take care Klank.")
                chatNpc(happy, "You too adventurer...")
            }
        }
    }

    private companion object {
        const val GAUNTLETS = "obj.klanks_gauntlets"
        const val GAUNTLETS_PRICE = 5_000
    }
}
