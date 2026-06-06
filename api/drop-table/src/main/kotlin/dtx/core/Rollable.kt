package dtx.core

public interface Rollable<T, R>: RollableHooks<T, R> {

    public fun selectResult(target: T, otherArgs: ArgMap): RollResult<R>

    public fun roll(target: T, otherArgs: ArgMap = ArgMap.Empty): RollResult<R> {

        if (vetoRoll(target, otherArgs)) {
            return onRollVetoed(target)
        }

        val result = selectResult(target, otherArgs)
        val transformed = transformResult(target, result)

        onRollCompleted(target, otherArgs, transformed)

        return transformed
    }

    public companion object {

        public data object EmptyRollable: Rollable<Any?, Any?> by EmptyRollable

        public fun <T, R> Empty(): Rollable<T, R> {
            return EmptyRollable as Rollable<T, R>
        }
    }
}
