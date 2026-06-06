package org.rsmod.api.droptable

import dtx.core.Rollable
import dtx.rs.RSGuaranteedTable
import dtx.rs.RSGuaranteedTableBuilder
import org.rsmod.game.entity.Player

@DropTableDsl
public class DropGuaranteedTableScope internal constructor(
    private val builder: RSGuaranteedTableBuilder<Player, DropRollItem>,
) {
    private val pendingItems = mutableListOf<PendingGuaranteedItem>()

    internal fun registerPendingItem(item: PendingGuaranteedItem) {
        pendingItems += item
    }

    internal fun flushPendingItems() {
        for (item in pendingItems) {
            item.commitIfNeeded()
        }
        pendingItems.clear()
    }

    public infix fun String.count(value: Int): PendingGuaranteedItem =
        PendingGuaranteedItem(builder, this, value..value, ::registerPendingItem)

    public infix fun String.count(range: IntRange): PendingGuaranteedItem =
        PendingGuaranteedItem(builder, this, range, ::registerPendingItem)

    public fun add(rollable: Rollable<Player, DropRollItem>) {
        builder.add(rollable)
    }

    public fun add(item: DropRollItem) {
        builder.add(item)
    }
}

@DropTableDsl
public class PendingGuaranteedItem internal constructor(
    private val builder: RSGuaranteedTableBuilder<Player, DropRollItem>,
    obj: String,
    count: IntRange,
    register: (PendingGuaranteedItem) -> Unit,
) : PendingDropItemHandle() {
    override val config: PendingDropItemConfig = PendingDropItemConfig(obj, count)

    init {
        register(this)
    }

    override fun onCommit(item: DropRollItem) {
        if (item.requiresRollableWrapper()) {
            builder.add(dropRollable(item))
        } else {
            builder.add(item)
        }
    }
}

public fun rsPlayerGuaranteedTable(
    block: DropGuaranteedTableScope.() -> Unit,
): RSGuaranteedTable<Player, DropRollItem> {
    val builder = RSGuaranteedTableBuilder<Player, DropRollItem>()
    DropGuaranteedTableScope(builder).apply {
        block()
        flushPendingItems()
    }
    return builder.build()
}

public fun RSGuaranteedTableBuilder<Player, DropRollItem>.add(spec: DropSpec) {
    add(spec.toItem())
}
