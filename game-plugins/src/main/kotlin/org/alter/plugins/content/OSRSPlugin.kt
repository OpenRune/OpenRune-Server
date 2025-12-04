package org.alter.plugins.content

import org.alter.api.Skills
import org.alter.api.ext.InterfaceEvent
import org.alter.api.ext.calculateAndSetCombatLevel
import org.alter.api.ext.closeInterface
import org.alter.api.ext.player
import org.alter.api.ext.sendCombatLevelText
import org.alter.api.ext.sendWeaponComponentInformation
import org.alter.api.ext.setInterfaceEvents
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
        /**
         * Closing main modal for players.
         */
        setModalCloseLogic {
            val modal = player.interfaces.getModal()
            if (modal != -1) {
                player.closeInterface(modal)
                player.interfaces.setModal(-1)
            }
        }

        onLogin {
            with(player) {
                if (getSkills().getBaseLevel(Skills.HITPOINTS) < 10) {
                    getSkills().setBaseLevel(Skills.HITPOINTS, 10)
                }

                calculateAndSetCombatLevel()
                sendWeaponComponentInformation()
                sendCombatLevelText()
                setVarbit("varbits.combatlevel_transmit", combatLevel)
                setInterfaceEvents(
                    interfaceId = 149,
                    component = 0,
                    range = 0..27,
                    setting =
                        arrayOf(
                            InterfaceEvent.ClickOp2,
                            InterfaceEvent.ClickOp3,
                            InterfaceEvent.ClickOp4,
                            InterfaceEvent.ClickOp6,
                            InterfaceEvent.ClickOp7,
                            InterfaceEvent.ClickOp10,
                            InterfaceEvent.UseOnGroundItem,
                            InterfaceEvent.UseOnNpc,
                            InterfaceEvent.UseOnObject,
                            InterfaceEvent.UseOnPlayer,
                            InterfaceEvent.UseOnInventory,
                            InterfaceEvent.UseOnComponent,
                            InterfaceEvent.DRAG_DEPTH1,
                            InterfaceEvent.DragTargetable,
                            InterfaceEvent.ComponentTargetable,
                        ),
                )
            }
        }


    }

}
