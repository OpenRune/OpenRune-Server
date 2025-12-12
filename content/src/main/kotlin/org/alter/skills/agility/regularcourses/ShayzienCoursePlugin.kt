package org.alter.skills.agility.regularcourses

import org.alter.api.ChatMessageType
import org.alter.api.Skills
import org.alter.api.ext.filterableMessage
import org.alter.api.ext.loopAnim
import org.alter.api.ext.stopLoopAnim
import org.alter.api.ext.stopWalkAnimOverride
import org.alter.api.ext.walkAnimOverride
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

class ShayzienCoursePlugin : PluginEvent() {

    private val MAX_STAGES = 7
    private val ADVANCED_STAGES = 8
    private val DROP_CHANCE = 1.0 / 10.0

    private val MARK_SPAWN_TILES = listOf(
        Tile(1525, 3636, 2)
    )

    private val GraceService = MarkOfGraceService(
        spawnTiles = MARK_SPAWN_TILES,
        dropChance = DROP_CHANCE
    )

    val STAGE = AttributeKey<Int>("ShayzienAgilityStage")
    fun Player.stage() = attr[STAGE] ?: 0
    fun Player.setStage(v: Int) { attr[STAGE] = v }

    val LAPS = AttributeKey<Int>("ShayzienAgilityLaps")
    fun Player.laps() = attr[LAPS] ?: 0
    fun Player.setLaps(v: Int) { attr[LAPS] = v }

    val ADVANCEDSTAGE = AttributeKey<Int>("AdvancedShayzienAgilityStage")
    fun Player.advancedstage() = attr[ADVANCEDSTAGE] ?: 0
    fun Player.setAdvancedStage(v: Int) { attr[ADVANCEDSTAGE] = v }

    val ADVANCEDLAPS = AttributeKey<Int>("AdvancedShayzienAgilityLaps")
    fun Player.advancedlaps() = attr[ADVANCEDLAPS] ?: 0
    fun Player.setAdvancedLaps(v: Int) { attr[ADVANCEDLAPS] = v }

    override fun init() {

        onObjectOption("objects.shayzien_agility_both_start_ladder", "climb") {
            val dest = Tile(1554, 3632, 3)

            player.queue {
                player.animate("sequences.human_reachforladder")

                wait(2)
                player.moveTo(dest)
                wait(1)
                player.animate(RSCM.NONE)

                player.addXp(Skills.AGILITY, 5.5)
                player.setStage(1)
                player.setAdvancedStage(1)
            }
        }

        onObjectOption("objects.shayzien_agility_both_rope_climb", "Climb") {
            val realDest = Tile(1541, 3633, 2)
            val fakeDest = Tile(realDest.x, realDest.z, player.tile.height)

            player.queue {
                player.animate("sequences.human_monkeybars_on")
                wait(1)
                player.walkAnimOverride("sequences.human_monkeybars_walk")
                val fm = ForcedMovement.of(
                    src = player.tile,
                    dst = fakeDest,
                    clientDuration1 = 5,
                    clientDuration2 = 350,
                    directionAngle = Direction.WEST.angle
                )
                player.forceMove(this, fm)
                player.stopWalkAnimOverride()
                player.animate("sequences.human_monkeybars_off")
                wait(1)
                player.moveTo(realDest)
                player.animate(RSCM.NONE)
                player.addXp(Skills.AGILITY, 8.0)
                player.setStage(2)
                player.setAdvancedStage(2)
            }
        }


        onObjectOption("objects.shayzien_agility_both_rope_walk", "Cross") {
            val dest = Tile(1528, 3633, 2)

            player.queue {
                player.animate("sequences.human_walk_logbalance_ready")
                wait(1)
                player.loopAnim("sequences.human_walk_logbalance_loop")
                val fm = ForcedMovement.of(
                    src = player.tile,
                    dst = dest,
                    clientDuration1 = 5,
                    clientDuration2 = 250,
                    directionAngle = Direction.WEST.angle
                )
                player.forceMove(this, fm)
                player.stopLoopAnim()
                wait(1)
                player.animate(RSCM.NONE)
                player.addXp(Skills.AGILITY, 9.0)
                player.setStage(3)
                player.setAdvancedStage(3)
            }
        }

        onObjectOption("objects.shayzien_agility_low_bar_climb", "Climb") {
            val dest = Tile(1523, 3643, 3)

            player.queue {
                player.animate("sequences.human_monkeybars_on")
                player.loopAnim("sequences.human_monkeybars_walk")
                wait(1)
                org.alter.game.info.PlayerInfo(player).syncAppearance()
                val fm = ForcedMovement.of(
                    src = player.tile,
                    dst = dest,
                    clientDuration1 = 5,
                    clientDuration2 = 250,
                    directionAngle = Direction.NORTH.angle
                )
                player.forceMove(this, fm)
                player.stopLoopAnim()
                player.animate("sequences.human_monkeybars_off")
                wait(1)
                player.moveTo(dest)
                player.animate(RSCM.NONE)
                player.addXp(Skills.AGILITY, 7.0)
                player.setStage(4)
                player.setAdvancedStage(0)
            }
        }

        onObjectOption("objects.shayzien_agility_low_rope_walk_1", "Cross") {
            val dest = Tile(1538, 3644, 3)

            player.queue {
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
                player.animate("sequences.agility_shortcut_wall_jumpdown")
                wait(1)
                player.moveTo(1539, 3644, 2)
                player.animate("sequences.agility_shortcut_wall_jumpdown2")
                wait(2)
                player.animate(RSCM.NONE)
                player.addXp(Skills.AGILITY, 9.0)
                player.setStage(5)
            }
        }

        onObjectOption("objects.shayzien_agility_low_rope_walk_2", "Cross") {
            val dest = Tile(1552, 3644, 2)

            player.queue {
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
                player.addXp(Skills.AGILITY, 9.0)
                player.filterableMessage("... and land safely.")
                player.setStage(6)
            }
        }

        onObjectOption("objects.shayzien_agility_low_end_jump", "Jump") {
            val dest = Tile(1554, 3640, 0)

            player.queue {
                player.faceDirection(Direction.NORTH)
                player.animate("sequences.agility_shortcut_wall_jumpdown")
                wait(2)
                player.moveTo(dest)
                player.animate("sequences.agility_shortcut_wall_jumpdown2")
                wait(1)
                player.animate(RSCM.NONE)
                player.addXp(Skills.AGILITY, 106.0)

                // LAP CHECK
                if (player.stage() == MAX_STAGES) {
                    val laps = player.laps() + 1
                    player.setLaps(laps)
                    player.filterableMessage("Your Shayzien Basic Agility Cource lap count is: <col=ff0000>$laps</col>.")

                    GraceService.spawnMarkofGrace(player)
                }

                player.setStage(0)
            }
        }
        //Advanced
        onObjectOption("objects.shayzien_agility_up_swing_jump_1", "Grapple") {
            val dest = Tile(1511, 3637, 2)

            //TODO: Add crossbow and mithrill grapple requirements

            player.queue {
                player.filterableMessage("You fire your grapple at the pylon...")
                player.animate("sequences.dorgesh_xbow_swing", 6)
                player.graphic("spotanims.dorgesh_grapple_spot",82, 6)
                wait(1)
                val fm = ForcedMovement.of(
                    src = player.tile,
                    dst = dest,
                    clientDuration1 = 5,
                    clientDuration2 = 250,
                    directionAngle = Direction.WEST.angle
                )
                player.forceMove(this, fm)
                player.moveTo(dest)
                player.animate(RSCM.NONE)
                player.addXp(Skills.AGILITY, 23.0)
                player.setStage(0)
                player.setAdvancedStage(4)
            }
        }
        onObjectOption("objects.shayzien_agility_up_jump_platform_1", "Jump") {
            val dest = Tile(1510, 3630, 2)


            player.queue {
                player.loopAnim("sequences.human_walk_b")
                player.lock
                player.unlock()
                player.moveTo(1510, 3636, 2)
                wait(3)
                player.stopLoopAnim()
                player.loopAnim("sequences.ic_running")
                player.moveTo(1510, 3635, 2)
                wait(1)
                player.stopLoopAnim()
                val fm = ForcedMovement.of(
                    src = player.tile,
                    dst = dest,
                    clientDuration1 = 5,
                    clientDuration2 = 250,
                    directionAngle = Direction.SOUTH.angle
                )
                player.animate("sequences.human_jump_hurdle")
                player.forceMove(this, fm)
                player.moveTo(dest)
                player.animate(RSCM.NONE)
                player.addXp(Skills.AGILITY, 18.0)
                player.setAdvancedStage(5)
            }
        }
        onObjectOption("objects.shayzien_agility_up_jump_platform_2", "Jump") {
            val dest1 = Tile(1510, 3625, 2)
            val dest2 = Tile(1510, 3622, 2)
            val dest3 = Tile(1510, 3620, 2)


            player.queue {
                val fm1 = ForcedMovement.of(
                    src = player.tile,
                    dst = dest1,
                    clientDuration1 = 5,
                    clientDuration2 = 50,
                    directionAngle = Direction.SOUTH.angle
                )
                player.animate("sequences.human_jump_hurdle")
                player.forceMove(this, fm1)
                wait(2)
                val fm2 = ForcedMovement.of(
                    src = player.tile,
                    dst = dest2,
                    clientDuration1 = 5,
                    clientDuration2 = 50,
                    directionAngle = Direction.SOUTH.angle
                )
                player.animate("sequences.human_jump_hurdle")
                player.forceMove(this, fm2)
                val fm3 = ForcedMovement.of(
                    src = player.tile,
                    dst = dest3,
                    clientDuration1 = 5,
                    clientDuration2 = 50,
                    directionAngle = Direction.SOUTH.angle
                )
                player.animate("sequences.human_jump_hurdle")
                player.forceMove(this, fm3)
                wait(5)
                player.animate(RSCM.NONE)
                player.addXp(Skills.AGILITY, 21.0)
                player.setAdvancedStage(6)
            }
        }
        onObjectOption("objects.shayzien_agility_up_swing_jump_2", "Grapple") {
            val dest = Tile(1521, 3619, 2)

            //TODO: Add crossbow and mithrill grapple requirements

            player.queue {
                player.filterableMessage("You fire your grapple at the pylon...")
                player.animate("sequences.dorgesh_xbow_swing", 6)
                player.graphic("spotanims.dorgesh_grapple_spot",82, 6)
                wait(2)
                val fm = ForcedMovement.of(
                    src = player.tile,
                    dst = dest,
                    clientDuration1 = 0,
                    clientDuration2 = 150,
                    directionAngle = Direction.EAST.angle
                )
                player.forceMove(this, fm)
                player.animate(RSCM.NONE)
                player.addXp(Skills.AGILITY, 23.0)
                player.setStage(0)
                player.setAdvancedStage(7)
            }
        }
        onObjectOption("objects.shayzien_agility_up_end_jump", "Slide") {
            val realDest = Tile(1522, 3625, 0)
            val fakeDest = Tile(realDest.x, realDest.z, player.tile.height)

            player.queue {
                player.faceDirection(Direction.NORTH)
                player.animate("sequences.human_monkeybars_on")
                wait(2)
                val fm = ForcedMovement.of(
                    src = player.tile,
                    dst = fakeDest,
                    clientDuration1 = 0,
                    clientDuration2 = 100,
                    directionAngle = Direction.NORTH.angle
                )
                wait(1)
                player.animate("sequences.human_monkeybars_off")
                player.moveTo(realDest)
                player.animate(RSCM.NONE)
                player.addXp(Skills.AGILITY, 400.0)

                if (player.stage() == ADVANCED_STAGES) {
                    val advancedlaps = player.advancedlaps() + 1
                    player.setAdvancedLaps(advancedlaps)
                    player.filterableMessage("Your Shayzien Advanced Agility Cource lap count is: <col=ff0000>$advancedlaps</col>.")

                    GraceService.spawnMarkofGrace(player)
                }

                player.setStage(0)
            }
        }
    }
}
