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
public val giantSeaSnakeDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Giant Sea Snake Drops",
    npcs = npcs("npc.royal_sea_snake_mother_smaller"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Giant Sea Snake Drops")
        2 weight "obj.waterrune" count 15
        2 weight "obj.mistrune" count 1
        2 weight "obj.slayer_broad_arrows" count 4
        41 weight "obj.coins" count 44
        11 weight "obj.coins" count 32
        9 weight "obj.coins" count 24
        7 weight "obj.coins" count 23
        10 weight "obj.adamant_dart_tip" count 2
        4 weight "obj.fishing_bait" count 50
        4 weight "obj.pearl_bolttips" count 3
        3 weight "obj.water_orb" count 1
        4 weight "obj.raw_bass" count 2
        2 weight "obj.raw_lobster" count 1
        2 weight "obj.seaweed" count 5
        2 weight "obj.edible_seaweed" count 5
        2 weight "obj.casket" count 1
        1 weight "obj.smalloysterpearls" count 2
        1 weight "obj.bigoysterpearls" count 1

        5 weight SharedDropTables.herb
        1 weight SharedDropTables.gem
        13 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.royal_box" count 1
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
        // Drops Need Manual (rate): The medium clue scroll drop rate increases to 1/121 after unlocking the medium Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
