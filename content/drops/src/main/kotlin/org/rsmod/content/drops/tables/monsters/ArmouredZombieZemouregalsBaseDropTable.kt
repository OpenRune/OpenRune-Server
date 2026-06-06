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
public val armouredZombieZemouregalsBaseDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Armoured zombie (Zemouregal's Base) Drops",
    npcs = npcs("npc.dov_armoured_zombie_melee_1", "npc.dov_armoured_zombie_melee_2", "npc.dov_armoured_zombie_melee_3", "npc.dov_armoured_zombie_melee_4", "npc.dov_armoured_zombie_melee_5", "npc.dov_armoured_zombie_ranged_1", "npc.dov_armoured_zombie_ranged_2", "npc.dov_armoured_zombie_ranged_3", "npc.dov_armoured_zombie_ranged_4", "npc.dov_armoured_zombie_ranged_5"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Armoured zombie (Zemouregal's Base) Drops")
        8 weight "obj.adamant_arrow" count 12
        4 weight "obj.bloodrune" count 4..10
        1 weight "obj.chaosrune" count 10..20
        3 weight "obj.cosmicrune" count 10..20
        1 weight "obj.deathrune" count 4..10
        2 weight "obj.naturerune" count 4..10
        12 weight "obj.cert_blankrune_high" count 20..50
        1 weight "obj.adamant_kiteshield" count 1
        3 weight "obj.adamant_mace" count 1
        3 weight "obj.coins" count 10..20
        31 weight "obj.coins" count 50..400
        1 weight "obj.cert_eye_of_newt" count 2..6
        1 weight "obj.fishing_bait" count 6
        6 weight "obj.cert_plank_oak" count 5
        5 weight "obj.cert_woodplank" count 10
        2 weight "obj.cert_plank_teak" count 2

        43 weight SharedDropTables.herb
        1 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_zombie_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
        1 outOf 800 weight "obj.zombie_axe_broken" count 1
        1 outOf 5000 weight "obj.champions_challenge_zombie" count 1
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/121 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
