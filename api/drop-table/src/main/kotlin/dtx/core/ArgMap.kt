package dtx.core

public data class ArgKey<T>(val str: String, val default: T)

public data class ArgPair<T>(val key: ArgKey<T>, val value: T)

public infix fun <T> ArgKey<T>.with(other: T): ArgPair<T> = ArgPair(this, other)

public class ArgMap private constructor(private val locked: Boolean = false) {

    public constructor(
        vararg args: ArgPair<*>,
        otherMap: ArgMap = ArgMap.Empty
    ): this(locked = false) {

        args.forEach { argPair ->
            setWildcard(argPair)
        }

        otherMap.map.forEach { pair -> setWildcard(pair.key, pair.value) }
    }

    private val map = mutableMapOf<ArgKey<*>, Any?>()

    public operator fun <T> set(key: ArgKey<T>, value: T) {

        if (locked) {
            throw IllegalStateException("ArgMap is locked")
        }

        map[key] = value
    }

    private fun setWildcard(argPair: ArgPair<*>) {
        map[argPair.key] = argPair.value
    }

    private fun setWildcard(key: ArgKey<*>, value: Any?) {
        map[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    public operator fun <T> get(key: ArgKey<T>): T {
        return (map[key] ?: key.default) as T
    }

    public companion object {

        public val Empty: ArgMap get() = ArgMap(locked = false)

        public val EmptyLocked: ArgMap = ArgMap(locked = true)
    }
}
