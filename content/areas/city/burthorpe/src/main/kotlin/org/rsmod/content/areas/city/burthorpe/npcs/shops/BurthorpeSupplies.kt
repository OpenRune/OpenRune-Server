package org.rsmod.content.areas.city.burthorpe.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Wistan
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class BurthorpeSupplies @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.death_shopkeeper") { startDialogue(it.npc) { wistan(it.npc) } }
        onOpNpc3("npc.death_shopkeeper") { player.openSupplies(it.npc) }
    }

    private fun Player.openSupplies(npc: Npc) {
        shops.open(this, npc, "Burthorpe Supplies", "inv.death_generalshop")
    }

    private suspend fun Dialogue.wistan(npc: Npc) {
        chatPlayer(happy, "Hi!")
        chatNpc(
            happy,
            "Welcome to Burthorpe Supplies. Your last shop before heading north into the " +
                "mountains!",
        )
        chatNpc(quiz, "Would you like to buy something?")
        when (choice2("Yes, please.", 1, "No, thanks.", 2)) {
            1 -> {
                chatPlayer(happy, "Yes, please.")
                player.openSupplies(npc)
            }
            2 -> chatPlayer(neutral, "No, thanks.")
        }
    }
}
