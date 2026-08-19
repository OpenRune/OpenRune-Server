package org.rsmod.content.drops.tables.locs

import dtx.core.RollResult
import dtx.core.singleRollable
import dtx.rs.RSDropTable
import dtx.rs.locs
import kotlin.random.Random
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.player.stat.statBase
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val larransSmallChestDropTable: RSDropTable<Player, DropRollItem> =
    RSDropTable(
        tableIdentifier = "Larran's Small Chest",
        locs = locs("loc.slayer_larran_chest_small_closed"),
        mainTable =
            rsPlayerWeightedTable(total = 60) {
                name("Larran's Small Chest")
                5 weight "obj.cert_uncut_diamond" count 15..25
                5 weight "obj.cert_uncut_ruby" count 20..30
                5 weight "obj.cert_coal" count 282..480
                4 weight "obj.cert_gold_ore" count 81..179
                4 weight "obj.dragon_arrowheads" count 41..182
                3 weight "obj.coins" count 40534..114792
                3 weight "obj.cert_iron_ore" count 300..449
                3 weight "obj.cert_rune_full_helm" count 1..3
                3 weight "obj.cert_rune_platebody" count 1..2
                3 weight "obj.cert_rune_platelegs" count 1..2
                3 weight "obj.cert_blankrune_high" count 3041..5989
                2 weight "obj.cert_runite_ore" count 5..10
                2 weight "obj.cert_steel_bar" count 253..450
                2 weight "obj.cert_magic_logs" count 80..120
                2 weight "obj.dragon_dart_tip" count 31..149
                1 weight "obj.palm_tree_seed" count 1..3
                1 weight "obj.magic_tree_seed" count 1..2
                1 weight "obj.celastrus_tree_seed" count 1..3
                1 weight "obj.dragonfruit_tree_seed" count 1..3
                1 weight "obj.redwood_tree_seed" count 1
                1 weight "obj.torstol_seed" count 2..4
                1 weight "obj.snapdragon_seed" count 2..4
                1 weight "obj.ranarr_seed" count 2..4
                3 weight
                    singleRollable<Player, DropRollItem> {
                        selectResult { player, _ ->
                            val fishing = player.statBase("stat.fishing")
                            val successChance = 1 + fishing / 33
                            fun roll(n: Int) = Random.nextInt(n) < successChance
                            RollResult.Single(
                                when {
                                    fishing >= 33 && roll(20) ->
                                        DropRollItem("obj.cert_raw_mantaray", 81..144)
                                    fishing >= 17 && roll(20) ->
                                        DropRollItem("obj.cert_raw_seaturtle", 81..177)
                                    fishing >= 17 && roll(8) ->
                                        DropRollItem("obj.cert_raw_shark", 126..250)
                                    roll(3) -> DropRollItem("obj.cert_raw_monkfish", 162..297)
                                    roll(2) -> DropRollItem("obj.cert_raw_swordfish", 113..264)
                                    else -> DropRollItem("obj.cert_raw_lobster", 163..342)
                                }
                            )
                        }
                    }
            },
    )
