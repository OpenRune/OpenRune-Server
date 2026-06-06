package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val salarinTheTwistedDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Salarin the twisted Drops",
    npcs = npcs("npc.salarin_the_twisted"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Salarin the twisted Drops")
        1 weight "obj.black_dagger" count 1
        5 weight "obj.waterrune" count 12
        4 weight "obj.lawrune" count 2
        1 weight "obj.naturerune" count 3
        1 weight "obj.firerune" count 36
        5 weight "obj.white_berries" count 1
        1 weight "obj.snape_grass" count 1
        1 weight "obj.vial_water" count 1
        5 weight "obj.coins" count 3
        3 weight "obj.coins" count 24
        3 weight "obj.coins" count 10
        37 weight ringNothing()
        11 weight "obj.1dose2defense" count 1
        10 weight "obj.sinister_key" count 1

        39 weight SharedDropTables.herb
        1 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/475 after unlocking the elite Combat Achievements rewards tier.
        1 outOf 500 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
