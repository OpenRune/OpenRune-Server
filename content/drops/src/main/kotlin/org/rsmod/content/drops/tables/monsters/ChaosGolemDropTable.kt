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
public val chaosGolemDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Chaos Golem Drops",
    npcs = npcs("npc.camdozaal_golem_chaos", "npc.camdozaal_golem_chaos_rock"),
    mainTable = rsPlayerWeightedTable(total = 52) {
        name("Chaos Golem Drops")
        6 weight "obj.clay" count 3..4
        6 weight "obj.iron_ore" count 3..4
        6 weight "obj.silver_ore" count 3..4
        6 weight "obj.gold_ore" count 2..3
        6 weight "obj.cert_blankrune" count 4..6
        8 weight "obj.chaosrune" count 5..10
        6 weight "obj.uncut_sapphire" count 1
        4 weight "obj.uncut_emerald" count 1
        2 weight "obj.uncut_ruby" count 1
        1 weight "obj.uncut_diamond" count 1
        1 weight "obj.chaos_talisman" count 1
    },
    tertiaries = rsPlayerTertiaryTable {
        3 outOf 10 weight "obj.camdozaal_barronite_shard" count 12..24
        2 outOf 15 weight "obj.camdozaal_golem_core_chaos" count 1
        1 outOf 150 weight "obj.barronite_mace_3" count 1
        1 outOf 100 weight "obj.trail_clue_beginner" count 1
    },
)
