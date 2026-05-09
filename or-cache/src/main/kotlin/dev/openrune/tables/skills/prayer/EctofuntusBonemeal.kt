package dev.openrune.tables.skills.prayer

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object EctofuntusBonemeal {

    const val COL_BONE = 0
    const val COL_BONEMEAL = 1
    const val COL_WORSHIP_XP = 2
    const val COL_REQUIRED_LEVEL = 3


    fun table() = dbTable("dbtable.prayer_ectofuntus_bonemeal", serverOnly = true) {
        column("bone", COL_BONE, VarType.OBJ)
        column("bonemeal", COL_BONEMEAL, VarType.OBJ)
        column("worship_xp", COL_WORSHIP_XP, VarType.INT)
        column("required_level", COL_REQUIRED_LEVEL, VarType.INT)

        row("dbrow.prayer_ecto_alan_bones") {
            columnRSCM(COL_BONE, "obj.alan_bones")
            columnRSCM(COL_BONEMEAL, "obj.pot_bonemeal_alan")
            column(COL_WORSHIP_XP, 12)
            column(COL_REQUIRED_LEVEL, 1)
        }

        row("dbrow.prayer_ecto_bones") {
            columnRSCM(COL_BONE, "obj.bones")
            columnRSCM(COL_BONEMEAL, "obj.pot_bonemeal")
            column(COL_WORSHIP_XP, 18)
            column(COL_REQUIRED_LEVEL, 1)
        }

        row("dbrow.prayer_ecto_bat_bones") {
            columnRSCM(COL_BONE, "obj.bat_bones")
            columnRSCM(COL_BONEMEAL, "obj.pot_bonemeal_bat")
            column(COL_WORSHIP_XP, 21)
            column(COL_REQUIRED_LEVEL, 1)
        }

        row("dbrow.prayer_ecto_big_bones") {
            columnRSCM(COL_BONE, "obj.big_bones")
            columnRSCM(COL_BONEMEAL, "obj.pot_bonemeal_big")
            column(COL_WORSHIP_XP, 60)
            column(COL_REQUIRED_LEVEL, 1)
        }

        row("dbrow.prayer_ecto_long_bone") {
            columnRSCM(COL_BONE, "obj.dorgesh_construction_bone")
            columnRSCM(COL_BONEMEAL, "obj.pot_bonemeal_big")
            column(COL_WORSHIP_XP, 60)
            column(COL_REQUIRED_LEVEL, 1)
        }

        row("dbrow.prayer_ecto_curved_bone") {
            columnRSCM(COL_BONE, "obj.dorgesh_construction_bone_curved")
            columnRSCM(COL_BONEMEAL, "obj.pot_bonemeal_big")
            column(COL_WORSHIP_XP, 60)
            column(COL_REQUIRED_LEVEL, 1)
        }

        row("dbrow.prayer_ecto_burnt_bones") {
            columnRSCM(COL_BONE, "obj.bones_burnt")
            columnRSCM(COL_BONEMEAL, "obj.pot_bonemeal_burnt")
            column(COL_WORSHIP_XP, 18)
            column(COL_REQUIRED_LEVEL, 1)
        }

        row("dbrow.prayer_ecto_burnt_jogre_bones") {
            columnRSCM(COL_BONE, "obj.tbwt_burnt_jogre_bones")
            columnRSCM(COL_BONEMEAL, "obj.pot_bonemeal_burnt_jogre")
            column(COL_WORSHIP_XP, 64)
            column(COL_REQUIRED_LEVEL, 1)
        }

        row("dbrow.prayer_ecto_jogre_bones") {
            columnRSCM(COL_BONE, "obj.tbwt_jogre_bones")
            columnRSCM(COL_BONEMEAL, "obj.pot_bonemeal_jogre")
            column(COL_WORSHIP_XP, 60)
            column(COL_REQUIRED_LEVEL, 1)
        }

        row("dbrow.prayer_ecto_zogre_bones") {
            columnRSCM(COL_BONE, "obj.zogre_bones")
            columnRSCM(COL_BONEMEAL, "obj.pot_bonemeal_zogre")
            column(COL_WORSHIP_XP, 90)
            column(COL_REQUIRED_LEVEL, 1)
        }

        row("dbrow.prayer_ecto_babydragon_bones") {
            columnRSCM(COL_BONE, "obj.babydragon_bones")
            columnRSCM(COL_BONEMEAL, "obj.pot_bonemeal_babydragon")
            column(COL_WORSHIP_XP, 120)
            column(COL_REQUIRED_LEVEL, 1)
        }

        row("dbrow.prayer_ecto_wyrmling_bones") {
            columnRSCM(COL_BONE, "obj.babywyrm_bones")
            columnRSCM(COL_BONEMEAL, "obj.pot_bonemeal_babydragon")
            column(COL_WORSHIP_XP, 120)
            column(COL_REQUIRED_LEVEL, 1)
        }

        row("dbrow.prayer_ecto_dragon_bones") {
            columnRSCM(COL_BONE, "obj.dragon_bones")
            columnRSCM(COL_BONEMEAL, "obj.pot_bonemeal_dragon")
            column(COL_WORSHIP_XP, 288)
            column(COL_REQUIRED_LEVEL, 1)
        }

        row("dbrow.prayer_ecto_wolf_bones") {
            columnRSCM(COL_BONE, "obj.wolf_bones")
            columnRSCM(COL_BONEMEAL, "obj.pot_bonemeal_wolf")
            column(COL_WORSHIP_XP, 18)
            column(COL_REQUIRED_LEVEL, 1)
        }

        row("dbrow.prayer_ecto_monkey_bones") {
            columnRSCM(COL_BONE, "obj.mm_normal_monkey_bones")
            columnRSCM(COL_BONEMEAL, "obj.pot_bonemeal_normal_monkey")
            column(COL_WORSHIP_XP, 20)
            column(COL_REQUIRED_LEVEL, 1)
        }

        row("dbrow.prayer_ecto_small_ninja_monkey_bones") {
            columnRSCM(COL_BONE, "obj.mm_small_ninja_monkey_bones")
            columnRSCM(COL_BONEMEAL, "obj.pot_bonemeal_small_ninja_monkey")
            column(COL_WORSHIP_XP, 20)
            column(COL_REQUIRED_LEVEL, 1)
        }

        row("dbrow.prayer_ecto_medium_ninja_monkey_bones") {
            columnRSCM(COL_BONE, "obj.mm_medium_ninja_monkey_bones")
            columnRSCM(COL_BONEMEAL, "obj.pot_bonemeal_medium_ninja_monkey")
            column(COL_WORSHIP_XP, 18)
            column(COL_REQUIRED_LEVEL, 1)
        }

        row("dbrow.prayer_ecto_gorilla_bones") {
            columnRSCM(COL_BONE, "obj.mm_normal_gorilla_monkey_bones")
            columnRSCM(COL_BONEMEAL, "obj.pot_bonemeal_normal_gorilla_monkey")
            column(COL_WORSHIP_XP, 72)
            column(COL_REQUIRED_LEVEL, 1)
        }

        row("dbrow.prayer_ecto_bearded_gorilla_bones") {
            columnRSCM(COL_BONE, "obj.mm_bearded_gorilla_monkey_bones")
            columnRSCM(COL_BONEMEAL, "obj.pot_bonemeal_bearded_gorilla_monkey")
            column(COL_WORSHIP_XP, 72)
            column(COL_REQUIRED_LEVEL, 1)
        }

        row("dbrow.prayer_ecto_small_zombie_monkey_bones") {
            columnRSCM(COL_BONE, "obj.mm_small_zombie_monkey_bones")
            columnRSCM(COL_BONEMEAL, "obj.pot_bonemeal_small_ninja_monkey")
            column(COL_WORSHIP_XP, 20)
            column(COL_REQUIRED_LEVEL, 1)
        }

        row("dbrow.prayer_ecto_large_zombie_monkey_bones") {
            columnRSCM(COL_BONE, "obj.mm_large_zombie_monkey_bones")
            columnRSCM(COL_BONEMEAL, "obj.pot_bonemeal_large_zombie_monkey")
            column(COL_WORSHIP_XP, 18)
            column(COL_REQUIRED_LEVEL, 1)
        }

        row("dbrow.prayer_ecto_ape_atoll_skeleton_bones") {
            columnRSCM(COL_BONE, "obj.mm_skeleton_bones")
            columnRSCM(COL_BONEMEAL, "obj.pot_bonemeal_skeleton")
            column(COL_WORSHIP_XP, 12)
            column(COL_REQUIRED_LEVEL, 1)
        }

        row("dbrow.prayer_ecto_dagannoth_bones") {
            columnRSCM(COL_BONE, "obj.dagannoth_king_bones")
            columnRSCM(COL_BONEMEAL, "obj.pot_bonemeal_dagannoth")
            column(COL_WORSHIP_XP, 500)
            column(COL_REQUIRED_LEVEL, 1)
        }

        row("dbrow.prayer_ecto_wyvern_bones") {
            columnRSCM(COL_BONE, "obj.wyvern_bones")
            columnRSCM(COL_BONEMEAL, "obj.pot_bonemeal_wyvern")
            column(COL_WORSHIP_XP, 288)
            column(COL_REQUIRED_LEVEL, 1)
        }

        row("dbrow.prayer_ecto_lava_dragon_bones") {
            columnRSCM(COL_BONE, "obj.lava_dragon_bones")
            columnRSCM(COL_BONEMEAL, "obj.pot_bonemeal_lavadragon")
            column(COL_WORSHIP_XP, 340)
            column(COL_REQUIRED_LEVEL, 1)
        }

        row("dbrow.prayer_ecto_fayrg_bones") {
            columnRSCM(COL_BONE, "obj.zogre_ancestral_bones_fayg")
            columnRSCM(COL_BONEMEAL, "obj.pot_bonemeal_ancestral_fayg")
            column(COL_WORSHIP_XP, 336)
            column(COL_REQUIRED_LEVEL, 1)
        }

        row("dbrow.prayer_ecto_raurg_bones") {
            columnRSCM(COL_BONE, "obj.zogre_ancestral_bones_raurg")
            columnRSCM(COL_BONEMEAL, "obj.pot_bonemeal_ancestral_raurg")
            column(COL_WORSHIP_XP, 384)
            column(COL_REQUIRED_LEVEL, 1)
        }

        row("dbrow.prayer_ecto_ourg_bones") {
            columnRSCM(COL_BONE, "obj.zogre_ancestral_bones_ourg")
            columnRSCM(COL_BONEMEAL, "obj.pot_bonemeal_ancestral_ourg")
            column(COL_WORSHIP_XP, 560)
            column(COL_REQUIRED_LEVEL, 1)
        }

        row("dbrow.prayer_ecto_shaikahan_bones") {
            columnRSCM(COL_BONE, "obj.tbwt_beast_bones")
            columnRSCM(COL_BONEMEAL, "obj.pot_bonemeal_beast")
            column(COL_WORSHIP_XP, 100)
            column(COL_REQUIRED_LEVEL, 1)
        }

        row("dbrow.prayer_ecto_superior_dragon_bones") {
            columnRSCM(COL_BONE, "obj.dragon_bones_superior")
            columnRSCM(COL_BONEMEAL, "obj.pot_bonemeal_dragon_superior")
            column(COL_WORSHIP_XP, 600)
            column(COL_REQUIRED_LEVEL, 70)
        }

        row("dbrow.prayer_ecto_frost_dragon_bones") {
            columnRSCM(COL_BONE, "obj.frost_dragon_bones")
            columnRSCM(COL_BONEMEAL, "obj.pot_bonemeal_frost_dragon")
            column(COL_WORSHIP_XP, 400)
            column(COL_REQUIRED_LEVEL, 1)
        }

        row("dbrow.prayer_ecto_wyrm_bones") {
            columnRSCM(COL_BONE, "obj.wyrm_bones")
            columnRSCM(COL_BONEMEAL, "obj.pot_bonemeal_wyrm")
            column(COL_WORSHIP_XP, 200)
            column(COL_REQUIRED_LEVEL, 1)
        }

        row("dbrow.prayer_ecto_strykewyrm_bones") {
            columnRSCM(COL_BONE, "obj.strykewyrm_bones")
            columnRSCM(COL_BONEMEAL, "obj.pot_bonemeal_wyrm")
            column(COL_WORSHIP_XP, 200)
            column(COL_REQUIRED_LEVEL, 1)
        }

        row("dbrow.prayer_ecto_drake_bones") {
            columnRSCM(COL_BONE, "obj.drake_bones")
            columnRSCM(COL_BONEMEAL, "obj.pot_bonemeal_drake")
            column(COL_WORSHIP_XP, 320)
            column(COL_REQUIRED_LEVEL, 1)
        }

        row("dbrow.prayer_ecto_hydra_bones") {
            columnRSCM(COL_BONE, "obj.hydra_bones")
            columnRSCM(COL_BONEMEAL, "obj.pot_bonemeal_hydra")
            column(COL_WORSHIP_XP, 440)
            column(COL_REQUIRED_LEVEL, 1)
        }
    }
}
