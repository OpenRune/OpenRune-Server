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
public val crazyArchaeologistDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Crazy archaeologist Drops",
    npcs = npcs("npc.crazy_archaeologist"),
    preRoll = rsPlayerPrerollTable {
        1 outOf 256 weight "obj.odium_shard2" count 1
        1 outOf 256 weight "obj.malediction_shard2" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 132) {
        name("Crazy archaeologist Drops")
        7 weight "obj.amulet_of_power" count 1
        5 weight "obj.xbows_crossbow_runite" count 2
        4 weight "obj.red_dragonhide_body" count 1
        4 weight "obj.rune_knife" count 10
        4 weight "obj.mudrune" count 30
        4 weight "obj.mcannonball" count 150
        1 weight "obj.dragon_arrow" count 75
        8 weight "obj.shark" count 1
        8 weight "obj.potato_cheese" count 3
        8 weight "obj.4doseprayerrestore" count 1
        4 weight "obj.cert_anchovie_pizza" count 8
        18 weight "obj.coins" count 499..3998
        8 weight "obj.cert_unidentified_dwarf_weed" count 4
        6 weight "obj.cert_white_berries" count 10
        6 weight "obj.cert_silver_ore" count 40
        5 weight "obj.cert_uncut_emerald" count 6 condition { player ->
            // Drops Need Manual: 6 uncut emeralds are dropped along with 4 uncut sapphires.
             true
        }
        5 weight "obj.cert_uncut_sapphire" count 4
        5 weight "obj.cert_dragonhide_red" count 10
        4 weight "obj.digsitesword" count 1
        4 weight "obj.muddy_key" count 1
        4 weight "obj.xbows_bolt_tips_onyx" count 12
        2 weight "obj.dorgesh_construction_bone" count 1

        4 weight SharedDropTables.rareDrop
        4 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 3 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
        onBuilder { brimstoneKeyRoll() }
        1 outOf 128 weight "obj.fedora" count 1
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/121 after unlocking the hard Combat Achievements rewards tier.
        // Drops Need Manual (rate): The drop rate of the hard clue scroll increases to 1/64 if a Ring of wealth (i) is worn.
        1 outOf 128 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
