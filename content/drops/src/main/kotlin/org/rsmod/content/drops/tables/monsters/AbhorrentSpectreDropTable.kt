package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import dtx.rs.rsWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.content.drops.tables.shared.doubleRollHerbDropTable
import org.rsmod.content.drops.tables.shared.herbDropTable
import org.rsmod.content.drops.tables.shared.tripleRollHerbDropTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.shouldDropBrimstoneKey
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val abhorrentSpectreDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Abhorrent spectre Drops",
    npcs = npcs("npc.superior_abberant_spectre"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.konar_key" count 1 killCondition {
            player, npc, areaChecker -> player.shouldDropBrimstoneKey(npc, areaChecker)
        }
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Abhorrent spectre Drops")
        3 weight "obj.steel_axe" count 1
        1 weight "obj.mithril_kiteshield" count 1
        1 weight "obj.lava_battlestaff" count 1
        1 weight "obj.adamant_platelegs" count 1
        1 weight "obj.rune_full_helm" count 1
        1 weight "obj.coins" count 460
        18 weight ringNothing()
        1 outOf 512 separate "obj.mystic_robe_bottom_dark" count 1

        78 weight rsWeightedTable(total = 26) {
            name("Multi-roll herb drop table")
            11 weight herbDropTable
            11 weight doubleRollHerbDropTable
            4 weight tripleRollHerbDropTable
        }
        5 weight SharedDropTables.gem
        19 weight SharedDropTables.rareSeed
    },
    tertiaries = rsPlayerTertiaryTable {
        10 outOf 121 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
    },
)
