package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.shouldDropLootingBag
import org.rsmod.content.drops.hasCompletedQuest
import org.rsmod.content.drops.isOnQuest
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val zombieWildernessDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Zombie (Wilderness) Drops",
    npcs = npcs("npc.wilderness_zombie", "npc.wilderness_zombie2", "npc.wilderness_zombie3", "npc.zombie2_rural1", "npc.zombie2_rural2", "npc.zombie2_rural3", "npc.zombie2_rural4", "npc.zombie_armed_rural1", "npc.zombie_armed_rural2", "npc.zombie_armed_rural3", "npc.zombie_armed_rural4", "npc.zombie_armed_rural5"),
    mainTable = rsPlayerWeightedTable(total = 211) {
        name("Zombie (Wilderness) Drops")
        2 weight "obj.bronze_kiteshield" count 1
        2 weight "obj.iron_dagger" count 1
        1 weight "obj.iron_mace" count 1
        6 weight "obj.cosmicrune" count 2
        5 weight "obj.chaosrune" count 4
        3 weight "obj.airrune" count 3
        3 weight "obj.bodyrune" count 3
        1 weight "obj.firerune" count 7
        19 weight "obj.coins" count 10
        5 weight "obj.coins" count 26
        4 weight "obj.coins" count 35
        2 weight "obj.coins" count 1
        1 weight "obj.coins" count 18
        42 weight "obj.fishing_bait" count 3 transformObj { player ->
            // Drops Need Manual (item): Fishing bait is replaced by dark fishing bait on members worlds.
             null
        }
        42 weight "obj.wilderness_fishing_bait" count 3
        25 weight "obj.fishing_bait" count 4
        25 weight "obj.wilderness_fishing_bait" count 4
        3 weight "obj.fishing_bait" count 7
        3 weight "obj.wilderness_fishing_bait" count 7
        2 weight "obj.eye_of_newt" count 1
        1 weight "obj.tinderbox" count 1

        13 weight SharedDropTables.herb
        1 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_zombie_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
        1 outOf 5000 weight "obj.champions_challenge_zombie" count 1
        1 outOf 3 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
    },
)
