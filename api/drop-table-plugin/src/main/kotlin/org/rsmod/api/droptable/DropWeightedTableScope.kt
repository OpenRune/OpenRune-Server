package org.rsmod.api.droptable

import dtx.core.Rollable
import dtx.rs.RSWeightedTable
import dtx.rs.RSWeightedTableBuilder
import dtx.rs.SeparateRollAccess
import dtx.rs.group
import org.rsmod.game.entity.Player

public data class DropSpec internal constructor(
    val obj: String,
    val count: IntRange,
) {
    public fun toItem(): DropRollItem = DropRollItem(obj = obj, count = count)
}

public fun drop(spec: DropSpec): DropRollItem = spec.toItem()

public infix fun String.count(value: Int): DropSpec = DropSpec(this, value..value)

public infix fun String.count(range: IntRange): DropSpec = DropSpec(this, range)

@DropTableDsl
public class DropWeightedTableScope internal constructor(
    private val builder: RSWeightedTableBuilder<Player, DropRollItem>,
) {
    private val pendingCounts = mutableListOf<PendingWeightedItemCount>()
    private val pendingSeparateItems = mutableListOf<PendingSeparateItem>()

    internal fun registerPendingCount(item: PendingWeightedItemCount) {
        pendingCounts += item
    }

    internal fun registerPendingSeparateItem(item: PendingSeparateItem) {
        pendingSeparateItems += item
    }

    internal fun flushPendingCounts() {
        for (item in pendingCounts) {
            item.dropIfNeeded()
        }
        pendingCounts.clear()
    }

    internal fun flushPendingSeparateItems() {
        for (item in pendingSeparateItems) {
            item.commitIfNeeded()
        }
        pendingSeparateItems.clear()
    }

    internal fun flushPendingItems() {
        flushPendingCounts()
        flushPendingSeparateItems()
    }

    public infix fun String.count(value: Int): PendingWeightedDrop =
        PendingWeightedDrop(builder, DropSpec(this, value..value))

    public infix fun String.count(range: IntRange): PendingWeightedDrop =
        PendingWeightedDrop(builder, DropSpec(this, range))

    public fun name(tableName: String) {
        builder.name(tableName)
    }

    public fun group(tableName: String, block: DropWeightedTableScope.() -> Unit) {
        builder.group(tableName) {
            DropWeightedTableScope(this).apply {
                block()
                flushPendingItems()
            }
        }
    }

    public infix fun Int.weight(rollable: Rollable<Player, DropRollItem>) {
        builder.run { this@weight weight rollable }
    }

    public infix fun Int.weight(item: DropRollItem) {
        builder.run { this@weight weight item }
    }

    public infix fun Int.weight(table: RSWeightedTable<Player, DropRollItem>) {
        builder.run { this@weight weight table }
    }

    public infix fun Int.weight(obj: String): PendingWeightedItem =
        PendingWeightedItem(builder, this, obj, ::registerPendingCount)

    public infix fun Int.outOf(other: Int): SeparateRollAccess =
        builder.run { this@outOf outOf other }

    public infix fun SeparateRollAccess.separate(objKey: String): PendingSeparateItemBuilder =
        PendingSeparateItemBuilder(builder, numerator, denominator, objKey, ::registerPendingSeparateItem)

    public infix fun SeparateRollAccess.separate(rollable: Rollable<Player, DropRollItem>) {
        builder.apply {
            this@separate separate rollable
        }
    }

    public infix fun SeparateRollAccess.separate(item: DropRollItem) {
        builder.apply {
            this@separate separate item
        }
    }

    public infix fun SeparateRollAccess.separate(spec: DropSpec) {
        builder.apply {
            this@separate separate spec.toItem()
        }
    }

    public infix fun SeparateRollAccess.separate(block: DropWeightedTableScope.() -> Unit) {
        builder.apply {
            this@separate separate {
                DropWeightedTableScope(this).apply {
                    block()
                    flushPendingItems()
                }
            }
        }
    }
}

@DropTableDsl
public class PendingWeightedItem internal constructor(
    private val builder: RSWeightedTableBuilder<Player, DropRollItem>,
    private val weight: Int,
    private val obj: String,
    private val register: (PendingWeightedItemCount) -> Unit,
) {
    public infix fun count(value: Int): PendingWeightedItemCount =
        PendingWeightedItemCount(builder, weight, obj, value..value, register)

    public infix fun count(range: IntRange): PendingWeightedItemCount =
        PendingWeightedItemCount(builder, weight, obj, range, register)
}

@DropTableDsl
public class PendingWeightedItemCount internal constructor(
    private val builder: RSWeightedTableBuilder<Player, DropRollItem>,
    private val weight: Int,
    obj: String,
    count: IntRange = 1..1,
    register: (PendingWeightedItemCount) -> Unit = {},
) : PendingDropItemHandle() {
    override val config: PendingDropItemConfig = PendingDropItemConfig(obj, count)

    init {
        register(this)
    }

    override fun onCommit(item: DropRollItem) {
        builder.run {
            if (item.requiresRollableWrapper()) {
                weight weight dropRollable(item)
            } else {
                weight weight item
            }
        }
    }

    internal fun dropIfNeeded() {
        commitIfNeeded()
    }
}

@DropTableDsl
public class PendingSeparateItemBuilder internal constructor(
    private val builder: RSWeightedTableBuilder<Player, DropRollItem>,
    private val numerator: Int,
    private val denominator: Int,
    private val obj: String,
    private val register: (PendingSeparateItem) -> Unit,
) {
    public infix fun count(value: Int): PendingSeparateItem =
        PendingSeparateItem(builder, numerator, denominator, obj, value..value, register)

    public infix fun count(range: IntRange): PendingSeparateItem =
        PendingSeparateItem(builder, numerator, denominator, obj, range, register)
}

@DropTableDsl
public class PendingSeparateItem internal constructor(
    private val builder: RSWeightedTableBuilder<Player, DropRollItem>,
    private val numerator: Int,
    private val denominator: Int,
    obj: String,
    count: IntRange,
    register: (PendingSeparateItem) -> Unit,
) : PendingDropItemHandle() {
    override val config: PendingDropItemConfig = PendingDropItemConfig(obj, count)

    init {
        register(this)
    }

    override fun onCommit(item: DropRollItem) {
        builder.apply {
            if (item.requiresRollableWrapper()) {
                (numerator outOf denominator) separate dropRollable(item)
            } else {
                (numerator outOf denominator) separate item
            }
        }
    }
}

@DropTableDsl
public class PendingWeightedDrop internal constructor(
    private val builder: RSWeightedTableBuilder<Player, DropRollItem>,
    private var spec: DropSpec,
) {
    public infix fun weight(value: Int) {
        builder.run { value weight spec.toItem() }
    }

    public infix fun separate(numerator: Int): PendingSeparateRate =
        PendingSeparateRate(builder, spec, numerator)
}

@DropTableDsl
public class PendingSeparateRate internal constructor(
    private val builder: RSWeightedTableBuilder<Player, DropRollItem>,
    private val spec: DropSpec,
    private val numerator: Int,
) {
    public infix fun outOf(denominator: Int) {
        builder.apply {
            (numerator outOf denominator) separate spec.toItem()
        }
    }
}

public fun rsPlayerWeightedTable(
    total: Int? = null,
    block: DropWeightedTableScope.() -> Unit,
): RSWeightedTable<Player, DropRollItem> {
    val builder = RSWeightedTableBuilder<Player, DropRollItem>()
    total?.let { builder.pool(it) }
    DropWeightedTableScope(builder).apply {
        block()
        flushPendingItems()
    }
    return builder.build()
}
