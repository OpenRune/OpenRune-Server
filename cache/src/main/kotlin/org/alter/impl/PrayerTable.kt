package org.alter.impl

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object PrayerTable {

    fun skillTable() = dbTable("tables.skill_prayer") {
        column("item", 0, arrayOf(VarType.OBJ))
        column("exp", 1, arrayOf(VarType.INT))
        column("ashes", 2, arrayOf(VarType.BOOLEAN))

        row("dbrows.bones") {
            columnRSCM(0, arrayOf("items.bones"))
            column(1, arrayOf(5))
            column(2, arrayOf(false))
        }

        row("dbrows.wolfbones") {
            columnRSCM(0, arrayOf("items.wolf_bones"))
            column(1, arrayOf(5))
            column(2, arrayOf(false))
        }

        row("dbrows.burntbones") {
            columnRSCM(0, arrayOf("items.bones_burnt"))
            column(1, arrayOf(5))
            column(2, arrayOf(false))
        }

        row("dbrows.monkeybones") {
            columnRSCM(0, arrayOf("items.mm_normal_monkey_bones"))
            column(1, arrayOf(5))
            column(2, arrayOf(false))
        }

        row("dbrows.batbones") {
            columnRSCM(0, arrayOf("items.bat_bones"))
            column(1, arrayOf(5))
            column(2, arrayOf(false))
        }

        row("dbrows.bigbones") {
            columnRSCM(0, arrayOf("items.big_bones"))
            column(1, arrayOf(15))
            column(2, arrayOf(false))
        }

        row("dbrows.jogrebones") {
            columnRSCM(0, arrayOf("items.tbwt_jogre_bones"))
            column(1, arrayOf(15))
            column(2, arrayOf(false))
        }

        row("dbrows.wyrmlingbones") {
            columnRSCM(0, arrayOf("items.babywyrm_bones"))
            column(1, arrayOf(21))
            column(2, arrayOf(false))
        }

        row("dbrows.zogrebones") {
            columnRSCM(0, arrayOf("items.zogre_bones"))
            column(1, arrayOf(23))
            column(2, arrayOf(false))
        }

        row("dbrows.shaikahanbones") {
            columnRSCM(0, arrayOf("items.tbwt_beast_bones"))
            column(1, arrayOf(25))
            column(2, arrayOf(false))
        }

        row("dbrows.babydragonbones") {
            columnRSCM(0, arrayOf("items.babydragon_bones"))
            column(1, arrayOf(30))
            column(2, arrayOf(false))
        }

        row("dbrows.wyrmbones") {
            columnRSCM(0, arrayOf("items.wyrm_bones"))
            column(1, arrayOf(50))
            column(2, arrayOf(false))
        }

        row("dbrows.wyvernbones") {
            columnRSCM(0, arrayOf("items.wyvern_bones"))
            column(1, arrayOf(72))
            column(2, arrayOf(false))
        }

        row("dbrows.dragonbones") {
            columnRSCM(0, arrayOf("items.dragon_bones"))
            column(1, arrayOf(72))
            column(2, arrayOf(false))
        }

        row("dbrows.drakebones") {
            columnRSCM(0, arrayOf("items.drake_bones"))
            column(1, arrayOf(80))
            column(2, arrayOf(false))
        }

        row("dbrows.fayrgbones") {
            columnRSCM(0, arrayOf("items.zogre_ancestral_bones_fayg"))
            column(1, arrayOf(84))
            column(2, arrayOf(false))
        }

        row("dbrows.lavadragonbones") {
            columnRSCM(0, arrayOf("items.lava_dragon_bones"))
            column(1, arrayOf(85))
            column(2, arrayOf(false))
        }

        row("dbrows.raurgbones") {
            columnRSCM(0, arrayOf("items.zogre_ancestral_bones_raurg"))
            column(1, arrayOf(96))
            column(2, arrayOf(false))
        }

        row("dbrows.hydrabones") {
            columnRSCM(0, arrayOf("items.hydra_bones"))
            column(1, arrayOf(110))
            column(2, arrayOf(false))
        }

        row("dbrows.dagannothbones") {
            columnRSCM(0, arrayOf("items.dagannoth_king_bones"))
            column(1, arrayOf(125))
            column(2, arrayOf(false))
        }

        row("dbrows.ourgbones") {
            columnRSCM(0, arrayOf("items.zogre_ancestral_bones_ourg"))
            column(1, arrayOf(140))
            column(2, arrayOf(false))
        }

        row("dbrows.superiordragonbones") {
            columnRSCM(0, arrayOf("items.dragon_bones_superior"))
            column(1, arrayOf(150))
            column(2, arrayOf(false))
        }

        row("dbrows.alansbones") {
            columnRSCM(0, arrayOf("items.alan_bones"))
            column(1, arrayOf(3))
            column(2, arrayOf(false))
        }

        row("dbrows.bonesapeatoll") {
            columnRSCM(0, arrayOf("items.mm_skeleton_bones"))
            column(1, arrayOf(3))
            column(2, arrayOf(false))
        }

        row("dbrows.bleachedbones") {
            columnRSCM(0, arrayOf("items.shade_bleached_bones"))
            column(1, arrayOf(5))
            column(2, arrayOf(false))
        }

        row("dbrows.smallzombiemonkeybones") {
            columnRSCM(0, arrayOf("items.mm_small_zombie_monkey_bones"))
            column(1, arrayOf(5))
            column(2, arrayOf(false))
        }

        row("dbrows.largezombiemonkeybones") {
            columnRSCM(0, arrayOf("items.mm_large_zombie_monkey_bones"))
            column(1, arrayOf(5))
            column(2, arrayOf(false))
        }

        row("dbrows.smallninjamonkeybones") {
            columnRSCM(0, arrayOf("items.mm_small_ninja_monkey_bones"))
            column(1, arrayOf(16))
            column(2, arrayOf(false))
        }

        row("dbrows.mediumninjamonkeybones") {
            columnRSCM(0, arrayOf("items.mm_medium_ninja_monkey_bones"))
            column(1, arrayOf(18))
            column(2, arrayOf(false))
        }

        row("dbrows.gorillabones") {
            columnRSCM(0, arrayOf("items.mm_normal_gorilla_monkey_bones"))
            column(1, arrayOf(18))
            column(2, arrayOf(false))
        }

        row("dbrows.beardedgorillabones") {
            columnRSCM(0, arrayOf("items.mm_bearded_gorilla_monkey_bones"))
            column(1, arrayOf(18))
            column(2, arrayOf(true))
        }

        row("dbrows.fiendishashes") {
            columnRSCM(0, arrayOf("items.fiendish_ashes"))
            column(1, arrayOf(10))
            column(2, arrayOf(true))
        }

        row("dbrows.vileashes") {
            columnRSCM(0, arrayOf("items.vile_ashes"))
            column(1, arrayOf(25))
            column(2, arrayOf(true))
        }

        row("dbrows.maliciousashes") {
            columnRSCM(0, arrayOf("items.malicious_ashes"))
            column(1, arrayOf(65))
            column(2, arrayOf(true))
        }

        row("dbrows.abyssalashes") {
            columnRSCM(0, arrayOf("items.abyssal_ashes"))
            column(1, arrayOf(85))
            column(2, arrayOf(true))
        }

        row("dbrows.infernalashes") {
            columnRSCM(0, arrayOf("items.infernal_ashes"))
            column(1, arrayOf(110))
            column(2, arrayOf(true))
        }
    }
}
