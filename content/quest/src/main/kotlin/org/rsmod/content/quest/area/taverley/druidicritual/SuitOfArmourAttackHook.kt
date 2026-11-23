package org.rsmod.content.quest.area.taverley.druidicritual

import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.npc.owner.isSpawnOwnedByOther
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

class SuitOfArmourAttackHook : NpcAttackValidateHook {
    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult {
        if (!npc.isType(SUIT_OF_ARMOUR) || !npc.isSpawnOwnedByOther(player)) {
            return NpcAttackValidateResult.Pass
        }
        return NpcAttackValidateResult.Deny(NOT_YOUR_TARGET_MESSAGE)
    }

    private companion object {
        const val SUIT_OF_ARMOUR = "npc.suit_of_armour"
        const val NOT_YOUR_TARGET_MESSAGE = "This is not your target."
    }
}
