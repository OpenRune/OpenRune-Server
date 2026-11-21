package org.rsmod.content.skills.agility.pack.course

import dev.openrune.definition.dbtables.DBTableBuilder
import org.rsmod.content.skills.agility.pack.CLIMB
import org.rsmod.content.skills.agility.pack.DOUBLE_PIPE_SQUEEZE
import org.rsmod.content.skills.agility.pack.FACE_NORTH
import org.rsmod.content.skills.agility.pack.LOG_BALANCE
import org.rsmod.content.skills.agility.pack.LOG_SYNTH
import org.rsmod.content.skills.agility.pack.SQUEEZE_IN_SYNTH
import org.rsmod.content.skills.agility.pack.TIGHTROPE_SYNTH
import org.rsmod.content.skills.agility.pack.lane
import org.rsmod.content.skills.agility.pack.lanes
import org.rsmod.content.skills.agility.pack.obstacle
import org.rsmod.content.skills.agility.pack.tile

private const val GNOME = "dbrow.agility_course_gnome_stronghold"

private const val NET_MESSAGE = "You climb the netting..."

private fun netX(x: Int): Int = x - (x - 2471) % 2

internal fun DBTableBuilder.gnomeStrongholdObstacles() {
    row("dbrow.agility_gnome_log_balance") {
        obstacle(GNOME, step = 0, xp = 100, "loc.gnome_log_balance1")
        lanes(
            lane(tile(2474, 3436, 0)) {
                message("You walk carefully across the slippery log...")
                balance(LOG_BALANCE)
                sound(LOG_SYNTH, loops = 4)
                step(tile(2474, 3435, 0))
                next()
                walkTo(tile(2474, 3429, 0))
                sound(LOG_SYNTH)
                next()
                balanceOff()
                face(tile(2474, 3429, 0))
                message("...You make it safely to the other side.")
                land()
            }
        )
    }

    row("dbrow.agility_gnome_obstacle_net_1") {
        obstacle(GNOME, step = 1, xp = 100, "loc.obstical_net2")
        lanes(
            *(2471..2476)
                .map { x ->
                    lane(tile(x, 3426, 0)) {
                        opMessage(NET_MESSAGE)
                        anim(CLIMB)
                        next()
                        telejump(tile(netX(x), 3423, 1))
                        land()
                    }
                }
                .toTypedArray()
        )
    }

    row("dbrow.agility_gnome_tree_branch_up") {
        obstacle(GNOME, step = 2, xp = 65, "loc.climbing_branch")
        lanes(
            lane(tile(2473, 3423, 1)) {
                opMessage("You climb the tree...")
                anim(CLIMB)
                next()
                message("...To the platform above.")
                telejump(tile(2473, 3420, 2))
                land()
            }
        )
    }

    row("dbrow.agility_gnome_balancing_rope") {
        obstacle(GNOME, step = 3, xp = 100, "loc.balancing_rope")
        lanes(
            lane(tile(2477, 3420, 2)) {
                message("You carefully cross the tightrope.")
                balance(LOG_BALANCE)
                sound(TIGHTROPE_SYNTH, loops = 5)
                step(tile(2478, 3420, 2))
                next()
                walkTo(tile(2483, 3420, 2))
                next()
                balanceOff()
                land()
            }
        )
    }

    row("dbrow.agility_gnome_tree_branch_down") {
        obstacle(GNOME, step = 4, xp = 65, "loc.climbing_tree", "loc.climbing_tree2")
        lanes(
            *listOf(tile(2485, 3419, 2), tile(2486, 3420, 2))
                .map { start ->
                    lane(start) {
                        opMessage("You climb down the tree...")
                        anim(CLIMB)
                        next()
                        message("You land on the ground.")
                        telejump(tile(2487, 3420, 0))
                        land()
                    }
                }
                .toTypedArray()
        )
    }

    row("dbrow.agility_gnome_obstacle_net_2") {
        obstacle(GNOME, step = 5, xp = 100, "loc.obstical_net3")
        lanes(
            *(2483..2488)
                .map { x ->
                    lane(tile(x, 3425, 0)) {
                        opMessage(NET_MESSAGE)
                        anim(CLIMB)
                        next()
                        step(tile(x - (x - 2483) % 2, 3428, 0))
                        land()
                    }
                }
                .toTypedArray()
        )
    }

    row("dbrow.agility_gnome_obstacle_pipe") {
        obstacle(GNOME, step = 6, xp = 75, "loc.obstical_pipe3_1", "loc.obstical_pipe3_2")
        lanes(
            *listOf(2484, 2487)
                .map { x ->
                    lane(tile(x, 3430, 0)) {
                        exactMove(tile(x, 3433, 0), delay1 = 30, delay2 = 96, angle = FACE_NORTH)
                        anim(DOUBLE_PIPE_SQUEEZE, delay = 30)
                        next(3)
                        sound(SQUEEZE_IN_SYNTH)
                        next(3)
                        step(tile(x, 3434, 0))
                        next()
                        exactMove(tile(x, 3437, 0), delay1 = 30, delay2 = 96, angle = FACE_NORTH)
                        anim(DOUBLE_PIPE_SQUEEZE, delay = 30)
                        next(3)
                        sound(SQUEEZE_IN_SYNTH)
                        land()
                    }
                }
                .toTypedArray()
        )
    }
}
