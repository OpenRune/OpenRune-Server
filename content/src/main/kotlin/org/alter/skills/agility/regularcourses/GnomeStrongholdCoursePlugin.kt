package org.alter.skills.agility.regularcourses

import org.alter.api.ChatMessageType
import org.alter.api.Skills
import org.alter.api.ext.filterableMessage
import org.alter.api.ext.loopAnim
import org.alter.api.ext.message
import org.alter.api.ext.stopLoopAnim
import org.alter.api.ext.stopWalkAnimOverride
import org.alter.api.ext.walkAnimOverride
import org.alter.game.model.Direction
import org.alter.game.model.ForcedMovement
import org.alter.game.model.Tile
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.entity.Player
import org.alter.game.model.move.forceStep
import org.alter.game.model.move.moveTo
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onObjectOption
import org.alter.rscm.RSCM
import org.alter.skills.agility.MarkOfGraceService

class GnomeStrongholdCoursePlugin : PluginEvent() {

    private val MAX_STAGES = 7
    private val BONUS_XP = 50.0
    private val DROP_CHANCE = 1.0 / 3.0 // 1/3 chance per lap completion

    private val MARK_SPAWN_TILES = listOf(
        Tile(2471, 3422, 1),
        Tile(2474, 3418, 2),
        Tile(2488, 3421, 2)
    )
    private val GraceService = MarkOfGraceService(
        spawnTiles = MARK_SPAWN_TILES,
        dropChance = DROP_CHANCE
    )
    val GNOME_AGILITY_STAGE = AttributeKey<Int>("GnomeAgilityStage")
    private fun Player.stage(): Int = attr[GNOME_AGILITY_STAGE] ?: 0
    private fun Player.setStage(v: Int) { attr[GNOME_AGILITY_STAGE] = v }

    val GNOME_LAPS = AttributeKey<Int>("GnomeAgilityLaps")
    private fun Player.laps(): Int = attr[GNOME_LAPS] ?: 0
    private fun Player.setLaps(v: Int) { attr[GNOME_LAPS] = v }


    override fun init() {
        onObjectOption("objects.gnome_log_balance1", "walk-across") {
            val dest = Tile(2474, 3429, 0)

            player.queue {
                player.message(type = ChatMessageType.GAME_MESSAGE, message = "You walk carefully across the slippery log...")
                player.animate("sequences.human_walk_logbalance_ready")
                wait(1)
                player.loopAnim("sequences.human_walk_logbalance_loop")
                val fm = ForcedMovement.of(
                    src = player.tile,
                    dst = dest,
                    clientDuration1 = 5,
                    clientDuration2 = 250,
                    directionAngle = Direction.SOUTH.angle
                )
                player.forceMove(this, fm)
                player.stopLoopAnim()
                wait(1)
                player.animate(RSCM.NONE)
                player.message(type = ChatMessageType.GAME_MESSAGE, message = "... and make it safely to the other side.")
                player.addXp(Skills.AGILITY, 7.5)
                player.setStage(1)
            }
        }
        onObjectOption("objects.obstical_net2", "climb-over") {
            if(player.tile.x !in 2471..2476) return@onObjectOption

            player.queue {
                player.message(type = ChatMessageType.GAME_MESSAGE, message = "You climb up the netting...")
                player.animate("sequences.human_reachforladder")
                wait(2)
                player.moveTo(player.tile.x, 3424, 1)
                wait(1)
                player.animate(RSCM.NONE)
                player.addXp(Skills.AGILITY, 10.0)
                player.setStage(2)
            }
        }

        onObjectOption("objects.climbing_branch", "climb") {
            val dest = Tile (2473, 3420, 2)

            player.queue {
                player.message(type = ChatMessageType.GAME_MESSAGE, message = "You climb the tree...")
                player.animate("sequences.human_reachforladder")
                wait(2)
                player.moveTo(dest)
                wait(1)
                player.animate(RSCM.NONE)
                player.message(type = ChatMessageType.GAME_MESSAGE, message = "... to the platform above.")
                player.addXp(Skills.AGILITY, 6.5)
                player.setStage(2)
            }
        }
        onObjectOption("objects.balancing_rope", "walk-on") {
            val dest = Tile(2483, 3420, 2)

            player.queue {
                player.message(type = ChatMessageType.GAME_MESSAGE, message = "You carefully cross the tighrope.")
                player.animate("sequences.human_walk_logbalance_ready")
                wait(1)
                player.loopAnim("sequences.human_walk_logbalance_loop")
                val fm = ForcedMovement.of(
                    src = player.tile,
                    dst = dest,
                    clientDuration1 = 5,
                    clientDuration2 = 250,
                    directionAngle = Direction.EAST.angle
                )
                player.forceMove(this, fm)
                player.stopLoopAnim()
                wait(1)
                player.animate(RSCM.NONE)
                player.addXp(Skills.AGILITY, 10.0)
                player.setStage(4)
            }
        }
        onObjectOption("objects.climbing_tree", "climb-down") {
            val dest = Tile (2487, 3420, 0)

            player.queue {
                player.message(type = ChatMessageType.GAME_MESSAGE, message = "You climb the tree...")
                player.animate("sequences.human_reachforladder")
                wait(2)
                player.moveTo(dest)
                wait(1)
                player.animate(RSCM.NONE)
                player.message(type = ChatMessageType.GAME_MESSAGE, message = "You land on the ground.")
                player.addXp(Skills.AGILITY, 6.5)
                player.setStage(5)
            }
        }
        onObjectOption("objects.climbing_tree2", "climb-down") {
            val dest = Tile (2487, 3420, 0)

            player.queue {
                player.message(type = ChatMessageType.GAME_MESSAGE, message = "You climb the tree...")
                player.animate("sequences.human_reachforladder")
                wait(2)
                player.moveTo(dest)
                wait(1)
                player.animate(RSCM.NONE)
                player.message(type = ChatMessageType.GAME_MESSAGE, message = "You land on the ground.")
                player.addXp(Skills.AGILITY, 6.5)
                player.setStage(5)
            }
        }
        onObjectOption("objects.obstical_net3", "climb-over") {
            if(player.tile.x !in 2483..2488) return@onObjectOption

            player.queue {
                player.message(type = ChatMessageType.GAME_MESSAGE, message = "You climb up the netting...")
                player.animate("sequences.human_reachforladder")
                wait(2)
                player.moveTo(player.tile.x, 3428, 0)
                wait(1)
                player.animate(RSCM.NONE)
                player.addXp(Skills.AGILITY, 10.0)
                player.setStage(6)
            }
        }
        onObjectOption("objects.obstical_pipe3_1", "squeeze-through") {
            val dest = Tile(2484, 3437, 0)

            player.queue {
                player.animate("sequences.human_pipesqueeze_ready")
                wait(1)
                player.loopAnim("sequences.human_pipesqueeze")
                val fm = ForcedMovement.of(
                    src = player.tile,
                    dst = dest,
                    clientDuration1 = 5,
                    clientDuration2 = 250,
                    directionAngle = Direction.NORTH.angle
                )
                player.forceMove(this, fm)
                player.stopLoopAnim()
                player.animate("sequences.human_pipeunsqueeze")
                wait(1)
                player.animate(RSCM.NONE)
                player.addXp(Skills.AGILITY, 10.0)

                if (player.stage() == MAX_STAGES) {
                    val laps = player.laps() + 1
                    player.setLaps(laps)
                    player.filterableMessage("Your Gnome Stronghold Agility lap count is: <col=ff0000>$laps</col>.")
                    player.addXp(Skills.AGILITY, BONUS_XP)

                    GraceService.spawnMarkofGrace(player)
                }
                player.setStage(0)
            }
        }
        onObjectOption("objects.obstical_pipe3_2", "squeeze-through") {
            val dest = Tile(2487, 3437, 0)

            player.queue {
                player.animate("sequences.human_pipesqueeze_ready")
                wait(1)
                player.loopAnim("sequences.human_pipesqueeze")
                val fm = ForcedMovement.of(
                    src = player.tile,
                    dst = dest,
                    clientDuration1 = 5,
                    clientDuration2 = 250,
                    directionAngle = Direction.NORTH.angle
                )
                player.forceMove(this, fm)
                player.stopLoopAnim()
                player.animate("sequences.human_pipeunsqueeze")
                wait(1)
                player.animate(RSCM.NONE)
                player.addXp(Skills.AGILITY, 10.0)

                if (player.stage() == MAX_STAGES) {
                    val laps = player.laps() + 1
                    player.setLaps(laps)
                    player.filterableMessage("Your Gnome Stronghold Agility lap count is: <col=ff0000>$laps</col>.")
                    player.addXp(Skills.AGILITY, BONUS_XP)

                    GraceService.spawnMarkofGrace(player)
                }
                player.setStage(0)
            }
        }
    }

}