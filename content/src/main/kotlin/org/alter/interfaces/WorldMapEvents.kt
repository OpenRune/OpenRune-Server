package org.alter.interfaces

import dev.openrune.ServerCacheManager
import org.alter.api.cfg.Sound
import org.alter.api.ext.sendWorldMapTile
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
import org.alter.interfaces.gameframe.GameFrameEvents
import org.alter.interfaces.gameframe.GameFrameEvents.Companion.queueGameframeMove
import org.alter.rscm.RSCM.asRSCM

class WorldMapEvents : PluginEvent() {

    companion object {
        val UPDATE_TIMER = TimerKey()
        val LAST_TILE = AttributeKey<Tile>()
        val FULLSCREEN_MINIMAP = AttributeKey<Boolean>()
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
                val player = player as Player
                if (player.ui.containsOverlay(ServerCacheManager.fromInterface("interfaces.worldmap"))) {
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
            where { component.combinedId == "components.worldmap:close".asRSCM() }
            then {
                if (player.attr[FULLSCREEN_MINIMAP] == true) {
                    player.queueGameframeMove(GameFrameEvents.gameframes[player.gameframeTopLevelLastKnown]!!)
                    player.attr[FULLSCREEN_MINIMAP] = false
                }
                player.ifCloseOverlay("interfaces.worldmap")
                player.attr.remove(LAST_TILE)
                player.timers.remove(UPDATE_TIMER)
            }
        }


        on<ButtonClickEvent> {
            where { component.combinedId == "components.orbs:worldmap".asRSCM() }
            then {
                if (!player.lock.canInterfaceInteract()) {
                    return@then
                }
                if (!player.ui.containsOverlay(ServerCacheManager.fromInterface("interfaces.worldmap"))) {
                    player.sendWorldMapTile()
                    player.playSound(Sound.INTERFACE_SELECT1, 100)
                    when(option) {
                        2 -> player.ifOpenOverlay("interfaces.worldmap")
                        3 -> {
                            player.queue {
                                player.animate("sequences.qip_watchtower_read_scroll")
                                wait(1)
                                player.queueGameframeMove(GameFrameEvents.gameframes["interfaces.toplevel_display"]!!)
                                player.ifOpenOverlay("interfaces.worldmap")
                                player.attr[FULLSCREEN_MINIMAP] = true
                                player.animate("sequences.qip_watchtower_stop_reading_scroll")
                            }
                        }
                        4 -> player.toggleVarbit("varbits.minimap_toggle")
                    }
                } else {
                    player.ifCloseOverlay("interfaces.worldmap")
                    player.attr.remove(LAST_TILE)
                    player.timers.remove(UPDATE_TIMER)
                }
                player.timers[UPDATE_TIMER] = 1
            }
        }

    }

}