package org.rsmod.content.skills.farming.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.pack.PluginPack
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.map.CoordGrid

class FarmingTithePack : PluginPack() {
    override fun dbTables(): List<DBTable> = listOf(
        dbTable("dbtable.farming_tithe", serverOnly = true) {
            column("crops", 0, VarType.OBJ, VarType.OBJ, VarType.INT, VarType.INT)
            column("stages", 1, VarType.INT, VarType.INT, VarType.LOC, VarType.LOC, VarType.LOC)
            column("stage_ticks", 2, VarType.INT)
            column("seed_limit", 3, VarType.INT)
            column("sack_size", 4, VarType.INT)
            column("bonus_threshold", 5, VarType.INT)
            column("points_cap", 6, VarType.INT)
            column("refunds", 7, VarType.OBJ, VarType.INT)
            column("grape_saves", 8, VarType.INT, VarType.INT, VarType.INT)
            column("herb_box_herbs", 9, VarType.OBJ, VarType.INT)
            column("blessing_cost", 10, VarType.INT)
            column("blessing_prayer", 11, VarType.INT)
            column("blessing_cooking", 12, VarType.INT)
            column("herb_box_rolls", 13, VarType.INT)
            column("sacks", 14, VarType.COORDGRID)
            val crops = mutableListOf<Int>()
            val stages = mutableListOf<Int>()
            fun crop(tier: Int, suffix: String, level: Int, xp: Int) {
                crops += listOf(
                    "obj.hosidius_tithe_seed_$suffix".asRSCM(),
                    "obj.hosidius_tithe_fruit_$suffix".asRSCM(), level, xp,
                )
                for (stage in 1..3) {
                    stages += listOf(tier, stage) + listOf("dry", "wet", "dead").map {
                        "loc.hosidius_tithe_${suffix}_${stage}_$it".asRSCM(RSCMType.LOC)
                    }
                }
                val grown = "loc.hosidius_tithe_${suffix}_4".asRSCM(RSCMType.LOC)
                stages += listOf(tier, 4, grown, grown, grown)
            }
            crop(1, "a", 34, 6)
            crop(2, "b", 54, 14)
            crop(3, "c", 74, 23)
            val refunds = mutableListOf<Int>()
            fun refund(piece: String, points: Int) {
                for (body in listOf("male", "female")) {
                    refunds += listOf("obj.tithe_reward_${piece}_$body".asRSCM(), points)
                }
            }
            refund("hat", 60)
            refund("torso", 120)
            refund("legs", 100)
            refund("feet", 40)
            val grapeSaves = mutableListOf<Int>()
            fun saves(blessed: Boolean, fertilised: Boolean, endpoints: List<Pair<Int, Int>>) {
                for ((boost, pair) in endpoints.withIndex()) {
                    val flags = (if (blessed) 1 else 0) or (if (fertilised) 2 else 0) or
                        (if (boost >= 2) 4 else 0) or (if (boost % 2 == 1) 8 else 0)
                    grapeSaves += listOf(flags, pair.first, pair.second)
                }
            }
            saves(false, false, listOf(-50 to 170, -52 to 178, -44 to 186, -46 to 195))
            saves(false, true, listOf(-50 to 204, -52 to 213, -44 to 223, -46 to 234))
            saves(true, false, listOf(-70 to 151, -73 to 158, -64 to 166, -67 to 174))
            saves(true, true, listOf(-50 to 181, -52 to 189, -44 to 199, -46 to 208))
            val herbs = mutableListOf<Int>()
            fun herb(name: String, weight: Int) {
                herbs += listOf("obj.unidentified_$name".asRSCM(), weight)
            }
            herb("guam", 32)
            herb("marentill", 24)
            herb("tarromin", 18)
            herb("harralander", 14)
            herb("ranarr", 11)
            herb("irit", 8)
            herb("avantoe", 6)
            herb("kwuarm", 5)
            herb("cadantine", 4)
            herb("lantadyme", 3)
            herb("dwarf_weed", 3)
            row("dbrow.farming_tithe") {
                column(0, *crops.toTypedArray())
                column(1, *stages.toTypedArray())
                column(2, 100)
                column(3, 10000)
                column(4, 100)
                column(5, 75)
                column(6, 16000)
                column(7, *refunds.toTypedArray())
                column(8, *grapeSaves.toTypedArray())
                column(9, *herbs.toTypedArray())
                column(10, 75000)
                column(11, 50)
                column(12, 65)
                column(13, 7)
                column(14, CoordGrid(1809, 3502).packed, CoordGrid(1820, 3500).packed, CoordGrid(1835, 3500).packed)
            }
        },
    )
}
