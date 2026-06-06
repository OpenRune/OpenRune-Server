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
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val kingKuraskDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "King kurask Drops",
    npcs = npcs("npc.superior_kurask"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.prif_crystal_shard" count (6..10) condition { player ->
            // Drops Need Manual: Crystal shards are only dropped by those found within the Iorwerth Dungeon.
             true
        }
        "obj.konar_key" count 1 killCondition {
            player, npc, areaChecker -> player.shouldDropBrimstoneKey(npc, areaChecker)
        }
    },
    mainTable = rsPlayerWeightedTable(total = 124) {
        name("King kurask Drops")
        3 weight "obj.mithril_kiteshield" count 1
        3 weight "obj.rune_longsword" count 1
        3 weight "obj.adamant_platebody" count 1
        3 weight "obj.rune_axe" count 1
        10 weight "obj.naturerune" count 10
        7 weight "obj.naturerune" count 15
        4 weight "obj.naturerune" count 30
        16 weight "obj.coins" count 2000..3000
        6 weight "obj.cert_flax" count 100
        6 weight "obj.cert_white_berries" count 12
        5 weight "obj.cert_big_bones" count 20
        5 weight "obj.coins" count 10000
        4 weight "obj.cert_papaya" count 10
        4 weight "obj.cert_coconut" count 10
        1 outOf 384 separate "obj.leafbladed_sword" count 1
        1 outOf 512 separate "obj.mystic_robe_top_light" count 1
        1 outOf 1026 separate "obj.leafbladed_battleaxe" count 1

        18 weight SharedDropTables.herb
        6 weight SharedDropTables.gem
        15 weight SharedDropTables.rareSeed
        6 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 3000 weight "obj.poh_trophydrop_kurask" count 1
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/12 after unlocking the hard Combat Achievements rewards tier.
        10 outOf 128 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
