package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val gryphonDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Gryphon Drops",
    npcs = npcs("npc.slayer_gryphon_1", "npc.slayer_gryphon_2"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.gryphon_feather" count 1..3
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Gryphon Drops")
        10 weight "obj.onion" count 1
        10 weight "obj.strawberry" count 1
        10 weight "obj.watermelon" count 1
        10 weight "obj.raw_lobster" count 1
        10 weight "obj.raw_swordfish" count 1
        8 weight "obj.raw_shark" count 1
        8 weight "obj.potato" count 1
        1 weight "obj.cabbage" count 1
        1 weight "obj.tomato" count 1
        1 weight "obj.sweetcorn" count 1
        1 weight "obj.coral_elkhorn_frag" count 1
        1 weight "obj.coral_pillar_frag" count 1
        17 weight "obj.gryphon_feather" count 10..25
        6 weight "obj.mithril_cannonball" count 8..12
        4 weight "obj.basket_empty" count 1
        4 weight "obj.sack_empty" count 1
        2 weight "obj.adamant_cannonball" count 5..9

        11 weight SharedDropTables.herb
        1 weight SharedDropTables.gem
        12 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): The rarity changes to 1/1,000 while on a Gryphon Slayer task.
        1 outOf 2500 weight "obj.horn_of_plenty_uncharged" count 1
        1 outOf 95 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
    },
)
