package org.rsmod.content.drops.tables.locs

import dtx.rs.RSDropTable
import dtx.rs.locs
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerPrerollTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val moonChestDropTable: RSDropTable<Player, DropRollItem> =
    RSDropTable(
        tableIdentifier = "Moon Chest",
        locs = locs("loc.varlamore_moon_chestclosed"),
        guaranteed = rsPlayerGuaranteedTable { "obj.sunfiresplinter" count 250 },
        preRoll =
            rsPlayerPrerollTable {
                1 outOf 5000 weight "obj.uncut_onyx" count 1
                1 outOf 500 weight "obj.moon_helmet" count 1
            },
        mainTable =
            rsPlayerWeightedTable(total = 107) {
                name("Moon Chest")
                20 weight "obj.dragonstone" count 1
                10 weight "obj.cert_dragon_platelegs" count 2
                10 weight "obj.naturerune" count 500
                10 weight "obj.huasca_seed" count 6
                10 weight "obj.cert_rune_platebody" count 6
                10 weight "obj.watermelon_seed" count 100
                10 weight "obj.cert_sun_kissed_bone" count 100
                10 weight "obj.cert_raw_monkfish" count 300
                10 weight "obj.cert_uncut_diamond" count 50
                1 weight "obj.cert_gold_ore" count 500
                1 weight "obj.coal" count 1
                1 weight "obj.cabbage" count 28
                1 weight "obj.crystal_key" count 1
                1 weight "obj.varlamore_key" count 1
                1 weight "obj.xbows_crossbow_bolts_runite_tipped_onyx" count 150
                1 weight "obj.spinach_roll" count 1
            },
    )
