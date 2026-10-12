package org.rsmod.content.areas.city.gnomestronghold.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Heckel_Funch
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:gnome_restaurant] delivery hand-in dialogue
// TODO(content): [system:treasure_trails] hard puzzle box step
class FunchsFineGroceries @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(HECKEL_FUNCH) { startDialogue(it.npc) { heckelFunch(it.npc) } }
        onOpNpc3(HECKEL_FUNCH) { player.openShop(it.npc) }
    }

    private fun Player.openShop(npc: Npc) {
        shops.open(this, npc, "Funch's Fine Groceries", "inv.gnomeshop_heck")
    }

    private suspend fun Dialogue.heckelFunch(npc: Npc) {
        chatPlayer(happy, "Hello there.")
        chatNpc(
            happy,
            "Good day to you my friend, a beautiful one at that. Would you like some groceries? " +
                "I have all sorts. Alcohol also, if you're partial to a drink.",
        )
        if (choice2("No thank you.", false, "I'll have a look.", true)) {
            chatPlayer(happy, "I'll have a look.")
            chatNpc(happy, "There's a good human.")
            player.openShop(npc)
            return
        }
        chatPlayer(neutral, "No thank you.")
        chatNpc(happy, "Ahh well, all the best to you.")
    }

    private companion object {
        const val HECKEL_FUNCH = "npc.gnome_heckelfunch"
    }
}
