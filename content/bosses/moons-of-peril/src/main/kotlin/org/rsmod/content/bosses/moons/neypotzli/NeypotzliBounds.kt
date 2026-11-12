package org.rsmod.content.bosses.moons.neypotzli

import jakarta.inject.Inject
import org.rsmod.api.player.hook.NeypotzliHook
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

class NeypotzliBounds @Inject constructor(private val hooks: Set<@JvmSuppressWildcards NeypotzliHook>) {

    fun contains(player: Player): Boolean {
        val coords = hooks.fold(player.coords) { acc, hook -> hook.normalCoords(player, acc) }
        return contains(coords)
    }

    fun contains(coords: CoordGrid): Boolean =
        BOXES.any { coords.level == it.level && coords.x in it.xs && coords.z in it.zs }

    private class Box(val xs: IntRange, val zs: IntRange, val level: Int)

    private companion object {
        private val BOXES =
            listOf(Box(1344..1535, 9536..9727, 0), Box(1408..1471, 9600..9663, 1))
    }
}
