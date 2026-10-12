package org.rsmod.content.areas.city.karamja.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Shop_keeper_(Musa_Point)
// https://oldschool.runescape.wiki/w/Transcript:Shop_assistant_(Musa_Point)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [diary:karamja] high alchemy sell prices with Karamja gloves 3 or better
// TODO(content): [verify:in_game] royal_generalstore may be Miscellania's inv (#14)
class GeneralStore @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        for (npc in listOf("npc.generalshopkeeper7", "npc.generalassistant7")) {
            onOpNpc1(npc) { startDialogue(it.npc) { shopKeeper(it.npc) } }
            onOpNpc3(npc) { player.openGeneralStore(it.npc) }
        }
    }

    private fun Player.openGeneralStore(npc: Npc) {
        shops.open(this, npc, "Karamja General Store", "inv.royal_generalstore")
    }

    private suspend fun Dialogue.shopKeeper(npc: Npc) {
        chatNpc(happy, "Can I help you at all?")
        when (choice2("Yes please. What are you selling?", 1, "No thanks.", 2)) {
            1 -> player.openGeneralStore(npc)
            2 -> chatPlayer(neutral, "No thanks.")
        }
    }
}
