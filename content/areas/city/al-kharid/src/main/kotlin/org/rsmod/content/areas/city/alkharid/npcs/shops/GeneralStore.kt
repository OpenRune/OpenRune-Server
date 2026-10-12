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

// https://oldschool.runescape.wiki/w/Transcript:Shop_keeper_(Al_Kharid)
// https://oldschool.runescape.wiki/w/Transcript:Shop_assistant_(Al_Kharid)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:family_crest] shop keeper "I'm in search of a man named Avan Fitzharmon."
// TODO(content): [miniquest:rogue_trader] "What do you think of Ali Morrisane?"
class GeneralStore @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        for (npc in listOf("npc.generalshopkeeper3", "npc.generalassistant3")) {
            onOpNpc1(npc) { shopDialogue(it.npc) }
            onOpNpc3(npc) { player.openGeneralStore(it.npc) }
        }
    }

    private fun Player.openGeneralStore(npc: Npc) {
        shops.open(this, npc, "Al Kharid General Store", "inv.generalshop1")
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
