package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val towerArcherDropTableNorthLevel19: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Tower Archer North (level 19)",
    npcs = npcs("npc.guild_towerarcher1"),
    mainTable = rsPlayerWeightedTable(total = 100) {
        name("Tower Archer North (level 19)")
        14 weight "obj.iron_arrow" count 1
        86 weight nothing()
    },
)

@field:RegisterDropTable
@JvmField
public val towerArcherDropTableEastLevel34: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Tower Archer East (level 34)",
    npcs = npcs("npc.guild_towerarcher2"),
    mainTable = rsPlayerWeightedTable(total = 100) {
        name("Tower Archer East (level 34)")
        12 weight "obj.steel_arrow" count 1
        88 weight nothing()
    },
)

@field:RegisterDropTable
@JvmField
public val towerArcherDropTableSouthLevel49: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Tower Archer South (level 49)",
    npcs = npcs("npc.guild_towerarcher3"),
    mainTable = rsPlayerWeightedTable(total = 100) {
        name("Tower Archer South (level 49)")
        10 weight "obj.mithril_arrow" count 1
        90 weight nothing()
    },
)

@field:RegisterDropTable
@JvmField
public val towerArcherDropTableWestLevel64: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Tower Archer West (level 64)",
    npcs = npcs("npc.guild_towerarcher4"),
    mainTable = rsPlayerWeightedTable(total = 100) {
        name("Tower Archer West (level 64)")
        8 weight "obj.adamant_arrow" count 1
        92 weight nothing()
    },
)
