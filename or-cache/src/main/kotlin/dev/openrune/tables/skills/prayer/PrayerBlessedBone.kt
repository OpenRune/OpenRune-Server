package dev.openrune.tables.skills.prayer

import dev.openrune.definition.util.VarType
import dev.openrune.tables.production.productionTable

object PrayerBlessedBone {
    const val COL_SHARD_COUNT = 7

    fun table() = productionTable(
        "dbtable.prayer_blessed_bone",
        serverOnly = true,
        defaultCategory = "Bless",
        extraColumns = {
            column("shard_count", COL_SHARD_COUNT, VarType.INT)
        },
    ) {
        row("dbrow.prayer_bb_blessed_bones") {
            production {
                input("obj.bones")
                statReq("stat.prayer", 1)
                xp(0)
                output("obj.blessed_bones")
            }
            column(COL_SHARD_COUNT, 4)
        }
        row("dbrow.prayer_bb_blessed_bat_bones") {
            production {
                input("obj.bat_bones")
                statReq("stat.prayer", 1)
                xp(0)
                output("obj.blessed_bat_bones")
            }
            column(COL_SHARD_COUNT, 5)
        }
        row("dbrow.prayer_bb_blessed_big_bones") {
            production {
                input("obj.big_bones")
                statReq("stat.prayer", 1)
                xp(0)
                output("obj.blessed_big_bones")
            }
            column(COL_SHARD_COUNT, 12)
        }
        row("dbrow.prayer_bb_blessed_zogre_bones") {
            production {
                input("obj.zogre_bones")
                statReq("stat.prayer", 1)
                xp(0)
                output("obj.blessed_zogre_bones")
            }
            column(COL_SHARD_COUNT, 18)
        }
        row("dbrow.prayer_bb_blessed_babywyrm_bones") {
            production {
                input("obj.babywyrm_bones")
                statReq("stat.prayer", 1)
                xp(0)
                output("obj.blessed_babywyrm_bones")
            }
            column(COL_SHARD_COUNT, 21)
        }
        row("dbrow.prayer_bb_blessed_babydragon_bones") {
            production {
                input("obj.babydragon_bones")
                statReq("stat.prayer", 1)
                xp(0)
                output("obj.blessed_babydragon_bones")
            }
            column(COL_SHARD_COUNT, 24)
        }
        row("dbrow.prayer_bb_blessed_strykewyrm_bones") {
            production {
                input("obj.strykewyrm_bones")
                statReq("stat.prayer", 1)
                xp(0)
                output("obj.blessed_strykewyrm_bones")
            }
            column(COL_SHARD_COUNT, 37)
        }
        row("dbrow.prayer_bb_blessed_wyrm_bones") {
            production {
                input("obj.wyrm_bones")
                statReq("stat.prayer", 1)
                xp(0)
                output("obj.blessed_wyrm_bones")
            }
            column(COL_SHARD_COUNT, 42)
        }
        row("dbrow.prayer_bb_sun_kissed_bones") {
            production {
                input("obj.sun_kissed_bone")
                statReq("stat.prayer", 1)
                xp(0)
                output("obj.sun_kissed_bone")
            }
            column(COL_SHARD_COUNT, 45)
        }
        row("dbrow.prayer_bb_blessed_wyvern_bones") {
            production {
                input("obj.wyvern_bones")
                statReq("stat.prayer", 1)
                xp(0)
                output("obj.blessed_wyvern_bones")
            }
            column(COL_SHARD_COUNT, 58)
        }
        row("dbrow.prayer_bb_blessed_dragon_bones") {
            production {
                input("obj.dragon_bones")
                statReq("stat.prayer", 1)
                xp(0)
                output("obj.blessed_dragon_bones")
            }
            column(COL_SHARD_COUNT, 58)
        }
        row("dbrow.prayer_bb_blessed_drake_bones") {
            production {
                input("obj.drake_bones")
                statReq("stat.prayer", 1)
                xp(0)
                output("obj.blessed_drake_bones")
            }
            column(COL_SHARD_COUNT, 67)
        }
        row("dbrow.prayer_bb_blessed_fayrg_bones") {
            production {
                input("obj.zogre_ancestral_bones_fayg")
                statReq("stat.prayer", 1)
                xp(0)
                output("obj.blessed_fayrg_bones")
            }
            column(COL_SHARD_COUNT, 67)
        }
        row("dbrow.prayer_bb_blessed_lava_dragon_bones") {
            production {
                input("obj.lava_dragon_bones")
                statReq("stat.prayer", 1)
                xp(0)
                output("obj.blessed_lava_dragon_bones")
            }
            column(COL_SHARD_COUNT, 68)
        }
        row("dbrow.prayer_bb_blessed_raurg_bones") {
            production {
                input("obj.zogre_ancestral_bones_raurg")
                statReq("stat.prayer", 1)
                xp(0)
                output("obj.blessed_raurg_bones")
            }
            column(COL_SHARD_COUNT, 77)
        }
        row("dbrow.prayer_bb_blessed_frost_dragon_bones") {
            production {
                input("obj.frost_dragon_bones")
                statReq("stat.prayer", 1)
                xp(0)
                output("obj.blessed_frost_dragon_bones")
            }
            column(COL_SHARD_COUNT, 84)
        }
        row("dbrow.prayer_bb_blessed_hydra_bones") {
            production {
                input("obj.hydra_bones")
                statReq("stat.prayer", 1)
                xp(0)
                output("obj.blessed_hydra_bones")
            }
            column(COL_SHARD_COUNT, 93)
        }
        row("dbrow.prayer_bb_blessed_dagannoth_bones") {
            production {
                input("obj.dagannoth_king_bones")
                statReq("stat.prayer", 1)
                xp(0)
                output("obj.blessed_dagannoth_bones")
            }
            column(COL_SHARD_COUNT, 100)
        }
        row("dbrow.prayer_bb_blessed_ourg_bones") {
            production {
                input("obj.zogre_ancestral_bones_ourg")
                statReq("stat.prayer", 1)
                xp(0)
                output("obj.blessed_ourg_bones")
            }
            column(COL_SHARD_COUNT, 112)
        }
        row("dbrow.prayer_bb_blessed_superior_dragon_bones") {
            production {
                input("obj.dragon_bones_superior")
                statReq("stat.prayer", 1)
                xp(0)
                output("obj.blessed_dragon_bones_superior")
            }
            column(COL_SHARD_COUNT, 121)
        }
    }
}
