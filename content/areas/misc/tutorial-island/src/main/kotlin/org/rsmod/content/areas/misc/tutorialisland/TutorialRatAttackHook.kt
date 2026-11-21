package org.rsmod.content.areas.misc.tutorialisland

import jakarta.inject.Inject
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

internal class TutorialRatAttackHook
@Inject
constructor(
    private val guide: TutorialGuide,
    private val protectedAccess: ProtectedAccessLauncher,
) : NpcAttackValidateHook {
    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult {
        if (!player.inTutorial) {
            return NpcAttackValidateResult.Pass
        }
        val isChicken =
            npc.visType.internalName == TutorialNpc.CHICKEN ||
                npc.type.internalName == TutorialNpc.CHICKEN
        if (isChicken) {
            if (player.tutorialStage >= TutorialStage.COMPLETE) {
                return deny(player, "You've already done that.<br>Perhaps you should move on.")
            }
            return NpcAttackValidateResult.Pass
        }
        if (npc.visType.internalName !in TutorialNpc.GIANT_RATS &&
            npc.type.internalName !in TutorialNpc.GIANT_RATS
        ) {
            return NpcAttackValidateResult.Pass
        }
        when (player.tutorialStage) {
            TutorialStage.COMBAT_TAB_CLOSED -> guide.advance(player, TutorialStage.RAT_ENGAGED)
            TutorialStage.RANGED_GIVEN -> guide.advance(player, TutorialStage.RAT_ENGAGED_RANGED)
            TutorialStage.RAT_KILLED_MELEE ->
                return deny(
                    player,
                    "You've already done that.<br>Perhaps you should move on to learn about ranged combat.",
                )
            TutorialStage.COMBAT_GUIDE_DONE ->
                return deny(player, "You've already done that.<br>Perhaps you should move on.")
        }
        return NpcAttackValidateResult.Pass
    }

    private fun deny(player: Player, text: String): NpcAttackValidateResult {
        if (player.attr[DENIAL_OPEN] == true) {
            return NpcAttackValidateResult.DenySilent
        }
        player.attr[DENIAL_OPEN] = true
        protectedAccess.launch(player) {
            mesbox(text)
            player.attr[DENIAL_OPEN] = false
        }
        return NpcAttackValidateResult.DenySilent
    }

    private companion object {
        private val DENIAL_OPEN = AttributeKey<Boolean>(temp = true)
    }
}
