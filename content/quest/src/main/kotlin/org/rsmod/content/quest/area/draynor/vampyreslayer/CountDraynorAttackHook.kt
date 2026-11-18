package org.rsmod.content.quest.area.draynor.vampyreslayer

import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.npc.owner.isSpawnOwnedByOther
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

class CountDraynorAttackHook : NpcAttackValidateHook {
    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult {
        if (!npc.isType(COUNT_DRAYNOR) || !npc.isSpawnOwnedByOther(player)) {
            return NpcAttackValidateResult.Pass
        }
        return NpcAttackValidateResult.Deny(NOT_YOUR_TARGET_MESSAGE)
    }

    private companion object {
        const val COUNT_DRAYNOR = "npc.count_draynor"
        const val NOT_YOUR_TARGET_MESSAGE = "This is not your target."
    }
}
