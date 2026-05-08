package dev.openrune.tables.skills

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object Herblore {

    const val COL_GRIMY = 0
    const val COL_CLEAN = 1
    const val COL_UNFINISHED = 2
    const val COL_LEVEL = 3
    const val COL_XP = 4

    fun herbs() = dbTable("dbtable.herblore_herbs", serverOnly = true) {

        column("grimy", COL_GRIMY, VarType.OBJ)
        column("clean", COL_CLEAN, VarType.OBJ)
        column("unfinished", COL_UNFINISHED, VarType.OBJ)
        column("level", COL_LEVEL, VarType.INT)
        column("xp", COL_XP, VarType.INT)

        row("dbrow.herblore_herb_guam") {
            columnRSCM(COL_GRIMY, "obj.unidentified_guam")
            columnRSCM(COL_CLEAN, "obj.guam_leaf")
            columnRSCM(COL_UNFINISHED, "obj.guamvial")
            column(COL_LEVEL, 3)
            column(COL_XP, 25)
        }

        row("dbrow.herblore_herb_marrentill") {
            columnRSCM(COL_GRIMY, "obj.unidentified_marentill")
            columnRSCM(COL_CLEAN, "obj.marentill")
            columnRSCM(COL_UNFINISHED, "obj.marrentillvial")
            column(COL_LEVEL, 5)
            column(COL_XP, 38)
        }

        row("dbrow.herblore_herb_tarromin") {
            columnRSCM(COL_GRIMY, "obj.unidentified_tarromin")
            columnRSCM(COL_CLEAN, "obj.tarromin")
            columnRSCM(COL_UNFINISHED, "obj.tarrominvial")
            column(COL_LEVEL, 11)
            column(COL_XP, 50)
        }

        row("dbrow.herblore_herb_harralander") {
            columnRSCM(COL_GRIMY, "obj.unidentified_harralander")
            columnRSCM(COL_CLEAN, "obj.harralander")
            columnRSCM(COL_UNFINISHED, "obj.harralandervial")
            column(COL_LEVEL, 20)
            column(COL_XP, 63)
        }

        row("dbrow.herblore_herb_ranarr") {
            columnRSCM(COL_GRIMY, "obj.unidentified_ranarr")
            columnRSCM(COL_CLEAN, "obj.ranarr_weed")
            columnRSCM(COL_UNFINISHED, "obj.ranarrvial")
            column(COL_LEVEL, 25)
            column(COL_XP, 75)
        }

        row("dbrow.herblore_herb_toadflax") {
            columnRSCM(COL_GRIMY, "obj.unidentified_toadflax")
            columnRSCM(COL_CLEAN, "obj.toadflax")
            columnRSCM(COL_UNFINISHED, "obj.toadflaxvial")
            column(COL_LEVEL, 30)
            column(COL_XP, 80)
        }

        row("dbrow.herblore_herb_irit") {
            columnRSCM(COL_GRIMY, "obj.unidentified_irit")
            columnRSCM(COL_CLEAN, "obj.irit_leaf")
            columnRSCM(COL_UNFINISHED, "obj.iritvial")
            column(COL_LEVEL, 40)
            column(COL_XP, 88)
        }

        row("dbrow.herblore_herb_avantoe") {
            columnRSCM(COL_GRIMY, "obj.unidentified_avantoe")
            columnRSCM(COL_CLEAN, "obj.avantoe")
            columnRSCM(COL_UNFINISHED, "obj.avantoevial")
            column(COL_LEVEL, 48)
            column(COL_XP, 100)
        }

        row("dbrow.herblore_herb_kwuarm") {
            columnRSCM(COL_GRIMY, "obj.unidentified_kwuarm")
            columnRSCM(COL_CLEAN, "obj.kwuarm")
            columnRSCM(COL_UNFINISHED, "obj.kwuarmvial")
            column(COL_LEVEL, 54)
            column(COL_XP, 113)
        }

        row("dbrow.herblore_herb_snapdragon") {
            columnRSCM(COL_GRIMY, "obj.unidentified_snapdragon")
            columnRSCM(COL_CLEAN, "obj.snapdragon")
            columnRSCM(COL_UNFINISHED, "obj.snapdragonvial")
            column(COL_LEVEL, 59)
            column(COL_XP, 118)
        }

        row("dbrow.herblore_herb_cadantine") {
            columnRSCM(COL_GRIMY, "obj.unidentified_cadantine")
            columnRSCM(COL_CLEAN, "obj.cadantine")
            columnRSCM(COL_UNFINISHED, "obj.cadantinevial")
            column(COL_LEVEL, 65)
            column(COL_XP, 125)
        }

        row("dbrow.herblore_herb_lantadyme") {
            columnRSCM(COL_GRIMY, "obj.unidentified_lantadyme")
            columnRSCM(COL_CLEAN, "obj.lantadyme")
            columnRSCM(COL_UNFINISHED, "obj.lantadymevial")
            column(COL_LEVEL, 67)
            column(COL_XP, 131)
        }

        row("dbrow.herblore_herb_dwarf_weed") {
            columnRSCM(COL_GRIMY, "obj.unidentified_dwarf_weed")
            columnRSCM(COL_CLEAN, "obj.dwarf_weed")
            columnRSCM(COL_UNFINISHED, "obj.dwarfweedvial")
            column(COL_LEVEL, 70)
            column(COL_XP, 138)
        }

        row("dbrow.herblore_herb_torstol") {
            columnRSCM(COL_GRIMY, "obj.unidentified_torstol")
            columnRSCM(COL_CLEAN, "obj.torstol")
            columnRSCM(COL_UNFINISHED, "obj.torstolvial")
            column(COL_LEVEL, 75)
            column(COL_XP, 150)
        }
    }

    const val COL_RESULT = 0
    const val COL_POT_UNFINISHED = 1
    const val COL_SECONDARY = 2
    const val COL_POT_LEVEL = 3
    const val COL_POT_XP = 4

    fun potions() = dbTable("dbtable.herblore_potions", serverOnly = true) {

        column("result", COL_RESULT, VarType.OBJ)
        column("unfinished", COL_POT_UNFINISHED, VarType.OBJ)
        column("secondary", COL_SECONDARY, VarType.OBJ)
        column("level", COL_POT_LEVEL, VarType.INT)
        column("xp", COL_POT_XP, VarType.INT)

        row("dbrow.herblore_potion_attack") {
            columnRSCM(COL_RESULT, "obj.3dose1attack")
            columnRSCM(COL_POT_UNFINISHED, "obj.guamvial")
            columnRSCM(COL_SECONDARY, "obj.eye_of_newt")
            column(COL_POT_LEVEL, 3)
            column(COL_POT_XP, 250)
        }

        row("dbrow.herblore_potion_antipoison") {
            columnRSCM(COL_RESULT, "obj.3doseantipoison")
            columnRSCM(COL_POT_UNFINISHED, "obj.marrentillvial")
            columnRSCM(COL_SECONDARY, "obj.unicorn_horn_dust")
            column(COL_POT_LEVEL, 5)
            column(COL_POT_XP, 375)
        }

        row("dbrow.herblore_potion_strength") {
            columnRSCM(COL_RESULT, "obj.3dose1strength")
            columnRSCM(COL_POT_UNFINISHED, "obj.tarrominvial")
            columnRSCM(COL_SECONDARY, "obj.limpwurt_root")
            column(COL_POT_LEVEL, 12)
            column(COL_POT_XP, 500)
        }

        row("dbrow.herblore_potion_restore") {
            columnRSCM(COL_RESULT, "obj.3dosestatrestore")
            columnRSCM(COL_POT_UNFINISHED, "obj.harralandervial")
            columnRSCM(COL_SECONDARY, "obj.red_spiders_eggs")
            column(COL_POT_LEVEL, 22)
            column(COL_POT_XP, 625)
        }

        row("dbrow.herblore_potion_energy") {
            columnRSCM(COL_RESULT, "obj.3dose1energy")
            columnRSCM(COL_POT_UNFINISHED, "obj.harralandervial")
            columnRSCM(COL_SECONDARY, "obj.chocolate_dust")
            column(COL_POT_LEVEL, 26)
            column(COL_POT_XP, 675)
        }

        row("dbrow.herblore_potion_defence") {
            columnRSCM(COL_RESULT, "obj.3dose1defense")
            columnRSCM(COL_POT_UNFINISHED, "obj.ranarrvial")
            columnRSCM(COL_SECONDARY, "obj.white_berries")
            column(COL_POT_LEVEL, 30)
            column(COL_POT_XP, 750)
        }

        row("dbrow.herblore_potion_prayer") {
            columnRSCM(COL_RESULT, "obj.3doseprayerrestore")
            columnRSCM(COL_POT_UNFINISHED, "obj.ranarrvial")
            columnRSCM(COL_SECONDARY, "obj.snape_grass")
            column(COL_POT_LEVEL, 38)
            column(COL_POT_XP, 875)
        }

        row("dbrow.herblore_potion_super_attack") {
            columnRSCM(COL_RESULT, "obj.3dose2attack")
            columnRSCM(COL_POT_UNFINISHED, "obj.iritvial")
            columnRSCM(COL_SECONDARY, "obj.eye_of_newt")
            column(COL_POT_LEVEL, 45)
            column(COL_POT_XP, 1000)
        }

        row("dbrow.herblore_potion_superantipoison") {
            columnRSCM(COL_RESULT, "obj.3dose2antipoison")
            columnRSCM(COL_POT_UNFINISHED, "obj.iritvial")
            columnRSCM(COL_SECONDARY, "obj.unicorn_horn_dust")
            column(COL_POT_LEVEL, 48)
            column(COL_POT_XP, 1063)
        }


        row("dbrow.herblore_potion_super_energy") {
            columnRSCM(COL_RESULT, "obj.3dose2energy")
            columnRSCM(COL_POT_UNFINISHED, "obj.avantoevial")
            columnRSCM(COL_SECONDARY, "obj.mortmyremushroom")
            column(COL_POT_LEVEL, 52)
            column(COL_POT_XP, 1175)
        }

        row("dbrow.herblore_potion_super_strength") {
            columnRSCM(COL_RESULT, "obj.3dose2strength")
            columnRSCM(COL_POT_UNFINISHED, "obj.kwuarmvial")
            columnRSCM(COL_SECONDARY, "obj.limpwurt_root")
            column(COL_POT_LEVEL, 55)
            column(COL_POT_XP, 1125)
        }


        row("dbrow.herblore_potion_super_restore") {
            columnRSCM(COL_RESULT, "obj.3dose2restore")
            columnRSCM(COL_POT_UNFINISHED, "obj.snapdragonvial")
            columnRSCM(COL_SECONDARY, "obj.red_spiders_eggs")
            column(COL_POT_LEVEL, 63)
            column(COL_POT_XP, 1425)
        }

        row("dbrow.herblore_potion_super_defence") {
            columnRSCM(COL_RESULT, "obj.3dose2defense")
            columnRSCM(COL_POT_UNFINISHED, "obj.cadantinevial")
            columnRSCM(COL_SECONDARY, "obj.white_berries")
            column(COL_POT_LEVEL, 66)
            column(COL_POT_XP, 1500)
        }

        row("dbrow.herblore_potion_magic") {
            columnRSCM(COL_RESULT, "obj.3dose1magic")
            columnRSCM(COL_POT_UNFINISHED, "obj.lantadymevial")
            columnRSCM(COL_SECONDARY, "obj.cactus_potato")
            column(COL_POT_LEVEL, 76)
            column(COL_POT_XP, 1725)
        }

        row("dbrow.herblore_potion_ranging") {
            columnRSCM(COL_RESULT, "obj.3doserangerspotion")
            columnRSCM(COL_POT_UNFINISHED, "obj.dwarfweedvial")
            columnRSCM(COL_SECONDARY, "obj.wine_of_zamorak")
            column(COL_POT_LEVEL, 72)
            column(COL_POT_XP, 1625)
        }
    }
}
