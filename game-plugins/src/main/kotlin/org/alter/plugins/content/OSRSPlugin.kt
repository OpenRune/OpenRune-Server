package org.alter.plugins.content

import org.alter.api.Skills
import org.alter.api.ext.calculateAndSetCombatLevel
import org.alter.api.ext.player
import org.alter.api.ext.sendCombatLevelText
import org.alter.api.ext.sendWeaponComponentInformation
import org.alter.api.ext.setVarbit
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

class OSRSPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    init {

        onLogin {
            with(player) {
                if (getSkills().getBaseLevel(Skills.HITPOINTS) < 10) {
                    getSkills().setBaseLevel(Skills.HITPOINTS, 10)
                }

                calculateAndSetCombatLevel()
                sendWeaponComponentInformation()
                sendCombatLevelText()
                setVarbit("varbits.combatlevel_transmit", combatLevel)
            }
        }


    }

}
