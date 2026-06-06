package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import dtx.rs.rsWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.content.drops.tables.shared.doubleRollHerbDropTable
import org.rsmod.content.drops.tables.shared.herbDropTable
import org.rsmod.content.drops.tables.shared.tripleRollHerbDropTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.shouldDropBrimstoneKey
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val nightBeastDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Night beast Drops",
    npcs = npcs("npc.superior_dark_beast"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.prif_crystal_shard" count (6..10) condition { player ->
            // Drops Need Manual: Crystal shards are only dropped by those found within the Iorwerth Dungeon.
             true
        }
        "obj.konar_key" count 1 killCondition {
            player, npc, areaChecker -> player.shouldDropBrimstoneKey(npc, areaChecker)
        }
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Night beast Drops")
        3 weight "obj.black_battleaxe" count 1
        1 weight "obj.adamant_sq_shield" count 1
        1 weight "obj.rune_chainbody" count 1
        1 weight "obj.rune_med_helm" count 1
        1 weight "obj.rune_full_helm" count 1
        1 weight "obj.rune_2h_sword" count 1
        1 weight "obj.rune_battleaxe" count 1
        8 weight "obj.deathrune" count 20
        7 weight "obj.chaosrune" count 30
        4 weight "obj.bloodrune" count 15
        2 weight "obj.cert_adamantite_bar" count 3
        1 weight "obj.cert_adamantite_ore" count 5
        1 weight "obj.cert_runite_ore" count 1
        40 weight "obj.coins" count 152
        6 weight "obj.coins" count 64
        6 weight "obj.coins" count 95
        5 weight "obj.coins" count 220
        3 weight "obj.shark" count 1
        1 weight "obj.shark" count 2
        1 weight "obj.death_talisman" count 1
        1 outOf 512 separate "obj.darkbow" count 1

        24 weight rsWeightedTable(total = 5) {
            4 weight herbDropTable
            1 weight doubleRollHerbDropTable
        }
        3 weight SharedDropTables.rareDrop
        3 weight SharedDropTables.gem
        4 weight SharedDropTables.rareSeed
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/12 after unlocking the hard Combat Achievements rewards tier.
        10 outOf 128 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/114 after unlocking the elite Combat Achievements rewards tier.
        1 outOf 120 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
