package dtx.impl.chain

import dtx.core.ArgMap
import dtx.core.Rollable
import dtx.core.RollableHooks
import dtx.core.SingleRollableBuilder
import dtx.core.singleRollable
import dtx.table.AbstractTableBuilder


public open class ChainedTableBuilder<T, R, TT : ChainedTable<T, R>>(
    private val impl: (name: String, head: ChainRollable<T, R>, hooks: ChainedTableHooks<T, R>) -> TT
) : AbstractTableBuilder<
        T,
        R,
        ChainRollable<T, R>,
        TT,
        ChainedTableHooks<T, R>,
        ChainedTableHooksBuilder<T, R>,
        ChainedTableBuilder<T, R, TT>
        >(createHookBuilder = ChainedTableHooksBuilder.new()) {

    override val entries: MutableCollection<ChainRollable<T, R>> = mutableListOf()

    private fun addRollable(rollable: ChainRollable<T, R>) {
        entries.add(rollable)
    }

    public inner class Intermediary(public val rollChance: Int, public val rollable: Rollable<T, R>)

    public infix operator fun Int.invoke(block: SingleRollableBuilder<T, R>.() -> Unit): Intermediary {
        return Intermediary(this, singleRollable(block))
    }

    public infix fun Int.rolls(rollable: Rollable<T, R>): Intermediary {
        return Intermediary(this, rollable)
    }

    public infix fun Int.rolls(block: SingleRollableBuilder<T, R>.() -> Unit): Intermediary {
        return rolls(singleRollable(block))
    }

    public infix fun Int.outOf(intermediary: Intermediary) {
        val chain = chainRollable(baseChance = this, rollChance = intermediary.rollChance, rollable = intermediary.rollable)
        addRollable(chain)
    }

    public inner class ChanceIntermediary(public val baseChance: Int, public val rollChance: Int)

    public infix fun Int.outOf(rollChance: Int): ChanceIntermediary = ChanceIntermediary(this, rollChance)

    public infix fun ChanceIntermediary.rolls(rollable: Rollable<T, R>) {
        val chain = chainRollable(baseChance = baseChance, rollChance = rollChance, rollable = rollable)
        addRollable(chain)
    }

    public infix fun ChanceIntermediary.rolls(block: SingleRollableBuilder<T, R>.() -> Unit) {
        return rolls(singleRollable(block))
    }

    public infix fun ChanceIntermediary.rolls(item: R) {
        return rolls(singleRollable { result(item) })
    }

    init {
        construct { hooks ->
            require(entries.isNotEmpty()) { "ChainedTable[$tableIdentifier] must have at least one chain link" }

            var next: ChainRollable<T, R> = ChainEnd()

            for (i in entries.size - 1 downTo 0) {

                val current = entries.elementAt(i)

                next = when (current) {
                    is ChainRollableImpl<T, R> -> ChainRollableImpl(current.base, current.rollChance, next, current.rollable, current.hooks)
                    else -> {
                        val adapter = object : Rollable<T, R>, RollableHooks<T, R> by current {
                            override fun selectResult(target: T, otherArgs: ArgMap): dtx.core.RollResult<R> {
                                return current.selectResult(target, otherArgs)
                            }
                        }
                        ChainRollableImpl(current.base, current.rollChance, next, rollable = adapter, hooks = current)
                    }
                }
            }

            impl(tableIdentifier, next, hooks)
        }
    }
}

public fun <T, R> chainedTable(
    tableName: String = "Unnamed Chained Table",
    block: ChainedTableBuilder<T, R, ChainedTable<T, R>>.() -> Unit
): ChainedTable<T, R> {
    val builder = ChainedTableBuilder<T, R, ChainedTable<T, R>> { name, head, hooks -> ChainedTableImpl(name, head, hooks) }
    builder.name(tableName)
    builder.apply(block)
    return builder.build()
}
