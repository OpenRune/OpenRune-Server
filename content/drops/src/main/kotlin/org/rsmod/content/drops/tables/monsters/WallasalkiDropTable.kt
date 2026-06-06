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
public val wallasalkiDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Wallasalki Drops",
    npcs = npcs("npc.dungeon_dagganoth_magic_monster", "npc.dungeon_dagganoth_magic_monster_deeper"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Wallasalki Drops")
        2 weight "obj.dagganoth_mage_gloves" count 1
        2 weight "obj.dagganoth_mage_feet" count 1
        10 weight "obj.airrune" count 1..10
        10 weight "obj.mindrune" count 1..10
        10 weight "obj.waterrune" count 1..10
        10 weight "obj.earthrune" count 1..10
        10 weight "obj.chaosrune" count 1..10
        10 weight "obj.deathrune" count 1..10
        10 weight "obj.bloodrune" count 1..5
        10 weight "obj.steel_arrow" count 1..10
        10 weight "obj.cert_blankrune_high" count 9
        2 weight "obj.dagganoth_mage_leg_part" count 1
        2 weight "obj.dagganoth_mage_body_part" count 1
        2 weight "obj.dagganoth_mage_head_part" count 1
        2 weight "obj.raw_herring" count 1..2
        3 weight "obj.raw_mackerel" count 1..2
        2 weight "obj.raw_tuna" count 1..2

        10 weight SharedDropTables.herb
        1 weight SharedDropTables.gem
        1 weight SharedDropTables.rareSeed
        9 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): The medium clue scroll drop rate increases to 1/121 after unlocking the medium Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
