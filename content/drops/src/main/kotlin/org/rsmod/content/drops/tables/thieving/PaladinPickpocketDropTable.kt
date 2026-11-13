package org.rsmod.content.drops.tables.thieving

import dtx.rs.RSDropTable
import dtx.rs.pickpockets
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val paladinPickpocketDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Paladin (Pickpocket)",
    pickpockets = pickpockets("paladin"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.pickpocket_coin_pouch_paladin" count 1
        "obj.chaosrune" count 2
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 500 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
    },
)
