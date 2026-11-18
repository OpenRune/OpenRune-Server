package org.rsmod.content.skills.agility.pack.course

import dev.openrune.definition.dbtables.DBRowBuilder
import dev.openrune.definition.dbtables.DBTableBuilder
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.COURSE
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.RECOVERY_STEP
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.STEP
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.XP
import org.rsmod.content.skills.agility.pack.FACE_EAST
import org.rsmod.content.skills.agility.pack.FACE_NORTH
import org.rsmod.content.skills.agility.pack.FACE_SOUTH
import org.rsmod.content.skills.agility.pack.FallDamage
import org.rsmod.content.skills.agility.pack.JUMP_SYNTH
import org.rsmod.content.skills.agility.pack.LaneBuilder
import org.rsmod.content.skills.agility.pack.apRange
import org.rsmod.content.skills.agility.pack.fails
import org.rsmod.content.skills.agility.pack.lane
import org.rsmod.content.skills.agility.pack.lanes
import org.rsmod.content.skills.agility.pack.obstacle
import org.rsmod.content.skills.agility.pack.tile
import org.rsmod.content.skills.agility.pack.walkOn
import org.rsmod.map.CoordGrid

private const val PENGUIN = "dbrow.agility_course_penguin"
private const val OFF_LAP = RECOVERY_STEP

private const val PENGUIN_JUMP = "seq.peng_jump"
private const val ICICLE_WALK = "seq.peng_icicle_walk"
private const val SLIPPERY_WALK = "seq.peng_slippery_walk"
private const val SLIP = "seq.peng_slippery_walk_fail"
private const val SLED_JUMP_A = "seq.peng_jump_a"
private const val SLED_JUMP_B = "seq.peng_jump_b"

private const val SNOW_SYNTH = "synth.peng_fall_snow"
private const val GATE_SYNTH = "synth.icegate_open"
private const val SLIP_SYNTH = "synth.peng_slip_snow"
private const val SLED_JUMP_SYNTH = "synth.peng_sled_jump"
private const val SLED_SYNTH = "synth.peng_sled_forward"

private const val COURSE_STATE = "varbit.peng_agility_state"

private class PenguinStone(val row: String, val loc: String, val to: CoordGrid, val angle: Int)

private val STONES =
    listOf(
        PenguinStone(
            "dbrow.agility_penguin_stepping_stone_1",
            "loc.peng_jump_stone_clickzone_01",
            tile(2631, 4057, 1),
            FACE_EAST,
        ),
        PenguinStone(
            "dbrow.agility_penguin_stepping_stone_2",
            "loc.peng_jump_stone_clickzone_02",
            tile(2631, 4059, 1),
            FACE_NORTH,
        ),
        PenguinStone(
            "dbrow.agility_penguin_stepping_stone_3",
            "loc.peng_jump_stone_clickzone_03",
            tile(2633, 4059, 1),
            FACE_EAST,
        ),
        PenguinStone(
            "dbrow.agility_penguin_stepping_stone_4",
            "loc.peng_jump_stone_clickzone_04",
            tile(2635, 4059, 1),
            FACE_EAST,
        ),
        PenguinStone(
            "dbrow.agility_penguin_stepping_stone_5",
            "loc.peng_jump_stone_clickzone_05",
            tile(2635, 4061, 1),
            FACE_NORTH,
        ),
        PenguinStone(
            "dbrow.agility_penguin_stepping_stone_6",
            "loc.peng_jump_stone_clickzone_06",
            tile(2635, 4063, 1),
            FACE_NORTH,
        ),
        PenguinStone(
            "dbrow.agility_penguin_stepping_stone_7",
            "loc.peng_jump_stone_clickzone_07",
            tile(2635, 4065, 1),
            FACE_NORTH,
        ),
    )

private val ICICLES =
    listOf(
        "dbrow.agility_penguin_icicles_1" to tile(2644, 4083, 1),
        "dbrow.agility_penguin_icicles_2" to tile(2652, 4080, 1),
        "dbrow.agility_penguin_icicles_3" to tile(2658, 4082, 1),
        "dbrow.agility_penguin_icicles_4" to tile(2662, 4082, 1),
    )

/** Each click on the ice moves the player one tile: from the first tile of a pair to the second. */
private val ICE_TILES =
    listOf(
        tile(2665, 4078, 1) to tile(2665, 4077, 1),
        tile(2665, 4077, 1) to tile(2665, 4076, 1),
        tile(2665, 4076, 1) to tile(2665, 4075, 1),
        tile(2665, 4075, 1) to tile(2665, 4074, 1),
        tile(2665, 4074, 1) to tile(2665, 4073, 1),
        tile(2664, 4074, 1) to tile(2664, 4073, 1),
        tile(2664, 4073, 1) to tile(2664, 4072, 1),
        tile(2664, 4072, 1) to tile(2664, 4071, 1),
    )

private class SledJump(
    val from: CoordGrid,
    val to: CoordGrid,
    val seq: String,
    val seqDelay: Int,
    val sledTicks: Int,
)

private val SLED_JUMPS =
    listOf(
        SledJump(tile(2663, 4066, 1), tile(2663, 4062, 1), SLED_JUMP_A, 20, sledTicks = 2),
        SledJump(tile(2662, 4059, 1), tile(2662, 4055, 1), SLED_JUMP_B, 20, sledTicks = 0),
        SledJump(tile(2657, 4046, 1), tile(2657, 4042, 1), SLED_JUMP_B, 10, sledTicks = 0),
    )

internal fun DBTableBuilder.penguinObstacles() {
    row("dbrow.agility_penguin_pool_entry") {
        offLapObstacle()
        walkOn()
        lanes(
            lane(tile(2638, 4052, 1)) {
                next()
                telejump(tile(2635, 4055, 0))
                land()
            }
        )
    }

    row("dbrow.agility_penguin_stone_climb") {
        obstacle(PENGUIN, step = 0, xp = 550, "loc.peng_agility_crushcourse_stepstone01")
        lanes(
            lane(tile(2629, 4057, 0)) {
                telejump(tile(2630, 4057, 1))
                varbit(COURSE_STATE, 1)
                land()
                hold()
            }
        )
    }

    var from = tile(2630, 4057, 1)
    for ((index, stone) in STONES.withIndex()) {
        val start = from
        val last = index == STONES.lastIndex
        row(stone.row) {
            obstacle(PENGUIN, step = 1 + index, xp = if (last) 800 else 0, stone.loc)
            apRange(2)
            lanes(
                lane(start) {
                    exactMove(stone.to, delay1 = 30, delay2 = 45, stone.angle, faceAngle = false)
                    anim(PENGUIN_JUMP)
                    sound(JUMP_SYNTH, delay = 10)
                    if (last) {
                        varbit(COURSE_STATE, 2)
                    }
                    land()
                    hold()
                }
            )
        }
        from = stone.to
    }

    for ((index, icicles) in ICICLES.withIndex()) {
        val (row, pillar) = icicles
        row(row) {
            obstacle(
                PENGUIN,
                step = 1 + STONES.size + index,
                xp = 400,
                "loc.peng_iciclepillar_clickzone",
            )
            apRange(2)
            lanes(
                lane(pillar.translateX(-1)) {
                    val dest = pillar.translateX(1)
                    exactMove(dest, delay1 = 5, delay2 = 60, FACE_EAST, faceAngle = false)
                    anim(ICICLE_WALK, delay = 2)
                    sound(SNOW_SYNTH, delay = 5)
                    next()
                    land()
                }
            )
        }
    }

    row("dbrow.agility_penguin_ice_tiles") {
        obstacle(
            PENGUIN,
            step = OFF_LAP,
            xp = 0,
            "loc.peng_agility_slippery_glitters01",
            "loc.peng_agility_slippery_glitters02",
            "loc.peng_agility_slippery_glitters03",
            "loc.peng_agility_slippery_glitters04",
            "loc.peng_agility_slippery_glitters05",
            "loc.peng_agility_slippery_glitters06",
            "loc.peng_agility_slippery_glitters07",
            "loc.peng_agility_slippery_glitters09",
        )
        apRange(2)
        val offTheIce =
            lane(tile(2664, 4070, 1)) {
                slide(tile(2664, 4069, 1))
                next()
                step(tile(2664, 4068, 1))
            }
        val steps = ICE_TILES.map { (start, dest) -> lane(start) { slide(dest) } }
        lanes(*steps.toTypedArray(), offTheIce)
    }

    // UNVERIFIED: which ice steps can slip and how often; one capture slips on this turn alone.
    row("dbrow.agility_penguin_ice_turn") {
        obstacle(PENGUIN, step = OFF_LAP, xp = 0, "loc.peng_agility_slippery_glitters05")
        lanes(lane(tile(2665, 4073, 1)) { slide(tile(2664, 4073, 1)) })
        fails(
            low = 180,
            high = 250,
            FallDamage(2, 2),
            lane(tile(2665, 4073, 1)) {
                message("You slip on the ice!")
                exactMove(
                    tile(2664, 4074, 1),
                    delay1 = 20,
                    delay2 = 50,
                    angle = 1023,
                    to = tile(2665, 4074, 1),
                    faceAngle = false,
                )
                anim(SLIP, delay = 2)
                sound(SLIP_SYNTH, delay = 2)
                next()
                hit()
            },
        )
    }

    row("dbrow.agility_penguin_ice") {
        obstacle(PENGUIN, step = 12, xp = 1800, "loc.peng_agility_slippery_glitters08")
        apRange(2)
        lanes(
            lane(tile(2664, 4071, 1)) {
                varbit(COURSE_STATE, 3)
                slide(tile(2664, 4070, 1))
            }
        )
    }

    row("dbrow.agility_penguin_sled_jumps") {
        obstacle(
            PENGUIN,
            step = OFF_LAP,
            xp = 0,
            "loc.peng_agility_obstaclejump01",
            "loc.peng_agility_obstaclejump02",
        )
        walkOn()
        val jumps = SLED_JUMPS.map { jump -> lane(jump.from) { sledJump(jump) } }
        val finish =
            lane(tile(2657, 4040, 1)) {
                next(2)
                step(tile(2657, 4038, 1))
                land()
            }
        lanes(*jumps.toTypedArray(), finish)
    }

    // UNVERIFIED: the gate pays on stepping through rather than up to it.
    row("dbrow.agility_penguin_gate") {
        obstacle(PENGUIN, step = 13, xp = 650, "loc.peng_agility_fencing_door")
        lanes(
            lane(tile(2652, 4039, 1)) {
                step(tile(2651, 4039, 1))
                sound(GATE_SYNTH)
                varbit(COURSE_STATE, 0)
                land()
            }
        )
    }
}

private fun DBRowBuilder.offLapObstacle() {
    columnRSCM(COURSE, PENGUIN)
    column(STEP, OFF_LAP)
    column(XP, 0)
}

private fun LaneBuilder.slide(dest: CoordGrid) {
    balance(SLIPPERY_WALK)
    step(dest)
    land()
    hold()
}

private fun LaneBuilder.sledJump(jump: SledJump) {
    next()
    exactMove(jump.to, delay1 = 30, delay2 = 49, FACE_SOUTH, faceAngle = false)
    anim(jump.seq, delay = jump.seqDelay)
    sound(SLED_JUMP_SYNTH, delay = 10)
    land()
    next(jump.sledTicks)
    sound(SLED_SYNTH)
}
