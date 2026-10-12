package org.rsmod.content.areas.zeah.piscarilius.npcs.shops

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class PiscariliusUnstockedShops : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.piscarilius_fish_trader") { startDialogue(it.npc) { frankie() } }
        onOpNpc1("npc.piscarilius_foodshop") { startDialogue(it.npc) { kenelme() } }
        onOpNpc1("npc.warrens_shopkeeper") { startDialogue(it.npc) { warrensShopKeeper() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Frankie
    // TODO(content): [system:shops] Frankie's Fishing Emporium; the piscarilius_fish_trader_*
    //  invs have no stock in the cache (Trade op)
    private suspend fun Dialogue.frankie() {
        while (true) {
            chatNpc(happy, "Welcome to Frankie's Fish Emporium, what can I do for you mate?")
            val option =
                choice3(
                    "What have you got for sale?",
                    1,
                    "What do you do around here?",
                    2,
                    "I'll be on my way",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What have you got for sale?")
                    chatNpc(happy, "We serve the finest fresh fish Piscarilius has to offer.")
                    return
                }
                2 -> {
                    chatPlayer(quiz, "What do you do around here?")
                    chatNpc(
                        neutral,
                        "I trade the finest fish in Piscarilius. In this city you get two " +
                            "choices, fishing or thuggery. I've kept afloat without resorting to " +
                            "the latter, not something which can be said by many of the folk " +
                            "around here.",
                    )
                }
                else -> {
                    chatPlayer(neutral, "I'll be on my way.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Kenelme
    // TODO(content): [system:shops] no server inv for Kenelme's Wares (Trade op)
    private suspend fun Dialogue.kenelme() {
        chatNpc(neutral, "What are you looking for, outsider?")
        val trade =
            choice2(
                "Let's see what you've got.",
                true,
                "I don't need anything.",
                false,
            )
        if (trade) {
            chatPlayer(happy, "Let's see what you've got.")
        } else {
            chatPlayer(neutral, "I don't need anything.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Shop_keeper_(The_Warrens)
    // TODO(content): [system:shops] no server inv for Warrens General Store (Trade op)
    private suspend fun Dialogue.warrensShopKeeper() {
        chatNpc(neutral, "Need something?")
        val selling =
            choice2("What are you selling?", true, "No thanks.", false)
        if (selling) {
            chatPlayer(quiz, "What are you selling?")
        } else {
            chatPlayer(neutral, "No thanks.")
        }
    }
}
