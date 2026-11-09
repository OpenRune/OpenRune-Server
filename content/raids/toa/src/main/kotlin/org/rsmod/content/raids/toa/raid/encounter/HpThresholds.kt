package org.rsmod.content.raids.toa.raid.encounter

import org.rsmod.game.entity.Npc

internal class HpThresholds(private val fractions: DoubleArray, private val catchUp: Boolean) {
    private var next = 0

    fun reset() {
        next = 0
    }

    fun crossed(npc: Npc): Boolean {
        if (!reached(npc, next)) return false
        next++
        if (!catchUp) while (reached(npc, next)) next++
        return true
    }

    private fun reached(npc: Npc, index: Int): Boolean {
        val fraction = fractions.getOrNull(index) ?: return false
        return npc.hitpoints <= npc.baseHitpointsLvl * fraction
    }
}
