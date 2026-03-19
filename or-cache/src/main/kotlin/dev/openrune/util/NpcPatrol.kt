package dev.openrune.util

public data class NpcPatrol(public val waypoints: List<NpcPatrolWaypoint>) :
    List<NpcPatrolWaypoint> by waypoints {
    public fun coordList(): List<Coord> = waypoints.map { it.destination }
}
