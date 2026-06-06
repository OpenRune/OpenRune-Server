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
public val tzHaarMejDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "TzHaar-Mej Drops",
    npcs = npcs("npc.tzhaar_mej1", "npc.tzhaar_mej2", "npc.tzhaar_mej3", "npc.tzhaar_mej4", "npc.tzhaar_mej5", "npc.tzhaar_mej6", "npc.tzhaar_mej7"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("TzHaar-Mej Drops")
        4 weight "obj.earthrune" count 25
        4 weight "obj.firerune" count 25
        4 weight "obj.airrune" count 25
        2 weight "obj.chaosrune" count 5
        2 weight "obj.naturerune" count 2
        2 weight "obj.deathrune" count 2
        62 weight "obj.tzhaar_token" count 1..21
        2 weight "obj.tzhaar_token" count 22
        1 outOf 4096 separate rsPlayerWeightedTable {
            1 weight "obj.tzhaar_cape_obsidian" count 1
            1 weight "obj.tzhaar_staff" count 1
        }
        16 outOf 2048 separate "obj.uncut_sapphire" count 1
        8 outOf 2048 separate "obj.uncut_emerald" count 1
        4 outOf 2048 separate "obj.uncut_ruby" count 1
        1 outOf 2048 separate "obj.uncut_diamond" count 1
        707 outOf 2048 separate ringNothing()
        46 weight nothing()
    },
)
