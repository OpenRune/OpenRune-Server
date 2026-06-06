package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.shouldDropLootingBag
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val spiritualRangerDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Spiritual ranger Drops",
    npcs = npcs("npc.godwars_spiritual_armadyl_ranger", "npc.godwars_spiritual_bandos_ranger", "npc.godwars_spiritual_saradomin_ranger", "npc.godwars_spiritual_zamorak_ranger", "npc.nex_prison_ranger"),
    mainTable = rsPlayerWeightedTable(total = 129) {
        name("Spiritual ranger Drops")
        5 weight "obj.oak_shortbow" count 1
        3 weight "obj.xbows_crossbow_steel" count 1
        4 weight "obj.maple_longbow" count 1
        1 weight "obj.magic_shortbow" count 1..2
        11 weight "obj.mithril_arrow_p+" count 1
        4 weight "obj.steel_arrow" count 12
        15 weight "obj.iron_arrow" count 12
        13 weight "obj.bronze_arrow" count 16
        1 weight "obj.bronze_arrow_p+" count 1
        4 weight "obj.bodyrune" count 12
        2 weight "obj.rune_arrow" count 5
        1 weight "obj.adamant_arrow" count 12
        3 weight "obj.adamant_arrow_p++" count 3
        2 weight "obj.adamant_unlitarrow" count 4
        2 weight ringNothing()
        10 weight "obj.headless_arrow" count 12
        11 weight "obj.iron_arrowheads" count 5
        9 weight "obj.adamant_arrowheads" count 13
        18 weight "obj.bow_string" count 7
        1 weight "obj.digsitearrow" count 1
        9 weight "obj.unstrung_oak_longbow" count 12
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 3 weight "obj.looting_bag" count 1 condition { player ->
            // Drops Need Manual: Only dropped by those found in the Wilderness God Wars Dungeon.
             true
        }
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/121 after unlocking the hard Combat Achievements rewards tier.
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/64 if a ring of wealth (i) is worn and fought in the Wilderness.
        1 outOf 128 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
