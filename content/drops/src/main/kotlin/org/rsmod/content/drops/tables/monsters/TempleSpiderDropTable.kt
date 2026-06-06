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
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val templeSpiderDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Temple Spider Drops",
    npcs = npcs("npc.hosdun_spider"),
    mainTable = rsPlayerWeightedTable(total = 100) {
        name("Temple Spider Drops")
        4 weight "obj.adamant_longsword" count 1
        4 weight "obj.adamant_med_helm" count 1
        1 weight "obj.rune_dagger" count 1
        1 weight "obj.rune_med_helm" count 1
        5 weight "obj.airrune" count 30..50
        5 weight "obj.earthrune" count 30..50
        5 weight "obj.firerune" count 30..50
        5 weight "obj.waterrune" count 30..50
        2 weight "obj.chaosrune" count 10..15
        2 weight "obj.cosmicrune" count 10..15
        2 weight "obj.deathrune" count 10..15
        2 weight "obj.naturerune" count 10..15
        1 weight "obj.lawrune" count 4..5
        1 weight "obj.soulrune" count 5
        17 weight "obj.coins" count 400..600
        8 weight "obj.cert_red_spiders_eggs" count 3..8
        8 weight "obj.2dose2antipoison" count 1
        2 weight "obj.weapon_poison+" count 1

        15 weight SharedDropTables.herb
        1 weight SharedDropTables.gem
        9 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): The drop rate to receive any page is 1/30. When rolled, the player will receive the page they have the fewest of.
        1 outOf 30 weight "obj.hosdun_moon_page" count 1
        1 outOf 30 weight "obj.hosdun_sun_page" count 1
        1 outOf 30 weight "obj.hosdun_temple_page" count 1
        1 outOf 100 weight "obj.hosdun_grubby_key" count 1
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/190 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 200 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
