package org.rsmod.content.slayer.dialogue

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.content.slayer.core.SlayerTaskManager
import org.rsmod.content.slayer.dialogue.SlayerMasterDialogue.chatMaster
import org.rsmod.content.slayer.dialogue.StandardSlayerDialogue.openContact
import org.rsmod.content.slayer.dialogue.masters.KonarDialogue.gemContact
import org.rsmod.content.slayer.dialogue.masters.KrystiliaDialogue.combatDifficulty
import org.rsmod.content.slayer.dialogue.masters.KrystiliaDialogue.needAnotherAssignment
import org.rsmod.content.slayer.konar.KonarSlayerDialogueHelpers
import org.rsmod.content.slayer.rewards.SlayerRewardsManager
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

        val remote =
            SlayerMasterDialogue.remoteMaster(masterId, player)
                ?: run {
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
                else -> gemContact(remote, masterId)
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
            val area =
                KonarSlayerDialogueHelpers.currentArea(player)
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
                SlayerMasters.TASK_KONAR -> konarNpcContactMenu()
                SlayerMasters.TASK_WILDERNESS -> krystiliaNpcContact()
                else -> openContact(npcId)
            }
        }
    }

    private suspend fun Dialogue.gemContact(remote: SlayerMasterDialogue.RemoteMaster, masterId: Int) {
        when (masterId) {
            SlayerMasters.TASK_WILDERNESS ->
                chatMaster(remote, neutral, "Yeah? What do you want?")
            else ->
                chatMaster(remote, neutral, "'Ello, can I help you?")
        }
        when (
            choice5(
                "How am I doing so far?",
                1,
                "Who are you?",
                2,
                "Where are you?",
                3,
                "Got any tips for me?",
                4,
                "Nothing really.",
                5,
            )
        ) {
            1 -> gemTaskProgress(remote, masterId)
            2 -> gemWhoAreYou(remote, masterId)
            3 -> gemWhereAreYou(remote, masterId)
            4 -> gemTips(remote, masterId)
            5 -> chatPlayer(neutral, "Nothing really.")
        }
    }

    private suspend fun Dialogue.konarNpcContactMenu() {
        chatNpc(neutral, "'Ello, can I help you?")
        when (
            choice3(
                "I need another assignment.",
                1,
                "Let's talk about the difficulty of my assignments.",
                2,
                "Err... Nothing...",
                3,
            )
        ) {
            1 -> chatPlayer(neutral, "I need another assignment.")
            2 -> chatPlayer(neutral, "Let's talk about the difficulty of my assignments.")
            3 -> chatPlayer(neutral, "Err... Nothing...")
        }
    }

    private suspend fun Dialogue.krystiliaNpcContact() {
        chatNpc(neutral, "Yeah? What do you want?")
        when (
            choice3(
                "I need another assignment.",
                1,
                "Let's talk about the difficulty of my assignments.",
                2,
                "Err... Nothing...",
                3,
            )
        ) {
            1 -> needAnotherAssignment()
            2 -> combatDifficulty()
            3 -> chatPlayer(neutral, "Err... Nothing...")
        }
    }

    private suspend fun Dialogue.gemTaskProgress(
        remote: SlayerMasterDialogue.RemoteMaster,
        masterId: Int,
    ) {
        chatPlayer(neutral, "How am I doing so far?")
        val task = SlayerTaskManager.getCurrentSlayerTask(access)
        if (task == null) {
            chatMaster(remote, neutral, "You don't currently have a Slayer assignment.")
            return
        }
        val count = access.vars["varp.slayer_count"]
        val points = SlayerRewardsManager.getPoints(access.player)
        val message =
            if (masterId == SlayerMasters.TASK_KONAR) {
                val monster = KonarSlayerDialogueHelpers.monsterName(task)
                val areaName =
                    KonarSlayerDialogueHelpers.currentArea(access.player)
                        ?.let { KonarSlayerDialogueHelpers.areaShortName(it) }
                        ?: "the assigned location"
                "You're currently assigned to bring balance to $monster in $areaName; you have $count more to go. Your reward point tally is $points."
            } else {
                "You're currently assigned to kill ${task.nameUppercase}; you have $count more to go. Your reward point tally is $points."
            }
        chatMaster(remote, neutral, message)
    }

    private suspend fun Dialogue.gemWhoAreYou(
        remote: SlayerMasterDialogue.RemoteMaster,
        masterId: Int,
    ) {
        chatPlayer(quiz, "Who are you?")
        val message =
            when (masterId) {
                SlayerMasters.TASK_KONAR ->
                    "I am Konar quo Maten. Like you, I am a bringer of death. Together, we can serve the balance."
                else -> "I'm ${remote.displayName}, one of the Slayer Masters."
            }
        chatMaster(remote, neutral, message)
    }

    private suspend fun Dialogue.gemWhereAreYou(
        remote: SlayerMasterDialogue.RemoteMaster,
        masterId: Int,
    ) {
        chatPlayer(quiz, "Where are you?")
        val message = gemWhereIsMessage(masterId, access.player)
        chatMaster(remote, neutral, message)
    }

    private suspend fun Dialogue.gemTips(remote: SlayerMasterDialogue.RemoteMaster, masterId: Int) {
        chatPlayer(quiz, "Got any tips for me?")
        val task = SlayerTaskManager.getCurrentSlayerTask(access)
        if (task != null) {
            when (masterId) {
                SlayerMasters.TASK_KONAR -> {
                    val area = KonarSlayerDialogueHelpers.currentArea(access.player)
                    val monster = KonarSlayerDialogueHelpers.monsterName(task)
                    chatMaster(remote, neutral, "You must bring balance to $monster.")
                    if (area != null) {
                        chatMaster(remote, neutral, KonarSlayerDialogueHelpers.areaDescription(area))
                    }
                }
                SlayerMasters.TASK_WILDERNESS ->
                    chatMaster(remote, neutral, "You've got to do the task in the Wilderness.")
                else -> {
                    for (tip in SlayerTaskTips.tipsFor(task, access.player)) {
                        chatMaster(remote, neutral, tip)
                    }
                }
            }
        }
        chatPlayer(happy, "Great, thanks!")
    }

    private fun gemWhereIsMessage(masterId: Int, player: Player): String {
        val assigned = SlayerTaskManager.getCurrentAssignedMaster(player)
        if (assigned != null) {
            when {
                assigned.npcIds.any { it.id == SlayerMasters.Npc.chaeldar } ->
                    return "You'll find me in Zanaris."
                assigned.npcIds.any { it.id == SlayerMasters.Npc.krystilia } ->
                    return "I'm in the Edgeville jail, but my tasks are for the Wilderness."
                assigned.npcIds.any { it.id == SlayerMasters.Npc.konar } ->
                    return "You'll find me on Mount Karuulm. I'll be here when you need a new purpose."
            }
        }
        return when (masterId) {
            SlayerMasters.TASK_TURAEL -> "You'll find me in Burthorpe."
            SlayerMasters.TASK_MAZCHNA -> "You'll find me in Canifis."
            SlayerMasters.TASK_VANNAKA ->
                "You'll find me on the ground floor of the Slayer Tower in Edgeville."
            SlayerMasters.TASK_DURADEL, SlayerMasters.TASK_NIEVE -> "You'll find me in the Slayer Tower."
            SlayerMasters.TASK_KONAR ->
                "You'll find me on Mount Karuulm. I'll be here when you need a new purpose."
            SlayerMasters.TASK_WILDERNESS ->
                "I'm in the Edgeville jail, but my tasks are for the Wilderness."
            else -> "You'll find me when you need a new assignment."
        }
    }

    private fun nearMasterMessage(masterId: Int, player: Player, npcRepo: NpcRepository): String? {
        val nearNpcIds =
            when (masterId) {
                SlayerMasters.TASK_DURADEL ->
                    listOf(SlayerMasters.Npc.duradel, SlayerMasters.Npc.kuradal)
                SlayerMasters.TASK_KONAR -> listOf(SlayerMasters.Npc.konar)
                else -> return null
            }
        if (!isNearAnyMaster(player, npcRepo, nearNpcIds)) {
            return null
        }
        return when (masterId) {
            SlayerMasters.TASK_DURADEL ->
                SlayerMasterProfiles.forNpc(SlayerMasters.Npc.duradel)?.nearContactMessage
            SlayerMasters.TASK_KONAR ->
                SlayerMasterProfiles.forNpc(SlayerMasters.Npc.konar)?.nearContactMessage
            else -> null
        }
    }

    private fun isNearAnyMaster(player: Player, npcRepo: NpcRepository, npcIds: List<Int>): Boolean {
        val zone = ZoneKey.Companion.from(player.coords)
        return npcRepo.findAll(zone, zoneRadius = 2).any { npc ->
            npc.id in npcIds &&
                npc.coords.chebyshevDistance(player.coords) <= NEAR_MASTER_DISTANCE
        }
    }
}
