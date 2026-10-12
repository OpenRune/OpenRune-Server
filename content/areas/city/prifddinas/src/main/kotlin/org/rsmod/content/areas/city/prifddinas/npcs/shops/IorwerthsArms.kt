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

// https://oldschool.runescape.wiki/w/Transcript:Celyn
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class IorwerthsArms @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.prif_weaponstore") { startDialogue(it.npc) { celyn(it.npc) } }
        onOpNpc3("npc.prif_weaponstore") { player.openArms(it.npc) }
    }

    private fun Player.openArms(npc: Npc) {
        shops.open(this, npc, "Iorwerth's Arms", "inv.prif_weapon_store")
    }

    private suspend fun Dialogue.celyn(npc: Npc) {
        chatNpc(
            happy,
            "Looking for a weapon? We've got the finest you could ever dream of here.",
        )
        when (choice2("Yes, please.", 1, "No thanks.", 2)) {
            1 -> {
                chatPlayer(happy, "Yes, please.")
                player.openArms(npc)
            }
            2 -> {
                chatPlayer(neutral, "No thanks.")
                chatNpc(angry, "Hmph. You probably wouldn't know how to use them, anyway.")
            }
        }
    }
}
