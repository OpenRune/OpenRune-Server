package dtx.impl.weighted

import dtx.core.ArgMap
import dtx.core.Rollable
import dtx.core.RollableHooks
import dtx.core.RollResult

public interface WeightedRollable<T, R>: Rollable<T, R> {

    public val weight: Double
    public val rollable: Rollable<T, R>

    public operator fun component1(): Double {
        return weight
    }

    public operator fun component2(): Rollable<T, R> {
        return rollable
    }

    private data object Empty: WeightedRollable<Any?, Any?> {

        override fun includeInRoll(onTarget: Any?, otherArgs: ArgMap): Boolean {
            return false
        }

        override fun selectResult(target: Any?, otherArgs: ArgMap): RollResult<Any?> {
            return rollable.roll(target, otherArgs)
        }

        override val weight: Double = 0.0

        override val rollable: Rollable<Any?, Any?> = Rollable.Empty()

        override fun vetoRoll(onTarget: Any?, otherArgs: ArgMap): Boolean {
            return true
        }

        override fun onRollVetoed(onTarget: Any?): RollResult<Any?> {
            return RollResult.Nothing()
        }

        override fun transformResult(withTarget: Any?, result: RollResult<Any?>): RollResult<Any?> {
            return result
        }

        override fun onRollCompleted(target: Any?, otherArgs: ArgMap, result: RollResult<Any?>) {
            // Do nothing
        }
    }

    public companion object {

        public fun <T, R> Empty(): WeightedRollable<T, R> {
            return Empty as WeightedRollable<T, R>
        }
    }
}

public data class WeightedRollableImpl<T, R>(
    override val weight: Double,
    override val rollable: Rollable<T, R>,
    private val hooks: RollableHooks<T, R> = RollableHooks.Default()
): WeightedRollable<T, R>, RollableHooks<T, R> by hooks {

    override fun includeInRoll(onTarget: T, otherArgs: ArgMap): Boolean {
        return rollable.includeInRoll(onTarget, otherArgs)
    }

    override fun selectResult(target: T, otherArgs: ArgMap): RollResult<R> {
        return rollable.roll(target, otherArgs)
    }
}

public class WeightedCollectionRollable<T, R>(
    override val weight: Double,
    internal val rollables: Collection<WeightedRollable<T, R>>,
    internal val hooks: RollableHooks<T, R> = RollableHooks.Default(),
): WeightedRollable<T, R>, RollableHooks<T, R> by hooks {

    override fun includeInRoll(onTarget: T, otherArgs: ArgMap): Boolean {
        return rollables.any { it.includeInRoll(onTarget, otherArgs) }
    }

    override val rollable: Rollable<T, R> get() = rollables.filter { it.weight > 0.0 }.random()

    override fun selectResult(target: T, otherArgs: ArgMap): RollResult<R> {

        if (rollables.isEmpty()) {
            return RollResult.Nothing()
        }

        if (rollables.all { !it.includeInRoll(target, otherArgs) }) {
            return RollResult.Nothing()
        }

        var picked = rollable

        while (!picked.includeInRoll(target, otherArgs)) {
            picked = rollable
        }

        return picked.roll(target, otherArgs)
    }
}