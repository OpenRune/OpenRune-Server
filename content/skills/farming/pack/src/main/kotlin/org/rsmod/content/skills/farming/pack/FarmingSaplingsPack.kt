package org.rsmod.content.skills.farming.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.pack.PluginPack
import dev.openrune.rscm.RSCM.asRSCM

class FarmingSaplingsPack : PluginPack() {
    override fun dbTables(): List<DBTable> = listOf(saplings())

    private fun saplings() = dbTable("dbtable.farming_saplings", serverOnly = true) {
        column("chains", 0, VarType.OBJ, VarType.OBJ, VarType.OBJ, VarType.OBJ, VarType.INT)
        val chains = mutableListOf<Int>()
        fun chain(seed: String, dry: String, sapling: String, level: Int) {
            chains += listOf(
                "obj.$seed".asRSCM(), "obj.$dry".asRSCM(), "obj.${dry}_watered".asRSCM(),
                "obj.$sapling".asRSCM(), level,
            )
        }
        chain("acorn", "plantpot_acorn", "plantpot_oak_sapling", 15)
        chain("willow_seed", "plantpot_willow_seed", "plantpot_willow_sapling", 30)
        chain("maple_seed", "plantpot_maple_seed", "plantpot_maple_sapling", 45)
        chain("yew_seed", "plantpot_yew_seed", "plantpot_yew_sapling", 60)
        chain("magic_tree_seed", "plantpot_magic_tree_seed", "plantpot_magic_tree_sapling", 75)
        chain("apple_tree_seed", "plantpot_apple_seed", "plantpot_apple_sapling", 27)
        chain("banana_tree_seed", "plantpot_banana_seed", "plantpot_banana_sapling", 33)
        chain("orange_tree_seed", "plantpot_orange_seed", "plantpot_orange_sapling", 39)
        chain("curry_tree_seed", "plantpot_curry_seed", "plantpot_curry_sapling", 42)
        chain("pineapple_tree_seed", "plantpot_pineapple_seed", "plantpot_pineapple_sapling", 51)
        chain("papaya_tree_seed", "plantpot_papaya_seed", "plantpot_papaya_sapling", 57)
        chain("palm_tree_seed", "plantpot_palm_seed", "plantpot_palm_sapling", 68)
        chain("dragonfruit_tree_seed", "plantpot_dragonfruit_seed", "plantpot_dragonfruit_sapling", 81)
        chain("teak_seed", "plantpot_teak_seed", "plantpot_teak_sapling", 35)
        chain("mahogany_seed", "plantpot_mahogany_seed", "plantpot_mahogany_sapling", 55)
        chain("camphor_seed", "plantpot_camphor_seed", "plantpot_camphor_sapling", 66)
        chain("ironwood_seed", "plantpot_ironwood_seed", "plantpot_ironwood_sapling", 80)
        chain("rosewood_seed", "plantpot_rosewood_seed", "plantpot_rosewood_sapling", 92)
        chain("calquat_tree_seed", "plantpot_calquat_seed", "plantpot_calquat_sapling", 72)
        chain("crystal_tree_seed", "plantpot_crystal_tree_seed", "plantpot_crystal_tree_sapling", 74)
        chain("spirit_tree_seed", "plantpot_spirit_tree_seed", "plantpot_spirit_tree_sapling", 83)
        chain("celastrus_tree_seed", "plantpot_celastrus_tree_seed", "plantpot_celastrus_tree_sapling", 85)
        chain("redwood_tree_seed", "plantpot_redwood_tree_seed", "plantpot_redwood_tree_sapling", 90)
        row("dbrow.farming_saplings") { column(0, *chains.toTypedArray()) }
    }
}
