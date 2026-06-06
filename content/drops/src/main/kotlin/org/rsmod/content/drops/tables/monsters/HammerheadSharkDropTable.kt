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
public val hammerheadSharkDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Hammerhead shark Drops",
    npcs = npcs("npc.sailing_hammerhead_shark"),
    mainTable = rsPlayerWeightedTable(total = 100) {
        name("Hammerhead shark Drops")
        10 weight "obj.yew_shortbow" count 1
        8 weight "obj.mithril_platebody" count 1
        6 weight "obj.adamant_mace" count 1
        5 weight "obj.blue_dragon_vambraces" count 1
        5 weight "obj.mystic_boots" count 1
        4 weight "obj.battlestaff" count 1
        3 weight "obj.water_battlestaff" count 1
        3 weight "obj.adamant_platebody" count 1
        12 weight "obj.coins" count 3000..8000
        12 weight "obj.ruby_necklace" count 1
        10 weight "obj.xbows_bolt_tips_sapphire" count 20..40
        8 weight "obj.raw_shark" count 1
        5 weight "obj.chaosrune" count 50..80
        4 weight "obj.mithril_cannonball" count 48..60
        2 weight "obj.sailing_barracuda_shipwreck_salvage" count 1
        12 outOf 1000 separate "obj.coral_pillar_frag" count 1
        6 outOf 1000 separate "obj.coral_elkhorn_frag" count 1
        2 outOf 1000 separate "obj.coral_umbral_frag" count 1

        1 weight SharedDropTables.gem
        2 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 2 weight "obj.sailing_hammerhead_shark_jaw" count 1 condition { player ->
            // Drops Need Manual: Only dropped while on an applicable bounty task.
             true
        }
        1 outOf 10 weight "obj.sailing_hammerhead_shark_liver" count 1
        // Drops Need Manual (rate): The medium clue scroll drop rate increases to 1/95 after unlocking the medium Combat Achievements rewards tier.
        1 outOf 100 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
