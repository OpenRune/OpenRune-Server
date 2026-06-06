package dtx.rs

import dtx.core.Rollable
import dtx.core.Single
import dtx.impl.weighted.WeightedTableBuilder

public class RSWeightedTableBuilder<T, R> :
    WeightedTableBuilder<T, R, RSWeightedTable<T, R>>({ _, _, _ ->
        error("RSWeightedTableBuilder must override construct")
    }) {

    public var poolTotal: Int? = null

    internal val inlineSeparateRolls: MutableList<InlineSeparateRoll<T, R>> = mutableListOf()

    public fun pool(total: Int): RSWeightedTableBuilder<T, R> {
        require(total > 0) { "pool total must be positive" }
        poolTotal = total
        return this
    }

    override fun build(): RSWeightedTable<T, R> {
        poolTotal?.let { expected ->
            val actual = weightedEntries.sumOf { it.weight.toInt() }
            require(actual == expected) {
                "main table weights sum to $actual but pool total is $expected"
            }
        }
        return super.build()
    }

    init {
        construct { hooks ->
            RSWeightedTable(
                tableIdentifier,
                weightedEntries.toList(),
                hooks,
                inlineSeparateRolls.toList(),
            )
        }
    }

    public infix fun Int.outOf(other: Int): SeparateRollAccess {
        require(other > 0) { "denominator must be positive" }
        require(this in 0..other) { "numerator must be within 0..denominator" }
        return SeparateRollAccess(this, other)
    }

    public infix fun SeparateRollAccess.separate(entry: R): RSWeightedTableBuilder<T, R> {
        captureSeparateRoll(numerator, denominator, Single(entry))
        return this@RSWeightedTableBuilder
    }

    public infix fun SeparateRollAccess.separate(rollable: Rollable<T, R>): RSWeightedTableBuilder<T, R> {
        captureSeparateRoll(numerator, denominator, rollable)
        return this@RSWeightedTableBuilder
    }

    public infix fun SeparateRollAccess.separate(
        block: RSWeightedTableBuilder<T, R>.() -> Unit,
    ): RSWeightedTableBuilder<T, R> {
        val nested = RSWeightedTableBuilder<T, R>()
        nested.apply(block)
        hoistInlineSeparateRolls(nested)
        captureSeparateRoll(numerator, denominator, nested.build())
        return this@RSWeightedTableBuilder
    }

    private fun captureSeparateRoll(numerator: Int, denominator: Int, rollable: Rollable<T, R>) {
        inlineSeparateRolls += InlineSeparateRoll(numerator, denominator, rollable)
    }

    internal fun hoistInlineSeparateRolls(other: RSWeightedTableBuilder<T, R>) {
        inlineSeparateRolls += other.inlineSeparateRolls
        other.inlineSeparateRolls.clear()
    }
}

public fun <T, R> RSWeightedTableBuilder<T, R>.accessTable(
    table: RSWeightedTable<T, R>,
    numerator: Int,
    denominator: Int = 128,
): RSWeightedTableBuilder<T, R> {
    require(denominator > 0) { "denominator must be positive" }
    require(numerator in 0..denominator) { "numerator must be within 0..denominator" }
    poolTotal?.let { pool ->
        require(pool == denominator) {
            "accessTable denominator $denominator does not match declared pool total $pool"
        }
    }
    numerator.weight(table)
    return this
}

public fun <T, R> RSWeightedTableBuilder<T, R>.group(
    tableName: String,
    block: RSWeightedTableBuilder<T, R>.() -> Unit,
): RSWeightedTableBuilder<T, R> {
    val subBuilder = RSWeightedTableBuilder<T, R>()
    subBuilder.apply(block)
    hoistInlineSeparateRolls(subBuilder)
    absorb(subBuilder)
    return this
}

public fun <T, R> rsWeightedTable(
    total: Int? = null,
    block: RSWeightedTableBuilder<T, R>.() -> Unit,
): RSWeightedTable<T, R> {
    val builder = RSWeightedTableBuilder<T, R>()
    total?.let { builder.pool(it) }
    builder.apply(block)
    return builder.build()
}
