package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val grimyLizardDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Grimy Lizard Drops",
    npcs = npcs("npc.crypt_of_tonali_grimy_lizard", "npc.grimy_lizard"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Grimy Lizard Drops")
        27 weight "obj.earthrune" count 10..25
        4 weight "obj.naturerune" count 5..10
        4 weight "obj.waterrune" count 40..60
        1 weight "obj.earth_talisman" count 1
        1 weight "obj.water_talisman" count 1
        19 weight "obj.hunting_fish_special" count 1
        13 weight "obj.oak_roots" count 1..3
        1 outOf 512 separate "obj.mystic_gloves_light" count 1

        10 weight SharedDropTables.herb
        4 weight SharedDropTables.gem
        1 weight SharedDropTables.usefulHerb
        33 weight SharedDropTables.seed
        5 weight SharedDropTables.rareSeed
        6 weight nothing()
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Clue scroll (medium) [tertiary/Rare]
