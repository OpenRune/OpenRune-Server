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
        if (context.npc.visType.internalName != TutorialNpc.GIANT_RAT) {
            return
        }
        when (player.tutorialStage) {
            in TutorialStage.RAT_PIT_OPENED until TutorialStage.COMBAT_GUIDE_MELEE ->
                guide.advance(player, TutorialStage.COMBAT_GUIDE_MELEE)
            in TutorialStage.RANGED_GIVEN until TutorialStage.COMBAT_GUIDE_DONE ->
                guide.advance(player, TutorialStage.COMBAT_GUIDE_DONE)
        }
    }
}
