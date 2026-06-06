package dtx.core

public data class AnyOf<T, R>(
    public val rollables: List<Rollable<T, R>>,
    public val withHooks: RollableHooks<T, R> = RollableHooks.Default()
): Rollable<T, R>, RollableHooks<T, R> by withHooks {

    override fun selectResult(target: T, otherArgs: ArgMap): RollResult<R> {
        return rollables.random().roll(target, otherArgs)
    }
}

public data class AllOf<T, R>(
    public val rollables: List<Rollable<T, R>>,
    public val withHooks: RollableHooks<T, R> = RollableHooks.Default()
): Rollable<T, R>, RollableHooks<T, R> by withHooks {

    override fun selectResult(target: T, otherArgs: ArgMap): RollResult<R> {
        return rollables
            .map { it.roll(target, otherArgs) }
            .flattenToList()
    }
}

public data class Single<T, R>(
    public val result: R,
    public val withHooks: RollableHooks<T, R> = RollableHooks.Default()
): Rollable<T, R>, RollableHooks<T, R> by withHooks {

    private val retResult = RollResult.Single<R>(result)

    override fun selectResult(target: T, otherArgs: ArgMap): RollResult<R> {
        return retResult
    }
}

public class SingleByFun<T, R>(
    public val resultSelector: ResultSelector<T, R>,
    public val withHooks: RollableHooks<T, R> = RollableHooks.Default()
): Rollable<T, R>, RollableHooks<T, R> by withHooks {

    override fun selectResult(target: T, otherArgs: ArgMap): RollResult<R> {
        return resultSelector(target, otherArgs)
    }
}
