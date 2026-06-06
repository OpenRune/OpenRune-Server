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
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val brassicanMageDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Brassican Mage Drops",
    npcs = npcs("npc.trail_cabbage_mage"),
    mainTable = rsPlayerWeightedTable(total = 50) {
        name("Brassican Mage Drops")
        1 weight "obj.staff_of_earth" count 1
        1 weight "obj.earth_battlestaff" count 1
        5 weight "obj.airrune" count 5..24
        5 weight "obj.earthrune" count 5..24
        5 weight "obj.deathrune" count 5..24
        5 weight "obj.cabbage" count 10..19
        10 weight "obj.coins" count 50..249
        7 weight "obj.cert_blankrune_high" count 25
        1 weight "obj.uncut_sapphire" count 1
        2 weight "obj.4doseprayerrestore" count 1

        5 weight SharedDropTables.herb
        3 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 3 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
    },
)
