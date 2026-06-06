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
public val growlerDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Growler Drops",
    npcs = npcs("npc.godwars_saradomin_lion"),
    mainTable = rsPlayerWeightedTable(total = 127) {
        name("Growler Drops")
        8 weight "obj.steel_arrow" count 95..100
        8 weight "obj.steel_dart" count 95..100
        8 weight "obj.lawrune" count 5..10
        8 weight "obj.monkfish" count 3
        8 weight "obj.summer_pie" count 1
        62 weight "obj.coins" count 1300..1400
        8 weight "obj.cert_unidentified_ranarr" count 1
        7 weight "obj.cert_snape_grass" count 5
        8 weight "obj.cert_unicorn_horn" count 6
        3 outOf 16129 separate "obj.saradomin_sword" count 1
        124 outOf 16129 separate "obj.coins" count 1400..1500
        1 outOf 1524 separate rsPlayerWeightedTable {
            1 weight "obj.godwars_godsword_blade1" count 1
            1 weight "obj.godwars_godsword_blade2" count 1
            1 weight "obj.godwars_godsword_blade3" count 1
        }
        9 outOf 1524 separate "obj.coins" count 1300..1400
        2 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 20 weight "obj.nex_frozen_key_saradomin" count 1 condition { player ->
            // Drops Need Manual: Frozen key pieces are only dropped during The Frozen Door miniquest.
             true
        }
        1 outOf 121 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
    },
)
