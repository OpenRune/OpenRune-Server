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
public val hamGuardPickpocketDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "H.A.M. Guard (Pickpocket)",
    pickpockets = pickpockets("ham_guard"),
    mainTable = rsPlayerWeightedTable(total = 500) {
        name("H.A.M. Guard (Pickpocket)")
        50 weight "obj.dttd_key_1" count 1
        50 weight "obj.dttd_key_2" count 1
        50 weight "obj.dttd_key_3" count 1
        50 weight "obj.dttd_key_4" count 1
        45 weight "obj.pickpocket_coin_pouch_ham" count 1
        9 weight "obj.bronze_arrow" count 1..13
        9 weight "obj.bronze_axe" count 1
        9 weight "obj.bronze_dagger" count 1
        9 weight "obj.bronze_pickaxe" count 1
        9 weight "obj.iron_axe" count 1
        9 weight "obj.iron_dagger" count 1
        9 weight "obj.iron_pickaxe" count 1
        9 weight "obj.leather_armour" count 1
        3 weight "obj.ham_boots" count 1
        3 weight "obj.ham_cloak" count 1
        3 weight "obj.ham_gloves" count 1
        3 weight "obj.ham_hood" count 1
        3 weight "obj.ham_badge" count 1
        3 weight "obj.ham_robe" count 1
        3 weight "obj.ham_shirt" count 1
        6 weight "obj.steel_arrow" count 1..13
        6 weight "obj.steel_axe" count 1
        6 weight "obj.steel_dagger" count 1
        6 weight "obj.steel_pickaxe" count 1
        12 weight "obj.digsitebuttons" count 1
        9 weight "obj.feather" count 1..7
        6 weight "obj.knife" count 1
        9 weight "obj.logs" count 1
        6 weight "obj.needle" count 1
        6 weight "obj.raw_anchovies" count 1
        6 weight "obj.raw_chicken" count 1
        9 weight "obj.thread" count 1..10
        6 weight "obj.tinderbox" count 1
        6 weight "obj.uncut_opal" count 1
        6 weight "obj.trail_clue_easy_simple001" count 1 transformObj { player ->
            player.clueScrollTransformObj("obj.trail_clue_easy_simple001")
        }
        6 weight "obj.coal" count 1
        9 weight "obj.cow_hide" count 1
        12 weight "obj.digsitearmour1" count 1
        6 weight rsPlayerWeightedTable(total = 11) {
            name("H.A.M. Guard Herbs (Pickpocket)")
            6 weight "obj.unidentified_guam" count 1
            3 weight "obj.unidentified_marentill" count 1
            2 weight "obj.unidentified_tarromin" count 1
        }
        6 weight "obj.iron_ore" count 1
        12 weight "obj.digsitesword" count 1
        6 weight "obj.uncut_jade" count 1
    },
)
