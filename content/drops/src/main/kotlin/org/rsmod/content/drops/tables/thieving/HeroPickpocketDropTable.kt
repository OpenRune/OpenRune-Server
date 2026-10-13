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
public val heroPickpocketDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Hero (Pickpocket)",
    pickpockets = pickpockets("hero"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Hero (Pickpocket)")
        105 weight "obj.pickpocket_coin_pouch_hero" count 1
        8 weight "obj.deathrune" count 2
        6 weight "obj.jug_wine" count 1
        5 weight "obj.bloodrune" count 1
        2 weight "obj.fire_orb" count 1
        1 weight "obj.diamond" count 1
        1 weight "obj.gold_ore" count 1
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 900 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            player.clueScrollTransformObj("obj.trail_elite_emote_exp1")
        }
    },
)
