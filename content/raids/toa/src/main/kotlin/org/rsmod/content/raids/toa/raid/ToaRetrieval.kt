package org.rsmod.content.raids.toa.raid

import kotlin.math.min
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.death.PlayerDeathDrops
import org.rsmod.api.death.PlayerDeathHandling
import org.rsmod.api.death.UntradeableHandling
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.player.ironman.PlayerGamemode
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.getInvObj

/**
 * The Tombs of Amascut item retrieval chest in the lobby (Offline_Scape ItemRetrievalService,
 * DeathMechanics.service and TOAManager.triggerTOAFailure).
 *
 * OSRS Wiki (Tombs of Amascut; Death/Item Recovery Fees): a failed raid, or a logout outside a
 * safe area, is a death under the normal rules: the 3 most valuable items are kept (4 with
 * Protect Item) and the rest go to this chest, behind the standard grave fee. With no death
 * invocation on, deaths are unlimited and nothing is lost.
 *
 * The chest is the cache's `gravestone` inv (525, 120 slots), the one interface 602 shows. It is
 * made persistent in toa_vars.toml. Only the raid uses it for now; any other retrieval service
 * added later would share it, as in OSRS.
 */
internal object ToaRetrieval {
    const val INV = "inv.gravestone"

    /** Protect Item at the killing hit. Prayers are off by the time the raid fails. */
    val PROTECT_ITEM_AT_DEATH = AttributeKey<Boolean>()

    var Player.retrievalLocked by intVarp("varp.toa_retrieval_locked")

    val Player.retrievalChest: Inventory
        get() = invMap.getOrPut(INV)

    /**
     * Keeps what the normal death rules keep and moves the rest into the chest. Items already in
     * the chest are lost: OSRS deletes an unclaimed retrieval service on the next death
     * (Offline_Scape cleared it too). Returns whether anything went into the chest.
     */
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

        // Like PlayerDeathDrops.applyDrops: empty both, then put the kept items back.
        player.inv.fillNulls()
        player.worn.fillNulls()
        for (obj in result.kept) {
            val slot = player.inv.objs.indexOfFirst { it == null }
            if (slot >= 0) player.inv[slot] = obj
        }
        player.rebuildAppearance()

        // At most 42 carried stacks, so they always fit the empty 120 slots.
        for (obj in lost) {
            player.invAdd(chest, obj.id, obj.count, obj.vars, strict = false)
        }
        if (chest.isEmpty()) return false
        if (fee(player, prices) > 0) player.retrievalLocked = 1
        return true
    }

    /** The normal PvM death: no supply pile, untradeables go with the rest. */
    private fun handling(protectItem: Boolean): PlayerDeathHandling =
        PlayerDeathHandling(
            keepCount = PlayerDeathDrops.standardKeepCount(protectItem),
            dropReceiver = null,
            dropDuration = 0,
            revealDelay = 0,
            supplyPile = false,
            untradeableHandling = UntradeableHandling.DROP,
        )

    /**
     * OSRS Wiki (Death/Item Recovery Fees, from a grave), which ToA uses: nothing under 100k, then
     * 1,000 / 10,000 / 100,000 per item from 100k / 1m / 10m, at most 500,000. Each stack counts
     * as one item. Ironmen pay half; ultimate ironmen don't get the discount (as at Death's
     * Office; the grave's wording just says ironmen).
     */
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
