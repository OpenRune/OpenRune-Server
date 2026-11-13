package org.rsmod.content.bosses.zulrah

internal data class ZulrahPoint(
    val x: Int = 0,
    val z: Int = 0,
)

internal data class ZulrahRoutineEvent(
    val tick: Int,
    val kind: String,
    val symbol: String = "",
    val x: Int = 0,
    val z: Int = 0,
    val initial: Boolean = false,
    val source: ZulrahPoint = ZulrahPoint(),
    val target: ZulrahPoint = ZulrahPoint(),
    val starttime: Int = 0,
    val endtime: Int = 0,
    val angle: Int = 0,
    val progress: Int = 0,
    val startheight: Int = 0,
    val endheight: Int = 0,
    val impactDelay: Int = 0,
    val cloudLifetime: Int = 0,
    val rotation: Int = 0,
    val spawn: String = "",
    val mixedAttack: Boolean = false,
)
