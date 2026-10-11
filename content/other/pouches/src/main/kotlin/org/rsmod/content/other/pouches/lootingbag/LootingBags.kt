package org.rsmod.content.other.pouches.lootingbag

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.util.UncheckedType
import org.rsmod.api.attr.AttributeKey
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory

object LootingBags {
    const val CLOSED = "obj.looting_bag"
    const val OPEN = "obj.looting_bag_open"
    const val INV = "inv.looting_bag"
    const val CAPACITY = 28

    val ALL = listOf(CLOSED, OPEN)

    val ids: Set<Int> by lazy { ALL.map { it.asRSCM(RSCMType.OBJ) }.toSet() }
    val openId: Int by lazy { OPEN.asRSCM(RSCMType.OBJ) }
    private val coinsId: Int by lazy { "obj.coins".asRSCM(RSCMType.OBJ) }

    /** Why [obj] cannot go into the bag, or `null` when it can. */
    fun rejection(obj: InvObj, type: ItemServerType): String? =
        when {
            obj.id in ids -> "You may be surprised to learn that bagception is not permitted."
            !type.tradeable -> "Only tradeable items can be put in the bag."
            else -> null
        }
}

/**
 * The bag contents live in the cache's `inv.looting_bag`, which is a temporary inv, so they are
 * mirrored into this persistent attribute (versioned `[id, count, vars, ...]` (legacy pairs are still read)) whenever
 * they change and restored on login.
 */
val LOOTING_BAG_ATTR: AttributeKey<MutableList<Int>> = AttributeKey(persistenceKey = "looting_bag")

val Player.lootingBag: Inventory
    get() = invMap.getOrPut(LootingBags.INV)

fun Player.hasLootingBag(): Boolean = inv.any { it != null && it.id in LootingBags.ids }

fun Player.hasOpenLootingBag(): Boolean = inv.any { it != null && it.id == LootingBags.openId }

fun Player.saveLootingBag() {
    val encoded = LootingBagCodec.encode(lootingBag.objs)
    if (encoded.isEmpty()) {
        attr.remove(LOOTING_BAG_ATTR)
    } else {
        attr[LOOTING_BAG_ATTR] = encoded
    }
}

fun Player.restoreLootingBag() {
    val saved = attr[LOOTING_BAG_ATTR] ?: return
    val bag = lootingBag
    val decoded = LootingBagCodec.decode(saved, bag.size)
    for (slot in bag.indices) {
        bag[slot] = decoded[slot]
    }
}

object LootingBagCodec {
    private const val MARKER = -1
    private const val VERSION = 1
    fun encode(objs: Array<InvObj?>): MutableList<Int> {
        val items = objs.filterNotNull()
        if (items.isEmpty()) return mutableListOf()
        val encoded = arrayListOf(MARKER, VERSION)
        for (obj in items) { encoded.add(obj.id); encoded.add(obj.count); encoded.add(obj.vars) }
        return encoded
    }

    /** Preserve legacy player saves and signed/unsigned item state without merging different copies. */
    @OptIn(UncheckedType::class)
    fun decode(encoded: List<Int>, size: Int): Array<InvObj?> {
        val objs = arrayOfNulls<InvObj>(size)
        val versioned = encoded.firstOrNull() == MARKER
        if (versioned && encoded.getOrNull(1) != VERSION) return objs
        val stride = if (versioned) 3 else 2
        var index = if (versioned) 2 else 0
        var slot = 0
        while (index + stride <= encoded.size && slot < size) {
            val id = encoded[index]; val count = encoded[index + 1]; val vars = if (versioned) encoded[index + 2] else 0
            index += stride
            if (id <= 0 || count <= 0) continue
            objs[slot++] = InvObj(id, count, vars)
        }
        return objs
    }
}
