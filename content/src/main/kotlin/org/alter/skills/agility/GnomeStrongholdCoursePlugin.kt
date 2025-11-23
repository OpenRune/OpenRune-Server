package org.alter.skills.agility

import org.alter.api.Skills
import org.alter.api.ext.filterableMessage
import org.alter.game.model.Direction
import org.alter.game.model.ForcedMovement
import org.alter.game.model.Tile
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onObjectOption
import org.alter.rscm.RSCM

class GnomeStrongholdCoursePlugin : PluginEvent() {

    override fun init() {

        // LOG
        onObjectOption("objects.gnome_log_balance1", "walk-across") {
            handleObstacle(
                player = player,
                destination = Tile(2474, 3429, 0),
                anim = "sequences.human_walk_logbalance_loop",
                angle = Direction.SOUTH.angle,
                duration1 = 5,
                duration2 = 250,
                xp = 7.5,
                messageStart = "You walk carefully across the slippery log...",
                messageEnd = "... and make it safely to the other side."
            )
        }

        // NET
        onObjectOption("objects.obstical_net2", "climb-over") {
            if (player.tile.x !in 2471..2476) {
                player.filterableMessage("You can't climb the net from this side.")
                return@onObjectOption
            }

            handleObstacle(
                player = player,
                destination = Tile(player.tile.x, 3424, 1),
                anim = "sequences.human_climbing",
                angle = Direction.SOUTH.angle,
                duration1 = 33,
                duration2 = 60,
                xp = 10.0,
                messageStart = "You climb up the net..."
            )
        }
    }

    private fun handleObstacle(
        player: Player,
        destination: Tile,
        anim: String,
        angle: Int,
        duration1: Int,
        duration2: Int,
        xp: Double,
        messageStart: String? = null,
        messageEnd: String? = null
    ) {
        val movement = ForcedMovement.of(
            src = player.tile,
            dst = destination,
            clientDuration1 = duration1,
            clientDuration2 = duration2,
            directionAngle = angle
        )

        player.queue {
            messageStart?.let { player.filterableMessage(it) }
            wait(1)
            player.animate(anim)
            player.forceMove(this, movement)
            player.animate(RSCM.NONE)
            wait(1)

            player.addXp(Skills.AGILITY, xp)
            messageEnd?.let { player.filterableMessage(it) }
        }
    }
}
