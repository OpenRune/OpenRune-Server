package org.rsmod.content.slayer

import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.player.events.PlayerSlayerEvents
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.content.slayer.core.SlayerTaskManager
import org.rsmod.content.slayer.items.BraceletOfSlaughter
import org.rsmod.content.slayer.items.ExpeditiousBracelet
import org.rsmod.content.slayer.superior.SlayerSuperiorManager
import org.rsmod.events.EventBus

class SlayerNpcKillHook @Inject constructor(
    private val npcRepo: NpcRepository,
    private val superiors: SlayerSuperiorManager,
    private val areaChecker: AreaChecker,
    private val random: GameRandom,
    private val eventBus: EventBus,
) : NpcDeathKillHook {

    override fun onKill(context: NpcDeathKillContext) {
        if (superiors.isSuperior(context.npc)) {
            superiors.onSuperiorDeath(context.npc)
        }

        if (!SlayerTaskManager.countsKillTowardTask(context.hero, context.npc, areaChecker)) return
        if (!SlayerTaskManager.isTaskKill(context.hero, context.npc)) return

        val prepare =
            PlayerSlayerEvents.KillPrepare(
                player = context.hero,
                npc = context.npc,
                extraKills = ExpeditiousBracelet.rollExtraKill(context.hero, random),
                skipDecrement = BraceletOfSlaughter.rollSkipTaskDecrement(context.hero, context.npc, random),
            )
        eventBus.publish(prepare)

        if (SlayerTaskManager.decreaseTask(context.hero, context.npc, eventBus, prepare.extraKills, prepare.skipDecrement)) {
            superiors.trySpawnSuperior(context.hero, context.npc, npcRepo)
        }
    }
}
