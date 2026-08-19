package org.rsmod.content.interfaces.xpdrops

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class XpDropsToggleScript @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButton("component.orbs:xp_drops") { toggleXpDrops() }
    }

    private fun ProtectedAccess.toggleXpDrops() {
        val enabled = player.xpDropsEnabled
        player.xpDropsEnabled = !enabled
        if (enabled) {
            ifCloseSub("interface.xp_drops")
        } else {
            ifOpenOverlay("interface.xp_drops", "component.toplevel_osrs_stretch:xp_drops")
        }
        ifOpenOverlay("interface.orbs", "component.toplevel_osrs_stretch:orbs")
    }
}

private var Player.xpDropsEnabled by boolVarBit("varbit.xpdrops_enabled")
