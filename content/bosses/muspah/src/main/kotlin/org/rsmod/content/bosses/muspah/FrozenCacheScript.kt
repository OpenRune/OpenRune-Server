package org.rsmod.content.bosses.muspah

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.flatten
import jakarta.inject.Inject
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.rollCount
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpHeld1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FrozenCacheScript @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1("obj.frozen_cache") { open() }
    }

    private fun ProtectedAccess.open() {
        if (invDel(inv, "obj.frozen_cache", 1).failure) {
            return
        }

        val preRoll = preRollTable.roll(player, ArgMap()).flatten()
        val preRollHit = preRoll is RollResult.Single && !preRoll.result.isNothing
        val result = if (preRollHit) preRoll else mainTable.roll(player, ArgMap()).flatten()
        when (result) {
            is RollResult.Nothing -> Unit
            is RollResult.Single -> give(result.result)
            is RollResult.ListOf -> result.results.forEach { give(it) }
        }
    }

    private fun ProtectedAccess.give(drop: DropRollItem) {
        if (drop.isNothing) return
        val count = drop.rollCount(random)
        if (drop.obj == "obj.frozen_cache") {
            mes("You open the frozen cache and find... another frozen cache inside! How peculiar.")
        }
        invAddOrDrop(objRepo, drop.obj, count)
    }

    private companion object {
        val preRollTable =
            rsPlayerWeightedTable(total = 500) {
                name("Frozen Cache Pre-roll")
                7 weight "obj.frozen_cache" count 1
                2 weight "obj.ancient_icon" count 1
                1 weight "obj.venator_shard" count 1
                490 weight nothing()
            }

        val ancientEssenceTable =
            rsPlayerWeightedTable(total = 93) {
                name("Frozen Cache Ancient Essence")
                60 weight "obj.ancient_essence" count 540..599
                23 weight DropRollItem("obj.ancient_essence", 885..995, (885..995 step 5).toList())
                10 weight DropRollItem("obj.ancient_essence", 1970..2060, (1970..2060 step 10).toList())
            }

        val mainTable =
            rsPlayerWeightedTable(total = 80) {
                name("Frozen Cache")
                15 weight ancientEssenceTable
                5 weight "obj.dragon_plateskirt" count 1
                5 weight "obj.cert_rune_platelegs" count 3
                5 weight "obj.black_dragonhide_body" count 1
                4 weight "obj.cert_dragon_platelegs" count 2
                1 weight "obj.rune_sword" count 1
                5 weight "obj.chaosrune" count 480
                5 weight "obj.firerune" count 1964
                5 weight "obj.mcannonball" count 670
                5 weight "obj.torstol_seed" count 2
                3 weight "obj.cert_unidentified_toadflax" count 40
                3 weight "obj.ranarr_seed" count 5
                4 weight "obj.snapdragon_seed" count 5
                2 weight "obj.spirit_tree_seed" count 1
                5 weight "obj.cert_coal" count 163
                3 weight "obj.cert_runite_ore" count 18
                3 weight "obj.cert_limpwurt_root" count 21
                2 weight "obj.cert_silver_ore" count 101
            }
    }
}
