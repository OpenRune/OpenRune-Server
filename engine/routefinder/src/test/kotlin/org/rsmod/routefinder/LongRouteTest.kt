package org.rsmod.routefinder

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

class LongRouteTest {
    // A bounded maze with 32 forced turns. All other tiles stay blocked, including zone padding.
    private fun maze(): CollisionFlagMap {
        val flags = CollisionFlagMap()
        for (x in 3184..3263) for (z in 3184..3263) flags[x, z, 0] = CollisionFlag.LOC
        for (row in 0..16) {
            val z = 3200 + row * 2
            for (x in 3200..3206) flags[x, z, 0] = 0
            if (row < 16) flags[if (row % 2 == 0) 3206 else 3200, z + 1, 0] = 0
        }
        return flags
    }

    @Test
    fun `default route retains destination beyond 25 turns`() {
        val route = RouteFinding(maze()).findRoute(0, 3200, 3200, 3206, 3232)
        assertTrue(route.success)
        assertFalse(route.alternative)
        assertEquals(RouteCoordinates(3206, 3232), route.last())
        assertTrue(route.size > 25, "The fixture must exceed the old cap")
    }

    @Test
    fun `explicit waypoint limit still returns a bounded prefix`() {
        val route = RouteFinding(maze()).findRoute(0, 3200, 3200, 3206, 3232, maxWaypoints = 25)
        assertTrue(route.success)
        assertEquals(25, route.size)
        assertTrue(route.last() != RouteCoordinates(3206, 3232))
        val fullRoute = RouteFinding(maze()).findRoute(0, 3200, 3200, 3206, 3232)
        assertEquals(fullRoute.take(25), route.waypoints)
    }

    @Test
    fun `unreachable tile still uses a nearby approach`() {
        val flags = maze()
        flags[3206, 3232, 0] = CollisionFlag.LOC
        val route = RouteFinding(flags).findRoute(0, 3200, 3200, 3206, 3232)
        assertTrue(route.success)
        assertTrue(route.alternative)
        val last = route.last()
        assertTrue(kotlin.math.abs(last.x - 3206) <= 10)
        assertTrue(kotlin.math.abs(last.z - 3232) <= 10)
        assertEquals(0, flags[last.x, last.z, 0])
    }
}
