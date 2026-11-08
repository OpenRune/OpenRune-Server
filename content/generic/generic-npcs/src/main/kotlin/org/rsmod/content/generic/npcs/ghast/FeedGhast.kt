package org.rsmod.content.generic.npcs.ghast

import dev.openrune.types.ItemServerType
import org.rsmod.api.config.Constants
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpcU
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** A ghast eats purple sweets. */
class FeedGhast : PluginScript() {
    override fun ScriptContext.startup() {
        GHASTS.forEach { ghast -> onOpNpcU(ghast) { feed(it.objType) } }
    }

    private fun ProtectedAccess.feed(obj: ItemServerType) {
        if (obj.internalName !in SWEETS) {
            mes(Constants.dm_default)
            return
        }
        if (invDel(inv, obj.internalName, 1).failure) {
            return
        }
        // UNVERIFIED: the message is not in any capture.
        mes("You feed the ghast the sweets.")
    }

    private companion object {
        val SWEETS = setOf("obj.trail_sweets")

        val GHASTS =
            listOf(
                "npc.ghast_vis",
                "npc.templetrek_ghast_vis_1",
                "npc.templetrek_ghast_vis_2",
                "npc.templetrek_ghast_vis_3",
            )
    }
}
