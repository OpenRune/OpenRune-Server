package org.rsmod.content.areas.city.alkharid.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Zeke
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:monkey_madness_i] "Hmmm, funny you should say that..." after the quest
// TODO(content): [miniquest:rogue_trader] "What do you think of Ali Morrisane?"
// TODO(content): [system:treasure_trails] easy clue reward
class ScimitarShop @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.zeke") { shopDialogue(it.npc) }
        onOpNpc3("npc.zeke") { player.openScimitarShop(it.npc) }
    }

    private fun Player.openScimitarShop(npc: Npc) {
        shops.open(this, npc, "Zeke's Superior Scimitars.", "inv.scimitarshop")
    }

    private suspend fun ProtectedAccess.shopDialogue(npc: Npc) =
        startDialogue(npc) { shopKeeper(npc) }

    private suspend fun Dialogue.shopKeeper(npc: Npc) {
        chatNpc(happy, "A thousand greetings, sir.")
        val option =
            choice3(
                "Do you want to trade?",
                1,
                "Nice cloak.",
                2,
                "Could you sell me a dragon scimitar?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Do you want to trade?")
                chatNpc(happy, "Yes, certainly. I deal in scimitars.")
            }
            2 -> {
                chatPlayer(happy, "Nice cloak.")
                chatNpc(happy, "Thank you.")
            }
            3 -> {
                chatPlayer(quiz, "Could you sell me a dragon scimitar?")
                chatNpc(shocked, "A dragon scimitar? A DRAGON scimitar?")
                chatNpc(
                    angry,
                    "The banana-brained nitwits who make them would never dream of selling any " +
                        "to me.",
                )
                chatNpc(
                    neutral,
                    "Seriously, you'll be a monkey's uncle before you'll ever hold a dragon " +
                        "scimitar.",
                )
                chatPlayer(sad, "Oh well, thanks anyway.")
                chatNpc(happy, "Perhaps you'd like to take a look at my stock?")
                if (choice2("Yes please, Zeke.", true, "Not today, thank you.", false)) {
                    player.openScimitarShop(npc)
                }
            }
        }
    }
}
