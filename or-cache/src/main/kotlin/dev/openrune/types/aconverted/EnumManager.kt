package dev.openrune.types.aconverted

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.EnumType
import dev.openrune.definition.util.BaseVarType
import dev.openrune.definition.util.VarType
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.dbcol.DbColumnCodec
import dev.openrune.types.dbcol.decodeEnumSlotForEnum

/** Represents a typed key/value pair from an enum. */
public data class EnumPair<K, V>(val key: K, val value: V)

/**
 * Top-level helper to retrieve a type-safe list of enum entries by name.
 *
 * Example:
 * ```
 * val entries = enum("enums.bone_data", DbColumnCodec.IntCodec, DbColumnCodec.IntCodec)
 * entries.forEach { (key, value) ->
 *     println("Key: $key, Value: $value")
 * }
 * ```
 */
public fun <V1, V2> enum(
    enumName: String,
    keyCodec: DbColumnCodec<*, V1>,
    valueCodec: DbColumnCodec<*, V2>,
): List<EnumPair<V1, V2>> = EnumHelper.of(enumName).getEnum(keyCodec, valueCodec)

public fun <V1, V2> enum(
    enumName: Int,
    keyCodec: DbColumnCodec<*, V1>,
    valueCodec: DbColumnCodec<*, V2>,
): List<EnumPair<V1, V2>> = EnumHelper.of(enumName).getEnum(keyCodec, valueCodec)

public fun <K, V> enumKey(
    enumName: String,
    key: K,
    keyCodec: DbColumnCodec<*, K>,
    valueCodec: DbColumnCodec<*, V>,
): V? = enum(enumName, keyCodec, valueCodec).firstOrNull { it.key == key }?.value

/**
 * Type-safe helper for working with an [dev.openrune.definition.type.EnumType].
 *
 * Example:
 * ```
 * val helper = EnumHelper.of("enums.bone_data")
 * val entries = helper.getEnum(DbColumnCodec.IntCodec, DbColumnCodec.IntCodec)
 * ```
 */
public class EnumHelper private constructor(public val enum: EnumType) {

    public val id: Int
        get() = enum.id

    public val keyType: VarType
        get() = enum.keyType

    public val valueType: VarType
        get() = enum.valueType

    override fun toString(): String =
        "EnumHelper(id=$id, size=${enum.values.size}, keys=${enum.values.keys.joinToString()})"

    public fun <V1, V2> getEnum(
        keyCodec: DbColumnCodec<*, V1>,
        valueCodec: DbColumnCodec<*, V2>,
    ): List<EnumPair<V1, V2>> {

        val entries = enum.values.toList()
        if (entries.isEmpty()) return emptyList()

        return entries.map { (rawKey, rawValue) ->
            val key: V1 =
                if (enum.keyType.baseType == BaseVarType.STRING) {
                    rawKey as V1
                } else {
                    decodeEnumSlotForEnum(keyCodec, rawKey.toString())
                }

            val value: V2 =
                if (enum.valueType.baseType == BaseVarType.STRING) {
                    rawValue as V2
                } else {
                    decodeEnumSlotForEnum(valueCodec, rawValue.toString())
                }

            EnumPair(key, value)
        }
    }

    public companion object {
        public fun load(enum: Int): EnumType {
            return ServerCacheManager.getEnum(enum)
                ?: throw NoSuchElementException("Enum '$enum' not found")
        }

        public fun load(name: String): EnumType {
            RSCM.requireRSCM(RSCMType.ENUM, name)
            return ServerCacheManager.getEnum(name.asRSCM())
                ?: throw NoSuchElementException("Enum '$name' not found")
        }

        public fun of(name: String): EnumHelper = EnumHelper(load(name))

        public fun of(name: Int): EnumHelper = EnumHelper(load(name))
    }
}
