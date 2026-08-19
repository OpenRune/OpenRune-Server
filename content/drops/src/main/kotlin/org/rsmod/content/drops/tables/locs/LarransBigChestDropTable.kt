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
public val larransBigChestDropTable: RSDropTable<Player, DropRollItem> =
    RSDropTable(
        tableIdentifier = "Larran's Big Chest",
        locs = locs("loc.slayer_larran_chest_big_closed"),
        tertiaries =
            rsPlayerTertiaryTable {
                1 outOf 256 weight "obj.dagonhai_hat" count 1
                1 outOf 256 weight "obj.dagonhai_robe_top" count 1
                1 outOf 256 weight "obj.dagonhai_robe_bottom" count 1
            },
        mainTable =
            rsPlayerWeightedTable(total = 60) {
                name("Larran's Big Chest")
                5 weight "obj.cert_uncut_diamond" count 35..45
                5 weight "obj.cert_uncut_ruby" count 35..45
                5 weight "obj.cert_coal" count 450..650
                4 weight "obj.cert_gold_ore" count 150..250
                4 weight "obj.dragon_arrowheads" count 100..250
                3 weight "obj.coins" count 75000..175000
                3 weight "obj.cert_iron_ore" count 500..650
                3 weight "obj.cert_rune_full_helm" count 3..5
                3 weight "obj.cert_rune_platebody" count 2..3
                3 weight "obj.cert_rune_platelegs" count 2..3
                3 weight "obj.cert_blankrune_high" count 4500..7500
                2 weight "obj.cert_runite_ore" count 15..20
                2 weight "obj.cert_steel_bar" count 350..550
                2 weight "obj.cert_magic_logs" count 180..220
                2 weight "obj.dragon_dart_tip" count 80..200
                1 weight "obj.palm_tree_seed" count 3..5
                1 weight "obj.magic_tree_seed" count 3..4
                1 weight "obj.celastrus_tree_seed" count 3..5
                1 weight "obj.dragonfruit_tree_seed" count 3..5
                1 weight "obj.redwood_tree_seed" count 1
                1 weight "obj.torstol_seed" count 4..6
                1 weight "obj.snapdragon_seed" count 4..6
                1 weight "obj.ranarr_seed" count 4..6
                3 weight
                    singleRollable<Player, DropRollItem> {
                        selectResult { player, _ ->
                            val fishing = player.statBase("stat.fishing")
                            val successChance = 1 + fishing / 33
                            fun roll(n: Int) = Random.nextInt(n) < successChance
                            fun fishOrLure(item: DropRollItem): DropRollItem {
                                val res =
                                    if (Random.nextBoolean())
                                        DropRollItem("obj.shark_lure", 240..750)
                                    else item
                                return res
                            }
                            RollResult.Single(
                                when {
                                    fishing >= 33 && roll(20) ->
                                        fishOrLure(DropRollItem("obj.cert_raw_mantaray", 120..240))
                                    fishing >= 17 && roll(20) ->
                                        fishOrLure(DropRollItem("obj.cert_raw_seaturtle", 120..300))
                                    fishing >= 17 && roll(8) ->
                                        fishOrLure(DropRollItem("obj.cert_raw_shark", 120..375))
                                    roll(3) -> DropRollItem("obj.cert_raw_monkfish", 150..450)
                                    roll(2) -> DropRollItem("obj.cert_raw_swordfish", 150..450)
                                    roll(2) -> DropRollItem("obj.cert_raw_tuna", 150..525)
                                    else -> DropRollItem("obj.cert_raw_lobster", 150..525)
                                }
                            )
                        }
                    }
            },
    )
