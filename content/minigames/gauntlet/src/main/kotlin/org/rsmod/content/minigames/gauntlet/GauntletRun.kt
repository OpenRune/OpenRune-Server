package org.rsmod.content.minigames.gauntlet

import org.rsmod.content.minigames.gauntlet.layout.GauntletLayout

class GauntletRun(val layout: GauntletLayout, val mode: GauntletMode) {
    val revealed: MutableSet<Int> = hashSetOf(layout.startIndex, GauntletLayout.BOSS_INDEX)
}
