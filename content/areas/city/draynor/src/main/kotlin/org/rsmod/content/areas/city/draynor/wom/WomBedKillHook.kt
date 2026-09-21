package org.rsmod.content.areas.city.draynor.wom

import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook

class WomBedKillHook : NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        if (!context.npc.type.isType("npc.wom_bed_active")) return
        val hero = context.hero
        if (hero.womTask == WOM_TASK_BED) hero.womTask = WOM_TASK_BED_KILLED
    }
}
