package org.alter.impl.skills

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object Fishing {

    const val COL_SPOT_NPC = 0
    const val COL_LEVEL = 1
    const val COL_XP = 2
    const val COL_FISH_ITEM = 3
    const val COL_TOOL = 4
    const val COL_BAIT = 5
    const val COL_CATCH_RATE_LOW = 6
    const val COL_CATCH_RATE_HIGH = 7
    const val COL_SPOT_TYPE = 8
    const val COL_MEMBERS = 9
    const val COL_ANIMATION = 10

    fun fishingSpots() = dbTable("tables.fishing_spots", serverOnly = true) {

        column("spot_npc", COL_SPOT_NPC, VarType.NPC)
        column("level", COL_LEVEL, VarType.INT)
        column("xp", COL_XP, VarType.INT)
        column("fish_item", COL_FISH_ITEM, VarType.OBJ)
        column("tool", COL_TOOL, VarType.OBJ)
        column("bait", COL_BAIT, VarType.OBJ)
        column("catch_rate_low", COL_CATCH_RATE_LOW, VarType.INT)
        column("catch_rate_high", COL_CATCH_RATE_HIGH, VarType.INT)
        column("spot_type", COL_SPOT_TYPE, VarType.STRING)
        column("members", COL_MEMBERS, VarType.BOOLEAN)
        column("animation", COL_ANIMATION, VarType.SEQ)

        // Shrimps (level 1)
        row("dbrows.fishing_shrimps") {
            column(COL_LEVEL, 1)
            column(COL_XP, 10)
            columnRSCM(COL_FISH_ITEM, "items.raw_shrimps")
            columnRSCM(COL_TOOL, "items.small_fishing_net")
            column(COL_CATCH_RATE_LOW, 128)
            column(COL_CATCH_RATE_HIGH, 400)
            column(COL_SPOT_TYPE, "net")
            column(COL_MEMBERS, false)
        }

        // Sardine (level 5)
        row("dbrows.fishing_sardine") {
            column(COL_LEVEL, 5)
            column(COL_XP, 20)
            columnRSCM(COL_FISH_ITEM, "items.raw_sardine")
            columnRSCM(COL_TOOL, "items.fishing_rod")
            columnRSCM(COL_BAIT, "items.fishing_bait")
            column(COL_CATCH_RATE_LOW, 120)
            column(COL_CATCH_RATE_HIGH, 380)
            column(COL_SPOT_TYPE, "bait")
            column(COL_MEMBERS, false)
        }

        // Herring (level 10)
        row("dbrows.fishing_herring") {
            column(COL_LEVEL, 10)
            column(COL_XP, 30)
            columnRSCM(COL_FISH_ITEM, "items.raw_herring")
            columnRSCM(COL_TOOL, "items.fishing_rod")
            columnRSCM(COL_BAIT, "items.fishing_bait")
            column(COL_CATCH_RATE_LOW, 100)
            column(COL_CATCH_RATE_HIGH, 350)
            column(COL_SPOT_TYPE, "bait")
            column(COL_MEMBERS, false)
        }

        // Anchovies (level 15)
        row("dbrows.fishing_anchovies") {
            column(COL_LEVEL, 15)
            column(COL_XP, 40)
            columnRSCM(COL_FISH_ITEM, "items.raw_anchovies")
            columnRSCM(COL_TOOL, "items.small_fishing_net")
            column(COL_CATCH_RATE_LOW, 90)
            column(COL_CATCH_RATE_HIGH, 320)
            column(COL_SPOT_TYPE, "net")
            column(COL_MEMBERS, false)
        }

        // Mackerel (level 16)
        row("dbrows.fishing_mackerel") {
            column(COL_LEVEL, 16)
            column(COL_XP, 20)
            columnRSCM(COL_FISH_ITEM, "items.raw_mackerel")
            columnRSCM(COL_TOOL, "items.big_fishing_net")
            column(COL_CATCH_RATE_LOW, 100)
            column(COL_CATCH_RATE_HIGH, 350)
            column(COL_SPOT_TYPE, "big_net")
            column(COL_MEMBERS, true)
        }

        // Trout (level 20)
        row("dbrows.fishing_trout") {
            column(COL_LEVEL, 20)
            column(COL_XP, 50)
            columnRSCM(COL_FISH_ITEM, "items.raw_trout")
            columnRSCM(COL_TOOL, "items.fly_fishing_rod")
            columnRSCM(COL_BAIT, "items.feather")
            column(COL_CATCH_RATE_LOW, 80)
            column(COL_CATCH_RATE_HIGH, 300)
            column(COL_SPOT_TYPE, "lure")
            column(COL_MEMBERS, false)
        }

        // Cod (level 23)
        row("dbrows.fishing_cod") {
            column(COL_LEVEL, 23)
            column(COL_XP, 45)
            columnRSCM(COL_FISH_ITEM, "items.raw_cod")
            columnRSCM(COL_TOOL, "items.big_fishing_net")
            column(COL_CATCH_RATE_LOW, 70)
            column(COL_CATCH_RATE_HIGH, 280)
            column(COL_SPOT_TYPE, "big_net")
            column(COL_MEMBERS, true)
        }

        // Pike (level 25)
        row("dbrows.fishing_pike") {
            column(COL_LEVEL, 25)
            column(COL_XP, 60)
            columnRSCM(COL_FISH_ITEM, "items.raw_pike")
            columnRSCM(COL_TOOL, "items.fishing_rod")
            columnRSCM(COL_BAIT, "items.fishing_bait")
            column(COL_CATCH_RATE_LOW, 60)
            column(COL_CATCH_RATE_HIGH, 250)
            column(COL_SPOT_TYPE, "bait")
            column(COL_MEMBERS, false)
        }

        // Salmon (level 30)
        row("dbrows.fishing_salmon") {
            column(COL_LEVEL, 30)
            column(COL_XP, 70)
            columnRSCM(COL_FISH_ITEM, "items.raw_salmon")
            columnRSCM(COL_TOOL, "items.fly_fishing_rod")
            columnRSCM(COL_BAIT, "items.feather")
            column(COL_CATCH_RATE_LOW, 50)
            column(COL_CATCH_RATE_HIGH, 220)
            column(COL_SPOT_TYPE, "lure")
            column(COL_MEMBERS, false)
        }

        // Tuna (level 35)
        row("dbrows.fishing_tuna") {
            column(COL_LEVEL, 35)
            column(COL_XP, 80)
            columnRSCM(COL_FISH_ITEM, "items.raw_tuna")
            columnRSCM(COL_TOOL, "items.harpoon")
            column(COL_CATCH_RATE_LOW, 30)
            column(COL_CATCH_RATE_HIGH, 200)
            column(COL_SPOT_TYPE, "harpoon")
            column(COL_MEMBERS, false)
        }

        // Lobster (level 40)
        row("dbrows.fishing_lobster") {
            column(COL_LEVEL, 40)
            column(COL_XP, 90)
            columnRSCM(COL_FISH_ITEM, "items.raw_lobster")
            columnRSCM(COL_TOOL, "items.lobster_pot")
            column(COL_CATCH_RATE_LOW, 20)
            column(COL_CATCH_RATE_HIGH, 175)
            column(COL_SPOT_TYPE, "cage")
            column(COL_MEMBERS, false)
        }

        // Bass (level 46)
        row("dbrows.fishing_bass") {
            column(COL_LEVEL, 46)
            column(COL_XP, 100)
            columnRSCM(COL_FISH_ITEM, "items.raw_bass")
            columnRSCM(COL_TOOL, "items.big_fishing_net")
            column(COL_CATCH_RATE_LOW, 20)
            column(COL_CATCH_RATE_HIGH, 150)
            column(COL_SPOT_TYPE, "big_net")
            column(COL_MEMBERS, true)
        }

        // Swordfish (level 50)
        row("dbrows.fishing_swordfish") {
            column(COL_LEVEL, 50)
            column(COL_XP, 100)
            columnRSCM(COL_FISH_ITEM, "items.raw_swordfish")
            columnRSCM(COL_TOOL, "items.harpoon")
            column(COL_CATCH_RATE_LOW, 10)
            column(COL_CATCH_RATE_HIGH, 130)
            column(COL_SPOT_TYPE, "harpoon")
            column(COL_MEMBERS, false)
        }

        // Lava eel (level 53)
        row("dbrows.fishing_lava_eel") {
            column(COL_LEVEL, 53)
            column(COL_XP, 30)
            columnRSCM(COL_FISH_ITEM, "items.raw_lava_eel")
            columnRSCM(COL_TOOL, "items.oily_fishing_rod")
            columnRSCM(COL_BAIT, "items.fishing_bait")
            column(COL_CATCH_RATE_LOW, 30)
            column(COL_CATCH_RATE_HIGH, 200)
            column(COL_SPOT_TYPE, "bait")
            column(COL_MEMBERS, true)
        }

        // Monkfish (level 62)
        row("dbrows.fishing_monkfish") {
            column(COL_LEVEL, 62)
            column(COL_XP, 120)
            columnRSCM(COL_FISH_ITEM, "items.raw_monkfish")
            columnRSCM(COL_TOOL, "items.small_fishing_net")
            column(COL_CATCH_RATE_LOW, 10)
            column(COL_CATCH_RATE_HIGH, 100)
            column(COL_SPOT_TYPE, "net")
            column(COL_MEMBERS, true)
        }

        // Karambwan (level 65)
        row("dbrows.fishing_karambwan") {
            column(COL_LEVEL, 65)
            column(COL_XP, 105)
            columnRSCM(COL_FISH_ITEM, "items.raw_karambwan")
            columnRSCM(COL_TOOL, "items.karambwan_vessel")
            columnRSCM(COL_BAIT, "items.raw_karambwanji")
            column(COL_CATCH_RATE_LOW, 15)
            column(COL_CATCH_RATE_HIGH, 120)
            column(COL_SPOT_TYPE, "vessel")
            column(COL_MEMBERS, true)
        }

        // Shark (level 76)
        row("dbrows.fishing_shark") {
            column(COL_LEVEL, 76)
            column(COL_XP, 110)
            columnRSCM(COL_FISH_ITEM, "items.raw_shark")
            columnRSCM(COL_TOOL, "items.harpoon")
            column(COL_CATCH_RATE_LOW, 5)
            column(COL_CATCH_RATE_HIGH, 80)
            column(COL_SPOT_TYPE, "harpoon")
            column(COL_MEMBERS, true)
        }

        // Infernal eel (level 80)
        row("dbrows.fishing_infernal_eel") {
            column(COL_LEVEL, 80)
            column(COL_XP, 95)
            columnRSCM(COL_FISH_ITEM, "items.raw_infernal_eel")
            columnRSCM(COL_TOOL, "items.oily_fishing_rod")
            columnRSCM(COL_BAIT, "items.fishing_bait")
            column(COL_CATCH_RATE_LOW, 8)
            column(COL_CATCH_RATE_HIGH, 70)
            column(COL_SPOT_TYPE, "bait")
            column(COL_MEMBERS, true)
        }

        // Anglerfish (level 82)
        row("dbrows.fishing_anglerfish") {
            column(COL_LEVEL, 82)
            column(COL_XP, 120)
            columnRSCM(COL_FISH_ITEM, "items.raw_anglerfish")
            columnRSCM(COL_TOOL, "items.fishing_rod")
            columnRSCM(COL_BAIT, "items.fishing_bait")
            column(COL_CATCH_RATE_LOW, 3)
            column(COL_CATCH_RATE_HIGH, 60)
            column(COL_SPOT_TYPE, "bait")
            column(COL_MEMBERS, true)
        }

        // Dark crab (level 85)
        row("dbrows.fishing_dark_crab") {
            column(COL_LEVEL, 85)
            column(COL_XP, 130)
            columnRSCM(COL_FISH_ITEM, "items.raw_dark_crab")
            columnRSCM(COL_TOOL, "items.lobster_pot")
            columnRSCM(COL_BAIT, "items.dark_fishing_bait")
            column(COL_CATCH_RATE_LOW, 2)
            column(COL_CATCH_RATE_HIGH, 50)
            column(COL_SPOT_TYPE, "cage")
            column(COL_MEMBERS, true)
        }

        // Sacred eel (level 87)
        row("dbrows.fishing_sacred_eel") {
            column(COL_LEVEL, 87)
            column(COL_XP, 105)
            columnRSCM(COL_FISH_ITEM, "items.raw_sacred_eel")
            columnRSCM(COL_TOOL, "items.fishing_rod")
            columnRSCM(COL_BAIT, "items.fishing_bait")
            column(COL_CATCH_RATE_LOW, 5)
            column(COL_CATCH_RATE_HIGH, 55)
            column(COL_SPOT_TYPE, "bait")
            column(COL_MEMBERS, true)
        }
    }

    const val TOOL_ITEM = 0
    const val TOOL_TYPE = 1
    const val TOOL_SPEED_MOD = 2
    const val TOOL_ANIMATION = 3

    fun fishingTools() = dbTable("tables.fishing_tools", serverOnly = true) {

        column("tool_item", TOOL_ITEM, VarType.OBJ)
        column("tool_type", TOOL_TYPE, VarType.STRING)
        column("tool_speed_mod", TOOL_SPEED_MOD, VarType.INT)
        column("tool_animation", TOOL_ANIMATION, VarType.SEQ)

        row("dbrows.fishing_tool_small_fishing_net") {
            columnRSCM(TOOL_ITEM, "items.small_fishing_net")
            column(TOOL_TYPE, "net")
            column(TOOL_SPEED_MOD, 100)
            columnRSCM(TOOL_ANIMATION, "sequences.human_net_fishing")
        }

        row("dbrows.fishing_tool_fishing_rod") {
            columnRSCM(TOOL_ITEM, "items.fishing_rod")
            column(TOOL_TYPE, "bait")
            column(TOOL_SPEED_MOD, 100)
            columnRSCM(TOOL_ANIMATION, "sequences.human_rod_fishing")
        }

        row("dbrows.fishing_tool_fly_fishing_rod") {
            columnRSCM(TOOL_ITEM, "items.fly_fishing_rod")
            column(TOOL_TYPE, "lure")
            column(TOOL_SPEED_MOD, 100)
            columnRSCM(TOOL_ANIMATION, "sequences.human_lure_fishing")
        }

        row("dbrows.fishing_tool_harpoon") {
            columnRSCM(TOOL_ITEM, "items.harpoon")
            column(TOOL_TYPE, "harpoon")
            column(TOOL_SPEED_MOD, 100)
            columnRSCM(TOOL_ANIMATION, "sequences.human_harpoon_fishing")
        }

        row("dbrows.fishing_tool_lobster_pot") {
            columnRSCM(TOOL_ITEM, "items.lobster_pot")
            column(TOOL_TYPE, "cage")
            column(TOOL_SPEED_MOD, 100)
            columnRSCM(TOOL_ANIMATION, "sequences.human_cage_fishing")
        }

        row("dbrows.fishing_tool_big_fishing_net") {
            columnRSCM(TOOL_ITEM, "items.big_fishing_net")
            column(TOOL_TYPE, "big_net")
            column(TOOL_SPEED_MOD, 100)
            columnRSCM(TOOL_ANIMATION, "sequences.human_bignet_fishing")
        }

        row("dbrows.fishing_tool_dragon_harpoon") {
            columnRSCM(TOOL_ITEM, "items.dragon_harpoon")
            column(TOOL_TYPE, "harpoon")
            column(TOOL_SPEED_MOD, 80)
            columnRSCM(TOOL_ANIMATION, "sequences.human_harpoon_fishing")
        }

        row("dbrows.fishing_tool_infernal_harpoon") {
            columnRSCM(TOOL_ITEM, "items.infernal_harpoon")
            column(TOOL_TYPE, "harpoon")
            column(TOOL_SPEED_MOD, 80)
            columnRSCM(TOOL_ANIMATION, "sequences.human_harpoon_fishing")
        }

        row("dbrows.fishing_tool_crystal_harpoon") {
            columnRSCM(TOOL_ITEM, "items.crystal_harpoon")
            column(TOOL_TYPE, "harpoon")
            column(TOOL_SPEED_MOD, 65)
            columnRSCM(TOOL_ANIMATION, "sequences.human_harpoon_fishing")
        }

        row("dbrows.fishing_tool_barbarian_rod") {
            columnRSCM(TOOL_ITEM, "items.barbarian_rod")
            column(TOOL_TYPE, "barb")
            column(TOOL_SPEED_MOD, 100)
            columnRSCM(TOOL_ANIMATION, "sequences.human_rod_fishing")
        }

        row("dbrows.fishing_tool_karambwan_vessel") {
            columnRSCM(TOOL_ITEM, "items.karambwan_vessel")
            column(TOOL_TYPE, "vessel")
            column(TOOL_SPEED_MOD, 100)
            columnRSCM(TOOL_ANIMATION, "sequences.human_rod_fishing")
        }

        row("dbrows.fishing_tool_oily_fishing_rod") {
            columnRSCM(TOOL_ITEM, "items.oily_fishing_rod")
            column(TOOL_TYPE, "bait")
            column(TOOL_SPEED_MOD, 100)
            columnRSCM(TOOL_ANIMATION, "sequences.human_rod_fishing")
        }
    }
}
