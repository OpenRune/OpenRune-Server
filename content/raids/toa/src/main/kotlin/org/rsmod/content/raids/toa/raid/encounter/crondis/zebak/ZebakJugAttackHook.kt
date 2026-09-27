package org.rsmod.content.raids.toa.raid.encounter.crondis.zebak

import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

/**
 * Attacking a jug doesn't start the attack delay ([ZebakJugs.attacking]). Bound in ToaRaidModule.
 *
 * A hook rather than an op handler: PvNCombat is internal to the combat module, so content can't
 * run the attack itself, but PvNCombat asks every hook right before it sets the delay. Never
 * denies; it only notes the delay for jugs.
 */
class ZebakJugAttackHook : NpcAttackValidateHook {
    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult {
        ZebakEncounter.onJugAttack(player, npc)
        return NpcAttackValidateResult.Pass
    }
}
