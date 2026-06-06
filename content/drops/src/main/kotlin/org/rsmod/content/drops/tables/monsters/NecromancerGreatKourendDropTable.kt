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
public val necromancerGreatKourendDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Necromancer (Great Kourend) Drops",
    npcs = npcs("npc.shayzien_necromancer"),
    mainTable = rsPlayerWeightedTable(total = 65) {
        name("Necromancer (Great Kourend) Drops")
        4 weight "obj.airrune" count 20..30
        4 weight "obj.waterrune" count 20..30
        4 weight "obj.earthrune" count 20..30
        4 weight "obj.firerune" count 20..30
        4 weight "obj.mindrune" count 20..30
        4 weight "obj.bodyrune" count 20..30
        3 weight "obj.cosmicrune" count 10..15
        3 weight "obj.chaosrune" count 10..15
        3 weight "obj.naturerune" count 10..15
        3 weight "obj.lawrune" count 10..15
        2 weight "obj.deathrune" count 4..8
        2 weight "obj.bloodrune" count 4..8
        6 weight "obj.coins" count 10..50
        2 weight "obj.plainstaff" count 1
        1 weight "obj.air_talisman" count 1
        1 weight "obj.water_talisman" count 1
        1 weight "obj.earth_talisman" count 1
        1 weight "obj.fire_talisman" count 1
        1 outOf 5 separate "obj.amulet_of_magic_cursed" count 1
        13 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): The medium clue scroll drop rate increases to 1/121 after unlocking the medium Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
