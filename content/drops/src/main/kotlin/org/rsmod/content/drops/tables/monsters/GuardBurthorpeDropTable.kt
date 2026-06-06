package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val guardBurthorpeDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Guard (Burthorpe) Drops",
    npcs = npcs("npc.death_guard1", "npc.death_guard2"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Guard (Burthorpe) Drops")
        8 weight "obj.steel_warhammer" count 1
        6 weight "obj.mithril_warhammer" count 1
        4 weight "obj.mithril_platebody" count 1
        1 weight "obj.mithril_full_helm" count 1
        1 weight "obj.bloodrune" count 3
        1 weight "obj.chaosrune" count 3
        1 weight "obj.naturerune" count 4
        2 weight "obj.firerune" count 22
        2 weight "obj.earthrune" count 23
        2 weight "obj.airrune" count 25
        3 weight "obj.bronze_arrow" count 12
        2 weight "obj.steel_arrow" count 12
        1 weight "obj.coins" count 2
        37 weight "obj.coins" count 4
        16 weight "obj.coins" count 12
        9 weight "obj.coins" count 18
        4 weight "obj.coins" count 32
        4 weight "obj.coins" count 45
        2 weight "obj.coins" count 50
        18 weight ringNothing()
        3 weight "obj.earth_talisman" count 1
        1 weight "obj.mithril_ore" count 1
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): The medium clue scroll drop rate increases to 1/121 after unlocking the medium Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
