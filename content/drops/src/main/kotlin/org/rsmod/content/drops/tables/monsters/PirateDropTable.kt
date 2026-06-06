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
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val pirateDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Pirate Drops",
    npcs = npcs("npc.fever_pirate_enemy_01", "npc.fever_pirate_enemy_02", "npc.fever_pirate_enemy_03", "npc.fever_pirate_enemy_04", "npc.fever_pirate_enemy_05", "npc.fever_pirate_enemy_06", "npc.fever_pirate_enemy_07", "npc.fever_pirate_enemy_08", "npc.fever_pirate_enemy_09", "npc.fever_pirate_enemy_10", "npc.jail_pirate", "npc.lady_pirate", "npc.pirate1", "npc.pirate2", "npc.pirate_aggressive", "npc.zeah_pirate1", "npc.zeah_pirate2", "npc.zeah_pirate3"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Pirate Drops")
        6 weight "obj.iron_dagger" count 1
        4 weight "obj.bronze_scimitar" count 1
        1 weight "obj.iron_platebody" count 1
        10 weight "obj.xbows_crossbow_bolts_iron" count 2..12
        6 weight "obj.chaosrune" count 2
        5 weight "obj.naturerune" count 2
        3 weight "obj.bronze_arrow" count 9
        2 weight "obj.bronze_arrow" count 12
        2 weight "obj.airrune" count 10
        2 weight "obj.earthrune" count 9
        2 weight "obj.firerune" count 5
        1 weight "obj.lawrune" count 2
        29 weight "obj.coins" count 4
        13 weight "obj.coins" count 25
        8 weight "obj.coins" count 7
        6 weight "obj.coins" count 12
        4 weight "obj.coins" count 35
        1 weight "obj.coins" count 55
        12 weight "obj.eye_patch" count 1
        8 weight ringNothing()
        1 weight "obj.chefs_hat" count 1
        1 weight "obj.iron_bar" count 1

        1 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.trail_elite_riddle_key32" count 1 condition { player ->
            // Drops Need Manual: Only dropped when completing a medium clue scroll asking to kill a pirate.
             true
        }
        1 outOf 6 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
    },
)
