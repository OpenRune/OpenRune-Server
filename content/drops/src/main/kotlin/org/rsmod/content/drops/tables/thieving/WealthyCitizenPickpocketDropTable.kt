package org.rsmod.content.drops.tables.thieving

import dtx.rs.RSDropTable
import dtx.rs.pickpockets
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val wealthyCitizenPickpocketDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Wealthy Citizen (Pickpocket)",
    pickpockets = pickpockets("wealthy_citizen"),
    mainTable = rsPlayerWeightedTable(total = 85) {
        name("Wealthy Citizen (Pickpocket)")
        79 weight "obj.pickpocket_coin_pouch_varlamore_wealthy" count 1
        5 weight "obj.varlamore_thieving_house_key" count 1
        1 weight "obj.trail_clue_easy_simple001" count 1 transformObj { player ->
            player.clueScrollTransformObj("obj.trail_clue_easy_simple001")
        }
    },
)
