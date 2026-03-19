package dev.openrune.types.enums

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM

public inline fun <reified K : Any, reified V : Any> enum(internal: String): EnumTypeMap<K, V> {
    val raw = ServerCacheManager.getEnum("enum.${internal}".asRSCM()) ?: error("Error finding enum")
    return EnumTypeMap(raw)
}
