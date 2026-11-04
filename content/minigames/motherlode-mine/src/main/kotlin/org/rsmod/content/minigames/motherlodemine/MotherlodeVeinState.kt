package org.rsmod.content.minigames.motherlodemine

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.random.GameRandom
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeConstants
import org.rsmod.game.MapClock
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid

@Singleton
class MotherlodeVeinState @Inject constructor(private val mapClock: MapClock, private val random: GameRandom) {
    private val depleteAtCycle = HashMap<CoordGrid, Int>()
    private val upperVeins = HashMap<CoordGrid, BoundLocInfo>()

    fun startIfAbsent(vein: BoundLocInfo, upperFloor: Boolean) {
        val coords = vein.coords
        if (coords in depleteAtCycle) {
            return
        }
        val range =
            if (upperFloor) {
                MotherlodeConstants.UPPER_VEIN_DEPLETE_TICKS
            } else {
                MotherlodeConstants.LOWER_VEIN_DEPLETE_TICKS
            }
        depleteAtCycle[coords] = mapClock.cycle + random.of(range.first, range.last)
        if (upperFloor) upperVeins[coords] = vein
    }

    fun isDepleted(coords: CoordGrid): Boolean {
        val deadline = depleteAtCycle[coords] ?: return false
        return mapClock.cycle >= deadline
    }

    fun clear(coords: CoordGrid) {
        depleteAtCycle.remove(coords)
        upperVeins.remove(coords)
    }

    fun dueUpperVeins(): List<BoundLocInfo> = upperVeins.values.filter { isDepleted(it.coords) }
}
