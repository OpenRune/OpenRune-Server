package org.rsmod.content.drops.tables.thieving

import dtx.rs.RSDropTable
import dtx.rs.pickpockets
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.game.entity.Player

private val pirateMainTable = rsPlayerWeightedTable(total = 200) {
    name("Pirate (Pickpocket)")
    170 weight "obj.pickpocket_coin_pouch_pirate" count 1
    14 weight "obj.bronze_cannonball" count 1
    10 weight "obj.iron_cannonball" count 1
    5 weight "obj.mcannonball" count 1
    1 weight "obj.coral_elkhorn_frag" count 1
}

@field:RegisterDropTable
@JvmField
public val piratePickpocketDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Pirate (Pickpocket)",
    pickpockets = pickpockets("pirate"),
    mainTable = rsPlayerWeightedTable(total = 1000) {
        name("Pirate pre-roll (Pickpocket)")
        1 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
            player.clueScrollTransformObj("obj.trail_medium_emote_exp1")
        }
        999 weight pirateMainTable
    },
)
