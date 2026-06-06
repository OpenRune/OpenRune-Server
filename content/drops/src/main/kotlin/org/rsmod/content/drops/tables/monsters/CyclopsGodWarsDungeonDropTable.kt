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
public val cyclopsGodWarsDungeonDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Cyclops (God Wars Dungeon) Drops",
    npcs = npcs("npc.godwars_ancient_cyclops", "npc.godwars_ancient_cyclops2"),
    mainTable = rsPlayerWeightedTable(total = 100) {
        name("Cyclops (God Wars Dungeon) Drops")
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
        40 weight "obj.coins" count 3..102
        10 weight "obj.coins" count 5..204

        3 weight SharedDropTables.herb
        2 weight SharedDropTables.gem
        1 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/486 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 512 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
