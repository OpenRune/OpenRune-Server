package org.rsmod.content.slayer.kill

import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.content.slayer.core.SlayerTaskManager
import org.rsmod.content.slayer.items.BraceletOfSlaughter
import org.rsmod.content.slayer.items.ExpeditiousBracelet
import org.rsmod.content.slayer.superior.SlayerSuperiorManager

class SlayerNpcKillHook @Inject constructor(
    private val npcRepo: NpcRepository,
    private val superiors: SlayerSuperiorManager,
    private val areaChecker: AreaChecker,
    private val random: GameRandom,
) : NpcDeathKillHook {

    override fun onKill(context: NpcDeathKillContext) {
        if (superiors.isSuperior(context.npc)) {
            superiors.onSuperiorDeath(context.npc)
        }

        if (!SlayerTaskManager.countsKillTowardTask(context.hero, context.npc, areaChecker)) return

        val extraKills = ExpeditiousBracelet.rollExtraKill(context.hero, random)
        val skipDecrement = BraceletOfSlaughter.rollSkipTaskDecrement(context.hero, context.npc, random)
        if (SlayerTaskManager.decreaseTask(context.hero, context.npc, extraCountDecrement = extraKills, skipCountDecrement = skipDecrement)) {
            superiors.trySpawnSuperior(context.hero, context.npc, npcRepo)
        }
    }
}
