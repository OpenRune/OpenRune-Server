package org.rsmod.content.areas.city.edgeville.npcs.shops

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

// https://oldschool.runescape.wiki/w/Transcript:Shop_keeper_(Edgeville)
// https://oldschool.runescape.wiki/w/Transcript:Shop_assistant_(Edgeville)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class GeneralStore @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        for (npc in listOf("npc.generalshopkeeper5", "npc.generalassistant5")) {
            onOpNpc1(npc) { shopDialogue(it.npc) }
            onOpNpc3(npc) { player.openGeneralStore(it.npc) }
        }
    }

    private fun Player.openGeneralStore(npc: Npc) {
        shops.open(this, npc, "Edgeville General Store", "inv.generalshop2")
    }

    private suspend fun ProtectedAccess.shopDialogue(npc: Npc) =
        startDialogue(npc) { shopKeeper(npc) }

    private suspend fun Dialogue.shopKeeper(npc: Npc) {
        chatNpc(happy, "Can I help you at all?")
        when (choice2("Yes please. What are you selling?", 1, "No thanks.", 2)) {
            1 -> player.openGeneralStore(npc)
            2 -> chatPlayer(neutral, "No thanks.")
        }
    }
}
