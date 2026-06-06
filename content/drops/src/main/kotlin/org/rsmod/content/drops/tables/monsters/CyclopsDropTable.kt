package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.rsPlayerPrerollTable
import org.rsmod.api.droptable.dropRollable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val cyclopsDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Cyclops Drops",
    npcs = npcs("npc.warguild_cyclops1", "npc.warguild_cyclops1_high", "npc.warguild_cyclops2", "npc.warguild_cyclops2_high", "npc.warguild_cyclops3", "npc.warguild_cyclops3_high", "npc.warguild_cyclops4", "npc.warguild_cyclops4_high", "npc.warguild_cyclops5", "npc.warguild_cyclops5_high", "npc.warguild_cyclops6", "npc.warguild_cyclops6_high"),
    preRoll = rsPlayerPrerollTable {
        1 outOf 100 weight "obj.dragon_parryingdagger" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 201) {
        name("Cyclops Drops")
        16 weight "obj.black_knife" count 4..13
        2 weight "obj.steel_chainbody" count 1
        2 weight "obj.iron_2h_sword" count 1
        2 weight "obj.iron_chainbody" count 1
        2 weight "obj.steel_dagger" count 1
        2 weight "obj.steel_mace" count 1
        2 weight "obj.steel_sword" count 1
        2 weight "obj.steel_battleaxe" count 1
        2 weight "obj.steel_2h_sword" count 1
        2 weight "obj.steel_longsword" count 1
        2 weight "obj.steel_med_helm" count 1
        1 weight "obj.black_2h_sword" count 1
        1 weight "obj.mithril_dagger" count 1
        1 weight "obj.mithril_longsword" count 1
        1 weight "obj.adamant_mace" count 1
        1 weight "obj.black_sword" count 1
        1 weight "obj.black_longsword" count 1
        1 weight "obj.black_dagger" count 1
        1 weight "obj.adamant_2h_sword" count 1
        2 weight "obj.bronze_parryingdagger" count 1
        2 weight "obj.iron_parryingdagger" count 1
        2 weight "obj.steel_parryingdagger" count 1
        2 weight "obj.black_parryingdagger" count 1
        2 weight "obj.mithril_parryingdagger" count 1
        2 weight "obj.adamant_parryingdagger" count 1
        2 weight "obj.rune_parryingdagger" count 1
        31 weight "obj.coins" count 3..102
        10 weight "obj.coins" count 5..204
        4 weight "obj.adamant_2h_sword" count 1
        6 weight "obj.mithril_dart" count 12
        5 weight "obj.adamant_dagger" count 1
        5 weight "obj.black_full_helm" count 1
        5 weight "obj.black_mace" count 1
        5 weight "obj.mithril_scimitar" count 1
        5 weight "obj.mithril_kiteshield" count 1
        5 weight "obj.steel_platebody" count 1
        7 weight "obj.steel_chainbody" count 1
        4 weight "obj.black_knife" count 22
        4 weight "obj.mithril_platelegs" count 1
        5 weight "obj.black_dagger" count 1
        3 weight "obj.rune_med_helm" count 1
        1 weight "obj.adamant_sq_shield" count 1
        1 weight "obj.rune_full_helm" count 1
        11 weight "obj.coins" count 96
        8 weight "obj.coins" count 350..449
        7 weight "obj.lobster" count 1

        3 weight SharedDropTables.herb
        4 weight SharedDropTables.herb
        2 weight SharedDropTables.gem
        2 weight SharedDropTables.rareDrop
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/486 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 512 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/243 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 256 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
