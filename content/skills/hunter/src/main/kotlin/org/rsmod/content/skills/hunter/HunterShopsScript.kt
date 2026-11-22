package org.rsmod.content.skills.hunter

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc5
import org.rsmod.api.shops.Shops
import org.rsmod.content.skills.hunter.HunterCapeScript.Companion.GUILD_LEVEL
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class HunterShopsScript @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc5(ALECK) { shops.open(player, it.npc, "Aleck's Hunter Emporium.", "inv.hunting_shop_yanille") }
        onOpNpc5(ARTIMEUS) { shops.open(player, it.npc, "Nardah Hunter Shop.", "inv.hunting_shop_nardah") }
        onOpNpc1(STROM) { startDialogue(it.npc) { strom(it.npc) } }
        onOpNpc3(STROM) { openStrom(player, it.npc) }
        onOpNpc1(IMIA) { startDialogue(it.npc) { imia(it.npc) } }
        onOpNpc3(IMIA) {
            if (qualified(player)) openImia(player, it.npc) else startDialogue(it.npc) { refuse() }
        }
    }

    private fun qualified(player: Player): Boolean = player.statBase(HunterTrapScript.HUNTER) >= GUILD_LEVEL

    private fun openImia(player: Player, npc: Npc) = shops.open(player, npc, "Imia's Supplies", "inv.hunting_shop_guild")

    private fun openStrom(player: Player, npc: Npc) =
        shops.open(player, npc, "Elder Strom's Hunting Stall", "inv.great_conch_hunter_shop")

    private suspend fun Dialogue.strom(npc: Npc) {
        chatNpc(neutral, "Human. Do you wish to hunt? If so, I have supplies for you.")
        if (!choice2("Let's see what you have.", true, "I'm good, thanks.", false)) {
            chatPlayer(neutral, "I'm good, thanks.")
            chatNpc(neutral, "As you wish.")
            return
        }
        chatPlayer(neutral, "Let's see what you have.")
        chatNpc(happy, "You won't be disappointed.")
        openStrom(player, npc)
    }

    private suspend fun Dialogue.refuse() {
        chatNpc(
            neutral,
            "Sorry, but I can only provide services to qualified members of the Hunter Guild. You need " +
                "$GUILD_LEVEL Hunter to access Hunter Guild amenities.",
        )
    }

    private suspend fun Dialogue.imia(npc: Npc) {
        if (!qualified(player)) {
            refuse()
            return
        }
        chatNpc(happy, "Hi there. How's it going?")
        val choice = choice3("What do you do here?", 1, "What have you got for sale?", 2, "Never mind.", 3)
        if (choice == 1) {
            chatPlayer(quiz, "What do you do here?")
            chatNpc(
                happy,
                "I keep the hunters well equipped! I can sell you hunting equipment at a reasonable price.",
            )
            if (!choice2("What have you got for sale?", true, "Never mind.", false)) {
                chatPlayer(neutral, "Never mind.")
                return
            }
        } else if (choice == 3) {
            chatPlayer(neutral, "Never mind.")
            return
        }
        chatPlayer(quiz, "What have you got for sale?")
        chatNpc(happy, "Hunting supplies for every occasion.")
        openImia(player, npc)
    }

    private companion object {
        const val ALECK = "npc.hunting_shop_owner_yanille"
        const val ARTIMEUS = "npc.hunting_shop_owner_nardah"
        const val STROM = "npc.tortugan_strom_2ops"
        const val IMIA = "npc.hg_mixedhide_seller"
    }
}
