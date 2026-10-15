package org.rsmod.content.raids.toa.raid.loot

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.flatten
import dtx.rs.RSWeightedTable
import dtx.rs.rsWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.game.entity.Player

private const val FIXED_QUANTITY = 0

internal object ToaChestTables {
    val commons: RSWeightedTable<Player, DropRollItem> = rsWeightedTable {
        name("Tombs of Amascut chest commons")
        1 weight perPoints("obj.coins", 1)
        1 weight perPoints("obj.deathrune", 20)
        1 weight perPoints("obj.soulrune", 40)
        1 weight perPoints("obj.cert_gold_ore", 90)
        1 weight perPoints("obj.dragon_dart_tip", 100)
        1 weight perPoints("obj.cert_mahogany_logs", 180)
        1 weight perPoints("obj.cert_sapphire", 200)
        1 weight perPoints("obj.cert_emerald", 250)
        1 weight perPoints("obj.cert_gold_bar", 250)
        1 weight perPoints("obj.cert_cactus_potato", 250)
        1 weight perPoints("obj.cert_raw_shark", 250)
        1 weight perPoints("obj.cert_ruby", 300)
        1 weight perPoints("obj.cert_diamond", 400)
        1 weight perPoints("obj.cert_raw_mantaray", 450)
        1 weight perPoints("obj.cert_cactus_spine", 600)
        1 weight perPoints("obj.cert_dragonstone", 600)
        1 weight perPoints("obj.cert_battlestaff", 1100)
        1 weight perPoints("obj.vial_coconut_milk", 1100)
        1 weight perPoints("obj.cert_lily_of_the_sands", 1100)
        1 weight perPoints("obj.toadflax_seed", 2000)
        1 weight perPoints("obj.ranarr_seed", 2500)
        1 weight perPoints("obj.torstol_seed", 3200)
        1 weight perPoints("obj.snapdragon_seed", 3200)
        1 weight perPoints("obj.cert_dragon_med_helm", 4000)
        1 weight perPoints("obj.magic_tree_seed", 6500)
        1 weight perPoints("obj.blood_essence_inactive", 7500)
        1 weight fixed("obj.toa_rune_cache")
    }

    private val uniqueBands: List<Pair<Int, RSWeightedTable<Player, DropRollItem>>> =
        listOf(
            300 to uniques(7, 7, 3, 2, 2, 2, 1),
            350 to uniques(6, 6, 3, 2, 2, 2, 1),
            400 to uniques(4, 5, 3, 2, 2, 2, 1),
            450 to uniques(4, 4, 3, 2, 2, 2, 1),
            500 to uniques(6, 7, 6, 4, 4, 4, 2),
        )

    fun rollCommon(player: Player): CommonDrop {
        val drop = player.roll(commons)
        return CommonDrop(drop.obj, drop.count.first)
    }

    fun rollUnique(player: Player, raidLevel: Int): String =
        player.roll(uniqueBands.minBy { (anchor, _) -> kotlin.math.abs(anchor - raidLevel) }.second).obj

    private fun uniques(
        fang: Int,
        lightbearer: Int,
        ward: Int,
        mask: Int,
        body: Int,
        chaps: Int,
        shadow: Int,
    ): RSWeightedTable<Player, DropRollItem> = rsWeightedTable {
        name("Tombs of Amascut chest uniques")
        fang weight DropRollItem("obj.osmumtens_fang", 1)
        lightbearer weight DropRollItem("obj.lightbearer", 1)
        ward weight DropRollItem("obj.elidinis_ward", 1)
        mask weight DropRollItem("obj.masori_mask", 1)
        body weight DropRollItem("obj.masori_body", 1)
        chaps weight DropRollItem("obj.masori_chaps", 1)
        shadow weight DropRollItem("obj.tumekens_shadow_uncharged", 1)
    }

    private fun perPoints(obj: String, divisor: Int) = DropRollItem(obj, divisor)

    private fun fixed(obj: String) = DropRollItem(obj, FIXED_QUANTITY)

    private fun Player.roll(table: RSWeightedTable<Player, DropRollItem>): DropRollItem =
        when (val result = table.roll(this, ArgMap()).flatten()) {
            is RollResult.Single<DropRollItem> -> result.result
            else -> error("Weighted table '${table.tableIdentifier}' returned no single drop")
        }
}

internal class CommonDrop(val obj: String, val pointsPerUnit: Int)
