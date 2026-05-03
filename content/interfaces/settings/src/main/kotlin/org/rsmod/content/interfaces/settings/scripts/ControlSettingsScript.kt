package org.rsmod.content.interfaces.settings.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.enumVarp
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.utils.vars.VarEnumDelegate
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ControlSettingsScript
@Inject
constructor(private val protectedAccess: ProtectedAccessLauncher) : PluginScript() {
    private var Player.acceptAid by boolVarBit("varbit.option_acceptaid")
    private var Player.skullPrevention by boolVarBit("varbit.skull_prevent_enabled")
    private var Player.priorityPlayer by enumVarp<PlayerPriority>("varp.option_attackpriority")
    private var Player.priorityNpc by enumVarp<NpcPriority>("varp.option_attackpriority_npc")

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

private enum class PlayerPriority(override val varValue: Int) : VarEnumDelegate {
    CombatLevel(0),
    RightClickAlways(1),
    LeftClick(2),
    Hidden(3),
    RightClickClan(4),
}

private enum class NpcPriority(override val varValue: Int) : VarEnumDelegate {
    CombatLevel(0),
    RightClickAlways(1),
    LeftClick(2),
    Hidden(3),
}
