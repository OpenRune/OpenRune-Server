package org.rsmod.content.drops.tables.shared

import dtx.rs.RSWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.game.entity.Player

public val uncommonSeedDropTable: RSWeightedTable<Player, DropRollItem> =
    rsPlayerWeightedTable(total = 1048) {
        name("Uncommon seed drop table")
        137 weight "obj.limpwurt_seed" count 1
        131 weight "obj.strawberry_seed" count 1
        125 weight "obj.marrentill_seed" count 1
        92 weight "obj.jangerberry_bush_seed" count 1
        85 weight "obj.tarromin_seed" count 1
        83 weight "obj.wildblood_hop_seed" count 1
        63 weight "obj.watermelon_seed" count 1
        56 weight "obj.harralander_seed" count 1
        40 weight "obj.snape_grass_seed" count 1
        39 weight "obj.ranarr_seed" count 1
        34 weight "obj.whiteberry_bush_seed" count 1
        29 weight "obj.mushroom_seed" count 1
        27 weight "obj.toadflax_seed" count 1
        18 weight "obj.belladonna_seed" count 1
        18 weight "obj.irit_seed" count 1
        13 weight "obj.poisonivy_bush_seed" count 1
        12 weight "obj.avantoe_seed" count 1
        12 weight "obj.cactus_seed" count 1
        9 weight "obj.kwuarm_seed" count 1
        8 weight "obj.potato_cactus_seed" count 1
        5 weight "obj.snapdragon_seed" count 1
        4 weight "obj.cadantine_seed" count 1
        3 weight "obj.lantadyme_seed" count 1
        2 weight "obj.dwarf_weed_seed" count 1
        1 weight "obj.torstol_seed" count 1
        2 weight nothing()
    }
