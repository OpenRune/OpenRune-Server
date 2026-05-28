package org.rsmod.content.slayer.superior

import jakarta.inject.Inject
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.npc.owner.hasSpawnOwner
import org.rsmod.api.npc.owner.isSpawnOwnedBy
import org.rsmod.api.npc.owner.isSpawnOwnedByOther
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

class SlayerSuperiorAttackHook @Inject constructor() : NpcAttackValidateHook {
    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult {
        if (!npc.hasSpawnOwner) {
            return NpcAttackValidateResult.Pass
        }
        if (npc.isSpawnOwnedByOther(player)) {
            return NpcAttackValidateResult.Deny(NOT_YOUR_SUPERIOR_MESSAGE)
        }
        if (npc.isSpawnOwnedBy(player)) {
            return NpcAttackValidateResult.BypassSingleWayPvnRestriction
        }
        return NpcAttackValidateResult.Pass
    }

    private companion object {
        const val NOT_YOUR_SUPERIOR_MESSAGE = "That is not your superior creature."
    }
}
