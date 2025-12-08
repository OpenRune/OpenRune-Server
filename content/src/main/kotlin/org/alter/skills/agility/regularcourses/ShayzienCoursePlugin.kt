package org.alter.skills.agility.regularcourses

import org.alter.api.HitType
import org.alter.api.Skills
import org.alter.api.ext.filterableMessage
import org.alter.api.ext.hit
import org.alter.game.model.Direction
import org.alter.game.model.ForcedMovement
import org.alter.game.model.Tile
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.entity.Player
import org.alter.game.model.move.moveTo
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onObjectOption
import org.alter.rscm.RSCM
import org.alter.skills.agility.MarkOfGraceService

//TODO:
// - Animations
// - Advanced Course
// - Messages
class ShayzienCoursePlugin : PluginEvent() {

    private val MAX_STAGES = 7

    private val DROP_CHANCE = 1.0 / 5.0   // 20%


    private val MARK_SPAWN_TILES = listOf(
        Tile(2504, 3545, 1),
        Tile(2533, 3555, 0)
    )
    private val GraceService = MarkOfGraceService(
        spawnTiles = MARK_SPAWN_TILES,
        dropChance = DROP_CHANCE
    )

    val SHAYZIEN_AGILITY_STAGE = AttributeKey<Int>("ShayzienAgilityStage")
    private fun Player.getStage(): Int = attr[SHAYZIEN_AGILITY_STAGE] ?: 0
    private fun Player.setStage(v: Int) { attr[SHAYZIEN_AGILITY_STAGE] = v }

    val SHAYZIEN_LAPS = AttributeKey<Int>("ShayzienAgilityLaps")
    private fun Player.getLaps(): Int = attr[SHAYZIEN_LAPS] ?: 0
    private fun Player.setLaps(v: Int) { attr[SHAYZIEN_LAPS] = v }

    override fun init() {

        onObjectOption("objects.shayzien_agility_both_start_ladder", "climb") {
            handleObstacle(
                player = player,
                destination = Tile(1554, 3632, 3),
                anim = "sequences.human_reachforladder",
                simpleMove = true,
                xp = 5.5,
                messageStart = "You climb up the netting...",
                stage = 1
            )
        }
        onObjectOption("objects.shayzien_agility_both_rope_climb", "Climb") {
            handleObstacle(
                player = player,
                destination = Tile(1541, 3633, 2),
                anim = "sequences.human_ropeswing_long",
                duration1 = 5,
                duration2 = 250,
                angle = Direction.EAST.angle,
                xp = 8.0,
                messageStart = "You grab the rope...",
                messageEnd = "... and land safely.",
                stage = 2,
            )
        }
        onObjectOption("objects.shayzien_agility_both_rope_walk", "Cross") {
            handleObstacle(
                player = player,
                destination = Tile(1528, 3633, 2),
                anim = "sequences.human_ropeswing_long",
                duration1 = 5,
                duration2 = 250,
                angle = Direction.EAST.angle,
                xp = 9.0,
                messageStart = "You grab the rope...",
                messageEnd = "... and land safely.",
                stage = 3,
            )
        }
        onObjectOption("objects.shayzien_agility_low_bar_climb", "Climb") {
            handleObstacle(
                player = player,
                destination = Tile(1523, 3643, 3),
                anim = "sequences.human_ropeswing_long",
                duration1 = 5,
                duration2 = 250,
                angle = Direction.EAST.angle,
                xp = 7.0,
                messageStart = "You grab the rope...",
                messageEnd = "... and land safely.",
                stage = 4,
            )
        }
        onObjectOption("objects.shayzien_agility_low_rope_walk_1", "Cross") {
            handleObstacle(
                player = player,
                destination = Tile(1539, 3644, 2),
                anim = "sequences.human_ropeswing_long",
                duration1 = 5,
                duration2 = 250,
                angle = Direction.EAST.angle,
                xp = 9.0,
                messageStart = "You grab the rope...",
                messageEnd = "... and land safely.",
                stage = 5,
            )
        }
        onObjectOption("objects.shayzien_agility_low_rope_walk_2", "Cross") {
            handleObstacle(
                player = player,
                destination = Tile(1552, 3644, 2),
                anim = "sequences.human_ropeswing_long",
                duration1 = 5,
                duration2 = 250,
                angle = Direction.EAST.angle,
                xp = 9.0,
                messageStart = "You grab the rope...",
                messageEnd = "... and land safely.",
                stage = 6,
            )
        }
        onObjectOption("objects.shayzien_agility_low_end_jump", "Jump") {
            handleObstacle(
                player = player,
                destination = Tile(1554, 3640, 0),
                anim = "sequences.human_ropeswing_long",
                duration1 = 5,
                duration2 = 250,
                angle = Direction.EAST.angle,
                xp = 106.0,
                messageStart = "You grab the rope...",
                messageEnd = "... and land safely.",
                endStage = true,
                stage = 7,
            )
        }
    }

    private fun handleObstacle(
        player: Player,
        destination: Tile,
        simpleMove: Boolean = false,
        angle: Int? = null,
        duration1: Int? = null,
        duration2: Int? = null,
        anim: String? = null,
        xp: Double = 0.0,
        messageStart: String? = null,
        messageEnd: String? = null,
        stage: Int = -1,
        endStage: Boolean = false,

        failChance: Double = 0.0,
        onFail: ((Player) -> Unit)? = null
    ) {

        val doForcedMove = angle != null && duration1 != null && duration2 != null
        val current = player.getStage()

        val isNext = current + 1 == stage
        val isRepeat = current == stage

        if (isNext) {
            player.setStage(stage)
        } else if (!isRepeat) {
            player.setStage(0)
        }

        player.queue {

            if (failChance > 0 && Math.random() < failChance) {
                player.setStage(0)
                onFail?.invoke(player)
                return@queue
            }

            messageStart?.let { player.filterableMessage(it) }
            anim?.let { player.animate(it) }

            if (doForcedMove) {
                val movement = ForcedMovement.of(
                    src = player.tile,
                    dst = destination,
                    clientDuration1 = duration1,
                    clientDuration2 = duration2,
                    directionAngle = angle
                )
                player.forceMove(this, movement)
                wait(1)
                player.animate(RSCM.NONE)

            } else if (simpleMove) {
                wait(2)
                player.moveTo(destination)
                wait(1)
                player.animate(RSCM.NONE)
            }

            wait(1)

            if (xp > 0.0)
                player.addXp(Skills.AGILITY, xp)

            messageEnd?.let { player.filterableMessage(it) }

            handleStage(player, stage, endStage)
        }
    }

    private fun handleStage(
        player: Player,
        stage: Int,
        endStage: Boolean
    ) {
        val cur = player.getStage()

        if (!endStage && stage >= 0) {
            if (stage == cur + 1) {
                player.setStage(stage)
            }
            return
        }

        if (endStage) {
            val completed = cur == MAX_STAGES

            if (completed) {

                val laps = player.getLaps() + 1
                player.setLaps(laps)

                player.filterableMessage("Your Shayzien lap count is: <col=ff0000>$laps</col>.")

                GraceService.spawnMarkofGrace(player)
            }

            player.setStage(0)
        }
    }
}
