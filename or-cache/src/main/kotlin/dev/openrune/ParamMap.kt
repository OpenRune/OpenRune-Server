package dev.openrune

import dev.openrune.definition.type.ParamType
import dev.openrune.literal.CacheVarTypeMap
import dev.openrune.literal.CacheVarTypeMap.codecOut

public class ParamMap(
    public val primitiveMap: Map<Int, Any>,
    public var typedMap: Map<Int, Any?>? = null,
) : Iterable<Map.Entry<Int, Any?>> {

    public val checkedTypedMap: Map<Int, Any?>
        get() = typedMap ?: buildTypedMap().also { typedMap = it }

    public val keys: Set<Int>
        get() = checkedTypedMap.keys

    public val values: Collection<Any?>
        get() = checkedTypedMap.values

    public val entries: Set<Map.Entry<Int, Any?>>
        get() = checkedTypedMap.entries

    public fun isEmpty(): Boolean = checkedTypedMap.isEmpty()

    public fun isNotEmpty(): Boolean = !isEmpty()

    public operator fun contains(param: ParamType): Boolean = primitiveMap.containsKey(param.id)

    public operator fun contains(param: TypedParamType<*>): Boolean = contains(param.raw)

    @Suppress("UNCHECKED_CAST")
    public fun <T : Any> getOrNull(param: ParamType): T? = checkedTypedMap[param.id] as? T

    public fun <T : Any> getOrNull(param: TypedParamType<T>): T? = getOrNull(param.raw)

    @Suppress("UNCHECKED_CAST")
    public operator fun <T : Any> get(param: ParamType): T? =
        getOrNull(param) ?: (param.typedDefault as? T)

    public operator fun <T : Any> get(param: TypedParamType<T>): T? = get(param.raw)

    public operator fun plus(other: ParamMap): ParamMap {
        val otherTypedMap = other.typedMap
        val thisTypedMap = typedMap
        val combinedTypedMap =
            when {
                thisTypedMap == null -> otherTypedMap
                otherTypedMap == null -> thisTypedMap
                else -> thisTypedMap + otherTypedMap
            }
        val combinedPrimitiveMap = primitiveMap + other.primitiveMap
        return ParamMap(combinedPrimitiveMap, combinedTypedMap)
    }

    override fun iterator(): Iterator<Map.Entry<Int, Any?>> = checkedTypedMap.iterator()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ParamMap) return false
        if (primitiveMap != other.primitiveMap) return false
        return true
    }

    override fun hashCode(): Int = primitiveMap.entries.hashCode()

    override fun toString(): String = typedMap?.toString() ?: primitiveMap.toString()
}

public fun Map<Int, Any>.toParamMap(): ParamMap {
    val primitive = this.mapNotNull { (key, value) -> key to value }.toMap()
    return ParamMap(primitiveMap = primitive)
}

public fun <T : Any> ParamMap?.resolve(param: ParamType): T {
    if (this == null) {
        val default = param.typedDefault as? T
        checkNotNull(default) {
            "Param `$param` does not have a default value. Use `paramOrNull` instead."
        }
        return default
    }

    val value = this.getOrNull<T>(param)
    if (value != null) {
        return value
    }

    val default = param.typedDefault as? T
    checkNotNull(default) {
        "Param `$param` does not have a default value. Use `paramOrNull` instead."
    }
    return default
}

public fun <T : Any> ParamMap?.resolve(param: TypedParamType<T>): T = resolve(param.raw)

@Suppress("UNCHECKED_CAST")
private val ParamType.typedDefault: Any?
    get() {
        val literal = type ?: return null
        val codecType = literal.codecOut
        val default: Any =
            when (codecType) {
                String::class -> defaultString ?: return null
                Long::class -> defaultLong
                else -> defaultInt
            }
        val codec = CacheVarTypeMap.findCodec<Any, Any>(codecType)
        return codec.decode(default)
    }

private fun ParamMap.buildTypedMap(): Map<Int, Any?> =
    primitiveMap.mapValues { (key, primitive) ->
        val param = ServerCacheManager.getParam(key)
        if (param?.type == null) {
            primitive
        } else {
            decodePrimitive(param, primitive)
        }
    }

private fun decodePrimitive(param: ParamType, primitive: Any): Any? {
    val codecType = param.type?.codecOut ?: return primitive
    val codec = CacheVarTypeMap.findCodec<Any, Any>(codecType)
    return codec.decode(primitive)
}
