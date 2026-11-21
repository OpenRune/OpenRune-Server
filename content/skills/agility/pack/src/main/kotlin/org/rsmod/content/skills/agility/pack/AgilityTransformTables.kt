package org.rsmod.content.skills.agility.pack

import dev.openrune.definition.dbtables.DBTableBuilder
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import org.rsmod.map.square.MapSquareKey

internal fun agilityTransformTables() =
    listOf(AgilityTransformsTable.transforms(), AgilityPengCrushersTable.crushers())

/**
 * The forms a course turns its players into. `npc` is the form and `obj` the worn obj that gives
 * it, as the greegrees do; Larry puts players in the penguin suit, so it has none. A form lasts
 * while its player stays in `mapsquares` (the greegrees work only on Ape Atoll, the suit only near
 * Larry and the penguins: https://oldschool.runescape.wiki/w/Clockwork_suit). `course` is a course
 * that admits only players in the form, refusing others with `refusal`; the form swims in
 * `swim_area` and sleds in `slide_area`, each given by its south-west and north-east corners.
 */
object AgilityTransformsTable {
    const val NPC = 0
    const val OBJ = 1
    const val MAPSQUARES = 2
    const val COURSE = 3
    const val SWIM_AREA = 4
    const val SLIDE_AREA = 5
    const val REFUSAL = 6

    // UNVERIFIED: the Ape Atoll dungeons are left out.
    private val APE_ATOLL = listOf(square(42, 42), square(42, 43), square(43, 42), square(43, 43))

    private val PENGUINS =
        listOf(
            square(40, 51),
            square(41, 62),
            square(41, 63),
            square(41, 162),
        )

    fun transforms() =
        dbTable("dbtable.agility_transforms", serverOnly = true) {
            column("npc", NPC, VarType.NPC)
            column("obj", OBJ, VarType.OBJ)
            column("mapsquares", MAPSQUARES, VarType.INT)
            column("course", COURSE, VarType.DBROW)
            column("swim_area", SWIM_AREA, VarType.COORDGRID, VarType.COORDGRID)
            column("slide_area", SLIDE_AREA, VarType.COORDGRID, VarType.COORDGRID)
            column("refusal", REFUSAL, VarType.STRING)

            // UNVERIFIED: the npc the suit turns a player into; no capture names it.
            row("dbrow.agility_transform_penguin_suit") {
                columnRSCM(NPC, "npc.peng_suit_npc")
                column(MAPSQUARES, PENGUINS)
                columnRSCM(COURSE, "dbrow.agility_course_penguin")
                // UNVERIFIED: wording guessed; live keeps humans away from the course altogether.
                column(REFUSAL, "Only penguins can use this course.")
                // UNVERIFIED: the pool's and sled run's corners, where the stances change.
                column(SWIM_AREA, tile(2626, 4050, 0).packed, tile(2636, 4058, 0).packed)
                column(SLIDE_AREA, tile(2655, 4039, 1).packed, tile(2666, 4068, 1).packed)
            }
            greegree(
                "dbrow.agility_transform_small_ninja_monkey",
                "obj.mm_monkey_greegree_for_small_ninja_monkey",
                "npc.mm_transmogrification_small_ninja_monkey",
            )
            greegree(
                "dbrow.agility_transform_medium_ninja_monkey",
                "obj.mm_monkey_greegree_for_medium_ninja_monkey",
                "npc.mm_transmogrification_medium_ninja_monkey",
            )
            // UNVERIFIED: that the Kruk greegree takes Kruk's own npc.
            greegree("dbrow.agility_transform_kruk", "obj.mm2_kruk_greegree", "npc.mm_kruk")
        }

    private fun DBTableBuilder.greegree(row: String, obj: String, npc: String) {
        row(row) {
            columnRSCM(NPC, npc)
            columnRSCM(OBJ, obj)
            column(MAPSQUARES, APE_ATOLL)
        }
    }

    private fun square(x: Int, z: Int): Int = MapSquareKey(x, z).id
}

/**
 * The Penguin course's crushers, which slide out of the pool's sides by a tile for two ticks once
 * every `period` ticks, from the tick the game clock reaches `phase`, and push a player in the way
 * one tile further on, `push` tiles north (negative south). Periods and phases are as captured.
 */
object AgilityPengCrushersTable {
    const val NPC = 0
    const val PERIOD = 1
    const val PHASE = 2
    const val PUSH = 3

    fun crushers() =
        dbTable("dbtable.agility_peng_crushers", serverOnly = true) {
            column("npc", NPC, VarType.NPC)
            column("period", PERIOD, VarType.INT)
            column("phase", PHASE, VarType.INT)
            column("push", PUSH, VarType.INT)

            crusher(
                "dbrow.agility_peng_crusher_1",
                "npc.peng_agility_crushcourse_crushblock01_npc",
                period = 10,
                phase = 8,
                push = 1,
            )
            crusher(
                "dbrow.agility_peng_crusher_2",
                "npc.peng_agility_crushcourse_crushblock02_npc",
                period = 6,
                phase = 2,
                push = -1,
            )
            crusher(
                "dbrow.agility_peng_crusher_3",
                "npc.peng_agility_crushcourse_crushblock03_npc",
                period = 4,
                phase = 2,
                push = 1,
            )
            crusher(
                "dbrow.agility_peng_crusher_4",
                "npc.peng_agility_crushcourse_crushblock04_npc",
                period = 8,
                phase = 6,
                push = -1,
            )
        }

    private fun DBTableBuilder.crusher(
        row: String,
        npc: String,
        period: Int,
        phase: Int,
        push: Int,
    ) {
        row(row) {
            columnRSCM(NPC, npc)
            column(PERIOD, period)
            column(PHASE, phase)
            column(PUSH, push)
        }
    }
}
