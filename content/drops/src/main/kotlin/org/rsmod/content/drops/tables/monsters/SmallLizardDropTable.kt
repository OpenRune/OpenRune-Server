package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val smallLizardDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Small Lizard Drops",
    npcs = npcs("npc.slayer_lizard_small1_green", "npc.slayer_lizard_small1_green_lowrange", "npc.slayer_lizard_small2_sandy"),
    mainTable = rsPlayerWeightedTable(total = 147) {
        name("Small Lizard Drops")
        30 weight "obj.firerune" count 5
        14 weight "obj.firerune" count 42
        4 weight "obj.naturerune" count 5
        13 weight "obj.coal" count 1
        3 weight "obj.copper_ore" count 1
        22 weight "obj.iron_ore" count 1
        2 weight "obj.silver_bar" count 1
        3 weight "obj.silver_ore" count 1
        4 weight "obj.tin_ore" count 1
        1 weight "obj.mithril_ore" count 1
        13 weight "obj.kebab" count 1
        13 weight "obj.water_skin0" count 2
        1 outOf 512 separate "obj.mystic_gloves_light" count 1

        10 weight SharedDropTables.herb
        6 weight SharedDropTables.gem
        9 weight SharedDropTables.seed
    },
)
