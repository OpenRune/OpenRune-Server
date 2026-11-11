package org.rsmod.content.travel.maptransports

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Inject
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
class MapTransportScript @Inject constructor(private val table: MapTransportTable) :
    PluginScript() {
    private val logger = InlineLogger()

    override fun ScriptContext.startup() {
        for ((key, transports) in table.byLocAndOp) {
            val (locId, op) = key
            when (op) {
                1 -> onProtectedEvent<LocUnimplementedEvents.Op1>(locId) { use(transports) }
                2 -> onProtectedEvent<LocUnimplementedEvents.Op2>(locId) { use(transports) }
                3 -> onProtectedEvent<LocUnimplementedEvents.Op3>(locId) { use(transports) }
                4 -> onProtectedEvent<LocUnimplementedEvents.Op4>(locId) { use(transports) }
                else -> onProtectedEvent<LocUnimplementedEvents.Op5>(locId) { use(transports) }
            }
        }
        logger.info {
            "Map transports: ${table.rows.size} rows, ${table.byLocAndOp.size} loc ops served."
        }
    }

    private suspend fun ProtectedAccess.use(transports: List<MapTransport>) =
        with(table) { travel(transports) }
}
