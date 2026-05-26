package org.rsmod.content.slayer.dialogue.masters

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.content.slayer.slayerKrystiliaEdgevilleSpawnActive
import org.rsmod.content.slayer.slayerKrystiliaEdgevilleSpawnUnlocked
import org.rsmod.content.slayer.slayerWildernessAssignmentBriefed
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.slayer.SlayerInterfaces
import org.rsmod.content.slayer.core.SlayerTaskManager
import org.rsmod.content.slayer.dialogue.SlayerAssignmentDialogue.assignNewTask
import org.rsmod.content.slayer.dialogue.GenericDialogue.ineligibleForTask

object KrystiliaDialogue {

    private const val EDGE_SPAWN_COST = 5_000_000

    suspend fun Dialogue.start() {
        chatNpc(neutral, "Yeah? What do you want?")
        when (
            access.menu(
                "",
                hotkeys = true,
                choices =
                    listOf(
                        "I need another assignment.",
                        "Have you any rewards for me, or anything to trade?",
                        "Let's talk about the difficulty of my assignments.",
                        "What can you do apart from Slayer Master stuff?",
                        "I love your hat! Where did you get it from?",
                        "Er... Nothing...",
                    ),
            )
        ) {
            0 -> needAnotherAssignment()
            1 -> rewardsOrShop()
            2 -> combatDifficultyDialogue()
            3 -> otherSlayerServices()
            4 -> hatDialogue()
            5 -> chatPlayer(neutral, "Er... Nothing...")
        }
    }

    suspend fun Dialogue.combatDifficulty() {
        combatDifficultyDialogue()
    }

    suspend fun Dialogue.needAnotherAssignment() {
        chatPlayer(neutral, "I need another assignment.")
        if (SlayerTaskManager.isUntrainedSlayer(player)) {
            chatNpc(
                neutral,
                "You're still only a beginner at Slayer. Go and see Turael in Burthorpe first - he'll teach you how it all works.",
            )
            return
        }

        val currentTask = SlayerTaskManager.getCurrentSlayerTask(access)
        val assignedMaster = SlayerTaskManager.getCurrentAssignedMaster(player)

        when {
            currentTask != null && assignedMaster != null && SlayerTaskManager.isWildernessMaster(assignedMaster) -> {
                val count = access.slayerCount()
                chatNpc(
                    neutral,
                    "You're still meant to be slaying ${currentTask.nameUppercase} in the Wilderness; you have $count to go.",
                )
                if (ineligibleForTask()) {
                    chatNpc(
                        neutral,
                        "You're still hunting ${currentTask.nameUppercase}, with $count to go.",
                    )
                    offerCancelTask()
                }
            }
            currentTask != null -> {
                val count = access.slayerCount()
                chatNpc(
                    neutral,
                    "You're still hunting ${currentTask.nameUppercase}; you have $count to go. Come back when you've finished your task.",
                )
            }
            !access.player.slayerWildernessAssignmentBriefed -> firstAssignmentBriefing()
            else -> assignWildernessTask()
        }
    }

    private suspend fun Dialogue.firstAssignmentBriefing() {
        chatNpc(
            neutral,
            "Before I assign you anything, I want to make something clear. My tasks have to be done in the Wilderness. Only kills inside the Wilderness will count.",
        )
        chatNpc(
            neutral,
            "I don't check combat levels when choosing tasks, either. So even if other masters won't assign you 'tough' monsters, I'll pick anything for which you have the Slayer level, provided you can physically get to it.",
        )
        chatNpc(
            neutral,
            "If you don't like my tasks, go and see Aya in Burthorpe, and ask her to assign you something else. She won't argue.",
        )
        chatNpc(
            neutral,
            "You might get a few unusual loot drops while you're on my assignments, if you're lucky. So, do you want me to assign you something?",
        )
        when (
            choice2(
                "Yes, I understand I must kill it in the Wilderness.",
                1,
                "No thanks, I don't want tasks from you.",
                2,
            )
        ) {
            1 -> {
                access.player.slayerWildernessAssignmentBriefed = true
                assignWildernessTask()
            }
            2 -> chatPlayer(neutral, "No thanks, I don't want tasks from you.")
        }
    }

    private suspend fun Dialogue.assignWildernessTask() {
        assignNewTask(SlayerTaskManager.krystiliaNpcId) { taskName, count ->
            chatNpc(neutral, "Your new task is to kill $count $taskName.")
            when (choice2("Got any tips for me?", 1, "Okay, great!", 2)) {
                1 -> krystiliaSlayerTip()
                2 -> {
                    chatPlayer(happy, "Okay, great!")
                    chatNpc(neutral, "Good luck! Don't forget to come back when you need a new assignment.")
                }
            }
        }
    }

    private suspend fun Dialogue.krystiliaSlayerTip() {
        chatPlayer(quiz, "Got any tips for me?")
        SlayerTaskManager.getCurrentSlayerTask(access)?.let { task ->
            chatNpc(neutral, "You'll need to hunt ${task.nameUppercase} in the Wilderness.")
        }
        chatNpc(neutral, "You've got to do the task in the Wilderness.")
        chatPlayer(happy, "Great, thanks!")
    }

    private suspend fun Dialogue.rewardsOrShop() {
        chatPlayer(neutral, "Have you any rewards for me, or anything to trade?")
        chatNpc(
            neutral,
            "I have quite a few rewards you can earn, and a wide variety of Slayer equipment for sale.",
        )
        when (choice3("Look at rewards.", 1, "Look at shop.", 2, "Cancel.", 3)) {
            1 -> SlayerInterfaces.openSlayerRewards(access)
            2 -> SlayerInterfaces.openSlayerEquipment(access)
        }
    }

    private suspend fun Dialogue.combatDifficultyDialogue() {
        chatPlayer(neutral, "Let's talk about the difficulty of my assignments.")
        if (SlayerTaskManager.isCombatCheckEnabled(access)) {
            chatNpc(
                neutral,
                "Other Slayer Masters take your combat level into account when choosing tasks for you, so they won't set you anything too hard. But I don't do that - if you get a task from me, it could be anything for which you",
            )
            chatNpc(neutral, "have the Slayer level, provided you can physically get to it.")
        } else {
            chatNpc(
                neutral,
                "All the Slayer Masters may currently assign you any task in our lists, regardless of your combat level.",
            )
        }
    }

    private suspend fun Dialogue.otherSlayerServices() {
        chatPlayer(neutral, "What can you do apart from Slayer Master stuff?")
        chatNpc(
            neutral,
            "Quite a lot! Those stupid guards think they've trapped me here for causing trouble, but I can get back out to the Wilderness for some fun anytime I like.",
        )
        edgevilleRespawnDialogue()
    }

    private suspend fun Dialogue.edgevilleRespawnDialogue() {
        val edgevilleActive = access.player.slayerKrystiliaEdgevilleSpawnActive
        if (edgevilleActive) {
            chatNpc(neutral, "So how's that Edgeville respawn working for you?")
            when (
                choice2(
                    "Please switch my respawn back to Lumbridge.",
                    1,
                    "It's fine, thanks.",
                    2,
                )
            ) {
                1 -> switchRespawnToLumbridge()
                2 -> chatPlayer(neutral, "It's fine, thanks.")
            }
            return
        }

        val unlocked = access.player.slayerKrystiliaEdgevilleSpawnUnlocked
        if (!unlocked) {
            offerEdgevilleSpawnPurchase()
            return
        }

        chatNpc(neutral, "So do you want to respawn in Edgeville in future?")
        when (
            choice2(
                "Please switch my respawn back to Edgeville.",
                1,
                "No, I don't want to respawn in Edgeville.",
                2,
            )
        ) {
            1 -> switchRespawnToEdgeville(paidPreviously = true)
            2 -> {
                chatPlayer(neutral, "No, I don't want to respawn in Edgeville.")
                chatNpc(neutral, "Boring!")
            }
        }
    }

    private suspend fun Dialogue.offerEdgevilleSpawnPurchase() {
        chatNpc(
            neutral,
            "Hey, perhaps you'd like me to fix it so that you respawn in Edgeville whenever you die. If you love the Wilderness as much as I do, you might enjoy that.",
        )
        when (choice2("Tell me more.", 1, "That doesn't interest me.", 2)) {
            1 -> edgevilleSpawnDetails()
            2 -> {
                chatPlayer(neutral, "That doesn't interest me.")
                chatNpc(neutral, "Maybe you just can't handle Edgeville!")
            }
        }
    }

    private suspend fun Dialogue.edgevilleSpawnDetails() {
        chatPlayer(neutral, "Tell me more.")
        chatNpc(
            neutral,
            "Alright, here's the deal: Whenever you drop dead, I'll try to make you reappear in Edgeville, down by the southern ruins. You'll lose your items as normal.",
        )
        chatNpc(
            neutral,
            "My magic won't affect you if you die in some fancy place like Castle Wars or the Emir's Arena; those places have their own magic trying to make you 'safe'.",
        )
        chatNpc(neutral, "This will apply on members' worlds and non-members' worlds alike.")
        chatNpc(
            neutral,
            "I'll want a one-off payment of 5 million coins. If you get your respawn changed later, and then need me to set it back to Edgeville, I won't bill you again.",
        )
        val coins = access.inv.count("obj.coins")
        if (coins < EDGE_SPAWN_COST) {
            chatPlayer(neutral, "I'm not carrying that much money.")
            chatNpc(neutral, "Scared of dropping it? Hah!")
            return
        }
        when (choice2("Okay, switch my spawn to Edgeville.", 1, "Er... Nothing...", 2)) {
            1 -> purchaseEdgevilleSpawn()
            2 -> chatPlayer(neutral, "Er... Nothing...")
        }
    }

    private suspend fun Dialogue.purchaseEdgevilleSpawn() {
        chatPlayer(neutral, "Okay, switch my spawn to Edgeville.")
        val coinDel =
            access.invDel(access.inv, "obj.coins", count = EDGE_SPAWN_COST, strict = true)
        if (coinDel.failure) {
            chatPlayer(neutral, "I'm not carrying that much money.")
            chatNpc(neutral, "Scared of dropping it? Hah!")
            return
        }
        access.player.slayerKrystiliaEdgevilleSpawnUnlocked = true
        access.player.slayerKrystiliaEdgevilleSpawnActive = true
        mesbox("Your respawn point has been set to Edgeville.")
    }

    private suspend fun Dialogue.switchRespawnToEdgeville(paidPreviously: Boolean) {
        chatPlayer(neutral, "Please switch my respawn back to Edgeville.")
        if (!paidPreviously) {
            chatNpc(neutral, "Sounds like you're looking for some fun! Are you sure you can handle it?")
            when (choice2("Yes, switch my respawn to Edgeville.", 1, "No, maybe another time.", 2)) {
                1 -> confirmEdgevilleRespawn()
                2 -> {
                    chatPlayer(neutral, "No, maybe another time.")
                    chatNpc(neutral, "Boring!")
                }
            }
            return
        }
        confirmEdgevilleRespawn()
    }

    private suspend fun Dialogue.confirmEdgevilleRespawn() {
        chatPlayer(neutral, "Yes, switch my respawn to Edgeville.")
        access.player.slayerKrystiliaEdgevilleSpawnActive = true
        mesbox("Your respawn point has been set to Edgeville.")
        chatNpc(neutral, "Done.")
    }

    private suspend fun Dialogue.switchRespawnToLumbridge() {
        chatPlayer(neutral, "Please switch my respawn back to Lumbridge.")
        chatNpc(
            neutral,
            "You really want to respawn in that dead-end slum? Are you sure? Come and see me if you want to respawn in Edgeville again; I won't ask for more money.",
        )
        when (choice2("Yes, switch my respawn to Lumbridge.", 1, "No, I'll keep the Edgeville respawn.", 2)) {
            1 -> {
                chatPlayer(neutral, "Yes, switch my respawn to Lumbridge.")
                access.player.slayerKrystiliaEdgevilleSpawnActive = false
                mesbox("Your respawn point has been set to Lumbridge.")
                chatNpc(neutral, "Done.")
            }
            2 -> {
                chatPlayer(neutral, "No, I'll keep the Edgeville respawn.")
                chatNpc(neutral, "Good choice.")
            }
        }
    }

    private suspend fun Dialogue.hatDialogue() {
        chatPlayer(neutral, "I love the hat! Where did you get it from?")
        chatNpc(neutral, "Thank you. I actually made it myself!")
        chatPlayer(neutral, "Oh, I didn't know you enjoyed arts and crafts?")
        chatNpc(
            neutral,
            "Oh yes, I put the craft in witchcraft! I have plenty of spare time outside of assigning tasks so I've been making comics and stuffed toys.",
        )
        chatPlayer(neutral, "That's so sweet! I didn't realise Slayer Masters had so much spare time.")
        chatNpc(
            neutral,
            "Perhaps I have a little too much spare time! I've made so many things that I'm running out of room.",
        )
        chatPlayer(neutral, "Have you thought about giving them to other people?")
        chatNpc(
            neutral,
            "I tried handing out stuffed chinchompas, but the guards mistook them for the real thing and arrested me for smuggling explosives out of my cell.",
        )
        chatPlayer(neutral, "Oh dear. Don't you have any less... dangerous crafts?")
        chatNpc(neutral, "Well, I do make hats too.")
        chatPlayer(neutral, "That's perfect! People love hats.")
    }

    private suspend fun Dialogue.offerCancelTask() {
        chatNpc(
            neutral,
            "I don't think that's a suitable task for you. Shall I cancel it? This will not wipe your task streaks.",
        )
        when (choice2("Yes, please cancel it.", 1, "No, thanks, I want to try doing it.", 2)) {
            1 -> {
                chatPlayer(neutral, "Yes, please cancel it.")
                chatNpc(
                    neutral,
                    "Alright, consider the task cancelled. You can now get a new assignment when you want one.",
                )
                SlayerTaskManager.resetTask(access)
            }
            2 -> {
                chatPlayer(neutral, "No, thanks, I want to try doing it.")
                chatNpc(neutral, "Good luck with that.")
            }
        }
    }

    private fun ProtectedAccess.slayerCount(): Int = vars["varp.slayer_count"]
}
