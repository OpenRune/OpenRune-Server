package org.rsmod.content.areas.zeah.hosidius.npcs

import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Konoo
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:treasure_trails] easy emote clue reward
class Konoo : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.hosidius_saltpetre_miner") { startDialogue(it.npc) { konoo() } }
    }

    private suspend fun Dialogue.konoo() {
        chatNpc(angry, "More digging, less talking!")
        while (true) {
            val option =
                choice4(
                    "What's going on here?",
                    1,
                    "What can I do with saltpetre?",
                    2,
                    "Have you got a spare spade?",
                    3,
                    "I'll leave you to work.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What's going on here?")
                    chatNpc(
                        neutral,
                        "I'm digging for saltpetre, of course. Got to have plenty of saltpetre " +
                            "for the farms. You should grab a spade and get to work too - they " +
                            "always need saltpetre.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "What can I do with saltpetre?")
                    chatNpc(
                        neutral,
                        "It's a critical ingredient in a special fertiliser used on the farms " +
                            "here. Just mix it with some compost - ordinary compost, not that " +
                            "super stuff - to make the fertiliser.",
                    )
                    chatNpc(
                        neutral,
                        "Once you've got fertiliser, you can use it on Farmer Gricoller's fruit " +
                            "farm. Alternatively, Hosidius farmers always want some. You could " +
                            "sell it to them.",
                    )
                }
                3 -> spareSpade()
                else -> {
                    chatPlayer(neutral, "I'll leave you to work.")
                    return
                }
            }
        }
    }

    // TODO(content): [verify:in_game] whether a spade worn or in the bank counts as having one
    private suspend fun Dialogue.spareSpade() {
        chatPlayer(quiz, "Have you got a spare spade?")
        if (player.inv.count(SPADE) > 0) {
            chatNpc(neutral, "Yes, but I can see you don't need it.")
            return
        }
        if (!player.invAdd(player.inv, SPADE).success) {
            chatNpc(neutral, "Yes, but you've got no room for it.")
            return
        }
        objbox(SPADE, "Konoo gives you a spade.")
    }

    private companion object {
        const val SPADE = "obj.spade"
    }
}
