package org.alter.objects

import org.alter.game.model.move.moveTo
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onObjectOption

class LumbridgeStairsPlugin : PluginEvent() {

    override fun init() {
        onObjectOption("objects.spiralstairsbottom_3", "climb-up") {
            player.queue {
                player.moveTo(player.tile.x, player.tile.z, player.tile.height + 1)
            }
        }

        onObjectOption("objects.spiralstairsbottom_3", "top-floor") {
            player.queue {
                player.moveTo(player.tile.x, player.tile.z, 2)
            }
        }

        onObjectOption("objects.spiralstairstop_3", "climb-down") {
            player.queue {
                player.moveTo(player.tile.x, player.tile.z, player.tile.height - 1)
            }
        }

        onObjectOption("objects.spiralstairstop_3", "bottom-floor") {
            player.queue {
                player.moveTo(player.tile.x, player.tile.z, 0)
            }
        }
    }
}

