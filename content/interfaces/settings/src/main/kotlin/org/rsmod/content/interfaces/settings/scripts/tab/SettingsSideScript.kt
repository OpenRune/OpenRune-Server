package org.rsmod.content.interfaces.settings.scripts.tab

import dev.openrune.definition.type.widget.IfEvent
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.enumVarBit
import org.rsmod.api.script.onIfOpen
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.utils.vars.VarEnumDelegate
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SettingsSideScript @Inject constructor(private val protectedAccess: ProtectedAccessLauncher) :
    PluginScript() {
    private var Player.panel by enumVarBit<Panel>("varbit.settings_side_panel_tab")

    override fun ScriptContext.startup() {
        onIfOpen("interface.settings_side") { player.updateIfEvents() }

        onIfOverlayButton("component.settings_side:settings_tab") { player.panel = Panel.Control }
        onIfOverlayButton("component.settings_side:audio_tab") { player.panel = Panel.Audio }
        onIfOverlayButton("component.settings_side:display_tab") { player.panel = Panel.Display }
    }

    private fun Player.updateIfEvents() {
        ifSetEvents("component.settings_side:music_bobble_container", 0..21, IfEvent.Op1)
        ifSetEvents("component.settings_side:sound_bobble_container", 0..21, IfEvent.Op1)
        ifSetEvents("component.settings_side:areasounds_bobble_container", 0..21, IfEvent.Op1)
        ifSetEvents("component.settings_side:master_bobble_container", 0..21, IfEvent.Op1)
        ifSetEvents("component.settings_side:attack_priority_player_buttons", 1..5, IfEvent.Op1)
        ifSetEvents("component.settings_side:attack_priority_npc_buttons", 1..4, IfEvent.Op1)
        ifSetEvents("component.settings_side:display_dynamic_setting_1_buttons", 1..3, IfEvent.Op1)
        ifSetEvents("component.settings_side:brightness_bobble_container", 0..21, IfEvent.Op1)
    }
}

private enum class Panel(override val varValue: Int) : VarEnumDelegate {
    Control(0),
    Audio(1),
    Display(2),
}
