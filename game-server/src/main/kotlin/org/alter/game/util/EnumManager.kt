package org.alter.game.util

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.EnumType
import dev.openrune.definition.util.BaseVarType
import dev.openrune.definition.util.VarType
import org.alter.game.util.vars.VarTypeImpl
import org.alter.rscm.RSCM.asRSCM

/**
 * Represents a typed key/value pair from an enum.
 */
data class EnumPair<K, V>(val key: K, val value: V)

/**
 * Top-level helper to retrieve a type-safe list of enum entries by name.
 *
 * Example:
 * ```
 * val entries = enum("enums.bone_data", ObjType, RowType)
 * entries.forEach { (key, value) ->
 *     println("Key: $key, Value: $value")
 * }
 * ```
 */
fun <K1, V1, K2, V2> enum(
    enumName: String,
    keyType: VarTypeImpl<K1, V1>,
    valueType: VarTypeImpl<K2, V2>
): List<EnumPair<V1, V2>> =
    EnumHelper.of(enumName).getEnum(keyType, valueType)

fun <K, V> enumKey(
    enumName: String,
    key: K,
    keyType: VarTypeImpl<*, K>,
    valueType: VarTypeImpl<*, V>
): V {
    return enum(enumName, keyType, valueType)
        .firstOrNull { it.key == key }
        ?.value ?: throw NoSuchElementException("Key '$key' not found in enum '$enumName'")
}

/**
 * Type-safe helper for working with an EnumType.
 *
 * Example:
 * ```
 * val helper = EnumHelper.of("enums.bone_data")
 * val entries = helper.getEnum(ObjType, RowType)
 * val value = helper.getByKey("DRAGON_BONE", ObjType, RowType)
 * val key = helper.getByValue(35, ObjType, RowType)
 * ```
 */
@Suppress("UNCHECKED_CAST")
class EnumHelper private constructor(val enum: EnumType) {

    val id: Int get() = enum.id
    val keyType: VarType get() = enum.keyType
    val valueType: VarType get() = enum.valueType

    override fun toString(): String =
        "EnumHelper(id=$id, size=${enum.values.size}, keys=${enum.values.keys.joinToString()})"

    /**
     * Returns a type-safe list of key/value pairs from the enum.
     *
     * Example:
     * ```
     * val entries = helper.getEnum(ObjType, RowType)
     * entries.forEach { (key, value) ->
     *     println("Key: $key, Value: $value")
     * }
     * ```
     */
    fun <K1, V1, K2, V2> getEnum(
        keyVarType: VarTypeImpl<K1, V1>,
        valueVarType: VarTypeImpl<K2, V2>
    ): List<EnumPair<V1, V2>> {
        val entries = enum.values.toList()
        if (entries.isEmpty()) return emptyList()

        return entries.mapIndexed { index, (rawKey, rawValue) ->
            val actualKeyType = enum.keyType
            val actualValueType = enum.valueType

            if (actualKeyType != keyVarType.type) {
                throw IllegalArgumentException(
                    "Type mismatch for key at index $index: expected '${keyVarType.type}', found '$actualKeyType'"
                )
            }

            if (actualValueType != valueVarType.type) {
                throw IllegalArgumentException(
                    "Type mismatch for value at index $index: expected '${valueVarType.type}', found '$actualValueType'"
                )
            }

            val key: K1 = if (keyVarType.type.baseType == BaseVarType.STRING) {
                rawKey as K1
            } else {
                (rawKey.toIntOrNull() as K1?)
                    ?: throw IllegalArgumentException("Cannot convert key '$rawKey' to Int")
            }

            val value: K2 = if (valueVarType.type.baseType == BaseVarType.STRING) {
                rawValue as K2
            } else {
                (rawValue.toString().toIntOrNull() as K2?)
                    ?: throw IllegalArgumentException("Cannot convert value '$rawValue' to Int")
            }

            EnumPair(keyVarType.convertTo(key), valueVarType.convertTo(value))
        }
    }

    companion object {
        /**
         * Load an EnumType from the server cache by name.
         */
        fun load(name: String): EnumType {
            require(name.startsWith("enums")) { "Invalid enum name '$name' — must start with 'enums'" }
            return ServerCacheManager.getEnum(name.asRSCM())
                ?: throw NoSuchElementException("Enum '$name' not found")
        }

        /**
         * Creates an [EnumHelper] from an enum name.
         */
        fun of(name: String): EnumHelper = EnumHelper(load(name))
    }
}
