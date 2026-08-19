package org.rsmod.content.drops.tables.locs

import dtx.rs.RSDropTable
import dtx.rs.locs
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val crystalChestDropTable: RSDropTable<Player, DropRollItem> =
    RSDropTable(
        tableIdentifier = "Crystal Chest",
        locs = locs("loc.crystal_chestclosed"),
        guaranteed = rsPlayerGuaranteedTable { "obj.uncut_dragonstone" count 1 },
        mainTable =
            rsPlayerWeightedTable(total = 128) {
                name("Crystal Chest")
                17 weight nothing()
                34 weight
                    rsPlayerGuaranteedTable {
                        "obj.spinach_roll" count 1
                        "obj.coins" count 2000
                    }
                12 weight
                    rsPlayerGuaranteedTable {
                        "obj.airrune" count 50
                        "obj.waterrune" count 50
                        "obj.earthrune" count 50
                        "obj.firerune" count 50
                        "obj.bodyrune" count 50
                        "obj.mindrune" count 50
                        "obj.chaosrune" count 10
                        "obj.deathrune" count 10
                        "obj.cosmicrune" count 10
                        "obj.naturerune" count 10
                        "obj.lawrune" count 10
                    }
                12 weight
                    rsPlayerGuaranteedTable {
                        "obj.ruby" count 2
                        "obj.diamond" count 2
                    }
                12 weight "obj.runite_bar" count 3
                10 weight
                    rsPlayerGuaranteedTable {
                        "obj.coins" count 750
                        add(
                            rsPlayerWeightedTable {
                                1 weight "obj.keyhalf2" count 1
                                1 weight "obj.keyhalf1" count 1
                            }
                        )
                    }
                10 weight "obj.cert_iron_ore" count 150
                10 weight "obj.cert_coal" count 100
                8 weight
                    rsPlayerGuaranteedTable {
                        "obj.coins" count 1000
                        "obj.raw_swordfish" count 5
                    }
                2 weight "obj.adamant_sq_shield" count 1
                1 weight
                    rsPlayerWeightedTable {
                        1 weight "obj.rune_platelegs" count 1
                        1 weight "obj.rune_plateskirt" count 1
                    }
            },
    )
