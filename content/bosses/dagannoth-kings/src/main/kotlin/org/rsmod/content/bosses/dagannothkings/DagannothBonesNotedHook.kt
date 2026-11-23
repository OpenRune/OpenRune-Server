package org.rsmod.content.bosses.dagannothkings

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.api.death.NpcDeathDropContext
import org.rsmod.api.death.NpcDeathDropHook
import org.rsmod.api.player.output.ClientScripts

internal class DagannothBonesNotedHook : NpcDeathDropHook {
    override fun tryConsume(context: NpcDeathDropContext): Boolean {
        val npc = context.npc ?: return false
        if (npc.visType.id !in BOSS_IDS) return false
        if (context.dropType.id != BONES) return false
        if (context.hero.vars[FREMENNIK_ELITE] == 0) return false

        val noted = ServerCacheManager.getItem(NOTED_BONES.asRSCM()) ?: return false
        val spawned = context.objRepo.add(noted, context.dropCoords, context.duration, context.hero)
        ClientScripts.lootTrackerAddLoot(
            context.hero,
            npc.visType.id,
            context.lootTrackerEventId,
            spawned.type,
            spawned.count,
        )
        return true
    }

    private companion object {
        val BOSS_IDS = DagannothKingsCommon.BOSS_IDS.map { it.asRSCM() }.toSet()
        val BONES = DagannothKingsCommon.BONES.asRSCM()
        val NOTED_BONES = DagannothKingsCommon.NOTED_BONES
        const val FREMENNIK_ELITE = "varbit.fremennik_diary_elite_complete"
    }
}
