package org.rsmod.content.slayer.dialogue

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.content.slayer.core.SlayerTaskManager
import org.rsmod.content.slayer.dialogue.GemContactDialogue.contact
import org.rsmod.content.slayer.dialogue.SlayerMasterDialogue.chatMaster
import org.rsmod.content.slayer.dialogue.StandardSlayerDialogue.openContact
import org.rsmod.content.slayer.dialogue.masters.KonarDialogue.gemContact
import org.rsmod.content.slayer.dialogue.masters.KonarDialogue.npcContactMenu
import org.rsmod.content.slayer.dialogue.masters.KrystiliaDialogue.npcContactMenu as krystiliaNpcContactMenu
import org.rsmod.content.slayer.dialogue.KonarSlayerDialogueHelpers
import org.rsmod.game.entity.Player
import org.rsmod.map.zone.ZoneKey

object SlayerContact {

    private const val NEAR_MASTER_DISTANCE = 16

    suspend fun ProtectedAccess.contactSlayerMaster(npcRepo: NpcRepository) {
        val masterId = player.vars["varbit.slayer_master"]
        if (masterId == 0) {
            mes("Your enchanted gem doesn't respond. You don't have a Slayer task assigned.")
            return
        }

        val remote = SlayerMasterDialogue.remoteMaster(masterId, player) ?: run {
            mes("Your enchanted gem doesn't respond.")
            return
        }

        val nearMessage = nearMasterMessage(masterId, player, npcRepo)
        if (nearMessage != null) {
            startDialogue { chatMaster(remote, neutral, nearMessage) }
            return
        }

        startDialogue {
            when (masterId) {
                SlayerMasters.TASK_KONAR -> gemContact(remote)
                else -> contact(remote, masterId)
            }
        }
    }

    fun ProtectedAccess.checkSlayerTask() {
        val task = SlayerTaskManager.getCurrentSlayerTask(this)
        if (task == null) {
            mes("You don't have a Slayer task to check.")
            return
        }

        val count = vars["varp.slayer_count"]
        val master = SlayerTaskManager.getCurrentAssignedMaster(player)
        if (master?.masterId == SlayerMasters.TASK_KONAR) {
            val monster = KonarSlayerDialogueHelpers.monsterName(task)
            val area = KonarSlayerDialogueHelpers.currentArea(player)
                ?.let { KonarSlayerDialogueHelpers.areaShortName(it) }
            if (area != null) {
                mes("You're assigned to bring balance to $monster in $area; you have $count to go.")
            } else {
                mes("You're assigned to bring balance to $monster; you have $count to go.")
            }
        } else {
            mes("You're assigned to kill ${task.nameUppercase}; you have $count to go.")
        }

        mes("Your reward point tally is ${player.vars["varbit.slayer_points"]}.")
    }

    suspend fun ProtectedAccess.npcContactSpell(npcRepo: NpcRepository) {
        val masterId = player.vars["varbit.slayer_master_in_focus"]
        if (masterId == 0) {
            mes("You don't have a Slayer master to contact.")
            return
        }
        val master = SlayerTaskManager.tasks.keys.find { it.masterId == masterId } ?: return
        val npcId = master.npcIds.firstOrNull()?.id ?: return

        val nearMessage = nearMasterMessage(masterId, player, npcRepo)
        if (nearMessage != null) {
            startDialogue { chatNpc(neutral, nearMessage) }
            return
        }

        startDialogue {
            when (masterId) {
                SlayerMasters.TASK_KONAR -> npcContactMenu()
                SlayerMasters.TASK_WILDERNESS -> krystiliaNpcContactMenu()
                else -> openContact(npcId)
            }
        }
    }

    private fun nearMasterMessage(masterId: Int, player: Player, npcRepo: NpcRepository): String? {
        val nearNpcIds = when (masterId) {
            SlayerMasters.TASK_DURADEL -> listOf(SlayerMasters.Npc.duradel, SlayerMasters.Npc.kuradal)
            SlayerMasters.TASK_KONAR -> listOf(SlayerMasters.Npc.konar)
            else -> return null
        }

        if (!isNearAnyMaster(player, npcRepo, nearNpcIds)) {
            return null
        }
        return when (masterId) {SlayerMasters.TASK_DURADEL ->
                SlayerMasterProfiles.forNpc(SlayerMasters.Npc.duradel)?.nearContactMessage
            SlayerMasters.TASK_KONAR ->
                SlayerMasterProfiles.forNpc(SlayerMasters.Npc.konar)?.nearContactMessage
            else -> null
        }
    }

    private fun isNearAnyMaster(player: Player, npcRepo: NpcRepository, npcIds: List<Int>): Boolean {
        val zone = ZoneKey.from(player.coords)
        return npcRepo.findAll(zone, zoneRadius = 2).any { npc ->
            npc.id in npcIds &&
                npc.coords.chebyshevDistance(player.coords) <= NEAR_MASTER_DISTANCE
        }
    }
}
