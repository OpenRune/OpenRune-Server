package org.rsmod.content.skills.agility.pack.course

import dev.openrune.definition.dbtables.DBTableBuilder
import org.rsmod.content.skills.agility.pack.CLIMB
import org.rsmod.content.skills.agility.pack.CRUMBLED_WALL
import org.rsmod.content.skills.agility.pack.FACE_EAST
import org.rsmod.content.skills.agility.pack.FACE_SOUTH
import org.rsmod.content.skills.agility.pack.INTO_SIDESTEP
import org.rsmod.content.skills.agility.pack.LEDGE_SYNTH
import org.rsmod.content.skills.agility.pack.LOG_BALANCE
import org.rsmod.content.skills.agility.pack.LOG_MESSAGE
import org.rsmod.content.skills.agility.pack.LOG_SYNTH
import org.rsmod.content.skills.agility.pack.OUT_OF_SIDESTEP
import org.rsmod.content.skills.agility.pack.ROPE_SWING
import org.rsmod.content.skills.agility.pack.SIDESTEP
import org.rsmod.content.skills.agility.pack.SWING_MESSAGE
import org.rsmod.content.skills.agility.pack.SWING_SYNTH
import org.rsmod.content.skills.agility.pack.lane
import org.rsmod.content.skills.agility.pack.lanes
import org.rsmod.content.skills.agility.pack.obstacle
import org.rsmod.content.skills.agility.pack.tile

private const val BARBARIAN = "dbrow.agility_course_barbarian"

private val BARBARIAN_WALLS =
    listOf(
        "dbrow.agility_barbarian_wall_1" to 2536,
        "dbrow.agility_barbarian_wall_2" to 2539,
        "dbrow.agility_barbarian_wall_3" to 2542,
    )

// UNVERIFIED: everything but the log, and the log's end message. The other lanes follow the
// Wilderness ropeswing and the Draynor ledge, and nothing fails since the wiki doesn't say where.
internal fun DBTableBuilder.barbarianObstacles() {
    row("dbrow.agility_barbarian_ropeswing") {
        obstacle(BARBARIAN, step = 0, xp = 220, "loc.obstical_ropeswing1")
        lanes(
            lane(tile(2551, 3554, 0)) {
                face(tile(2551, 3552, 0))
                next()
                exactMove(tile(2551, 3549, 0), delay1 = 45, delay2 = 70, angle = FACE_SOUTH)
                anim(ROPE_SWING, delay = 20)
                next()
                sound(SWING_SYNTH)
                message(SWING_MESSAGE)
                land()
            }
        )
    }

    row("dbrow.agility_barbarian_log_balance") {
        obstacle(BARBARIAN, step = 1, xp = 137, "loc.barbarian_log_balance1")
        lanes(
            lane(tile(2551, 3546, 0)) {
                message(LOG_MESSAGE)
                step(tile(2550, 3546, 0))
                balance(LOG_BALANCE)
                next()
                sound(LOG_SYNTH, loops = 4)
                walkTo(tile(2545, 3546, 0))
                next()
                sound(LOG_SYNTH, loops = 2)
                walkTo(tile(2541, 3546, 0))
                next()
                balanceOff()
                message("...You make it safely to the other side.")
                land()
            }
        )
    }

    row("dbrow.agility_barbarian_net") {
        obstacle(BARBARIAN, step = 2, xp = 82, "loc.agility_obstical_net_barbarian")
        lanes(
            *(3545..3547)
                .map { z ->
                    lane(tile(2539, z, 0)) {
                        opMessage("You climb the netting...")
                        anim(CLIMB)
                        next()
                        telejump(tile(2537, z, 1))
                        land()
                    }
                }
                .toTypedArray()
        )
    }

    row("dbrow.agility_barbarian_ledge") {
        obstacle(BARBARIAN, step = 3, xp = 220, "loc.balancing_ledge1")
        lanes(
            lane(tile(2536, 3547, 1)) {
                message("You put your foot on the ledge and try to edge across...")
                anim(INTO_SIDESTEP)
                balance(SIDESTEP)
                step(tile(2535, 3547, 1))
                next()
                sound(LEDGE_SYNTH, loops = 3)
                walkTo(tile(2532, 3547, 1))
                next()
                balanceOff()
                anim(OUT_OF_SIDESTEP)
                message("You skillfully edge across the gap.")
                land()
            }
        )
    }

    row("dbrow.agility_barbarian_ladder") {
        obstacle(BARBARIAN, step = 4, xp = 0, "loc.barbarian_laddertop_norim")
        lanes(
            lane(tile(2532, 3546, 1)) {
                anim(CLIMB)
                next()
                telejump(tile(2532, 3546, 0))
                land()
            }
        )
    }

    for ((index, wall) in BARBARIAN_WALLS.withIndex()) {
        val (row, x) = wall
        row(row) {
            obstacle(BARBARIAN, step = 5 + index, xp = 137, "loc.castlecrumbly1")
            lanes(
                lane(tile(x - 1, 3553, 0)) {
                    exactMove(tile(x + 1, 3553, 0), delay1 = 30, delay2 = 60, angle = FACE_EAST)
                    anim(CRUMBLED_WALL)
                    next(2)
                    land()
                }
            )
        }
    }
}
