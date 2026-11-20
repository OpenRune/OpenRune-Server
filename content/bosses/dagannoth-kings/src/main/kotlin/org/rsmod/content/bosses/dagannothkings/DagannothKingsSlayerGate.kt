package org.rsmod.content.bosses.dagannothkings

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.config.SlayerTaskCredit
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

internal class DagannothKingsSlayerGate : NpcAttackValidateHook {
    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult {
        if (npc.visType.id !in BOSS_IDS) return NpcAttackValidateResult.Pass
        if (npc.coords.z !in SLAYER_LAIR_Z) return NpcAttackValidateResult.Pass
        if (SlayerTaskCredit.counts(npc.visType, SlayerTaskCredit.taskKey(player))) {
            return NpcAttackValidateResult.Pass
        }
        return NpcAttackValidateResult.Deny(
            "You need to be on a Dagannoth Slayer task to damage this creature."
        )
    }

    private companion object {
        val BOSS_IDS =
            setOf(
                "npc.dagcave_melee_boss".asRSCM(RSCMType.NPC),
                "npc.dagcave_magic_boss".asRSCM(RSCMType.NPC),
                "npc.dagcave_ranged_boss".asRSCM(RSCMType.NPC),
            )
        val SLAYER_LAIR_Z = 4352..4415
    }
}
