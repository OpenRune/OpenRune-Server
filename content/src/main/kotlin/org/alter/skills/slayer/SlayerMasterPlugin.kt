package org.alter.skills.slayer

import org.alter.api.ext.*
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onNpcOption
import org.alter.skills.slayer.SlayerDefinitions.SlayerMaster

/**
 * Slayer Master plugin handling all slayer masters.
 * Provides dialogue for task assignment, information, rewards, etc.
 */
class SlayerMasterPlugin : PluginEvent() {

    // All slayer master NPC keys
    private val slayerMasters = listOf(
        "npcs.slayer_master_1" to SlayerMaster.TURAEL,
        "npcs.slayer_master_1_tureal" to SlayerMaster.TURAEL,
        "npcs.slayer_master_1_aya" to SlayerMaster.TURAEL, // Aya replaces Turael after WGS
        "npcs.slayer_master_2" to SlayerMaster.MAZCHNA,
        "npcs.slayer_master_2_mazchna" to SlayerMaster.MAZCHNA,
        "npcs.slayer_master_3" to SlayerMaster.VANNAKA,
        "npcs.slayer_master_4" to SlayerMaster.CHAELDAR,
        "npcs.slayer_master_5" to SlayerMaster.DURADEL,
        "npcs.slayer_master_5_duradel" to SlayerMaster.DURADEL,
        "npcs.slayer_master_6" to SlayerMaster.KONAR,
        "npcs.slayer_master_7" to SlayerMaster.NIEVE,
        "npcs.slayer_master_nieve" to SlayerMaster.NIEVE,
        "npcs.slayer_master_steve" to SlayerMaster.STEVE,
        "npcs.slayer_master_8" to SlayerMaster.KRYSTILIA,
        "npcs.slayer_master_9" to SlayerMaster.NIEVE, // Master 9 maps to Nieve's list
        "npcs.slayer_master_9_active" to SlayerMaster.NIEVE,
    )

    override fun init() {
        // Register talk-to option for all slayer masters
        slayerMasters.forEach { (npcKey, master) ->
            try {
                onNpcOption(npcKey, "talk-to") {
                    player.queue { masterDialogue(player, master) }
                }

                onNpcOption(npcKey, "assignment") {
                    player.queue { assignmentDialogue(player, master) }
                }

                onNpcOption(npcKey, "rewards") {
                    openRewardsInterface(player)
                }

                onNpcOption(npcKey, "trade") {
                    openSlayerShop(player, master)
                }
            } catch (_: Exception) {
                // NPC key doesn't exist in RSCM, skip
            }
        }
    }

    /**
     * Main dialogue when talking to a slayer master.
     */
    private suspend fun QueueTask.masterDialogue(player: Player, master: SlayerMaster) {
        // Check combat level requirement
        if (player.combatLevel < master.combatReq) {
            chatNpc(player, "You need a combat level of at least ${master.combatReq} to receive tasks from me.")
            return
        }

        // Check slayer level requirement (for Duradel)
        if (master.slayerReq > 0 && player.getSkills().getCurrentLevel(org.alter.api.Skills.SLAYER) < master.slayerReq) {
            chatNpc(player, "You need a Slayer level of at least ${master.slayerReq} to receive tasks from me.")
            return
        }

        val greeting = getGreeting(master)
        chatNpc(player, greeting)

        val hasTask = SlayerManager.hasTask(player)

        val option = if (hasTask) {
            options(
                player,
                "I need another assignment.",
                "How am I doing so far?",
                "Do you have anything for trade?",
                "Er... Nothing...",
            )
        } else {
            options(
                player,
                "I need an assignment.",
                "Have you any rewards for me?",
                "Do you have anything for trade?",
                "Er... Nothing...",
            )
        }

        when (option) {
            1 -> assignmentDialogue(player, master)
            2 -> if (hasTask) taskProgressDialogue(player, master) else openRewardsInterface(player)
            3 -> openSlayerShop(player, master)
            4 -> chatPlayer(player, "Never mind.")
        }
    }

    /**
     * Handle assignment request.
     */
    private suspend fun QueueTask.assignmentDialogue(player: Player, master: SlayerMaster) {
        // Check requirements
        if (player.combatLevel < master.combatReq) {
            chatNpc(player, "You need a combat level of at least ${master.combatReq} to receive tasks from me.")
            return
        }

        if (master.slayerReq > 0 && player.getSkills().getCurrentLevel(org.alter.api.Skills.SLAYER) < master.slayerReq) {
            chatNpc(player, "You need a Slayer level of at least ${master.slayerReq} to receive tasks from me.")
            return
        }

        if (SlayerManager.hasTask(player)) {
            val currentTask = SlayerManager.getCurrentTask(player)
            val remaining = SlayerManager.getRemainingKills(player)

            chatNpc(player, "You're still hunting ${currentTask?.nameLowercase ?: "something"}; you have $remaining more to go.")
            chatNpc(player, "Come back when you've finished your task.")

            // Offer to cancel or reassign if talking to different master
            if (master == SlayerMaster.TURAEL && SlayerManager.getCurrentMaster(player) != master.id) {
                val cancel = options(
                    player,
                    "Can you give me an easier task?",
                    "Never mind, I'll finish my current task.",
                )
                if (cancel == 1) {
                    chatNpc(player, "I can give you an easier task, but this will reset your task streak to zero. Is that okay?")
                    val confirm = options(player, "Yes, reset my streak.", "No, I'll keep my current task.")
                    if (confirm == 1) {
                        SlayerManager.cancelTaskViaTurael(player)
                        assignNewTask(player, master)
                    }
                }
            }
            return
        }

        assignNewTask(player, master)
    }

    /**
     * Assign a new task to the player.
     */
    private suspend fun QueueTask.assignNewTask(player: Player, master: SlayerMaster) {
        val task = SlayerManager.assignTask(player, master.id)

        if (task == null) {
            chatNpc(player, "I'm sorry, I don't have any suitable tasks for you right now.")
            return
        }

        val taskMessage = buildTaskAssignmentMessage(task, master)
        chatNpc(player, taskMessage)

        // Offer tips
        val wantTip = options(
            player,
            "Got any tips?",
            "Okay, great!",
        )

        if (wantTip == 1) {
            val tip = getTaskTip(task)
            chatNpc(player, tip)
        }
    }

    /**
     * Build the task assignment message.
     */
    private fun buildTaskAssignmentMessage(task: SlayerTask, master: SlayerMaster): String {
        val areaMessage = if (task.hasAreaRestriction) {
            val areaName = task.getAreaName() ?: "a specific location"
            " in $areaName"
        } else ""

        return "Your new task is to kill ${task.remainingAmount} ${task.nameLowercase}$areaMessage."
    }

    /**
     * Get a tip for the assigned task.
     */
    private fun getTaskTip(task: SlayerTask): String {
        // Default tips based on task name
        return when {
            task.nameLowercase.contains("dragon") -> "Dragons breathe fire, so make sure you have an anti-dragon shield or dragonfire protection."
            task.nameLowercase.contains("demon") -> "Demons are weak to demonbane weapons."
            task.nameLowercase.contains("gargoyle") -> "Gargoyles must be finished off with a rock hammer when they're low on health."
            task.nameLowercase.contains("rockslug") -> "Rock slugs must be finished off with a bag of salt."
            task.nameLowercase.contains("crawling hand") -> "These are found in the Slayer Tower."
            task.nameLowercase.contains("aberrant spectre") -> "Wear a nosepeg or Slayer helmet to protect yourself from their stench."
            task.nameLowercase.contains("dust devil") -> "Wear a face mask or Slayer helmet when fighting dust devils."
            task.nameLowercase.contains("kurask") -> "Kurasks can only be damaged with leaf-bladed weapons or broad bolts."
            task.nameLowercase.contains("turoth") -> "Turoths can only be damaged with leaf-bladed weapons or broad bolts."
            task.nameLowercase.contains("basilisk") -> "Wear a mirror shield when fighting basilisks to avoid being turned to stone."
            task.nameLowercase.contains("cockatrice") -> "Wear a mirror shield when fighting cockatrices."
            task.nameLowercase.contains("cave horror") -> "Wear a witchwood icon to protect yourself from cave horrors."
            task.nameLowercase.contains("banshee") -> "Wear earmuffs or a Slayer helmet when fighting banshees."
            else -> "Good luck with your assignment!"
        }
    }

    /**
     * Show task progress dialogue.
     */
    private suspend fun QueueTask.taskProgressDialogue(player: Player, master: SlayerMaster) {
        val currentTask = SlayerManager.getCurrentTask(player)
        val remaining = SlayerManager.getRemainingKills(player)
        val streak = SlayerManager.getTaskStreak(player)
        val points = SlayerManager.getPoints(player)

        if (currentTask == null) {
            chatNpc(player, "You don't have a task at the moment. Would you like one?")
            return
        }

        chatNpc(player, "You're currently assigned to kill ${currentTask.nameLowercase}; only $remaining more to go.")
        chatNpc(player, "You've completed $streak tasks in a row and have $points Slayer reward points.")
    }

    /**
     * Get greeting message based on master.
     */
    private fun getGreeting(master: SlayerMaster): String {
        return when (master) {
            SlayerMaster.TURAEL -> "'Ello, and what are you after then?"
            SlayerMaster.SPRIA -> "Hello there! Looking for a task?"
            SlayerMaster.MAZCHNA -> "Greetings, adventurer. What brings you to Canifis?"
            SlayerMaster.VANNAKA -> "Greetings, warrior. I am Vannaka, the greatest swordsman alive!"
            SlayerMaster.CHAELDAR -> "What do you want, human?"
            SlayerMaster.KONAR -> "What do you seek, outlander?"
            SlayerMaster.NIEVE -> "Hello, adventurer. Ready for a challenge?"
            SlayerMaster.STEVE -> "Hello! I'm Steve, Nieve's... replacement."
            SlayerMaster.DURADEL -> "Greetings. I am Duradel, the master of Slayer."
            SlayerMaster.KRYSTILIA -> "What do you want? I don't have time for idle chat."
        }
    }

    /**
     * Open the rewards interface.
     */
    private fun openRewardsInterface(player: Player) {
        player.openInterface("interfaces.slayer_rewards", org.alter.api.InterfaceDestination.MAIN_SCREEN)
    }

    /**
     * Open slayer equipment shop.
     */
    private fun openSlayerShop(player: Player, master: SlayerMaster) {
        // Create slayer shop with common slayer equipment
        player.message("The slayer shop is not yet implemented.")
    }
}

