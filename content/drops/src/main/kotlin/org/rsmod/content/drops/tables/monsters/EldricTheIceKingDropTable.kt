package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.rsPlayerPrerollTable
import org.rsmod.api.droptable.dropRollable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val eldricTheIceKingDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Eldric the Ice King Drops",
    npcs = npcs("npc.rt_ice_king"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.desiccated_page" count 10..19
    },
    preRoll = rsPlayerPrerollTable {
        1 outOf 75 weight "obj.twinflame_piece_1" count 1
        1 outOf 75 weight "obj.deadeye_prayer_scroll" count 1 condition { player ->
            // Drops Need Manual: No longer dropped after reading the scroll.
             true
        }
        1 outOf 16 weight "obj.giantsoul_amulet_charged" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 56) {
        name("Eldric the Ice King Drops")
        2 weight "obj.cert_mystic_water_staff" count 4
        2 weight "obj.cert_water_battlestaff" count 6
        2 weight "obj.rune_platelegs" count 2
        2 weight "obj.rune_sq_shield" count 2
        2 weight "obj.rune_axe" count 2
        1 weight "obj.rune_pickaxe" count 2
        2 weight "obj.waterrune" count 1500..3000
        2 weight "obj.chaosrune" count 200..400
        2 weight "obj.deathrune" count 150..300
        2 weight "obj.naturerune" count 100..150
        2 weight "obj.lawrune" count 100..150
        2 weight "obj.soulrune" count 100..150
        2 weight "obj.bloodrune" count 100..150
        2 weight "obj.rune_arrow" count 100..300
        2 weight "obj.cert_coal" count 280..320
        2 weight "obj.cert_gold_ore" count 60..100
        2 weight "obj.cert_water_orb" count 20..40
        2 weight "obj.avantoe_seed" count 4
        2 weight "obj.cadantine_seed" count 4
        2 weight "obj.dwarf_weed_seed" count 3
        2 weight "obj.irit_seed" count 4
        2 weight "obj.kwuarm_seed" count 4
        2 weight "obj.lantadyme_seed" count 4
        2 weight "obj.ranarr_seed" count 4
        1 weight "obj.maple_seed" count 4..6
        1 weight "obj.palm_tree_seed" count 2
        1 weight "obj.yew_seed" count 2
        2 weight "obj.coins" count 10000..30000
        2 weight "obj.4doseprayerrestore" count 2..4
        2 weight "obj.desiccated_page" count 6..24
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): Sacrificing loot doubles the pet drop rate.
        1 outOf 3000 weight "obj.rtbrandapet" count 1
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/23 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 25 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/95 after unlocking the elite Combat Achievements rewards tier.
        1 outOf 100 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
