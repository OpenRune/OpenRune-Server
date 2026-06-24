package org.rsmod.content.drops.tables.locs

import dtx.rs.RSDropTable
import dtx.rs.locs
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.game.entity.Player

private val grubbyFoodRollTable =
    rsPlayerWeightedTable(total = 20) {
        7 weight "obj.shark" count 4
        12 weight "obj.potato_egg+tomato" count 4
        1 weight
            rsPlayerGuaranteedTable {
                "obj.2dosepotionofsaradomin" count 3
                "obj.2dose2restore" count 1
            }
    }

private val grubbyPotionRollTable =
    rsPlayerWeightedTable(total = 20) {
        8 weight
            rsPlayerGuaranteedTable {
                "obj.2dose2attack" count 1
                "obj.2dose2strength" count 1
                "obj.2dose2defense" count 1
            }
        8 weight
            rsPlayerGuaranteedTable {
                "obj.2doserangerspotion" count 1
                "obj.2dose2defense" count 1
            }
        1 weight "obj.3dose2restore" count 2
        3 weight "obj.3doseprayerrestore" count 2
    }

@field:RegisterDropTable
@JvmField
public val grubbyChestDropTable: RSDropTable<Player, DropRollItem> =
    RSDropTable(
        tableIdentifier = "Grubby Chest",
        locs = locs("loc.hosdun_grubby_chest"),
        guaranteed =
            rsPlayerGuaranteedTable {
                add(grubbyFoodRollTable)
                add(grubbyFoodRollTable)
                add(grubbyPotionRollTable)
            },
        mainTable =
            rsPlayerWeightedTable(total = 100) {
                name("Grubby Chest")
                10 weight "obj.lawrune" count 200
                10 weight "obj.deathrune" count 200
                10 weight "obj.astralrune" count 200
                10 weight "obj.bloodrune" count 200
                8 weight "obj.coins" count 10000
                8 weight "obj.cert_unidentified_toadflax" count 10
                8 weight "obj.cert_unidentified_ranarr" count 10
                7 weight "obj.cert_unidentified_snapdragon" count 10
                7 weight "obj.cert_unidentified_torstol" count 5
                6 weight "obj.crystal_key" count 1
                6 weight "obj.cert_dragon_bones" count 10
                6 weight "obj.cert_dragonhide_red" count 10
                2 weight "obj.dragon_dart_tip" count 50
                2 weight "obj.dragon_arrowheads" count 100
            },
        tertiaries =
            rsPlayerTertiaryTable {
                1 outOf 25 weight "obj.hosdun_orange_egg_sac" count 1
                1 outOf 25 weight "obj.hosdun_blue_egg_sac" count 1
            },
    )
