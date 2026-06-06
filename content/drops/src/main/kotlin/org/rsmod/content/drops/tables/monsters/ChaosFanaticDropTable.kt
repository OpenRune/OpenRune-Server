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
import org.rsmod.content.drops.shouldDropLootingBag
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val chaosFanaticDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Chaos Fanatic Drops",
    npcs = npcs("npc.chaos_fanatic"),
    preRoll = rsPlayerPrerollTable {
        1 outOf 256 weight "obj.odium_shard1" count 1
        1 outOf 256 weight "obj.malediction_shard1" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 133) {
        name("Chaos Fanatic Drops")
        5 weight "obj.cert_battlestaff" count 5
        5 weight "obj.splitbark_body" count 1
        5 weight "obj.splitbark_legs" count 1
        4 weight dropRollable(DropRollItem("obj.zamrobetop", 1, bonusDrops = listOf(
            DropRollItem("obj.zamrobebottom", 1),
        )))
        1 weight "obj.staff_of_zaros" count 1
        4 weight "obj.firerune" count 250
        4 weight "obj.smokerune" count 30
        4 weight "obj.chaosrune" count 175
        4 weight "obj.bloodrune" count 50
        8 weight "obj.monkfish" count 3
        8 weight "obj.shark" count 1
        8 weight "obj.4doseprayerrestore" count 1
        4 weight "obj.cert_anchovie_pizza" count 8
        18 weight "obj.coins" count 499..3998
        8 weight "obj.cert_unidentified_lantadyme" count 4
        7 weight "obj.ring_of_life" count 1
        6 weight "obj.chaos_talisman" count 1
        6 weight "obj.cert_wine_of_zamorak" count 10
        5 weight "obj.cert_uncut_emerald" count 6 condition { player ->
            // Drops Need Manual: 6 uncut emeralds are dropped alongside 4 uncut sapphires.
             true
        }
        5 weight "obj.cert_uncut_sapphire" count 4
        4 weight "obj.sinister_key" count 1
        2 weight "obj.cert_blankrune_high" count 250

        4 weight SharedDropTables.rareDrop
        4 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 3 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
        onBuilder { brimstoneKeyRoll() }
        1 outOf 1000 weight "obj.chaoselepet" count 1
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/121 after unlocking the hard Combat Achievements rewards tier.
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/64 if a ring of wealth (i) is worn.
        1 outOf 128 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
