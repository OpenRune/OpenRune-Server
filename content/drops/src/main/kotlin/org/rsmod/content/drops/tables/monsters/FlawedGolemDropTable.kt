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
public val flawedGolemDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Flawed Golem Drops",
    npcs = npcs("npc.camdozaal_golem_flawed", "npc.camdozaal_golem_flawed_rock"),
    mainTable = rsPlayerWeightedTable(total = 52) {
        name("Flawed Golem Drops")
        8 weight "obj.clay" count 1
        5 weight "obj.clay" count 2
        3 weight "obj.clay" count 3
        12 weight "obj.cert_blankrune" count 1..3
        9 weight "obj.cert_blankrune" count 2..4
        6 weight "obj.tin_ore" count 1..2
        6 weight "obj.copper_ore" count 1..2
        2 weight "obj.uncut_sapphire" count 1
        1 weight "obj.uncut_emerald" count 1
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 16 weight "obj.camdozaal_barronite_shard" count 6..12
        1 outOf 800 weight "obj.barronite_mace_3" count 1
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Clue scroll (beginner) [tertiary/Rare]
