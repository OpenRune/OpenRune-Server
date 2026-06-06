package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val flightKilisaDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Flight Kilisa Drops",
    npcs = npcs("npc.godwars_armadyl_bodyguard_kilisa"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.feather" count 1..11
    },
    mainTable = rsPlayerWeightedTable(total = 127) {
        name("Flight Kilisa Drops")
        7 weight "obj.steel_arrow" count 91..101
        8 weight "obj.steel_dart" count 91..101
        8 weight "obj.smokerune" count 10..15
        8 weight "obj.mantaray" count 2
        8 weight "obj.potato_mushroom+onion" count 3
        70 weight "obj.coins" count 1000..1100
        8 weight "obj.cert_crushed_bird_nest" count 2
        8 weight "obj.cert_unidentified_kwuarm" count 1
        1 outOf 16129 separate rsPlayerWeightedTable {
            1 weight "obj.armadyl_helmet" count 1
            1 weight "obj.armadyl_chestplate" count 1
            1 weight "obj.armadyl_skirt" count 1
        }
        124 outOf 16129 separate "obj.coins" count 1000..1100
        1 outOf 1524 separate rsPlayerWeightedTable {
            1 weight "obj.godwars_godsword_blade1" count 1
            1 weight "obj.godwars_godsword_blade2" count 1
            1 weight "obj.godwars_godsword_blade3" count 1
        }
        9 outOf 1524 separate "obj.coins" count 1000..1100
        2 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 20 weight "obj.nex_frozen_key_armadyl" count 1 condition { player ->
            // Drops Need Manual: [[Frozen key (The Frozen Door)
             true
        }
        onBuilder { brimstoneKeyRoll() }
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/121 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
