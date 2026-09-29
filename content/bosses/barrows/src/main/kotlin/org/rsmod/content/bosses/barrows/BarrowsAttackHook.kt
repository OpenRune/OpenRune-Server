package org.rsmod.content.bosses.barrows

import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.npc.owner.isSpawnOwnedBy
import org.rsmod.api.npc.owner.isSpawnOwnedByOther
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

internal class BarrowsAttackHook : NpcAttackValidateHook {
    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult {
        if (!npc.isBarrowsNpc()) return NpcAttackValidateResult.Pass
        if (npc.isSpawnOwnedByOther(player)) {
            return NpcAttackValidateResult.Deny("This is not your target.")
        }
        if (npc.isSpawnOwnedBy(player)) {
            return NpcAttackValidateResult.BypassSingleWayPvnRestriction
        }
        return NpcAttackValidateResult.Pass
    }
}
