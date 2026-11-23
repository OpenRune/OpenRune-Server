package org.rsmod.content.other.travelcoverage

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ObjectServerType
import jakarta.inject.Inject
import java.nio.file.Path
import kotlin.concurrent.thread
import kotlin.system.exitProcess
import org.rsmod.api.game.process.PluginScriptBootGate
import org.rsmod.api.player.events.interact.LocCategoryEvents
import org.rsmod.api.player.events.interact.LocContentEvents
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.events.interact.LocUnimplementedEvents
import org.rsmod.content.generic.locs.autotravel.TravelLoc
import org.rsmod.content.generic.locs.autotravel.TravelType
import org.rsmod.events.EventBus
import org.rsmod.events.SuspendEvent
import org.rsmod.game.map.LocZoneStorage
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Writes `build/reports/travel-coverage/` listing every travel op on the map that no script
 * answers, then stops the server. Run it with `gradlew travelReport`, which boots the server with
 * `OPENRUNE_TRAVEL_REPORT=1` so the report sees every script's handlers.
 */
class TravelCoverageScript
@Inject
constructor(
    private val locs: LocZoneStorage,
    private val eventBus: EventBus,
    private val bootGate: PluginScriptBootGate,
) : PluginScript() {
    private val logger = InlineLogger()

    override fun ScriptContext.startup() {
        if (System.getenv(REPORT_ENV) != "1") return
        thread(isDaemon = true, name = "travel-coverage") {
            bootGate.awaitReady()
            logger.info { "Travel coverage written to ${writeReport().toAbsolutePath()}" }
            exitProcess(0)
        }
    }

    private fun writeReport(): Path {
        val placed = placedLocs()
        val result = TravelCoverage.scan(placed.asSequence(), ::objectType, ::isHandled)
        val unhandledIds = result.unhandled.mapTo(HashSet()) { it.locId }
        val ladders = LadderPairing.pair(placed, unhandledIds, ::ladderType)
        LadderTagWriter.write(ladders, REPORT_DIR, ::objectType, ::internalName)
        val doorTypes = doorTypes()
        val closedDoors = result.unhandled.filter { it.op == 1 }.map { internalName(it.locId) }
        DoorTagWriter.write(DoorPairing.pair(closedDoors, doorTypes), REPORT_DIR)
        writeAutoTravelTags(unhandledIds)
        return TravelCoverageReport.write(result, REPORT_DIR, ::internalName)
    }

    private fun writeAutoTravelTags(unhandledIds: Set<Int>) {
        val byTile = HashMap<CoordGrid, MutableList<TravelLoc>>()
        val byId = HashMap<Int, MutableList<TravelLoc>>()
        locs.mapLocs.forEachLoc { zone, key, entity ->
            val loc = TravelLoc(entity.id, zone.toCoords().translate(key.x, key.z), entity.angle)
            byTile.getOrPut(loc.coords) { mutableListOf() } += loc
            byId.getOrPut(loc.id) { mutableListOf() } += loc
        }
        val result =
            AutoTravelPairing.pair(
                placed = byId,
                unhandledIds = unhandledIds,
                type = ::travelType,
                contentGroup = { objectType(it)?.contentGroup ?: -1 },
                symbol = ::internalName,
                idOf = { runCatching { RSCM.getRSCM(it) }.getOrNull() },
                locsAt = { byTile[it].orEmpty() },
            )
        AutoTravelTagWriter.write(result, REPORT_DIR)
    }

    private fun travelType(id: Int): TravelType? {
        val type = objectType(id) ?: return null
        val ops = (0 until 5).map { type.actions.getOpOrNull(it).orEmpty() }
        return TravelType(type.name, ops, type.width, type.length)
    }

    private fun doorTypes(): Map<String, DoorType> =
        ServerCacheManager.getObjects()
            .values
            .filter { it.name.lowercase().let { name -> "door" in name || "gate" in name } }
            .associate { type ->
                val symbol = internalName(type.id)
                val ops = (0 until 5).map { type.actions.getOpOrNull(it).orEmpty() }
                symbol to DoorType(symbol, type.name, ops, type.contentGroup)
            }

    private fun placedLocs(): List<PlacedLoc> {
        val placed = ArrayList<PlacedLoc>(locs.mapLocCount())
        locs.mapLocs.forEachLoc { zone, key, entity ->
            placed += PlacedLoc(entity.id, zone.toCoords().translate(key.x, key.z))
        }
        return placed
    }

    private fun ladderType(id: Int): LadderType? {
        val type = objectType(id) ?: return null
        val ops = (0 until 5).map { type.actions.getOpOrNull(it).orEmpty() }
        return LadderType(type.name, ops, type.contentGroup)
    }

    private fun objectType(id: Int): ObjectServerType? = ServerCacheManager.getObject(id)

    private fun internalName(id: Int): String =
        runCatching { RSCM.getReverseMapping(RSCMType.LOC, id) }.getOrDefault("loc.$id")

    private fun isHandled(type: ObjectServerType, op: Int): Boolean {
        val index = op - 1
        if (answers(TYPE_EVENTS[index], type.id) || answers(TYPE_APPROACH[index], type.id)) {
            return true
        }
        if (answers(FALLBACK_EVENTS[index], type.id)) {
            return true
        }
        val content = type.contentGroup
        if (content != -1) {
            if (answers(CONTENT_EVENTS[index], content)) return true
            if (answers(CONTENT_APPROACH[index], content)) return true
        }
        val category = type.category
        if (category != -1) {
            if (answers(CATEGORY_EVENTS[index], category)) return true
            if (answers(CATEGORY_APPROACH[index], category)) return true
        }
        return false
    }

    private fun answers(event: Class<out SuspendEvent<*>>, key: Int): Boolean =
        eventBus.contains(event, key.toLong())

    private companion object {
        const val REPORT_ENV = "OPENRUNE_TRAVEL_REPORT"
        val REPORT_DIR: Path = Path.of("build", "reports", "travel-coverage")

        val TYPE_EVENTS =
            listOf(
                LocEvents.Op1::class.java,
                LocEvents.Op2::class.java,
                LocEvents.Op3::class.java,
                LocEvents.Op4::class.java,
                LocEvents.Op5::class.java,
            )
        val FALLBACK_EVENTS =
            listOf(
                LocUnimplementedEvents.Op1::class.java,
                LocUnimplementedEvents.Op2::class.java,
                LocUnimplementedEvents.Op3::class.java,
                LocUnimplementedEvents.Op4::class.java,
                LocUnimplementedEvents.Op5::class.java,
            )
        val TYPE_APPROACH =
            listOf(
                LocEvents.Ap1::class.java,
                LocEvents.Ap2::class.java,
                LocEvents.Ap3::class.java,
                LocEvents.Ap4::class.java,
                LocEvents.Ap5::class.java,
            )
        val CONTENT_EVENTS =
            listOf(
                LocContentEvents.Op1::class.java,
                LocContentEvents.Op2::class.java,
                LocContentEvents.Op3::class.java,
                LocContentEvents.Op4::class.java,
                LocContentEvents.Op5::class.java,
            )
        val CONTENT_APPROACH =
            listOf(
                LocContentEvents.Ap1::class.java,
                LocContentEvents.Ap2::class.java,
                LocContentEvents.Ap3::class.java,
                LocContentEvents.Ap4::class.java,
                LocContentEvents.Ap5::class.java,
            )
        val CATEGORY_EVENTS =
            listOf(
                LocCategoryEvents.Op1::class.java,
                LocCategoryEvents.Op2::class.java,
                LocCategoryEvents.Op3::class.java,
                LocCategoryEvents.Op4::class.java,
                LocCategoryEvents.Op5::class.java,
            )
        val CATEGORY_APPROACH =
            listOf(
                LocCategoryEvents.Ap1::class.java,
                LocCategoryEvents.Ap2::class.java,
                LocCategoryEvents.Ap3::class.java,
                LocCategoryEvents.Ap4::class.java,
                LocCategoryEvents.Ap5::class.java,
            )
    }
}
