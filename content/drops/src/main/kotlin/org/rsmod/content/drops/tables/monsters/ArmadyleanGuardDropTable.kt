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
public val armadyleanGuardDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Armadylean guard Drops",
    npcs = npcs("npc.elite_npc_1"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Armadylean guard Drops")
        1 weight "obj.rune_spear" count 1
        3 weight "obj.rune_thrownaxe" count 6
        5 weight "obj.naturerune" count 20
        5 weight "obj.bloodrune" count 20
        5 weight "obj.cosmicrune" count 20
        37 weight "obj.coins" count 35
        12 weight "obj.coins" count 350
        5 weight "obj.hammer" count 1
        7 weight "obj.coal" count 1
        5 weight "obj.feather" count 30
        10 weight "obj.headless_arrow" count 30
        1 weight "obj.cert_adamantite_bar" count 5
        3 weight "obj.shark" count 1
        1 outOf 2000000 separate rsPlayerWeightedTable {
            1 weight "obj.armadyl_helmet" count 1
            1 weight "obj.bandos_boots" count 1
        }

        10 weight SharedDropTables.herb
        9 weight SharedDropTables.gem
        10 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 3 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
    },
)
