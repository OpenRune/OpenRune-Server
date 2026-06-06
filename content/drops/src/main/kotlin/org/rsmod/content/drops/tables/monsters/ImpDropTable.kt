package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.shouldDropLootingBag
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val impDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Imp Drops",
    npcs = npcs("npc.godwars_ancient_imp", "npc.imp"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Imp Drops")
        5 weight "obj.black_bead" count 1
        5 weight "obj.red_bead" count 1
        5 weight "obj.white_bead" count 1
        5 weight "obj.yellow_bead" count 1
        8 weight "obj.bolt" count 1
        8 weight "obj.bluewizhat" count 1
        5 weight "obj.egg" count 1
        5 weight "obj.raw_chicken" count 1
        4 weight "obj.burnt_bread" count 1
        4 weight "obj.burnt_meat" count 1
        2 weight "obj.cabbage" count 1
        2 weight "obj.bread_dough" count 1
        1 weight "obj.bread" count 1
        1 weight "obj.cooked_meat" count 1
        8 weight "obj.hammer" count 1
        5 weight "obj.tinderbox" count 1
        4 weight "obj.shears" count 1
        4 weight "obj.bucket_empty" count 1
        2 weight "obj.bucket_water" count 1
        2 weight "obj.jug_empty" count 1
        2 weight "obj.jug_water" count 1
        2 weight "obj.pot_empty" count 1
        2 weight "obj.pot_flour" count 1
        8 weight "obj.ball_of_wool" count 1
        7 weight "obj.mind_talisman" count 1
        6 weight "obj.ashes" count 1
        4 weight "obj.clay" count 1
        4 weight "obj.cadavaberries" count 1
        3 weight "obj.grain" count 1
        2 weight "obj.chefs_hat" count 1
        2 weight "obj.flier" count 1
        1 weight "obj.acne_potion" count 1
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 25 weight "obj.arceuus_corpse_imp" count 1
        1 outOf 15 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
        1 outOf 5000 weight "obj.champions_challenge_imp" count 1
    },
)
