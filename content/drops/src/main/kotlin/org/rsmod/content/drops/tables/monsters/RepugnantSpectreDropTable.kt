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
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val repugnantSpectreDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Repugnant spectre Drops",
    npcs = npcs("npc.superior_kourend_spectre"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.konar_key" count 1 killCondition {
            player, npc, areaChecker -> player.shouldDropBrimstoneKey(npc, areaChecker)
        }
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Repugnant spectre Drops")
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
        1 outOf 512 weight "obj.mystic_robe_bottom_dark" count 1
        10 outOf 121 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
    },
)
