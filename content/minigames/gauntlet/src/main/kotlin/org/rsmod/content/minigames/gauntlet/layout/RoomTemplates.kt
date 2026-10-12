package org.rsmod.content.minigames.gauntlet.layout

import org.rsmod.content.minigames.gauntlet.GauntletMode

internal object RoomTemplates {
    const val BOSS_ZONE_Z = 710
    const val START_ZONE_Z = 708

    private val VARIANT_ZONE_Z = intArrayOf(704, 706, 708, 710)

    fun zoneX(mode: GauntletMode, kind: RoomKind): Int {
        val base = if (mode.corrupted) CORRUPTED_BASE else NORMAL_BASE
        return base +
            when (kind) {
                RoomKind.MIDDLE -> 0
                RoomKind.EDGE -> 2
                RoomKind.CORNER -> 4
                RoomKind.START,
                RoomKind.BOSS -> 6
            }
    }

    fun zoneZ(room: GauntletRoom): Int =
        when (room.kind) {
            RoomKind.START -> START_ZONE_Z
            RoomKind.BOSS -> BOSS_ZONE_Z
            else -> VARIANT_ZONE_Z[room.variant]
        }

    private const val NORMAL_BASE = 232
    private const val CORRUPTED_BASE = 240
}
