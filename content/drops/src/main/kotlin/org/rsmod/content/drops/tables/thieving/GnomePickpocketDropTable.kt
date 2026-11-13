package org.rsmod.content.drops.tables.thieving

import dtx.rs.RSDropTable
import dtx.rs.pickpockets
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val gnomePickpocketDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Gnome (Pickpocket)",
    pickpockets = pickpockets("gnome"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Gnome (Pickpocket)")
        56 weight "obj.arrow_shaft" count 2..4
        30 weight "obj.pickpocket_coin_pouch_gnome" count 1
        24 weight "obj.swamp_toad" count 1
        8 weight "obj.gold_ore" count 1
        5 weight "obj.earthrune" count 1
        3 weight "obj.king_worm" count 1
        2 weight "obj.fire_orb" count 1
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 150 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
            player.clueScrollTransformObj("obj.trail_medium_emote_exp1")
        }
    },
)
