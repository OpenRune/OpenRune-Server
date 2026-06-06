package org.rsmod.api.droptable

import dtx.core.Rollable
import dtx.rs.RSPreRollTable
import dtx.rs.RSPrerollTableBuilder
import org.rsmod.game.entity.Player

@DropTableDsl
public class DropChanceTableScope internal constructor(
    private val builder: RSPrerollTableBuilder<Player, DropRollItem>,
    private val rollStyle: ChanceRollStyle,
) {
    private val pendingRateFirstItems = mutableListOf<PendingRateFirstItem>()

    internal fun registerPendingRateFirstItem(item: PendingRateFirstItem) {
        pendingRateFirstItems += item
    }

    internal fun flushPendingRateFirstItems() {
        for (item in pendingRateFirstItems) {
            item.commitIfNeeded()
        }
        pendingRateFirstItems.clear()
    }

    public fun group(name: String, block: DropChanceTableScope.() -> Unit) {
        block()
    }

    public infix fun Int.outOf(denominator: Int): PendingRateFirstAccess =
        PendingRateFirstAccess(builder, this, denominator, rollStyle, ::registerPendingRateFirstItem)

    public fun onBuilder(block: RSPrerollTableBuilder<Player, DropRollItem>.() -> Unit) {
        builder.block()
    }
}

internal enum class ChanceRollStyle {
    Chance,
    Rolls,
}

@DropTableDsl
public class PendingRateFirstAccess internal constructor(
    private val builder: RSPrerollTableBuilder<Player, DropRollItem>,
    private val numerator: Int,
    private val denominator: Int,
    private val style: ChanceRollStyle,
    private val register: (PendingRateFirstItem) -> Unit,
) {
    public infix fun weight(objKey: String): PendingRateFirstItemBuilder =
        PendingRateFirstItemBuilder(builder, numerator, denominator, objKey, style, register)

    public infix fun chance(rollable: Rollable<Player, DropRollItem>) {
        builder.addRateFirstRollable(numerator, denominator, style, rollable)
    }

    public infix fun chance(item: DropRollItem) {
        builder.addRateFirstItem(numerator, denominator, style, item)
    }

    public infix fun rolls(rollable: Rollable<Player, DropRollItem>) {
        builder.apply {
            (numerator outOf denominator) rolls rollable
        }
    }

    public infix fun rolls(item: DropRollItem) {
        builder.apply {
            (numerator outOf denominator) rolls item
        }
    }
}

@DropTableDsl
public class PendingRateFirstItemBuilder internal constructor(
    private val builder: RSPrerollTableBuilder<Player, DropRollItem>,
    private val numerator: Int,
    private val denominator: Int,
    private val obj: String,
    private val style: ChanceRollStyle,
    private val register: (PendingRateFirstItem) -> Unit,
) {
    public infix fun count(value: Int): PendingRateFirstItem =
        PendingRateFirstItem(builder, numerator, denominator, obj, value..value, style, register)

    public infix fun count(range: IntRange): PendingRateFirstItem =
        PendingRateFirstItem(builder, numerator, denominator, obj, range, style, register)
}

@DropTableDsl
public class PendingRateFirstItem internal constructor(
    private val builder: RSPrerollTableBuilder<Player, DropRollItem>,
    private val numerator: Int,
    private val denominator: Int,
    obj: String,
    count: IntRange,
    private val style: ChanceRollStyle,
    register: (PendingRateFirstItem) -> Unit,
) : PendingDropItemHandle() {
    override val config: PendingDropItemConfig = PendingDropItemConfig(obj, count)

    init {
        register(this)
    }

    override fun onCommit(item: DropRollItem) {
        builder.addRateFirstItem(numerator, denominator, style, item)
    }
}

internal fun RSPrerollTableBuilder<Player, DropRollItem>.addRateFirstItem(
    numerator: Int,
    denominator: Int,
    style: ChanceRollStyle,
    item: DropRollItem,
) {
    if (item.requiresRollableWrapper()) {
        val rollable = dropRollable(item)
        when (style) {
            ChanceRollStyle.Chance -> (numerator outOf denominator) chance rollable
            ChanceRollStyle.Rolls -> (numerator outOf denominator) rolls rollable
        }
        return
    }
    when (style) {
        ChanceRollStyle.Chance -> (numerator outOf denominator) chance item
        ChanceRollStyle.Rolls -> (numerator outOf denominator) rolls item
    }
}

internal fun RSPrerollTableBuilder<Player, DropRollItem>.addRateFirstRollable(
    numerator: Int,
    denominator: Int,
    style: ChanceRollStyle,
    rollable: Rollable<Player, DropRollItem>,
) {
    when (style) {
        ChanceRollStyle.Chance -> (numerator outOf denominator) chance rollable
        ChanceRollStyle.Rolls -> (numerator outOf denominator) rolls rollable
    }
}

public fun rsPlayerTertiaryTable(
    block: DropChanceTableScope.() -> Unit,
): RSPreRollTable<Player, DropRollItem> {
    val builder = RSPrerollTableBuilder<Player, DropRollItem>()
    DropChanceTableScope(builder, ChanceRollStyle.Chance).apply {
        block()
        flushPendingRateFirstItems()
    }
    return builder.build() as RSPreRollTable<Player, DropRollItem>
}

public fun rsPlayerPrerollTable(
    block: DropChanceTableScope.() -> Unit,
): RSPreRollTable<Player, DropRollItem> {
    val builder = RSPrerollTableBuilder<Player, DropRollItem>()
    DropChanceTableScope(builder, ChanceRollStyle.Rolls).apply {
        block()
        flushPendingRateFirstItems()
    }
    return builder.build() as RSPreRollTable<Player, DropRollItem>
}
