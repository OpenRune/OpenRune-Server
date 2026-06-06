package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val sergeantStrongstackDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Sergeant Strongstack Drops",
    npcs = npcs("npc.godwars_sergeant_goblin1"),
    mainTable = rsPlayerWeightedTable(total = 127) {
        name("Sergeant Strongstack Drops")
        7 weight "obj.steel_arrow" count 95..100
        8 weight "obj.steel_dart" count 95..100
        8 weight "obj.naturerune" count 15..20
        8 weight "obj.cosmicrune" count 25..30
        8 weight "obj.shark" count 2
        8 weight "obj.potato_chilli+carne" count 3
        66 weight "obj.coins" count 1400..1500
        8 weight "obj.cert_limpwurt_root" count 5
        2 weight "obj.3dosecombat" count 1
        2 weight "obj.3dose2strength" count 1
        1 outOf 16256 separate rsPlayerWeightedTable {
            1 weight "obj.bandos_chestplate" count 1
            1 weight "obj.bandos_skirt" count 1
            1 weight "obj.bandos_boots" count 1
        }
        125 outOf 16256 separate "obj.coins" count 1400..1500
        1 outOf 1524 separate rsPlayerWeightedTable {
            1 weight "obj.godwars_godsword_blade1" count 1
            1 weight "obj.godwars_godsword_blade2" count 1
            1 weight "obj.godwars_godsword_blade3" count 1
        }
        9 outOf 1524 separate "obj.coins" count 1400..1500
        2 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 6 weight "obj.kebab" count 1
        1 outOf 20 weight "obj.nex_frozen_key_bandos" count 1 condition { player ->
            // Drops Need Manual: Frozen key pieces are only dropped during The Frozen Door miniquest.
             true
        }
        1 outOf 5000 weight "obj.champions_challenge_goblin" count 1
        1 outOf 121 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
    },
)
