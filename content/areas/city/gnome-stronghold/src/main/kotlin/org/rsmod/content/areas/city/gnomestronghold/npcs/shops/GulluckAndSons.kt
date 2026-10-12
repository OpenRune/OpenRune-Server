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

// https://oldschool.runescape.wiki/w/Transcript:Gulluck
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:gnome_restaurant] delivery hand-in dialogue
class GulluckAndSons @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(GULLUCK) { startDialogue(it.npc) { gulluck(it.npc) } }
        onOpNpc3(GULLUCK) { player.openShop(it.npc) }
    }

    private fun Player.openShop(npc: Npc) {
        shops.open(this, npc, "Gulluck and Sons", "inv.gnomeshop_gulluck")
    }

    private suspend fun Dialogue.gulluck(npc: Npc) {
        chatPlayer(happy, "Hello.")
        chatNpc(
            happy,
            "Good day brave adventurer! Could I interest you in my fine selection of weapons?",
        )
        if (choice2("I'll take a look.", true, "No thanks.", false)) {
            chatPlayer(happy, "I'll take a look.")
            player.openShop(npc)
            return
        }
        chatPlayer(neutral, "No thanks.")
        chatNpc(angry, "Grrrr!")
    }

    private companion object {
        const val GULLUCK = "npc.gnome_gulluck"
    }
}
