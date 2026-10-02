package org.rsmod.content.raids.toa.raid.encounter.crondis.zebak

import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.content.raids.toa.raid.encounter.ToaRooms
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

class ZebakJugAttackHook : NpcAttackValidateHook {
    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult {
        ToaRooms.of<ZebakEncounter>(npc)?.jugs?.attacking(player, npc)
        return NpcAttackValidateResult.Pass
    }
}
