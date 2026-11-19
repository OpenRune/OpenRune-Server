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
        if (npc.visType.internalName !in TutorialNpc.GIANT_RATS &&
            npc.type.internalName !in TutorialNpc.GIANT_RATS
        ) {
            return NpcAttackValidateResult.Pass
        }
        when (player.tutorialStage) {
            TutorialStage.COMBAT_TAB_OPENED,
            TutorialStage.COMBAT_TAB_CLOSED -> guide.advance(player, TutorialStage.RAT_ENGAGED)
            TutorialStage.RANGED_GIVEN -> guide.advance(player, TutorialStage.RAT_ENGAGED_RANGED)
        }
        return NpcAttackValidateResult.Pass
    }
}
