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
public val greaterAbyssalDemonDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Greater abyssal demon Drops",
    npcs = npcs("npc.superior_abyssal_demon"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.arceuus_corpse_abyssal" count 1
        "obj.konar_key" count 1 killCondition {
            player, npc, areaChecker -> player.shouldDropBrimstoneKey(npc, areaChecker)
        }
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Greater abyssal demon Drops")
        4 weight "obj.black_sword" count 1
        3 weight "obj.steel_battleaxe" count 1
        2 weight "obj.black_axe" count 1
        1 weight "obj.mithril_kiteshield" count 1
        1 weight "obj.rune_chainbody" count 1
        1 weight "obj.rune_med_helm" count 1
        35 weight "obj.coins" count 132
        9 weight "obj.coins" count 220
        7 weight "obj.coins" count 30
        6 weight "obj.coins" count 44
        1 weight "obj.coins" count 460
        8 weight "obj.airrune" count 50
        6 weight "obj.chaosrune" count 10
        5 weight "obj.cert_blankrune_high" count 60
        4 weight "obj.bloodrune" count 7
        2 weight "obj.lobster" count 1
        2 weight "obj.adamantite_bar" count 1
        1 weight "obj.lawrune" count 3
        1 weight "obj.cosmic_talisman" count 1
        1 weight "obj.chaos_talisman" count 1
        1 weight "obj.3dose1defense" count 1
        1 outOf 512 separate "obj.abyssal_whip" count 1
        1 outOf 32000 separate "obj.abyssal_dagger" count 1

        19 weight SharedDropTables.herb
        2 weight SharedDropTables.rareDrop
        5 weight SharedDropTables.gem
        1 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 6000 weight "obj.poh_trophydrop_abyssaldemon" count 1
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
