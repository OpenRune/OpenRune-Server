package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.rsWeightedTable
import org.rsmod.api.droptable.ClueTier
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.clue
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.content.drops.tables.shared.doubleRollHerbDropTable
import org.rsmod.content.drops.tables.shared.tripleRollHerbDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val deviantSpectreDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Deviant spectre Drops",
    npcs = npcs("npc.kourend_spectre"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Deviant spectre Drops")
        5 weight "obj.battlestaff" count 1
        5 weight "obj.black_platelegs" count 1
        5 weight "obj.mithril_battleaxe" count 1
        2 weight "obj.rune_full_helm" count 1
        1 weight "obj.lava_battlestaff" count 1
        1 weight "obj.rune_chainbody" count 1
        5 weight "obj.adamantite_ore" count 1

        46 weight rsWeightedTable(total = 46) {
            29 weight doubleRollHerbDropTable
            17 weight tripleRollHerbDropTable
        }
        32 weight SharedDropTables.gem
        16 weight SharedDropTables.rareSeed
        10 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        1 outOf 512 weight "obj.mystic_robe_bottom_dark" count 1
        1 outOf 121 chance clue(ClueTier.Hard)
    },
)
