package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.hasCompletedQuest
import org.rsmod.content.drops.isOnQuest
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val marketGuardDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Market Guard Drops",
    npcs = npcs("npc.viking_guard"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Market Guard Drops")
        3 weight "obj.bronze_warhammer" count 1
        6 weight "obj.iron_warhammer" count 1
        1 weight "obj.steel_warhammer" count 1
        1 weight "obj.black_warhammer" count 1
        1 weight "obj.viking_helmet" count 1 condition {
            player -> player.hasCompletedQuest("quest_fremenniktrials")
        }
        1 weight "obj.viking_sword" count 1
        1 weight "obj.viking_shield" count 1
        42 weight "obj.coins" count 15
        5 weight "obj.coins" count 17
        9 weight "obj.coins" count 22
        3 weight "obj.coins" count 27
        1 weight "obj.bucket_water" count 1
        1 weight "obj.fur" count 1
        4 weight "obj.keg_of_beer" count 1
        2 weight "obj.copper_ore" count 5
        1 weight "obj.tin_ore" count 1
        3 weight "obj.iron_ore" count 2
        1 weight "obj.raw_sardine" count 1
        1 weight "obj.beer" count 1
        4 weight "obj.cake" count 1
        1 weight "obj.ring_mould" count 1

        2 weight SharedDropTables.herb
        1 weight SharedDropTables.gem
        33 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.trail_elite_riddle_key32" count 1 condition { player ->
            // Drops Need Manual: The key is only dropped when completing a medium clue scroll asking the player to kill a Rellekka marketplace guard.
             true
        }
        // Drops Need Manual (rate): The medium clue scroll drop rate increases to 1/121 after unlocking the medium Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
