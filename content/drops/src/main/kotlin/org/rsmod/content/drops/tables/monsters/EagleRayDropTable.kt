package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.rsPlayerPrerollTable
import org.rsmod.api.droptable.dropRollable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val eagleRayDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Eagle ray Drops",
    npcs = npcs("npc.sailing_eagle_ray"),
    preRoll = rsPlayerPrerollTable {
        1 outOf 50 weight "obj.ray_barbs" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 750) {
        name("Eagle ray Drops")
        6 weight "obj.coral_elkhorn_frag" count 1
        3 weight "obj.coral_pillar_frag" count 1
        1 weight "obj.coral_umbral_frag" count 1
        5 weight "obj.flax_seed" count 1
        3 weight "obj.hemp_seed" count 1
        2 weight "obj.cotton_seed" count 1
        14 outOf 150 separate rsPlayerWeightedTable {
            14 weight "obj.cert_bucket_sand" count 10..15
            14 weight "obj.cert_seaweed" count 10..15
        }

        35 weight SharedDropTables.herb
        3 weight SharedDropTables.combatHerb
        692 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 10 weight "obj.sailing_eagle_ray_skin" count 1 condition { player ->
            // Drops Need Manual: Only dropped while on an applicable bounty task.
             true
        }
        1 outOf 2 weight "obj.sailing_eagle_ray_fin" count 1
        // Drops Need Manual (rate): The easy clue scroll drop rate increases to 1/71 after unlocking the easy Combat Achievements rewards tier.
        1 outOf 75 weight "obj.trail_clue_easy_simple001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
