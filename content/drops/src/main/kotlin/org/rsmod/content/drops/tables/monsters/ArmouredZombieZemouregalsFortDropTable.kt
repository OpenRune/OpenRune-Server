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
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val armouredZombieZemouregalsFortDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Armoured zombie (Zemouregal's Fort) Drops",
    npcs = npcs("npc.coa_armoured_zombie_melee_1", "npc.coa_armoured_zombie_melee_2", "npc.coa_armoured_zombie_melee_3", "npc.coa_armoured_zombie_melee_4", "npc.coa_armoured_zombie_melee_5", "npc.coa_armoured_zombie_ranged_1", "npc.coa_armoured_zombie_ranged_2", "npc.coa_armoured_zombie_ranged_3", "npc.coa_armoured_zombie_ranged_4", "npc.coa_armoured_zombie_ranged_5"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Armoured zombie (Zemouregal's Fort) Drops")
        3 weight "obj.cosmicrune" count 15..30
        2 weight "obj.naturerune" count 6..16
        1 weight "obj.deathrune" count 6..14
        1 weight "obj.chaosrune" count 15..30
        4 weight "obj.bloodrune" count 6..14
        11 weight "obj.cert_blankrune_high" count 20..50
        8 weight "obj.rune_arrow" count 12
        3 weight "obj.coins" count 20..30
        30 weight "obj.coins" count 200..600
        5 weight "obj.cert_woodplank" count 12
        6 weight "obj.cert_plank_oak" count 6
        2 weight "obj.cert_plank_teak" count 3
        1 weight "obj.cert_eye_of_newt" count 4..8
        3 weight "obj.rune_mace" count 1
        1 weight "obj.rune_kiteshield" count 1
        1 weight "obj.fishing_bait" count 6

        45 weight SharedDropTables.herb
        1 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_zombie_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
        1 outOf 600 weight "obj.zombie_helmet_broken" count 1
        1 outOf 600 weight "obj.zombie_axe_broken" count 1
        1 outOf 5000 weight "obj.champions_challenge_zombie" count 1
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/95 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 100 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
