package org.rsmod.content.interfaces.settings.scripts

import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.table.SettingsConfigsRow
import org.rsmod.game.entity.Player

object SettingUtils {

    const val MAX_RGB_COLOUR: Int = 0x00ffffff

    fun setNumber(player: Player, value : Int,setting: Setting): Boolean {
        val row = SettingsConfigsRow.all().find { it.settingId == setting.id }
        val min = row?.min?:0
        val max = row?.max?:Int.MAX_VALUE

        val normalized = value.coerceIn(min, max)

        row?.varValue?.let { VarPlayerIntMapSetter.set(player, it, normalized) }
        row?.enableToggle?.varValue?.let {
            VarPlayerIntMapSetter.set(player, it, if (normalized > 0) 1 else 0)
        }
        return true
    }

    fun setDropdown(player: Player, dropdownOption: Int, setting: Setting): Boolean {

        val allowedOptions = setting.dropdownEntries?.keys.orEmpty()
        if (dropdownOption !in allowedOptions) {
            println("Invalid dropdown option=$dropdownOption for setting id=${setting.id}, ${setting.dropdownEntries}")
            return false
        }

        if (setting.type == SettingType.KEYBIND) {
            val keybinds = Settings.settingsByType[SettingType.KEYBIND] ?: return false
            for (keybind in keybinds) {
                if (keybind == setting) continue
                val keybindValue = keybind.row?.varValue ?: continue

                if (player.vars[keybindValue] == dropdownOption) {
                    VarPlayerIntMapSetter.set(player, keybindValue, 0)
                }
            }
        }
        val varValue = setting.row?.varValue ?: return false
        VarPlayerIntMapSetter.set(player, varValue, dropdownOption)
        return true
    }

    fun setColour(player: Player, colour: Int, setting: Setting): Boolean {
        val varValue = setting.row?.varValue ?: return false
        VarPlayerIntMapSetter.set(player, varValue, colour.coerceIn(0, MAX_RGB_COLOUR) + 1)
        player.runClientScript(101, 0)
        return true
    }

}
