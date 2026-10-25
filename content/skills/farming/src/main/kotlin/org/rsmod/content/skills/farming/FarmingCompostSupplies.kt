package org.rsmod.content.skills.farming

import dev.openrune.types.ItemServerType
import org.rsmod.api.table.farming.FarmingCompostRow
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.isType

internal class CompostBucket(val obj: ItemServerType, val kind: Compost, val empty: ItemServerType) {
    val noted: Boolean get() = obj.certtemplate != 0
}

internal enum class CompostUpgradeKind { POTION, SALTPETRE, ASH }

internal class CompostUpgrade(
    val bucket: ItemServerType,
    val reagent: ItemServerType,
    val cost: Int,
    val output: ItemServerType,
    val remainder: ItemServerType,
    val xp: Double,
    val ticks: Int,
    val repeatTicks: Int,
    val kind: CompostUpgradeKind,
)

internal object FarmingCompostSupplies {
    val ROWS by lazy { FarmingCompostRow.all() }
    val CAPACITY by lazy { checkNotNull(ROWS.first { it.kind == "NONE" }.capacity) }
    val CHARGES_PER_BUCKET by lazy { checkNotNull(ROWS.first { it.kind == "NONE" }.chargesPerBucket) }
    val BUCKETS by lazy {
        ROWS.flatMap { row -> row.buckets.map { CompostBucket(it.t0, Compost.valueOf(it.t1), it.t2) } }
    }
    val UPGRADES by lazy {
        ROWS.flatMap { it.upgrades }.map {
            CompostUpgrade(it.t0, it.t1, it.t2, it.t3, it.t4, it.t5 / 10.0, it.t6, it.t7, CompostUpgradeKind.valueOf(it.t8))
        }
    }
    fun bucket(obj: String): CompostBucket? = BUCKETS.firstOrNull { it.obj.internalName == obj }
}

data class BottomlessCompost(val kind: Compost, val charges: Int)

object FarmingBottomlessCompost {
    const val EMPTY = "obj.bottomless_compost_bucket"
    const val FILLED = "obj.bottomless_compost_bucket_filled"

    val capacity: Int get() = FarmingCompostSupplies.CAPACITY
    val chargesPerBucket: Int get() = FarmingCompostSupplies.CHARGES_PER_BUCKET

    fun isBucket(obj: String): Boolean = obj == EMPTY || obj == FILLED

    fun contents(obj: InvObj?): BottomlessCompost? {
        if (obj.isType(EMPTY) && obj.count == 1 && obj.vars == 0) return BottomlessCompost(Compost.NONE, 0)
        if (!obj.isType(FILLED) || obj.count != 1) return null
        val kind = Compost.entries[obj.vars and KIND_MASK]
        val charges = obj.vars ushr KIND_BITS
        if (kind == Compost.NONE || charges !in 1..capacity) return null
        return BottomlessCompost(kind, charges)
    }

    fun bucket(kind: Compost, charges: Int): InvObj {
        require(charges in 0..capacity)
        if (charges == 0) return InvObj(EMPTY)
        require(kind != Compost.NONE)
        return InvObj(FILLED, vars = (charges shl KIND_BITS) or kind.ordinal)
    }

    private const val KIND_BITS = 2
    private const val KIND_MASK = 3
}
