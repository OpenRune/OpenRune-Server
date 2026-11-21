package org.rsmod.content.areas.misc.tutorialisland

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.spells.attack.SpellCastHook
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity

internal class TutorialSpellCastHook
@Inject
constructor(private val guide: TutorialGuide) : SpellCastHook {
    override suspend fun afterSpell(
        access: ProtectedAccess,
        target: PathingEntity,
        attack: CombatAttack.Spell,
    ) {
        val player = access.player
        if (!player.inTutorial || attack.spell.obj.internalName != WIND_STRIKE) {
            return
        }
        if (target !is Npc ||
            (target.visType.internalName != TutorialNpc.CHICKEN &&
                target.type.internalName != TutorialNpc.CHICKEN)
        ) {
            return
        }
        if (player.tutorialStage == TutorialStage.RUNES_GIVEN) {
            guide.advance(player, TutorialStage.WIND_STRIKE_CAST)
        }
        if (player.tutorialStage == TutorialStage.WIND_STRIKE_CAST) {
            player.statAdvance("stat.magic", 1.5, rate = 1.0, globalRate = 1.0)
            guide.advance(player, TutorialStage.WIND_STRIKE_RESOLVED)
            TutorialHighlights.clearWindStrike(player)
        }
    }

    private companion object {
        private const val WIND_STRIKE = "obj.01_wind_strike"
    }
}
