package org.alter.impl.skills.runecrafting

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.util.Coords

enum class AltarData(
    val ruins: List<String>,
    val altar: String,
    val exitPortal: String,
    val talisman: String,
    val tiara: String,
    val varbit: String,
    val rune: Rune,
    val entrance: Int,
    val exit: Int,
    val option: String = "craft-rune",
) {
    AIR(
        ruins = listOf("objects.airtemple_ruined_old", "objects.airtemple_ruined_new"),
        altar = "objects.air_altar",
        exitPortal = "objects.airtemple_exit_portal",
        talisman = "items.air_talisman",
        tiara = "items.tiara_air",
        varbit = "varbits.rc_no_tally_required_air",
        rune = Rune.AIR,
        entrance = Coords(2841, 4830),
        exit = Coords(2983, 3288),
    ),
    MIND(
        ruins = listOf("objects.mindtemple_ruined_old", "objects.mindtemple_ruined_new"),
        altar = "objects.mind_altar",
        exitPortal = "objects.mindtemple_exit_portal",
        talisman = "items.mind_talisman",
        tiara = "items.tiara_mind",
        varbit = "varbits.rc_no_tally_required_mind",
        rune = Rune.MIND,
        entrance = Coords(2793, 4829),
        exit = Coords(2980, 3511),
    ),
    WATER(
        ruins = listOf(Objs.MYSTERIOUS_RUINS_29096, Objs.MYSTERIOUS_RUINS_29097),
        altar = Objs.ALTAR_34762,
        exitPortal = Objs.PORTAL_34750,
        talisman = Items.WATER_TALISMAN,
        tiara = Items.WATER_TIARA,
        varbit = 609,
        rune = Rune.WATER,
        entrance = Coords(2725, 4832),
        exit = Coords(3182, 3162),
    ),
    EARTH(
        ruins = listOf(Objs.MYSTERIOUS_RUINS_29098, Objs.MYSTERIOUS_RUINS_29099),
        altar = Objs.ALTAR_34763,
        exitPortal = Objs.PORTAL_34751,
        talisman = Items.EARTH_TALISMAN,
        tiara = Items.EARTH_TIARA,
        varbit = 610,
        rune = Rune.EARTH,
        entrance = Coords(2657, 4830),
        exit = Coords(3302, 3477),
    ),
    FIRE(
        ruins = listOf(Objs.MYSTERIOUS_RUINS_30371, Objs.MYSTERIOUS_RUINS_30372),
        altar = Objs.ALTAR_34764,
        exitPortal = Objs.PORTAL_34752,
        talisman = Items.FIRE_TALISMAN,
        tiara = Items.FIRE_TIARA,
        varbit = 611,
        rune = Rune.FIRE,
        entrance = Coords(2576, 4848),
        exit = Coords(3310, 3252),
    ),
    BODY(
        ruins = listOf(Objs.MYSTERIOUS_RUINS_30373, Objs.MYSTERIOUS_RUINS_31584),
        altar = Objs.ALTAR_34765,
        exitPortal = Objs.PORTAL_34753,
        talisman = Items.BODY_TALISMAN,
        tiara = Items.BODY_TIARA,
        varbit = 612,
        rune = Rune.BODY,
        entrance = Coords(2519, 4847),
        exit = Coords(3050, 3442),
    ),
    COSMIC(
        ruins = listOf(Objs.MYSTERIOUS_RUINS_31607, Objs.MYSTERIOUS_RUINS_31725),
        altar = Objs.ALTAR_34766,
        exitPortal = Objs.PORTAL_34754,
        talisman = Items.COSMIC_TALISMAN,
        tiara = Items.COSMIC_TIARA,
        varbit = 613,
        rune = Rune.COSMIC,
        entrance = Coords(2142, 4813),
        exit = Coords(2405, 4381),
    ),
    CHAOS(
        ruins = listOf(Objs.MYSTERIOUS_RUINS_34742, Objs.MYSTERIOUS_RUINS_34743),
        altar = Objs.ALTAR_34769,
        exitPortal = Objs.PORTAL_34757,
        talisman = Items.CHAOS_TALISMAN,
        tiara = Items.CHAOS_TIARA,
        varbit = 616,
        rune = Rune.CHAOS,
        entrance = Coords(2280, 4837),
        exit = Coords(3060, 3585),
    ),
    ASTRAL(altar = Objs.ALTAR_34771, rune = Rune.ASTRAL),
    NATURE(
        ruins = listOf(Objs.MYSTERIOUS_RUINS_32491, Objs.MYSTERIOUS_RUINS_32492),
        altar = Objs.ALTAR_34768,
        exitPortal = Objs.PORTAL_34756,
        talisman = Items.NATURE_TALISMAN,
        tiara = Items.NATURE_TIARA,
        varbit = 615,
        rune = Rune.NATURE,
        entrance = Coords(2400, 4835),
        exit = Coords(2865, 3022),
    ),
    LAW(
        ruins = listOf(Objs.MYSTERIOUS_RUINS_32489, Objs.MYSTERIOUS_RUINS_32490),
        altar = Objs.ALTAR_34767,
        exitPortal = Objs.PORTAL_34755,
        talisman = Items.LAW_TALISMAN,
        tiara = Items.LAW_TIARA,
        varbit = 614,
        rune = Rune.LAW,
        entrance = Coords(2464, 4819),
        exit = Coords(2858, 3378),
    ),
    DEATH(
        ruins = listOf(Objs.MYSTERIOUS_RUINS_34744, Objs.MYSTERIOUS_RUINS_34745),
        altar = Objs.ALTAR_34770,
        exitPortal = Objs.PORTAL_34758,
        talisman = Items.DEATH_TALISMAN,
        tiara = Items.DEATH_TIARA,
        varbit = 617,
        rune = Rune.DEATH,
        entrance = Coords(2208, 4830),
        exit = Coords(1863, 4639),
    ),
    BLOOD(altar = Objs.BLOOD_ALTAR, rune = Rune.BLOOD, option = "bind"),
    SOUL(altar = Objs.SOUL_ALTAR, rune = Rune.SOUL, option = "bind"),
    WRATH(
        ruins = listOf(Objs.MYSTERIOUS_RUINS_34746, Objs.MYSTERIOUS_RUINS_34747),
        altar = Objs.ALTAR_34772,
        exitPortal = Objs.PORTAL_34759,
        talisman = Items.WRATH_TALISMAN,
        tiara = Items.WRATH_TIARA,
        varbit = 6220,
        rune = Rune.WRATH,
        entrance = Coords(2335, 4826),
        exit = Coords(2447, 2822),
    ),
    ;

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

    fun altars() = dbTable("tables.runecrafting_altars") {

        column("altar_object", ALTAR_OBJECT, VarType.LOC)
        column("exit_portal", EXIT_PORTAL, VarType.LOC)
        column("talisman", TALISMAN, VarType.OBJ)
        column("tiara", TIARA_ITEM, VarType.OBJ)
        column("varbit", VARBIT, VarType.INT)
        column("rune", RUNE, VarType.DBROW)
        column("entrance", ENTRANCE, VarType.COORDGRID)
        column("exit", EXIT, VarType.COORDGRID)
        column("ruins", RUINS, VarType.LOC)


    }

}