package org.rsmod.content.bosses.dagannothkings

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

internal class DagannothWaterGate : NpcAttackValidateHook {
    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult {
        if (npc.visType.id !in WATER_IDS) return NpcAttackValidateResult.Pass
        return NpcAttackValidateResult.Deny("You can't attack this npc.")
    }

    private companion object {
        val WATER_IDS =
            listOf(
                    "npc.dagannoth_water_creature_1x1_invisible",
                    "npc.dagannoth_water_creature_1x1_invisible2",
                    "npc.dagannoth_water_creature_1x1_invisible3",
                )
                .map { it.asRSCM(RSCMType.NPC) }
                .toSet()
    }
}
