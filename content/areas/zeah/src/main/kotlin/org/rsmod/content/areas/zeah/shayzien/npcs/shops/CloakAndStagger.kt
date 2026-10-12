package org.rsmod.content.areas.zeah.shayzien.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.content.areas.zeah.kourend.kourendRumour
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Oswald
// https://oldschool.runescape.wiki/w/Transcript:Sheryl
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class CloakAndStagger @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        for (type in BAR_STAFF) {
            onOpNpc1(type) { startDialogue(it.npc) { barStaff(it.npc) } }
            onOpNpc3(type) { player.openShop(it.npc) }
        }
    }

    private fun Player.openShop(npc: Npc) {
        shops.open(this, npc, "The Cloak and Stagger", "inv.shayzien_pub")
    }

    private suspend fun Dialogue.barStaff(npc: Npc) {
        if (npc.isType(OSWALD)) {
            val address = if (access.isBodyTypeA()) "lad" else "lassy"
            chatNpc(happy, "'Ello there $address! What can I be getting you?")
        } else {
            chatNpc(happy, "Welcome to the Cloak and Stagger! Anything I can get you?")
        }
        while (true) {
            val option =
                choice5(
                    "I'd like to order a drink, please.",
                    1,
                    "Got anything special on tap?",
                    2,
                    "What is this place?",
                    3,
                    "Have you heard any rumours here?",
                    4,
                    "Actually, I think I need to be going.",
                    5,
                )
            when (option) {
                1 -> {
                    chatPlayer(happy, "I'd like to order a drink, please.")
                    chatNpc(happy, "Of course, what would you like?")
                    player.openShop(npc)
                    return
                }
                2 -> {
                    chatPlayer(quiz, "Got anything special on tap?")
                    chatNpc(
                        happy,
                        "We're always running a keg of our locally-brewed ale, Lizardkicker.",
                    )
                    chatNpc(neutral, "I will warn you though, its an acquired taste.")
                    chatPlayer(quiz, "What's in it?")
                    chatNpc(neutral, "Oh, the usual... Wheat, hops, yeast...")
                    chatNpc(shifty, "...reptile scales...")
                    chatPlayer(shocked, "Scales?!")
                    chatNpc(neutral, "Don't worry about the scales.")
                    chatNpc(quiz, "Was there anything else?")
                }
                3 -> {
                    chatPlayer(quiz, "What is this place?")
                    chatNpc(
                        happy,
                        "This here is the Cloak and Stagger. The finest drinking establishment " +
                            "in the whole of Shayzien.",
                    )
                    chatPlayer(quiz, "Isn't it the only pub in the city?")
                    chatNpc(laugh, "Like I say, the finest in all of Shayzien!")
                    chatNpc(
                        neutral,
                        "Most of our patrons are soldiers returning on leave from the " +
                            "frontlines. We do get a few regular folk in too though.",
                    )
                    chatNpc(
                        neutral,
                        "By the way, its generally considered polite to buy a drink if you're " +
                            "going to stand around and chat.",
                    )
                    chatNpc(quiz, "...With that said, is there anything else I can help you with?")
                }
                4 -> {
                    chatPlayer(quiz, "Have you heard any rumours here?")
                    kourendRumour()
                    return
                }
                else -> {
                    chatPlayer(neutral, "Actually, I think I need to be going.")
                    chatNpc(happy, "Safe travels!")
                    return
                }
            }
        }
    }

    private companion object {
        const val OSWALD = "npc.shayzien_barman"
        val BAR_STAFF = listOf(OSWALD, "npc.shayzien_barmaid")
    }
}
