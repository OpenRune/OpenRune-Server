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

    private fun isTaskLocked(npc: Npc): Boolean {
        val name = npc.visType.internalName
        if (name in TASK_LOCKED_NAMES) return true
        return TASK_LOCKED_PREFIXES.any(name::startsWith) && TENTACLE_MARKER !in name
    }

    private companion object {
        const val MESSAGE = "You need to have a Slayer task to harm this creature."
        const val TENTACLE_MARKER = "_tentacle"

        val TASK_LOCKED_NAMES = setOf("npc.slayer_kraken")

        val TASK_LOCKED_PREFIXES =
            listOf(
                "npc.slayer_kraken_boss",
                "npc.slayer_kraken_sub",
                "npc.smoke_devil",
                "npc.cerberus",
                "npc.abyssalsire",
                "npc.gargboss_",
                "npc.araxxor",
            )
    }
}
