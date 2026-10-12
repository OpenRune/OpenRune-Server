package org.rsmod.content.areas.city.yanille.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Frenita
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class CookeryShop @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.frenita") { startDialogue(it.npc) { frenita(it.npc) } }
        onOpNpc3("npc.frenita") { player.openCookeryShop(it.npc) }
    }

    private fun Player.openCookeryShop(npc: Npc) {
        shops.open(this, npc, "Frenita's Cookery Shop.", "inv.cookeryshop")
    }

    private suspend fun Dialogue.frenita(npc: Npc) {
        chatNpc(happy, "Would you like to buy some cooking equipment?")
        if (choice2("Yes please.", true, "No thank you.", false)) {
            player.openCookeryShop(npc)
            return
        }
        chatPlayer(neutral, "No thank you.")
    }
}
