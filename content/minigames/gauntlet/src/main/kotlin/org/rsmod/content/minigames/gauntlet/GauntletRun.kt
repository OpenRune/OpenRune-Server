package org.rsmod.content.minigames.gauntlet

import org.rsmod.content.minigames.gauntlet.layout.GauntletLayout
import org.rsmod.content.minigames.gauntlet.layout.RoomContents
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

class GauntletRun(
    val layout: GauntletLayout,
    val mode: GauntletMode,
    val contents: Map<Int, RoomContents>,
) {
    val revealed: MutableSet<Int> = hashSetOf(layout.startIndex, GauntletLayout.BOSS_INDEX)
    val charges: MutableMap<CoordGrid, Int> = hashMapOf()
    val nextGather: MutableMap<Player, Int> = hashMapOf()
    var weakFrameGiven = false
    var strongKillsWithoutFrame = 0
    var componentsObtained = 0
    var completed = false
    var hunllef: Npc? = null
    var fighter: Player? = null
}
