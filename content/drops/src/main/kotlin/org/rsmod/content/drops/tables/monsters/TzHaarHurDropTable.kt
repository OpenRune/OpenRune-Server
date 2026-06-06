package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val tzHaarHurDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "TzHaar-Hur Drops",
    npcs = npcs("npc.tzhaar_hur1", "npc.tzhaar_hur2", "npc.tzhaar_hur3", "npc.tzhaar_hur4", "npc.tzhaar_hur5", "npc.tzhaar_hur6", "npc.tzhaar_hur_city1", "npc.tzhaar_hur_city2", "npc.tzhaar_hur_city3", "npc.tzhaar_hur_city4", "npc.tzhaar_hur_city5", "npc.tzhaar_hur_city6"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("TzHaar-Hur Drops")
        8 weight "obj.chisel" count 1
        8 weight "obj.hammer" count 1
        8 weight "obj.jug_empty" count 1
        8 weight "obj.knife" count 1
        8 weight "obj.pot_empty" count 1
        64 weight "obj.tzhaar_token" count 1..16
        16 outOf 2048 separate "obj.uncut_sapphire" count 1
        8 outOf 2048 separate "obj.uncut_emerald" count 1
        4 outOf 2048 separate "obj.uncut_ruby" count 1
        1 outOf 2048 separate "obj.uncut_diamond" count 1
        355 outOf 2048 separate ringNothing()
        24 weight nothing()
    },
)
