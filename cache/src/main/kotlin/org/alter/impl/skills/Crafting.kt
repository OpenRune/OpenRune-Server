package org.alter.impl.skills

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object Crafting {

    // Spinning columns
    const val SPIN_INPUT_ITEM = 0
    const val SPIN_OUTPUT_ITEM = 1
    const val SPIN_LEVEL = 2
    const val SPIN_XP = 3

    fun craftingSpinning() = dbTable("tables.crafting_spinning", serverOnly = true) {

        column("input_item", SPIN_INPUT_ITEM, VarType.OBJ)
        column("output_item", SPIN_OUTPUT_ITEM, VarType.OBJ)
        column("level", SPIN_LEVEL, VarType.INT)
        column("xp", SPIN_XP, VarType.INT)

        // Wool (level 1)
        row("dbrows.crafting_spin_wool") {
            columnRSCM(SPIN_INPUT_ITEM, "items.wool")
            columnRSCM(SPIN_OUTPUT_ITEM, "items.ball_of_wool")
            column(SPIN_LEVEL, 1)
            column(SPIN_XP, 25)
        }

        // Flax (level 10)
        row("dbrows.crafting_spin_flax") {
            columnRSCM(SPIN_INPUT_ITEM, "items.flax")
            columnRSCM(SPIN_OUTPUT_ITEM, "items.bow_string")
            column(SPIN_LEVEL, 10)
            column(SPIN_XP, 150)
        }

        // Sinew (level 10)
        row("dbrows.crafting_spin_sinew") {
            columnRSCM(SPIN_INPUT_ITEM, "items.xbows_sinew")
            columnRSCM(SPIN_OUTPUT_ITEM, "items.xbows_crossbow_string")
            column(SPIN_LEVEL, 10)
            column(SPIN_XP, 150)
        }

        // Magic roots (level 19)
        row("dbrows.crafting_spin_magic") {
            columnRSCM(SPIN_INPUT_ITEM, "items.magic_roots")
            columnRSCM(SPIN_OUTPUT_ITEM, "items.magic_string")
            column(SPIN_LEVEL, 19)
            column(SPIN_XP, 300)
        }
    }

    // Gem cutting columns
    const val GEM_UNCUT = 0
    const val GEM_CUT = 1
    const val GEM_CRUSH_ITEM = 2
    const val GEM_LEVEL = 3
    const val GEM_XP = 4
    const val GEM_CRUSH_XP = 5

    fun craftingGems() = dbTable("tables.crafting_gems", serverOnly = true) {

        column("uncut", GEM_UNCUT, VarType.OBJ)
        column("cut", GEM_CUT, VarType.OBJ)
        column("crush_item", GEM_CRUSH_ITEM, VarType.OBJ)
        column("level", GEM_LEVEL, VarType.INT)
        column("xp", GEM_XP, VarType.INT)
        column("crush_xp", GEM_CRUSH_XP, VarType.INT)

        // Opal (level 1)
        row("dbrows.crafting_gem_opal") {
            columnRSCM(GEM_UNCUT, "items.uncut_opal")
            columnRSCM(GEM_CUT, "items.opal")
            columnRSCM(GEM_CRUSH_ITEM, "items.crushed_gemstone")
            column(GEM_LEVEL, 1)
            column(GEM_XP, 150)
            column(GEM_CRUSH_XP, 38)
        }

        // Jade (level 13)
        row("dbrows.crafting_gem_jade") {
            columnRSCM(GEM_UNCUT, "items.uncut_jade")
            columnRSCM(GEM_CUT, "items.jade")
            columnRSCM(GEM_CRUSH_ITEM, "items.crushed_gemstone")
            column(GEM_LEVEL, 13)
            column(GEM_XP, 200)
            column(GEM_CRUSH_XP, 50)
        }

        // Red topaz (level 16)
        row("dbrows.crafting_gem_topaz") {
            columnRSCM(GEM_UNCUT, "items.uncut_red_topaz")
            columnRSCM(GEM_CUT, "items.red_topaz")
            columnRSCM(GEM_CRUSH_ITEM, "items.crushed_gemstone")
            column(GEM_LEVEL, 16)
            column(GEM_XP, 250)
            column(GEM_CRUSH_XP, 63)
        }

        // Sapphire (level 20)
        row("dbrows.crafting_gem_sapphire") {
            columnRSCM(GEM_UNCUT, "items.uncut_sapphire")
            columnRSCM(GEM_CUT, "items.sapphire")
            column(GEM_LEVEL, 20)
            column(GEM_XP, 500)
            column(GEM_CRUSH_XP, 0)
        }

        // Emerald (level 27)
        row("dbrows.crafting_gem_emerald") {
            columnRSCM(GEM_UNCUT, "items.uncut_emerald")
            columnRSCM(GEM_CUT, "items.emerald")
            column(GEM_LEVEL, 27)
            column(GEM_XP, 675)
            column(GEM_CRUSH_XP, 0)
        }

        // Ruby (level 34)
        row("dbrows.crafting_gem_ruby") {
            columnRSCM(GEM_UNCUT, "items.uncut_ruby")
            columnRSCM(GEM_CUT, "items.ruby")
            column(GEM_LEVEL, 34)
            column(GEM_XP, 850)
            column(GEM_CRUSH_XP, 0)
        }

        // Diamond (level 43)
        row("dbrows.crafting_gem_diamond") {
            columnRSCM(GEM_UNCUT, "items.uncut_diamond")
            columnRSCM(GEM_CUT, "items.diamond")
            column(GEM_LEVEL, 43)
            column(GEM_XP, 1075)
            column(GEM_CRUSH_XP, 0)
        }

        // Dragonstone (level 55)
        row("dbrows.crafting_gem_dragonstone") {
            columnRSCM(GEM_UNCUT, "items.uncut_dragonstone")
            columnRSCM(GEM_CUT, "items.dragonstone")
            column(GEM_LEVEL, 55)
            column(GEM_XP, 1375)
            column(GEM_CRUSH_XP, 0)
        }

        // Onyx (level 67)
        row("dbrows.crafting_gem_onyx") {
            columnRSCM(GEM_UNCUT, "items.uncut_onyx")
            columnRSCM(GEM_CUT, "items.onyx")
            column(GEM_LEVEL, 67)
            column(GEM_XP, 1675)
            column(GEM_CRUSH_XP, 0)
        }

        // Zenyte (level 89)
        row("dbrows.crafting_gem_zenyte") {
            columnRSCM(GEM_UNCUT, "items.uncut_zenyte")
            columnRSCM(GEM_CUT, "items.zenyte")
            column(GEM_LEVEL, 89)
            column(GEM_XP, 2000)
            column(GEM_CRUSH_XP, 0)
        }
    }

    // Leather crafting columns
    const val LEATHER_OUTPUT = 0
    const val LEATHER_TYPE = 1
    const val LEATHER_AMOUNT = 2
    const val LEATHER_LEVEL = 3
    const val LEATHER_XP = 4
    const val LEATHER_THREAD_COST = 5

    fun craftingLeather() = dbTable("tables.crafting_leather", serverOnly = true) {

        column("output_item", LEATHER_OUTPUT, VarType.OBJ)
        column("leather_type", LEATHER_TYPE, VarType.OBJ)
        column("amount_needed", LEATHER_AMOUNT, VarType.INT)
        column("level", LEATHER_LEVEL, VarType.INT)
        column("xp", LEATHER_XP, VarType.INT)
        column("thread_cost", LEATHER_THREAD_COST, VarType.INT)

        // Leather gloves (level 1)
        row("dbrows.crafting_leather_gloves") {
            columnRSCM(LEATHER_OUTPUT, "items.leather_gloves")
            columnRSCM(LEATHER_TYPE, "items.leather")
            column(LEATHER_AMOUNT, 1)
            column(LEATHER_LEVEL, 1)
            column(LEATHER_XP, 138)
            column(LEATHER_THREAD_COST, 1)
        }

        // Leather boots (level 7)
        row("dbrows.crafting_leather_boots") {
            columnRSCM(LEATHER_OUTPUT, "items.leather_boots")
            columnRSCM(LEATHER_TYPE, "items.leather")
            column(LEATHER_AMOUNT, 1)
            column(LEATHER_LEVEL, 7)
            column(LEATHER_XP, 163)
            column(LEATHER_THREAD_COST, 1)
        }

        // Leather cowl (level 9)
        row("dbrows.crafting_leather_cowl") {
            columnRSCM(LEATHER_OUTPUT, "items.leather_cowl")
            columnRSCM(LEATHER_TYPE, "items.leather")
            column(LEATHER_AMOUNT, 1)
            column(LEATHER_LEVEL, 9)
            column(LEATHER_XP, 185)
            column(LEATHER_THREAD_COST, 1)
        }

        // Leather vambraces (level 11)
        row("dbrows.crafting_leather_vambraces") {
            columnRSCM(LEATHER_OUTPUT, "items.leather_vambraces")
            columnRSCM(LEATHER_TYPE, "items.leather")
            column(LEATHER_AMOUNT, 1)
            column(LEATHER_LEVEL, 11)
            column(LEATHER_XP, 220)
            column(LEATHER_THREAD_COST, 1)
        }

        // Leather body (level 14)
        row("dbrows.crafting_leather_body") {
            columnRSCM(LEATHER_OUTPUT, "items.leather_armour")
            columnRSCM(LEATHER_TYPE, "items.leather")
            column(LEATHER_AMOUNT, 1)
            column(LEATHER_LEVEL, 14)
            column(LEATHER_XP, 250)
            column(LEATHER_THREAD_COST, 1)
        }

        // Leather chaps (level 18)
        row("dbrows.crafting_leather_chaps") {
            columnRSCM(LEATHER_OUTPUT, "items.leather_chaps")
            columnRSCM(LEATHER_TYPE, "items.leather")
            column(LEATHER_AMOUNT, 1)
            column(LEATHER_LEVEL, 18)
            column(LEATHER_XP, 270)
            column(LEATHER_THREAD_COST, 1)
        }

        // Hardleather body (level 28)
        row("dbrows.crafting_hardleather_body") {
            columnRSCM(LEATHER_OUTPUT, "items.hardleather_body")
            columnRSCM(LEATHER_TYPE, "items.hard_leather")
            column(LEATHER_AMOUNT, 1)
            column(LEATHER_LEVEL, 28)
            column(LEATHER_XP, 350)
            column(LEATHER_THREAD_COST, 1)
        }
    }

    // Gold jewelry columns
    const val JEWELRY_GEM = 0
    const val JEWELRY_OUTPUT = 1
    const val JEWELRY_LEVEL = 2
    const val JEWELRY_XP = 3

    fun craftingJewelryGold() = dbTable("tables.crafting_jewelry_gold", serverOnly = true) {

        column("gem", JEWELRY_GEM, VarType.OBJ)
        column("output", JEWELRY_OUTPUT, VarType.OBJ)
        column("level", JEWELRY_LEVEL, VarType.INT)
        column("xp", JEWELRY_XP, VarType.INT)

        // Gold ring (level 5)
        row("dbrows.crafting_gold_ring") {
            columnRSCM(JEWELRY_OUTPUT, "items.gold_ring")
            column(JEWELRY_LEVEL, 5)
            column(JEWELRY_XP, 150)
        }

        // Gold necklace (level 6)
        row("dbrows.crafting_gold_necklace") {
            columnRSCM(JEWELRY_OUTPUT, "items.gold_necklace")
            column(JEWELRY_LEVEL, 6)
            column(JEWELRY_XP, 200)
        }

        // Gold amulet (level 8)
        row("dbrows.crafting_gold_amulet") {
            columnRSCM(JEWELRY_OUTPUT, "items.unstrung_gold_amulet")
            column(JEWELRY_LEVEL, 8)
            column(JEWELRY_XP, 300)
        }

        // Sapphire ring (level 20)
        row("dbrows.crafting_sapphire_ring") {
            columnRSCM(JEWELRY_GEM, "items.sapphire")
            columnRSCM(JEWELRY_OUTPUT, "items.sapphire_ring")
            column(JEWELRY_LEVEL, 20)
            column(JEWELRY_XP, 400)
        }

        // Sapphire necklace (level 22)
        row("dbrows.crafting_sapphire_necklace") {
            columnRSCM(JEWELRY_GEM, "items.sapphire")
            columnRSCM(JEWELRY_OUTPUT, "items.sapphire_necklace")
            column(JEWELRY_LEVEL, 22)
            column(JEWELRY_XP, 550)
        }

        // Sapphire amulet (level 24)
        row("dbrows.crafting_sapphire_amulet") {
            columnRSCM(JEWELRY_GEM, "items.sapphire")
            columnRSCM(JEWELRY_OUTPUT, "items.unstrung_sapphire_amulet")
            column(JEWELRY_LEVEL, 24)
            column(JEWELRY_XP, 650)
        }

        // Emerald ring (level 27)
        row("dbrows.crafting_emerald_ring") {
            columnRSCM(JEWELRY_GEM, "items.emerald")
            columnRSCM(JEWELRY_OUTPUT, "items.emerald_ring")
            column(JEWELRY_LEVEL, 27)
            column(JEWELRY_XP, 550)
        }

        // Emerald necklace (level 29)
        row("dbrows.crafting_emerald_necklace") {
            columnRSCM(JEWELRY_GEM, "items.emerald")
            columnRSCM(JEWELRY_OUTPUT, "items.emerald_necklace")
            column(JEWELRY_LEVEL, 29)
            column(JEWELRY_XP, 600)
        }

        // Ruby ring (level 34)
        row("dbrows.crafting_ruby_ring") {
            columnRSCM(JEWELRY_GEM, "items.ruby")
            columnRSCM(JEWELRY_OUTPUT, "items.ruby_ring")
            column(JEWELRY_LEVEL, 34)
            column(JEWELRY_XP, 700)
        }

        // Ruby necklace (level 40)
        row("dbrows.crafting_ruby_necklace") {
            columnRSCM(JEWELRY_GEM, "items.ruby")
            columnRSCM(JEWELRY_OUTPUT, "items.ruby_necklace")
            column(JEWELRY_LEVEL, 40)
            column(JEWELRY_XP, 750)
        }

        // Diamond ring (level 43)
        row("dbrows.crafting_diamond_ring") {
            columnRSCM(JEWELRY_GEM, "items.diamond")
            columnRSCM(JEWELRY_OUTPUT, "items.diamond_ring")
            column(JEWELRY_LEVEL, 43)
            column(JEWELRY_XP, 850)
        }

        // Diamond necklace (level 56)
        row("dbrows.crafting_diamond_necklace") {
            columnRSCM(JEWELRY_GEM, "items.diamond")
            columnRSCM(JEWELRY_OUTPUT, "items.diamond_necklace")
            column(JEWELRY_LEVEL, 56)
            column(JEWELRY_XP, 900)
        }

        // Dragonstone ring (level 55)
        row("dbrows.crafting_dragonstone_ring") {
            columnRSCM(JEWELRY_GEM, "items.dragonstone")
            columnRSCM(JEWELRY_OUTPUT, "items.dragonstone_ring")
            column(JEWELRY_LEVEL, 55)
            column(JEWELRY_XP, 1000)
        }

        // Dragonstone necklace (level 72)
        row("dbrows.crafting_dragonstone_necklace") {
            columnRSCM(JEWELRY_GEM, "items.dragonstone")
            columnRSCM(JEWELRY_OUTPUT, "items.dragonstone_necklace")
            column(JEWELRY_LEVEL, 72)
            column(JEWELRY_XP, 1050)
        }

        // Onyx ring (level 67)
        row("dbrows.crafting_onyx_ring") {
            columnRSCM(JEWELRY_GEM, "items.onyx")
            columnRSCM(JEWELRY_OUTPUT, "items.onyx_ring")
            column(JEWELRY_LEVEL, 67)
            column(JEWELRY_XP, 1150)
        }

        // Zenyte ring (level 89)
        row("dbrows.crafting_zenyte_ring") {
            columnRSCM(JEWELRY_GEM, "items.zenyte")
            columnRSCM(JEWELRY_OUTPUT, "items.zenyte_ring")
            column(JEWELRY_LEVEL, 89)
            column(JEWELRY_XP, 1500)
        }
    }

    // Glass blowing columns
    const val GLASS_OUTPUT = 0
    const val GLASS_LEVEL = 1
    const val GLASS_XP = 2

    fun craftingGlass() = dbTable("tables.crafting_glass", serverOnly = true) {

        column("output_item", GLASS_OUTPUT, VarType.OBJ)
        column("level", GLASS_LEVEL, VarType.INT)
        column("xp", GLASS_XP, VarType.INT)

        // Beer glass (level 1)
        row("dbrows.crafting_beer_glass") {
            columnRSCM(GLASS_OUTPUT, "items.beer_glass")
            column(GLASS_LEVEL, 1)
            column(GLASS_XP, 175)
        }

        // Candle lantern (level 4)
        row("dbrows.crafting_candle_lantern") {
            columnRSCM(GLASS_OUTPUT, "items.candle_lantern_empty")
            column(GLASS_LEVEL, 4)
            column(GLASS_XP, 190)
        }

        // Oil lamp (level 12)
        row("dbrows.crafting_oil_lamp") {
            columnRSCM(GLASS_OUTPUT, "items.oil_lamp_empty")
            column(GLASS_LEVEL, 12)
            column(GLASS_XP, 250)
        }

        // Vial (level 33)
        row("dbrows.crafting_vial") {
            columnRSCM(GLASS_OUTPUT, "items.vial_empty")
            column(GLASS_LEVEL, 33)
            column(GLASS_XP, 350)
        }

        // Fishbowl (level 42)
        row("dbrows.crafting_fishbowl") {
            columnRSCM(GLASS_OUTPUT, "items.fishbowl_empty")
            column(GLASS_LEVEL, 42)
            column(GLASS_XP, 425)
        }

        // Unpowered orb (level 46)
        row("dbrows.crafting_unpowered_orb") {
            columnRSCM(GLASS_OUTPUT, "items.stafforb")
            column(GLASS_LEVEL, 46)
            column(GLASS_XP, 525)
        }

        // Bullseye lantern lens (level 49)
        row("dbrows.crafting_lantern_lens") {
            columnRSCM(GLASS_OUTPUT, "items.bullseye_lantern_lens")
            column(GLASS_LEVEL, 49)
            column(GLASS_XP, 550)
        }
    }
}
