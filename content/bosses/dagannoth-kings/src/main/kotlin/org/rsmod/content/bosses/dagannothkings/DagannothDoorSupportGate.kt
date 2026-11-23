package org.rsmod.content.bosses.dagannothkings

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import kotlin.math.abs
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

internal class DagannothDoorSupportGate : NpcAttackValidateHook {
    override val stopsApproach: Boolean = true

    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult {
        val fromFront =
            when (npc.type.id) {
                SOUTH_ID ->
                    player.coords.z < npc.coords.z &&
                        abs(player.coords.x - npc.coords.x) <= FRONT_WIDTH

                NORTH_ID ->
                    player.coords.z > npc.coords.z &&
                        abs(player.coords.x - npc.coords.x) <= FRONT_WIDTH

                WEST_ID ->
                    player.coords.x < npc.coords.x &&
                        abs(player.coords.z - npc.coords.z) <= FRONT_WIDTH

                else -> false
            }
        return if (fromFront) NpcAttackValidateResult.Deny(MESSAGE) else NpcAttackValidateResult.Pass
    }

    private companion object {
        const val FRONT_WIDTH = 1
        const val MESSAGE = "This door does not seem to be openable from this side..."

        val SOUTH_ID = "npc.dagannoth_weak_door_south".asRSCM(RSCMType.NPC)
        val NORTH_ID = "npc.dagannoth_weak_door_north".asRSCM(RSCMType.NPC)
        val WEST_ID = "npc.dagannoth_weak_door_west".asRSCM(RSCMType.NPC)
    }
}
