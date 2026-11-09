package org.rsmod.content.other.grandexchange

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.concurrent.ConcurrentLinkedQueue
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.output.mes
import org.rsmod.api.server.config.ServerConfig
import org.rsmod.content.other.grandexchange.market.GeAccount
import org.rsmod.content.other.grandexchange.market.GeBook
import org.rsmod.content.other.grandexchange.market.GeHistoryEntry
import org.rsmod.content.other.grandexchange.market.GeMarketRules
import org.rsmod.content.other.grandexchange.market.GeOffer
import org.rsmod.content.other.grandexchange.market.GeOfferType
import org.rsmod.content.other.grandexchange.market.GeOwner
import org.rsmod.content.other.grandexchange.persistence.GePersistence
import org.rsmod.content.other.grandexchange.persistence.GeRow
import org.rsmod.content.other.grandexchange.persistence.GeStoredBook
import org.rsmod.content.other.grandexchange.prices.GePrices
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.uncert

/**
 * Owns the [GeBook] and everything that crosses its edge: coins and items moving between a
 * player's inventory and their offers, offer state reaching the database, and fills reaching the
 * client.
 *
 * Durability: a player's offer rows are saved inside their character save, in the same
 * transaction as their inventory, so a placement or a collection is never half-saved. An offer is
 * [GeOffer.locked] out of matching until that save lands, so a fill written on its own can never
 * carry an unsaved inventory change. Fills write every offer they touch in one transaction.
 */
@Singleton
class GrandExchange
@Inject
constructor(
    private val prices: GePrices,
    private val persistence: GePersistence,
    serverConfig: ServerConfig,
) {
    private val logger = InlineLogger()

    val config = serverConfig.gameplay.grandExchange

    val book =
        GeBook(
            prices::guide,
            GeMarketRules(
                wiggleRoomPercent = config.wiggleRoomPercent.coerceAtLeast(0.0),
                gradual = config.fillMode.equals("gradual", ignoreCase = true),
                gradualFillPercent = config.gradualFillPercent.coerceIn(0.1, 100.0),
            ),
        )

    var ready: Boolean = false
        private set

    private val online = HashMap<GeOwner, Player>()
    private val pendingMatch = LinkedHashSet<GeOffer>()
    private val saveResults = ConcurrentLinkedQueue<SaveResult>()
    private val lockTokens = HashMap<GeOffer, Long>()
    private var nextLockToken = 1L
    private var ticksUntilSimulation = 0

    fun owner(player: Player): GeOwner = GeOwner(player.worldType.key, player.characterId)

    fun account(player: Player): GeAccount = book.account(owner(player))

    fun load() {
        if (ready) {
            return
        }
        persistence.load(::restore)
    }

    fun restore(stored: GeStoredBook) {
        for (row in stored.rows) {
            val account = book.account(row.owner)
            val offer = row.offer?.toOffer(row.owner, row.slot)
            if (offer != null) {
                book.restore(offer, row.version)
                pendingMatch += offer
            } else {
                account.versions[row.slot] = maxOf(account.versions[row.slot], row.version)
            }
        }
        for ((owner, entries) in stored.history) {
            val account = book.account(owner)
            entries.asReversed().forEach(account::record)
        }
        ready = true
        logger.info {
            "Grand Exchange: ${stored.rows.count { it.offer != null }} offers loaded; " +
                "${prices.snapshotCount} stored prices."
        }
        for (player in online.values) {
            GeClientSync.syncAll(player, account(player))
        }
    }

    fun login(player: Player) {
        online[owner(player)] = player
        if (ready) {
            GeClientSync.syncAll(player, account(player))
        }
    }

    fun logout(player: Player) {
        online.remove(owner(player))
    }

    fun guidePrice(obj: Int): Long? = prices.guide(obj)

    fun placeBuy(player: Player, slot: Int, obj: Int, count: Int, price: Long): String? {
        val type = uncert(ServerCacheManager.getItem(obj) ?: return "You can't buy that.")
        validate(player, slot, type, count, price)?.let { return it }
        if (price > Int.MAX_VALUE / count) {
            return "You don't have enough coins."
        }
        val total = (price * count).toInt()
        val coins = player.inv.held(COINS)
        if (coins < total) {
            return "You don't have enough coins."
        }
        val taken = player.invTransaction(player.inv) {
            delete(select(player.inv), COINS, total, slot = null, strict = true, placehold = false)
        }
        if (!taken.success) {
            return "You don't have enough coins."
        }
        val offer = book.place(owner(player), slot, GeOfferType.Buy, type.id, price, count)
        lock(player, offer)
        GeClientSync.lastOffer(player, GeHistoryEntry(GeOfferType.Buy, type.id, count, price * count))
        GeClientSync.syncSlot(player, account(player), slot)
        return null
    }

    fun placeSell(player: Player, slot: Int, obj: Int, count: Int, price: Long): String? {
        val held = ServerCacheManager.getItem(obj) ?: return "You can't sell that."
        val type = uncert(held)
        validate(player, slot, type, count, price)?.let { return it }
        val noted = notedOf(type)
        val plain = player.inv.held(type.id)
        val notes = noted?.let { player.inv.held(it) } ?: 0
        if (plain.toLong() + notes < count) {
            return "You don't have that many to sell."
        }
        val fromPlain = minOf(plain, count)
        val fromNotes = count - fromPlain
        val taken = player.invTransaction(player.inv) {
            val inv = select(player.inv)
            if (fromPlain > 0) {
                delete(inv, type.id, fromPlain, slot = null, strict = true, placehold = false)
            }
            if (fromNotes > 0 && noted != null) {
                delete(inv, noted, fromNotes, slot = null, strict = true, placehold = false)
            }
        }
        if (!taken.success) {
            return "You don't have that many to sell."
        }
        val offer = book.place(owner(player), slot, GeOfferType.Sell, type.id, price, count)
        lock(player, offer)
        GeClientSync.lastOffer(player, GeHistoryEntry(GeOfferType.Sell, type.id, count, price * count))
        GeClientSync.syncSlot(player, account(player), slot)
        return null
    }

    fun abort(player: Player, slot: Int): String? {
        if (!ready) return STARTING
        val offer = book.offer(owner(player), slot) ?: return null
        if (!book.abort(offer)) {
            return null
        }
        pendingMatch -= offer
        player.mes(
            "Abort request acknowledged. Please be aware that your offer may have already been " +
                "completed.",
        )
        if (offer.locked) {
            requestSave(player)
        } else {
            persist(listOf(offer))
        }
        GeClientSync.syncSlot(player, account(player), slot)
        return null
    }

    /**
     * Moves an offer's collection box into the inventory, or the bank when [toBank]. Each part
     * that fits is moved whole; coins stop at the inventory's coin cap and the rest stays behind.
     */
    fun collect(
        player: Player,
        slot: Int,
        items: Boolean = true,
        coins: Boolean = true,
        toBank: Boolean = false,
        noted: Boolean = true,
    ): String? {
        if (!ready) return STARTING
        val offer = book.offer(owner(player), slot) ?: return null
        val target = if (toBank) player.bank else player.inv
        var failure: String? = null
        var moved = false
        if (items && offer.collectItems > 0) {
            val type = ServerCacheManager.getItem(offer.obj)
            val give = if (!toBank && noted) notedOf(type) ?: offer.obj else offer.obj
            val amount = offer.collectItems
            val added = player.invAdd(target, give, amount, strict = true)
            if (added.success) {
                book.takeItems(offer, amount)
                moved = true
            } else {
                failure = noSpace(toBank)
            }
        }
        if (coins && offer.collectCoins > 0) {
            val room = Int.MAX_VALUE.toLong() - target.held(COINS)
            val amount = minOf(offer.collectCoins, room).toInt()
            val added =
                if (amount > 0) player.invAdd(target, COINS, amount, strict = true) else null
            if (added != null && added.success) {
                book.takeCoins(offer, amount.toLong())
                moved = true
            } else {
                failure = failure ?: noSpace(toBank)
            }
        }
        if (moved) {
            book.clearIfDone(offer)
            if (offer.active) {
                lock(player, offer)
            } else {
                lockTokens -= offer
                requestSave(player)
            }
            GeClientSync.syncSlot(player, account(player), slot)
        }
        return failure
    }

    fun collectAll(player: Player, toBank: Boolean): String? {
        var failure: String? = null
        for (slot in 0 until GeAccount.SLOT_COUNT) {
            val offer = book.offer(owner(player), slot) ?: continue
            if (!offer.collectable) continue
            failure = collect(player, slot, toBank = toBank) ?: failure
        }
        return failure
    }

    /** Runs once per game cycle: lands confirmed saves, then trades whatever became matchable. */
    fun tick() {
        if (!ready) {
            return
        }
        drainSaveResults()
        if (prices.consumeRefreshed()) {
            pendingMatch += book.activeOffers()
        }
        if (book.rules.gradual && --ticksUntilSimulation <= 0) {
            ticksUntilSimulation = (config.gradualFillSeconds.coerceAtLeast(1) * 1000) / 600
            pendingMatch += book.activeOffers()
        }
        if (pendingMatch.isEmpty()) {
            return
        }
        val changed = LinkedHashSet<GeOffer>()
        val batch = pendingMatch.toList()
        pendingMatch.clear()
        for (offer in batch) {
            changed += book.match(offer)
        }
        if (changed.isEmpty()) {
            return
        }
        persist(changed)
        for (offer in changed) {
            val player = online[offer.owner] ?: continue
            GeClientSync.syncSlot(player, account(player), offer.slot)
            GeClientSync.fillMessage(player, offer)
        }
    }

    private fun validate(
        player: Player,
        slot: Int,
        type: ItemServerType,
        count: Int,
        price: Long,
    ): String? {
        if (!ready) return STARTING
        if (slot !in 0 until GeAccount.SLOT_COUNT) return "That slot can't be used."
        if (book.offer(owner(player), slot) != null) return "That slot is already in use."
        if (!tradeable(type)) return "You can't trade that item on the Grand Exchange."
        if (count <= 0) return "You must choose a quantity."
        if (price <= 0 || price > MAX_PRICE) return "You must choose a price."
        return null
    }

    private fun lock(player: Player, offer: GeOffer) {
        offer.locked = true
        val token = nextLockToken++
        lockTokens[offer] = token
        val owner = offer.owner
        val slot = offer.slot
        persistence.saveCharacter(player) { saved ->
            saveResults += SaveResult(owner, slot, token, saved)
        }
    }

    private fun requestSave(player: Player) {
        persistence.saveCharacter(player) {}
    }

    private fun drainSaveResults() {
        while (true) {
            val result = saveResults.poll() ?: break
            val offer = book.offer(result.owner, result.slot) ?: continue
            if (lockTokens[offer] != result.token) {
                continue
            }
            if (result.saved) {
                lockTokens -= offer
                offer.locked = false
                pendingMatch += offer
                continue
            }
            val player = online[result.owner]
            if (player != null) {
                lock(player, offer)
            } else {
                logger.error { "Grand Exchange: save failed, offer stays held: $offer" }
            }
        }
    }

    private fun persist(offers: Collection<GeOffer>) {
        persistence.write(offers.map { GeRow.of(book.account(it.owner), it.slot) })
    }

    private fun noSpace(toBank: Boolean): String =
        if (toBank) "You don't have enough space in your bank." else "You don't have enough inventory space."

    private val Player.bank: Inventory
        get() = invMap.getOrPut("inv.bank")

    private data class SaveResult(
        val owner: GeOwner,
        val slot: Int,
        val token: Long,
        val saved: Boolean,
    )

    companion object {
        const val COINS_NAME = "obj.coins"
        val COINS: Int by lazy { COINS_NAME.asRSCM(RSCMType.OBJ) }
        const val MAX_PRICE = Int.MAX_VALUE.toLong()
        private const val STARTING = "The Grand Exchange is still opening. Try again in a moment."

        fun tradeable(type: ItemServerType): Boolean =
            type.tradeable && !type.isCert && !type.isPlaceholder && type.id != COINS

        fun notedOf(type: ItemServerType?): Int? {
            if (type == null || !type.canCert) return null
            return type.certlink.takeIf { it > 0 }
        }

        fun historyOf(account: GeAccount): List<GeHistoryEntry> = account.history.toList()

        fun Inventory.held(obj: Int): Int =
            physicalCount(RSCM.getReverseMapping(RSCMType.OBJ, obj))
    }
}
