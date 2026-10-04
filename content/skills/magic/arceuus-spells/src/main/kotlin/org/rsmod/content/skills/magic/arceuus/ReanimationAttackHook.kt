package org.rsmod.content.skills.magic.arceuus

import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.npc.owner.isSpawnOwnedByOther
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

internal class ReanimationAttackHook : NpcAttackValidateHook {
    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult {
        if (npc.id !in ReanimatedHead.byNpcId) return NpcAttackValidateResult.Pass
        if (npc.isSpawnOwnedByOther(player)) {
            return NpcAttackValidateResult.Deny("You cannot attack that creature.")
        }
        return NpcAttackValidateResult.Pass
    }
}
