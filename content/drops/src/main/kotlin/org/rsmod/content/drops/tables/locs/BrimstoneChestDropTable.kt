package org.rsmod.content.drops.tables.locs

import dtx.core.RollResult
import dtx.core.singleRollable
import dtx.rs.RSDropTable
import dtx.rs.locs
import kotlin.random.Random
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.player.stat.statBase
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val brimstoneChestDropTable: RSDropTable<Player, DropRollItem> =
    RSDropTable(
        tableIdentifier = "Brimstone Chest",
        locs = locs("loc.brimstone_konar_chest_closed"),
        tertiaries =
            rsPlayerTertiaryTable {
                1 outOf
                    100 chance
                    rsPlayerWeightedTable(total = 10) {
                        5 weight "obj.broken_dragon_hasta" count 1
                        1 weight "obj.mystic_hat_dusk" count 1
                        1 weight "obj.mystic_robe_top_dusk" count 1
                        1 weight "obj.mystic_robe_bottom_dusk" count 1
                        1 weight "obj.mystic_gloves_dusk" count 1
                        1 weight "obj.mystic_boots_dusk" count 1
                    }
            },
        mainTable =
            rsPlayerWeightedTable(total = 60) {
                name("Brimstone Chest")
                5 weight "obj.cert_uncut_diamond" count 25..35
                5 weight "obj.cert_uncut_ruby" count 25..35
                5 weight "obj.cert_coal" count 300..500
                5 weight "obj.coins" count 50000..150000
                4 weight "obj.cert_gold_ore" count 100..200
                4 weight "obj.dragon_arrowheads" count 50..200
                3 weight "obj.cert_iron_ore" count 350..500
                3 weight "obj.cert_rune_full_helm" count 2..4
                3 weight "obj.cert_rune_platebody" count 1..2
                3 weight "obj.cert_rune_platelegs" count 1..2
                2 weight "obj.cert_runite_ore" count 10..15
                2 weight "obj.cert_steel_bar" count 300..500
                2 weight "obj.cert_magic_logs" count 120..160
                2 weight "obj.dragon_dart_tip" count 40..160
                1 weight "obj.palm_tree_seed" count 2..4
                1 weight "obj.magic_tree_seed" count 2..3
                1 weight "obj.celastrus_tree_seed" count 2..4
                1 weight "obj.dragonfruit_tree_seed" count 2..4
                1 weight "obj.redwood_tree_seed" count 1
                1 weight "obj.torstol_seed" count 3..5
                1 weight "obj.snapdragon_seed" count 3..5
                1 weight "obj.ranarr_seed" count 3..5
                1 weight "obj.cert_blankrune_high" count 3000..6000
                3 weight
                    singleRollable<Player, DropRollItem> {
                        selectResult { player, _ ->
                            val fishing = player.statBase("stat.fishing")
                            val successChance = 1 + fishing / 33
                            fun roll(n: Int) = Random.nextInt(n) < successChance
                            fun fishOrLure(item: DropRollItem): DropRollItem {
                                val res =
                                    if (Random.nextBoolean())
                                        DropRollItem("obj.shark_lure", 160..500)
                                    else item
                                return res
                            }
                            RollResult.Single(
                                when {
                                    fishing >= 33 && roll(20) ->
                                        fishOrLure(DropRollItem("obj.cert_raw_mantaray", 80..160))
                                    fishing >= 17 && roll(20) ->
                                        fishOrLure(DropRollItem("obj.cert_raw_seaturtle", 80..200))
                                    fishing >= 17 && roll(8) ->
                                        fishOrLure(DropRollItem("obj.cert_raw_shark", 80..250))
                                    roll(3) -> DropRollItem("obj.cert_raw_monkfish", 100..300)
                                    roll(2) -> DropRollItem("obj.cert_raw_swordfish", 100..300)
                                    roll(2) -> DropRollItem("obj.cert_raw_tuna", 150..350)
                                    else -> DropRollItem("obj.cert_raw_lobster", 100..350)
                                }
                            )
                        }
                    }
            },
    )
