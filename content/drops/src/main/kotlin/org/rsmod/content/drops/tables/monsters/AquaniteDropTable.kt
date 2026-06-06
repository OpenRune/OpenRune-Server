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
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val aquaniteDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Aquanite Drops",
    npcs = npcs("npc.slayer_aquanite", "npc.slayer_aquanite_nolure"),
    preRoll = rsPlayerPrerollTable {
        1 outOf 3500 weight "obj.aquanite_tendon" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Aquanite Drops")
        10 weight "obj.waterrune" count 150..350
        2 weight "obj.rune_cannonball" count 14..22
        1 weight "obj.dragon_cannonball" count 9..15
        8 weight "obj.ranarr_seed" count 1
        8 weight "obj.irit_seed" count 1
        8 weight "obj.avantoe_seed" count 1
        8 weight "obj.kwuarm_seed" count 1
        8 weight "obj.cadantine_seed" count 1
        6 weight "obj.snape_grass_seed" count 1..2
        6 weight "obj.watermelon_seed" count 3..5
        6 weight "obj.pineapple_tree_seed" count 1
        6 weight "obj.coral_elkhorn_frag" count 2..4
        6 weight "obj.coral_pillar_frag" count 1..3
        6 weight "obj.coral_umbral_frag" count 1
        10 weight "obj.snape_grass" count 1..2
        10 weight "obj.seaweed" count 1
        6 weight "obj.shark_lure" count 3..5
        6 weight "obj.water_battlestaff" count 1
        4 weight "obj.coins" count 100..200
        3 weight "obj.uncut_diamond" count 1
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
        1 outOf 118 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
    },
)
