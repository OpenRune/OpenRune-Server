package dtx.impl.meta

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.Rollable
import dtx.core.Single
import dtx.core.SingleByFun
import dtx.core.SingleRollableBuilder

public abstract class AbstractMetaRollableBuilder<
        Target,
        RollType,
        EntryType: MetaRollable<Target, RollType>,
        BuilderType: AbstractMetaRollableBuilder<Target, RollType, EntryType, BuilderType>
> {

    public var identifier: String = ""
    public var rollable: Rollable<Target, RollType> = Rollable.Empty()
    public var initialValue: Double = 1.0
    public var minValue: Double = Double.MIN_VALUE
    public var maxValue: Double = Double.MAX_VALUE
    public val filters: MutableSet<MetaEntryFilter<Target, RollType>> = mutableSetOf()

    public fun value(newValue: Double): BuilderType {

        initialValue = newValue

        return this as BuilderType
    }

    public fun maximum(newValue: Double): BuilderType {

        maxValue = newValue

        return this as BuilderType
    }

    public fun minimum(newValue: Double): BuilderType {

        minValue = newValue

        return this as BuilderType
    }

    public fun identifier(identifier: String): BuilderType {

        this.identifier = identifier

        return this as BuilderType
    }

    public fun id(identifier: String): BuilderType {
        return identifier(identifier)
    }

    public fun rollable(newRollable: Rollable<Target, RollType>): BuilderType {

        rollable = newRollable

        return this as BuilderType
    }

    public fun rollable(item: RollType): BuilderType {
        return rollable(Single(item))
    }

    public fun rollableBy(block: SingleByFun<Target, RollType>.(Target, ArgMap) -> RollResult<RollType>): BuilderType {
        return rollable(SingleByFun(block))
    }

    public fun rollable(block: SingleRollableBuilder<Target, RollType>.() -> Unit): BuilderType {

        val built = SingleRollableBuilder<Target, RollType>()
            .apply(block)
            .build()
        rollable(built)

        return this as BuilderType
    }

    public fun addFilter(filter: MetaEntryFilter<Target, RollType>): BuilderType {

        filters.add(filter)

        return this as BuilderType
    }


    public fun addFilter(block: MetaEntryFilterBuilder<Target, RollType>.() -> Unit): BuilderType {
        return addFilter(MetaEntryFilterBuilder<Target, RollType>().apply(block).build())
    }

    public abstract fun build(): EntryType
}