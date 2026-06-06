package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.rsPlayerPrerollTable
import org.rsmod.api.droptable.dropRollable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val sulphurNaguaDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Sulphur Nagua Drops",
    npcs = npcs("npc.pmoon_sulphur_nagua"),
    preRoll = rsPlayerPrerollTable {
        1 outOf 450 weight "obj.sulphur_blades" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Sulphur Nagua Drops")
        4 weight "obj.chaosrune" count 40..60
        1 weight "obj.deathrune" count 40..60
        26 weight "obj.firerune" count 10..40
        1 weight "obj.firerune" count 40..60
        4 weight "obj.naturerune" count 5..10
        4 weight "obj.sulphurous_essence" count 6..10
        13 weight "obj.cert_coal" count 5..10
        21 weight "obj.coal" count 1
        3 weight "obj.cert_copper_ore" count 10..15
        27 weight "obj.cert_iron_ore" count 5..10
        12 weight "obj.cert_silver_ore" count 5..10
        5 weight "obj.cert_tin_ore" count 10..15
        1 weight "obj.cert_mithril_ore" count 5..10

        3 weight SharedDropTables.gem
        3 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 256 weight "obj.trail_clue_hard_map001" count 1
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
    },
)
