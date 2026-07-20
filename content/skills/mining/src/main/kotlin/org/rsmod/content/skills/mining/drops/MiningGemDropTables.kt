package org.rsmod.content.skills.mining.drops

import dtx.rs.RSWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.game.entity.Player

object MiningGemDropTables {
    const val KEEP_ORE = "mining.gem_keep_ore"

    val randomGem: RSWeightedTable<Player, DropRollItem> = miningRandomGemTable(glory = false)

    val randomGemGlory: RSWeightedTable<Player, DropRollItem> = miningRandomGemTable(glory = true)

    val gemRock: RSWeightedTable<Player, DropRollItem> =
        rsPlayerWeightedTable(total = 128) {
            name("Gem rock")
            60 weight "obj.uncut_opal" count 1
            30 weight "obj.uncut_jade" count 1
            15 weight "obj.uncut_red_topaz" count 1
            9 weight "obj.uncut_sapphire" count 1
            5 weight "obj.uncut_emerald" count 1
            5 weight "obj.uncut_ruby" count 1
            4 weight "obj.uncut_diamond" count 1
        }

    private fun miningRandomGemTable(glory: Boolean): RSWeightedTable<Player, DropRollItem> {
        val total = if (glory) 8216 else 32768
        return rsPlayerWeightedTable(total = total) {
            name(if (glory) "Mining gem drop table (glory)" else "Mining gem drop table")
            70 weight nothing()
            32 weight "obj.uncut_sapphire" count 1
            16 weight "obj.uncut_emerald" count 1
            8 weight "obj.uncut_ruby" count 1
            2 weight "obj.uncut_diamond" count 1
            (total - 128) weight DropRollItem(KEEP_ORE, 1)
        }
    }
}
