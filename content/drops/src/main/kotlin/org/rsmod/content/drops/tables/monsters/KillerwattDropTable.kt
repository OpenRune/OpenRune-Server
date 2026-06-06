package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val killerwattDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Killerwatt Drops",
    npcs = npcs("npc.slayer_killerwatt", "npc.slayer_killerwatt_ball"),
    mainTable = rsPlayerWeightedTable(total = 158) {
        name("Killerwatt Drops")
        2 weight "obj.staff_of_fire" count 1
        2 weight "obj.staff_of_air" count 1
        1 weight "obj.fire_battlestaff" count 1
        1 weight "obj.air_battlestaff" count 1
        3 weight "obj.airrune" count 5
        2 weight "obj.airrune" count 17
        5 weight "obj.firerune" count 18
        8 weight "obj.firerune" count 45
        11 weight "obj.naturerune" count 8
        1 weight "obj.naturerune" count 37
        9 weight "obj.chaosrune" count 4
        3 weight "obj.deathrune" count 2
        2 weight "obj.steamrune" count 2
        2 weight "obj.mcannonball" count 3
        23 weight "obj.coins" count 11
        28 weight "obj.coins" count 44
        1 weight "obj.coins" count 76
        1 weight "obj.coins" count 127
        11 weight "obj.coins" count 200
        1 weight "obj.fire_orb" count 2
        1 weight "obj.soda_ash" count 4
        1 outOf 512 separate "obj.mystic_fire_staff" count 1

        35 weight SharedDropTables.herb
        5 weight SharedDropTables.gem
    },
)
