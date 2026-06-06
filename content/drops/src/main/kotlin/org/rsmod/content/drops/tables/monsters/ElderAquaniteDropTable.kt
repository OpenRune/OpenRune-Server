package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val elderAquaniteDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Elder aquanite Drops",
    npcs = npcs("npc.superior_aquanite", "npc.superior_aquanite_nolure"),
    mainTable = rsPlayerWeightedTable(total = 22) {
        name("Elder aquanite Drops")
        1 weight "obj.coral_umbral_frag" count 1
        1 weight "obj.coral_pillar_frag" count 1..3
        1 weight "obj.coral_elkhorn_frag" count 2..4
        1 weight "obj.snape_grass_seed" count 1..3
        1 weight "obj.watermelon_seed" count 3..5
        1 weight "obj.pineapple_tree_seed" count 1
        1 weight "obj.shark_lure" count 3..5
        1 weight "obj.water_battlestaff" count 1
        1 outOf 750 separate "obj.aquanite_tendon" count 1
        1 outOf 128 separate "obj.dragon_cannonball" count 9..15
        1 outOf 64 separate "obj.rune_cannonball" count 14..22
        1 outOf 16 separate rsPlayerWeightedTable {
            1 weight "obj.ranarr_seed" count 1
            1 weight "obj.irit_seed" count 1
            1 weight "obj.avantoe_seed" count 1
            1 weight "obj.kwuarm_seed" count 1
            1 weight "obj.cadantine_seed" count 1
        }
        1 outOf 13 separate rsPlayerWeightedTable {
            1 weight "obj.snape_grass" count 1..2
            1 weight "obj.seaweed" count 1
            1 weight "obj.waterrune" count 150..350
        }
        1 outOf 42 separate "obj.uncut_diamond" count 1
        1 outOf 32 separate "obj.coins" count 104..188
        14 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Clue scroll (hard) [tertiary/Unknown]
