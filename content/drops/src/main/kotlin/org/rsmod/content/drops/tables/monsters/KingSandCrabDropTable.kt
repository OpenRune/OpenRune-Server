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
public val kingSandCrabDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "King Sand Crab Drops",
    npcs = npcs("npc.kourend_rockcrab", "npc.kourend_rockcrab_inactive"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("King Sand Crab Drops")
        5 weight "obj.steel_platebody" count 1
        5 weight "obj.black_claws" count 1
        5 weight "obj.black_platebody" count 1
        10 weight "obj.mithril_axe" count 1
        2 weight "obj.dagganoth_melee_gloves" count 1
        2 weight "obj.dagganoth_melee_feet" count 1
        5 weight "obj.black_warhammer" count 1
        5 weight "obj.adamant_longsword" count 1
        10 weight "obj.waterrune" count 1..40
        10 weight "obj.earthrune" count 1..40
        10 weight "obj.coins" count 1..140
        10 weight "obj.raw_lobster" count 1
        10 weight "obj.raw_bass" count 1
        10 weight "obj.tinderbox" count 1
        2 weight "obj.casket" count 1
        2 weight "obj.dagganoth_melee_leg_part" count 1
        2 weight "obj.dagganoth_melee_head_part" count 1
        2 weight "obj.dagganoth_melee_body_part" count 1

        8 weight SharedDropTables.herb
        1 weight SharedDropTables.gem
        1 weight SharedDropTables.rareSeed
        11 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): The medium clue scroll drop rate increases to 1/121 after unlocking the medium Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
