package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val hAMGuardDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "H.A.M. Guard Drops",
    npcs = npcs("npc.favour_guard_long_beard", "npc.favour_guard_male_bearded", "npc.favour_guard_male_no_beard"),
    mainTable = rsPlayerWeightedTable(total = 110) {
        name("H.A.M. Guard Drops")
        3 weight "obj.bronze_axe" count 1
        3 weight "obj.bronze_dagger" count 1
        3 weight "obj.bronze_pickaxe" count 1
        3 weight "obj.iron_axe" count 1
        3 weight "obj.iron_pickaxe" count 1
        3 weight "obj.iron_dagger" count 1
        3 weight "obj.leather_armour" count 1
        3 weight "obj.ham_gloves" count 1
        2 weight "obj.steel_dagger" count 1
        2 weight "obj.steel_axe" count 1
        2 weight "obj.steel_pickaxe" count 1
        2 weight "obj.ham_boots" count 1
        1 weight "obj.ham_shirt" count 1
        1 weight "obj.ham_robe" count 1
        1 weight "obj.ham_badge" count 1
        1 weight "obj.ham_hood" count 1
        1 weight "obj.ham_cloak" count 1
        3 weight "obj.bronze_arrow" count 1..12
        2 weight "obj.steel_arrow" count 1..10
        1 weight "obj.unidentified_guam" count 1
        1 weight "obj.unidentified_marentill" count 1
        0 weight "obj.unidentified_tarromin" count 1
        3 weight "obj.cow_hide" count 1..3
        3 weight "obj.logs" count 1..3
        2 weight "obj.uncut_opal" count 1
        2 weight "obj.uncut_jade" count 1
        2 weight "obj.iron_ore" count 1
        2 weight "obj.coal" count 1
        2 weight "obj.raw_anchovies" count 1..3
        2 weight "obj.raw_chicken" count 1..3
        14 weight "obj.coins" count 1..20
        4 weight "obj.digsitebuttons" count 1
        4 weight "obj.digsitearmour1" count 1
        4 weight "obj.digsitesword" count 1
        3 weight "obj.feather" count 1..6
        2 weight "obj.thread" count 1..10
        2 weight "obj.knife" count 1
        2 weight "obj.needle" count 1
        2 weight "obj.tinderbox" count 1

        5 weight SharedDropTables.seed
        6 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): The easy clue scroll drop rate increases to 1/52 after unlocking the easy Combat Achievements rewards tier.
        1 outOf 55 weight "obj.trail_clue_easy_simple001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
