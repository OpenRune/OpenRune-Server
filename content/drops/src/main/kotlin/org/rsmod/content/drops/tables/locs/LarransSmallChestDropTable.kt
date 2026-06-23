package org.rsmod.content.drops.tables.locs

import dtx.rs.RSDropTable
import dtx.rs.locs
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val larransSmallChestDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Larran's Small Chest",
    locs = locs("loc.slayer_larran_chest_small_closed"),
    mainTable = rsPlayerWeightedTable(total = 60) {
        name("Larran's Small Chest")
        // 5/60
        5 weight "obj.cert_uncut_diamond" count 15..25
        5 weight "obj.cert_uncut_ruby" count 20..30
        5 weight "obj.cert_coal" count 282..480
        // 4/60
        4 weight "obj.cert_gold_ore" count 81..179
        4 weight "obj.dragon_arrowheads" count 41..182
        // 3/60
        3 weight "obj.coins" count 40534..114792
        3 weight "obj.cert_iron_ore" count 300..449
        3 weight "obj.cert_rune_full_helm" count 1..3
        3 weight "obj.cert_rune_platebody" count 1..2
        3 weight "obj.cert_rune_platelegs" count 1..2
        3 weight "obj.cert_blankrune_high" count 3041..5989
        // 2/60
        2 weight "obj.cert_runite_ore" count 5..10
        2 weight "obj.cert_steel_bar" count 253..450
        2 weight "obj.cert_magic_logs" count 80..120
        2 weight "obj.dragon_dart_tip" count 31..149
        // 1/60
        1 weight "obj.palm_tree_seed" count 1..3
        1 weight "obj.magic_tree_seed" count 1..2
        1 weight "obj.celastrus_tree_seed" count 1..3
        1 weight "obj.dragonfruit_tree_seed" count 1..3
        1 weight "obj.redwood_tree_seed" count 1
        1 weight "obj.torstol_seed" count 2..4
        1 weight "obj.snapdragon_seed" count 2..4
        1 weight "obj.ranarr_seed" count 2..4
        // 3/60 = 1/20: fish sub-table (level-dependent sequential rolls, raw tuna is the minimum)
        3 weight rsPlayerWeightedTable {
            1 weight "obj.cert_raw_manta_ray" count 1
            1 weight "obj.cert_raw_seaturtle" count 81..177
            2 weight "obj.cert_raw_shark" count 126..250
            2 weight "obj.cert_raw_monkfish" count 162..297
            2 weight "obj.cert_raw_swordfish" count 113..264
            2 weight "obj.cert_raw_lobster" count 163..342
            2 weight "obj.cert_raw_tuna" count 112..307
        }
    },
)
