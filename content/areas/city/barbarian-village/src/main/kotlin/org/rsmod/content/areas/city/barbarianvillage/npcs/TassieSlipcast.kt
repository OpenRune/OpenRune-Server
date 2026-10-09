package org.rsmod.content.areas.city.barbarianvillage.npcs

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class TassieSlipcast : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.favour_tassie_slipcast") { talk(it.npc) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            chatNpc(
                neutral,
                "Please feel free to use the pottery wheel, I won't be using it all the time. Put " +
                    "your pots in the kiln when you've made one.",
            )
            chatNpc(neutral, "And make sure you tidy up after yourself!")
        }
}
