package org.rsmod.content.drops.tables.shared

import dtx.rs.RSWeightedTable
import dtx.rs.rsWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.dropRollable
import org.rsmod.api.droptable.ringNothing
import org.rsmod.content.drops.isOverworld
import org.rsmod.content.drops.isUnderground
import org.rsmod.game.entity.Player

public val gemDropTable: RSWeightedTable<Player, DropRollItem> = rsWeightedTable {
    name("Gem drop table")
    63 weight ringNothing()
    32 weight DropRollItem("obj.uncut_sapphire", 1)
    16 weight DropRollItem("obj.uncut_emerald", 1)
    8 weight DropRollItem("obj.uncut_ruby", 1)
    3 weight dropRollable(
        DropRollItem(
            "obj.chaos_talisman",
            1,
            killCondition = { _, npc, _ -> npc.coords.isUnderground() },
        ),
    )
    3 weight dropRollable(
        DropRollItem(
            "obj.nature_talisman",
            1,
            killCondition = { _, npc, _ -> npc.coords.isOverworld() },
        ),
    )
    2 weight DropRollItem("obj.uncut_diamond", 1)
    1 weight DropRollItem("obj.rune_javelin", 5)
    1 weight DropRollItem("obj.keyhalf2", 1)
    1 weight DropRollItem("obj.keyhalf1", 1)
    1 weight megaRareDropTable
}
