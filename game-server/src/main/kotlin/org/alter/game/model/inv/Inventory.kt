package org.alter.game.model.inv

import dev.openrune.ServerCacheManager

import dev.openrune.types.InvStackType
import dev.openrune.types.InventoryServerType
import dev.openrune.types.ItemServerType
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import org.alter.game.model.container.ItemTransaction
import org.alter.game.model.inv.invtx.add
import org.alter.game.model.inv.invtx.delete
import org.alter.game.model.inv.invtx.toTransactionStackType
import org.alter.game.model.inv.invtx.transactions
import org.alter.game.model.inv.objtx.Transaction
import org.alter.game.model.inv.objtx.TransactionInventory
import org.alter.game.model.inv.objtx.TransactionResultList
import org.alter.game.model.inv.objtx.isOk
import org.alter.game.model.item.Item
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.asRSCM
import org.alter.rscm.RSCM.getRSCM
import org.alter.rscm.RSCMType
import java.util.BitSet
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

public class Inventory(public val type: InventoryServerType, public val objs: Array<Item?>) : Iterable<Item?> {
    public val modifiedSlots: BitSet = BitSet()

    public val size: Int
        get() = objs.size

    public val indices: IntRange
        get() = objs.indices

    public fun isNotEmpty(): Boolean = !isEmpty()

    public fun isEmpty(): Boolean = objs.all { it == null }

    public fun isFull(): Boolean = objs.all { it != null }

    public fun freeSpace(): Int = objs.count { it == null }

    public fun occupiedSpace(): Int = objs.count { it != null }

    val freeSlotCount = freeSpace()

    public fun hasFreeSpace(): Boolean = objs.any { it == null }

    public fun lastOccupiedSlot(): Int = indexOfLast { it != null } + 1

    /**
     * Maps and returns the indices (`slots`) of objs in the inventory that satisfy the given
     * [predicate].
     *
     * The [predicate] function is invoked for each slot with its index and the obj at that slot as
     * parameters.
     *
     * **Example Usage:** Find all slots with non-null objs
     *
     * ```
     * val occupiedSlots = inventory.mapSlots { _, obj -> obj != null }
     * ```
     *
     * @param predicate A lambda that takes the slot and the obj and returns `true` for slots to
     *   map.
     * @return A [Set] of slot indices where the objects satisfy the given [predicate].
     */
    public fun mapSlots(predicate: (Int, Item?) -> Boolean): Set<Int> =
        objs.mapSlotsTo(IntOpenHashSet(), predicate)

    public fun filterNotNull(predicate: (Item) -> Boolean): List<Item> =
        objs.mapNotNull { if (it != null && predicate(it)) it else null }

    public fun fillNulls() {
        for (i in objs.indices) {
            if (objs[i] == null) {
                continue
            }
            objs[i] = null
            modifiedSlots.set(i)
        }
    }

    public fun hasModifiedSlots(): Boolean = !modifiedSlots.isEmpty

    public fun clearModifiedSlots() {
        modifiedSlots.clear()
    }

    public fun getValue(slot: Int): Item =
        this[slot] ?: throw NoSuchElementException("Slot $slot is missing in the inv.")

    public operator fun get(slot: Int): Item? = objs.getOrNull(slot)

    public operator fun set(slot: Int, obj: Item?) {
        objs[slot] = obj
        modifiedSlots.set(slot)
    }

    fun getItemCount(itemId: String): Int {
        RSCM.requireRSCM(RSCMType.OBJTYPES, itemId)
        return getItemCount(itemId.asRSCM())
    }

    fun getItemCount(itemId: Int) = count(itemId)

    fun add(item : Item) = add(item.id,item.amount)

    fun hasAt(slot: Int, itemId: Int): Boolean =
        this[slot]?.id == itemId

    fun add(
        item: Int,
        amount: Int = 1,
        assureFullInsertion: Boolean = true,
        forceNoStack: Boolean = false,
        beginSlot: Int? = null
    ): ItemTransaction {

        val transaction = invAdd(obj = item, count = amount, slot = beginSlot, strict = assureFullInsertion).results.first()

        return if (transaction.isOk()) {
            ItemTransaction(transaction.requested,transaction.completed, emptyList())
        } else {
            ItemTransaction(amount,0, emptyList())
        }

    }


    fun add(
        item: String,
        amount: Int = 1,
        assureFullInsertion: Boolean = true,
        forceNoStack: Boolean = false,
        beginSlot: Int? = null
    ): ItemTransaction {
        RSCM.requireRSCM(RSCMType.OBJTYPES, item)
        val transaction = invAdd(obj = item.asRSCM(), count = amount, slot = beginSlot, strict = assureFullInsertion).results.first()
        return if (transaction.isOk()) {
            ItemTransaction(transaction.requested,transaction.completed, emptyList())
        } else {
            ItemTransaction(amount,0, emptyList())
        }
    }

    fun removeAll() {
        objs.filterNotNull().forEach {
            invDel(it.id,it.amount)
        }
    }


    fun remove(item : Item) = remove(item.id,item.amount)

    fun remove(
        item: String,
        amount: Int = 1,
        assureFullRemoval: Boolean = false,
        beginSlot: Int? = null
    ): ItemTransaction {
        return remove(getRSCM(item), amount, assureFullRemoval, beginSlot)
    }


    fun remove(
        item: Int,
        amount: Int = 1,
        assureFullRemoval: Boolean = false,
        beginSlot: Int? = null
    ): ItemTransaction {
        val transaction = invDel(obj = item, count = amount, slot = beginSlot, strict = assureFullRemoval).results.first()
        return if (transaction.isOk()) {
            ItemTransaction(transaction.requested,transaction.completed, emptyList())
        } else {
            ItemTransaction(amount,0, emptyList())
        }
    }

    fun replace(oldItem: Int, newItem: Int, slot: Int? = null): Boolean {
        val count = count(oldItem)
        invDel(obj = oldItem, count = count,slot = slot)
        invAdd(obj = newItem, count = count, slot = slot)
        return true
    }


    fun getItemIndex(id: Int, skipAttrItems: Boolean): Int {
        return objs.indexOfFirst { item ->
            item != null && item.id == id && !skipAttrItems
        }
    }

    public operator fun contains(id: Int): Boolean = objs.any { it?.id == id }

    public fun contains(vararg itemIds: String): Boolean =
        itemIds.all { id -> objs.any { it?.id == getRSCM(id) } }

    public fun contains(vararg itemIds: Item): Boolean =
        itemIds.all { item -> getItemCount(item.id) >= item.amount }

    public fun containsAny(vararg itemIds: String): Boolean =
        itemIds.any { id ->
            val rscm = getRSCM(id)
            objs.any { it?.id == rscm }
        }

    public operator fun contains(type: ItemServerType): Boolean = objs.any { type.isAssociatedWith(it) }

    public fun count(objType: ItemServerType): Int {
        val obj = objs.firstOrNull { it?.id == objType.id } ?: return 0
        val singleStack = type.stack == InvStackType.Always || objType.stackable
        if (singleStack) {
            return obj.amount
        }
        return individualCount(obj)
    }

    public fun count(id: Int): Int {
        val obj = objs.firstOrNull { it?.id == id } ?: return 0
        val singleStack = type.stack == InvStackType.Always || ServerCacheManager.getItem(id)?.stackable?: false
        if (singleStack) {
            return obj.amount
        }
        return individualCount(obj)
    }

    public fun count(obj: Item, objType: ItemServerType): Int {
        val singleStack = type.stack == InvStackType.Always || objType.stackable
        if (singleStack) {
            return obj.amount
        }
        return individualCount(obj)
    }

    private fun individualCount(obj: Item): Int {
        var count = 0
        for (i in objs.indices) {
            val other = objs[i] ?: continue
            if (other.id == obj.id) {
                count += other.amount
            }
        }
        return count
    }

    override fun iterator(): Iterator<Item?> = objs.iterator()

    override fun toString(): String = "Inventory(type=$type, objs=${(objs.mapNotNullEntries())})"

    public companion object {

        public fun create(type: InventoryServerType): Inventory {
            val objs = arrayOfNulls<Item>(type.size)
            if (type.stock != null) {
                for (i in type.stock.indices) {
                    val copy = type.stock[i] ?: continue
                    objs[i] = Item(copy.obj, copy.count)
                }
            }
            return Inventory(type, objs)
        }

        private inline fun <T : MutableCollection<Int>> Array<Item?>.mapSlotsTo(
            destination: T,
            predicate: (Int, Item?) -> Boolean,
        ): T {
            for (slot in indices) {
                val obj = this[slot]
                if (predicate(slot, obj)) {
                    destination.add(slot)
                }
            }
            return destination
        }

        private fun Array<Item?>.mapNotNullEntries(): List<Pair<Int, Item>> =
            mapIndexedNotNull { slot, obj ->
                obj?.let { slot to obj }
            }
    }

    //Dont like this dupe code but needed to keep api the same

    public fun invAdd(
        obj: Int,
        count: Int,
        vars: Int = 0,
        slot: Int? = null,
        strict: Boolean = true,
        cert: Boolean = false,
        uncert: Boolean = false,
        autoCommit: Boolean = true,
    ): TransactionResultList<Item> =
        invTransaction(autoCommit) {
            val targetInv = select()
            add(
                inv = targetInv,
                obj = obj,
                count = count,
                vars = vars,
                slot = slot,
                strict = strict,
                cert = cert,
                uncert = uncert,
            )
        }

    public fun invTransaction(
        from: Inventory,
        into: Inventory?,
        autoCommit: Boolean = true,
        transaction: Transaction<Item>.() -> Unit,
    ): TransactionResultList<Item> {
        val result = transactions.transaction(autoCommit) { transaction() }
        return result
    }

    public fun invTransaction(
        autoCommit: Boolean = true,
        transaction: Transaction<Item>.() -> Unit,
    ): TransactionResultList<Item> =
        invTransaction(from = this, into = null, autoCommit = autoCommit, transaction = transaction)

    public fun Transaction<Item>.select(): TransactionInventory<Item> {
        val image = Array(objs.size) { input(objs[it]) }
        val stack = type.stack.toTransactionStackType()
        val transformed =
            TransactionInventory(stack, objs, image, type.placeholders, modifiedSlots)
        register(transformed)
        return transformed
    }

    public fun invDel(
        obj: Int,
        count: Int,
        slot: Int? = null,
        strict: Boolean = true,
        placehold: Boolean = false,
        autoCommit: Boolean = true,
    ): TransactionResultList<Item> =
        invTransaction(autoCommit) {
            val targetInv = select()
            delete(
                inv = targetInv,
                obj = obj,
                count = count,
                slot = slot,
                strict = strict,
                placehold = placehold,
            )
        }


}


@OptIn(ExperimentalContracts::class)
public fun ItemServerType.isAssociatedWith(obj: Item?): Boolean {
    contract { returns(true) implies (obj != null) }
    return obj != null && obj.id == id
}

@OptIn(ExperimentalContracts::class)
public fun ItemServerType?.isType(other: ItemServerType): Boolean {
    contract { returns(true) implies (this@isType != null) }
    return this != null && this.id == other.id
}

@OptIn(ExperimentalContracts::class)
public fun ItemServerType?.isAnyType(type1: ItemServerType, type2: ItemServerType): Boolean {
    contract { returns(true) implies (this@isAnyType != null) }
    return this != null && (type1.id == id || type2.id == id)
}

@OptIn(ExperimentalContracts::class)
public fun ItemServerType?.isAnyType(type1: ItemServerType, type2: ItemServerType, type3: ItemServerType): Boolean {
    contract { returns(true) implies (this@isAnyType != null) }
    return this != null && (type1.id == id || type2.id == id || type3.id == id)
}