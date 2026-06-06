package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val moonlightCockatriceDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Moonlight Cockatrice Drops",
    npcs = npcs("npc.varlamore_cockatrice_moon01"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Moonlight Cockatrice Drops")
        3 weight "obj.mithril_sword" count 1
        3 weight "obj.mithril_dagger" count 1
        1 weight "obj.iron_armoured_boots" count 1
        1 weight "obj.mithril_javelin" count 5
        1 weight "obj.mithril_longsword" count 1
        6 weight "obj.naturerune" count 5
        4 weight "obj.naturerune" count 8
        2 weight "obj.naturerune" count 10
        3 weight "obj.lawrune" count 5
        2 weight "obj.waterrune" count 10
        2 weight "obj.firerune" count 15
        12 weight "obj.coins" count 125
        4 weight "obj.coins" count 62
        16 weight "obj.coins" count 50
        3 weight "obj.coins" count 42
        1 weight "obj.coins" count 30
        12 weight "obj.coins" count 15
        21 weight "obj.limpwurt_root" count 1
        1 weight ringNothing()
        1 outOf 512 separate "obj.mystic_boots_light" count 1

        10 weight SharedDropTables.herb
        2 weight SharedDropTables.gem
        18 weight SharedDropTables.seed
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1000 weight "obj.poh_trophydrop_cockatrice" count 1
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Clue scroll (medium) [tertiary/Rare]
