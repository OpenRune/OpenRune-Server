package org.rsmod.content.skills.agility.pack.course

import dev.openrune.definition.dbtables.DBTableBuilder
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.REQUIREMENT_NINJA_GREEGREE
import org.rsmod.content.skills.agility.pack.FACE_EAST
import org.rsmod.content.skills.agility.pack.FACE_WEST
import org.rsmod.content.skills.agility.pack.JUMP_SYNTH
import org.rsmod.content.skills.agility.pack.lane
import org.rsmod.content.skills.agility.pack.lanes
import org.rsmod.content.skills.agility.pack.obstacle
import org.rsmod.content.skills.agility.pack.requirement
import org.rsmod.content.skills.agility.pack.tile

private const val APE_ATOLL = "dbrow.agility_course_ape_atoll"

private const val APE_STONE_JUMP = "seq.100_ilm_stepping_stone_jump"
private const val APE_TREE_CLIMB = "seq.100_ilm_climb_tree"
private const val APE_BARS_ON = "seq.100_ilm_monkeybar_jump_up"
private const val APE_BARS_MOVE = "seq.100_ilm_monkeybar_move"
private const val APE_BARS_OFF = "seq.100_ilm_monkeybar_jump_down"
private const val APE_SLOPE = "seq.100_ilm_climb_slope"
private const val APE_VINE_SWING = "seq.100_ilm_vine_swing"
private const val APE_DOWN_VINE = "seq.100_ilm_monkey_down_vine"

private val NINJA_GREEGREES =
    arrayOf(
        "obj.mm_monkey_greegree_for_small_ninja_monkey",
        "obj.mm_monkey_greegree_for_medium_ninja_monkey",
        "obj.mm2_kruk_greegree",
    )

// UNVERIFIED: no capture. The lanes follow the cache's monkey seqs, and nothing fails since
// the wiki gives no fall tiles.
internal fun DBTableBuilder.apeAtollObstacles() {
    row("dbrow.agility_ape_atoll_stepping_stone") {
        obstacle(APE_ATOLL, step = 0, xp = 400, "loc.100_ilm_stepping_stone")
        requirement(REQUIREMENT_NINJA_GREEGREE, *NINJA_GREEGREES)
        lanes(
            lane(tile(2755, 2742, 0)) {
                exactMove(tile(2754, 2742, 0), delay1 = 15, delay2 = 30, angle = FACE_WEST)
                anim(APE_STONE_JUMP)
                sound(JUMP_SYNTH)
                next(2)
                exactMove(tile(2753, 2742, 0), delay1 = 15, delay2 = 30, angle = FACE_WEST)
                anim(APE_STONE_JUMP)
                sound(JUMP_SYNTH)
                next(2)
                land()
            }
        )
    }

    row("dbrow.agility_ape_atoll_tropical_tree") {
        obstacle(APE_ATOLL, step = 1, xp = 400, "loc.100_ilm_climbable_tree")
        requirement(REQUIREMENT_NINJA_GREEGREE, *NINJA_GREEGREES)
        lanes(
            lane(tile(2753, 2742, 0)) {
                face(tile(2753, 2741, 0))
                anim(APE_TREE_CLIMB)
                next(4)
                telejump(tile(2752, 2741, 2))
                land()
            }
        )
    }

    row("dbrow.agility_ape_atoll_monkeybars") {
        obstacle(
            APE_ATOLL,
            step = 2,
            xp = 400,
            "loc.100_ilm_monkeybars_start",
            "loc.100_ilm_monkeybars_end",
        )
        requirement(REQUIREMENT_NINJA_GREEGREE, *NINJA_GREEGREES)
        lanes(
            lane(tile(2752, 2741, 2)) {
                anim(APE_BARS_ON)
                next(2)
                balance(APE_BARS_MOVE)
                for (x in 2751 downTo 2748) {
                    step(tile(x, 2741, 2))
                    next(2)
                }
                balanceOff()
                anim(APE_BARS_OFF)
                next()
                telejump(tile(2747, 2741, 0))
                land()
            }
        )
    }

    row("dbrow.agility_ape_atoll_skull_slope") {
        obstacle(APE_ATOLL, step = 3, xp = 600, "loc.100_ilm_cliff_climb_1")
        requirement(REQUIREMENT_NINJA_GREEGREE, *NINJA_GREEGREES)
        lanes(
            lane(tile(2747, 2741, 0)) {
                exactMove(tile(2743, 2741, 0), delay1 = 0, delay2 = 60, angle = FACE_WEST)
                anim(APE_SLOPE)
                next(2)
                land()
            }
        )
    }

    row("dbrow.agility_ape_atoll_rope") {
        obstacle(APE_ATOLL, step = 4, xp = 1000, "loc.100_ilm_rope_swing")
        requirement(REQUIREMENT_NINJA_GREEGREE, *NINJA_GREEGREES)
        lanes(
            lane(tile(2751, 2731, 0)) {
                exactMove(tile(2756, 2731, 0), delay1 = 30, delay2 = 70, angle = FACE_EAST)
                anim(APE_VINE_SWING)
                next(3)
                land()
            }
        )
    }

    row("dbrow.agility_ape_atoll_tropical_tree_down") {
        obstacle(APE_ATOLL, step = 5, xp = 0, "loc.100_ilm_agility_tree_base")
        requirement(REQUIREMENT_NINJA_GREEGREE, *NINJA_GREEGREES)
        lanes(
            lane(tile(2757, 2733, 0)) {
                face(tile(2757, 2734, 0))
                anim(APE_DOWN_VINE)
                next(2)
                telejump(tile(2755, 2741, 0))
                land()
            }
        )
    }
}
