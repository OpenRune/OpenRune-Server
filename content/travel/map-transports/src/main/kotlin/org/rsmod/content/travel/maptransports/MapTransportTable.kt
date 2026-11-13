package org.rsmod.content.travel.maptransports

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.protect.ProtectedAccess

/**
 * The Shortest Path transport rows keyed by loc id and op slot. Scripts that own a loc's op
 * themselves can still hand a click on a copy they don't handle to [travel].
 */
@Singleton
class MapTransportTable @Inject constructor() {
    val rows: List<MapTransport> = FILES.flatMap { file -> MapTransports.parse(read(file)) }

    val byLocAndOp: Map<Pair<Int, Int>, List<MapTransport>> = buildMap<Pair<Int, Int>, MutableList<MapTransport>> {
        for (row in rows) {
            val locId = row.loc.asRSCM(RSCMType.LOC)
            val type = ServerCacheManager.getObject(locId) ?: continue
            val op =
                (1..5).firstOrNull {
                    type.actions.getOpOrNull(it - 1).equals(row.option, ignoreCase = true)
                } ?: continue
            getOrPut(locId to op) { mutableListOf() } += row
        }
    }

    suspend fun ProtectedAccess.travel(locId: Int, op: Int): Boolean {
        val transports = byLocAndOp[locId to op] ?: return false
        travel(transports)
        return true
    }

    suspend fun ProtectedAccess.travel(transports: List<MapTransport>) {
        val transport = MapTransports.pick(transports, player.coords) ?: return
        for ((stat, level) in transport.skills) {
            if (statBase(stat) < level) {
                val name = stat.removePrefix("stat.").replaceFirstChar { it.uppercase() }
                mes("You need a $name level of $level to do that.")
                return
            }
        }
        arriveDelay()
        telejump(transport.destination)
    }

    private fun read(file: String): String =
        MapTransportTable::class.java.classLoader.getResourceAsStream("transports/$file")
            ?.bufferedReader()
            ?.use { it.readText() }
            .orEmpty()

    private companion object {
        val FILES =
            listOf(
                "transports.tsv",
                "agility_shortcuts.tsv",
                "teleportation_levers.tsv",
                "teleportation_portals.tsv",
            )
    }
}
