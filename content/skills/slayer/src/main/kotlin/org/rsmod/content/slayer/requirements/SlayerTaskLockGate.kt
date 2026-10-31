package org.rsmod.content.slayer.requirements

import jakarta.inject.Inject
import org.rsmod.api.config.SlayerTaskCredit
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

internal class SlayerTaskLockAttackHook @Inject constructor() : NpcAttackValidateHook {
    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult {
        if (!isTaskLocked(npc)) return NpcAttackValidateResult.Pass
        if (SlayerTaskCredit.counts(npc.visType, SlayerTaskCredit.taskKey(player))) {
            return NpcAttackValidateResult.Pass
        }
        return NpcAttackValidateResult.Deny(MESSAGE)
    }

    private fun isTaskLocked(npc: Npc): Boolean =
        TASK_LOCKED_PREFIXES.any(npc.visType.internalName::startsWith) &&
            !npc.visType.internalName.endsWith(TENTACLE_SUFFIX)

    private companion object {
        const val MESSAGE = "You need to have a Slayer task to harm this creature."
        const val TENTACLE_SUFFIX = "_tentacle"

        val TASK_LOCKED_PREFIXES =
            listOf(
                "npc.slayer_kraken_boss",
                "npc.smoke_devil",
                "npc.cerberus",
                "npc.abyssalsire",
                "npc.gargboss_",
                "npc.araxxor",
            )
    }
}
