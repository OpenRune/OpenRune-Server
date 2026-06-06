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
public val ancientWizardDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Ancient Wizard Drops",
    npcs = npcs("npc.trail_zaros_melee", "npc.trail_zaros_range", "npc.trail_zaros_wizard"),
    mainTable = rsPlayerWeightedTable(total = 50) {
        name("Ancient Wizard Drops")
        1 weight "obj.fire_battlestaff" count 1
        1 weight "obj.staff_of_fire" count 1
        5 weight "obj.airrune" count 5..24
        5 weight "obj.firerune" count 5..24
        5 weight "obj.deathrune" count 5..24
        5 weight "obj.xbows_crossbow_bolts_runite" count 1..5
        10 weight "obj.coins" count 50..249
        2 weight "obj.4doseprayerrestore" count 1
        7 weight "obj.cert_blankrune_high" count 25
        1 weight "obj.xbows_crossbow_unstrung_runite" count 1

        5 weight SharedDropTables.herb
        3 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 3 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
    },
)
