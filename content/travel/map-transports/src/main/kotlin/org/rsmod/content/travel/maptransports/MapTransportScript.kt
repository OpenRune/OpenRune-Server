package org.rsmod.content.travel.maptransports

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.events.interact.LocUnimplementedEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onProtectedEvent
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Serves the Shortest Path plugin's transport rows with a known start and end tile for every loc
 * op no other script answers. It listens on the unimplemented-op fallback, which the engine
 * only reaches once a loc has no id, content or category handler.
 *
 * The bundled rows are refreshed by the revision sync workflow or `downloadTransports`.
 */
class MapTransportScript : PluginScript() {
    private val logger = InlineLogger()

    override fun ScriptContext.startup() {
        val rows = FILES.flatMap { file -> MapTransports.parse(read(file)) }
        val byLocAndOp = HashMap<Pair<Int, Int>, MutableList<MapTransport>>()
        for (row in rows) {
            val locId = row.loc.asRSCM(RSCMType.LOC)
            val type = ServerCacheManager.getObject(locId) ?: continue
            val op =
                (1..5).firstOrNull {
                    type.actions.getOpOrNull(it - 1).equals(row.option, ignoreCase = true)
                } ?: continue
            byLocAndOp.getOrPut(locId to op) { mutableListOf() } += row
        }
        for ((key, transports) in byLocAndOp) {
            val (locId, op) = key
            when (op) {
                1 -> onProtectedEvent<LocUnimplementedEvents.Op1>(locId) { use(transports) }
                2 -> onProtectedEvent<LocUnimplementedEvents.Op2>(locId) { use(transports) }
                3 -> onProtectedEvent<LocUnimplementedEvents.Op3>(locId) { use(transports) }
                4 -> onProtectedEvent<LocUnimplementedEvents.Op4>(locId) { use(transports) }
                else -> onProtectedEvent<LocUnimplementedEvents.Op5>(locId) { use(transports) }
            }
        }
        logger.info { "Map transports: ${rows.size} rows, ${byLocAndOp.size} loc ops served." }
    }

    private suspend fun ProtectedAccess.use(transports: List<MapTransport>) {
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
        MapTransportScript::class.java.classLoader.getResourceAsStream("transports/$file")
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
