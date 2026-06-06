package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.shouldDropLootingBag
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val jellyDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Jelly Drops",
    npcs = npcs("npc.slayer_jelly_1", "npc.slayer_jelly_2", "npc.slayer_jelly_3", "npc.slayer_jelly_4", "npc.slayer_jelly_5", "npc.slayer_jelly_6", "npc.wild_cave_jelly_1", "npc.wild_cave_jelly_2", "npc.wild_cave_jelly_3", "npc.wild_cave_jelly_4", "npc.wild_cave_jelly_5"),
    mainTable = rsPlayerWeightedTable(total = 133) {
        name("Jelly Drops")
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
        11 outOf 64 separate "obj.adamant_battleaxe" count 1
        5 outOf 64 separate "obj.black_2h_sword" count 1
        3 outOf 64 separate "obj.adamant_axe" count 1
        2 outOf 64 separate rsPlayerWeightedTable {
            2 weight "obj.adamant_2h_sword" count 1
            2 weight "obj.mithril_armoured_boots" count 1
            2 weight "obj.rune_kiteshield" count 1
        }
        1 outOf 64 separate "obj.rune_full_helm" count 1
        5 outOf 64 separate "obj.chaosrune" count 45
        3 outOf 64 separate "obj.deathrune" count 15
        21 outOf 64 separate "obj.coins" count 1000
        2 outOf 64 separate "obj.gold_bar" count 1
        1 outOf 64 separate "obj.thread" count 10

        4 weight SharedDropTables.gem
        5 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/121 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
        1 outOf 3 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/60 after unlocking the hard Combat Achievements rewards tier.
        // Drops Need Manual (rate): The hard clue scroll rarity changes to 1/32 if a ring of wealth (i) is worn.
        1 outOf 64 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
