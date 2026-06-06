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
public val sulphurLizardDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Sulphur Lizard Drops",
    npcs = npcs("npc.sulphur_lizard"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Sulphur Lizard Drops")
        26 weight "obj.firerune" count 10..25
        4 weight "obj.firerune" count 40..60
        4 weight "obj.naturerune" count 5..10
        22 weight "obj.cert_iron_ore" count 5..10
        13 weight "obj.cert_coal" count 5..10
        4 weight "obj.cert_iron_bar" count 6..10
        4 weight "obj.cert_steel_bar" count 3..5
        4 weight "obj.cert_tin_ore" count 10..15
        3 weight "obj.cert_copper_ore" count 10..15
        3 weight "obj.cert_silver_ore" count 5..10
        2 weight "obj.cert_silver_bar" count 3..5
        1 weight "obj.cert_mithril_ore" count 3..5
        13 weight "obj.hunting_fish_special" count 1
        1 outOf 512 separate "obj.mystic_gloves_light" count 1

        10 weight SharedDropTables.herb
        6 weight SharedDropTables.gem
        9 weight SharedDropTables.seed
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): The medium clue scroll drop rate increases to 1/121 after unlocking the medium Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
