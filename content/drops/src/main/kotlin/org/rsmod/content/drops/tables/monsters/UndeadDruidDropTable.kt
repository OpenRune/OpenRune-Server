package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val undeadDruidDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Undead Druid Drops",
    npcs = npcs("npc.hosdun_druid"),
    mainTable = rsPlayerWeightedTable(total = 100) {
        name("Undead Druid Drops")
        2 weight "obj.air_battlestaff" count 1
        2 weight "obj.earth_battlestaff" count 1
        5 weight "obj.airrune" count 200..300
        5 weight "obj.earthrune" count 200..300
        3 weight "obj.bloodrune" count 20..30
        3 weight "obj.chaosrune" count 50..80
        3 weight "obj.cosmicrune" count 20..30
        3 weight "obj.deathrune" count 20..30
        3 weight "obj.naturerune" count 20..30
        3 weight "obj.mudrune" count 30..70
        2 weight "obj.lawrune" count 10..20
        2 weight "obj.cert_eye_of_newt" count 25..30
        2 weight "obj.cert_cactus_potato" count 10..15
        2 weight "obj.cert_white_berries" count 10..15
        2 weight "obj.cert_wine_of_zamorak" count 5..8
        6 weight "obj.coins" count 1000..5000
        5 weight "obj.amulet_of_defence" count 1
        5 weight "obj.amulet_of_magic" count 1
        5 weight "obj.amulet_of_strength" count 1
        1 outOf 1000 separate "obj.hosdun_temple_mask" count 1

        22 weight SharedDropTables.herb
        1 weight SharedDropTables.gem
        2 weight SharedDropTables.rareSeed
        12 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): The drop rate to receive any page is 1/20. When rolled, the player will receive the page they have the fewest of.
        1 outOf 20 weight "obj.hosdun_moon_page" count 1
        1 outOf 20 weight "obj.hosdun_sun_page" count 1
        1 outOf 20 weight "obj.hosdun_temple_page" count 1
        1 outOf 75 weight "obj.hosdun_grubby_key" count 1
        1 outOf 5000 weight "obj.champions_challenge_zombie" count 1
        1 outOf 95 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
    },
)
