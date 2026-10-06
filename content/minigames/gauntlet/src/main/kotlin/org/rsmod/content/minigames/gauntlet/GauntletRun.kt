package org.rsmod.content.minigames.gauntlet

import org.rsmod.content.minigames.gauntlet.layout.GauntletLayout
import org.rsmod.content.minigames.gauntlet.layout.RoomContents
import org.rsmod.map.CoordGrid

class GauntletRun(
    val layout: GauntletLayout,
    val mode: GauntletMode,
    val contents: Map<Int, RoomContents>,
) {
    val revealed: MutableSet<Int> = hashSetOf(layout.startIndex, GauntletLayout.BOSS_INDEX)
    val charges: MutableMap<CoordGrid, Int> = hashMapOf()
    var weaponFrameGiven = false
}
