package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val jungleHorrorDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Jungle horror Drops",
    npcs = npcs("npc.harmless_island_jungle_horror_guard", "npc.harmless_island_jungle_horror_warrior", "npc.harmless_island_jungle_horror_worker", "npc.harmless_island_jungle_horror_young_warrior", "npc.harmless_island_jungle_horror_young_worker"),
    mainTable = rsPlayerWeightedTable(total = 129) {
        name("Jungle horror Drops")
        18 weight "obj.iron_warhammer" count 1
        5 weight "obj.iron_kiteshield" count 1
        4 weight "obj.iron_spear" count 1
        2 weight "obj.iron_javelin" count 5
        7 weight "obj.naturerune" count 2
        2 weight "obj.naturerune" count 3
        10 weight "obj.naturerune" count 4
        5 weight "obj.deathrune" count 1
        27 weight "obj.iron_ore" count 1
        9 weight "obj.cert_teak_logs" count 3
        3 weight "obj.mahogany_logs" count 1
        8 weight "obj.pineapple" count 2
        5 weight "obj.bones" count 1 condition { player ->
            // Drops Need Manual: Always dropped with big bones
             true
        }
        3 weight "obj.big_bones" count 1
        2 weight "obj.big_bones" count 3
        2 weight ringNothing()

        6 weight SharedDropTables.herb
        1 weight SharedDropTables.gem
        // Pool padding (F2P drops removed / subtable access missing from wiki parse)
        10 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 40 weight "obj.arceuus_corpse_horror" count 1
        // Drops Need Manual (rate): The medium clue scroll drop rate increases to 1/121 after unlocking the medium Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
