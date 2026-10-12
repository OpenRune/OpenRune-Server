package org.rsmod.content.areas.misc.villages.portkhazard.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Shop_keeper_(Port_Khazard)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class KhazardGeneralStore @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(SHOPKEEPER) { startDialogue(it.npc) { shopKeeper(it.npc) } }
        onOpNpc3(SHOPKEEPER) { player.openShop(it.npc) }
    }

    private fun Player.openShop(npc: Npc) {
        shops.open(this, npc, "Khazard General Store", "inv.khazardshop")
    }

    private suspend fun Dialogue.shopKeeper(npc: Npc) {
        chatNpc(happy, "Can I help you at all?")
        when (choice2("Yes please. What are you selling?", 1, "No thanks.", 2)) {
            1 -> {
                chatPlayer(quiz, "Yes please. What are you selling?")
                chatNpc(happy, "Take a look.")
                player.openShop(npc)
            }
            2 -> chatPlayer(neutral, "No thanks.")
        }
    }

    private companion object {
        const val SHOPKEEPER = "npc.khazard_shopkeeper"
    }
}
