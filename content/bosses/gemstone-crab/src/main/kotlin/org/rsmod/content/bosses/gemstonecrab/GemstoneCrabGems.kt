package org.rsmod.content.bosses.gemstonecrab

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.flatten
import dtx.rs.RSDropTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.game.entity.Player

public object GemstoneCrabGems {
    private const val ROLLS_PER_MINE = 3
    private const val DRAGONSTONE_CHANCE_DENOMINATOR = 500
    private const val MAIN_TABLE_TOTAL = 32

    private val table: RSDropTable<Player, DropRollItem> =
        RSDropTable(
            tableIdentifier = "Gemstone Crab Shell",
            mainRolls = ROLLS_PER_MINE,
            mainTable =
                rsPlayerWeightedTable(total = DRAGONSTONE_CHANCE_DENOMINATOR) {
                    name("Gemstone Crab Shell")
                    1 weight "obj.uncut_dragonstone" count 1
                    (DRAGONSTONE_CHANCE_DENOMINATOR - 1) weight
                        rsPlayerWeightedTable(total = MAIN_TABLE_TOTAL) {
                            9 weight "obj.uncut_opal" count 1
                            9 weight "obj.uncut_jade" count 1
                            6 weight "obj.uncut_red_topaz" count 1
                            3 weight "obj.uncut_sapphire" count 1
                            2 weight "obj.uncut_emerald" count 1
                            2 weight "obj.uncut_ruby" count 1
                            1 weight "obj.uncut_diamond" count 1
                        }
                },
        )

    public fun rollAll(player: Player): List<DropRollItem> =
        when (val result = table.roll(player, ArgMap.Empty).flatten()) {
            is RollResult.Nothing -> emptyList()
            is RollResult.Single -> listOf(result.result)
            is RollResult.ListOf -> result.results
        }
}
