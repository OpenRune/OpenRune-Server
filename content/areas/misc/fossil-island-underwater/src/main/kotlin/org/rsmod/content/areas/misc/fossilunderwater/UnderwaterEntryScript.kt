package org.rsmod.content.areas.misc.fossilunderwater

import jakarta.inject.Inject
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.MiscOutput
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.events.EventBus
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The way down to the sea floor and back: the north rowboat out to the small island, the small
 * island's rowboat to dive or row back, and the anchor rope up from the sea floor.
 */
class UnderwaterEntryScript @Inject constructor(private val eventBus: EventBus) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(ROWBOAT_NORTH) { travelFromNorth() }
        onOpLoc1(ROWBOAT_DIVING) { travelFromSea() }
        onOpLoc2(ROWBOAT_DIVING) { dive() }
        onOpLoc1(ROPE) { climbRope() }
        // TODO(content): the Museum Camp's and the canal barge's rowboats.
    }

    private suspend fun ProtectedAccess.travelFromNorth() {
        arriveDelay()
        val trip =
            choice4(
                "Row to the barge.",
                BARGE,
                "Row to the camp.",
                CAMP,
                "Row out to sea.",
                SEA,
                "Cancel.",
                null,
            ) ?: return
        row(trip)
    }

    private suspend fun ProtectedAccess.travelFromSea() {
        arriveDelay()
        val trip =
            choice5(
                "Row to the barge.",
                BARGE,
                "Row to the north of the island.",
                NORTH,
                "Row to the camp.",
                CAMP,
                "Dive into the sea.",
                null,
                "Cancel.",
                CANCEL,
            )
        when (trip) {
            null -> dive()
            CANCEL -> return
            else -> row(trip)
        }
    }

    private suspend fun ProtectedAccess.row(trip: Trip) {
        fadeOverlay(0, Underwater.TRANSPARENT, 0, Underwater.SOLID, Underwater.FADE_CYCLES)
        delay(2)
        spam(trip.message)
        telejump(trip.dest, TeleportType.Exempt)
        delay(1)
        fadeOverlay(0, Underwater.SOLID, 0, Underwater.TRANSPARENT, Underwater.FADE_CYCLES)
        closeFadeOverlay(FADE_CLOSE)
    }

    private suspend fun ProtectedAccess.dive() {
        arriveDelay()
        fadeOverlay(0, Underwater.TRANSPARENT, 0, Underwater.SOLID, Underwater.FADE_CYCLES)
        delay(2)
        player.oxygen = Breath.FULL
        player.foundOpen = false
        player.foundWindow = false
        if (player.pufferfishSeen == 0) {
            // UNVERIFIED: live keeps one of the seven pufferfish sets per player; when it is
            // given is not captured.
            player.pufferfishSeen = random.of(1, PUFFERFISH_SETS)
        }
        telejump(Underwater.DIVE_ARRIVAL, TeleportType.Exempt)
        Underwater.openOverlay(player, eventBus)
    }

    private suspend fun ProtectedAccess.climbRope() {
        arriveDelay()
        telejump(Underwater.SMALL_ISLAND, TeleportType.Exempt)
        ifCloseSub(Underwater.OVERLAY)
        MiscOutput.hintArrowReset(player)
    }

    private class Trip(val dest: CoordGrid, val message: String)

    private companion object {
        const val ROWBOAT_NORTH = "loc.fossil_rowboat_north"
        const val ROWBOAT_DIVING = "loc.fossil_rowboat_diving"
        const val ROPE = "loc.fossil_underwater_rope"
        const val PUFFERFISH_SETS = 7
        const val FADE_CLOSE = 2

        private val BARGE = Trip(CoordGrid(3723, 3784, 1), "You row to the barge...")
        private val CAMP = Trip(CoordGrid(3724, 3808, 0), "You row to the expedition camp...")
        private val NORTH = Trip(CoordGrid(3733, 3893, 0), "You row to the north of the island...")
        private val SEA = Trip(Underwater.SMALL_ISLAND, "You row out to sea...")
        private val CANCEL = Trip(Underwater.SMALL_ISLAND, "")
    }
}
