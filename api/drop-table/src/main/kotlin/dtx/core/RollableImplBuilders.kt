package dtx.core

public open class SingleRollableBuilder<T, R>: AbstractRollableBuilder<
    T,
    R,
    Rollable<T, R>,
    RollableHooks<T, R>,
    DefaultRollableHooksBuilder<T, R>,
    SingleRollableBuilder<T, R>>(
        { DefaultRollableHooksBuilder() }
    ) {

    init {
        construct { hooks: RollableHooks<T, R> ->
            SingleByFun<T, R>(selectResultFunc!!, hooks)
        }
    }

    public override fun result(result: R): SingleRollableBuilder<T, R> {
        return selectResult { target, otherArgs ->
            RollResult.Single(result)
        }
    }
}

public fun <T, R> singleRollable(block: SingleRollableBuilder<T, R>.() -> Unit): Rollable<T, R> {

    val builder = SingleRollableBuilder<T, R>()
    builder.apply(block)

    return builder.build()
}

public open class CollectionRollableBuilder<T, R>: SingleRollableBuilder<T, R>() {

    init {
        construct { hooks: RollableHooks<T, R> ->
            if (rollableType == RollableType.Any) {
                AnyOf<T, R>(collection, hooks)
            } else {
                AllOf<T, R>(collection, hooks)
            }
        }
    }

    protected val collection: MutableList<Rollable<T, R>> = mutableListOf()

    public enum class RollableType {
        Any, All;
    }

    public lateinit var rollableType: RollableType

    public fun type(type: RollableType): CollectionRollableBuilder<T, R> {

        rollableType = type

        return this
    }
}