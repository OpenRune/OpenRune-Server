package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val zombieStrongholdOfSecurityDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Zombie (Stronghold of Security) Drops",
    npcs = npcs("npc.sos_fam_zombie2", "npc.sos_fam_zombie2_b", "npc.sos_fam_zombie2_c", "npc.sos_fam_zombie_armed", "npc.sos_fam_zombie_armed2", "npc.sos_fam_zombie_armed3", "npc.sos_fam_zombie_unarmed", "npc.sos_fam_zombie_unarmed2", "npc.sos_fam_zombie_unarmed3"),
    mainTable = rsPlayerWeightedTable(total = 100) {
        name("Zombie (Stronghold of Security) Drops")
        20 weight "obj.steel_arrow" count 5..14
        10 weight "obj.bodyrune" count 7
        10 weight "obj.coins" count 5..84
        10 weight "obj.beer" count 1
        4 weight "obj.ashes" count 1
        40 weight ringNothing()

        4 weight SharedDropTables.herb
        2 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 5000 weight "obj.champions_challenge_zombie" count 1
    },
)
