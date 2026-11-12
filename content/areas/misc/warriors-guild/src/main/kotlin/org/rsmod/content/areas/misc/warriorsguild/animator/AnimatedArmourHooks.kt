package org.rsmod.content.areas.misc.warriorsguild.animator

import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.npc.owner.isSpawnOwnedBy
import org.rsmod.api.npc.owner.isSpawnOwnedByOther
import org.rsmod.api.player.output.MiscOutput
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

internal class AnimatedArmourKillHook : NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        val npc = context.npc
        if (AnimatorSets.byNpc(npc.id) == null || !npc.isSpawnOwnedBy(context.hero)) return
        AnimatorSets.releaseHold(context.hero, npc.id)
        MiscOutput.hintArrowReset(context.hero)
    }
}

internal class AnimatedArmourAttackHook : NpcAttackValidateHook {
    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult {
        if (AnimatorSets.byNpc(npc.id) == null) return NpcAttackValidateResult.Pass
        if (npc.isSpawnOwnedByOther(player)) {
            return NpcAttackValidateResult.Deny("That's not your armour to fight.")
        }
        return NpcAttackValidateResult.Pass
    }
}
