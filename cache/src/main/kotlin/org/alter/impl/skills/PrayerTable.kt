package org.alter.impl.skills

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object PrayerTable {

    fun skillTable() = dbTable("tables.skill_prayer") {
        column("item", 0, VarType.OBJ)
        column("exp", 1, VarType.INT)
        column("ashes", 2, VarType.BOOLEAN)

        row("dbrows.bones") {
            columnRSCM(0, "items.bones")
            column(1, 5)
            column(2, false)
        }

        row("dbrows.wolfbones") {
            columnRSCM(0, "items.wolf_bones")
            column(1, 5)
            column(2, false)
        }

        row("dbrows.burntbones") {
            columnRSCM(0, "items.bones_burnt")
            column(1, 5)
            column(2, false)
        }

        row("dbrows.monkeybones") {
            columnRSCM(0, "items.mm_normal_monkey_bones")
            column(1, 5)
            column(2, false)
        }

        row("dbrows.batbones") {
            columnRSCM(0, "items.bat_bones")
            column(1, 5)
            column(2, false)
        }

        row("dbrows.bigbones") {
            columnRSCM(0, "items.big_bones")
            column(1, 15)
            column(2, false)
        }

        row("dbrows.jogrebones") {
            columnRSCM(0, "items.tbwt_jogre_bones")
            column(1, 15)
            column(2, false)
        }

        row("dbrows.wyrmlingbones") {
            columnRSCM(0, "items.babywyrm_bones")
            column(1, 21)
            column(2, false)
        }

        row("dbrows.zogrebones") {
            columnRSCM(0, "items.zogre_bones")
            column(1, 23)
            column(2, false)
        }

        row("dbrows.shaikahanbones") {
            columnRSCM(0, "items.tbwt_beast_bones")
            column(1, 25)
            column(2, false)
        }

        row("dbrows.babydragonbones") {
            columnRSCM(0, "items.babydragon_bones")
            column(1, 30)
            column(2, false)
        }

        row("dbrows.wyrmbones") {
            columnRSCM(0, "items.wyrm_bones")
            column(1, 50)
            column(2, false)
        }

        row("dbrows.wyvernbones") {
            columnRSCM(0, "items.wyvern_bones")
            column(1, 72)
            column(2, false)
        }

        row("dbrows.dragonbones") {
            columnRSCM(0, "items.dragon_bones")
            column(1, 72)
            column(2, false)
        }

        row("dbrows.drakebones") {
            columnRSCM(0, "items.drake_bones")
            column(1, 80)
            column(2, false)
        }

        row("dbrows.fayrgbones") {
            columnRSCM(0, "items.zogre_ancestral_bones_fayg")
            column(1, 84)
            column(2, false)
        }

        row("dbrows.lavadragonbones") {
            columnRSCM(0, "items.lava_dragon_bones")
            column(1, 85)
            column(2, false)
        }

        row("dbrows.raurgbones") {
            columnRSCM(0, "items.zogre_ancestral_bones_raurg")
            column(1, 96)
            column(2, false)
        }

        row("dbrows.hydrabones") {
            columnRSCM(0, "items.hydra_bones")
            column(1, 110)
            column(2, false)
        }

        row("dbrows.dagannothbones") {
            columnRSCM(0, "items.dagannoth_king_bones")
            column(1, 125)
            column(2, false)
        }

        row("dbrows.ourgbones") {
            columnRSCM(0, "items.zogre_ancestral_bones_ourg")
            column(1, 140)
            column(2, false)
        }

        row("dbrows.superiordragonbones") {
            columnRSCM(0, "items.dragon_bones_superior")
            column(1, 150)
            column(2, false)
        }

        row("dbrows.alansbones") {
            columnRSCM(0, "items.alan_bones")
            column(1, 3)
            column(2, false)
        }

        row("dbrows.bonesapeatoll") {
            columnRSCM(0, "items.mm_skeleton_bones")
            column(1, 3)
            column(2, false)
        }

        row("dbrows.bleachedbones") {
            columnRSCM(0, "items.shade_bleached_bones")
            column(1, 5)
            column(2, false)
        }

        row("dbrows.smallzombiemonkeybones") {
            columnRSCM(0, "items.mm_small_zombie_monkey_bones")
            column(1, 5)
            column(2, false)
        }

        row("dbrows.largezombiemonkeybones") {
            columnRSCM(0, "items.mm_large_zombie_monkey_bones")
            column(1, 5)
            column(2, false)
        }

        row("dbrows.smallninjamonkeybones") {
            columnRSCM(0, "items.mm_small_ninja_monkey_bones")
            column(1, 16)
            column(2, false)
        }

        row("dbrows.mediumninjamonkeybones") {
            columnRSCM(0, "items.mm_medium_ninja_monkey_bones")
            column(1, 18)
            column(2, false)
        }

        row("dbrows.gorillabones") {
            columnRSCM(0, "items.mm_normal_gorilla_monkey_bones")
            column(1, 18)
            column(2, false)
        }

        row("dbrows.beardedgorillabones") {
            columnRSCM(0, "items.mm_bearded_gorilla_monkey_bones")
            column(1, 18)
            column(2, true)
        }

        row("dbrows.fiendishashes") {
            columnRSCM(0, "items.fiendish_ashes")
            column(1, 10)
            column(2, true)
        }

        row("dbrows.vileashes") {
            columnRSCM(0, "items.vile_ashes")
            column(1, 25)
            column(2, true)
        }

        row("dbrows.maliciousashes") {
            columnRSCM(0, "items.malicious_ashes")
            column(1, 65)
            column(2, true)
        }

        row("dbrows.abyssalashes") {
            columnRSCM(0, "items.abyssal_ashes")
            column(1, 85)
            column(2, true)
        }

        row("dbrows.infernalashes") {
            columnRSCM(0, "items.infernal_ashes")
            column(1, 110)
            column(2, true)
        }
    }
}