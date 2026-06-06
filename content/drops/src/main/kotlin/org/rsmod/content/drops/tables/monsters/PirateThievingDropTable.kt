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
public val pirateThievingDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Pirate (Thieving) Drops",
    npcs = npcs("npc.pirate_pickpocketable_1", "npc.pirate_pickpocketable_2", "npc.pirate_pickpocketable_3", "npc.pirate_pickpocketable_4", "npc.pirate_pickpocketable_5"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Pirate (Thieving) Drops")
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
        1 outOf 6 weight "obj.motd_frag_3" count 1 condition { player ->
            // Drops Need Manual: The fragment will only drop if the Pirate is located on The Onyx Crest.
             true
        }
    },
)
