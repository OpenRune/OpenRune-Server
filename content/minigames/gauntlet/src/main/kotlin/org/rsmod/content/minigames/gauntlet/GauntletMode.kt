package org.rsmod.content.minigames.gauntlet

import org.rsmod.api.instances.RegionLocal
import org.rsmod.map.CoordGrid

enum class GauntletMode(val corrupted: Boolean, val startRoomZoneX: Int) {
    NORMAL(corrupted = false, startRoomZoneX = 238),
    CORRUPTED(corrupted = true, startRoomZoneX = 246);

    val timerTicks: Int
        get() = if (corrupted) CORRUPTED_TICKS else NORMAL_TICKS

    fun enterSource(): CoordGrid =
        CoordGrid(
            startRoomZoneX * ZONE_SIZE + ENTER_OFFSET_X,
            GauntletZones.START_ROOM_ZONE_Z * ZONE_SIZE + ENTER_OFFSET_Z,
            GauntletZones.WALKABLE_LEVEL,
        )

    fun enterTile(): RegionLocal {
        val source = enterSource()
        return RegionLocal(source.level, source.mx, source.mz, source.lx, source.lz)
    }

    private companion object {
        const val ZONE_SIZE = 8
        const val ENTER_OFFSET_X = 11
        const val ENTER_OFFSET_Z = 10
        const val NORMAL_TICKS = 1000
        const val CORRUPTED_TICKS = 750
    }
}

internal object GauntletZones {
    const val START_ROOM_ZONE_Z = 708
    const val ROOM_ZONES = 2
    const val WALKABLE_LEVEL = 1
}

private val SHARED_OBJS =
    setOf(
        "pestle",
        "food",
        "raw_food",
        "burnt_food",
        "potion_unfinished",
        "potion_1",
        "potion_2",
        "potion_3",
        "potion_4",
        "vial_water",
    )

internal fun gauntletObj(name: String, corrupted: Boolean): String =
    if (corrupted && name !in SHARED_OBJS) "obj.gauntlet_${name}_hm" else "obj.gauntlet_$name"
