package org.rsmod.content.skills.hunter.pack

import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object HunterLootSackTable {
    const val SACK = 0
    const val ROLLS = 1
    const val LOOT = 2
    const val HERBS = 3
    const val UNIQUES = 4
    const val EXPERT_UNIQUES = 5

    const val ENTRIES = 0

    private fun drop(obj: String, min: Int, max: Int = min, noted: Boolean = false) =
        listOf(ConstantProvider.getMapping(obj), min, max, noted)

    private val BASIC =
        listOf(
            drop("obj.hg_seedsack", 1),
            drop("obj.coins", 750, 1250),
            drop("obj.hg_speartip", 15, 30),
            drop("obj.blessed_bone_shard", 100, 200),
            drop("obj.hunting_kyatt_meat", 2, noted = true),
            drop("obj.hunting_fennecfox_meat", 3, noted = true),
        )

    private val ADEPT =
        listOf(
            drop("obj.hg_seedsack", 1),
            drop("obj.bird_nest_empty", 1, noted = true),
            drop("obj.bird_nest_empty", 2, 4, noted = true),
            drop("obj.sun_kissed_bone", 2, noted = true),
            drop("obj.blessed_bone_shard", 100, 200),
            drop("obj.hg_speartip", 15, 30),
            drop("obj.hunting_kyatt_meat", 2, noted = true),
            drop("obj.hunting_fennecfox_meat", 3, noted = true),
            drop("obj.hunting_antelopesun_meat", 2, noted = true),
            drop("obj.coins", 750, 1250),
        )

    private val EXPERT =
        listOf(
            drop("obj.hg_seedsack", 1),
            drop("obj.bird_nest_empty", 1, noted = true),
            drop("obj.bird_nest_empty", 2, 4, noted = true),
            drop("obj.sun_kissed_bone", 2, noted = true),
            drop("obj.sun_kissed_bone", 3, noted = true),
            drop("obj.blessed_bone_shard", 100, 200),
            drop("obj.hg_speartip", 15, 30),
            drop("obj.hunting_kyatt_meat", 2, noted = true),
            drop("obj.hunting_fennecfox_meat", 3, noted = true),
            drop("obj.hunting_antelopesun_meat", 2, noted = true),
            drop("obj.hunting_antelopemoon_meat", 2, noted = true),
            drop("obj.coins", 750, 1250),
            drop("obj.coins", 2500, 3500),
        )

    private fun cascade(obj: String, low: Int, high: Int, req: Int = 1) =
        listOf(ConstantProvider.getMapping(obj), low, high, req)

    fun lootSacks() =
        dbTable("dbtable.hunter_loot_sacks", serverOnly = true) {
            column("sack", SACK, VarType.OBJ)
            column("rolls", ROLLS, VarType.INT)
            column("loot", LOOT, VarType.OBJ, VarType.INT, VarType.INT, VarType.BOOLEAN)
            column("herbs", HERBS, VarType.BOOLEAN)
            column("uniques", UNIQUES, VarType.BOOLEAN)
            column("expert_uniques", EXPERT_UNIQUES, VarType.BOOLEAN)

            fun sack(row: String, obj: String, rolls: Int, loot: List<List<Any>>, herbs: Boolean, uniques: Boolean, expert: Boolean) =
                row(row) {
                    columnRSCM(SACK, obj)
                    column(ROLLS, rolls)
                    column(LOOT, loot.flatten())
                    column(HERBS, herbs)
                    column(UNIQUES, uniques)
                    column(EXPERT_UNIQUES, expert)
                }

            sack("dbrow.hunter_loot_sack_basic", "obj.hg_lootsack_t0", 5, BASIC, false, false, false)
            sack("dbrow.hunter_loot_sack_adept", "obj.hg_lootsack_t1", 7, ADEPT, true, true, false)
            sack("dbrow.hunter_loot_sack_expert", "obj.hg_lootsack_t2", 9, EXPERT, true, true, true)
            sack("dbrow.hunter_loot_sack_master", "obj.hg_lootsack_t3", 11, EXPERT, true, true, true)
            sack("dbrow.hunter_loot_sack_supplies", "obj.hg_lootsack_trade", 11, EXPERT, true, false, false)
        }

    fun cascades() =
        dbTable("dbtable.hunter_loot_sack_cascades", serverOnly = true) {
            column("entries", ENTRIES, VarType.OBJ, VarType.INT, VarType.INT, VarType.INT)

            row("dbrow.hunter_loot_sack_herbs") {
                column(
                    ENTRIES,
                    listOf(
                        cascade("obj.unidentified_lantadyme", -30, 60, 34),
                        cascade("obj.unidentified_cadantine", -10, 70, 13),
                        cascade("obj.unidentified_kwuarm", 10, 85),
                        cascade("obj.unidentified_avantoe", 20, 100),
                        cascade("obj.unidentified_irit", 30, 115),
                        cascade("obj.unidentified_ranarr", 10, 170),
                        cascade("obj.unidentified_tarromin", 70, -20),
                        cascade("obj.unidentified_harralander", 256, 256),
                    ).flatten(),
                )
            }
            row("dbrow.hunter_loot_sack_logs") {
                column(
                    ENTRIES,
                    listOf(
                        cascade("obj.magic_logs", -60, 60, 50),
                        cascade("obj.yew_logs", -50, 90, 36),
                        cascade("obj.mahogany_logs", -40, 130, 24),
                        cascade("obj.maple_logs", 0, 160),
                        cascade("obj.teak_logs", 256, 256),
                    ).flatten(),
                )
            }
        }
}
