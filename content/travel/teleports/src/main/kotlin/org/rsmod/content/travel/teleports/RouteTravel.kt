package org.rsmod.content.travel.teleports

import org.rsmod.api.player.protect.ProtectedAccess

typealias Travel = suspend ProtectedAccess.(TeleportDestination, String) -> Unit

/**
 * Shared travel for the vehicle networks (carpets, minecarts): pick a stop from a list, fade out,
 * land, fade in. Which stop the player is at comes from where they stand, so it works whichever
 * form or loc of the vehicle they clicked.
 */
object RouteTravel {
    private val DEFAULT_TRAVEL: Travel = { stop, message -> ride(stop, message) }
    private const val HUB = 0
    private const val NEARBY = 16
    private const val TRAVEL_TICKS = 4
    private const val FADE_CYCLES = 50
    private const val CANCEL = "Cancel"

    /**
     * Hub-and-spoke networks: each list starts with its hub. From the hub every other stop is
     * offered; from anywhere else only the hub.
     */
    suspend fun ProtectedAccess.travelFromHub(
        networks: List<List<TeleportDestination>>,
        title: String,
        travel: Travel = DEFAULT_TRAVEL,
        depart: (String) -> String,
    ) {
        for (stops in networks) {
            val here = stops.indexOfFirst { it.coords.chebyshevDistance(coords) <= NEARBY }
            if (here == -1) {
                continue
            }
            val choices = if (here == HUB) stops.indices.filter { it != HUB } else listOf(HUB)
            choose(stops, choices, title, depart, travel)
            return
        }
        mes("Nothing leaves from here.")
    }

    /** Point-to-point networks: every stop but the current one is offered. */
    suspend fun ProtectedAccess.travelAnywhere(
        stops: List<TeleportDestination>,
        title: String,
        depart: (String) -> String,
    ) {
        val here = stops.indexOfFirst { it.coords.chebyshevDistance(coords) <= NEARBY }
        choose(stops, stops.indices.filter { it != here }, title, depart, DEFAULT_TRAVEL)
    }

    private suspend fun ProtectedAccess.choose(
        stops: List<TeleportDestination>,
        choices: List<Int>,
        title: String,
        depart: (String) -> String,
        travel: Travel,
    ) {
        val labels = choices.map { stops[it].name } + CANCEL
        val picked = menu(title, hotkeys = true, choices = labels)
        val stop = choices.getOrNull(picked)?.let(stops::get) ?: return
        travel(stop, depart(stop.name))
    }

    suspend fun ProtectedAccess.ride(stop: TeleportDestination, message: String) {
        if (actionDelay > mapClock) {
            return
        }
        ifClose()
        actionDelay = mapClock + TRAVEL_TICKS + 2
        mes(message)
        fadeOverlay(0, 255, 0, 0, FADE_CYCLES)
        delay(TRAVEL_TICKS)
        telejump(stop.coords)
        fadeOverlay(0, 0, 0, 255, FADE_CYCLES)
        closeFadeOverlay(cycles = 2)
    }
}
