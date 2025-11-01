package org.alter.plugins.content.interfaces.keybind

import org.alter.api.ext.getVarbit
import org.alter.api.ext.setVarbit
import org.alter.game.model.entity.Player

/**
 * @author Tom <rspsmods@gmail.com>
 */
object KeyBinding {
    const val FOCUSED_HOTKEY_VARBIT = "varbits.keybinding_stone_selection"
    const val ESC_CLOSES_INTERFACES = "varbits.keybinding_esc_to_close"

    private fun disableAny(
        p: Player,
        hotkeyValue: Int,
    ) {
        Hotkey.values.forEach { hotkey ->
            if (p.getVarbit(hotkey.varbit) == hotkeyValue) {
                p.setVarbit(hotkey.varbit, 0)
            }
        }
    }

    fun set(
        p: Player,
        hotkey: Hotkey,
        hotkeyValue: Int,
    ) {
        disableAny(p, hotkeyValue)
        p.setVarbit(hotkey.varbit, hotkeyValue)
    }
}
