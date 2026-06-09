package dev.openrune.tables.skills.runecrafting

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.util.Coord

enum class AltarData(
    val ruins: List<String>? = null,
    val altar: String,
    val exitPortal: String? = null,
    val talisman: String? = null,
    val tiara: String? = null,
    val varbit: String? = null,
    val rune: Rune,
    val entrance: Int? = null,
    val exit: Int? = null,
    val option: String = "craft-rune",
    val row : String,
    val combo : List<CombinationRuneData> = emptyList()
) {
    AIR(
        ruins = listOf("loc.airtemple_ruined_old", "loc.airtemple_ruined_new"),
        altar = "loc.air_altar",
        exitPortal = "loc.airtemple_exit_portal",
        talisman = "obj.air_talisman",
        tiara = "dbrow.runecrafting_tiara_air",
        varbit = "varbit.rc_no_tally_required_air",
        rune = Rune.AIR,
        entrance = Coord(2841, 4830).pack(),
        exit = Coord(2983, 3288).pack(),
        row = "dbrow.runecrafting_altar_air",
        combo = listOf(
            CombinationRuneData.MIST_AIR,
            CombinationRuneData.SMOKE_AIR,
            CombinationRuneData.DUST_AIR,
        )
    ),
    MIND(
        ruins = listOf("loc.mindtemple_ruined_old", "loc.mindtemple_ruined_new"),
        altar = "loc.mind_altar",
        exitPortal = "loc.mindtemple_exit_portal",
        talisman = "obj.mind_talisman",
        tiara = "dbrow.runecrafting_tiara_mind",
        varbit = "varbit.rc_no_tally_required_mind",
        rune = Rune.MIND,
        entrance = Coord(2793, 4829).pack(),
        exit = Coord(2980, 3511).pack(),
        row = "dbrow.runecrafting_altar_mind"
    ),
    WATER(
        ruins = listOf("loc.watertemple_ruined_old", "loc.watertemple_ruined_new"),
        altar = "loc.water_altar",
        exitPortal = "loc.watertemple_exit_portal",
        talisman = "obj.water_talisman",
        tiara = "dbrow.runecrafting_tiara_water",
        varbit = "varbit.rc_no_tally_required_water",
        rune = Rune.WATER,
        entrance = Coord(2725, 4832).pack(),
        exit = Coord(3182, 3162).pack(),
        row = "dbrow.runecrafting_altar_water",
        combo = listOf(
            CombinationRuneData.MUD_WATER, CombinationRuneData.MIST_WATER,
            CombinationRuneData.STEAM_WATER)
    ),
    EARTH(
        ruins = listOf("loc.earthtemple_ruined_old", "loc.earthtemple_ruined_new"),
        altar = "loc.earth_altar",
        exitPortal = "loc.earthtemple_exit_portal",
        talisman = "obj.earth_talisman",
        tiara = "dbrow.runecrafting_tiara_earth",
        varbit = "varbit.rc_no_tally_required_earth",
        rune = Rune.EARTH,
        entrance = Coord(2657, 4830).pack(),
        exit = Coord(3302, 3477).pack(),
        row = "dbrow.runecrafting_altar_earth",
        combo = listOf(
            CombinationRuneData.DUST_EARTH, CombinationRuneData.MUD_EARTH,
            CombinationRuneData.LAVA_EARTH)
    ),
    FIRE(
        ruins = listOf("loc.firetemple_ruined_old", "loc.firetemple_ruined_new"),
        altar = "loc.fire_altar",
        exitPortal = "loc.firetemple_exit_portal",
        talisman = "obj.fire_talisman",
        tiara = "dbrow.runecrafting_tiara_fire",
        varbit = "varbit.rc_no_tally_required_fire",
        rune = Rune.FIRE,
        entrance = Coord(2576, 4848).pack(),
        exit = Coord(3310, 3252).pack(),
        row = "dbrow.runecrafting_altar_fire",
        combo = listOf(
            CombinationRuneData.LAVA_FIRE, CombinationRuneData.SMOKE_FIRE,
            CombinationRuneData.STEAM_FIRE)
    ),
    BODY(
        ruins = listOf("loc.bodytemple_ruined_old", "loc.bodytemple_ruined_new"),
        altar = "loc.body_altar",
        exitPortal = "loc.bodytemple_exit_portal",
        talisman = "obj.body_talisman",
        tiara = "dbrow.runecrafting_tiara_body",
        varbit = "varbit.rc_no_tally_required_body",
        rune = Rune.BODY,
        entrance = Coord(2519, 4847).pack(),
        exit = Coord(3050, 3442).pack(),
        row = "dbrow.runecrafting_altar_body"
    ),
    COSMIC(
        ruins = listOf("loc.cosmictemple_ruined_old", "loc.cosmictemple_ruined_new"),
        altar = "loc.cosmic_altar",
        exitPortal = "loc.cosmictemple_exit_portal",
        talisman = "obj.cosmic_talisman",
        tiara = "dbrow.runecrafting_tiara_cosmic",
        varbit = "varbit.rc_no_tally_required_cosmic",
        rune = Rune.COSMIC,
        entrance = Coord(2142, 4813).pack(),
        exit = Coord(2405, 4381).pack(),
        row = "dbrow.runecrafting_altar_cosmic"
    ),
    CHAOS(
        ruins = listOf("loc.chaostemple_ruined_old", "loc.chaostemple_ruined_new"),
        altar = "loc.chaos_altar",
        exitPortal = "loc.chaostemple_exit_portal",
        talisman = "obj.chaos_talisman",
        tiara = "dbrow.runecrafting_tiara_chaos",
        varbit = "varbit.rc_no_tally_required_chaos",
        rune = Rune.CHAOS,
        entrance = Coord(2280, 4837).pack(),
        exit = Coord(3060, 3585).pack(),
        row = "dbrow.runecrafting_altar_chaos"
    ),
    ASTRAL(
        altar = "loc.astral_altar",
        rune = Rune.ASTRAL,
        row = "dbrow.runecrafting_altar_astral"
    ),
    NATURE(
        ruins = listOf("loc.naturetemple_ruined_old", "loc.naturetemple_ruined_new"),
        altar = "loc.nature_altar",
        exitPortal = "loc.naturetemple_exit_portal",
        talisman = "obj.nature_talisman",
        tiara = "dbrow.runecrafting_tiara_nature",
        varbit = "varbit.rc_no_tally_required_nature",
        rune = Rune.NATURE,
        entrance = Coord(2400, 4835).pack(),
        exit = Coord(2865, 3022).pack(),
        row = "dbrow.runecrafting_altar_nature"
    ),
    LAW(
        ruins = listOf("loc.lawtemple_ruined_old", "loc.lawtemple_ruined_new"),
        altar = "loc.law_altar",
        exitPortal = "loc.lawtemple_exit_portal",
        talisman = "obj.law_talisman",
        tiara = "dbrow.runecrafting_tiara_law",
        varbit = "varbit.rc_no_tally_required_law",
        rune = Rune.LAW,
        entrance = Coord(2464, 4819).pack(),
        exit = Coord(2858, 3378).pack(),
        row = "dbrow.runecrafting_altar_law"
    ),
    DEATH(
        ruins = listOf("loc.deathtemple_ruined_old", "loc.deathtemple_ruined_new"),
        altar = "loc.death_altar",
        exitPortal = "loc.deathtemple_exit_portal",
        talisman = "obj.death_talisman",
        tiara = "dbrow.runecrafting_tiara_death",
        varbit = "varbit.rc_no_tally_required_death",
        rune = Rune.DEATH,
        entrance = Coord(2208, 4830).pack(),
        exit = Coord(1863, 4639).pack(),
        row = "dbrow.runecrafting_altar_death"
    ),
    SUNFIRE(
        altar = "loc.ralos_shrine",
        rune = Rune.SUNFIRE,
        row = "dbrow.runecrafting_altar_sunfire",
    ),
    BLOOD(
        altar = "loc.blood_altar",
        rune = Rune.BLOOD,
        option = "bind",
        row = "dbrow.runecrafting_altar_blood",
    ),
    BLOOD_KOUREND(
        altar = "loc.archeus_altar_blood",
        rune = Rune.BLOOD_DARK,
        option = "bind",
        row = "dbrow.runecrafting_altar_blood_kourend",
    ),
    SOUL(
        altar = "loc.archeus_altar_soul",
        rune = Rune.SOUL,
        option = "bind",
        row = "dbrow.runecrafting_altar_soul",
    ),
    OURANIA(
        altar = "loc.rc_zmi_dungeon_cracked_center_altar",
        rune = Rune.AIR,
        row = "dbrow.runecrafting_altar_ourania",
    ),
    WRATH(
        ruins = listOf("loc.wrathtemple_ruined_0op", "loc.wrathtemple_ruined_1op"),
        altar = "loc.wrath_altar",
        exitPortal = "loc.wrathtemple_exit_portal",
        talisman = "obj.wrath_talisman",
        tiara = "dbrow.runecrafting_tiara_wrath",
        varbit = "varbit.rc_no_tally_required_wrath",
        rune = Rune.WRATH,
        entrance = Coord(2335, 4826).pack(),
        exit = Coord(2447, 2822).pack(),
        row = "dbrow.runecrafting_altar_wrath"
    );

    companion object {
        val values = enumValues<AltarData>()
    }
}

object Alters {

    const val ALTAR_OBJECT = 0
    const val EXIT_PORTAL = 1
    const val TALISMAN = 2
    const val TIARA_ITEM = 3
    const val VARBIT = 4
    const val RUNE = 5
    const val ENTRANCE = 6
    const val EXIT = 7
    const val RUINS = 8
    const val COMBO = 9

    fun altars() = dbTable("dbtable.runecrafting_altars", serverOnly = true) {

        column("altar_object", ALTAR_OBJECT, VarType.LOC)
        column("exit_portal", EXIT_PORTAL, VarType.LOC)
        column("talisman", TALISMAN, VarType.OBJ)
        column("tiara", TIARA_ITEM, VarType.DBROW)
        column("varbit", VARBIT, VarType.INT)
        column("rune", RUNE, VarType.DBROW)
        column("entrance", ENTRANCE, VarType.COORDGRID)
        column("exit", EXIT, VarType.COORDGRID)
        column("ruins", RUINS, VarType.LOC)
        column("combo", COMBO, VarType.DBROW)

        AltarData.values.forEach {
            row(it.row) {
                columnRSCM(ALTAR_OBJECT, it.altar)
                columnRSCM(RUNE, it.rune.dbId)

                if (it.ruins != null) {
                    columnRSCM(RUINS, *it.ruins.toTypedArray())
                }

                if (it.exit != null) {
                    column(EXIT, it.exit)
                }

                if (it.exitPortal != null) {
                    columnRSCM(EXIT_PORTAL, it.exitPortal)
                }

                if (it.talisman != null) {
                    columnRSCM(TALISMAN, it.talisman)
                }

                if (it.tiara != null) {
                    columnRSCM(TIARA_ITEM, it.tiara)
                }

                if (it.entrance != null) {
                    column(ENTRANCE, it.entrance)
                }

                if (it.varbit != null) {
                    columnRSCM(VARBIT, it.varbit)
                }

                if (it.combo.isNotEmpty()) {
                    columnRSCM(COMBO, *it.combo.map { combo -> combo.row }.toTypedArray())
                }

            }
        }

    }

}
