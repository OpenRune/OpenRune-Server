package dtx.impl.misc

@JvmInline
public value class Percent(public val value: Double) {

    public constructor(intValue: Int): this(intValue.toDouble()) {
        require(intValue in 0..100) { "Value [$intValue] must be between 0 and 100" }
    }
}

public inline val Double.percent: Percent get() = Percent(this)

public inline val Int.percent: Percent get() = Percent(this)
