package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val mindGolemDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Mind Golem Drops",
    npcs = npcs("npc.camdozaal_golem_mind", "npc.camdozaal_golem_mind_rock"),
    mainTable = rsPlayerWeightedTable(total = 52) {
        name("Mind Golem Drops")
        6 weight "obj.clay" count 2..3
        6 weight "obj.copper_ore" count 2..3
        6 weight "obj.tin_ore" count 2..3
        6 weight "obj.iron_ore" count 1..2
        6 weight "obj.cert_blankrune" count 3..5
        1 weight "obj.cert_blankrune" count 1
        8 weight "obj.mindrune" count 5..10
        6 weight "obj.mindrune" count 1
        4 weight "obj.uncut_sapphire" count 1
        2 weight "obj.uncut_emerald" count 1
        1 weight "obj.uncut_ruby" count 1
    },
    tertiaries = rsPlayerTertiaryTable {
        2 outOf 15 weight "obj.camdozaal_barronite_shard" count 8..16
        2 outOf 15 weight "obj.camdozaal_golem_core_mind" count 1
        1 outOf 500 weight "obj.barronite_mace_3" count 1
        1 outOf 50 weight "obj.trail_clue_beginner" count 1
    },
)
