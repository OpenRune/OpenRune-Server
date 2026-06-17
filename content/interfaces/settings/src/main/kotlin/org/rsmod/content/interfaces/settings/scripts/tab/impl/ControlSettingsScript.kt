package org.rsmod.content.interfaces.settings.scripts.tab.impl

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.table.SettingsConfigsRow
import org.rsmod.content.interfaces.settings.scripts.SettingUtils
import org.rsmod.content.interfaces.settings.scripts.Settings
import org.rsmod.content.interfaces.settings.scripts.varValue
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ControlSettingsScript
@Inject
constructor(private val protectedAccess: ProtectedAccessLauncher) : PluginScript() {

    override fun ScriptContext.startup() {
        onIfOverlayButton("component.settings_side:skull_prevention") { player.toggleSkullPrevention() }

        onIfOverlayButton("component.settings_side:attack_priority_player_buttons") {
            player.selectPlayerPriority(it.comsub)
        }

        onIfOverlayButton("component.settings_side:attack_priority_npc_buttons") {
            player.selectNpcPriority(it.comsub)
        }

        onIfOverlayButton("component.settings_side:acceptaid") { player.toggleAcceptAid() }
        onIfOverlayButton("component.settings_side:houseoptions") { player.selectHouseOptions() }
        onIfOverlayButton("component.settings_side:bondoptions") { player.selectBondPouch() }
    }

    private fun Player.toggleSkullPrevention() {
        val row = SettingsConfigsRow.all().find { it.settingId == 206 }
        row?.varValue?.let {
            VarPlayerIntMapSetter.toggle(this, it)
        }
    }

    private fun Player.selectPlayerPriority(comsub: Int) {
        val setting = Settings.getSetting(55)
        SettingUtils.setDropdown(this, comsub, setting)
    }

    private fun Player.selectNpcPriority(comsub: Int) {
        val setting = Settings.getSetting(56)
        SettingUtils.setDropdown(this, comsub, setting)
    }

    private fun Player.toggleAcceptAid() {
        val row = SettingsConfigsRow.all().find { it.settingId == 59 }
        row?.varValue?.let {
            VarPlayerIntMapSetter.toggle(this, it)
        }
    }

    private fun Player.selectHouseOptions() {
        protectedAccess.launch(this) { ifOpenSide("interface.poh_options") }
    }

    private fun Player.selectBondPouch() {
        val opened = protectedAccess.launch(this) { ifOpenMainModal("interface.bond_main", -1, -2) }
        if (!opened) {
            mes(constants.dm_busy)
        }
    }
}
