package dev.openrune.tables.skills.prayer

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object PrayerTable {

    fun skillTable() = dbTable("dbtable.skill_prayer", serverOnly = true) {

        column("item", 0, VarType.OBJ)
        column("exp", 1, VarType.INT)
        column("ashes", 2, VarType.BOOLEAN)
    
        column("prayer_restore", 3, VarType.INT)

        row("dbrow.bones") {
            columnRSCM(0, "obj.bones")
            column(1, 5)
            column(2, false)
            column(3, 1)
        }

        row("dbrow.wolfbones") {
            columnRSCM(0, "obj.wolf_bones")
            column(1, 5)
            column(2, false)
            column(3, 1)
        }

        row("dbrow.burntbones") {
            columnRSCM(0, "obj.bones_burnt")
            column(1, 5)
            column(2, false)
            column(3, 1)
        }

        row("dbrow.monkeybones") {
            columnRSCM(0, "obj.mm_normal_monkey_bones")
            column(1, 5)
            column(2, false)
            column(3, 1)
        }

        row("dbrow.batbones") {
            columnRSCM(0, "obj.bat_bones")
            column(1, 5)
            column(2, false)
            column(3, 1)
        }

        row("dbrow.bigbones") {
            columnRSCM(0, "obj.big_bones")
            column(1, 15)
            column(2, false)
            column(3, 2)
        }

        row("dbrow.jogrebones") {
            columnRSCM(0, "obj.tbwt_jogre_bones")
            column(1, 15)
            column(2, false)
            column(3, 2)
        }

        row("dbrow.wyrmlingbones") {
            columnRSCM(0, "obj.babywyrm_bones")
            column(1, 21)
            column(2, false)
            column(3, 2)
        }

        row("dbrow.zogrebones") {
            columnRSCM(0, "obj.zogre_bones")
            column(1, 23)
            column(2, false)
            column(3, 2)
        }

        row("dbrow.shaikahanbones") {
            columnRSCM(0, "obj.tbwt_beast_bones")
            column(1, 25)
            column(2, false)
            column(3, 2)
        }

        row("dbrow.babydragonbones") {
            columnRSCM(0, "obj.babydragon_bones")
            column(1, 30)
            column(2, false)
            column(3, 3)
        }

        row("dbrow.wyrmbones") {
            columnRSCM(0, "obj.wyrm_bones")
            column(1, 50)
            column(2, false)
            column(3, 3)
        }

        row("dbrow.wyvernbones") {
            columnRSCM(0, "obj.wyvern_bones")
            column(1, 72)
            column(2, false)
            column(3, 4)
        }

        row("dbrow.dragonbones") {
            columnRSCM(0, "obj.dragon_bones")
            column(1, 72)
            column(2, false)
            column(3, 4)
        }

        row("dbrow.drakebones") {
            columnRSCM(0, "obj.drake_bones")
            column(1, 80)
            column(2, false)
            column(3, 4)
        }

        row("dbrow.fayrgbones") {
            columnRSCM(0, "obj.zogre_ancestral_bones_fayg")
            column(1, 84)
            column(2, false)
            column(3, 4)
        }

        row("dbrow.lavadragonbones") {
            columnRSCM(0, "obj.lava_dragon_bones")
            column(1, 85)
            column(2, false)
            column(3, 4)
        }

        row("dbrow.raurgbones") {
            columnRSCM(0, "obj.zogre_ancestral_bones_raurg")
            column(1, 96)
            column(2, false)
            column(3, 4)
        }

        row("dbrow.hydrabones") {
            columnRSCM(0, "obj.hydra_bones")
            column(1, 110)
            column(2, false)
            column(3, 4)
        }

        row("dbrow.dagannothbones") {
            columnRSCM(0, "obj.dagannoth_king_bones")
            column(1, 125)
            column(2, false)
            column(3, 4)
        }

        row("dbrow.ourgbones") {
            columnRSCM(0, "obj.zogre_ancestral_bones_ourg")
            column(1, 140)
            column(2, false)
            column(3, 5)
        }

        row("dbrow.superiordragonbones") {
            columnRSCM(0, "obj.dragon_bones_superior")
            column(1, 150)
            column(2, false)
            column(3, 5)
        }

        row("dbrow.alansbones") {
            columnRSCM(0, "obj.alan_bones")
            column(1, 3)
            column(2, false)
            column(3, 1)
        }

        row("dbrow.bonesapeatoll") {
            columnRSCM(0, "obj.mm_skeleton_bones")
            column(1, 3)
            column(2, false)
            column(3, 1)
        }

        row("dbrow.bleachedbones") {
            columnRSCM(0, "obj.shade_bleached_bones")
            column(1, 5)
            column(2, false)
            column(3, 1)
        }

        row("dbrow.smallzombiemonkeybones") {
            columnRSCM(0, "obj.mm_small_zombie_monkey_bones")
            column(1, 5)
            column(2, false)
            column(3, 1)
        }

        row("dbrow.largezombiemonkeybones") {
            columnRSCM(0, "obj.mm_large_zombie_monkey_bones")
            column(1, 5)
            column(2, false)
            column(3, 1)
        }

        row("dbrow.smallninjamonkeybones") {
            columnRSCM(0, "obj.mm_small_ninja_monkey_bones")
            column(1, 16)
            column(2, false)
            column(3, 2)
        }

        row("dbrow.mediumninjamonkeybones") {
            columnRSCM(0, "obj.mm_medium_ninja_monkey_bones")
            column(1, 18)
            column(2, false)
            column(3, 2)
        }

        row("dbrow.gorillabones") {
            columnRSCM(0, "obj.mm_normal_gorilla_monkey_bones")
            column(1, 18)
            column(2, false)
            column(3, 2)
        }

        row("dbrow.beardedgorillabones") {
            columnRSCM(0, "obj.mm_bearded_gorilla_monkey_bones")
            column(1, 18)
            column(2, true)
            column(3, 0)
        }

        row("dbrow.fiendishashes") {
            columnRSCM(0, "obj.fiendish_ashes")
            column(1, 10)
            column(2, true)
            column(3, 0)
        }

        row("dbrow.vileashes") {
            columnRSCM(0, "obj.vile_ashes")
            column(1, 25)
            column(2, true)
            column(3, 0)
        }

        row("dbrow.maliciousashes") {
            columnRSCM(0, "obj.malicious_ashes")
            column(1, 65)
            column(2, true)
            column(3, 0)
        }

        row("dbrow.abyssalashes") {
            columnRSCM(0, "obj.abyssal_ashes")
            column(1, 85)
            column(2, true)
            column(3, 0)
        }

        row("dbrow.infernalashes") {
            columnRSCM(0, "obj.infernal_ashes")
            column(1, 110)
            column(2, true)
            column(3, 0)
        }
    }
}
