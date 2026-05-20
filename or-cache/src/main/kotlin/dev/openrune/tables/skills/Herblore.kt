package dev.openrune.tables.skills

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object Herblore {

    // Unfinished potions table columns (in order of definition)
    const val COL_HERB_ITEM = 0
    const val COL_LEVEL = 1
    const val COL_XP = 2
    const val COL_UNFINISHED_POTION = 3

    // Finished potions table columns (in order of definition)
    const val COL_UNF_POT = 0
    const val COL_SECONDARIES = 1
    const val COL_LEVEL_REQUIRED = 2
    const val COL_XP_FINISHED = 3
    const val COL_OUTPUT_POTION = 4
    const val COL_SECONDARIES_AMT_NEEDED = 5

    // Cleaning herbs table columns (in order of definition)
    const val COL_GRIMY_HERB = 0
    const val COL_CLEAN_LEVEL = 1
    const val COL_CLEAN_XP = 2
    const val COL_CLEAN_HERB = 3

    // Barbarian mixes table columns (in order of definition)
    const val COL_TWO_DOSE_POTION = 0
    const val COL_MIX_INGREDIENT = 1
    const val COL_MIX_LEVEL = 2
    const val COL_MIX_XP = 3
    const val COL_BARBARIAN_MIX = 4

    // Swamp tar table columns (in order of definition)
    const val COL_TAR_HERB = 0
    const val COL_TAR_LEVEL = 1
    const val COL_TAR_XP = 2
    const val COL_TAR_FINISHED = 3

    // Crushing table columns (in order of definition)
    const val COL_CRUSH_ITEM = 0
    const val COL_CRUSH_LEVEL = 1
    const val COL_CRUSH_XP = 2
    const val COL_CRUSHED_ITEM = 3

    /**
     * Table for creating unfinished potions (herb + vial of water)
     */
    fun unfinishedPotions() = dbTable("dbtable.herblore_unfinished", serverOnly = true) {
        column("herb_item", COL_HERB_ITEM, VarType.OBJ)
        column("level", COL_LEVEL, VarType.INT)
        column("xp", COL_XP, VarType.INT)
        column("unfinished_potion", COL_UNFINISHED_POTION, VarType.OBJ)

        // Guam leaf
        row("dbrow.herblore_guam_unfinished") {
            columnRSCM(COL_HERB_ITEM, "obj.guam_leaf")
            column(COL_LEVEL, 3)
            column(COL_XP, 2)
            columnRSCM(COL_UNFINISHED_POTION, "obj.guamvial")
        }

        // Marrentill
        row("dbrow.herblore_marrentill_unfinished") {
            columnRSCM(COL_HERB_ITEM, "obj.marentill")
            column(COL_LEVEL, 5)
            column(COL_XP, 3)
            columnRSCM(COL_UNFINISHED_POTION, "obj.marrentillvial")
        }

        // Tarromin
        row("dbrow.herblore_tarromin_unfinished") {
            columnRSCM(COL_HERB_ITEM, "obj.tarromin")
            column(COL_LEVEL, 11)
            column(COL_XP, 5)
            columnRSCM(COL_UNFINISHED_POTION, "obj.tarrominvial")
        }

        // Harralander
        row("dbrow.herblore_harralander_unfinished") {
            columnRSCM(COL_HERB_ITEM, "obj.harralander")
            column(COL_LEVEL, 20)
            column(COL_XP, 6)
            columnRSCM(COL_UNFINISHED_POTION, "obj.harralandervial")
        }

        // Ranarr weed
        row("dbrow.herblore_ranarr_unfinished") {
            columnRSCM(COL_HERB_ITEM, "obj.ranarr_weed")
            column(COL_LEVEL, 25)
            column(COL_XP, 8)
            columnRSCM(COL_UNFINISHED_POTION, "obj.ranarrvial")
        }

        // Toadflax
        row("dbrow.herblore_toadflax_unfinished") {
            columnRSCM(COL_HERB_ITEM, "obj.toadflax")
            column(COL_LEVEL, 30)
            column(COL_XP, 8)
            columnRSCM(COL_UNFINISHED_POTION, "obj.toadflaxvial")
        }

        // Irit leaf
        row("dbrow.herblore_irit_unfinished") {
            columnRSCM(COL_HERB_ITEM, "obj.irit_leaf")
            column(COL_LEVEL, 40)
            column(COL_XP, 9)
            columnRSCM(COL_UNFINISHED_POTION, "obj.iritvial")
        }

        // Avantoe
        row("dbrow.herblore_avantoe_unfinished") {
            columnRSCM(COL_HERB_ITEM, "obj.avantoe")
            column(COL_LEVEL, 48)
            column(COL_XP, 10)
            columnRSCM(COL_UNFINISHED_POTION, "obj.avantoevial")
        }

        // Kwuarm
        row("dbrow.herblore_kwuarm_unfinished") {
            columnRSCM(COL_HERB_ITEM, "obj.kwuarm")
            column(COL_LEVEL, 54)
            column(COL_XP, 11)
            columnRSCM(COL_UNFINISHED_POTION, "obj.kwuarmvial")
        }

        // Snapdragon
        row("dbrow.herblore_snapdragon_unfinished") {
            columnRSCM(COL_HERB_ITEM, "obj.snapdragon")
            column(COL_LEVEL, 59)
            column(COL_XP, 12)
            columnRSCM(COL_UNFINISHED_POTION, "obj.snapdragonvial")
        }

        // Cadantine
        row("dbrow.herblore_cadantine_unfinished") {
            columnRSCM(COL_HERB_ITEM, "obj.cadantine")
            column(COL_LEVEL, 65)
            column(COL_XP, 13)
            columnRSCM(COL_UNFINISHED_POTION, "obj.cadantinevial")
        }

        // Lantadyme
        row("dbrow.herblore_lantadyme_unfinished") {
            columnRSCM(COL_HERB_ITEM, "obj.lantadyme")
            column(COL_LEVEL, 67)
            column(COL_XP, 13)
            columnRSCM(COL_UNFINISHED_POTION, "obj.lantadymevial")
        }

        // Dwarf weed
        row("dbrow.herblore_dwarf_weed_unfinished") {
            columnRSCM(COL_HERB_ITEM, "obj.dwarf_weed")
            column(COL_LEVEL, 70)
            column(COL_XP, 13)
            columnRSCM(COL_UNFINISHED_POTION, "obj.dwarfweedvial")
        }

        // Torstol
        row("dbrow.herblore_torstol_unfinished") {
            columnRSCM(COL_HERB_ITEM, "obj.torstol")
            column(COL_LEVEL, 75)
            column(COL_XP, 14)
            columnRSCM(COL_UNFINISHED_POTION, "obj.torstolvial")
        }
    }

    /**
     * Table for creating finished potions (unfinished potion + secondary ingredient)
     * Supports multi-step potions with additional ingredients
     */
    fun finishedPotions() = dbTable("dbtable.herblore_finished", serverOnly = true) {
        column("unf_pot", COL_UNF_POT, VarType.OBJ)
        column("secondaries", COL_SECONDARIES, VarType.OBJ)
        column("level_required", COL_LEVEL_REQUIRED, VarType.INT)
        column("xp", COL_XP_FINISHED, VarType.INT)
        column("output_potion", COL_OUTPUT_POTION, VarType.OBJ)
        column("secondaries_amount", COL_SECONDARIES_AMT_NEEDED, VarType.INT)

        // Attack potion (Guam + Eye of newt)
        row("dbrow.herblore_attack_potion") {
            columnRSCM(COL_UNF_POT, "obj.guamvial")
            columnRSCM(COL_SECONDARIES, "obj.eye_of_newt")
            column(COL_LEVEL_REQUIRED, 3)
            column(COL_XP_FINISHED, 25)
            columnRSCM(COL_OUTPUT_POTION, "obj.3dose1attack")
        }

        // Antipoison (Marrentill + Unicorn horn dust)
        row("dbrow.herblore_antipoison") {
            columnRSCM(COL_UNF_POT, "obj.marrentillvial")
            columnRSCM(COL_SECONDARIES, "obj.unicorn_horn_dust")
            column(COL_LEVEL_REQUIRED, 5)
            column(COL_XP_FINISHED, 38)
            columnRSCM(COL_OUTPUT_POTION, "obj.3doseantipoison")
        }

        // Strength potion (Tarromin + Limpwurt root)
        row("dbrow.herblore_strength_potion") {
            columnRSCM(COL_UNF_POT, "obj.tarrominvial")
            columnRSCM(COL_SECONDARIES, "obj.limpwurt_root")
            column(COL_LEVEL_REQUIRED, 12)
            column(COL_XP_FINISHED, 50)
            columnRSCM(COL_OUTPUT_POTION, "obj.3dose1strength")
        }

        // Restore potion (Harralander + Red spiders' eggs)
        row("dbrow.herblore_restore_potion") {
            columnRSCM(COL_UNF_POT, "obj.harralandervial")
            columnRSCM(COL_SECONDARIES, "obj.red_spiders_eggs")
            column(COL_LEVEL_REQUIRED, 22)
            column(COL_XP_FINISHED, 63)
            columnRSCM(COL_OUTPUT_POTION, "obj.3dosestatrestore")
        }

        // Energy potion (Harralander + Chocolate dust)
        row("dbrow.herblore_energy_potion") {
            columnRSCM(COL_UNF_POT, "obj.harralandervial")
            columnRSCM(COL_SECONDARIES, "obj.chocolate_dust")
            column(COL_LEVEL_REQUIRED, 26)
            column(COL_XP_FINISHED, 68)
            columnRSCM(COL_OUTPUT_POTION, "obj.3dose1energy")
        }

        // Prayer potion (Ranarr + Snape grass)
        row("dbrow.herblore_prayer_potion") {
            columnRSCM(COL_UNF_POT, "obj.ranarrvial")
            columnRSCM(COL_SECONDARIES, "obj.snape_grass")
            column(COL_LEVEL_REQUIRED, 38)
            column(COL_XP_FINISHED, 88)
            columnRSCM(COL_OUTPUT_POTION, "obj.3doseprayerrestore")
        }

        // Super attack (Irit + Eye of newt)
        row("dbrow.herblore_super_attack") {
            columnRSCM(COL_UNF_POT, "obj.iritvial")
            columnRSCM(COL_SECONDARIES, "obj.eye_of_newt")
            column(COL_LEVEL_REQUIRED, 45)
            column(COL_XP_FINISHED, 100)
            columnRSCM(COL_OUTPUT_POTION, "obj.3dose2attack")
        }

        // Superantipoison (Irit + Unicorn horn dust)
        row("dbrow.herblore_superantipoison") {
            columnRSCM(COL_UNF_POT, "obj.iritvial")
            columnRSCM(COL_SECONDARIES, "obj.unicorn_horn_dust")
            column(COL_LEVEL_REQUIRED, 48)
            column(COL_XP_FINISHED, 105)
            columnRSCM(COL_OUTPUT_POTION, "obj.3dose2antipoison")
        }

        // Fishing potion (Avantoe + Snape grass)
        row("dbrow.herblore_fishing_potion") {
            columnRSCM(COL_UNF_POT, "obj.avantoevial")
            columnRSCM(COL_SECONDARIES, "obj.snape_grass")
            column(COL_LEVEL_REQUIRED, 50)
            column(COL_XP_FINISHED, 113)
            columnRSCM(COL_OUTPUT_POTION, "obj.3dosefisherspotion")
        }

        // Super energy (Avantoe + Mort myre fungus)
        row("dbrow.herblore_super_energy") {
            columnRSCM(COL_UNF_POT, "obj.avantoevial")
            columnRSCM(COL_SECONDARIES, "obj.mortmyremushroom")
            column(COL_LEVEL_REQUIRED, 52)
            column(COL_XP_FINISHED, 118)
            columnRSCM(COL_OUTPUT_POTION, "obj.3dose2energy")
        }

        // Super strength (Kwuarm + Limpwurt root)
        row("dbrow.herblore_super_strength") {
            columnRSCM(COL_UNF_POT, "obj.kwuarmvial")
            columnRSCM(COL_SECONDARIES, "obj.limpwurt_root")
            column(COL_LEVEL_REQUIRED, 55)
            column(COL_XP_FINISHED, 125)
            columnRSCM(COL_OUTPUT_POTION, "obj.3dose2strength")
        }

        // Weapon poison (Kwuarm + Dragon scale dust)
        row("dbrow.herblore_weapon_poison") {
            columnRSCM(COL_UNF_POT, "obj.kwuarmvial")
            columnRSCM(COL_SECONDARIES, "obj.dragon_scale_dust")
            column(COL_LEVEL_REQUIRED, 60)
            column(COL_XP_FINISHED, 138)
            columnRSCM(COL_OUTPUT_POTION, "obj.weapon_poison")
        }

        // Super restore (Snapdragon + Red spiders' eggs)
        row("dbrow.herblore_super_restore") {
            columnRSCM(COL_UNF_POT, "obj.snapdragonvial")
            columnRSCM(COL_SECONDARIES, "obj.red_spiders_eggs")
            column(COL_LEVEL_REQUIRED, 63)
            column(COL_XP_FINISHED, 143)
            columnRSCM(COL_OUTPUT_POTION, "obj.3dose2restore")
        }

        // Super defence (Cadantine + White berries)
        row("dbrow.herblore_super_defence") {
            columnRSCM(COL_UNF_POT, "obj.cadantinevial")
            columnRSCM(COL_SECONDARIES, "obj.white_berries")
            column(COL_LEVEL_REQUIRED, 66)
            column(COL_XP_FINISHED, 150)
            columnRSCM(COL_OUTPUT_POTION, "obj.3dose2defense")
        }

        // Antifire (Lantadyme + Dragon scale dust)
        row("dbrow.herblore_antifire") {
            columnRSCM(COL_UNF_POT, "obj.lantadymevial")
            columnRSCM(COL_SECONDARIES, "obj.dragon_scale_dust")
            column(COL_LEVEL_REQUIRED, 69)
            column(COL_XP_FINISHED, 158)
            columnRSCM(COL_OUTPUT_POTION, "obj.3dose1antidragon")
        }

        // Super antifire (Lantadyme + Crushed superior dragon bones)
        row("dbrow.herblore_super_antifire") {
            columnRSCM(COL_UNF_POT, "obj.lantadymevial")
            columnRSCM(COL_SECONDARIES, "obj.crushed_dragon_bones")
            column(COL_LEVEL_REQUIRED, 92)
            column(COL_XP_FINISHED, 180)
            columnRSCM(COL_OUTPUT_POTION, "obj.3dose2antidragon")
        }

        // Ranging potion (Dwarf weed + Wine of zamorak)
        row("dbrow.herblore_ranging_potion") {
            columnRSCM(COL_UNF_POT, "obj.dwarfweedvial")
            columnRSCM(COL_SECONDARIES, "obj.wine_of_zamorak")
            column(COL_LEVEL_REQUIRED, 72)
            column(COL_XP_FINISHED, 163)
            columnRSCM(COL_OUTPUT_POTION, "obj.3doserangerspotion")
        }

        // Magic potion (Lantadyme + Potato cactus)
        row("dbrow.herblore_magic_potion") {
            columnRSCM(COL_UNF_POT, "obj.lantadymevial")
            columnRSCM(COL_SECONDARIES, "obj.cactus_potato")
            column(COL_LEVEL_REQUIRED, 76)
            column(COL_XP_FINISHED, 173)
            columnRSCM(COL_OUTPUT_POTION, "obj.3dose1magic")
        }

        // Zamorak brew (Torstol + Jangerberries)
        row("dbrow.herblore_zamorak_brew") {
            columnRSCM(COL_UNF_POT, "obj.torstolvial")
            columnRSCM(COL_SECONDARIES, "obj.jangerberries")
            column(COL_LEVEL_REQUIRED, 78)
            column(COL_XP_FINISHED, 175)
            columnRSCM(COL_OUTPUT_POTION, "obj.3dosepotionofzamorak")
        }

        // Saradomin brew (Toadflax + Crushed nest)
        row("dbrow.herblore_saradomin_brew") {
            columnRSCM(COL_UNF_POT, "obj.toadflaxvial")
            columnRSCM(COL_SECONDARIES, "obj.crushed_bird_nest")
            column(COL_LEVEL_REQUIRED, 81)
            column(COL_XP_FINISHED, 180)
            columnRSCM(COL_OUTPUT_POTION, "obj.3dosepotionofsaradomin")
        }

        // Defence potion (Ranarr + White berries)
        row("dbrow.herblore_defence_potion") {
            columnRSCM(COL_UNF_POT, "obj.ranarrvial")
            columnRSCM(COL_SECONDARIES, "obj.white_berries")
            column(COL_LEVEL_REQUIRED, 30)
            column(COL_XP_FINISHED, 75)
            columnRSCM(COL_OUTPUT_POTION, "obj.3dose1defense")
        }

        // Agility potion (Toadflax + Toad's legs)
        row("dbrow.herblore_agility_potion") {
            columnRSCM(COL_UNF_POT, "obj.toadflaxvial")
            columnRSCM(COL_SECONDARIES, "obj.toads_legs")
            column(COL_LEVEL_REQUIRED, 34)
            column(COL_XP_FINISHED, 80)
            columnRSCM(COL_OUTPUT_POTION, "obj.3dose1agility")
        }

        // Combat potion (Harralander + Goat horn dust)
        row("dbrow.herblore_combat_potion") {
            columnRSCM(COL_UNF_POT, "obj.harralandervial")
            columnRSCM(COL_SECONDARIES, "obj.ground_desert_goat_horn")
            column(COL_LEVEL_REQUIRED, 36)
            column(COL_XP_FINISHED, 84)
            columnRSCM(COL_OUTPUT_POTION, "obj.3dosecombat")
        }

        // Hunter potion (Avantoe + Kebbit teeth)
        row("dbrow.herblore_hunter_potion") {
            columnRSCM(COL_UNF_POT, "obj.avantoevial")
            columnRSCM(COL_SECONDARIES, "obj.huntingbeast_sabreteeth")
            column(COL_LEVEL_REQUIRED, 53)
            column(COL_XP_FINISHED, 120)
            columnRSCM(COL_OUTPUT_POTION, "obj.3dosehunting")
        }

        // Bastion potion: Cadantine (unf) + Wine of zamorak + Crushed superior dragon bones → Bastion (3)
        row("dbrow.herblore_bastion_potion") {
            columnRSCM(COL_UNF_POT, "obj.cadantinevial")
            columnRSCM(COL_SECONDARIES, "obj.wine_of_zamorak", "obj.crushed_dragon_bones")
            column(COL_LEVEL_REQUIRED, 80)
            column(COL_XP_FINISHED, 155)
            columnRSCM(COL_OUTPUT_POTION, "obj.3dosebastion")
        }

        // Battlemage potion: Cadantine (unf) + Potato cactus + Crushed superior dragon bones → Battlemage (3)
        row("dbrow.herblore_battlemage_potion") {
            columnRSCM(COL_UNF_POT, "obj.cadantinevial")
            columnRSCM(COL_SECONDARIES, "obj.cactus_potato", "obj.crushed_dragon_bones")
            column(COL_LEVEL_REQUIRED, 79)
            column(COL_XP_FINISHED, 155)
            columnRSCM(COL_OUTPUT_POTION, "obj.3dosebattlemage")
        }

        // Super combat potion: Torstol (unf) + Super attack (3) + Super strength (3) + Super defence (3) = Super combat (3)
        row("dbrow.herblore_super_combat_potion") {
            columnRSCM(COL_UNF_POT, "obj.torstol")
            columnRSCM(COL_SECONDARIES, "obj.3dose2attack", "obj.3dose2strength", "obj.3dose2defense")
            column(COL_LEVEL_REQUIRED, 90)
            column(COL_XP_FINISHED, 150)
            columnRSCM(COL_OUTPUT_POTION, "obj.3dose2combat")
        }

        // Extended anti-venom+ (Anti-venom+ (3) + Zulrah's scales)
        row("dbrow.herblore_extended_antivenom_plus") {
            columnRSCM(COL_UNF_POT, "obj.antivenom+3")
            columnRSCM(COL_SECONDARIES, "obj.snakeboss_scale")
            column(COL_LEVEL_REQUIRED, 94)
            column(COL_XP_FINISHED, 160)
            columnRSCM(COL_OUTPUT_POTION, "obj.extended_antivenom+3")
        }

        // Stamina potion (1) (Super energy(1) + Amylase crystal)
        row("dbrow.herblore_stamina_potion_1") {
            columnRSCM(COL_UNF_POT, "obj.1dose2energy")
            columnRSCM(COL_SECONDARIES, "obj.amylase")
            column(COL_LEVEL_REQUIRED, 77)
            column(COL_XP_FINISHED, 25)
            columnRSCM(COL_OUTPUT_POTION, "obj.1dosestamina")
        }

        // Stamina potion (2) (Super energy(2) + 2x Amylase crystal)
        row("dbrow.herblore_stamina_potion_2") {
            columnRSCM(COL_UNF_POT, "obj.2dose2energy")
            columnRSCM(COL_SECONDARIES, "obj.amylase")
            column(COL_LEVEL_REQUIRED, 77)
            column(COL_XP_FINISHED, 51)
            columnRSCM(COL_OUTPUT_POTION, "obj.2dosestamina")
            column(COL_SECONDARIES_AMT_NEEDED, 2)
        }

        // Stamina potion (3) (Super energy(3) + 3x Amylase crystal)
        row("dbrow.herblore_stamina_potion_3") {
            columnRSCM(COL_UNF_POT, "obj.3dose2energy")
            columnRSCM(COL_SECONDARIES, "obj.amylase")
            column(COL_LEVEL_REQUIRED, 77)
            column(COL_XP_FINISHED, 77)
            columnRSCM(COL_OUTPUT_POTION, "obj.3dosestamina")
            column(COL_SECONDARIES_AMT_NEEDED, 3)
        }

        // Stamina potion (4) (Super energy(4) + 4x Amylase crystal)
        row("dbrow.herblore_stamina_potion_4") {
            columnRSCM(COL_UNF_POT, "obj.4dose2energy")
            columnRSCM(COL_SECONDARIES, "obj.amylase")
            column(COL_LEVEL_REQUIRED, 77)
            column(COL_XP_FINISHED, 102)
            columnRSCM(COL_OUTPUT_POTION, "obj.4dosestamina")
            column(COL_SECONDARIES_AMT_NEEDED, 5)
        }

        // Divine bastion potion (1) (Bastion(1) + Crystal dust)
        row("dbrow.herblore_divine_bastion_1") {
            columnRSCM(COL_UNF_POT, "obj.1dosebastion")
            columnRSCM(COL_SECONDARIES, "obj.sote_crystal_dust")
            column(COL_LEVEL_REQUIRED, 86)
            column(COL_XP_FINISHED, 5)
            columnRSCM(COL_OUTPUT_POTION, "obj.1dosedivinebastion")
        }

        row("dbrow.herblore_divine_bastion_2") {
            columnRSCM(COL_UNF_POT, "obj.2dosebastion")
            columnRSCM(COL_SECONDARIES, "obj.sote_crystal_dust")
            column(COL_LEVEL_REQUIRED, 86)
            column(COL_XP_FINISHED, 10)
            columnRSCM(COL_OUTPUT_POTION, "obj.2dosedivinebastion")
            column(COL_SECONDARIES_AMT_NEEDED, 2)
        }

        row("dbrow.herblore_divine_bastion_3") {
            columnRSCM(COL_UNF_POT, "obj.3dosebastion")
            columnRSCM(COL_SECONDARIES, "obj.sote_crystal_dust")
            column(COL_LEVEL_REQUIRED, 86)
            column(COL_XP_FINISHED, 15)
            columnRSCM(COL_OUTPUT_POTION, "obj.3dosedivinebastion")
            column(COL_SECONDARIES_AMT_NEEDED, 3)
        }

        row("dbrow.herblore_divine_bastion_4") {
            columnRSCM(COL_UNF_POT, "obj.4dosebastion")
            columnRSCM(COL_SECONDARIES, "obj.sote_crystal_dust")
            column(COL_LEVEL_REQUIRED, 86)
            column(COL_XP_FINISHED, 20)
            columnRSCM(COL_OUTPUT_POTION, "obj.4dosedivinebastion")
            column(COL_SECONDARIES_AMT_NEEDED, 4)
        }

        // Divine battlemage potion (1) (Battlemage(1) + Crystal dust)
        row("dbrow.herblore_divine_battlemage_1") {
            columnRSCM(COL_UNF_POT, "obj.1dosebattlemage")
            columnRSCM(COL_SECONDARIES, "obj.sote_crystal_dust")
            column(COL_LEVEL_REQUIRED, 86)
            column(COL_XP_FINISHED, 5)
            columnRSCM(COL_OUTPUT_POTION, "obj.1dosedivinebattlemage")
        }

        row("dbrow.herblore_divine_battlemage_2") {
            columnRSCM(COL_UNF_POT, "obj.2dosebattlemage")
            columnRSCM(COL_SECONDARIES, "obj.sote_crystal_dust")
            column(COL_LEVEL_REQUIRED, 86)
            column(COL_XP_FINISHED, 10)
            columnRSCM(COL_OUTPUT_POTION, "obj.2dosedivinebattlemage")
            column(COL_SECONDARIES_AMT_NEEDED, 2)
        }

        row("dbrow.herblore_divine_battlemage_3") {
            columnRSCM(COL_UNF_POT, "obj.3dosebattlemage")
            columnRSCM(COL_SECONDARIES, "obj.sote_crystal_dust")
            column(COL_LEVEL_REQUIRED, 86)
            column(COL_XP_FINISHED, 15)
            columnRSCM(COL_OUTPUT_POTION, "obj.3dosedivinebattlemage")
            column(COL_SECONDARIES_AMT_NEEDED, 3)
        }

        row("dbrow.herblore_divine_battlemage_4") {
            columnRSCM(COL_UNF_POT, "obj.4dosebattlemage")
            columnRSCM(COL_SECONDARIES, "obj.sote_crystal_dust")
            column(COL_LEVEL_REQUIRED, 86)
            column(COL_XP_FINISHED, 20)
            columnRSCM(COL_OUTPUT_POTION, "obj.4dosedivinebattlemage")
            column(COL_SECONDARIES_AMT_NEEDED, 4)
        }

        // Divine magic potion (1) (Magic potion(1) + Crystal dust)
        row("dbrow.herblore_divine_magic_1") {
            columnRSCM(COL_UNF_POT, "obj.1dose1magic")
            columnRSCM(COL_SECONDARIES, "obj.sote_crystal_dust")
            column(COL_LEVEL_REQUIRED, 78)
            column(COL_XP_FINISHED, 5)
            columnRSCM(COL_OUTPUT_POTION, "obj.1dosedivinemagic")
        }

        row("dbrow.herblore_divine_magic_2") {
            columnRSCM(COL_UNF_POT, "obj.2dose1magic")
            columnRSCM(COL_SECONDARIES, "obj.sote_crystal_dust")
            column(COL_LEVEL_REQUIRED, 78)
            column(COL_XP_FINISHED, 10)
            columnRSCM(COL_OUTPUT_POTION, "obj.2dosedivinemagic")
            column(COL_SECONDARIES_AMT_NEEDED, 2)
        }

        row("dbrow.herblore_divine_magic_3") {
            columnRSCM(COL_UNF_POT, "obj.3dose1magic")
            columnRSCM(COL_SECONDARIES, "obj.sote_crystal_dust")
            column(COL_LEVEL_REQUIRED, 78)
            column(COL_XP_FINISHED, 15)
            columnRSCM(COL_OUTPUT_POTION, "obj.3dosedivinemagic")
            column(COL_SECONDARIES_AMT_NEEDED, 3)
        }

        row("dbrow.herblore_divine_magic_4") {
            columnRSCM(COL_UNF_POT, "obj.4dose1magic")
            columnRSCM(COL_SECONDARIES, "obj.sote_crystal_dust")
            column(COL_LEVEL_REQUIRED, 78)
            column(COL_XP_FINISHED, 20)
            columnRSCM(COL_OUTPUT_POTION, "obj.4dosedivinemagic")
            column(COL_SECONDARIES_AMT_NEEDED, 4)
        }

        // Divine ranging potion (1) (Ranging potion(1) + Crystal dust)
        row("dbrow.herblore_divine_ranging_1") {
            columnRSCM(COL_UNF_POT, "obj.1doserangerspotion")
            columnRSCM(COL_SECONDARIES, "obj.sote_crystal_dust")
            column(COL_LEVEL_REQUIRED, 74)
            column(COL_XP_FINISHED, 5)
            columnRSCM(COL_OUTPUT_POTION, "obj.1dosedivinerange")
        }

        row("dbrow.herblore_divine_ranging_2") {
            columnRSCM(COL_UNF_POT, "obj.2doserangerspotion")
            columnRSCM(COL_SECONDARIES, "obj.sote_crystal_dust")
            column(COL_LEVEL_REQUIRED, 74)
            column(COL_XP_FINISHED, 10)
            columnRSCM(COL_OUTPUT_POTION, "obj.2dosedivinerange")
            column(COL_SECONDARIES_AMT_NEEDED, 2)
        }

        row("dbrow.herblore_divine_ranging_3") {
            columnRSCM(COL_UNF_POT, "obj.3doserangerspotion")
            columnRSCM(COL_SECONDARIES, "obj.sote_crystal_dust")
            column(COL_LEVEL_REQUIRED, 74)
            column(COL_XP_FINISHED, 15)
            columnRSCM(COL_OUTPUT_POTION, "obj.3dosedivinerange")
            column(COL_SECONDARIES_AMT_NEEDED, 3)
        }

        row("dbrow.herblore_divine_ranging_4") {
            columnRSCM(COL_UNF_POT, "obj.4doserangerspotion")
            columnRSCM(COL_SECONDARIES, "obj.sote_crystal_dust")
            column(COL_LEVEL_REQUIRED, 74)
            column(COL_XP_FINISHED, 20)
            columnRSCM(COL_OUTPUT_POTION, "obj.4dosedivinerange")
            column(COL_SECONDARIES_AMT_NEEDED, 4)
        }

        // Divine super attack potion (1) (Super attack(1) + Crystal dust)
        row("dbrow.herblore_divine_super_attack_1") {
            columnRSCM(COL_UNF_POT, "obj.1dose2attack")
            columnRSCM(COL_SECONDARIES, "obj.sote_crystal_dust")
            column(COL_LEVEL_REQUIRED, 70)
            column(COL_XP_FINISHED, 5)
            columnRSCM(COL_OUTPUT_POTION, "obj.1dosedivineattack")
        }

        row("dbrow.herblore_divine_super_attack_2") {
            columnRSCM(COL_UNF_POT, "obj.2dose2attack")
            columnRSCM(COL_SECONDARIES, "obj.sote_crystal_dust")
            column(COL_LEVEL_REQUIRED, 70)
            column(COL_XP_FINISHED, 10)
            columnRSCM(COL_OUTPUT_POTION, "obj.2dosedivineattack")
            column(COL_SECONDARIES_AMT_NEEDED, 2)
        }

        row("dbrow.herblore_divine_super_attack_3") {
            columnRSCM(COL_UNF_POT, "obj.3dose2attack")
            columnRSCM(COL_SECONDARIES, "obj.sote_crystal_dust")
            column(COL_LEVEL_REQUIRED, 70)
            column(COL_XP_FINISHED, 15)
            columnRSCM(COL_OUTPUT_POTION, "obj.3dosedivineattack")
            column(COL_SECONDARIES_AMT_NEEDED, 3)
        }

        row("dbrow.herblore_divine_super_attack_4") {
            columnRSCM(COL_UNF_POT, "obj.4dose2attack")
            columnRSCM(COL_SECONDARIES, "obj.sote_crystal_dust")
            column(COL_LEVEL_REQUIRED, 70)
            column(COL_XP_FINISHED, 20)
            columnRSCM(COL_OUTPUT_POTION, "obj.4dosedivineattack")
            column(COL_SECONDARIES_AMT_NEEDED, 4)
        }

        // Divine super combat potion (1) (Super combat(1) + Crystal dust)
        row("dbrow.herblore_divine_super_combat_1") {
            columnRSCM(COL_UNF_POT, "obj.1dose2combat")
            columnRSCM(COL_SECONDARIES, "obj.sote_crystal_dust")
            column(COL_LEVEL_REQUIRED, 97)
            column(COL_XP_FINISHED, 5)
            columnRSCM(COL_OUTPUT_POTION, "obj.1dosedivinecombat")
        }

        row("dbrow.herblore_divine_super_combat_2") {
            columnRSCM(COL_UNF_POT, "obj.2dose2combat")
            columnRSCM(COL_SECONDARIES, "obj.sote_crystal_dust")
            column(COL_LEVEL_REQUIRED, 97)
            column(COL_XP_FINISHED, 10)
            columnRSCM(COL_OUTPUT_POTION, "obj.2dosedivinecombat")
            column(COL_SECONDARIES_AMT_NEEDED, 2)
        }

        row("dbrow.herblore_divine_super_combat_3") {
            columnRSCM(COL_UNF_POT, "obj.3dose2combat")
            columnRSCM(COL_SECONDARIES, "obj.sote_crystal_dust")
            column(COL_LEVEL_REQUIRED, 97)
            column(COL_XP_FINISHED, 15)
            columnRSCM(COL_OUTPUT_POTION, "obj.3dosedivinecombat")
            column(COL_SECONDARIES_AMT_NEEDED, 3)
        }

        row("dbrow.herblore_divine_super_combat_4") {
            columnRSCM(COL_UNF_POT, "obj.4dose2combat")
            columnRSCM(COL_SECONDARIES, "obj.sote_crystal_dust")
            column(COL_LEVEL_REQUIRED, 97)
            column(COL_XP_FINISHED, 20)
            columnRSCM(COL_OUTPUT_POTION, "obj.4dosedivinecombat")
            column(COL_SECONDARIES_AMT_NEEDED, 4)
        }

        // Divine super defence potion (1) (Super defence(1) + Crystal dust)
        row("dbrow.herblore_divine_super_defence_1") {
            columnRSCM(COL_UNF_POT, "obj.1dose2defense")
            columnRSCM(COL_SECONDARIES, "obj.sote_crystal_dust")
            column(COL_LEVEL_REQUIRED, 70)
            column(COL_XP_FINISHED, 5)
            columnRSCM(COL_OUTPUT_POTION, "obj.1dosedivinedefence")
        }

        row("dbrow.herblore_divine_super_defence_2") {
            columnRSCM(COL_UNF_POT, "obj.2dose2defense")
            columnRSCM(COL_SECONDARIES, "obj.sote_crystal_dust")
            column(COL_LEVEL_REQUIRED, 70)
            column(COL_XP_FINISHED, 10)
            columnRSCM(COL_OUTPUT_POTION, "obj.2dosedivinedefence")
            column(COL_SECONDARIES_AMT_NEEDED, 2)
        }

        row("dbrow.herblore_divine_super_defence_3") {
            columnRSCM(COL_UNF_POT, "obj.3dose2defense")
            columnRSCM(COL_SECONDARIES, "obj.sote_crystal_dust")
            column(COL_LEVEL_REQUIRED, 70)
            column(COL_XP_FINISHED, 15)
            columnRSCM(COL_OUTPUT_POTION, "obj.3dosedivinedefence")
            column(COL_SECONDARIES_AMT_NEEDED, 3)
        }

        row("dbrow.herblore_divine_super_defence_4") {
            columnRSCM(COL_UNF_POT, "obj.4dose2defense")
            columnRSCM(COL_SECONDARIES, "obj.sote_crystal_dust")
            column(COL_LEVEL_REQUIRED, 70)
            column(COL_XP_FINISHED, 20)
            columnRSCM(COL_OUTPUT_POTION, "obj.4dosedivinedefence")
            column(COL_SECONDARIES_AMT_NEEDED, 4)
        }

        // Divine super strength potion (1) (Super strength(1) + Crystal dust)
        row("dbrow.herblore_divine_super_strength_1") {
            columnRSCM(COL_UNF_POT, "obj.1dose2strength")
            columnRSCM(COL_SECONDARIES, "obj.sote_crystal_dust")
            column(COL_LEVEL_REQUIRED, 70)
            column(COL_XP_FINISHED, 5)
            columnRSCM(COL_OUTPUT_POTION, "obj.1dosedivinestrength")
        }

        row("dbrow.herblore_divine_super_strength_2") {
            columnRSCM(COL_UNF_POT, "obj.2dose2strength")
            columnRSCM(COL_SECONDARIES, "obj.sote_crystal_dust")
            column(COL_LEVEL_REQUIRED, 70)
            column(COL_XP_FINISHED, 10)
            columnRSCM(COL_OUTPUT_POTION, "obj.2dosedivinestrength")
            column(COL_SECONDARIES_AMT_NEEDED, 2)
        }

        row("dbrow.herblore_divine_super_strength_3") {
            columnRSCM(COL_UNF_POT, "obj.3dose2strength")
            columnRSCM(COL_SECONDARIES, "obj.sote_crystal_dust")
            column(COL_LEVEL_REQUIRED, 70)
            column(COL_XP_FINISHED, 15)
            columnRSCM(COL_OUTPUT_POTION, "obj.3dosedivinestrength")
            column(COL_SECONDARIES_AMT_NEEDED, 3)
        }

        row("dbrow.herblore_divine_super_strength_4") {
            columnRSCM(COL_UNF_POT, "obj.4dose2strength")
            columnRSCM(COL_SECONDARIES, "obj.sote_crystal_dust")
            column(COL_LEVEL_REQUIRED, 70)
            column(COL_XP_FINISHED, 20)
            columnRSCM(COL_OUTPUT_POTION, "obj.4dosedivinestrength")
            column(COL_SECONDARIES_AMT_NEEDED, 4)
        }

    }

    /**
     * Table for cleaning grimy herbs (unidentified -> clean)
     * One-time action, no repeatable delay
     */
    fun cleaningHerbs() = dbTable("dbtable.herblore_cleaning", serverOnly = true) {
        column("grimy_herb", COL_GRIMY_HERB, VarType.OBJ)
        column("level", COL_CLEAN_LEVEL, VarType.INT)
        column("xp", COL_CLEAN_XP, VarType.INT)
        column("clean_herb", COL_CLEAN_HERB, VarType.OBJ)

        // Guam
        row("dbrow.herblore_clean_guam") {
            columnRSCM(COL_GRIMY_HERB, "obj.unidentified_guam")
            column(COL_CLEAN_LEVEL, 3)
            column(COL_CLEAN_XP, 2)
            columnRSCM(COL_CLEAN_HERB, "obj.guam_leaf")
        }

        // Marrentill
        row("dbrow.herblore_clean_marrentill") {
            columnRSCM(COL_GRIMY_HERB, "obj.unidentified_marentill")
            column(COL_CLEAN_LEVEL, 5)
            column(COL_CLEAN_XP, 3)
            columnRSCM(COL_CLEAN_HERB, "obj.marentill")
        }

        // Tarromin
        row("dbrow.herblore_clean_tarromin") {
            columnRSCM(COL_GRIMY_HERB, "obj.unidentified_tarromin")
            column(COL_CLEAN_LEVEL, 11)
            column(COL_CLEAN_XP, 5)
            columnRSCM(COL_CLEAN_HERB, "obj.tarromin")
        }

        // Harralander
        row("dbrow.herblore_clean_harralander") {
            columnRSCM(COL_GRIMY_HERB, "obj.unidentified_harralander")
            column(COL_CLEAN_LEVEL, 20)
            column(COL_CLEAN_XP, 6)
            columnRSCM(COL_CLEAN_HERB, "obj.harralander")
        }

        // Ranarr
        row("dbrow.herblore_clean_ranarr") {
            columnRSCM(COL_GRIMY_HERB, "obj.unidentified_ranarr")
            column(COL_CLEAN_LEVEL, 25)
            column(COL_CLEAN_XP, 8)
            columnRSCM(COL_CLEAN_HERB, "obj.ranarr_weed")
        }

        // Toadflax
        row("dbrow.herblore_clean_toadflax") {
            columnRSCM(COL_GRIMY_HERB, "obj.unidentified_toadflax")
            column(COL_CLEAN_LEVEL, 30)
            column(COL_CLEAN_XP, 8)
            columnRSCM(COL_CLEAN_HERB, "obj.toadflax")
        }

        // Irit
        row("dbrow.herblore_clean_irit") {
            columnRSCM(COL_GRIMY_HERB, "obj.unidentified_irit")
            column(COL_CLEAN_LEVEL, 40)
            column(COL_CLEAN_XP, 9)
            columnRSCM(COL_CLEAN_HERB, "obj.irit_leaf")
        }

        // Avantoe
        row("dbrow.herblore_clean_avantoe") {
            columnRSCM(COL_GRIMY_HERB, "obj.unidentified_avantoe")
            column(COL_CLEAN_LEVEL, 48)
            column(COL_CLEAN_XP, 10)
            columnRSCM(COL_CLEAN_HERB, "obj.avantoe")
        }

        // Kwuarm
        row("dbrow.herblore_clean_kwuarm") {
            columnRSCM(COL_GRIMY_HERB, "obj.unidentified_kwuarm")
            column(COL_CLEAN_LEVEL, 54)
            column(COL_CLEAN_XP, 11)
            columnRSCM(COL_CLEAN_HERB, "obj.kwuarm")
        }

        // Snapdragon
        row("dbrow.herblore_clean_snapdragon") {
            columnRSCM(COL_GRIMY_HERB, "obj.unidentified_snapdragon")
            column(COL_CLEAN_LEVEL, 59)
            column(COL_CLEAN_XP, 12)
            columnRSCM(COL_CLEAN_HERB, "obj.snapdragon")
        }

        // Cadantine
        row("dbrow.herblore_clean_cadantine") {
            columnRSCM(COL_GRIMY_HERB, "obj.unidentified_cadantine")
            column(COL_CLEAN_LEVEL, 65)
            column(COL_CLEAN_XP, 13)
            columnRSCM(COL_CLEAN_HERB, "obj.cadantine")
        }

        // Lantadyme
        row("dbrow.herblore_clean_lantadyme") {
            columnRSCM(COL_GRIMY_HERB, "obj.unidentified_lantadyme")
            column(COL_CLEAN_LEVEL, 67)
            column(COL_CLEAN_XP, 13)
            columnRSCM(COL_CLEAN_HERB, "obj.lantadyme")
        }

        // Dwarf weed
        row("dbrow.herblore_clean_dwarf_weed") {
            columnRSCM(COL_GRIMY_HERB, "obj.unidentified_dwarf_weed")
            column(COL_CLEAN_LEVEL, 70)
            column(COL_CLEAN_XP, 13)
            columnRSCM(COL_CLEAN_HERB, "obj.dwarf_weed")
        }

        // Torstol
        row("dbrow.herblore_clean_torstol") {
            columnRSCM(COL_GRIMY_HERB, "obj.unidentified_torstol")
            column(COL_CLEAN_LEVEL, 75)
            column(COL_CLEAN_XP, 14)
            columnRSCM(COL_CLEAN_HERB, "obj.torstol")
        }
    }

    /**
     * Table for creating barbarian mixes (two-dose potion + roe/caviar)
     */
    fun barbarianMixes() = dbTable("dbtable.herblore_barbarian_mixes", serverOnly = true) {
        column("two_dose_potion", COL_TWO_DOSE_POTION, VarType.OBJ)
        column("mix_ingredient", COL_MIX_INGREDIENT, VarType.OBJ)
        column("level", COL_MIX_LEVEL, VarType.INT)
        column("xp", COL_MIX_XP, VarType.INT)
        column("barbarian_mix", COL_BARBARIAN_MIX, VarType.OBJ)

        // Attack mix (2-dose attack + roe)
        row("dbrow.herblore_attack_mix") {
            columnRSCM(COL_TWO_DOSE_POTION, "obj.2dose1attack")
            columnRSCM(COL_MIX_INGREDIENT, "obj.brut_roe")
            column(COL_MIX_LEVEL, 3)
            column(COL_MIX_XP, 0)
            columnRSCM(COL_BARBARIAN_MIX, "obj.brutal_2dose1attack")
        }

        // Antipoison mix (2-dose antipoison + roe)
        row("dbrow.herblore_antipoison_mix") {
            columnRSCM(COL_TWO_DOSE_POTION, "obj.2doseantipoison")
            columnRSCM(COL_MIX_INGREDIENT, "obj.brut_roe")
            column(COL_MIX_LEVEL, 5)
            column(COL_MIX_XP, 0)
            columnRSCM(COL_BARBARIAN_MIX, "obj.brutal_2doseantipoison")
        }

        // Attack mix (2-dose attack + caviar) - Alternative to roe
        row("dbrow.herblore_attack_mix_caviar") {
            columnRSCM(COL_TWO_DOSE_POTION, "obj.2dose1attack")
            columnRSCM(COL_MIX_INGREDIENT, "obj.brut_caviar")
            column(COL_MIX_LEVEL, 3)
            column(COL_MIX_XP, 0)
            columnRSCM(COL_BARBARIAN_MIX, "obj.brutal_2dose1attack")
        }

        // Strength mix (2-dose strength + roe)
        row("dbrow.herblore_strength_mix") {
            columnRSCM(COL_TWO_DOSE_POTION, "obj.2dose1strength")
            columnRSCM(COL_MIX_INGREDIENT, "obj.brut_roe")
            column(COL_MIX_LEVEL, 12)
            column(COL_MIX_XP, 0)
            columnRSCM(COL_BARBARIAN_MIX, "obj.brutal_2dose1strength")
        }

        // Stat restore mix (2-dose restore + roe)
        row("dbrow.herblore_stat_restore_mix") {
            columnRSCM(COL_TWO_DOSE_POTION, "obj.2dosestatrestore")
            columnRSCM(COL_MIX_INGREDIENT, "obj.brut_roe")
            column(COL_MIX_LEVEL, 22)
            column(COL_MIX_XP, 0)
            columnRSCM(COL_BARBARIAN_MIX, "obj.brutal_2dosestatrestore")
        }

        // Energy mix (2-dose energy + roe)
        row("dbrow.herblore_energy_mix") {
            columnRSCM(COL_TWO_DOSE_POTION, "obj.2dose1energy")
            columnRSCM(COL_MIX_INGREDIENT, "obj.brut_roe")
            column(COL_MIX_LEVEL, 26)
            column(COL_MIX_XP, 0)
            columnRSCM(COL_BARBARIAN_MIX, "obj.brutal_2dose1energy")
        }

        // Defence mix (2-dose defence + roe)
        row("dbrow.herblore_defence_mix") {
            columnRSCM(COL_TWO_DOSE_POTION, "obj.2dose1defense")
            columnRSCM(COL_MIX_INGREDIENT, "obj.brut_roe")
            column(COL_MIX_LEVEL, 30)
            column(COL_MIX_XP, 0)
            columnRSCM(COL_BARBARIAN_MIX, "obj.brutal_2dose1defense")
        }

        // Agility mix (2-dose agility + roe)
        row("dbrow.herblore_agility_mix") {
            columnRSCM(COL_TWO_DOSE_POTION, "obj.2dose1agility")
            columnRSCM(COL_MIX_INGREDIENT, "obj.brut_roe")
            column(COL_MIX_LEVEL, 34)
            column(COL_MIX_XP, 0)
            columnRSCM(COL_BARBARIAN_MIX, "obj.brutal_2dose1agility")
        }

        // Prayer mix (2-dose prayer + roe)
        row("dbrow.herblore_prayer_mix") {
            columnRSCM(COL_TWO_DOSE_POTION, "obj.2doseprayerrestore")
            columnRSCM(COL_MIX_INGREDIENT, "obj.brut_roe")
            column(COL_MIX_LEVEL, 38)
            column(COL_MIX_XP, 0)
            columnRSCM(COL_BARBARIAN_MIX, "obj.brutal_2doseprayerrestore")
        }

        // Super attack mix (2-dose super attack + caviar)
        row("dbrow.herblore_super_attack_mix") {
            columnRSCM(COL_TWO_DOSE_POTION, "obj.2dose2attack")
            columnRSCM(COL_MIX_INGREDIENT, "obj.brut_caviar")
            column(COL_MIX_LEVEL, 45)
            column(COL_MIX_XP, 0)
            columnRSCM(COL_BARBARIAN_MIX, "obj.brutal_2dose2attack")
        }

        // Super antipoison mix (2-dose super antipoison + caviar)
        row("dbrow.herblore_super_antipoison_mix") {
            columnRSCM(COL_TWO_DOSE_POTION, "obj.2dose2antipoison")
            columnRSCM(COL_MIX_INGREDIENT, "obj.brut_caviar")
            column(COL_MIX_LEVEL, 48)
            column(COL_MIX_XP, 0)
            columnRSCM(COL_BARBARIAN_MIX, "obj.brutal_2dose2antipoison")
        }

        // Fishing mix (2-dose fishing + caviar)
        row("dbrow.herblore_fishing_mix") {
            columnRSCM(COL_TWO_DOSE_POTION, "obj.2dosefisherspotion")
            columnRSCM(COL_MIX_INGREDIENT, "obj.brut_caviar")
            column(COL_MIX_LEVEL, 50)
            column(COL_MIX_XP, 0)
            columnRSCM(COL_BARBARIAN_MIX, "obj.brutal_2dosefisherspotion")
        }

        // Super energy mix (2-dose super energy + caviar)
        row("dbrow.herblore_super_energy_mix") {
            columnRSCM(COL_TWO_DOSE_POTION, "obj.2dose2energy")
            columnRSCM(COL_MIX_INGREDIENT, "obj.brut_caviar")
            column(COL_MIX_LEVEL, 52)
            column(COL_MIX_XP, 0)
            columnRSCM(COL_BARBARIAN_MIX, "obj.brutal_2dose2energy")
        }

        // Super strength mix (2-dose super strength + caviar)
        row("dbrow.herblore_super_strength_mix") {
            columnRSCM(COL_TWO_DOSE_POTION, "obj.2dose2strength")
            columnRSCM(COL_MIX_INGREDIENT, "obj.brut_caviar")
            column(COL_MIX_LEVEL, 55)
            column(COL_MIX_XP, 0)
            columnRSCM(COL_BARBARIAN_MIX, "obj.brutal_2dose2strength")
        }

        // Super restore mix (2-dose super restore + caviar)
        row("dbrow.herblore_super_restore_mix") {
            columnRSCM(COL_TWO_DOSE_POTION, "obj.2dose2restore")
            columnRSCM(COL_MIX_INGREDIENT, "obj.brut_caviar")
            column(COL_MIX_LEVEL, 63)
            column(COL_MIX_XP, 0)
            columnRSCM(COL_BARBARIAN_MIX, "obj.brutal_2dose2restore")
        }

        // Super defence mix (2-dose super defence + caviar)
        row("dbrow.herblore_super_defence_mix") {
            columnRSCM(COL_TWO_DOSE_POTION, "obj.2dose2defense")
            columnRSCM(COL_MIX_INGREDIENT, "obj.brut_caviar")
            column(COL_MIX_LEVEL, 66)
            column(COL_MIX_XP, 0)
            columnRSCM(COL_BARBARIAN_MIX, "obj.brutal_2dose2defense")
        }

        // Antifire mix (2-dose antifire + caviar)
        row("dbrow.herblore_antifire_mix") {
            columnRSCM(COL_TWO_DOSE_POTION, "obj.2dose1antidragon")
            columnRSCM(COL_MIX_INGREDIENT, "obj.brut_caviar")
            column(COL_MIX_LEVEL, 69)
            column(COL_MIX_XP, 0)
            columnRSCM(COL_BARBARIAN_MIX, "obj.brutal_2dose1antidragon")
        }

        // Ranging mix (2-dose ranging + caviar)
        row("dbrow.herblore_ranging_mix") {
            columnRSCM(COL_TWO_DOSE_POTION, "obj.2doserangerspotion")
            columnRSCM(COL_MIX_INGREDIENT, "obj.brut_caviar")
            column(COL_MIX_LEVEL, 72)
            column(COL_MIX_XP, 0)
            columnRSCM(COL_BARBARIAN_MIX, "obj.brutal_2doserangerspotion")
        }

        // Magic mix (2-dose magic + caviar)
        row("dbrow.herblore_magic_mix") {
            columnRSCM(COL_TWO_DOSE_POTION, "obj.2dose1magic")
            columnRSCM(COL_MIX_INGREDIENT, "obj.brut_caviar")
            column(COL_MIX_LEVEL, 76)
            column(COL_MIX_XP, 0)
            columnRSCM(COL_BARBARIAN_MIX, "obj.brutal_2dose1magic")
        }

        // Zamorak mix (2-dose zamorak + caviar)
        row("dbrow.herblore_zamorak_mix") {
            columnRSCM(COL_TWO_DOSE_POTION, "obj.2dosepotionofzamorak")
            columnRSCM(COL_MIX_INGREDIENT, "obj.brut_caviar")
            column(COL_MIX_LEVEL, 78)
            column(COL_MIX_XP, 0)
            columnRSCM(COL_BARBARIAN_MIX, "obj.brutal_2dosepotionofzamorak")
        }
    }

    /**
     * Table for creating swamp tar
     */
    fun swampTar() = dbTable("dbtable.herblore_swamp_tar", serverOnly = true) {
        column("herb", COL_TAR_HERB, VarType.OBJ)
        column("level", COL_TAR_LEVEL, VarType.INT)
        column("xp", COL_TAR_XP, VarType.INT)
        column("finished_tar", COL_TAR_FINISHED, VarType.OBJ)

        // Guam tar (Guam leaf + 15x swamp tar = 15x green tar)
        row("dbrow.herblore_guam_tar") {
            columnRSCM(COL_TAR_HERB, "obj.guam_leaf")
            column(COL_TAR_LEVEL, 19)
            column(COL_TAR_XP, 30)
            columnRSCM(COL_TAR_FINISHED, "obj.salamander_tar_green")
        }

        // Marrentill tar (Marrentill + 15x swamp tar = 15x orange tar)
        row("dbrow.herblore_marrentill_tar") {
            columnRSCM(COL_TAR_HERB, "obj.marentill")
            column(COL_TAR_LEVEL, 31)
            column(COL_TAR_XP, 42)
            columnRSCM(COL_TAR_FINISHED, "obj.salamander_tar_orange")
        }

        // Tarromin tar (Tarromin + 15x swamp tar = 15x red tar)
        row("dbrow.herblore_tarromin_tar") {
            columnRSCM(COL_TAR_HERB, "obj.tarromin")
            column(COL_TAR_LEVEL, 39)
            column(COL_TAR_XP, 55)
            columnRSCM(COL_TAR_FINISHED, "obj.salamander_tar_red")
        }

        // Harralander tar (Harralander + 15x swamp tar = 15x black tar)
        row("dbrow.herblore_harralander_tar") {
            columnRSCM(COL_TAR_HERB, "obj.harralander")
            column(COL_TAR_LEVEL, 44)
            column(COL_TAR_XP, 72)
            columnRSCM(COL_TAR_FINISHED, "obj.salamander_tar_black")
        }

        // Irit tar (Irit leaf + 15x swamp tar = 15x mountain tar)
        row("dbrow.herblore_irit_tar") {
            columnRSCM(COL_TAR_HERB, "obj.irit_leaf")
            column(COL_TAR_LEVEL, 50)
            column(COL_TAR_XP, 84)
            columnRSCM(COL_TAR_FINISHED, "obj.salamander_tar_mountain")
        }
    }

    /**
     * Table for crushing items with pestle and mortar
     * Auto-crushes every 3 ticks when multiple items are available
     */
    fun crushing() = dbTable("dbtable.herblore_crushing", serverOnly = true) {
        column("item", COL_CRUSH_ITEM, VarType.OBJ)
        column("level", COL_CRUSH_LEVEL, VarType.INT)
        column("xp", COL_CRUSH_XP, VarType.INT)
        column("crushed_item", COL_CRUSHED_ITEM, VarType.OBJ)

        // Bird nest (empty) → Crushed bird nest
        row("dbrow.herblore_crush_bird_nest") {
            columnRSCM(COL_CRUSH_ITEM, "obj.bird_nest_empty")
            column(COL_CRUSH_LEVEL, 1)
            column(COL_CRUSH_XP, 0)
            columnRSCM(COL_CRUSHED_ITEM, "obj.crushed_bird_nest")
        }

        // Chocolate bar → Chocolate dust
        row("dbrow.herblore_crush_chocolate") {
            columnRSCM(COL_CRUSH_ITEM, "obj.chocolate_bar")
            column(COL_CRUSH_LEVEL, 1)
            column(COL_CRUSH_XP, 0)
            columnRSCM(COL_CRUSHED_ITEM, "obj.chocolate_dust")
        }

        // Unicorn horn → Unicorn horn dust
        row("dbrow.herblore_crush_unicorn_horn") {
            columnRSCM(COL_CRUSH_ITEM, "obj.unicorn_horn")
            column(COL_CRUSH_LEVEL, 1)
            column(COL_CRUSH_XP, 0)
            columnRSCM(COL_CRUSHED_ITEM, "obj.unicorn_horn_dust")
        }

        // Blue dragon scale → Dragon scale dust
        row("dbrow.herblore_crush_dragon_scale") {
            columnRSCM(COL_CRUSH_ITEM, "obj.blue_dragon_scale")
            column(COL_CRUSH_LEVEL, 1)
            column(COL_CRUSH_XP, 0)
            columnRSCM(COL_CRUSHED_ITEM, "obj.dragon_scale_dust")
        }

        // Desert goat horn → Ground desert goat horn
        row("dbrow.herblore_crush_goat_horn") {
            columnRSCM(COL_CRUSH_ITEM, "obj.desert_goat_horn")
            column(COL_CRUSH_LEVEL, 1)
            column(COL_CRUSH_XP, 0)
            columnRSCM(COL_CRUSHED_ITEM, "obj.ground_desert_goat_horn")
        }

        // Superior dragon bones → Crushed superior dragon bones
        row("dbrow.herblore_crush_superior_dragon_bones") {
            columnRSCM(COL_CRUSH_ITEM, "obj.dragon_bones_superior")
            column(COL_CRUSH_LEVEL, 1)
            column(COL_CRUSH_XP, 0)
            columnRSCM(COL_CRUSHED_ITEM, "obj.crushed_dragon_bones")
        }
    }
}

