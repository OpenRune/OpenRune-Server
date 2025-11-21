package org.alter.objects.ditch

import org.alter.api.ext.getInteractingGameObj
import org.alter.game.model.*
import org.alter.game.model.entity.*
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onObjectOption

class WildernessDitchPlugin : PluginEvent() {

    override fun init() {
        onObjectOption("objects.ditch_wilderness_cover", "cross") {
            val ditch = player.getInteractingGameObj()
            val (endTile, angle) = computeDitchCrossTarget(player, ditch)

            val movement = ForcedMovement.of(
                src = player.tile,
                dst = endTile,
                clientDuration1 = 33,
                clientDuration2 = 60,
                directionAngle = angle
            )
            player.crossDitch(movement)
        }
    }

    private fun computeDitchCrossTarget(player: Player, ditch: GameObject): Pair<Tile, Int> {
        val sameRow = player.tile.z == ditch.tile.z

        return if (sameRow) {
            val isWest = player.tile.x < ditch.tile.x
            if (isWest) {
                ditch.tile.step(Direction.EAST, 2) to Direction.EAST.angle
            } else {
                ditch.tile.step(Direction.WEST, 1) to Direction.WEST.angle
            }
        } else {
            val isSouth = player.tile.z < ditch.tile.z
            if (isSouth) {
                ditch.tile.step(Direction.NORTH, 2) to Direction.NORTH.angle
            } else {
                ditch.tile.step(Direction.SOUTH, 1) to Direction.SOUTH.angle
            }
        }
    }

    private fun Player.crossDitch(movement: ForcedMovement) {
        queue {
            playSound(2452)
            animate("sequences.wild_ditch_jump")
            forceMove(this, movement)
        }
    }
}