package org.rsmod.content.areas.misc.tutorialisland

import jakarta.inject.Inject
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

internal class TutorialRatAttackHook
@Inject
constructor(private val guide: TutorialGuide) : NpcAttackValidateHook {
    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult {
        if (!player.inTutorial) {
            return NpcAttackValidateResult.Pass
        }
        val isChicken =
            npc.visType.internalName == TutorialNpc.CHICKEN ||
                npc.type.internalName == TutorialNpc.CHICKEN
        if (isChicken) {
            if (player.tutorialStage >= TutorialStage.COMPLETE) {
                return NpcAttackValidateResult.Deny(
                    "You've already done that. Perhaps you should move on.",
                )
            }
            return NpcAttackValidateResult.Pass
        }
        if (npc.visType.internalName !in TutorialNpc.GIANT_RATS &&
            npc.type.internalName !in TutorialNpc.GIANT_RATS
        ) {
            return NpcAttackValidateResult.Pass
        }
        when (player.tutorialStage) {
            TutorialStage.COMBAT_TAB_OPENED,
            TutorialStage.COMBAT_TAB_CLOSED -> guide.advance(player, TutorialStage.RAT_ENGAGED)
            TutorialStage.RANGED_GIVEN -> guide.advance(player, TutorialStage.RAT_ENGAGED_RANGED)
            TutorialStage.RAT_KILLED_MELEE ->
                return NpcAttackValidateResult.Deny(
                    "You've already done that. Perhaps you should move on to learn about ranged combat.",
                )
            TutorialStage.COMBAT_GUIDE_DONE ->
                return NpcAttackValidateResult.Deny(
                    "You've already done that. Perhaps you should move on.",
                )
        }
        return NpcAttackValidateResult.Pass
    }
}
