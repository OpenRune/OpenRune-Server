package dev.openrune.types.util

import dev.openrune.ParamMap
import dev.openrune.definition.type.ParamType
import dev.openrune.literal.CacheVarTypeMap

@JvmInline
public value class ParamMapBuilder(private val typed: MutableMap<Int, Any> = hashMapOf()) {
    public fun toParamMap(): ParamMap {
        val primitive =
            typed.entries.associate {
                val codec = CacheVarTypeMap.findCodec<Any, Any>(it.value::class)
                val primitive = codec.encode(it.value)
                it.key to primitive
            }
        return ParamMap(primitive, typed)
    }

    public fun isNotEmpty(): Boolean = typed.isNotEmpty()

    public operator fun <T : Any> set(param: ParamType, value: T) {
        typed[param.id] = value
    }
}
