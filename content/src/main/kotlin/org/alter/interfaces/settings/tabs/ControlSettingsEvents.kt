package org.alter.interfaces.settings.tabs

import org.alter.api.ext.boolVarBit
import org.alter.api.ext.enumVarp
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onButton
import org.alter.interfaces.ifOpenMainModal
import org.alter.interfaces.ifOpenSide
import org.alter.interfaces.settings.configs.setting_components


class ControlSettingsEvents : PluginEvent() {

    private var Player.acceptAid by boolVarBit("varbits.option_acceptaid")
    private var Player.skullPrevention by boolVarBit("varbits.skull_prevent_enabled")
    private var Player.priorityPlayer by enumVarp<PlayerPriority>("varp.option_attackpriority")
    private var Player.priorityNpc by enumVarp<NpcPriority>("varp.option_attackpriority_npc")

    override fun init() {
        onButton(setting_components.skull_prevention) { player.toggleSkullPrevention() }

        onButton(setting_components.attack_priority_player_buttons) {
            player.selectPlayerPriority(slot)
        }

        onButton(setting_components.attack_priority_npc_buttons) {
            player.selectNpcPriority(slot)
        }

        onButton(setting_components.acceptaid) { player.toggleAcceptAid() }
        onButton(setting_components.houseoptions) {
            player.ifOpenSide("interfaces.poh_options")
        }
        onButton(setting_components.bondoptions) {
            player.ifOpenMainModal("interfaces.bond_main", -1, -2)
        }
    }


    private fun Player.toggleSkullPrevention() {
        skullPrevention = !skullPrevention
    }

    private fun Player.selectPlayerPriority(comsub: Int) {
        val priority =
            when (comsub) {
                1 -> PlayerPriority.CombatLevel
                2 -> PlayerPriority.RightClickAlways
                3 -> PlayerPriority.LeftClick
                4 -> PlayerPriority.Hidden
                5 -> PlayerPriority.RightClickClan
                else -> error("Invalid comsub: $comsub")
            }
        priorityPlayer = priority
    }

    private fun Player.selectNpcPriority(comsub: Int) {
        val priority =
            when (comsub) {
                1 -> NpcPriority.CombatLevel
                2 -> NpcPriority.RightClickAlways
                3 -> NpcPriority.LeftClick
                4 -> NpcPriority.Hidden
                else -> error("Invalid comsub: $comsub")
            }
        priorityNpc = priority
    }

    private fun Player.toggleAcceptAid() {
        acceptAid = !acceptAid
    }

}

private enum class PlayerPriority(val varValue: Int) {
    CombatLevel(0),
    RightClickAlways(1),
    LeftClick(2),
    Hidden(3),
    RightClickClan(4),
}

private enum class NpcPriority(val varValue: Int) {
    CombatLevel(0),
    RightClickAlways(1),
    LeftClick(2),
    Hidden(3),
}
