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
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val butterflyRayDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Butterfly ray Drops",
    npcs = npcs("npc.sailing_butterfly_ray"),
    preRoll = rsPlayerPrerollTable {
        1 outOf 45 weight "obj.ray_barbs" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 2980) {
        name("Butterfly ray Drops")
        72 weight "obj.coral_elkhorn_frag" count 1
        36 weight "obj.coral_pillar_frag" count 1
        12 weight "obj.coral_umbral_frag" count 1
        50 weight "obj.flax_seed" count 1
        30 weight "obj.hemp_seed" count 1
        20 weight "obj.cotton_seed" count 1
        15 weight "obj.camphor_seed" count 1
        4 weight "obj.ironwood_seed" count 1
        1 weight "obj.rosewood_seed" count 1
        6 outOf 149 separate rsPlayerWeightedTable {
            6 weight "obj.cert_bucket_sand" count 10..15
            6 weight "obj.cert_seaweed" count 10..15
        }

        39 weight SharedDropTables.herb
        6 weight SharedDropTables.combatHerb
        2695 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 10 weight "obj.sailing_butterfly_ray_skin" count 1 condition { player ->
            // Drops Need Manual: Only dropped while on an applicable bounty task.
             true
        }
        1 outOf 2 weight "obj.sailing_butterfly_ray_fin" count 1
        1 outOf 80 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_medium_emote_exp1")
        }
    },
)
