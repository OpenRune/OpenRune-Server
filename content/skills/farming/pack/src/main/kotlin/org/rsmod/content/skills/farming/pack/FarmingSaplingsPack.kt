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
        fun chain(seed: String, dry: String, watered: String, sapling: String, level: Int) {
            chains += listOf(seed.asRSCM(), dry.asRSCM(), watered.asRSCM(), sapling.asRSCM(), level)
        }
        chain("obj.acorn", "obj.plantpot_acorn", "obj.plantpot_acorn_watered", "obj.plantpot_oak_sapling", 15)
        chain("obj.willow_seed", "obj.plantpot_willow_seed", "obj.plantpot_willow_seed_watered", "obj.plantpot_willow_sapling", 30)
        chain("obj.maple_seed", "obj.plantpot_maple_seed", "obj.plantpot_maple_seed_watered", "obj.plantpot_maple_sapling", 45)
        chain("obj.yew_seed", "obj.plantpot_yew_seed", "obj.plantpot_yew_seed_watered", "obj.plantpot_yew_sapling", 60)
        chain("obj.magic_tree_seed", "obj.plantpot_magic_tree_seed", "obj.plantpot_magic_tree_seed_watered", "obj.plantpot_magic_tree_sapling", 75)
        chain("obj.apple_tree_seed", "obj.plantpot_apple_seed", "obj.plantpot_apple_seed_watered", "obj.plantpot_apple_sapling", 27)
        chain("obj.banana_tree_seed", "obj.plantpot_banana_seed", "obj.plantpot_banana_seed_watered", "obj.plantpot_banana_sapling", 33)
        chain("obj.orange_tree_seed", "obj.plantpot_orange_seed", "obj.plantpot_orange_seed_watered", "obj.plantpot_orange_sapling", 39)
        chain("obj.curry_tree_seed", "obj.plantpot_curry_seed", "obj.plantpot_curry_seed_watered", "obj.plantpot_curry_sapling", 42)
        chain("obj.pineapple_tree_seed", "obj.plantpot_pineapple_seed", "obj.plantpot_pineapple_seed_watered", "obj.plantpot_pineapple_sapling", 51)
        chain("obj.papaya_tree_seed", "obj.plantpot_papaya_seed", "obj.plantpot_papaya_seed_watered", "obj.plantpot_papaya_sapling", 57)
        chain("obj.palm_tree_seed", "obj.plantpot_palm_seed", "obj.plantpot_palm_seed_watered", "obj.plantpot_palm_sapling", 68)
        chain("obj.dragonfruit_tree_seed", "obj.plantpot_dragonfruit_seed", "obj.plantpot_dragonfruit_seed_watered", "obj.plantpot_dragonfruit_sapling", 81)
        chain("obj.teak_seed", "obj.plantpot_teak_seed", "obj.plantpot_teak_seed_watered", "obj.plantpot_teak_sapling", 35)
        chain("obj.mahogany_seed", "obj.plantpot_mahogany_seed", "obj.plantpot_mahogany_seed_watered", "obj.plantpot_mahogany_sapling", 55)
        chain("obj.camphor_seed", "obj.plantpot_camphor_seed", "obj.plantpot_camphor_seed_watered", "obj.plantpot_camphor_sapling", 66)
        chain("obj.ironwood_seed", "obj.plantpot_ironwood_seed", "obj.plantpot_ironwood_seed_watered", "obj.plantpot_ironwood_sapling", 80)
        chain("obj.rosewood_seed", "obj.plantpot_rosewood_seed", "obj.plantpot_rosewood_seed_watered", "obj.plantpot_rosewood_sapling", 92)
        chain("obj.calquat_tree_seed", "obj.plantpot_calquat_seed", "obj.plantpot_calquat_seed_watered", "obj.plantpot_calquat_sapling", 72)
        chain("obj.crystal_tree_seed", "obj.plantpot_crystal_tree_seed", "obj.plantpot_crystal_tree_seed_watered", "obj.plantpot_crystal_tree_sapling", 74)
        chain("obj.spirit_tree_seed", "obj.plantpot_spirit_tree_seed", "obj.plantpot_spirit_tree_seed_watered", "obj.plantpot_spirit_tree_sapling", 83)
        chain("obj.celastrus_tree_seed", "obj.plantpot_celastrus_tree_seed", "obj.plantpot_celastrus_tree_seed_watered", "obj.plantpot_celastrus_tree_sapling", 85)
        chain("obj.redwood_tree_seed", "obj.plantpot_redwood_tree_seed", "obj.plantpot_redwood_tree_seed_watered", "obj.plantpot_redwood_tree_sapling", 90)
        row("dbrow.farming_saplings") { column(0, *chains.toTypedArray()) }
    }
}
