package org.rsmod.content.raids.toa.raid.encounter.apmeken.puzzle

import org.rsmod.api.combat.commons.npc.NpcAttackHook
import org.rsmod.content.raids.toa.raid.encounter.ToaRooms
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

class ApmekenAttackHook : NpcAttackHook {
    override fun onAttack(npc: Npc, target: Player) {
        ToaRooms.of<ApmekenPuzzleEncounter>(npc)?.baboons?.onAttack(npc, target)
    }

    override fun replacesAttack(npc: Npc, target: Player): Boolean =
        ToaRooms.of<ApmekenPuzzleEncounter>(npc)?.baboons?.trySummon(npc, target) ?: false
}
