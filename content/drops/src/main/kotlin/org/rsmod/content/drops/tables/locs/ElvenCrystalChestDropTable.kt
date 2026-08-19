package org.rsmod.content.drops.tables.locs

import dtx.rs.RSDropTable
import dtx.rs.locs
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val elvenCrystalChestDropTable: RSDropTable<Player, DropRollItem> =
    RSDropTable(
        tableIdentifier = "Elven Crystal Chest",
        locs = locs("loc.prif_crystal_chest_closed"),
        guaranteed = rsPlayerGuaranteedTable { "obj.uncut_dragonstone" count 1 },
        tertiaries =
            rsPlayerTertiaryTable {
                1 outOf 10000 weight "obj.uncut_onyx" count 1
                1 outOf
                    500 chance
                    rsPlayerWeightedTable {
                        1 weight "obj.dragonstone_helmet" count 1
                        1 weight "obj.dragonstone_platebody" count 1
                        1 weight "obj.dragonstone_platelegs" count 1
                        1 weight "obj.dragonstone_gauntlets" count 1
                        1 weight "obj.dragonstone_armoured_boots" count 1
                    }
            },
        mainTable =
            rsPlayerWeightedTable(total = 256) {
                name("Elven Crystal Chest")
                // 64/256: coins 10k-15k + a crystal key half
                64 weight
                    rsPlayerGuaranteedTable {
                        "obj.coins" count 10000..15000
                        add(
                            rsPlayerWeightedTable {
                                1 weight "obj.keyhalf2" count 1
                                1 weight "obj.keyhalf1" count 1
                            }
                        )
                    }
                // 32/256: noted rubies OR noted diamonds
                32 weight
                    rsPlayerWeightedTable {
                        1 weight "obj.cert_uncut_ruby" count 8..13
                        1 weight "obj.cert_uncut_diamond" count 5..8
                    }
                // 26/256: crystal key
                26 weight "obj.crystal_key" count 1
                // 20/256: large coins + crystal shards
                20 weight
                    rsPlayerGuaranteedTable {
                        "obj.coins" count 30000..50000
                        "obj.prif_crystal_shard" count 13..19
                    }
                // 17/256: crystal shards only
                17 weight "obj.prif_crystal_shard" count 25..35
                // 17/256: crystal shards + rune platelegs or plateskirt
                17 weight
                    rsPlayerGuaranteedTable {
                        "obj.prif_crystal_shard" count 7..9
                        add(
                            rsPlayerWeightedTable {
                                1 weight "obj.rune_platelegs" count 1
                                1 weight "obj.rune_plateskirt" count 1
                            }
                        )
                    }
                // 17/256: one type of rune (50-100)
                17 weight
                    rsPlayerWeightedTable {
                        1 weight "obj.cosmicrune" count 50..100
                        1 weight "obj.chaosrune" count 50..100
                        1 weight "obj.naturerune" count 50..100
                        1 weight "obj.lawrune" count 50..100
                        1 weight "obj.deathrune" count 50..100
                    }
                // 16/256: yew seed
                16 weight "obj.yew_seed" count 1
                // 16/256: noted raw sharks
                16 weight "obj.cert_raw_shark" count 50..100
                // 12/256: noted gold ore
                12 weight "obj.cert_gold_ore" count 350..500
                // 9/256: runite ores
                9 weight "obj.runite_ore" count 7..10
                // 7/256: crystal acorns
                7 weight "obj.crystal_tree_seed" count 4..6
                // 3/256: dragon item (platelegs, plateskirt, or shield left half)
                3 weight
                    rsPlayerWeightedTable {
                        1 weight "obj.dragon_platelegs" count 1
                        1 weight "obj.dragon_plateskirt" count 1
                        1 weight "obj.dragonshield_a" count 1
                    }
            },
    )
