package org.rsmod.content.raids.toa.raid

import kotlin.math.min
import org.rsmod.api.death.PlayerDeathDrops
import org.rsmod.api.death.PlayerDeathHandling
import org.rsmod.api.death.UntradeableHandling
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.player.ironman.PlayerGamemode
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.getInvObj

internal object ToaRetrieval {
    const val INV = "inv.gravestone"

    var Player.retrievalLocked by intVarp("varp.toa_retrieval_locked")
    var Player.protectItemAtDeath by boolVarBit("varbit.toa_protect_item_at_death")
    val Player.retrievalChest: Inventory
        get() = invMap.getOrPut(INV)

    fun store(
        player: Player,
        drops: PlayerDeathDrops,
        prices: MarketPrices,
        protectItem: Boolean,
    ): Boolean {
        val carried = player.inv.filterNotNull { true } + player.worn.filterNotNull { true }
        val chest = player.retrievalChest
        chest.fillNulls()
        player.retrievalLocked = 0
        if (carried.isEmpty()) return false

        val rules =
            PlayerDeathDrops.DeathDropRules(
                isUIM = player.gamemode == PlayerGamemode.ULTIMATE_IRONMAN
            )
        val result = drops.selectDrops(carried, rules, handling(protectItem))
        val lost = result.lostTradeable + result.lostUntradeable + result.supplyPile

        player.inv.fillNulls()
        player.worn.fillNulls()
        for (obj in result.kept) {
            val slot = player.inv.objs.indexOfFirst { it == null }
            if (slot >= 0) player.inv[slot] = obj
        }
        player.rebuildAppearance()

        for (obj in lost) {
            player.invAdd(chest, obj.id, obj.count, obj.vars, strict = false)
        }
        if (chest.isEmpty()) return false
        if (fee(player, prices) > 0) player.retrievalLocked = 1
        return true
    }

    private fun handling(protectItem: Boolean): PlayerDeathHandling =
        PlayerDeathHandling(
            keepCount = PlayerDeathDrops.standardKeepCount(protectItem),
            dropReceiver = null,
            dropDuration = 0,
            revealDelay = 0,
            supplyPile = false,
            untradeableHandling = UntradeableHandling.DROP,
        )

    fun discard(player: Player) {
        val chest = player.invMap[INV] ?: return
        if (chest.isEmpty()) return
        chest.fillNulls()
        player.retrievalLocked = 0
    }

    fun fee(player: Player, prices: MarketPrices): Int {
        var total = 0L
        for (obj in player.retrievalChest.filterNotNull { true }) {
            val type = getInvObj(obj)
            val value = (prices[type] ?: type.cost).toLong() * obj.count
            total +=
                when {
                    value >= TIER_3 -> FEE_3
                    value >= TIER_2 -> FEE_2
                    value >= TIER_1 -> FEE_1
                    else -> 0
                }
        }
        var fee = min(total, MAX_FEE).toInt()
        val discounted =
            player.gamemode != PlayerGamemode.NORMAL &&
                player.gamemode != PlayerGamemode.ULTIMATE_IRONMAN
        if (discounted) fee /= 2
        return fee
    }

    private const val TIER_1 = 100_000L
    private const val TIER_2 = 1_000_000L
    private const val TIER_3 = 10_000_000L
    private const val FEE_1 = 1_000L
    private const val FEE_2 = 10_000L
    private const val FEE_3 = 100_000L
    private const val MAX_FEE = 500_000L
}
