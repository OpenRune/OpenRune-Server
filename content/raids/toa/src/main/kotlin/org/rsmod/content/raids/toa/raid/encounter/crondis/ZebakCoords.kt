package org.rsmod.content.raids.toa.raid.encounter.crondis.zebak

import org.rsmod.map.CoordGrid

/** Static coordinates of the Zebak room (Offline_Scape unless noted). Convert with `coords()`. */
internal object ZebakCoords {
    val ZEBAK = CoordGrid(3918, 5404, 0)
    val TAIL = CoordGrid(3909, 5403, 0)

    /** Capture: where the damage sound plays. */
    val CENTRE = CoordGrid(3922, 5408, 0)

    /** Where roar sounds and dust ripples start. */
    val MIDDLE = CoordGrid(3926, 5408, 0)

    val PROJECTILE_START = CoordGrid(3925, 5408, 0)
    val PROJECTILE_BASE = CoordGrid(3933, 5408, 0)
    val SPLIT_HELPER = CoordGrid(3930, 5405, 0)

    /** The arena floor: acid, jugs, roar dust. */
    val GROUND_MIN = CoordGrid(3926, 5398, 0)
    val GROUND_MAX = CoordGrid(3942, 5418, 0)

    val BOULDER_MIN = CoordGrid(3925, 5401, 0)
    val BOULDER_MAX = CoordGrid(3935, 5415, 0)

    val BLOOD_SPELL = listOf(CoordGrid(3924, 5406, 0), CoordGrid(3925, 5410, 0))

    /** North side, south side. */
    val BLOOD_CLOUDS = listOf(CoordGrid(3931, 5413, 0), CoordGrid(3934, 5401, 0))

    /** West end of each wave row. */
    val WAVE_SOUTH = CoordGrid(3923, 5397, 0)
    val WAVE_NORTH = CoordGrid(3923, 5419, 0)

    /** Capture (one sample, low confidence; Offline_Scape's differ by 1-4 tiles). */
    val WATER_CROCS =
        listOf(
            CoordGrid(3948, 5408, 0),
            CoordGrid(3948, 5408, 0),
            CoordGrid(3935, 5422, 0),
            CoordGrid(3934, 5420, 0),
            CoordGrid(3921, 5418, 0),
            CoordGrid(3937, 5396, 0),
            CoordGrid(3937, 5395, 0),
            CoordGrid(3919, 5403, 0),
        )
}
