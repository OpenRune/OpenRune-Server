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
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val nechryarchDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Nechryarch Drops",
    npcs = npcs("npc.superior_nechryael"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.konar_key" count 1 killCondition {
            player, npc, areaChecker -> player.shouldDropBrimstoneKey(npc, areaChecker)
        }
        "obj.prif_crystal_shard" count (6..10) condition { player ->
            // Drops Need Manual: Crystal shards are only dropped by those found within the Iorwerth Dungeon.
             true
        }
        "obj.konar_key" count 1 killCondition {
            player, npc, areaChecker -> player.shouldDropBrimstoneKey(npc, areaChecker)
        }
    },
    mainTable = rsPlayerWeightedTable(total = 147) {
        name("Nechryarch Drops")
        7 weight "obj.adamant_kiteshield" count 1
        7 weight "obj.rune_axe" count 1
        7 weight "obj.rune_sq_shield" count 1
        5 weight "obj.adamant_battleaxe" count 1
        4 weight "obj.rune_med_helm" count 1
        3 weight "obj.rune_full_helm" count 1
        2 weight "obj.mystic_air_staff" count 1
        1 weight "obj.rune_armoured_boots" count 1
        1 weight "obj.rune_chainbody" count 1
        12 weight "obj.deathrune" count 23
        10 weight "obj.bloodrune" count 20
        10 weight "obj.chaosrune" count 50
        6 weight "obj.airrune" count 150
        5 weight "obj.soulrune" count 25
        10 weight "obj.lobster" count 1
        8 weight "obj.coins" count 2000..2500
        7 weight "obj.cert_gold_bar" count 5
        6 weight "obj.tuna" count 2
        2 weight "obj.cert_wine_of_zamorak" count 3
        4 outOf 116 separate rsPlayerWeightedTable {
            4 weight "obj.adamant_platelegs" count 1
            4 weight "obj.rune_2h_sword" count 1
        }
        3 outOf 116 separate "obj.rune_full_helm" count 1
        2 outOf 116 separate "obj.adamant_kiteshield" count 1
        1 outOf 116 separate "obj.rune_armoured_boots" count 1
        8 outOf 116 separate "obj.chaosrune" count 37
        6 outOf 116 separate rsPlayerWeightedTable {
            6 weight "obj.deathrune" count 5
            6 weight "obj.deathrune" count 10
        }
        5 outOf 116 separate "obj.lawrune" count 25..35
        4 outOf 116 separate "obj.bloodrune" count 15..20
        13 outOf 116 separate "obj.coins" count 1000..1499
        11 outOf 116 separate "obj.coins" count 1500..2000
        6 outOf 116 separate "obj.coins" count 2500..2999
        3 outOf 116 separate rsPlayerWeightedTable {
            3 weight "obj.coins" count 3000..3500
            3 weight "obj.coins" count 500..999
        }
        1 outOf 116 separate "obj.coins" count 5000
        4 outOf 116 separate "obj.cert_softclay" count 25
        3 outOf 116 separate "obj.tuna" count 1

        7 weight SharedDropTables.herb
        3 weight SharedDropTables.gem
        1 weight SharedDropTables.rareDrop
        18 weight SharedDropTables.rareSeed
        5 weight SharedDropTables.rareSeed
    },
    tertiaries = rsPlayerTertiaryTable {
        10 outOf 121 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
        1 outOf 121 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
    },
)
