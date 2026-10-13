package org.rsmod.content.drops.tables.thieving

import dtx.rs.RSDropTable
import dtx.rs.locs
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val seedStallDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Seed Stall (Thieving)",
    locs = locs("loc.seed_stall"),
    mainTable = rsPlayerWeightedTable(total = 1000) {
        name("Seed Stall (Thieving)")
        120 weight "obj.hammerstone_hop_seed" count 1
        119 weight "obj.potato_seed" count 1
        119 weight "obj.marigold_seed" count 1
        118 weight "obj.barley_seed" count 1
        89 weight "obj.onion_seed" count 1
        83 weight "obj.asgarnian_hop_seed" count 1
        71 weight "obj.cabbage_seed" count 1
        47 weight "obj.yanillian_hop_seed" count 1
        36 weight "obj.rosemary_seed" count 1
        35 weight "obj.nasturtium_seed" count 1
        35 weight "obj.tomato_seed" count 1
        35 weight "obj.jute_seed" count 1
        30 weight "obj.sweetcorn_seed" count 1
        24 weight "obj.krandorian_hop_seed" count 1
        18 weight "obj.strawberry_seed" count 1
        12 weight "obj.wildblood_hop_seed" count 1
        9 weight "obj.watermelon_seed" count 1
    },
)
