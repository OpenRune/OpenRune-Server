package dtx.core

public sealed interface RollResult<R> {

    public data object Nothing: RollResult<Nothing>
    public data class Single<R>(val result: R): RollResult<R>
    public data class ListOf<R>(val results: List<R>): RollResult<R>

    public companion object {
        public fun <R> Nothing(): RollResult<R> = Nothing as RollResult<R>
    }
}

public fun <R> List<RollResult<R>>.flattenToList(): RollResult.ListOf<R> {

    if (isEmpty()) {
        return RollResult.ListOf<R>(emptyList())
    }

    if (size == 1) {

        val first = first().flatten()

        return when (first) {

            is RollResult.Nothing -> RollResult.ListOf<R>(emptyList())
            is RollResult.Single<R> -> RollResult.ListOf<R>(listOf(first.result))
            is RollResult.ListOf<R> -> RollResult.ListOf<R>(first.results)
        }
    }

    val filtered = filterNot { it is RollResult.Nothing }
    val singles = filtered
        .filterIsInstance<RollResult.Single<R>>()
        .map { it.result }
    val singlesResult = RollResult.ListOf(singles)

    if (singles.size == filtered.size) {
        return singlesResult
    }

    val lists = (filtered.filterIsInstance<RollResult.ListOf<R>>()).map { it.results }
    val listsResult = RollResult.ListOf(lists.flatten())

    if (lists.size == filtered.size) {
        return listsResult
    }

    return RollResult.ListOf(listsResult.results + singles)

}

public fun <R> RollResult<R>.flatten(): RollResult<R> = when (this) {

    is RollResult.Nothing -> this
    is RollResult.Single<R> -> this

    is RollResult.ListOf<R> -> when (results.size) {

        0 -> RollResult.Nothing()
        1 -> RollResult.Single(results.first())
        else -> RollResult.ListOf(results)
    }
}
