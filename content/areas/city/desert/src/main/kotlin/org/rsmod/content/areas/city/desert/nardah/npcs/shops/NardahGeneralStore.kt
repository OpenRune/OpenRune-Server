package org.rsmod.content.areas.city.desert.nardah.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Kazemde
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class NardahGeneralStore @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(KAZEMDE) { startDialogue(it.npc) { kazemde(it.npc) } }
        onOpNpc3(KAZEMDE) { player.openStore(it.npc) }
    }

    private fun Player.openStore(npc: Npc) {
        shops.open(this, npc, "Nardah General Store.", "inv.generalshopnardah")
    }

    private suspend fun Dialogue.kazemde(npc: Npc) {
        chatNpc(happy, "Can I help you at all?")
        if (choice2("Yes please. What are you selling?", true, "No thanks.", false)) {
            player.openStore(npc)
        } else {
            chatPlayer(neutral, "No thanks.")
        }
    }

    private companion object {
        const val KAZEMDE = "npc.elid_general_seller"
    }
}
