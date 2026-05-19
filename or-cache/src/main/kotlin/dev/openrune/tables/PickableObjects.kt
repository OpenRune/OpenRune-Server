package dev.openrune.tables

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType


object PickableObjects {

    const val LOCS = 0
    const val COL_ITEM_GIVEN = 1
    const val COL_ITEM_GIVEN_AMOUNT = 2
    const val DESPAWN_CHANCE = 3
    const val RESPAWN_TICK_TIME = 4
    const val MESSAGES = 5
    const val SEED = 6
    const val OBJECT_CYCLE = 7
    const val REPLACEMENT_LOC = 8
    const val FORCES_WALK = 9

    fun pickableObjects() = dbTable("dbtable.pickable_objects", serverOnly = true) {

        column("objects", LOCS, VarType.LOC)
        column("itemGiven", COL_ITEM_GIVEN, VarType.OBJ)
        column("itemAmount", COL_ITEM_GIVEN_AMOUNT, VarType.INT)
        column("despawnChance", DESPAWN_CHANCE, VarType.INT)
        column("respawnTime", RESPAWN_TICK_TIME, VarType.INT)
        column("messages", MESSAGES, VarType.INT)
        column("seed", SEED, VarType.OBJ)
        column("objectCycle", OBJECT_CYCLE, VarType.BOOLEAN)
        column("replacementLoc", REPLACEMENT_LOC, VarType.LOC)
        column("forcesWalk", FORCES_WALK, VarType.BOOLEAN)

        row("dbrow.pickable_flax") {
            columnRSCM(LOCS, "loc.misc_flax_heavyweeds", "loc.flax", "loc.misc_flax_medweeds", "loc.misc_flax_lightweeds", "loc.misc_flax_noweeds")
            columnRSCM(COL_ITEM_GIVEN, "obj.flax")
            column(COL_ITEM_GIVEN_AMOUNT, 1)
            column(DESPAWN_CHANCE, 3,16)
            column(RESPAWN_TICK_TIME, 10)
            column(OBJECT_CYCLE, false)
            column(FORCES_WALK, false)
        }

        row("dbrow.pickable_banana") {
            columnRSCM(LOCS, "loc.bananatreefull", "loc.bananatreefour", "loc.bananatreethree", "loc.bananatreetwo", "loc.bananatreeone")
            columnRSCM(REPLACEMENT_LOC, "loc.bananatreeempty")
            columnRSCM(COL_ITEM_GIVEN, "obj.banana")
            column(COL_ITEM_GIVEN_AMOUNT, 1)
            column(RESPAWN_TICK_TIME, 500)
            column(OBJECT_CYCLE, true)
            column(FORCES_WALK, false)
        }

        row("dbrow.pickable_cabbage") {
            columnRSCM(LOCS, "loc.cabbage","loc.brain_farm_cabbage","loc.avium_cabbage01","loc.avium_cabbage02")
            columnRSCM(COL_ITEM_GIVEN, "obj.cabbage")
            column(COL_ITEM_GIVEN_AMOUNT, 1)
            column(RESPAWN_TICK_TIME, 30)
            columnRSCM(SEED, "obj.cabbage_seed")
            column(OBJECT_CYCLE, false)
            column(FORCES_WALK, true)
        }

        row("dbrow.pickable_cadava_bush") {
            columnRSCM(LOCS, "loc.fai_varrock_cadavabush_2","loc.fai_varrock_cadavabush_1")
            columnRSCM(REPLACEMENT_LOC, "loc.fai_varrock_cadavabush_0")
            columnRSCM(COL_ITEM_GIVEN, "obj.cadavaberries")
            column(COL_ITEM_GIVEN_AMOUNT, 1)
            column(RESPAWN_TICK_TIME, 200)
            column(OBJECT_CYCLE, true)
            column(FORCES_WALK, false)
        }

        row("dbrow.pickable_caerula_bush") {
            columnRSCM(LOCS, "loc.caerula_bush")
            columnRSCM(COL_ITEM_GIVEN, "obj.caerula_berries")
            column(COL_ITEM_GIVEN_AMOUNT, 1)
            column(RESPAWN_TICK_TIME, -1)
            column(OBJECT_CYCLE, false)
            column(FORCES_WALK, false)
        }

        row("dbrow.pickable_damiana_bush") {
            columnRSCM(LOCS, "loc.damiana_shrub")
            columnRSCM(COL_ITEM_GIVEN, "obj.damiana_leaves")
            column(COL_ITEM_GIVEN_AMOUNT, 1)
            column(RESPAWN_TICK_TIME, -1)
            column(OBJECT_CYCLE, false)
            column(FORCES_WALK, false)
        }

        row("dbrow.pickable_nettles") {
            columnRSCM(LOCS, "loc.nettles","loc.nettles1","loc.nettles2","loc.nettles3","loc.nettles4","loc.nettles5","loc.nettles6")
            columnRSCM(COL_ITEM_GIVEN, "obj.nettles_picked")
            column(COL_ITEM_GIVEN_AMOUNT, 1)
            column(RESPAWN_TICK_TIME, 13)
            column(OBJECT_CYCLE, false)
            column(FORCES_WALK, false)
        }

        row("dbrow.pickable_onion") {
            columnRSCM(LOCS, "loc.onion","loc.avium_onion01","loc.avium_onion02")
            columnRSCM(COL_ITEM_GIVEN, "obj.onion")
            columnRSCM(SEED, "obj.onion_seed")
            column(COL_ITEM_GIVEN_AMOUNT, 1)
            column(RESPAWN_TICK_TIME, 50)
            column(OBJECT_CYCLE, false)
            column(FORCES_WALK, true)
        }

        row("dbrow.pickable_pineapple_brimhaven") {
            columnRSCM(LOCS, "loc.pineapple_plant","loc.pineapple_plant_four","loc.pineapple_plant_three","loc.pineapple_plant_two","loc.pineapple_plant_one")
            columnRSCM(REPLACEMENT_LOC, "loc.pineapple_plant_no_pineapples")
            columnRSCM(COL_ITEM_GIVEN, "obj.pineapple")
            column(COL_ITEM_GIVEN_AMOUNT, 1)
            column(RESPAWN_TICK_TIME, 500)
            column(OBJECT_CYCLE, true)
            column(FORCES_WALK, false)
        }

        row("dbrow.pickable_pineapple_ape_atoll") {
            columnRSCM(LOCS, "loc.mm_pineapple_plant_one")
            columnRSCM(REPLACEMENT_LOC,"loc.pineapple_plant_no_pineapples")
            columnRSCM(COL_ITEM_GIVEN, "obj.pineapple")
            column(COL_ITEM_GIVEN_AMOUNT, 1)
            column(RESPAWN_TICK_TIME, 500)
            column(OBJECT_CYCLE, false)
            column(FORCES_WALK, false)
        }

        row("dbrow.pickable_potato") {
            columnRSCM(LOCS, "loc.potato")
            columnRSCM(COL_ITEM_GIVEN, "obj.potato")
            columnRSCM(SEED, "obj.potato_seed")
            column(COL_ITEM_GIVEN_AMOUNT, 1)
            column(RESPAWN_TICK_TIME, 50)
            column(OBJECT_CYCLE, false)
            column(FORCES_WALK, true)
        }

        row("dbrow.pickable_redberry") {
            columnRSCM(LOCS, "loc.fai_varrock_redberrybush_2","loc.fai_varrock_redberrybush_1")
            columnRSCM(REPLACEMENT_LOC, "loc.fai_varrock_redberrybush_0")
            columnRSCM(COL_ITEM_GIVEN, "obj.redberries")
            column(COL_ITEM_GIVEN_AMOUNT, 1)
            column(RESPAWN_TICK_TIME, 200)
            column(OBJECT_CYCLE, true)
            column(FORCES_WALK, false)
        }

        row("dbrow.pickable_sweetcorn_1") {
            columnRSCM(LOCS, "loc.avium_corn01")
            columnRSCM(REPLACEMENT_LOC, "loc.avium_corn01_picked")
            columnRSCM(COL_ITEM_GIVEN, "obj.sweetcorn")
            columnRSCM(SEED, "obj.sweetcorn_seed")
            column(COL_ITEM_GIVEN_AMOUNT, 1)
            column(RESPAWN_TICK_TIME, 100)
            column(OBJECT_CYCLE, false)
            column(FORCES_WALK, false)
        }

        row("dbrow.pickable_sweetcorn_2") {
            columnRSCM(LOCS, "loc.avium_corn02")
            columnRSCM(REPLACEMENT_LOC, "loc.avium_corn02_picked")
            columnRSCM(COL_ITEM_GIVEN, "obj.sweetcorn")
            columnRSCM(SEED, "obj.sweetcorn_seed")
            column(COL_ITEM_GIVEN_AMOUNT, 1)
            column(RESPAWN_TICK_TIME, 100)
            column(OBJECT_CYCLE, false)
            column(FORCES_WALK, false)
        }


        row("dbrow.wheat") {
            columnRSCM(LOCS,
                "loc.wheat",
                "loc.fai_varrock_wheat",
                "loc.fai_varrock_wheat_corner",
                "loc.fai_varrock_wheat_small",
                "loc.brain_farm_wheat",
                "loc.brain_wheat_small",
                "loc.brain_wheat_smallest",
                "loc.wheat_tall",
                "loc.wheat_small",
                "loc.wheat_smallest",
                "loc.brain_wheat",
                "loc.brain_wheat_tall"
            )
            columnRSCM(COL_ITEM_GIVEN, "obj.grain")
            column(COL_ITEM_GIVEN_AMOUNT, 1)
            column(RESPAWN_TICK_TIME, 20)
            column(OBJECT_CYCLE, false)
            column(FORCES_WALK, true)
        }

    }
}
