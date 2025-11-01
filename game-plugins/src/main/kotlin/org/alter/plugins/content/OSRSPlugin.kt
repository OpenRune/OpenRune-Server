package org.alter.plugins.content

import org.alter.api.*
import org.alter.api.CommonClientScripts
import org.alter.api.InterfaceDestination
import org.alter.api.ext.*
import org.alter.game.*
import org.alter.game.model.*
import org.alter.game.model.entity.*
import org.alter.game.plugin.*

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
        /**
         * Check if the player has a menu opened.
         */
        setMenuOpenCheck {
            player.getInterfaceAt(dest = InterfaceDestination.MAIN_SCREEN) != -1
        }

        /**
         * Execute when a player logs in.
         */
        onLogin {
            with(player) {
                /**
                 * @TODO Inspect, uhh seems that this logic is being repeated, not removing it yet as im unsure rn if it's needed or not
                 */
                // Skill-related logic.
                calculateAndSetCombatLevel()
                if (getSkills().getBaseLevel(Skills.HITPOINTS) < 10) {
                    getSkills().setBaseLevel(Skills.HITPOINTS, 10)
                }
                calculateAndSetCombatLevel()
                sendWeaponComponentInformation()
                sendCombatLevelText()
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
                player.openDefaultInterfaces()
                setVarbit("varbits.combatlevel_transmit", combatLevel)
                setVarbit("varbits.has_displayname_transmitter", 1)
                runClientScript(CommonClientScripts.INTRO_MUSIC_RESTORE)
                if (getVarp("varp.chat_filter_assist") == 0 && username.isNotBlank()) {
                    syncVarp("varp.chat_filter_assist")
                }
                // Sync attack priority options.
                syncVarp("varp.option_attackpriority_npc")
                syncVarp("varp.option_attackpriority")
                // Send player interaction options.
                sendOption("Follow", 3)
                sendOption("Trade with", 4)
                sendOption("Report", 5)
                // Game-related logic.
                sendRunEnergy(player.runEnergy.toInt())
                message("Welcome to ${world.gameContext.name}.", ChatMessageType.GAME_MESSAGE)
                // player.social.pushFriends(player)
                // player.social.pushIgnores(player)
                setVarbit("varbits.keybinding_esc_to_close", 1)

                /**
                 * @TODO
                 * As for now these varbit's disable Black bar on right side for Native client,
                 * The black bar is for loot tracker n whatnot
                 */
                setVarbit("varbits.popout_panel_desktop_disabled", 1)
                setVarbit("varbits.popout_panel_mobile_enabled", 1)
            }
        }



        // TODO Whats this for:?
        onButton(245, 20) {
            player.openInterface(interfaceId = 626, dest = InterfaceDestination.MAIN_SCREEN)
        }
    }

    fun Player.openDefaultInterfaces() {
        openOverlayInterface(interfaces.displayMode)
        openModals(this)
        setInterfaceEvents(interfaceId = 239, component = 3, range = 0..665, setting = 6) // enable music buttons
        initInterfaces(interfaces.displayMode)
    }

    fun openModals(
        player: Player,
        fullscreen: Boolean = false,
    ) {
        InterfaceDestination.getModals().forEach { pane ->
            if (pane == InterfaceDestination.XP_COUNTER && player.getVarbit("varbits.xpdrops_enabled") == 0) {
                return@forEach
            } else if (pane == InterfaceDestination.MINI_MAP && player.getVarbit("varbits.orbs_disabled") == 1) {
                return@forEach
            }
            player.openInterface(pane.interfaceId, pane, fullscreen)
        }
    }

}
