package dtx.impl.chain

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.Rollable
import dtx.table.Table
import kotlin.random.Random


public interface ChainedTable<T, R> : Table<T, R>, ChainedTableHooks<T, R> {
    override val tableEntries: Collection<ChainRollable<T, R>>

    public val head: ChainRollable<T, R>
}

public open class ChainedTableImpl<T, R>(
    public override val tableIdentifier: String,
    public override val head: ChainRollable<T, R>,
    private val hooks: ChainedTableHooks<T, R> = ChainedTableHooks.Default()
) : ChainedTable<T, R>, ChainedTableHooks<T, R> by hooks {

    override val tableEntries: Collection<ChainRollable<T, R>> = head.collect()

    override fun selectResult(target: T, otherArgs: ArgMap): RollResult<R> {

        onChainStart(target)

        var link = head

        while (link !== ChainEnd) {

            val chainHooks: ChainRollableHooks<T, R> = when (link) {
                is ChainRollableImpl<T, R> -> link.hooks
                is ChainEnd -> ChainRollableHooks.Default()
                else -> link as ChainRollableHooks<T, R>
            }

            if (chainHooks.skipLink(target)) {
                link = chainHooks.nextOverride(target, link.next)
                continue
            }

            val (adjustedBase, adjustedRollChance) = chainHooks.adjustChance(
                target = target,
                base = link.base,
                rollChance = link.rollChance
            )

            if (adjustedRollChance <= 0) {
                link = chainHooks.nextOverride(target, link.next)
                continue
            }

            val threshold = adjustedBase.coerceIn(0, adjustedRollChance)
            val rolled = Random.nextInt(0, adjustedRollChance)
            val passed = rolled < threshold

            onEachLink(target, link, rolled, threshold, passed)
            chainHooks.onLinkEvaluated(target, rolled, threshold, passed)

            if (passed) {
                val result = when (link) {
                    is ChainRollableImpl<T, R> -> link.rollable.roll(target, otherArgs)
                    else -> link.roll(target, otherArgs)
                }
                onChainEnd(target, result)
                return result
            } else {
                link = chainHooks.nextOverride(target, link.next)
            }
        }
        val nothing: RollResult<R> = RollResult.Nothing()
        onChainEnd(target, nothing)
        return nothing
    }

    override fun toString(): String = "ChainedTable[$tableIdentifier]"
}
