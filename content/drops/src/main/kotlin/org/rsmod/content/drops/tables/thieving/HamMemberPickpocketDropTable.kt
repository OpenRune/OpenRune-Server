package org.rsmod.content.drops.tables.thieving

import dtx.rs.RSDropTable
import dtx.rs.pickpockets
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.game.entity.Player

private val hamMemberMainTable = rsPlayerWeightedTable(total = 102) {
    name("H.A.M. Member (Pickpocket)")
    17 weight "obj.pickpocket_coin_pouch_ham" count 1
    1 weight "obj.ham_boots" count 1
    1 weight "obj.ham_cloak" count 1
    1 weight "obj.ham_gloves" count 1
    1 weight "obj.ham_hood" count 1
    1 weight "obj.ham_badge" count 1
    1 weight "obj.ham_robe" count 1
    1 weight "obj.ham_shirt" count 1
    3 weight "obj.bronze_arrow" count 1..13
    3 weight "obj.bronze_axe" count 1
    3 weight "obj.bronze_dagger" count 1
    3 weight "obj.bronze_pickaxe" count 1
    3 weight "obj.iron_axe" count 1
    3 weight "obj.iron_dagger" count 1
    3 weight "obj.iron_pickaxe" count 1
    3 weight "obj.leather_armour" count 1
    2 weight "obj.steel_arrow" count 1..13
    2 weight "obj.steel_axe" count 1
    2 weight "obj.steel_dagger" count 1
    2 weight "obj.steel_pickaxe" count 1
    4 weight "obj.digsitebuttons" count 1
    3 weight "obj.feather" count 1..7
    2 weight "obj.knife" count 1
    3 weight "obj.logs" count 1
    2 weight "obj.needle" count 1
    2 weight "obj.raw_anchovies" count 1
    2 weight "obj.raw_chicken" count 1
    3 weight "obj.thread" count 1..10
    2 weight "obj.tinderbox" count 1
    2 weight "obj.uncut_opal" count 1
    2 weight "obj.coal" count 1
    3 weight "obj.cow_hide" count 1
    4 weight "obj.digsitearmour1" count 1
    2 weight rsPlayerWeightedTable(total = 11) {
        name("H.A.M. Member Herbs (Pickpocket)")
        6 weight "obj.unidentified_guam" count 1
        3 weight "obj.unidentified_marentill" count 1
        2 weight "obj.unidentified_tarromin" count 1
    }
    2 weight "obj.iron_ore" count 1
    4 weight "obj.digsitesword" count 1
    2 weight "obj.uncut_jade" count 1
    2 weight nothing()
}

@field:RegisterDropTable
@JvmField
public val hamMemberPickpocketDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "H.A.M. Member (Pickpocket)",
    pickpockets = pickpockets("ham_member"),
    mainTable = rsPlayerWeightedTable(total = 50) {
        name("H.A.M. Member pre-roll (Pickpocket)")
        1 weight "obj.trail_clue_easy_simple001" count 1 transformObj { player ->
            player.clueScrollTransformObj("obj.trail_clue_easy_simple001")
        }
        1 weight nothing()
        48 weight hamMemberMainTable
    },
)
