package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.shouldDropLootingBag
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val orkDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Ork Drops",
    npcs = npcs("npc.godwars_ancient_ork1", "npc.godwars_ancient_ork2", "npc.godwars_ancient_ork3", "npc.godwars_ancient_ork4"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Ork Drops")
        5 weight "obj.bronze_2h_sword" count 1
        5 weight "obj.bronze_battleaxe" count 1
        5 weight "obj.bronze_dart" count 2
        5 weight "obj.bronze_halberd" count 1
        5 weight "obj.bronze_scimitar" count 1
        5 weight "obj.bronze_warhammer" count 1
        5 weight "obj.iron_dart" count 2
        5 weight "obj.iron_longsword" count 1
        5 weight "obj.iron_mace" count 1
        5 weight "obj.iron_scimitar" count 1
        5 weight "obj.steel_axe" count 1
        5 weight "obj.steel_dagger" count 1
        5 weight "obj.steel_mace" count 1
        4 weight "obj.iron_spear" count 1
        1 weight "obj.bronze_scimitar" count 1
        5 weight "obj.bronze_med_helm" count 1
        5 weight "obj.bronze_full_helm" count 1
        5 weight "obj.bronze_platelegs" count 1
        5 weight "obj.iron_chainbody" count 1
        5 weight "obj.steel_med_helm" count 1
        8 weight "obj.coins" count 10
        5 weight "obj.coins" count 5
        5 weight "obj.coins" count 8
        5 weight "obj.coins" count 12
        5 weight "obj.coins" count 23
        5 weight "obj.knife" count 1
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 3 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
        // Drops Need Manual (rate): The easy clue scroll drop rate increases to 1/121 after unlocking the easy Combat Achievements rewards tier.
        // Drops Need Manual (rate): Clue scroll drop rates are increased to 1/64 if a ring of wealth (i) is worn and fought in the Wilderness God Wars Dungeon.
        1 outOf 128 weight "obj.trail_clue_easy_simple001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/121 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
