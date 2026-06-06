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
public val cockathriceDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Cockathrice Drops",
    npcs = npcs("npc.superior_cockatrice"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Cockathrice Drops")
        3 weight "obj.iron_sword" count 1
        3 weight "obj.steel_dagger" count 1
        1 weight "obj.iron_armoured_boots" count 1
        1 weight "obj.iron_javelin" count 5
        1 weight "obj.steel_longsword" count 1
        6 weight "obj.naturerune" count 2
        4 weight "obj.naturerune" count 4
        2 weight "obj.naturerune" count 6
        3 weight "obj.lawrune" count 2
        2 weight "obj.waterrune" count 2
        2 weight "obj.firerune" count 7
        16 weight "obj.coins" count 15
        12 weight "obj.coins" count 5
        12 weight "obj.coins" count 28
        4 weight "obj.coins" count 62
        3 weight "obj.coins" count 42
        1 weight "obj.coins" count 1
        21 weight "obj.limpwurt_root" count 1
        1 weight ringNothing()
        1 outOf 512 separate "obj.mystic_boots_light" count 1

        10 weight SharedDropTables.herb
        2 weight SharedDropTables.gem
        18 weight SharedDropTables.seed
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1000 weight "obj.poh_trophydrop_cockatrice" count 1
        // Drops Need Manual (rate): The medium clue scroll drop rate increases to 1/12 after unlocking the medium Combat Achievements rewards tier.
        10 outOf 128 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
