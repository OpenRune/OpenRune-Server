package dev.openrune

import dev.openrune.literal.CacheVarTypeMap
import kotlin.reflect.KClass

public data class TypedParamType<T : Any>(
    public val raw: dev.openrune.definition.type.ParamType,
    public val expectedType: KClass<T>,
) {
    public val id: Int
        get() = raw.id
}

public object ParamTypes {
    private val cache: MutableMap<Pair<Int, KClass<*>>, TypedParamType<*>> = mutableMapOf()

    public inline fun <reified T : Any> byId(id: Int): TypedParamType<T> = byId(id, T::class)

    @Suppress("UNCHECKED_CAST")
    public fun <T : Any> byId(id: Int, expectedType: KClass<T>): TypedParamType<T> {
        val key = id to expectedType
        return cache.getOrPut(key) {
            val raw = checkNotNull(ServerCacheManager.getParam(id)) { "Unknown param id: $id" }
            of(raw, expectedType)
        } as TypedParamType<T>
    }

    public inline fun <reified T : Any> of(
        raw: dev.openrune.definition.type.ParamType
    ): TypedParamType<T> = of(raw, T::class)

    public fun <T : Any> of(
        raw: dev.openrune.definition.type.ParamType,
        expectedType: KClass<T>,
    ): TypedParamType<T> {
        val expectedLiteral = CacheVarTypeMap.classedLiterals[expectedType]
        val actualLiteral = raw.type

        return TypedParamType(raw = raw, expectedType = expectedType)
    }
}
