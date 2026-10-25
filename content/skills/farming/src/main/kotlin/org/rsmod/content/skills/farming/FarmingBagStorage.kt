package org.rsmod.content.skills.farming

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.InvStackType
import dev.openrune.types.ItemServerType
import dev.openrune.types.enums.EnumTypeMap
import org.rsmod.api.enums.FarmingEnums
import org.rsmod.api.enums.FarmingSeedEnums
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.getInvObj
import org.rsmod.objtx.TransactionResultList

/**
 * A storage item whose contents live in the player's persisted [inv]; [closed] and [open] are the
 * held item variants. Store slots are never compacted, so their order is the player's order.
 */
class FarmingBag internal constructor(
    val inv: String,
    val closed: List<String>,
    val open: List<String>,
    private val limitOf: (bag: String) -> Int,
    private val items: () -> EnumTypeMap<Int, ItemServerType>,
) {
    val variants: List<String> = closed + open

    /** Storable objs in their cache enum order. */
    val storable: List<ItemServerType> by lazy {
        items().backing.entries.sortedBy { it.key }.mapNotNull { it.value }
    }

    private val storableIds: Set<Int> by lazy { storable.mapTo(HashSet()) { it.id } }

    fun isStorable(obj: Int): Boolean = obj in storableIds

    fun store(player: Player): Inventory = player.invMap.getOrPut(inv)

    fun count(player: Player, obj: String): Int = store(player).countOf(obj.asRSCM(RSCMType.OBJ))

    fun remove(player: Player, obj: String, count: Int): Boolean {
        require(count > 0) { "count must be positive: $count" }
        val store = store(player)
        val id = obj.asRSCM(RSCMType.OBJ)
        return player.invTransaction(store) { delete(select(store), id, count) }.success
    }

    fun limit(bag: String): Int = limitOf(bag)

    fun held(player: Player): String? = variants.firstOrNull { it in player.inv }

    fun openHeld(player: Player): String? = open.firstOrNull { it in player.inv }

    /** How many more of [obj] fit while [bag] is the variant being used. */
    fun room(player: Player, bag: String, obj: Int): Int {
        if (!isStorable(obj)) return 0
        val store = store(player)
        val stored = store.countOf(obj)
        if (stored == 0 && !store.hasFreeSpace()) return 0
        return (limit(bag) - stored).coerceAtLeast(0)
    }

    /** Adds [count] of [obj] to the bag, taking them from [from] when given; all or nothing. */
    internal fun put(
        player: Player,
        obj: Int,
        count: Int,
        from: Inventory? = null,
        autoCommit: Boolean = true,
    ): TransactionResultList<InvObj> {
        val store = store(player)
        return player.invTransaction(from ?: store, from?.let { store }, autoCommit) {
            val into = select(store)
            if (from != null) delete(select(from), obj, count)
            add(into, obj, count)
        }
    }

    /** Moves [count] of [obj] out of the bag into [into]; all or nothing. */
    internal fun take(player: Player, obj: Int, count: Int, into: Inventory): Boolean {
        val store = store(player)
        return player.invTransaction(store, into) {
            val from = select(store)
            val target = select(into)
            delete(from, obj, count)
            add(target, obj, count)
        }.success
    }

    /** Clears the contents once no variant of this bag remains in the inventory, worn or bank. */
    internal fun clearIfGone(player: Player) {
        val bank = player.invMap.getOrPut("inv.bank")
        if (variants.any { it in player.inv || it in player.worn || it in bank }) return
        val store = store(player)
        for (slot in store.indices) store[slot] = null
    }
}

object FarmingBags {
    private const val HERB_SACK_LIMIT = 30
    private const val SILKLINED_HERB_SACK_LIMIT = 100
    private const val REAGENT_POUCH_LIMIT = 26

    const val HERB_SACK = "obj.slayer_herb_sack"
    const val HERB_SACK_OPEN = "obj.slayer_herb_sack_open"
    const val SILKLINED_HERB_SACK = "obj.slayer_herb_sack_silk"
    const val SILKLINED_HERB_SACK_OPEN = "obj.slayer_herb_sack_silk_open"
    const val REAGENT_POUCH = "obj.mm_secondary_pouch"
    const val REAGENT_POUCH_OPEN = "obj.mm_secondary_pouch_open"
    const val SEED_BOX = "obj.seed_box"
    const val SEED_BOX_OPEN = "obj.seed_box_open"

    private val SILKLINED = setOf(SILKLINED_HERB_SACK, SILKLINED_HERB_SACK_OPEN)

    val herbSack =
        FarmingBag(
            inv = "inv.farming_herb_sack",
            closed = listOf(HERB_SACK, SILKLINED_HERB_SACK),
            open = listOf(HERB_SACK_OPEN, SILKLINED_HERB_SACK_OPEN),
            limitOf = { if (it in SILKLINED) SILKLINED_HERB_SACK_LIMIT else HERB_SACK_LIMIT },
        ) {
            FarmingEnums.farming_herb_sack_items
        }

    val reagentPouch =
        FarmingBag(
            inv = "inv.farming_reagent_pouch",
            closed = listOf(REAGENT_POUCH),
            open = listOf(REAGENT_POUCH_OPEN),
            limitOf = { REAGENT_POUCH_LIMIT },
        ) {
            FarmingEnums.farming_reagent_pouch_items
        }

    val seedBox =
        FarmingBag(
            inv = "inv.seed_box",
            closed = listOf(SEED_BOX),
            open = listOf(SEED_BOX_OPEN),
            limitOf = { Int.MAX_VALUE },
        ) {
            FarmingSeedEnums.farming_seed_box_items
        }

    fun isSilklined(bag: String): Boolean = bag in SILKLINED

    fun herbSackName(bag: String): String = if (isSilklined(bag)) "silklined herb sack" else "herb sack"

    /** The first seed, in the player's box order, that [suitable] accepts. */
    fun firstSuitableSeed(player: Player, suitable: (ItemServerType) -> Boolean): ItemServerType? =
        seedBox.store(player).firstNotNullOfOrNull { obj -> obj?.let(::getInvObj)?.takeIf(suitable) }

    /** Puts Master Farmer loot straight into an open seed box when all of it fits. */
    fun storePickpocketSeed(
        player: Player,
        obj: String,
        count: Int,
        autoCommit: Boolean = true,
    ): Boolean {
        val box = seedBox.openHeld(player) ?: return false
        val id = obj.asRSCM(RSCMType.OBJ)
        if (seedBox.room(player, box, id) < count) return false
        return seedBox.put(player, id, count, autoCommit = autoCommit).success
    }
}

internal fun Inventory.countOf(obj: Int): Int {
    var total = 0L
    for (slot in indices) {
        val held = this[slot] ?: continue
        if (held.id == obj) total += held.count
    }
    return total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}

/** How many of [type] can be added to this inventory. */
internal fun Inventory.fits(type: ItemServerType, count: Int): Int {
    if (type.isStackable || this.type.stack == InvStackType.Always) {
        val stored = countOf(type.id)
        if (stored == 0 && !hasFreeSpace()) return 0
        return minOf(count.toLong(), Int.MAX_VALUE.toLong() - stored).toInt()
    }
    return minOf(count, freeSpace())
}
