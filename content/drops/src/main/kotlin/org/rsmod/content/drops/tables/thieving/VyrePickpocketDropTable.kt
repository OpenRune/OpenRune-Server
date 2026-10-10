package org.rsmod.content.drops.tables.thieving

import dtx.rs.RSDropTable
import dtx.rs.pickpockets
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val vyrePickpocketDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Vyre (Pickpocket)",
    pickpockets = pickpockets("vyre"),
    mainTable = rsPlayerWeightedTable(total = 132) {
        name("Vyre (Pickpocket)")
        109 weight "obj.pickpocket_coin_pouch_vyre" count 1
        8 weight "obj.deathrune" count 2
        6 weight "obj.blood_pint" count 1
        5 weight "obj.uncut_ruby" count 1
        2 weight "obj.bloodrune" count 4
        1 weight "obj.diamond" count 1
        1 weight "obj.cooked_mystery_meat" count 1
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 5000 weight "obj.blood_shard" count 1
    },
)
