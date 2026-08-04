package dev.openrune.tables.skills

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object Mining {

    const val COL_ROCK_OBJECT = 0
    const val COL_LEVEL = 1
    const val COL_XP = 2
    const val COL_ORE_ITEM = 3
    const val COL_RESPAWN_CYCLES = 4
    const val COL_SUCCESS_RATE_LOW = 5
    const val COL_SUCCESS_RATE_HIGH = 6
    const val COL_DEPLETE_MECHANIC = 7
    const val COL_EMPTY_ROCK = 8
    const val COL_CLUE_BASE_CHANCE = 9
    const val COL_DEPLETE_MIN = 10
    const val COL_DEPLETE_MAX = 11
    const val COL_MINING_WALL = 12
    const val COL_MINING_GLOVES = 13
    const val COL_VARROCK_ARMOUR = 14
    const val COL_MINING_CAPE = 15
    const val COL_CELESTIAL_RING = 16

    fun rocks() =
        dbTable("dbtable.mining_rocks", serverOnly = true) {
            column("rock_object", COL_ROCK_OBJECT, VarType.LOC)
            column("level", COL_LEVEL, VarType.INT)
            column("xp", COL_XP, VarType.INT)
            column("ore_item", COL_ORE_ITEM, VarType.OBJ)
            column("respawn_cycles", COL_RESPAWN_CYCLES, VarType.INT)
            column("success_rate_low", COL_SUCCESS_RATE_LOW, VarType.INT)
            column("success_rate_high", COL_SUCCESS_RATE_HIGH, VarType.INT)
            column("deplete_mechanic", COL_DEPLETE_MECHANIC, VarType.INT)
            column("empty_rock_object", COL_EMPTY_ROCK, VarType.LOC)
            column("clue_base_chance", COL_CLUE_BASE_CHANCE, VarType.INT)
            column("deplete_min_amount", COL_DEPLETE_MIN, VarType.INT)
            column("deplete_max_amount", COL_DEPLETE_MAX, VarType.INT)
            column("mining_wall", COL_MINING_WALL, VarType.BOOLEAN)
            column("mining_gloves", COL_MINING_GLOVES, VarType.INT)
            column("varrock_armour_level", COL_VARROCK_ARMOUR, VarType.INT)
            column("mining_cape", COL_MINING_CAPE, VarType.BOOLEAN)
            column("celestial_ring", COL_CELESTIAL_RING, VarType.BOOLEAN)

            row("dbrow.mining_clayrock") {
                columnRSCM(COL_ROCK_OBJECT, "loc.clayrock1", "loc.clayrock2")
                column(COL_LEVEL, 1)
                column(COL_XP, 50)
                columnRSCM(COL_ORE_ITEM, "obj.clay")
                column(COL_RESPAWN_CYCLES, 2)
                column(COL_SUCCESS_RATE_LOW, 128)
                column(COL_SUCCESS_RATE_HIGH, 400)
                column(COL_DEPLETE_MECHANIC, 1)
                columnRSCM(COL_EMPTY_ROCK, "loc.rocks2")
                column(COL_CLUE_BASE_CHANCE, 741600)
                column(COL_MINING_WALL, false)
                column(COL_MINING_GLOVES, 0)
                column(COL_VARROCK_ARMOUR, 1)
                column(COL_MINING_CAPE, true)
                column(COL_CELESTIAL_RING, true)
            }

            row("dbrow.mining_copperrock") {
                columnRSCM(COL_ROCK_OBJECT, "loc.copperrock1", "loc.copperrock2")
                column(COL_LEVEL, 1)
                column(COL_XP, 175)
                columnRSCM(COL_ORE_ITEM, "obj.copper_ore")
                column(COL_RESPAWN_CYCLES, 4)
                column(COL_SUCCESS_RATE_LOW, 100)
                column(COL_SUCCESS_RATE_HIGH, 350)
                column(COL_DEPLETE_MECHANIC, 1)
                columnRSCM(COL_EMPTY_ROCK, "loc.rocks2")
                column(COL_CLUE_BASE_CHANCE, 741600)
                column(COL_MINING_WALL, false)
                column(COL_MINING_GLOVES, 1)
                column(COL_VARROCK_ARMOUR, 1)
                column(COL_MINING_CAPE, true)
                column(COL_CELESTIAL_RING, true)
            }

            row("dbrow.mining_tinrock") {
                columnRSCM(COL_ROCK_OBJECT, "loc.tinrock1", "loc.tinrock2")
                column(COL_LEVEL, 1)
                column(COL_XP, 175)
                columnRSCM(COL_ORE_ITEM, "obj.tin_ore")
                column(COL_RESPAWN_CYCLES, 4)
                column(COL_SUCCESS_RATE_LOW, 100)
                column(COL_SUCCESS_RATE_HIGH, 350)
                column(COL_DEPLETE_MECHANIC, 1)
                columnRSCM(COL_EMPTY_ROCK, "loc.rocks2")
                column(COL_CLUE_BASE_CHANCE, 741600)
                column(COL_MINING_WALL, false)
                column(COL_MINING_GLOVES, 1)
                column(COL_VARROCK_ARMOUR, 1)
                column(COL_MINING_CAPE, true)
                column(COL_CELESTIAL_RING, true)
            }

            row("dbrow.mining_bluriterock") {
                columnRSCM(COL_ROCK_OBJECT, "loc.blurite_rock_1", "loc.blurite_rock_2")
                column(COL_LEVEL, 10)
                column(COL_XP, 175)
                columnRSCM(COL_ORE_ITEM, "obj.blurite_ore")
                column(COL_RESPAWN_CYCLES, 42)
                column(COL_SUCCESS_RATE_LOW, 90)
                column(COL_SUCCESS_RATE_HIGH, 350)
                column(COL_DEPLETE_MECHANIC, 1)
                columnRSCM(COL_EMPTY_ROCK, "loc.rocks2")
                column(COL_CLUE_BASE_CHANCE, 741600)
                column(COL_MINING_WALL, false)
                column(COL_MINING_GLOVES, 0)
                column(COL_VARROCK_ARMOUR, 1)
                column(COL_MINING_CAPE, true)
                column(COL_CELESTIAL_RING, true)
            }

            row("dbrow.mining_ironrock") {
                columnRSCM(COL_ROCK_OBJECT, "loc.ironrock1", "loc.ironrock2")
                column(COL_LEVEL, 15)
                column(COL_XP, 350)
                columnRSCM(COL_ORE_ITEM, "obj.iron_ore")
                column(COL_RESPAWN_CYCLES, 9)
                column(COL_SUCCESS_RATE_LOW, 96)
                column(COL_SUCCESS_RATE_HIGH, 350)
                column(COL_DEPLETE_MECHANIC, 1)
                columnRSCM(COL_EMPTY_ROCK, "loc.rocks2")
                column(COL_CLUE_BASE_CHANCE, 741600)
                column(COL_MINING_WALL, false)
                column(COL_MINING_GLOVES, 1)
                column(COL_VARROCK_ARMOUR, 1)
                column(COL_MINING_CAPE, true)
                column(COL_CELESTIAL_RING, true)
            }

            row("dbrow.mining_silverrock") {
                columnRSCM(COL_ROCK_OBJECT, "loc.silverrock1", "loc.silverrock2")
                column(COL_LEVEL, 20)
                column(COL_XP, 400)
                columnRSCM(COL_ORE_ITEM, "obj.silver_ore")
                column(COL_RESPAWN_CYCLES, 100)
                column(COL_SUCCESS_RATE_LOW, 25)
                column(COL_SUCCESS_RATE_HIGH, 200)
                column(COL_DEPLETE_MECHANIC, 1)
                columnRSCM(COL_EMPTY_ROCK, "loc.rocks2")
                column(COL_CLUE_BASE_CHANCE, 741600)
                column(COL_MINING_WALL, false)
                column(COL_MINING_GLOVES, 1)
                column(COL_VARROCK_ARMOUR, 1)
                column(COL_MINING_CAPE, true)
                column(COL_CELESTIAL_RING, true)
            }

            row("dbrow.mining_leadrock") {
                columnRSCM(COL_ROCK_OBJECT, "loc.leadrock1")
                column(COL_LEVEL, 25)
                column(COL_XP, 405)
                columnRSCM(COL_ORE_ITEM, "obj.lead_ore")
                column(COL_RESPAWN_CYCLES, 10)
                column(COL_SUCCESS_RATE_LOW, 110)
                column(COL_SUCCESS_RATE_HIGH, 255)
                column(COL_DEPLETE_MECHANIC, 1)
                columnRSCM(COL_EMPTY_ROCK, "loc.leadrock1_empty")
                column(COL_CLUE_BASE_CHANCE, 290641)
                column(COL_MINING_WALL, false)
                column(COL_MINING_GLOVES, 1)
                column(COL_VARROCK_ARMOUR, 1)
                column(COL_MINING_CAPE, true)
                column(COL_CELESTIAL_RING, true)
            }

            row("dbrow.mining_coalrock") {
                columnRSCM(COL_ROCK_OBJECT, "loc.coalrock1", "loc.coalrock2")
                column(COL_LEVEL, 30)
                column(COL_XP, 500)
                columnRSCM(COL_ORE_ITEM, "obj.coal")
                column(COL_RESPAWN_CYCLES, 50)
                column(COL_SUCCESS_RATE_LOW, 16)
                column(COL_SUCCESS_RATE_HIGH, 100)
                column(COL_DEPLETE_MECHANIC, 1)
                columnRSCM(COL_EMPTY_ROCK, "loc.rocks2")
                column(COL_CLUE_BASE_CHANCE, 290640)
                column(COL_MINING_WALL, false)
                column(COL_MINING_GLOVES, 1)
                column(COL_VARROCK_ARMOUR, 1)
                column(COL_MINING_CAPE, true)
                column(COL_CELESTIAL_RING, true)
            }

            row("dbrow.mining_gemrock") {
                columnRSCM(COL_ROCK_OBJECT, "loc.gemrock1", "loc.gemrock")
                column(COL_LEVEL, 40)
                column(COL_XP, 650)
                column(COL_RESPAWN_CYCLES, 99)
                column(COL_SUCCESS_RATE_LOW, 28)
                column(COL_SUCCESS_RATE_HIGH, 70)
                column(COL_DEPLETE_MECHANIC, 1)
                columnRSCM(COL_EMPTY_ROCK, "loc.rocks1")
                column(COL_CLUE_BASE_CHANCE, 211866)
                column(COL_MINING_WALL, false)
                column(COL_MINING_GLOVES, 0)
                column(COL_VARROCK_ARMOUR, 0)
                column(COL_MINING_CAPE, false)
                column(COL_CELESTIAL_RING, false)
            }

            row("dbrow.mining_goldrock") {
                columnRSCM(COL_ROCK_OBJECT, "loc.goldrock1", "loc.goldrock2")
                column(COL_LEVEL, 40)
                column(COL_XP, 650)
                columnRSCM(COL_ORE_ITEM, "obj.gold_ore")
                column(COL_RESPAWN_CYCLES, 100)
                column(COL_SUCCESS_RATE_LOW, 7)
                column(COL_SUCCESS_RATE_HIGH, 75)
                column(COL_DEPLETE_MECHANIC, 1)
                columnRSCM(COL_EMPTY_ROCK, "loc.rocks2")
                column(COL_CLUE_BASE_CHANCE, 296640)
                column(COL_MINING_WALL, false)
                column(COL_MINING_GLOVES, 1)
                column(COL_VARROCK_ARMOUR, 1)
                column(COL_MINING_CAPE, true)
                column(COL_CELESTIAL_RING, true)
            }

            row("dbrow.mining_mithrilrock") {
                columnRSCM(COL_ROCK_OBJECT, "loc.mithrilrock1", "loc.mithrilrock2")
                column(COL_LEVEL, 55)
                column(COL_XP, 800)
                columnRSCM(COL_ORE_ITEM, "obj.mithril_ore")
                column(COL_RESPAWN_CYCLES, 200)
                column(COL_SUCCESS_RATE_LOW, 4)
                column(COL_SUCCESS_RATE_HIGH, 50)
                column(COL_DEPLETE_MECHANIC, 1)
                columnRSCM(COL_EMPTY_ROCK, "loc.rocks2")
                column(COL_CLUE_BASE_CHANCE, 148320)
                column(COL_MINING_WALL, false)
                column(COL_MINING_GLOVES, 3)
                column(COL_VARROCK_ARMOUR, 2)
                column(COL_MINING_CAPE, true)
                column(COL_CELESTIAL_RING, true)
            }

            row("dbrow.mining_lovakiterock") {
                columnRSCM(COL_ROCK_OBJECT, "loc.lovakite_rock1", "loc.lovakite_rock2")
                column(COL_LEVEL, 65)
                column(COL_XP, 600)
                columnRSCM(COL_ORE_ITEM, "obj.lovakite_ore")
                column(COL_RESPAWN_CYCLES, 59)
                column(COL_SUCCESS_RATE_LOW, 2)
                column(COL_SUCCESS_RATE_HIGH, 50)
                column(COL_DEPLETE_MECHANIC, 1)
                columnRSCM(COL_EMPTY_ROCK, "loc.rocks2")
                column(COL_CLUE_BASE_CHANCE, 245562)
                column(COL_MINING_WALL, false)
                column(COL_MINING_GLOVES, 0)
                column(COL_VARROCK_ARMOUR, 2)
                column(COL_MINING_CAPE, true)
                column(COL_CELESTIAL_RING, true)
            }

            row("dbrow.mining_adamantiterock") {
                columnRSCM(COL_ROCK_OBJECT, "loc.adamantiterock1", "loc.adamantiterock2")
                column(COL_LEVEL, 70)
                column(COL_XP, 950)
                columnRSCM(COL_ORE_ITEM, "obj.adamantite_ore")
                column(COL_RESPAWN_CYCLES, 400)
                column(COL_SUCCESS_RATE_LOW, 2)
                column(COL_SUCCESS_RATE_HIGH, 25)
                column(COL_DEPLETE_MECHANIC, 1)
                columnRSCM(COL_EMPTY_ROCK, "loc.rocks2")
                column(COL_CLUE_BASE_CHANCE, 59328)
                column(COL_MINING_WALL, false)
                column(COL_MINING_GLOVES, 3)
                column(COL_VARROCK_ARMOUR, 3)
                column(COL_MINING_CAPE, true)
                column(COL_CELESTIAL_RING, true)
            }

            row("dbrow.mining_nickelrock") {
                columnRSCM(COL_ROCK_OBJECT, "loc.nickelrock1")
                column(COL_LEVEL, 74)
                column(COL_XP, 805)
                columnRSCM(COL_ORE_ITEM, "obj.nickel_ore")
                column(COL_RESPAWN_CYCLES, 200)
                column(COL_SUCCESS_RATE_LOW, -1)
                column(COL_SUCCESS_RATE_HIGH, 25)
                column(COL_DEPLETE_MECHANIC, 1)
                columnRSCM(COL_EMPTY_ROCK, "loc.nickelrock1_empty")
                column(COL_CLUE_BASE_CHANCE, 59328)
                column(COL_MINING_WALL, false)
                column(COL_MINING_GLOVES, 0)
                column(COL_VARROCK_ARMOUR, 4)
                column(COL_MINING_CAPE, false)
                column(COL_CELESTIAL_RING, false)
            }

            row("dbrow.mining_runiterock") {
                columnRSCM(COL_ROCK_OBJECT, "loc.runiterock1", "loc.runiterock2")
                column(COL_LEVEL, 85)
                column(COL_XP, 1250)
                columnRSCM(COL_ORE_ITEM, "obj.runite_ore")
                column(COL_RESPAWN_CYCLES, 1200)
                column(COL_SUCCESS_RATE_LOW, 1)
                column(COL_SUCCESS_RATE_HIGH, 18)
                column(COL_DEPLETE_MECHANIC, 1)
                columnRSCM(COL_EMPTY_ROCK, "loc.rocks2")
                column(COL_CLUE_BASE_CHANCE, 42377)
                column(COL_MINING_WALL, false)
                column(COL_MINING_GLOVES, 2)
                column(COL_VARROCK_ARMOUR, 4)
                column(COL_MINING_CAPE, false)
                column(COL_CELESTIAL_RING, false)
            }

            row("dbrow.mining_amethystrock") {
                columnRSCM(COL_ROCK_OBJECT, "loc.amethystrock1", "loc.amethystrock2")
                column(COL_LEVEL, 92)
                column(COL_XP, 2400)
                columnRSCM(COL_ORE_ITEM, "obj.amethyst")
                column(COL_RESPAWN_CYCLES, 125)
                column(COL_SUCCESS_RATE_LOW, -18)
                column(COL_SUCCESS_RATE_HIGH, 10)
                column(COL_DEPLETE_MECHANIC, 2)
                columnRSCM(COL_EMPTY_ROCK, "loc.amethystrock_empty")
                column(COL_CLUE_BASE_CHANCE, 46350)
                column(COL_DEPLETE_MIN, 2)
                column(COL_DEPLETE_MAX, 3)
                column(COL_MINING_WALL, true)
                column(COL_MINING_GLOVES, 2)
                column(COL_VARROCK_ARMOUR, 4)
                column(COL_MINING_CAPE, false)
                column(COL_CELESTIAL_RING, false)
            }

            row("dbrow.mining_essence") {
                columnRSCM(COL_ROCK_OBJECT, "loc.blankrunestone")
                column(COL_LEVEL, 1)
                column(COL_XP, 50)
                columnRSCM(COL_ORE_ITEM, "obj.blankrune")
                column(COL_RESPAWN_CYCLES, 0)
                column(COL_SUCCESS_RATE_LOW, 256)
                column(COL_SUCCESS_RATE_HIGH, 256)
                column(COL_DEPLETE_MECHANIC, 3)
                column(COL_CLUE_BASE_CHANCE, 317647)
                column(COL_MINING_WALL, true)
                column(COL_MINING_GLOVES, 0)
                column(COL_VARROCK_ARMOUR, 0)
                column(COL_MINING_CAPE, false)
                column(COL_CELESTIAL_RING, false)
            }

            row("dbrow.mining_softclayrock") {
                columnRSCM(COL_ROCK_OBJECT, "loc.softclayrock1", "loc.softclayrock2")
                column(COL_LEVEL, 70)
                column(COL_XP, 50)
                columnRSCM(COL_ORE_ITEM, "obj.softclay")
                column(COL_RESPAWN_CYCLES, 2)
                column(COL_SUCCESS_RATE_LOW, 256)
                column(COL_SUCCESS_RATE_HIGH, 256)
                column(COL_DEPLETE_MECHANIC, 1)
                columnRSCM(COL_EMPTY_ROCK, "loc.rocks2")
                column(COL_CLUE_BASE_CHANCE, 148320)
                column(COL_MINING_WALL, false)
                column(COL_MINING_GLOVES, 0)
                column(COL_VARROCK_ARMOUR, 2)
                column(COL_MINING_CAPE, true)
                column(COL_CELESTIAL_RING, true)
            }

            row("dbrow.mining_limestonerock") {
                columnRSCM(
                    COL_ROCK_OBJECT,
                    "loc.limestone_rock1",
                    "loc.limestone_rock2",
                    "loc.limestone_rock3",
                )
                column(COL_LEVEL, 10)
                column(COL_XP, 265)
                columnRSCM(COL_ORE_ITEM, "obj.limestone")
                column(COL_RESPAWN_CYCLES, 6)
                column(COL_SUCCESS_RATE_LOW, 100)
                column(COL_SUCCESS_RATE_HIGH, 350)
                column(COL_DEPLETE_MECHANIC, 1)
                columnRSCM(COL_EMPTY_ROCK, "loc.limestone_rock_noore")
                column(COL_CLUE_BASE_CHANCE, 741600)
                column(COL_MINING_WALL, false)
                column(COL_MINING_GLOVES, 0)
                column(COL_VARROCK_ARMOUR, 1)
                column(COL_MINING_CAPE, true)
                column(COL_CELESTIAL_RING, true)
            }

            row("dbrow.mining_volcanicash") {
                columnRSCM(COL_ROCK_OBJECT, "loc.fossil_ashpile")
                column(COL_LEVEL, 22)
                column(COL_XP, 100)
                columnRSCM(COL_ORE_ITEM, "obj.fossil_volcanic_ash")
                column(COL_RESPAWN_CYCLES, 50)
                column(COL_SUCCESS_RATE_LOW, 50)
                column(COL_SUCCESS_RATE_HIGH, 200)
                column(COL_DEPLETE_MECHANIC, 1)
                columnRSCM(COL_EMPTY_ROCK, "loc.fossil_ashpile_empty")
                column(COL_CLUE_BASE_CHANCE, 741600)
                column(COL_MINING_WALL, false)
                column(COL_MINING_GLOVES, 0)
                column(COL_VARROCK_ARMOUR, 1)
                column(COL_MINING_CAPE, true)
                column(COL_CELESTIAL_RING, true)
            }

            row("dbrow.mining_sandstonerock") {
                columnRSCM(COL_ROCK_OBJECT, "loc.enakh_sandstone_rocks")
                column(COL_LEVEL, 35)
                column(COL_XP, 300)
                columnRSCM(COL_ORE_ITEM, "obj.enakh_sandstone_tiny")
                column(COL_RESPAWN_CYCLES, 6)
                column(COL_SUCCESS_RATE_LOW, 50)
                column(COL_SUCCESS_RATE_HIGH, 200)
                column(COL_DEPLETE_MECHANIC, 1)
                columnRSCM(COL_EMPTY_ROCK, "loc.rocks2")
                column(COL_CLUE_BASE_CHANCE, 500000)
                column(COL_MINING_WALL, false)
                column(COL_MINING_GLOVES, 0)
                column(COL_VARROCK_ARMOUR, 1)
                column(COL_MINING_CAPE, true)
                column(COL_CELESTIAL_RING, true)
            }

            row("dbrow.mining_denseessence") {
                columnRSCM(COL_ROCK_OBJECT, "loc.arceuus_runestone_top_mine")
                column(COL_LEVEL, 38)
                column(COL_XP, 120)
                columnRSCM(COL_ORE_ITEM, "obj.arceuus_essence_block")
                column(COL_RESPAWN_CYCLES, 0)
                column(COL_SUCCESS_RATE_LOW, 256)
                column(COL_SUCCESS_RATE_HIGH, 256)
                column(COL_DEPLETE_MECHANIC, 3)
                column(COL_CLUE_BASE_CHANCE, 317647)
                column(COL_MINING_WALL, true)
                column(COL_MINING_GLOVES, 0)
                column(COL_VARROCK_ARMOUR, 0)
                column(COL_MINING_CAPE, false)
                column(COL_CELESTIAL_RING, false)
            }

            row("dbrow.mining_volcanicsulphur") {
                columnRSCM(
                    COL_ROCK_OBJECT,
                    "loc.sulphur_rock_01",
                    "loc.sulphur_rock_02",
                    "loc.sulphur_rock_03",
                )
                column(COL_LEVEL, 42)
                column(COL_XP, 250)
                columnRSCM(COL_ORE_ITEM, "obj.lovakengj_sulphur")
                column(COL_RESPAWN_CYCLES, 20)
                column(COL_SUCCESS_RATE_LOW, 40)
                column(COL_SUCCESS_RATE_HIGH, 180)
                column(COL_DEPLETE_MECHANIC, 1)
                columnRSCM(COL_EMPTY_ROCK, "loc.rocks2")
                column(COL_CLUE_BASE_CHANCE, 400000)
                column(COL_MINING_WALL, false)
                column(COL_MINING_GLOVES, 0)
                column(COL_VARROCK_ARMOUR, 1)
                column(COL_MINING_CAPE, true)
                column(COL_CELESTIAL_RING, true)
            }

            row("dbrow.mining_graniterock") {
                columnRSCM(COL_ROCK_OBJECT, "loc.enakh_granite_rocks")
                column(COL_LEVEL, 45)
                column(COL_XP, 500)
                columnRSCM(COL_ORE_ITEM, "obj.enakh_granite_tiny")
                column(COL_RESPAWN_CYCLES, 6)
                column(COL_SUCCESS_RATE_LOW, 40)
                column(COL_SUCCESS_RATE_HIGH, 180)
                column(COL_DEPLETE_MECHANIC, 1)
                columnRSCM(COL_EMPTY_ROCK, "loc.rocks2")
                column(COL_CLUE_BASE_CHANCE, 400000)
                column(COL_MINING_WALL, false)
                column(COL_MINING_GLOVES, 0)
                column(COL_VARROCK_ARMOUR, 2)
                column(COL_MINING_CAPE, true)
                column(COL_CELESTIAL_RING, true)
            }

            row("dbrow.mining_rubiumrock") {
                columnRSCM(COL_ROCK_OBJECT, "loc.rubiumrock1")
                column(COL_LEVEL, 48)
                column(COL_XP, 300)
                columnRSCM(COL_ORE_ITEM, "obj.rubium_splinters")
                column(COL_RESPAWN_CYCLES, 50)
                column(COL_SUCCESS_RATE_LOW, 30)
                column(COL_SUCCESS_RATE_HIGH, 150)
                column(COL_DEPLETE_MECHANIC, 1)
                columnRSCM(COL_EMPTY_ROCK, "loc.rubiumrock1_empty")
                column(COL_CLUE_BASE_CHANCE, 300000)
                column(COL_MINING_WALL, false)
                column(COL_MINING_GLOVES, 0)
                column(COL_VARROCK_ARMOUR, 2)
                column(COL_MINING_CAPE, true)
                column(COL_CELESTIAL_RING, true)
            }

            row("dbrow.mining_lunarore") {
                columnRSCM(
                    COL_ROCK_OBJECT,
                    "loc.lunar_mine_stalagmite_twin",
                    "loc.lunar_mine_stalagmite_small",
                )
                column(COL_LEVEL, 60)
                column(COL_XP, 0)
                columnRSCM(COL_ORE_ITEM, "obj.quest_lunar_magic_ore")
                column(COL_RESPAWN_CYCLES, 100)
                column(COL_SUCCESS_RATE_LOW, 50)
                column(COL_SUCCESS_RATE_HIGH, 150)
                column(COL_DEPLETE_MECHANIC, 1)
                columnRSCM(COL_EMPTY_ROCK, "loc.rocks2")
                column(COL_CLUE_BASE_CHANCE, 0)
                column(COL_MINING_WALL, false)
                column(COL_MINING_GLOVES, 0)
                column(COL_VARROCK_ARMOUR, 0)
                column(COL_MINING_CAPE, false)
                column(COL_CELESTIAL_RING, false)
            }

            row("dbrow.mining_daeyaltshard") {
                columnRSCM(COL_ROCK_OBJECT, "loc.daeyalt_stone_top_active")
                column(COL_LEVEL, 60)
                column(COL_XP, 50)
                columnRSCM(COL_ORE_ITEM, "obj.daeyalt_shard")
                column(COL_RESPAWN_CYCLES, 10)
                column(COL_SUCCESS_RATE_LOW, 50)
                column(COL_SUCCESS_RATE_HIGH, 200)
                column(COL_DEPLETE_MECHANIC, 1)
                columnRSCM(COL_EMPTY_ROCK, "loc.daeyalt_stone_top")
                column(COL_CLUE_BASE_CHANCE, 200000)
                column(COL_MINING_WALL, false)
                column(COL_MINING_GLOVES, 0)
                column(COL_VARROCK_ARMOUR, 2)
                column(COL_MINING_CAPE, true)
                column(COL_CELESTIAL_RING, true)
            }

            row("dbrow.mining_rubiumgeode") {
                columnRSCM(COL_ROCK_OBJECT, "loc.rubiumdeposit1")
                column(COL_LEVEL, 68)
                column(COL_XP, 550)
                columnRSCM(COL_ORE_ITEM, "obj.rubium_geode")
                column(COL_RESPAWN_CYCLES, 50)
                column(COL_SUCCESS_RATE_LOW, 20)
                column(COL_SUCCESS_RATE_HIGH, 100)
                column(COL_DEPLETE_MECHANIC, 1)
                columnRSCM(COL_EMPTY_ROCK, "loc.rubiumdeposit1_empty")
                column(COL_CLUE_BASE_CHANCE, 200000)
                column(COL_MINING_WALL, false)
                column(COL_MINING_GLOVES, 0)
                column(COL_VARROCK_ARMOUR, 2)
                column(COL_MINING_CAPE, true)
                column(COL_CELESTIAL_RING, true)
            }

            row("dbrow.mining_saltrock_red") {
                columnRSCM(COL_ROCK_OBJECT, "loc.my2arm_saltrock_red")
                column(COL_LEVEL, 72)
                column(COL_XP, 50)
                columnRSCM(COL_ORE_ITEM, "obj.red_salt")
                column(COL_RESPAWN_CYCLES, 20)
                column(COL_SUCCESS_RATE_LOW, 50)
                column(COL_SUCCESS_RATE_HIGH, 200)
                column(COL_DEPLETE_MECHANIC, 1)
                columnRSCM(COL_EMPTY_ROCK, "loc.my2arm_saltrock_empty")
                column(COL_CLUE_BASE_CHANCE, 200000)
                column(COL_MINING_WALL, false)
                column(COL_MINING_GLOVES, 0)
                column(COL_VARROCK_ARMOUR, 3)
                column(COL_MINING_CAPE, true)
                column(COL_CELESTIAL_RING, true)
            }

            row("dbrow.mining_saltrock_blue") {
                columnRSCM(COL_ROCK_OBJECT, "loc.my2arm_saltrock_blue")
                column(COL_LEVEL, 72)
                column(COL_XP, 50)
                columnRSCM(COL_ORE_ITEM, "obj.blue_salt")
                column(COL_RESPAWN_CYCLES, 20)
                column(COL_SUCCESS_RATE_LOW, 50)
                column(COL_SUCCESS_RATE_HIGH, 200)
                column(COL_DEPLETE_MECHANIC, 1)
                columnRSCM(COL_EMPTY_ROCK, "loc.my2arm_saltrock_empty")
                column(COL_CLUE_BASE_CHANCE, 200000)
                column(COL_MINING_WALL, false)
                column(COL_MINING_GLOVES, 0)
                column(COL_VARROCK_ARMOUR, 3)
                column(COL_MINING_CAPE, true)
                column(COL_CELESTIAL_RING, true)
            }

            row("dbrow.mining_saltrock_green") {
                columnRSCM(COL_ROCK_OBJECT, "loc.my2arm_saltrock_green")
                column(COL_LEVEL, 72)
                column(COL_XP, 50)
                columnRSCM(COL_ORE_ITEM, "obj.green_salt")
                column(COL_RESPAWN_CYCLES, 20)
                column(COL_SUCCESS_RATE_LOW, 50)
                column(COL_SUCCESS_RATE_HIGH, 200)
                column(COL_DEPLETE_MECHANIC, 1)
                columnRSCM(COL_EMPTY_ROCK, "loc.my2arm_saltrock_empty")
                column(COL_CLUE_BASE_CHANCE, 200000)
                column(COL_MINING_WALL, false)
                column(COL_MINING_GLOVES, 0)
                column(COL_VARROCK_ARMOUR, 3)
                column(COL_MINING_CAPE, true)
                column(COL_CELESTIAL_RING, true)
            }

            row("dbrow.mining_saltrock_basalt") {
                columnRSCM(COL_ROCK_OBJECT, "loc.my2arm_saltrock_special")
                column(COL_LEVEL, 72)
                column(COL_XP, 50)
                columnRSCM(COL_ORE_ITEM, "obj.basalt")
                column(COL_RESPAWN_CYCLES, 20)
                column(COL_SUCCESS_RATE_LOW, 50)
                column(COL_SUCCESS_RATE_HIGH, 200)
                column(COL_DEPLETE_MECHANIC, 1)
                columnRSCM(COL_EMPTY_ROCK, "loc.my2arm_saltrock_empty")
                column(COL_CLUE_BASE_CHANCE, 200000)
                column(COL_MINING_WALL, false)
                column(COL_MINING_GLOVES, 0)
                column(COL_VARROCK_ARMOUR, 3)
                column(COL_MINING_CAPE, true)
                column(COL_CELESTIAL_RING, true)
            }

            row("dbrow.mining_ancientessence") {
                columnRSCM(COL_ROCK_OBJECT, "loc.ancient_essence_rock_active")
                column(COL_LEVEL, 75)
                column(COL_XP, 135)
                columnRSCM(COL_ORE_ITEM, "obj.ancient_essence")
                column(COL_RESPAWN_CYCLES, 40)
                column(COL_SUCCESS_RATE_LOW, 20)
                column(COL_SUCCESS_RATE_HIGH, 100)
                column(COL_DEPLETE_MECHANIC, 1)
                columnRSCM(COL_EMPTY_ROCK, "loc.ancient_essence_rock_empty")
                column(COL_CLUE_BASE_CHANCE, 150000)
                column(COL_MINING_WALL, false)
                column(COL_MINING_GLOVES, 0)
                column(COL_VARROCK_ARMOUR, 3)
                column(COL_MINING_CAPE, true)
                column(COL_CELESTIAL_RING, true)
            }

            row("dbrow.mining_infernalshale") {
                columnRSCM(
                    COL_ROCK_OBJECT,
                    "loc.varlamore_mining_rock",
                    "loc.varlamore_mining_rock02",
                    "loc.varlamore_mining_rock03",
                    "loc.varlamore_mining_rock04",
                )
                column(COL_LEVEL, 78)
                column(COL_XP, 550)
                columnRSCM(COL_ORE_ITEM, "obj.infernal_shale")
                column(COL_RESPAWN_CYCLES, 40)
                column(COL_SUCCESS_RATE_LOW, 15)
                column(COL_SUCCESS_RATE_HIGH, 80)
                column(COL_DEPLETE_MECHANIC, 1)
                columnRSCM(COL_EMPTY_ROCK, "loc.varlamore_mining_rock_empty")
                column(COL_CLUE_BASE_CHANCE, 100000)
                column(COL_MINING_WALL, false)
                column(COL_MINING_GLOVES, 0)
                column(COL_VARROCK_ARMOUR, 4)
                column(COL_MINING_CAPE, false)
                column(COL_CELESTIAL_RING, false)
            }
        }
}
