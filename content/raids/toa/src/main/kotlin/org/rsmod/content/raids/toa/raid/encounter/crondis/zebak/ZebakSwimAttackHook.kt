package org.rsmod.content.raids.toa.raid.encounter.crondis.zebak

import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.content.raids.toa.raid.encounter.ToaRooms
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

class ZebakSwimAttackHook : NpcAttackValidateHook {
    override val stopsApproach: Boolean = true

    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult =
        if (ToaRooms.of<ZebakEncounter>(player)?.water?.isSwimming(player) == true) {
            NpcAttackValidateResult.Deny("I can't hit him from here. I'll have to get back onto the island!")
        } else {
            NpcAttackValidateResult.Pass
        }
}
