package org.rsmod.content.raids.toa.raid.encounter.crondis

import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

/**
 * Offline_Scape Zebak.canAttack / CrondisJug.canAttack: a player washed into the water by Zebak's
 * Tidal Waves can't start combat until they climb out.
 *
 * NpcAttackValidateHook is the engine's content hook for this: PvNCombat runs every bound hook
 * before a player attacks an npc, and a Deny shows its message and stops the attack. Bound in
 * ToaRaidModule, like ToaTeleportHook. Anyone not swimming in a Zebak room passes.
 */
class ZebakSwimAttackHook : NpcAttackValidateHook {
    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult =
        if (ZebakEncounter.isSwimmingInRaid(player)) {
            NpcAttackValidateResult.Deny("You cannot initiate combat while swimming!")
        } else {
            NpcAttackValidateResult.Pass
        }
}
