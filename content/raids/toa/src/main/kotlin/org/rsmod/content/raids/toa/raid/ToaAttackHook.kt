package org.rsmod.content.raids.toa.raid

import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.content.raids.toa.raid.encounter.ToaRooms
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

class ToaAttackHook : NpcAttackValidateHook {
    override val stopsApproach: Boolean = true

    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult =
        ToaRooms.roomOf(player)?.validateAttack(player, npc) ?: NpcAttackValidateResult.Pass
}
