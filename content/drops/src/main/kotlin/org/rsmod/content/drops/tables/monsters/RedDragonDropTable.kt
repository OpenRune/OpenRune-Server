package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.content.drops.hasCompletedQuest
import org.rsmod.content.drops.isOnQuest
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val redDragonDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Red dragon Drops",
    npcs = npcs("npc.ds2_red_dragon", "npc.ds2_red_dragon_cutscene_flying", "npc.ds2_red_dragon_flying", "npc.red_dragon", "npc.red_dragon2", "npc.red_dragon3", "npc.red_dragon4", "npc.red_dragon5"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.dragonhide_red" count 1 condition { player ->
            // Drops Need Manual: Red dragonhide is dropped in bank note form if players have completed the Karamja Diary and are killing red dragons in the Brimhaven Dungeon.
             true
        }
    },
    mainTable = rsPlayerWeightedTable(total = 138) {
        name("Red dragon Drops")
        4 weight "obj.mithril_2h_sword" count 1
        3 weight "obj.mithril_axe" count 1
        3 weight "obj.mithril_battleaxe" count 1
        3 weight "obj.rune_dart" count 8
        1 weight "obj.mithril_javelin" count 20
        1 weight "obj.mithril_kiteshield" count 1
        1 weight "obj.adamant_platebody" count 1
        1 weight "obj.rune_longsword" count 1
        8 weight "obj.rune_arrow" count 4
        5 weight "obj.lawrune" count 4
        4 weight "obj.bloodrune" count 2
        3 weight "obj.deathrune" count 5
        40 weight "obj.coins" count 196
        29 weight "obj.coins" count 66
        10 weight "obj.coins" count 330 condition {
            player -> !player.hasCompletedQuest("quest_monkeymadness2")
        }
        1 weight "obj.coins" count 690
        10 weight "obj.dragon_javelin_head" count 10 condition {
            player -> player.hasCompletedQuest("quest_monkeymadness2")
        }
        3 weight "obj.chocolate_cake" count 3
        1 weight "obj.adamantite_bar" count 1

        2 weight SharedDropTables.herb
        5 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): The drop rate to receive any tattered page is 1/10. When rolled, the player will receive the page they have the fewest of.
        // Drops Need Manual (rate): Tattered pages and grubby keys are only dropped by those found in the Forthos Dungeon. The drop rate to receive any tattered page is 1/10. When rolled, the player will receive the page they have the fewest of.
        1 outOf 10 weight "obj.hosdun_moon_page" count 1 condition { player ->
            // Drops Need Manual: Tattered pages and grubby keys are only dropped by those found in the Forthos Dungeon.
             true
        }
        1 outOf 10 weight "obj.hosdun_sun_page" count 1
        1 outOf 10 weight "obj.hosdun_temple_page" count 1
        1 outOf 40 weight "obj.arceuus_corpse_dragon" count 1
        1 outOf 50 weight "obj.hosdun_grubby_key" count 1
        onBuilder { brimstoneKeyRoll() }
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/121 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
