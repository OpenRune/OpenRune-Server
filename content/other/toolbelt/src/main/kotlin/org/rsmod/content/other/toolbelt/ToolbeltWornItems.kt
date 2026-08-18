package org.rsmod.content.other.toolbelt

import org.rsmod.api.player.ui.ifSetHide
import org.rsmod.content.other.toolbelt.pack.WORN_FOOTER_TOOLBELT_ROW
import org.rsmod.content.other.toolbelt.pack.WORN_FOOTER_VANILLA
import org.rsmod.game.entity.Player

object ToolbeltWornItems {
    fun sync(player: Player) {
        val enabled = Toolbelt.isEnabled()
        for (name in WORN_FOOTER_VANILLA) {
            player.ifSetHide(name, hide = enabled)
        }
        for (name in WORN_FOOTER_TOOLBELT_ROW) {
            player.ifSetHide(name, hide = !enabled)
        }
    }
}
