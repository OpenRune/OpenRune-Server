package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val wallBeastDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Wall beast Drops",
    npcs = npcs("npc.swamp_wallbeast", "npc.swamp_wallbeast_combat"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Wall beast Drops")
        3 weight "obj.airrune" count 3
        3 weight "obj.cosmicrune" count 2
        2 weight "obj.chaosrune" count 3
        1 weight "obj.chaosrune" count 7
        2 weight "obj.bronze_med_helm" count 1
        2 weight "obj.bronze_full_helm" count 1
        8 weight "obj.iron_med_helm" count 1
        7 weight "obj.steel_med_helm" count 1
        4 weight "obj.steel_full_helm" count 1
        1 weight "obj.mithril_med_helm" count 1
        1 weight "obj.mithril_full_helm" count 1
        1 weight "obj.adamant_med_helm" count 1
        32 weight "obj.unidentified_guam" count 1
        10 weight "obj.coins" count 15
        21 weight "obj.tinderbox" count 1
        12 weight "obj.bullseye_lantern_lens" count 1
        9 weight "obj.torch_unlit" count 1
        1 weight "obj.eye_of_newt" count 1
        5 weight "obj.bullseye_lantern_nolens" count 1
        3 weight ringNothing()
        1 outOf 512 separate "obj.mystic_hat_light" count 1
    },
)
