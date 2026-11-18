package org.rsmod.content.areas.misc.fossilunderwater

import dev.openrune.types.aconverted.interf.IfSubType
import org.rsmod.api.player.ui.ifOpenFullOverlay
import org.rsmod.api.player.ui.ifOpenSub
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

/** The two map squares of sea floor west of Fossil Island's small island. */
internal object Underwater {
    const val OVERLAY = "interface.fossil_underwater"
    const val FADE_CYCLES = 50
    const val TRANSPARENT = 255
    const val SOLID = 0

    val DIVE_ARRIVAL = CoordGrid(3732, 10281, 1)
    val SMALL_ISLAND = CoordGrid(3769, 3898, 0)

    private const val WATER = "interface.underwater_shallow"
    private const val WATER_TARGET = "component.fossil_underwater:water_overlay"
    private const val LEVEL = 1
    private const val MIN_X = 3712
    private const val MAX_X = 3839
    private const val MIN_Z = 10240
    private const val MAX_Z = 10303

    val ZONES =
        ZoneKey.covering(CoordGrid(MIN_X, MIN_Z, LEVEL), CoordGrid(MAX_X, MAX_Z, LEVEL))

    fun contains(coords: CoordGrid): Boolean =
        coords.level == LEVEL && coords.x in MIN_X..MAX_X && coords.z in MIN_Z..MAX_Z

    fun openOverlay(player: Player, eventBus: EventBus) {
        player.ifOpenFullOverlay(OVERLAY, eventBus)
        // TODO(content): live swaps in underwater_deep over the deeper parts of the cave.
        player.ifOpenSub(WATER, WATER_TARGET, IfSubType.Overlay, eventBus)
    }
}
