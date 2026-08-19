package org.rsmod.content.interfaces.xpdrops

import jakarta.inject.Inject
import org.rsmod.annotations.InternalApi
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.ifSetHide
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class XpDropsToggleScript @Inject constructor(private val protectedAccess: ProtectedAccessLauncher) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButton("component.orbs:xp_drops") { toggleXpDrops() }
        onPlayerLogin { updateXpState(player) }
    }

    private fun ProtectedAccess.toggleXpDrops() {
        val enabled = player.xpDropsEnabled
        player.xpDropsEnabled = !enabled
        updateXpState(player)
    }

    @OptIn(InternalApi::class)
    fun updateXpState(player: Player) {
        protectedAccess.launchLenient(player) {
            if (!player.xpDropsEnabled) {
                ifCloseSub("interface.xp_drops")
            } else {
                ifOpenOverlay("interface.xp_drops", "component.toplevel_osrs_stretch:xp_drops")
            }
        }
    }
}

private var Player.xpDropsEnabled by boolVarBit("varbit.xpdrops_enabled")
