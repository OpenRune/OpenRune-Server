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
import org.rsmod.content.drops.shouldDropBrimstoneKey
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val marbleGargoyleDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Marble gargoyle Drops",
    npcs = npcs("npc.superior_gargoyle", "npc.superior_gargoyle_dead"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.konar_key" count 1 killCondition {
            player, npc, areaChecker -> player.shouldDropBrimstoneKey(npc, areaChecker)
        }
    },
    preRoll = rsPlayerPrerollTable {
        1 outOf 256 weight "obj.granite_maul" count 1
        1 outOf 512 weight "obj.mystic_robe_top_dark" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Marble gargoyle Drops")
        4 weight "obj.adamant_platelegs" count 1
        3 weight "obj.rune_full_helm" count 1
        2 weight "obj.rune_2h_sword" count 1
        1 weight "obj.adamant_armoured_boots" count 1
        1 weight "obj.rune_battleaxe" count 1
        1 weight "obj.rune_platelegs" count 1
        10 weight "obj.firerune" count 75
        8 weight "obj.chaosrune" count 30
        6 weight "obj.firerune" count 150
        5 weight "obj.deathrune" count 15
        10 weight "obj.cert_gold_ore" count 10..20
        6 weight "obj.cert_blankrune_high" count 150
        6 weight "obj.cert_steel_bar" count 15
        3 weight "obj.cert_gold_bar" count 10..15
        2 weight "obj.cert_mithril_bar" count 15
        2 weight "obj.runite_ore" count 1
        28 weight "obj.coins" count 400..800
        20 weight "obj.coins" count 500..1000
        5 weight "obj.coins" count 10000

        5 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 150 weight "obj.slayer_roof_key" count 1 condition { player ->
            // Drops Need Manual: Brittle keys are only dropped if the player has not unlocked the roof access, does not have one in their bank or inventory, and has gargoyles as their current Slayer task.
             true
        }
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/12 after unlocking the hard Combat Achievements rewards tier.
        10 outOf 128 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
