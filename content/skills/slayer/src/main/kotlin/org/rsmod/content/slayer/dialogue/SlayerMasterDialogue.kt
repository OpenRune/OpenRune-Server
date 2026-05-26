package org.rsmod.content.slayer.dialogue

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.MesAnimType
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.table.slayer.SlayerMastersRow
import org.rsmod.content.slayer.core.SlayerTaskManager
import org.rsmod.game.entity.Player

object SlayerMasterDialogue {

    data class RemoteMaster(val displayName: String, val npcType: String)

    fun remoteMaster(masterId: Int, player: Player? = null): RemoteMaster? {
        val master = player
                ?.let { SlayerTaskManager.getCurrentAssignedMaster(it) }
                ?.takeIf { it.masterId == masterId }
                ?: SlayerMastersRow.all().find { it.masterId == masterId }
                ?: return null
        val npcId = master.npcIds.firstOrNull()?.id ?: return null
        val npcType = RSCM.getReverseMapping(RSCMType.NPC, npcId) ?: return null
        val name = SlayerMasterProfiles.forNpc(npcId)?.displayName ?: displayName(masterId)
        return RemoteMaster(name, npcType)
    }

    suspend fun Dialogue.chatMaster(remote: RemoteMaster, mesanim: MesAnimType, text: String) {
        chatNpcSpecific(remote.displayName, remote.npcType, mesanim, text)
    }

    private fun displayName(masterId: Int): String =
        when (masterId) {
            SlayerMasters.TASK_KONAR -> "Konar quo Maten"
            SlayerMasters.TASK_WILDERNESS -> "Krystilia"
            SlayerMasters.TASK_TURAEL -> "Turael"
            SlayerMasters.TASK_MAZCHNA -> "Mazchna"
            SlayerMasters.TASK_VANNAKA -> "Vannaka"
            SlayerMasters.TASK_DURADEL -> "Duradel"
            SlayerMasters.TASK_NIEVE -> "Nieve"
            else -> "Slayer Master"
        }
}
