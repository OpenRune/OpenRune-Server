package org.rsmod.content.skills.agility.pack.course

import dev.openrune.definition.dbtables.DBTableBuilder
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.REQUIREMENT_BARE_HEAD
import org.rsmod.content.skills.agility.pack.CLIMBING
import org.rsmod.content.skills.agility.pack.CLIMBING_SYNTH
import org.rsmod.content.skills.agility.pack.DOUBLE_PIPE_SQUEEZE
import org.rsmod.content.skills.agility.pack.FACE_EAST
import org.rsmod.content.skills.agility.pack.FACE_NORTH
import org.rsmod.content.skills.agility.pack.FACE_SOUTH
import org.rsmod.content.skills.agility.pack.HURDLE
import org.rsmod.content.skills.agility.pack.HURDLE_SYNTH
import org.rsmod.content.skills.agility.pack.JUMP_SYNTH
import org.rsmod.content.skills.agility.pack.LaneBuilder
import org.rsmod.content.skills.agility.pack.SQUEEZE_IN_SYNTH
import org.rsmod.content.skills.agility.pack.ZIPLINE
import org.rsmod.content.skills.agility.pack.apRange
import org.rsmod.content.skills.agility.pack.handIn
import org.rsmod.content.skills.agility.pack.landSpawn
import org.rsmod.content.skills.agility.pack.lane
import org.rsmod.content.skills.agility.pack.lanes
import org.rsmod.content.skills.agility.pack.npcObstacle
import org.rsmod.content.skills.agility.pack.obstacle
import org.rsmod.content.skills.agility.pack.requirement
import org.rsmod.content.skills.agility.pack.tile
import org.rsmod.map.CoordGrid

private const val WEREWOLF = "dbrow.agility_course_werewolf"

private const val JUMP_STONES = "seq.human_jump_stones"
private const val ZIP_BITE = "seq.zipline_bite"

private const val BITE_SYNTH = "synth.bite"
private const val DEATHSLIDE_FIRST_SYNTH = "synth.deathslide_first"
private const val DEATHSLIDE_SECOND_SYNTH = "synth.deathslide_second"

private const val TRAINER = "npc.werewolf_trainer_2"
private const val TRAINER_SHOUTS = "enum.agility_werewolf_obstacle_shouts"
private const val SLIDE_SHOUTS = "enum.agility_werewolf_slide_shouts"
private const val TRAINER_RANGE = 8

private val WEREWOLF_HURDLES =
    listOf(
        "dbrow.agility_werewolf_hurdle_1" to 9893,
        "dbrow.agility_werewolf_hurdle_2" to 9896,
        "dbrow.agility_werewolf_hurdle_3" to 9899,
    )
private val WEREWOLF_PIPES = listOf(3538, 3541, 3544)
private val WEREWOLF_STICK_LANDINGS = listOf(tile(3538, 9912, 0), tile(3543, 9911, 0))

private class WerewolfStone(
    val row: String,
    val from: CoordGrid,
    val to: CoordGrid,
    val angle: Int,
)

private val WEREWOLF_STONES =
    listOf(
        WerewolfStone(
            "dbrow.agility_werewolf_stepping_stone_1",
            tile(3538, 9873, 0),
            tile(3538, 9875, 0),
            FACE_NORTH,
        ),
        WerewolfStone(
            "dbrow.agility_werewolf_stepping_stone_2",
            tile(3538, 9875, 0),
            tile(3538, 9877, 0),
            FACE_NORTH,
        ),
        WerewolfStone(
            "dbrow.agility_werewolf_stepping_stone_3",
            tile(3538, 9877, 0),
            tile(3540, 9877, 0),
            FACE_EAST,
        ),
        WerewolfStone(
            "dbrow.agility_werewolf_stepping_stone_4",
            tile(3540, 9877, 0),
            tile(3540, 9879, 0),
            FACE_NORTH,
        ),
        WerewolfStone(
            "dbrow.agility_werewolf_stepping_stone_5",
            tile(3540, 9879, 0),
            tile(3540, 9881, 0),
            FACE_NORTH,
        ),
    )

// UNVERIFIED: the skull slope's walk style, the hurdles' hold, the trainer's wording for a
// missing stick, and the deathslide, which never fails.
internal fun DBTableBuilder.werewolfObstacles() {
    for ((index, stone) in WEREWOLF_STONES.withIndex()) {
        row(stone.row) {
            obstacle(WEREWOLF, step = index, xp = 100, "loc.werewolf_steping_stone")
            apRange(2)
            lanes(
                lane(stone.from) {
                    exactMove(stone.to, delay1 = 12, delay2 = 34, stone.angle, faceAngle = false)
                    anim(JUMP_STONES)
                    sound(JUMP_SYNTH, delay = 5)
                    land()
                    hold()
                }
            )
            if (index == 0) {
                landSpawn("obj.waa_stick", WEREWOLF_STICK_LANDINGS)
            }
        }
    }

    for ((index, hurdle) in WEREWOLF_HURDLES.withIndex()) {
        val (row, z) = hurdle
        row(row) {
            obstacle(
                WEREWOLF,
                step = WEREWOLF_STONES.size + index,
                xp = 200,
                "loc.werewolf_hurdle_mid",
                "loc.werewolf_hurdle_end",
                "loc.werewolf_hurdle_end_mirror",
            )
            lanes(
                *(3538..3541)
                    .map { x ->
                        lane(tile(x, z - 1, 0)) {
                            val dest = tile(x, z + 1, 0)
                            exactMove(dest, delay1 = 8, delay2 = 50, FACE_NORTH, faceAngle = false)
                            anim(HURDLE)
                            sound(HURDLE_SYNTH)
                            land()
                            hold()
                        }
                    }
                    .toTypedArray()
            )
        }
    }

    row("dbrow.agility_werewolf_pipe") {
        obstacle(WEREWOLF, step = 8, xp = 150, "loc.waa_pipe")
        lanes(*WEREWOLF_PIPES.map { x -> lane(tile(x, 9904, 0)) { pipe(x) } }.toTypedArray())
    }

    row("dbrow.agility_werewolf_skull_slope") {
        obstacle(WEREWOLF, step = 9, xp = 250, "loc.werewolf_skull_climb_1")
        lanes(
            *(9909..9911)
                .map { z ->
                    lane(tile(3533, z, 0)) {
                        npcSay(TRAINER, TRAINER_SHOUTS, TRAINER_RANGE)
                        balance(CLIMBING)
                        sound(CLIMBING_SYNTH, loops = 4, delay = 10)
                        step(tile(3532, z, 0))
                        next()
                        walkTo(tile(3530, z, 0))
                        next()
                        balanceOff()
                        land()
                    }
                }
                .toTypedArray()
        )
    }

    row("dbrow.agility_werewolf_zip_line") {
        obstacle(
            WEREWOLF,
            step = 10,
            xp = 2000,
            "loc.werewolf_slide_center",
            "loc.werewolf_slide_side",
            "loc.werewolf_slide_side_mirror",
        )
        requirement(REQUIREMENT_BARE_HEAD)
        lanes(lane(tile(3528, 9910, 0)) { deathslide() })
    }

    row("dbrow.agility_werewolf_stick") {
        npcObstacle(WEREWOLF, step = 11, xp = 3800, "npc.werewolf_trainer_stick")
        handIn(
            "obj.waa_stick",
            given = "You give the stick to the werewolf.",
            refused = "You don't have a stick to give the trainer.",
        )
    }
}

private fun LaneBuilder.pipe(x: Int) {
    npcSay(TRAINER, TRAINER_SHOUTS, TRAINER_RANGE)
    exactMove(tile(x, 9907, 0), delay1 = 24, delay2 = 120, FACE_NORTH, faceAngle = false)
    anim(DOUBLE_PIPE_SQUEEZE, delay = 20)
    sound(SQUEEZE_IN_SYNTH, delay = 7)
    next(4)
    exactMove(tile(x, 9910, 0), delay1 = 0, delay2 = 80, FACE_NORTH, faceAngle = false)
    anim(DOUBLE_PIPE_SQUEEZE)
    sound(SQUEEZE_IN_SYNTH, delay = 10)
    land()
    next(3)
    resetAnim()
}

private fun LaneBuilder.deathslide() {
    message("You bravely cling on to the death slide by your teeth...")
    npcSay(TRAINER, SLIDE_SHOUTS, TRAINER_RANGE)
    anim(ZIP_BITE)
    faceAngle(FACE_SOUTH)
    sound(BITE_SYNTH)
    next(2)
    say("WAAAAAARRRGGGHHHH!!!!!!")
    anim(ZIPLINE)
    sound(DEATHSLIDE_FIRST_SYNTH, delay = 15)
    for (z in 9905 downTo 9875 step 5) {
        if (z != 9905) {
            next()
        }
        exactMove(tile(3528, z, 0), delay1 = 0, delay2 = 30, FACE_SOUTH, faceAngle = false)
    }
    sound(DEATHSLIDE_SECOND_SYNTH)
    next()
    exactMove(tile(3528, 9873, 0), delay1 = 0, delay2 = 15, FACE_SOUTH, faceAngle = false)
    next()
    message("... and land safely on your feet.")
    resetAnim()
    land()
}
