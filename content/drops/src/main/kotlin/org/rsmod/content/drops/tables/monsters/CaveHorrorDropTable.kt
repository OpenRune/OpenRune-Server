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
public val caveHorrorDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Cave horror Drops",
    npcs = npcs("npc.harmless_island_cave_horror_alpha", "npc.harmless_island_cave_horror_guard", "npc.harmless_island_cave_horror_small_guard", "npc.harmless_island_cave_horror_worker", "npc.harmless_island_cave_horror_young_worker"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Cave horror Drops")
        3 weight "obj.mithril_axe" count 1
        1 weight "obj.rune_dagger" count 1
        1 weight "obj.adamant_full_helm" count 1
        1 weight "obj.mithril_kiteshield" count 1
        6 weight "obj.naturerune" count 6
        5 weight "obj.naturerune" count 4
        1 weight "obj.naturerune" count 3
        28 weight "obj.coins" count 44
        12 weight "obj.coins" count 132
        1 weight "obj.coins" count 440
        7 weight "obj.limpwurt_root" count 1
        7 weight "obj.cert_teak_logs" count 4
        3 weight "obj.mahogany_logs" count 2
        1 outOf 512 separate "obj.harmless_black_mask_10" count 1

        13 weight SharedDropTables.herb
        5 weight SharedDropTables.gem
        18 weight SharedDropTables.rareSeed
        16 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 30 weight "obj.arceuus_corpse_horror" count 1
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/121 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
