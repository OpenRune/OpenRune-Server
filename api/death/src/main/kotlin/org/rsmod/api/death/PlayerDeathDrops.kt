package org.rsmod.api.death

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.config.refs.params
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.obj.Obj
import org.rsmod.game.type.getInvObj
import org.rsmod.map.CoordGrid

@Singleton
public class PlayerDeathDrops
@Inject
constructor(
    private val mapClock: MapClock,
    private val objRepo: ObjRepository,
    private val marketPrices: MarketPrices,
) {
    public fun selectDrops(
        player: Player,
        context: PlayerDeathContext,
        handling: PlayerDeathHandling,
    ): DeathDropResult =
        selectDrops(
            carried = sortedCarriedObjs(player),
            rules = DeathDropRules(isUIM = context.isUIM, isPvpDeath = context.isPvpDeath),
            handling = handling,
        )

    public fun selectDrops(
        carried: List<InvObj>,
        rules: DeathDropRules,
        handling: PlayerDeathHandling,
    ): DeathDropResult {
        val allCarried = carried.sortedByDescending { marketPriceSingle(it) }

        if (rules.isUIM) {
            return selectUimDrops(allCarried, handling, rules)
        }

        val slotlessKept = allCarried.filter { isSlotlessKept(it, rules) }
        val neverKept = allCarried.filter { isNeverKeptInternal(it, rules) }
        val normal = allCarried - slotlessKept.toSet() - neverKept.toSet()

        if (handling.untradeableHandling == UntradeableHandling.KEEP) {
            return selectInstanceDrops(slotlessKept, normal, handling)
        }

        val keptNormal = normal.take(handling.keepCount)
        val lost = normal.drop(handling.keepCount)

        val (supply, lostRemainder) = if (handling.supplyPile) {
            lost.partition { isSupplyPileItem(it) }
        } else {
            emptyList<InvObj>() to lost
        }

        val (lostUntradeable, lostTradeable) = lostRemainder.partition { isUntradeable(it) }

        val coinsForKiller = if (handling.untradeableHandling == UntradeableHandling.COINS) {
            lostUntradeable.sumOf { untradeableConversionValue(it) }
        } else 0L

        return DeathDropResult(
            kept = slotlessKept + keptNormal,
            supplyPile = supply,
            lostTradeable = lostTradeable,
            lostUntradeable = lostUntradeable,
            coinsForKiller = coinsForKiller,
        )
    }

    public data class DeathDropRules(
        val isUIM: Boolean = false,
        val isPvpDeath: Boolean = false,
    )

    private fun selectInstanceDrops(
        slotlessKept: List<InvObj>,
        normal: List<InvObj>,
        handling: PlayerDeathHandling,
    ): DeathDropResult {
        val (untradeablesKept, tradeables) = normal.partition { isUntradeable(it) }
        val keptTradeables = tradeables.take(handling.keepCount)
        val lostTradeables = tradeables.drop(handling.keepCount)
        return DeathDropResult(
            kept = slotlessKept + untradeablesKept + keptTradeables,
            supplyPile = emptyList(),
            lostTradeable = lostTradeables,
            lostUntradeable = emptyList(),
            coinsForKiller = 0L,
        )
    }

    private fun selectUimDrops(
        allCarried: List<InvObj>,
        handling: PlayerDeathHandling,
        rules: DeathDropRules,
    ): DeathDropResult {
        val slotlessKept = allCarried.filter { isSlotlessKept(it, rules) }
        val neverKept = allCarried.filter { isNeverKeptInternal(it, rules) }
        val rest = allCarried - slotlessKept.toSet() - neverKept.toSet()

        val (supply, remaining) = if (handling.supplyPile) {
            rest.partition { isSupplyPileItem(it) }
        } else {
            emptyList<InvObj>() to rest
        }

        return DeathDropResult(
            kept = slotlessKept,
            supplyPile = supply,
            lostTradeable = remaining,
            lostUntradeable = emptyList(),
            coinsForKiller = 0L,
        )
    }

    public fun spawnRemains(coords: CoordGrid, handling: PlayerDeathHandling) {
        val obj = Obj.fromServer(mapClock, coords, InvObj(BONES_OBJ, 1))
        objRepo.add(obj, handling.dropDuration, handling.revealDelay)
    }

    public fun applyDrops(
        player: Player,
        result: DeathDropResult,
        handling: PlayerDeathHandling,
        coords: CoordGrid,
    ) {
        player.inv.fillNulls()
        player.worn.fillNulls()

        for (item in result.kept) {
            addToInvDirect(player, item)
        }

        for (item in result.supplyPile) {
            dropItem(player, item, coords, handling, receiver = null)
        }

        for (item in result.lostTradeable) {
            dropItem(player, item, coords, handling, handling.dropReceiver)
        }

        when (handling.untradeableHandling) {
            UntradeableHandling.DROP -> {
                for (item in result.lostUntradeable) {
                    dropItem(player, item, coords, handling, handling.dropReceiver)
                }
            }
            UntradeableHandling.COINS -> {
                if (result.coinsForKiller > 0) {
                    val count = result.coinsForKiller.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                    dropItem(player, InvObj(COINS_OBJ, count), coords, handling, handling.dropReceiver)
                }
            }
            UntradeableHandling.DESTROY -> Unit
            UntradeableHandling.KEEP -> Unit
        }
    }

    private fun dropItem(
        player: Player,
        item: InvObj,
        coords: CoordGrid,
        handling: PlayerDeathHandling,
        receiver: Player?,
    ) {
        val obj =
            when {
                receiver != null && receiver !== player -> Obj.fromPvp(receiver, player, item)
                receiver != null -> Obj.fromOwner(receiver, coords, item)
                else -> Obj.fromServer(mapClock, coords, item)
            }
        objRepo.add(obj, handling.dropDuration, handling.revealDelay)
    }

    private fun addToInvDirect(player: Player, item: InvObj) {
        val freeSlot = player.inv.objs.indexOfFirst { it == null }
        if (freeSlot >= 0) {
            player.inv[freeSlot] = item
        }
    }

    private fun sortedCarriedObjs(player: Player): List<InvObj> =
        (player.inv.filterNotNull { true } + player.worn.filterNotNull { true })
            .sortedByDescending { marketPriceSingle(it) }

    private fun isSlotlessKept(obj: InvObj, rules: DeathDropRules): Boolean {
        val type = getInvObj(obj)
        if (type.param(params.bond_item)) return true
        if (!rules.isPvpDeath) {
            if (type.param(params.death_always_kept)) return true
            if (type.param(params.death_auto_protected)) return true
        }
        return false
    }

    public fun isNeverKept(obj: InvObj, rules: DeathDropRules): Boolean = isNeverKeptInternal(obj, rules)

    private fun isNeverKeptInternal(obj: InvObj, rules: DeathDropRules): Boolean {
        val type = getInvObj(obj)
        if (type.param(params.death_never_kept)) return true
        if (rules.isPvpDeath && isRunePouch(type.internalName)) return true
        return false
    }

    private fun isRunePouch(internalName: String): Boolean =
        internalName.startsWith("rune_pouch") || internalName.startsWith("bh_rune_pouch") ||
            internalName.startsWith("divine_rune_pouch")

    private fun isSupplyPileItem(obj: InvObj): Boolean {
        val type = getInvObj(obj)
        return type.interfaceOptions.any { option ->
            option.equals("Eat", ignoreCase = true) || option.equals("Drink", ignoreCase = true)
        }
    }

    private fun isUntradeable(obj: InvObj): Boolean = !getInvObj(obj).tradeable

    private fun marketPriceSingle(obj: InvObj): Long = effectiveDeathValue(getInvObj(obj))

    private fun effectiveDeathValue(type: dev.openrune.types.ItemServerType): Long =
        (marketPrices[type] ?: type.cost).toLong().coerceAtLeast(1)

    private fun untradeableConversionValue(obj: InvObj): Long {
        val type = getInvObj(obj)
        val override = type.paramOrNull(params.death_pvp_coin_value)
        if (override != null && override > 0) return override.toLong()
        return (marketPrices[type] ?: type.cost).toLong()
    }

    public data class DeathDropResult(
        val kept: List<InvObj>,
        val supplyPile: List<InvObj>,
        val lostTradeable: List<InvObj>,
        val lostUntradeable: List<InvObj>,
        val coinsForKiller: Long,
    )

    public companion object {
        public const val DROP_DURATION_STANDARD: Int = 6000
        public const val DROP_DURATION_PVP: Int = 500
        public const val DROP_DURATION_PVP_EXTERNAL: Int = 6100
        public const val DROP_DURATION_REVENANT: Int = 6000
        public const val PVP_REVEAL_DELAY: Int = 100

        private const val COINS_OBJ = "obj.coins"
        private const val BONES_OBJ = "obj.bones"

        public fun standardKeepCount(hasProtectItem: Boolean): Int = if (hasProtectItem) 4 else 3

        public fun wildernessKeepCount(isSkulled: Boolean, hasProtectItem: Boolean): Int {
            val base = if (isSkulled) 0 else 3
            return if (hasProtectItem) base + 1 else base
        }
    }
}
