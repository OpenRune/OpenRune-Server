package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.api.attr.AttributeKey

internal data class ContainerSpec(
    val family: String, val symbol: String, val capacity: Int, val perType: Boolean,
    val maxTypes: Int, val bankOnly: Boolean, val autoCollect: Boolean, val accepted: Set<Int>,
) {
    val id = symbol.asRSCM()
    val opened = symbol.endsWith("_open")
    val stateKey = if (family == CollectionBags.FAMILY) CollectionBags.contents else AttributeKey<MutableMap<String, Int>>(persistenceKey = family)
    fun limit(item: Int): Int = if (family == "flamtaer_bag" && item == "obj.swamppaste".asRSCM()) 500 else capacity
}

internal object ContainerCatalog {
    val all: List<ContainerSpec> by lazy {
        javaClass.getResourceAsStream("/containers.tsv")!!.bufferedReader().useLines { lines ->
            lines.drop(1).filter { it.isNotBlank() }.map { line ->
                val v = line.split('\t')
                val accepted = v[7].split(',').filter(String::isNotBlank).map { it.asRSCM() }.toSet()
                check(accepted.isNotEmpty()) { "No contents for ${v[0]}" }
                ContainerSpec(v[0], v[1], v[2].toInt(), v[3] == "TYPE", v[4].toInt(), v[5].toBooleanStrict(), v[6].toBooleanStrict(), accepted)
                    .also { check(ServerCacheManager.getItem(it.id) != null) { it.symbol } }
            }.toList()
        }
    }
    val byId: Map<Int, ContainerSpec> by lazy { all.associateBy { it.id } }
}

/** Long arithmetic prevents capacity overflow at the seed box's maximum stack. */
internal object ContainerCapacity {
    fun remaining(spec: ContainerSpec, contents: Map<String, Int>, item: Int): Int {
        if (item !in spec.accepted) return 0
        val key = item.toString()
        if (key !in contents && contents.size >= spec.maxTypes) return 0
        val used = if (spec.perType) (contents[key] ?: 0).toLong() else contents.values.sumOf { it.toLong() }
        return (spec.limit(item).toLong() - used).coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
    }
}
