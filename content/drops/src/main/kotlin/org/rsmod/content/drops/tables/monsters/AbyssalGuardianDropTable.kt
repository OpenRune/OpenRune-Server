package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val abyssalGuardianDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Abyssal guardian Drops",
    npcs = npcs("npc.abyssal_pyramid"),
    mainTable = rsPlayerWeightedTable(total = 256) {
        name("Abyssal guardian Drops")
        5 weight "obj.air_talisman" count 1
        5 weight "obj.water_talisman" count 1
        5 weight "obj.earth_talisman" count 1
        5 weight "obj.fire_talisman" count 1
        5 weight "obj.mind_talisman" count 1
        5 weight "obj.body_talisman" count 1
        2 weight "obj.cosmic_talisman" count 1
        2 weight "obj.chaos_talisman" count 1
        2 weight "obj.nature_talisman" count 1
        1 weight "obj.elemental_talisman" count 1
        36 weight "obj.cert_blankrune_high" count 1
        34 weight "obj.blankrune_high" count 1
        4 weight "obj.cert_blankrune_high" count 5
        4 weight "obj.cert_blankrune_high" count 10
        4 weight "obj.cert_blankrune_high" count 15
        3 weight "obj.magic_emerald_necklace" count 1
        1 outOf 42 separate rsPlayerWeightedTable {
            1 weight "obj.rcu_pouch_small" count 1 condition { player ->
                // Drops Need Manual: Players will only get a pouch if they have all the smaller pouches, and if a player has them all, they will no longer get them as a drop, unless one was lost.
                 true
            }
            1 weight "obj.rcu_pouch_medium" count 1
            1 weight "obj.rcu_pouch_large" count 1
            1 weight "obj.rcu_pouch_giant" count 1
        }
        134 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): The medium clue scroll drop rate increases to 1/243 after unlocking the medium Combat Achievements rewards tier.
        1 outOf 256 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
