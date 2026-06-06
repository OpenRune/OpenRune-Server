package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import dtx.rs.rsWeightedTable
import org.rsmod.content.drops.tables.shared.doubleRollHerbDropTable
import org.rsmod.content.drops.tables.shared.herbDropTable
import org.rsmod.content.drops.tables.shared.tripleRollHerbDropTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val forgottenSoulDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Forgotten Soul Drops",
    npcs = npcs("npc.sw_tower_ghost1", "npc.sw_tower_ghost2"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Forgotten Soul Drops")
        1 weight "obj.steel_longsword" count 1
        1 weight "obj.earth_battlestaff" count 1
        1 weight "obj.fire_battlestaff" count 1
        1 weight "obj.air_battlestaff" count 1
        1 weight "obj.water_battlestaff" count 1
        1 weight "obj.black_robe" count 1
        1 weight "obj.black_skirt" count 1
        1 weight "obj.blackwizhat" count 1
        7 weight "obj.lawrune" count 2..4
        2 weight "obj.xbows_crossbow_bolts_mithril" count 12..24
        5 weight "obj.mindrune" count 12..32
        4 weight "obj.airrune" count 30..50
        2 weight "obj.earthrune" count 10..20
        2 weight "obj.waterrune" count 10..30
        2 weight "obj.bodyrune" count 10..30
        1 weight "obj.naturerune" count 3..5
        5 weight "obj.coins" count 3
        5 weight "obj.coins" count 5
        20 weight "obj.coins" count 40..140
        10 weight "obj.cert_vial_water" count 2..4
        8 weight "obj.net" count 1
        1 weight "obj.ball_of_wool" count 1
        1 weight "obj.snape_grass" count 1

        44 weight rsWeightedTable(total = 44) {
            35 weight herbDropTable
            9 weight doubleRollHerbDropTable
        }
        1 weight nothing()
    },
)
