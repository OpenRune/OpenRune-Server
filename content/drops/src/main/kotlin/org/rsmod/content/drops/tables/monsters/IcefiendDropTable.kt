package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.shouldDropLootingBag
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val icefiendDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Icefiend Drops",
    npcs = npcs("npc.godwars_icefiend_1", "npc.slayer_icefiend_1"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Icefiend Drops")
        1 weight "obj.airrune" count 1..5
        4 weight "obj.waterrune" count 1..5
        112 weight ringNothing()
        4 weight "obj.coins" count 1..30
        4 weight "obj.coins" count 1..20

        3 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 8 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
        1 outOf 1373 weight "obj.varlamore_key_half_1" count 1
        1 outOf 150 weight "obj.frozen_tear" count 5 condition { player ->
            // Drops Need Manual: Only dropped in Ruins of Tapoyauik
             true
        }
        // Drops Need Manual (rate): The easy clue scroll drop rate increases to 1/121 after unlocking the easy Combat Achievements rewards tier.
        // Drops Need Manual (rate): The easy clue scroll drop rate inceases to 1/64 if a Ring of wealth (i) is worn and fought in the Wilderness.
        1 outOf 128 weight "obj.trail_clue_easy_simple001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
