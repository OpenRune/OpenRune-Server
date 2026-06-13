package dev.openrune.tables.skills

import dev.openrune.tables.production.productionTable

object Herblore {

    /**
     * Table for creating unfinished potions (herb + vial of water)
     */
    fun unfinishedPotions() =
        productionTable("dbtable.herblore_unfinished", serverOnly = true) {

            row("dbrow.herblore_guam_unfinished") {
                production {
                    input("obj.guam_leaf")
                    statReq("stat.herblore", 3)
                    xp(2)
                    output("obj.guamvial")
                    category("Unfinished Potions")
                }
            }
            row("dbrow.herblore_marrentill_unfinished") {
                production {
                    input("obj.marentill")
                    statReq("stat.herblore", 5)
                    xp(3)
                    output("obj.marrentillvial")
                    category("Unfinished Potions")
                }
            }
            row("dbrow.herblore_tarromin_unfinished") {
                production {
                    input("obj.tarromin")
                    statReq("stat.herblore", 11)
                    xp(5)
                    output("obj.tarrominvial")
                    category("Unfinished Potions")
                }
            }
            row("dbrow.herblore_harralander_unfinished") {
                production {
                    input("obj.harralander")
                    statReq("stat.herblore", 20)
                    xp(6)
                    output("obj.harralandervial")
                    category("Unfinished Potions")
                }
            }
            row("dbrow.herblore_ranarr_unfinished") {
                production {
                    input("obj.ranarr_weed")
                    statReq("stat.herblore", 25)
                    xp(8)
                    output("obj.ranarrvial")
                    category("Unfinished Potions")
                }
            }
            row("dbrow.herblore_toadflax_unfinished") {
                production {
                    input("obj.toadflax")
                    statReq("stat.herblore", 30)
                    xp(8)
                    output("obj.toadflaxvial")
                    category("Unfinished Potions")
                }
            }
            row("dbrow.herblore_irit_unfinished") {
                production {
                    input("obj.irit_leaf")
                    statReq("stat.herblore", 40)
                    xp(9)
                    output("obj.iritvial")
                    category("Unfinished Potions")
                }
            }
            row("dbrow.herblore_avantoe_unfinished") {
                production {
                    input("obj.avantoe")
                    statReq("stat.herblore", 48)
                    xp(10)
                    output("obj.avantoevial")
                    category("Unfinished Potions")
                }
            }
            row("dbrow.herblore_kwuarm_unfinished") {
                production {
                    input("obj.kwuarm")
                    statReq("stat.herblore", 54)
                    xp(11)
                    output("obj.kwuarmvial")
                    category("Unfinished Potions")
                }
            }
            row("dbrow.herblore_snapdragon_unfinished") {
                production {
                    input("obj.snapdragon")
                    statReq("stat.herblore", 59)
                    xp(12)
                    output("obj.snapdragonvial")
                    category("Unfinished Potions")
                }
            }
            row("dbrow.herblore_cadantine_unfinished") {
                production {
                    input("obj.cadantine")
                    statReq("stat.herblore", 65)
                    xp(13)
                    output("obj.cadantinevial")
                    category("Unfinished Potions")
                }
            }
            row("dbrow.herblore_lantadyme_unfinished") {
                production {
                    input("obj.lantadyme")
                    statReq("stat.herblore", 67)
                    xp(13)
                    output("obj.lantadymevial")
                    category("Unfinished Potions")
                }
            }
            row("dbrow.herblore_dwarf_weed_unfinished") {
                production {
                    input("obj.dwarf_weed")
                    statReq("stat.herblore", 70)
                    xp(13)
                    output("obj.dwarfweedvial")
                    category("Unfinished Potions")
                }
            }
            row("dbrow.herblore_torstol_unfinished") {
                production {
                    input("obj.torstol")
                    statReq("stat.herblore", 75)
                    xp(14)
                    output("obj.torstolvial")
                    category("Unfinished Potions")
                }
            }

        }

    /**
     * Table for creating finished potions (unfinished potion + secondary ingredient)
     */
    fun finishedPotions() =
        productionTable("dbtable.herblore_finished", serverOnly = true) {

            row("dbrow.herblore_attack_potion") {
                production {
                    input("obj.guamvial")
                    input("obj.eye_of_newt")
                    statReq("stat.herblore", 3)
                    xp(25)
                    output("obj.3dose1attack")
                    category("Potions")
                }
            }
            row("dbrow.herblore_antipoison") {
                production {
                    input("obj.marrentillvial")
                    input("obj.unicorn_horn_dust")
                    statReq("stat.herblore", 5)
                    xp(38)
                    output("obj.3doseantipoison")
                    category("Potions")
                }
            }
            row("dbrow.herblore_strength_potion") {
                production {
                    input("obj.tarrominvial")
                    input("obj.limpwurt_root")
                    statReq("stat.herblore", 12)
                    xp(50)
                    output("obj.3dose1strength")
                    category("Potions")
                }
            }
            row("dbrow.herblore_restore_potion") {
                production {
                    input("obj.harralandervial")
                    input("obj.red_spiders_eggs")
                    statReq("stat.herblore", 22)
                    xp(63)
                    output("obj.3dosestatrestore")
                    category("Potions")
                }
            }
            row("dbrow.herblore_energy_potion") {
                production {
                    input("obj.harralandervial")
                    input("obj.chocolate_dust")
                    statReq("stat.herblore", 26)
                    xp(68)
                    output("obj.3dose1energy")
                    category("Potions")
                }
            }
            row("dbrow.herblore_prayer_potion") {
                production {
                    input("obj.ranarrvial")
                    input("obj.snape_grass")
                    statReq("stat.herblore", 38)
                    xp(88)
                    output("obj.3doseprayerrestore")
                    category("Potions")
                }
            }
            row("dbrow.herblore_super_attack") {
                production {
                    input("obj.iritvial")
                    input("obj.eye_of_newt")
                    statReq("stat.herblore", 45)
                    xp(100)
                    output("obj.3dose2attack")
                    category("Potions")
                }
            }
            row("dbrow.herblore_superantipoison") {
                production {
                    input("obj.iritvial")
                    input("obj.unicorn_horn_dust")
                    statReq("stat.herblore", 48)
                    xp(105)
                    output("obj.3dose2antipoison")
                    category("Potions")
                }
            }
            row("dbrow.herblore_fishing_potion") {
                production {
                    input("obj.avantoevial")
                    input("obj.snape_grass")
                    statReq("stat.herblore", 50)
                    xp(113)
                    output("obj.3dosefisherspotion")
                    category("Potions")
                }
            }
            row("dbrow.herblore_super_energy") {
                production {
                    input("obj.avantoevial")
                    input("obj.mortmyremushroom")
                    statReq("stat.herblore", 52)
                    xp(118)
                    output("obj.3dose2energy")
                    category("Potions")
                }
            }
            row("dbrow.herblore_super_strength") {
                production {
                    input("obj.kwuarmvial")
                    input("obj.limpwurt_root")
                    statReq("stat.herblore", 55)
                    xp(125)
                    output("obj.3dose2strength")
                    category("Potions")
                }
            }
            row("dbrow.herblore_weapon_poison") {
                production {
                    input("obj.kwuarmvial")
                    input("obj.dragon_scale_dust")
                    statReq("stat.herblore", 60)
                    xp(138)
                    output("obj.weapon_poison")
                    category("Potions")
                }
            }
            row("dbrow.herblore_super_restore") {
                production {
                    input("obj.snapdragonvial")
                    input("obj.red_spiders_eggs")
                    statReq("stat.herblore", 63)
                    xp(143)
                    output("obj.3dose2restore")
                    category("Potions")
                }
            }
            row("dbrow.herblore_super_defence") {
                production {
                    input("obj.cadantinevial")
                    input("obj.white_berries")
                    statReq("stat.herblore", 66)
                    xp(150)
                    output("obj.3dose2defense")
                    category("Potions")
                }
            }
            row("dbrow.herblore_antifire") {
                production {
                    input("obj.lantadymevial")
                    input("obj.dragon_scale_dust")
                    statReq("stat.herblore", 69)
                    xp(158)
                    output("obj.3dose1antidragon")
                    category("Potions")
                }
            }
            row("dbrow.herblore_super_antifire") {
                production {
                    input("obj.lantadymevial")
                    input("obj.crushed_dragon_bones")
                    statReq("stat.herblore", 92)
                    xp(180)
                    output("obj.3dose2antidragon")
                    category("Potions")
                }
            }
            row("dbrow.herblore_ranging_potion") {
                production {
                    input("obj.dwarfweedvial")
                    input("obj.wine_of_zamorak")
                    statReq("stat.herblore", 72)
                    xp(163)
                    output("obj.3doserangerspotion")
                    category("Potions")
                }
            }
            row("dbrow.herblore_magic_potion") {
                production {
                    input("obj.lantadymevial")
                    input("obj.cactus_potato")
                    statReq("stat.herblore", 76)
                    xp(173)
                    output("obj.3dose1magic")
                    category("Potions")
                }
            }
            row("dbrow.herblore_zamorak_brew") {
                production {
                    input("obj.torstolvial")
                    input("obj.jangerberries")
                    statReq("stat.herblore", 78)
                    xp(175)
                    output("obj.3dosepotionofzamorak")
                    category("Potions")
                }
            }
            row("dbrow.herblore_saradomin_brew") {
                production {
                    input("obj.toadflaxvial")
                    input("obj.crushed_bird_nest")
                    statReq("stat.herblore", 81)
                    xp(180)
                    output("obj.3dosepotionofsaradomin")
                    category("Potions")
                }
            }
            row("dbrow.herblore_defence_potion") {
                production {
                    input("obj.ranarrvial")
                    input("obj.white_berries")
                    statReq("stat.herblore", 30)
                    xp(75)
                    output("obj.3dose1defense")
                    category("Potions")
                }
            }
            row("dbrow.herblore_agility_potion") {
                production {
                    input("obj.toadflaxvial")
                    input("obj.toads_legs")
                    statReq("stat.herblore", 34)
                    xp(80)
                    output("obj.3dose1agility")
                    category("Potions")
                }
            }
            row("dbrow.herblore_combat_potion") {
                production {
                    input("obj.harralandervial")
                    input("obj.ground_desert_goat_horn")
                    statReq("stat.herblore", 36)
                    xp(84)
                    output("obj.3dosecombat")
                    category("Potions")
                }
            }
            row("dbrow.herblore_hunter_potion") {
                production {
                    input("obj.avantoevial")
                    input("obj.huntingbeast_sabreteeth")
                    statReq("stat.herblore", 53)
                    xp(120)
                    output("obj.3dosehunting")
                    category("Potions")
                }
            }
            row("dbrow.herblore_bastion_potion") {
                production {
                    input("obj.cadantinevial")
                    input("obj.wine_of_zamorak")
                    input("obj.crushed_dragon_bones")
                    statReq("stat.herblore", 80)
                    xp(155)
                    output("obj.3dosebastion")
                    category("Potions")
                }
            }
            row("dbrow.herblore_battlemage_potion") {
                production {
                    input("obj.cadantinevial")
                    input("obj.cactus_potato")
                    input("obj.crushed_dragon_bones")
                    statReq("stat.herblore", 79)
                    xp(155)
                    output("obj.3dosebattlemage")
                    category("Potions")
                }
            }
            row("dbrow.herblore_super_combat_potion") {
                production {
                    input("obj.torstol")
                    input("obj.3dose2attack")
                    input("obj.3dose2strength")
                    input("obj.3dose2defense")
                    statReq("stat.herblore", 90)
                    xp(150)
                    output("obj.3dose2combat")
                    category("Potions")
                }
            }
            row("dbrow.herblore_extended_antivenom_plus") {
                production {
                    input("obj.antivenom+3")
                    input("obj.snakeboss_scale")
                    statReq("stat.herblore", 94)
                    xp(160)
                    output("obj.extended_antivenom+3")
                    category("Potions")
                }
            }
            row("dbrow.herblore_stamina_potion_1") {
                production {
                    input("obj.1dose2energy")
                    input("obj.amylase")
                    statReq("stat.herblore", 77)
                    xp(25)
                    output("obj.1dosestamina")
                    category("Potions")
                }
            }
            row("dbrow.herblore_stamina_potion_2") {
                production {
                    input("obj.2dose2energy")
                    input("obj.amylase", 2)
                    statReq("stat.herblore", 77)
                    xp(51)
                    output("obj.2dosestamina")
                    category("Potions")
                }
            }
            row("dbrow.herblore_stamina_potion_3") {
                production {
                    input("obj.3dose2energy")
                    input("obj.amylase", 3)
                    statReq("stat.herblore", 77)
                    xp(77)
                    output("obj.3dosestamina")
                    category("Potions")
                }
            }
            row("dbrow.herblore_stamina_potion_4") {
                production {
                    input("obj.4dose2energy")
                    input("obj.amylase", 5)
                    statReq("stat.herblore", 77)
                    xp(102)
                    output("obj.4dosestamina")
                    category("Potions")
                }
            }
            row("dbrow.herblore_divine_bastion_1") {
                production {
                    input("obj.1dosebastion")
                    input("obj.sote_crystal_dust")
                    statReq("stat.herblore", 86)
                    xp(5)
                    output("obj.1dosedivinebastion")
                    category("Potions")
                }
            }
            row("dbrow.herblore_divine_bastion_2") {
                production {
                    input("obj.2dosebastion")
                    input("obj.sote_crystal_dust", 2)
                    statReq("stat.herblore", 86)
                    xp(10)
                    output("obj.2dosedivinebastion")
                    category("Potions")
                }
            }
            row("dbrow.herblore_divine_bastion_3") {
                production {
                    input("obj.3dosebastion")
                    input("obj.sote_crystal_dust", 3)
                    statReq("stat.herblore", 86)
                    xp(15)
                    output("obj.3dosedivinebastion")
                    category("Potions")
                }
            }
            row("dbrow.herblore_divine_bastion_4") {
                production {
                    input("obj.4dosebastion")
                    input("obj.sote_crystal_dust", 4)
                    statReq("stat.herblore", 86)
                    xp(20)
                    output("obj.4dosedivinebastion")
                    category("Potions")
                }
            }
            row("dbrow.herblore_divine_battlemage_1") {
                production {
                    input("obj.1dosebattlemage")
                    input("obj.sote_crystal_dust")
                    statReq("stat.herblore", 86)
                    xp(5)
                    output("obj.1dosedivinebattlemage")
                    category("Potions")
                }
            }
            row("dbrow.herblore_divine_battlemage_2") {
                production {
                    input("obj.2dosebattlemage")
                    input("obj.sote_crystal_dust", 2)
                    statReq("stat.herblore", 86)
                    xp(10)
                    output("obj.2dosedivinebattlemage")
                    category("Potions")
                }
            }
            row("dbrow.herblore_divine_battlemage_3") {
                production {
                    input("obj.3dosebattlemage")
                    input("obj.sote_crystal_dust", 3)
                    statReq("stat.herblore", 86)
                    xp(15)
                    output("obj.3dosedivinebattlemage")
                    category("Potions")
                }
            }
            row("dbrow.herblore_divine_battlemage_4") {
                production {
                    input("obj.4dosebattlemage")
                    input("obj.sote_crystal_dust", 4)
                    statReq("stat.herblore", 86)
                    xp(20)
                    output("obj.4dosedivinebattlemage")
                    category("Potions")
                }
            }
            row("dbrow.herblore_divine_magic_1") {
                production {
                    input("obj.1dose1magic")
                    input("obj.sote_crystal_dust")
                    statReq("stat.herblore", 78)
                    xp(5)
                    output("obj.1dosedivinemagic")
                    category("Potions")
                }
            }
            row("dbrow.herblore_divine_magic_2") {
                production {
                    input("obj.2dose1magic")
                    input("obj.sote_crystal_dust", 2)
                    statReq("stat.herblore", 78)
                    xp(10)
                    output("obj.2dosedivinemagic")
                    category("Potions")
                }
            }
            row("dbrow.herblore_divine_magic_3") {
                production {
                    input("obj.3dose1magic")
                    input("obj.sote_crystal_dust", 3)
                    statReq("stat.herblore", 78)
                    xp(15)
                    output("obj.3dosedivinemagic")
                    category("Potions")
                }
            }
            row("dbrow.herblore_divine_magic_4") {
                production {
                    input("obj.4dose1magic")
                    input("obj.sote_crystal_dust", 4)
                    statReq("stat.herblore", 78)
                    xp(20)
                    output("obj.4dosedivinemagic")
                    category("Potions")
                }
            }
            row("dbrow.herblore_divine_ranging_1") {
                production {
                    input("obj.1doserangerspotion")
                    input("obj.sote_crystal_dust")
                    statReq("stat.herblore", 74)
                    xp(5)
                    output("obj.1dosedivinerange")
                    category("Potions")
                }
            }
            row("dbrow.herblore_divine_ranging_2") {
                production {
                    input("obj.2doserangerspotion")
                    input("obj.sote_crystal_dust", 2)
                    statReq("stat.herblore", 74)
                    xp(10)
                    output("obj.2dosedivinerange")
                    category("Potions")
                }
            }
            row("dbrow.herblore_divine_ranging_3") {
                production {
                    input("obj.3doserangerspotion")
                    input("obj.sote_crystal_dust", 3)
                    statReq("stat.herblore", 74)
                    xp(15)
                    output("obj.3dosedivinerange")
                    category("Potions")
                }
            }
            row("dbrow.herblore_divine_ranging_4") {
                production {
                    input("obj.4doserangerspotion")
                    input("obj.sote_crystal_dust", 4)
                    statReq("stat.herblore", 74)
                    xp(20)
                    output("obj.4dosedivinerange")
                    category("Potions")
                }
            }
            row("dbrow.herblore_divine_super_attack_1") {
                production {
                    input("obj.1dose2attack")
                    input("obj.sote_crystal_dust")
                    statReq("stat.herblore", 70)
                    xp(5)
                    output("obj.1dosedivineattack")
                    category("Potions")
                }
            }
            row("dbrow.herblore_divine_super_attack_2") {
                production {
                    input("obj.2dose2attack")
                    input("obj.sote_crystal_dust", 2)
                    statReq("stat.herblore", 70)
                    xp(10)
                    output("obj.2dosedivineattack")
                    category("Potions")
                }
            }
            row("dbrow.herblore_divine_super_attack_3") {
                production {
                    input("obj.3dose2attack")
                    input("obj.sote_crystal_dust", 3)
                    statReq("stat.herblore", 70)
                    xp(15)
                    output("obj.3dosedivineattack")
                    category("Potions")
                }
            }
            row("dbrow.herblore_divine_super_attack_4") {
                production {
                    input("obj.4dose2attack")
                    input("obj.sote_crystal_dust", 4)
                    statReq("stat.herblore", 70)
                    xp(20)
                    output("obj.4dosedivineattack")
                    category("Potions")
                }
            }
            row("dbrow.herblore_divine_super_combat_1") {
                production {
                    input("obj.1dose2combat")
                    input("obj.sote_crystal_dust")
                    statReq("stat.herblore", 97)
                    xp(5)
                    output("obj.1dosedivinecombat")
                    category("Potions")
                }
            }
            row("dbrow.herblore_divine_super_combat_2") {
                production {
                    input("obj.2dose2combat")
                    input("obj.sote_crystal_dust", 2)
                    statReq("stat.herblore", 97)
                    xp(10)
                    output("obj.2dosedivinecombat")
                    category("Potions")
                }
            }
            row("dbrow.herblore_divine_super_combat_3") {
                production {
                    input("obj.3dose2combat")
                    input("obj.sote_crystal_dust", 3)
                    statReq("stat.herblore", 97)
                    xp(15)
                    output("obj.3dosedivinecombat")
                    category("Potions")
                }
            }
            row("dbrow.herblore_divine_super_combat_4") {
                production {
                    input("obj.4dose2combat")
                    input("obj.sote_crystal_dust", 4)
                    statReq("stat.herblore", 97)
                    xp(20)
                    output("obj.4dosedivinecombat")
                    category("Potions")
                }
            }
            row("dbrow.herblore_divine_super_defence_1") {
                production {
                    input("obj.1dose2defense")
                    input("obj.sote_crystal_dust")
                    statReq("stat.herblore", 70)
                    xp(5)
                    output("obj.1dosedivinedefence")
                    category("Potions")
                }
            }
            row("dbrow.herblore_divine_super_defence_2") {
                production {
                    input("obj.2dose2defense")
                    input("obj.sote_crystal_dust", 2)
                    statReq("stat.herblore", 70)
                    xp(10)
                    output("obj.2dosedivinedefence")
                    category("Potions")
                }
            }
            row("dbrow.herblore_divine_super_defence_3") {
                production {
                    input("obj.3dose2defense")
                    input("obj.sote_crystal_dust", 3)
                    statReq("stat.herblore", 70)
                    xp(15)
                    output("obj.3dosedivinedefence")
                    category("Potions")
                }
            }
            row("dbrow.herblore_divine_super_defence_4") {
                production {
                    input("obj.4dose2defense")
                    input("obj.sote_crystal_dust", 4)
                    statReq("stat.herblore", 70)
                    xp(20)
                    output("obj.4dosedivinedefence")
                    category("Potions")
                }
            }
            row("dbrow.herblore_divine_super_strength_1") {
                production {
                    input("obj.1dose2strength")
                    input("obj.sote_crystal_dust")
                    statReq("stat.herblore", 70)
                    xp(5)
                    output("obj.1dosedivinestrength")
                    category("Potions")
                }
            }
            row("dbrow.herblore_divine_super_strength_2") {
                production {
                    input("obj.2dose2strength")
                    input("obj.sote_crystal_dust", 2)
                    statReq("stat.herblore", 70)
                    xp(10)
                    output("obj.2dosedivinestrength")
                    category("Potions")
                }
            }
            row("dbrow.herblore_divine_super_strength_3") {
                production {
                    input("obj.3dose2strength")
                    input("obj.sote_crystal_dust", 3)
                    statReq("stat.herblore", 70)
                    xp(15)
                    output("obj.3dosedivinestrength")
                    category("Potions")
                }
            }
            row("dbrow.herblore_divine_super_strength_4") {
                production {
                    input("obj.4dose2strength")
                    input("obj.sote_crystal_dust", 4)
                    statReq("stat.herblore", 70)
                    xp(20)
                    output("obj.4dosedivinestrength")
                    category("Potions")
                }
            }

        }

    /**
     * Table for cleaning grimy herbs (unidentified -> clean)
     */
    fun cleaningHerbs() =
        productionTable("dbtable.herblore_cleaning", serverOnly = true) {
            row("dbrow.herblore_clean_guam") {
                production {
                    input("obj.unidentified_guam")
                    statReq("stat.herblore", 3)
                    xp(2)
                    output("obj.guam_leaf")
                    category("Herbs")
                }
            }

            row("dbrow.herblore_clean_marrentill") {
                production {
                    input("obj.unidentified_marentill")
                    statReq("stat.herblore", 5)
                    xp(3)
                    output("obj.marentill")
                    category("Herbs")
                }
            }

            row("dbrow.herblore_clean_tarromin") {
                production {
                    input("obj.unidentified_tarromin")
                    statReq("stat.herblore", 11)
                    xp(5)
                    output("obj.tarromin")
                    category("Herbs")
                }
            }

            row("dbrow.herblore_clean_harralander") {
                production {
                    input("obj.unidentified_harralander")
                    statReq("stat.herblore", 20)
                    xp(6)
                    output("obj.harralander")
                    category("Herbs")
                }
            }

            row("dbrow.herblore_clean_ranarr") {
                production {
                    input("obj.unidentified_ranarr")
                    statReq("stat.herblore", 25)
                    xp(8)
                    output("obj.ranarr_weed")
                    category("Herbs")
                }
            }

            row("dbrow.herblore_clean_toadflax") {
                production {
                    input("obj.unidentified_toadflax")
                    statReq("stat.herblore", 30)
                    xp(8)
                    output("obj.toadflax")
                    category("Herbs")
                }
            }

            row("dbrow.herblore_clean_irit") {
                production {
                    input("obj.unidentified_irit")
                    statReq("stat.herblore", 40)
                    xp(9)
                    output("obj.irit_leaf")
                    category("Herbs")
                }
            }

            row("dbrow.herblore_clean_avantoe") {
                production {
                    input("obj.unidentified_avantoe")
                    statReq("stat.herblore", 48)
                    xp(10)
                    output("obj.avantoe")
                    category("Herbs")
                }
            }

            row("dbrow.herblore_clean_kwuarm") {
                production {
                    input("obj.unidentified_kwuarm")
                    statReq("stat.herblore", 54)
                    xp(11)
                    output("obj.kwuarm")
                    category("Herbs")
                }
            }

            row("dbrow.herblore_clean_snapdragon") {
                production {
                    input("obj.unidentified_snapdragon")
                    statReq("stat.herblore", 59)
                    xp(12)
                    output("obj.snapdragon")
                    category("Herbs")
                }
            }

            row("dbrow.herblore_clean_cadantine") {
                production {
                    input("obj.unidentified_cadantine")
                    statReq("stat.herblore", 65)
                    xp(13)
                    output("obj.cadantine")
                    category("Herbs")
                }
            }

            row("dbrow.herblore_clean_lantadyme") {
                production {
                    input("obj.unidentified_lantadyme")
                    statReq("stat.herblore", 67)
                    xp(13)
                    output("obj.lantadyme")
                    category("Herbs")
                }
            }

            row("dbrow.herblore_clean_dwarf_weed") {
                production {
                    input("obj.unidentified_dwarf_weed")
                    statReq("stat.herblore", 70)
                    xp(13)
                    output("obj.dwarf_weed")
                    category("Herbs")
                }
            }

            row("dbrow.herblore_clean_torstol") {
                production {
                    input("obj.unidentified_torstol")
                    statReq("stat.herblore", 75)
                    xp(14)
                    output("obj.torstol")
                    category("Herbs")
                }
            }
        }


    /**
     * Table for creating barbarian mixes (two-dose potion + roe/caviar)
     */
    fun barbarianMixes() =
        productionTable("dbtable.herblore_barbarian_mixes", serverOnly = true) {

            row("dbrow.herblore_attack_mix") {
                production {
                    input("obj.2dose1attack")
                    input("obj.brut_roe")
                    statReq("stat.herblore", 3)
                    xp(0)
                    output("obj.brutal_2dose1attack")
                    category("Barbarian Mixes")
                }
            }
            row("dbrow.herblore_antipoison_mix") {
                production {
                    input("obj.2doseantipoison")
                    input("obj.brut_roe")
                    statReq("stat.herblore", 5)
                    xp(0)
                    output("obj.brutal_2doseantipoison")
                    category("Barbarian Mixes")
                }
            }
            row("dbrow.herblore_attack_mix_caviar") {
                production {
                    input("obj.2dose1attack")
                    input("obj.brut_caviar")
                    statReq("stat.herblore", 3)
                    xp(0)
                    output("obj.brutal_2dose1attack")
                    category("Barbarian Mixes")
                }
            }
            row("dbrow.herblore_strength_mix") {
                production {
                    input("obj.2dose1strength")
                    input("obj.brut_roe")
                    statReq("stat.herblore", 12)
                    xp(0)
                    output("obj.brutal_2dose1strength")
                    category("Barbarian Mixes")
                }
            }
            row("dbrow.herblore_stat_restore_mix") {
                production {
                    input("obj.2dosestatrestore")
                    input("obj.brut_roe")
                    statReq("stat.herblore", 22)
                    xp(0)
                    output("obj.brutal_2dosestatrestore")
                    category("Barbarian Mixes")
                }
            }
            row("dbrow.herblore_energy_mix") {
                production {
                    input("obj.2dose1energy")
                    input("obj.brut_roe")
                    statReq("stat.herblore", 26)
                    xp(0)
                    output("obj.brutal_2dose1energy")
                    category("Barbarian Mixes")
                }
            }
            row("dbrow.herblore_defence_mix") {
                production {
                    input("obj.2dose1defense")
                    input("obj.brut_roe")
                    statReq("stat.herblore", 30)
                    xp(0)
                    output("obj.brutal_2dose1defense")
                    category("Barbarian Mixes")
                }
            }
            row("dbrow.herblore_agility_mix") {
                production {
                    input("obj.2dose1agility")
                    input("obj.brut_roe")
                    statReq("stat.herblore", 34)
                    xp(0)
                    output("obj.brutal_2dose1agility")
                    category("Barbarian Mixes")
                }
            }
            row("dbrow.herblore_prayer_mix") {
                production {
                    input("obj.2doseprayerrestore")
                    input("obj.brut_roe")
                    statReq("stat.herblore", 38)
                    xp(0)
                    output("obj.brutal_2doseprayerrestore")
                    category("Barbarian Mixes")
                }
            }
            row("dbrow.herblore_super_attack_mix") {
                production {
                    input("obj.2dose2attack")
                    input("obj.brut_caviar")
                    statReq("stat.herblore", 45)
                    xp(0)
                    output("obj.brutal_2dose2attack")
                    category("Barbarian Mixes")
                }
            }
            row("dbrow.herblore_super_antipoison_mix") {
                production {
                    input("obj.2dose2antipoison")
                    input("obj.brut_caviar")
                    statReq("stat.herblore", 48)
                    xp(0)
                    output("obj.brutal_2dose2antipoison")
                    category("Barbarian Mixes")
                }
            }
            row("dbrow.herblore_fishing_mix") {
                production {
                    input("obj.2dosefisherspotion")
                    input("obj.brut_caviar")
                    statReq("stat.herblore", 50)
                    xp(0)
                    output("obj.brutal_2dosefisherspotion")
                    category("Barbarian Mixes")
                }
            }
            row("dbrow.herblore_super_energy_mix") {
                production {
                    input("obj.2dose2energy")
                    input("obj.brut_caviar")
                    statReq("stat.herblore", 52)
                    xp(0)
                    output("obj.brutal_2dose2energy")
                    category("Barbarian Mixes")
                }
            }
            row("dbrow.herblore_super_strength_mix") {
                production {
                    input("obj.2dose2strength")
                    input("obj.brut_caviar")
                    statReq("stat.herblore", 55)
                    xp(0)
                    output("obj.brutal_2dose2strength")
                    category("Barbarian Mixes")
                }
            }
            row("dbrow.herblore_super_restore_mix") {
                production {
                    input("obj.2dose2restore")
                    input("obj.brut_caviar")
                    statReq("stat.herblore", 63)
                    xp(0)
                    output("obj.brutal_2dose2restore")
                    category("Barbarian Mixes")
                }
            }
            row("dbrow.herblore_super_defence_mix") {
                production {
                    input("obj.2dose2defense")
                    input("obj.brut_caviar")
                    statReq("stat.herblore", 66)
                    xp(0)
                    output("obj.brutal_2dose2defense")
                    category("Barbarian Mixes")
                }
            }
            row("dbrow.herblore_antifire_mix") {
                production {
                    input("obj.2dose1antidragon")
                    input("obj.brut_caviar")
                    statReq("stat.herblore", 69)
                    xp(0)
                    output("obj.brutal_2dose1antidragon")
                    category("Barbarian Mixes")
                }
            }
            row("dbrow.herblore_ranging_mix") {
                production {
                    input("obj.2doserangerspotion")
                    input("obj.brut_caviar")
                    statReq("stat.herblore", 72)
                    xp(0)
                    output("obj.brutal_2doserangerspotion")
                    category("Barbarian Mixes")
                }
            }
            row("dbrow.herblore_magic_mix") {
                production {
                    input("obj.2dose1magic")
                    input("obj.brut_caviar")
                    statReq("stat.herblore", 76)
                    xp(0)
                    output("obj.brutal_2dose1magic")
                    category("Barbarian Mixes")
                }
            }
            row("dbrow.herblore_zamorak_mix") {
                production {
                    input("obj.2dosepotionofzamorak")
                    input("obj.brut_caviar")
                    statReq("stat.herblore", 78)
                    xp(0)
                    output("obj.brutal_2dosepotionofzamorak")
                    category("Barbarian Mixes")
                }
            }

        }

    /**
     * Table for creating swamp tar
     */
    fun swampTar() =
        productionTable("dbtable.herblore_swamp_tar", serverOnly = true) {

            row("dbrow.herblore_guam_tar") {
                production {
                    input("obj.guam_leaf")
                    statReq("stat.herblore", 19)
                    xp(30)
                    output("obj.salamander_tar_green")
                    category("Swamp Tar")
                }
            }
            row("dbrow.herblore_marrentill_tar") {
                production {
                    input("obj.marentill")
                    statReq("stat.herblore", 31)
                    xp(42)
                    output("obj.salamander_tar_orange")
                    category("Swamp Tar")
                }
            }
            row("dbrow.herblore_tarromin_tar") {
                production {
                    input("obj.tarromin")
                    statReq("stat.herblore", 39)
                    xp(55)
                    output("obj.salamander_tar_red")
                    category("Swamp Tar")
                }
            }
            row("dbrow.herblore_harralander_tar") {
                production {
                    input("obj.harralander")
                    statReq("stat.herblore", 44)
                    xp(72)
                    output("obj.salamander_tar_black")
                    category("Swamp Tar")
                }
            }
            row("dbrow.herblore_irit_tar") {
                production {
                    input("obj.irit_leaf")
                    statReq("stat.herblore", 50)
                    xp(84)
                    output("obj.salamander_tar_mountain")
                    category("Swamp Tar")
                }
            }

        }

    /**
     * Table for crushing items with pestle and mortar
     */
    fun crushing() =
        productionTable("dbtable.herblore_crushing", serverOnly = true) {

            row("dbrow.herblore_crush_bird_nest") {
                production {
                    input("obj.bird_nest_empty")
                    statReq("stat.herblore", 1)
                    xp(0)
                    output("obj.crushed_bird_nest")
                    category("Crushing")
                }
            }
            row("dbrow.herblore_crush_chocolate") {
                production {
                    input("obj.chocolate_bar")
                    statReq("stat.herblore", 1)
                    xp(0)
                    output("obj.chocolate_dust")
                    category("Crushing")
                }
            }
            row("dbrow.herblore_crush_unicorn_horn") {
                production {
                    input("obj.unicorn_horn")
                    statReq("stat.herblore", 1)
                    xp(0)
                    output("obj.unicorn_horn_dust")
                    category("Crushing")
                }
            }
            row("dbrow.herblore_crush_dragon_scale") {
                production {
                    input("obj.blue_dragon_scale")
                    statReq("stat.herblore", 1)
                    xp(0)
                    output("obj.dragon_scale_dust")
                    category("Crushing")
                }
            }
            row("dbrow.herblore_crush_goat_horn") {
                production {
                    input("obj.desert_goat_horn")
                    statReq("stat.herblore", 1)
                    xp(0)
                    output("obj.ground_desert_goat_horn")
                    category("Crushing")
                }
            }
            row("dbrow.herblore_crush_superior_dragon_bones") {
                production {
                    input("obj.dragon_bones_superior")
                    statReq("stat.herblore", 1)
                    xp(0)
                    output("obj.crushed_dragon_bones")
                    category("Crushing")
                }
            }
        }
}
