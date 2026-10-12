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

// https://oldschool.runescape.wiki/w/Transcript:Ranael
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:treasure_trails] clue reward
class SkirtShop @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.ranael") { shopDialogue(it.npc) }
        onOpNpc3("npc.ranael") { player.openSkirtShop(it.npc) }
    }

    private fun Player.openSkirtShop(npc: Npc) {
        shops.open(this, npc, "Ranael's Super Skirt Store.", "inv.skirtshop")
    }

    private suspend fun ProtectedAccess.shopDialogue(npc: Npc) =
        startDialogue(npc) { shopKeeper(npc) }

    private suspend fun Dialogue.shopKeeper(npc: Npc) {
        chatNpc(
            happy,
            "Do you want to buy any armoured skirts? Designed especially for ladies who like " +
                "to fight.",
        )
        when (choice2("Yes please.", 1, "No thank you, that's not my scene.", 2)) {
            1 -> player.openSkirtShop(npc)
            2 -> chatPlayer(neutral, "No thank you, that's not my scene.")
        }
    }
}
