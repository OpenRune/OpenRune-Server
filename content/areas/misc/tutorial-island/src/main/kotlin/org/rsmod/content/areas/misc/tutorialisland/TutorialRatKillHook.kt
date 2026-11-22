package org.rsmod.content.areas.misc.tutorialisland

import jakarta.inject.Inject
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook

internal class TutorialRatKillHook
@Inject
constructor(private val guide: TutorialGuide) : NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        val player = context.hero
        if (!player.inTutorial) {
            return
        }
        if (context.npc.visType.internalName !in TutorialNpc.GIANT_RATS &&
            context.npc.type.internalName !in TutorialNpc.GIANT_RATS
        ) {
            return
        }
        when (player.tutorialStage) {
            TutorialStage.RAT_ENGAGED -> guide.advance(player, TutorialStage.RAT_KILLED_MELEE)
            TutorialStage.RAT_ENGAGED_RANGED -> {
                guide.advance(player, TutorialStage.COMBAT_GUIDE_DONE)
                TutorialHighlights.clearItems(player)
            }
        }
    }
}
