package dev.openrune.tables.skills.prayer

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType


object PrayerBlessedBone {
    const val COL_BONE_ORIGINAL = 0
    const val COL_BONE_BLESSED = 1
    const val COL_SHARD_COUNT = 2


    fun table() = dbTable("dbtable.prayer_blessed_bone", serverOnly = true) {
        column("bone", COL_BONE_ORIGINAL, VarType.OBJ)
        column("bone_blessed", COL_BONE_BLESSED, VarType.OBJ)
        column("shard_count", COL_SHARD_COUNT, VarType.INT)
        row("dbrow.prayer_bb_blessed_bones") {
            columnRSCM(COL_BONE_ORIGINAL, "obj.bones")
            columnRSCM(COL_BONE_BLESSED, "obj.blessed_bones")
            column(COL_SHARD_COUNT, 4)
        }

        row("dbrow.prayer_bb_blessed_bat_bones") {
            columnRSCM(COL_BONE_ORIGINAL, "obj.bat_bones")
            columnRSCM(COL_BONE_BLESSED, "obj.blessed_bat_bones")
            column(COL_SHARD_COUNT, 5)
        }

        row("dbrow.prayer_bb_blessed_big_bones") {
            columnRSCM(COL_BONE_ORIGINAL, "obj.big_bones")
            columnRSCM(COL_BONE_BLESSED, "obj.blessed_big_bones")
            column(COL_SHARD_COUNT, 12)
        }

        row("dbrow.prayer_bb_blessed_zogre_bones") {
            columnRSCM(COL_BONE_ORIGINAL, "obj.zogre_bones")
            columnRSCM(COL_BONE_BLESSED, "obj.blessed_zogre_bones")
            column(COL_SHARD_COUNT, 18)
        }

        row("dbrow.prayer_bb_blessed_babywyrm_bones") {
            columnRSCM(COL_BONE_ORIGINAL, "obj.babywyrm_bones")
            columnRSCM(COL_BONE_BLESSED, "obj.blessed_babywyrm_bones")
            column(COL_SHARD_COUNT, 21)
        }

        row("dbrow.prayer_bb_blessed_babydragon_bones") {
            columnRSCM(COL_BONE_ORIGINAL, "obj.babydragon_bones")
            columnRSCM(COL_BONE_BLESSED, "obj.blessed_babydragon_bones")
            column(COL_SHARD_COUNT, 24)
        }

        row("dbrow.prayer_bb_blessed_strykewyrm_bones") {
            columnRSCM(COL_BONE_ORIGINAL, "obj.strykewyrm_bones")
            columnRSCM(COL_BONE_BLESSED, "obj.blessed_strykewyrm_bones")
            column(COL_SHARD_COUNT, 37)
        }

        row("dbrow.prayer_bb_blessed_wyrm_bones") {
            columnRSCM(COL_BONE_ORIGINAL, "obj.wyrm_bones")
            columnRSCM(COL_BONE_BLESSED, "obj.blessed_wyrm_bones")
            column(COL_SHARD_COUNT, 42)
        }

        row("dbrow.prayer_bb_sun_kissed_bones") {
            columnRSCM(COL_BONE_ORIGINAL, "obj.sun_kissed_bone")
            columnRSCM(COL_BONE_BLESSED, "obj.sun_kissed_bone")
            column(COL_SHARD_COUNT, 45)
        }

        row("dbrow.prayer_bb_blessed_wyvern_bones") {
            columnRSCM(COL_BONE_ORIGINAL, "obj.wyvern_bones")
            columnRSCM(COL_BONE_BLESSED, "obj.blessed_wyvern_bones")
            column(COL_SHARD_COUNT, 58)
        }

        row("dbrow.prayer_bb_blessed_dragon_bones") {
            columnRSCM(COL_BONE_ORIGINAL, "obj.dragon_bones")
            columnRSCM(COL_BONE_BLESSED, "obj.blessed_dragon_bones")
            column(COL_SHARD_COUNT, 58)
        }

        row("dbrow.prayer_bb_blessed_drake_bones") {
            columnRSCM(COL_BONE_ORIGINAL, "obj.drake_bones")
            columnRSCM(COL_BONE_BLESSED, "obj.blessed_drake_bones")
            column(COL_SHARD_COUNT, 67)
        }

        row("dbrow.prayer_bb_blessed_fayrg_bones") {
            columnRSCM(COL_BONE_ORIGINAL, "obj.zogre_ancestral_bones_fayg")
            columnRSCM(COL_BONE_BLESSED, "obj.blessed_fayrg_bones")
            column(COL_SHARD_COUNT, 67)
        }

        row("dbrow.prayer_bb_blessed_lava_dragon_bones") {
            columnRSCM(COL_BONE_ORIGINAL, "obj.lava_dragon_bones")
            columnRSCM(COL_BONE_BLESSED, "obj.blessed_lava_dragon_bones")
            column(COL_SHARD_COUNT, 68)
        }

        row("dbrow.prayer_bb_blessed_raurg_bones") {
            columnRSCM(COL_BONE_ORIGINAL, "obj.zogre_ancestral_bones_raurg")
            columnRSCM(COL_BONE_BLESSED, "obj.blessed_raurg_bones")
            column(COL_SHARD_COUNT, 77)
        }

        row("dbrow.prayer_bb_blessed_frost_dragon_bones") {
            columnRSCM(COL_BONE_ORIGINAL, "obj.frost_dragon_bones")
            columnRSCM(COL_BONE_BLESSED, "obj.blessed_frost_dragon_bones")
            column(COL_SHARD_COUNT, 84)
        }

        row("dbrow.prayer_bb_blessed_hydra_bones") {
            columnRSCM(COL_BONE_ORIGINAL, "obj.hydra_bones")
            columnRSCM(COL_BONE_BLESSED, "obj.blessed_hydra_bones")
            column(COL_SHARD_COUNT, 93)
        }

        row("dbrow.prayer_bb_blessed_dagannoth_bones") {
            columnRSCM(COL_BONE_ORIGINAL, "obj.dagannoth_king_bones")
            columnRSCM(COL_BONE_BLESSED, "obj.blessed_dagannoth_bones")
            column(COL_SHARD_COUNT, 100)
        }

        row("dbrow.prayer_bb_blessed_ourg_bones") {
            columnRSCM(COL_BONE_ORIGINAL, "obj.zogre_ancestral_bones_ourg")
            columnRSCM(COL_BONE_BLESSED, "obj.blessed_ourg_bones")
            column(COL_SHARD_COUNT, 112)
        }

        row("dbrow.prayer_bb_blessed_superior_dragon_bones") {
            columnRSCM(COL_BONE_ORIGINAL, "obj.dragon_bones_superior")
            columnRSCM(COL_BONE_BLESSED, "obj.blessed_dragon_bones_superior")
            column(COL_SHARD_COUNT, 121)
        }

    }
}
