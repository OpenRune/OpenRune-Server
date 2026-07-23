package org.rsmod.game.entity

public fun Player.timerAt(
    timer: String,
    mapClock: Int,
    cycles: Int,
) {
    require(cycles > 0) {
        "`cycles` must be greater than 0. (cycles=$cycles)"
    }

    timerMap.schedule(
        timer = timer,
        mapClock = mapClock,
        interval = cycles,
    )
}
