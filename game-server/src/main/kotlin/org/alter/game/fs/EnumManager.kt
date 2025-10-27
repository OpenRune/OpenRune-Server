package org.alter.game.fs

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.EnumType
import dev.openrune.definition.util.Type
import org.alter.rscm.RSCM.asRSCM

object EnumManager {


    fun lookup(name: String): EnumType {
        require(name.startsWith("enums")) { "Invalid RSCM table: '$name' must start with 'enums'" }

        val rscmKey = name.asRSCM()
        return ServerCacheManager.getEnum(rscmKey)
            ?: throw NoSuchElementException("Enum '$name' not found in server cache")
    }

    operator fun get(name: String): EnumType = lookup(name)

    fun EnumType.getInt(key: Int): Int = getValue<Int, Int>(key)
    fun EnumType.getString(key: Int): String = getValue<Int, String>(key)
    fun EnumType.getInt(key: String): Int = getValue<String, Int>(key)
    fun EnumType.getString(key: String): String = getValue<String, String>(key)

    inline fun <reified K : Any, reified V : Any> EnumType.getValue(key: K): V {
        val expectedKeyType = when (K::class) {
            Int::class -> Type.INT
            String::class -> Type.STRING
            else -> throw IllegalArgumentException("Unsupported key type: ${K::class}")
        }

        if (keyType != expectedKeyType) {
            throw IllegalArgumentException(
                "Enum key type mismatch: expected $keyType, got $expectedKeyType"
            )
        }

        val value: Any = when (key) {
            is Int -> values[key.toString()]
            is String -> values[key]
            else -> throw IllegalArgumentException("Unsupported key type: ${key::class}")
        } ?: throw NoSuchElementException("Key '$key' not found in enum id=$id")

        val expectedValueType = when (V::class) {
            Int::class -> Type.INT
            String::class -> Type.STRING
            else -> throw IllegalArgumentException("Unsupported value type: ${V::class}")
        }

        if (valueType != expectedValueType) {
            throw IllegalArgumentException(
                "Enum value type mismatch: expected $valueType, got ${if (value is Int) Type.INT else Type.STRING} for key '$key'"
            )
        }

        return value as V
    }

    inline fun <reified K : Any, reified V : Any> EnumType.forEachTyped(action: (K, V) -> Unit) {
        for ((k, v) in values) {
            val key: K = when (K::class) {
                Int::class -> k.toIntOrNull() as K? ?: throw IllegalArgumentException("Invalid key: $k")
                String::class -> k as K
                else -> throw IllegalArgumentException("Unsupported key type: ${K::class}")
            }

            val value: V = v as? V ?: throw IllegalArgumentException("Invalid value type for key '$k'")
            action(key, value)
        }
    }

    inline fun <reified K : Any, reified V : Any> EnumType.asTypedMap(): Map<K, V> {
        val result = mutableMapOf<K, V>()
        // Specify K and V explicitly
        this.forEachTyped<K, V> { k, v -> result[k] = v }
        return result
    }

}