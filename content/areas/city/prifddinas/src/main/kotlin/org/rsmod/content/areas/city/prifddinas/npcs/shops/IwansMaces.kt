package org.rsmod.content.areas.city.prifddinas.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Iwan
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class IwansMaces @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.prif_macestore") { startDialogue(it.npc) { iwan(it.npc) } }
        onOpNpc3("npc.prif_macestore") { player.openMaces(it.npc) }
    }

    private fun Player.openMaces(npc: Npc) {
        shops.open(this, npc, "Iwan's Maces", "inv.prif_mace_store")
    }

    private suspend fun Dialogue.iwan(npc: Npc) {
        val noun = if (access.isBodyTypeA()) "man" else "woman"
        chatNpc(
            happy,
            "You look like a $noun that could use a mace. Finest weapons around! Want to take a " +
                "look?",
        )
        when (choice2("Yes, please.", 1, "No thanks.", 2)) {
            1 -> {
                chatPlayer(happy, "Yes, please.")
                player.openMaces(npc)
            }
            2 -> {
                chatPlayer(neutral, "No thanks.")
                chatNpc(neutral, "Your loss, I suppose. You won't find a weapon like a mace!")
            }
        }
    }
}
