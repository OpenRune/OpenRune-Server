package org.alter.objects.ladder

import org.alter.api.ext.*
import org.alter.game.model.entity.Player
import org.alter.game.model.move.moveTo
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onObjectOption

class LadderPlugin : PluginEvent() {

    override fun init() {
        registerStairs()
        registerLadders()
        registerTrapdoors()
    }

    private fun registerStairs() {
        val stairs = arrayOf(
            "objects.spiralstairsmiddle",
            "objects.spiralstairstop",
            "objects.spiralstairs",
        )

        stairs.forEach { id ->
            onObjectOption(id, "climb") { stairsMenu(player) }
            onObjectOption(id, "climb-up") { climbUpStairs(player) }
            onObjectOption(id, "climb-down") { climbDownStairs(player) }
        }
    }

    private fun registerLadders() {
        val ladders = arrayOf(
            "objects.qip_cook_ladder",
            "objects.qip_cook_ladder_middle",
            "objects.ladder",
            "objects.qip_cook_ladder_top",
            "objects.laddertop",
            "objects.laddermiddle",
        )

        ladders.forEach { id ->
            onObjectOption(id, "climb") { ladderMenu(player) }
            onObjectOption(id, "climb-up") { climbUpLadder(player) }
            onObjectOption(id, "climb-down") { climbDownLadder(player) }
        }
    }

    private fun registerTrapdoors() {
        onObjectOption("objects.qip_cook_trapdoor_open", "climb-down") {
            player.moveTo(3210, 9616, 0)
        }
        onObjectOption("objects.ladder_from_cellar", "climb-up") {
            player.moveTo(3210, 3216, 0)
        }
    }

    private fun climbUpLadder(player: Player) =
        climbLadder(player, +1)

    private fun climbDownLadder(player: Player) =
        climbLadder(player, -1)

    private fun climbLadder(player: Player, deltaZ: Int) {
        player.queue {
            player.animate("sequences.human_reachforladder")
            player.lock()
            wait(2)
            player.moveTo(player.tile.x, player.tile.z, player.tile.height + deltaZ)
            player.unlock()
        }
    }

    private fun ladderMenu(player: Player) {
        player.queue {
            when (options(player, "Climb up the ladder.", "Climb down the ladder")) {
                1 -> climbUpLadder(player)
                2 -> climbDownLadder(player)
            }
        }
    }

    private fun climbUpStairs(player: Player) =
        player.moveTo(player.tile.x, player.tile.z, player.tile.height + 1)

    private fun climbDownStairs(player: Player) =
        player.moveTo(player.tile.x, player.tile.z, player.tile.height - 1)

    private fun stairsMenu(player: Player) {
        player.queue {
            when (options(player, "Climb up the stairs.", "Climb down the stairs.")) {
                1 -> climbUpStairs(player)
                2 -> climbDownStairs(player)
            }
        }
    }
}
