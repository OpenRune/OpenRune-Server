package org.rsmod.content.areas.city.lumbridge.npcs

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** The retired Deadman hitpoints insurance broker in the Lumbridge graveyard. */
class Gelin : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.deadman_gelin") { startDialogue(it.npc) }
    }

    private suspend fun ProtectedAccess.startDialogue(npc: Npc) {
        startDialogue(npc) {
            chatNpc(
                neutral,
                "I used to sell life insurance, protecting people's Hitpoints XP when they " +
                    "died. Now there's no more need for that, so I'm just giving refunds now. " +
                    "But I don't think I owe you anything.",
            )
        }
    }
}
