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
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val feralVampyreDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Feral Vampyre Drops",
    npcs = npcs("npc.burgh_vampire_juve_angry", "npc.godwars_ancient_vampire", "npc.myq3_angry_juvenile", "npc.myq3_angry_juvinate", "npc.trek_vampire_juve_angry_1", "npc.trek_vampire_juve_angry_2", "npc.trek_vampire_juve_angry_3", "npc.vampire_flyer", "npc.vampire_juve"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Feral Vampyre Drops")
        10 weight "obj.earthrune" count 4
        10 weight "obj.deathrune" count 2
        8 weight "obj.chaosrune" count 3
        5 weight "obj.bloodrune" count 1
        1 weight "obj.bloodrune" count 2
        40 weight "obj.coins" count 15
        16 weight ringNothing()
        3 weight "obj.black_axe" count 1
        2 weight "obj.earth_talisman" count 1

        10 weight SharedDropTables.herb
        4 weight SharedDropTables.gem
        // Pool padding (F2P drops removed / subtable access missing from wiki parse)
        19 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 3 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
        // Drops Need Manual (rate): The medium clue scroll drop rate increases to 1/121 after unlocking the medium Combat Achievements rewards tier.
        // Drops Need Manual (rate): The medium clue scroll drop rate increases to 1/64 if a ring of wealth (i) is worn and fought in the Wilderness.
        1 outOf 128 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
