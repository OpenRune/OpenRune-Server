package org.alter.interfaces

import org.alter.api.InterfaceDestination
import org.alter.api.cfg.Sound
import org.alter.api.ext.InterfaceEvent
import org.alter.api.ext.closeInterface
import org.alter.api.ext.isInterfaceVisible
import org.alter.api.ext.message
import org.alter.api.ext.openInterface
import org.alter.api.ext.openOverlayInterface
import org.alter.api.ext.playSound
import org.alter.api.ext.sendWorldMapTile
import org.alter.api.ext.setInterfaceEvents
import org.alter.api.ext.toggleVarbit
import org.alter.game.model.Tile
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.entity.Player
import org.alter.game.model.move.moveTo
import org.alter.game.model.priv.Privilege
import org.alter.game.model.timer.TimerKey
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ButtonClickEvent
import org.alter.game.pluginnew.event.impl.ClickWorldMapEvent
import org.alter.game.pluginnew.event.impl.TimerEvent
import org.alter.rscm.RSCM.asRSCM

class WorldMapEvents : PluginEvent() {

    companion object {
        val UPDATE_TIMER = TimerKey()
        val LAST_TILE = AttributeKey<Tile>()

        val WORLD_MAP_INTERFACE_ID = "interfaces.worldmap".asRSCM()
    }


    override fun init() {

        on<ClickWorldMapEvent> {
            where { player.world.privileges.isEligible(player.privilege, Privilege.ADMIN_POWER) }
            then {
                player.moveTo(tile)
            }
        }

        on<TimerEvent> {
            where { timer == UPDATE_TIMER }
            then {
                val player = pawn as Player
                if (player.isInterfaceVisible(WORLD_MAP_INTERFACE_ID)) {
                    val lastTile = player.attr[LAST_TILE]
                    if (lastTile == null || !lastTile.sameAs(player.tile)) {
                        player.sendWorldMapTile()
                        player.attr[LAST_TILE] = player.tile
                    }

                    player.timers[UPDATE_TIMER] = 1
                }
            }
        }

        on<ButtonClickEvent> {
            where { component.combinedId == "components.worldmap:universe".asRSCM() }
            then {
                player.closeInterface(WORLD_MAP_INTERFACE_ID)
                player.openOverlayInterface(player.interfaces.displayMode)
                player.attr.remove(LAST_TILE)
                player.timers.remove(UPDATE_TIMER)
            }
        }


        on<ButtonClickEvent> {
            where { component.combinedId == "components.orbs:store_icon".asRSCM() }
            then {
                if (!player.lock.canInterfaceInteract()) {
                    return@then
                }
                if (!player.isInterfaceVisible(WORLD_MAP_INTERFACE_ID)) {
                    player.sendWorldMapTile()
                    player.playSound(Sound.INTERFACE_SELECT1, 100)
                    when(option) {
                        2 -> {
                            player.openInterface(interfaceId = WORLD_MAP_INTERFACE_ID, dest = InterfaceDestination.WORLD_MAP, fullscreen = false)
                            player.setInterfaceEvents(interfaceId = WORLD_MAP_INTERFACE_ID, component = 21, range = 0..4, setting = InterfaceEvent.ClickOp1)
                        }
                        3 -> {
                            player.queue {
                                player.animate("sequences.qip_watchtower_read_scroll".asRSCM())
                                wait(1)
                                player.message("Fullscreen minimap was temporarily disabled.")
                                player.animate("sequences.qip_watchtower_stop_reading_scroll".asRSCM())
                            }
                        }
                        4 -> player.toggleVarbit("varbits.minimap_toggle".asRSCM())
                    }
                }
                player.timers[UPDATE_TIMER] = 1
            }
        }

    }

}