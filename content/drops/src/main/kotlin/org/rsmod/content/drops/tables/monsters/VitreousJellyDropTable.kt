package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.shouldDropBrimstoneKey
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val vitreousJellyDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Vitreous Jelly Drops",
    npcs = npcs("npc.superior_jelly"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.konar_key" count 1 killCondition {
            player, npc, areaChecker -> player.shouldDropBrimstoneKey(npc, areaChecker)
        }
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Vitreous Jelly Drops")
        11 weight "obj.steel_battleaxe" count 1
        7 weight "obj.steel_2h_sword" count 1
        3 weight "obj.steel_axe" count 1
        2 weight "obj.mithril_kiteshield" count 1
        1 weight "obj.mithril_armoured_boots" count 1
        1 weight "obj.rune_full_helm" count 1
        39 weight "obj.coins" count 102
        30 weight "obj.coins" count 44
        10 weight "obj.coins" count 220
        7 weight "obj.coins" count 11
        2 weight "obj.coins" count 460
        5 weight "obj.chaosrune" count 15
        3 weight "obj.deathrune" count 5
        2 weight "obj.gold_bar" count 1
        1 weight "obj.thread" count 10

        4 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/12 after unlocking the hard Combat Achievements rewards tier.
        10 outOf 128 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
