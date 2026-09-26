package org.rsmod.content.raids.toa.raid.encounter.crondis.zebak

import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

/** No starting combat while swimming (Offline_Scape Zebak.canAttack). Bound in ToaRaidModule. */
class ZebakSwimAttackHook : NpcAttackValidateHook {
    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult =
        if (ZebakEncounter.isSwimming(player)) {
            NpcAttackValidateResult.Deny("You cannot initiate combat while swimming!")
        } else {
            NpcAttackValidateResult.Pass
        }
}
