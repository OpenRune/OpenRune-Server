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
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val tribesmanDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Tribesman Drops",
    npcs = npcs("npc.tribesman", "npc.tribesman_variant01"),
    mainTable = rsPlayerWeightedTable(total = 138) {
        name("Tribesman Drops")
        7 weight "obj.bronze_spear" count 1
        3 weight "obj.steel_javelin" count 10
        2 weight "obj.poison_bolt" count 4
        2 weight "obj.iron_spear" count 1
        2 weight "obj.steel_arrow_p" count 5
        2 weight "obj.mithril_javelin" count 10
        1 weight "obj.mithril_spear" count 1
        5 weight "obj.unidentified_rogues_purse" count 1
        5 weight "obj.unidentified_snake_weed" count 1
        25 weight "obj.village_trade_sticks" count 15
        20 weight "obj.snape_grass" count 1
        12 weight "obj.limpwurt_root" count 1
        12 weight "obj.tbwt_cleaning_cloth" count 1
        8 weight "obj.naturerune" count 3
        5 weight "obj.village_trade_sticks" count 62
        5 weight "obj.gold_ore" count 1
        3 weight "obj.2dose2antipoison" count 1
        1 weight "obj.3dose2antipoison" count 1
        1 weight "obj.bread" count 1
        1 weight "obj.tin_ore" count 1
        1 weight "obj.pot_flour" count 1
        2 outOf 128 separate ringNothing()

        11 weight SharedDropTables.herb
        2 weight SharedDropTables.gem
        2 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): The medium clue scroll drop rate increases to 1/131 after unlocking the medium Combat Achievements rewards tier.
        1 outOf 138 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
