package dtx.rs

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.impl.chain.ChainEnd
import dtx.impl.chain.ChainRollable
import dtx.impl.chain.ChainedTable
import dtx.impl.chain.ChainedTableBuilder
import dtx.impl.chain.ChainedTableHooks
import dtx.impl.chain.ChainedTableImpl

public open class RSChainedTable<T, R>(
    private val inner: ChainedTable<T, R>,
) : RSTable<T, R>, ChainedTable<T, R>, ChainedTableHooks<T, R> by inner {

    override val tableIdentifier: String = inner.tableIdentifier
    override val head: ChainRollable<T, R> = inner.head
    override val tableEntries: Collection<ChainRollable<T, R>> = inner.tableEntries

    override fun selectResult(target: T, otherArgs: ArgMap): RollResult<R> = inner.selectResult(target, otherArgs)

    override fun toString(): String = inner.toString()

    public companion object {
        private val EmptyTable: RSChainedTable<Any?, Any?> = RSChainedTable(
            ChainedTableImpl("", ChainEnd(), ChainedTableHooks.Default()),
        )

        @Suppress("UNCHECKED_CAST")
        public fun <T, R> Empty(): RSChainedTable<T, R> = EmptyTable as RSChainedTable<T, R>
    }
}

public open class RSChainedTableBuilder<T, R> : ChainedTableBuilder<T, R, RSChainedTable<T, R>>(
    impl = { name, head, hooks ->
        val inner = ChainedTableImpl(name, head, hooks)
        RSChainedTable(inner)
    },
)

public fun <T, R> rsChainedTable(block: RSChainedTableBuilder<T, R>.() -> Unit): RSChainedTable<T, R> {
    val builder = RSChainedTableBuilder<T, R>()
    builder.apply(block)
    return builder.build()
}
