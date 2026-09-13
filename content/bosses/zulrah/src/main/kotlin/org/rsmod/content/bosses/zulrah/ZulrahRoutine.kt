package org.rsmod.content.bosses.zulrah

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

internal data class ZulrahPoint(val x: Int = 0, val z: Int = 0)

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

internal data class ZulrahRoutine(
    val source: String,
    val sourceSha256: String,
    val revision: String,
    val mode: String,
    val startTick: Int,
    val endTick: Int,
    val caveat: String,
    val events: List<ZulrahRoutineEvent>,
) {
    init {
        require(endTick > 0 && events.isNotEmpty())
        require(events.first().kind == "emerge" && events.first().tick == 0)
        require(events.zipWithNext().all { (a, b) -> a.tick <= b.tick })
        require(events.all { it.tick in 0 until endTick })
        require(events.all { it.kind in setOf("emerge", "dive", "tail", "attack", "gas", "egg") })
        require(events.filter { it.kind == "gas" || it.kind == "egg" }.all { it.impactDelay > 0 })
        require(events.filter { it.kind == "gas" }.all { it.cloudLifetime > 0 })
    }

    companion object {
        val recorded: ZulrahRoutine by lazy {
            val stream = requireNotNull(ZulrahRoutine::class.java.getResourceAsStream("/zulrah/recorded-routine.json"))
            stream.use { jacksonObjectMapper().readValue(it) }
        }
    }
}
