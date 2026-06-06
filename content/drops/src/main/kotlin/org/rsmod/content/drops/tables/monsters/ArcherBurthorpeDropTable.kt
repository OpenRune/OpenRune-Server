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
public val archerBurthorpeDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Archer (Burthorpe) Drops",
    npcs = npcs("npc.death_archer1", "npc.death_archer2", "npc.death_archer_trapped"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Archer (Burthorpe) Drops")
        4 weight "obj.willow_longbow" count 1
        8 weight "obj.studded_chaps" count 1
        6 weight "obj.dragonhide_chaps" count 1
        1 weight "obj.maple_shortbow" count 1
        2 weight "obj.earthrune" count 18
        3 weight "obj.bronze_arrow" count 14
        2 weight "obj.steel_arrow" count 14
        2 weight "obj.firerune" count 17
        2 weight "obj.airrune" count 29
        1 weight "obj.naturerune" count 6
        1 weight "obj.bloodrune" count 1
        1 weight "obj.chaosrune" count 2
        1 weight "obj.coins" count 3
        37 weight "obj.coins" count 5
        16 weight "obj.coins" count 15
        9 weight "obj.coins" count 12
        4 weight "obj.coins" count 28
        4 weight "obj.coins" count 42
        2 weight "obj.coins" count 46
        3 weight "obj.earth_talisman" count 1
        1 weight "obj.mithril_ore" count 1
        18 weight ringNothing()
    },
)
