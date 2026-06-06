package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val eliteDarkWarriorDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Elite Dark Warrior Drops",
    npcs = npcs("npc.elite_dark_warrior_1", "npc.elite_dark_warrior_2"),
    mainTable = rsPlayerWeightedTable(total = 117) {
        name("Elite Dark Warrior Drops")
        16 weight "obj.lobster" count 1..2
        16 weight "obj.1doseprayerrestore" count 1
        16 weight "obj.1dose2attack" count 1
        16 weight "obj.1dose2strength" count 1
        7 weight "obj.meat_pie" count 1
        15 weight "obj.coins" count 22..100
        15 weight "obj.coins" count 153..250
        15 weight "obj.coins" count 677..1000
        1 weight "obj.diamond_ring" count 1
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Clue scroll (hard) [tertiary/Rare]
