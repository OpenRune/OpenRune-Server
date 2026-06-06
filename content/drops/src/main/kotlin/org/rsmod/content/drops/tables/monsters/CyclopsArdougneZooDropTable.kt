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
public val cyclopsArdougneZooDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Cyclops (Ardougne Zoo) Drops",
    npcs = npcs("npc.cyclops"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Cyclops (Ardougne Zoo) Drops")
        4 weight "obj.iron_dagger" count 1
        5 weight "obj.iron_full_helm" count 1
        3 weight "obj.iron_kiteshield" count 1
        2 weight "obj.steel_longsword" count 1
        6 weight "obj.iron_arrow" count 3
        3 weight "obj.firerune" count 15
        3 weight "obj.waterrune" count 7
        3 weight "obj.lawrune" count 2
        2 weight "obj.steel_arrow" count 10
        2 weight "obj.mindrune" count 3
        2 weight "obj.cosmicrune" count 2
        2 weight "obj.naturerune" count 6
        1 weight "obj.chaosrune" count 2
        1 weight "obj.deathrune" count 2
        14 weight "obj.coins" count 38
        10 weight "obj.coins" count 52
        8 weight "obj.coins" count 15
        6 weight "obj.coins" count 8
        2 weight "obj.coins" count 88
        11 weight "obj.limpwurt_root" count 1
        6 weight "obj.beer" count 1
        2 weight "obj.body_talisman" count 1
        1 weight "obj.hillgiant_boss_key" count 1

        7 weight SharedDropTables.herb
        3 weight SharedDropTables.gem
        18 weight SharedDropTables.seed
        1 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.trail_elite_riddle_key32" count 1 condition { player ->
            // Drops Need Manual: The key is only dropped when completing a medium clue scroll asking the player to kill a Hill Giant.
             true
        }
        1 outOf 25 weight "obj.arceuus_corpse_giant" count 1
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5000 weight "obj.champions_challenge_giant" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
        1 outOf 50 weight "obj.trail_clue_beginner" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
