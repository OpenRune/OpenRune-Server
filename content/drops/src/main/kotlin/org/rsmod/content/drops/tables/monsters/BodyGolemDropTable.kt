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
public val bodyGolemDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Body Golem Drops",
    npcs = npcs("npc.camdozaal_golem_body", "npc.camdozaal_golem_body_rock"),
    mainTable = rsPlayerWeightedTable(total = 52) {
        name("Body Golem Drops")
        6 weight "obj.clay" count 3..4
        6 weight "obj.copper_ore" count 3..4
        6 weight "obj.tin_ore" count 3..4
        6 weight "obj.iron_ore" count 2..3
        6 weight "obj.cert_blankrune" count 4..6
        1 weight "obj.cert_blankrune" count 1
        8 weight "obj.bodyrune" count 5..10
        6 weight "obj.bodyrune" count 1
        4 weight "obj.uncut_sapphire" count 1
        2 weight "obj.uncut_emerald" count 1
        1 weight "obj.uncut_ruby" count 1
    },
    tertiaries = rsPlayerTertiaryTable {
        2 outOf 10 weight "obj.camdozaal_barronite_shard" count 10..20
        2 outOf 15 weight "obj.camdozaal_golem_core_body" count 1
        1 outOf 250 weight "obj.barronite_mace_3" count 1
        1 outOf 62 weight "obj.league_clue_box_beginner" count 1
    },
)
