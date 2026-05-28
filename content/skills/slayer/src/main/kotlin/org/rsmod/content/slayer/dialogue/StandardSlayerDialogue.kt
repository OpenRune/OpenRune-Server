package org.rsmod.content.slayer.dialogue

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.baseSlayerLvl
import org.rsmod.content.slayer.core.SlayerTaskManager
import org.rsmod.content.slayer.dialogue.GenericDialogue.capeDialogue
import org.rsmod.content.slayer.dialogue.GenericDialogue.ineligibleForTask
import org.rsmod.content.slayer.dialogue.GenericDialogue.masterDoesNotHaveCurrentTask
import org.rsmod.content.slayer.dialogue.GenericDialogue.offerCancelTask
import org.rsmod.content.slayer.dialogue.GenericDialogue.offerTuraelReroll
import org.rsmod.content.slayer.dialogue.GenericDialogue.rewardsOrShopDialogue
import org.rsmod.content.slayer.dialogue.SlayerAssignmentDialogue.assignNewTask

typealias SlayerMenuOption = suspend Dialogue.() -> Unit

object StandardSlayerDialogue {

    suspend fun Dialogue.openMain(
        npcId: Int,
        extras: List<Pair<String, SlayerMenuOption>> = emptyList(),
    ) {
        val profile = SlayerMasterProfiles.forNpcOrDefault(npcId)
        chatNpc(neutral, "'Ello, and what are you after then?")
        mainMenu(profile, extras)
    }

    suspend fun Dialogue.requestAssignment(npcId: Int) {
        chatPlayer(neutral, "I need another assignment.")
        val profile = SlayerMasterProfiles.forNpcOrDefault(npcId)
        if (maybeHighCombatRedirect(profile)) {
            return
        }
        handleAssignment(npcId, profile)
    }

    suspend fun Dialogue.openContact(
        npcId: Int,
        extras: List<Pair<String, SlayerMenuOption>> = emptyList(),
    ) {
        chatNpc(neutral, "'Ello, can I help you?")
        val profile = SlayerMasterProfiles.forNpcOrDefault(npcId)
        contactMenu(profile, extras)
    }

    suspend fun Dialogue.contactRequestAssignment(npcId: Int) {
        chatPlayer(neutral, "I need another assignment.")
        val profile = SlayerMasterProfiles.forNpcOrDefault(npcId)
        if (maybeHighCombatRedirect(profile)) {
            return
        }
        handleAssignment(npcId, profile)
    }

    suspend fun Dialogue.contactCombatDifficulty(npcId: Int) {
        combatDifficultyDialogue(SlayerMasterProfiles.forNpcOrDefault(npcId))
    }

    private suspend fun Dialogue.mainMenu(
        profile: SlayerMasterProfile,
        extras: List<Pair<String, SlayerMenuOption>>,
    ) {
        val choices = mutableListOf<Pair<String, SlayerMenuOption>>()
        choices.add("I need another assignment." to { requestAssignment(profile.npcId) })
        choices.add(
            "Have you any rewards for me, or anything to trade?" to { rewardsOrShopDialogue() },
        )
        choices.add(
            "Let's talk about the difficulty of my assignments." to {
                combatDifficultyDialogue(profile)
            },
        )
        if (profile.supportsCape) {
            val capeLabel =
                if (access.player.baseSlayerLvl >= 99) "Can you sell me a Slayer Skillcape?"
                else "Tell me about your skillcape, please."
            choices.add(capeLabel to { capeDialogue(profile.npcId) })
        }
        choices.addAll(extras)
        choices.add("Er... Nothing..." to { chatPlayer(neutral, "Er... Nothing...") })
        runMenu(choices)
    }

    private suspend fun Dialogue.contactMenu(
        profile: SlayerMasterProfile,
        extras: List<Pair<String, SlayerMenuOption>>,
    ) {
        val choices = mutableListOf<Pair<String, SlayerMenuOption>>()
        choices.add("I need another assignment." to { contactRequestAssignment(profile.npcId) })
        choices.add(
            "Let's talk about the difficulty of my assignments." to {
                contactCombatDifficulty(profile.npcId)
            },
        )
        choices.addAll(extras)
        choices.add("Err... Nothing..." to { chatPlayer(neutral, "Err... Nothing...") })
        runMenu(choices)
    }

    private suspend fun Dialogue.runMenu(choices: List<Pair<String, SlayerMenuOption>>) {
        when (choices.size) {
            2 ->
                when (choice2(choices[0].first, 1, choices[1].first, 2)) {
                    1 -> choices[0].second.invoke(this)
                    2 -> choices[1].second.invoke(this)
                }
            3 ->
                when (choice3(choices[0].first, 1, choices[1].first, 2, choices[2].first, 3)) {
                    1 -> choices[0].second.invoke(this)
                    2 -> choices[1].second.invoke(this)
                    3 -> choices[2].second.invoke(this)
                }
            4 ->
                when (
                    choice4(
                        choices[0].first,
                        1,
                        choices[1].first,
                        2,
                        choices[2].first,
                        3,
                        choices[3].first,
                        4,
                    )
                ) {
                    1 -> choices[0].second.invoke(this)
                    2 -> choices[1].second.invoke(this)
                    3 -> choices[2].second.invoke(this)
                    4 -> choices[3].second.invoke(this)
                }
            5 ->
                when (
                    choice5(
                        choices[0].first,
                        1,
                        choices[1].first,
                        2,
                        choices[2].first,
                        3,
                        choices[3].first,
                        4,
                        choices[4].first,
                        5,
                    )
                ) {
                    1 -> choices[0].second.invoke(this)
                    2 -> choices[1].second.invoke(this)
                    3 -> choices[2].second.invoke(this)
                    4 -> choices[3].second.invoke(this)
                    5 -> choices[4].second.invoke(this)
                }
            else -> {
                val labels = choices.map { it.first }
                val index =
                    access.menu(
                        "",
                        hotkeys = true,
                        choices = labels,
                    )
                if (index in choices.indices) {
                    choices[index].second.invoke(this)
                }
            }
        }
    }

    private suspend fun Dialogue.maybeHighCombatRedirect(profile: SlayerMasterProfile): Boolean {
        val redirect = profile.highCombatRedirect ?: return false
        if (player.combatLevel < redirect.minimumCombat) {
            return false
        }
        chatNpc(
            neutral,
            "You're actually very strong, are you sure you don't want ${redirect.suggestedMaster} in ${redirect.location} to assign you a task?",
        )
        when (
            choice2(
                "No that's okay, I'll take a task from you.",
                1,
                "Oh okay then, I'll go talk to ${redirect.suggestedMaster}.",
                2,
            )
        ) {
            1 -> {
                chatPlayer(neutral, "No that's okay, I'll take a task from you.")
                handleAssignment(profile.npcId, profile)
            }
            2 ->
                chatPlayer(
                    neutral,
                    "Oh okay then, I'll go talk to ${redirect.suggestedMaster}.",
                )
        }
        return true
    }

    private suspend fun Dialogue.handleAssignment(npcId: Int, profile: SlayerMasterProfile) {
        val currentTask = SlayerTaskManager.getCurrentSlayerTask(access)
        if (currentTask == null) {
            assignNewTask(npc!!.visType.internalName)
            return
        }

        val count = access.vars["varp.slayer_count"]
        val monster = currentTask.nameUppercase
        if (ineligibleForTask()) {
            chatNpc(neutral, "You're still hunting $monster, with $count to go.")
            offerCancelTask()
            return
        }
        chatNpc(neutral, activeTaskMessage(profile.activeTaskStyle, monster, count))
        if (profile.supportsTaskSkip && masterDoesNotHaveCurrentTask(access, npcId)) {
            offerTuraelReroll(npcId)
        }
    }

    private fun activeTaskMessage(style: ActiveTaskMessageStyle, monster: String, count: Int): String =
        when (style) {
            ActiveTaskMessageStyle.WithComebackSemicolon ->
                "You're still hunting $monster; you have $count to go. Come back when you've finished your task."
            ActiveTaskMessageStyle.WithComebackComma ->
                "You're still hunting $monster, you have $count to go. Come back when you've finished your task."
            ActiveTaskMessageStyle.Short ->
                "You're still hunting $monster, you have $count to go."
            ActiveTaskMessageStyle.SemicolonOnly ->
                "You're still hunting $monster; you have $count to go."
        }

    private suspend fun Dialogue.combatDifficultyDialogue(profile: SlayerMasterProfile) {
        chatPlayer(neutral, "Let's talk about the difficulty of my assignments.")
        if (SlayerTaskManager.isCombatCheckEnabled(access)) {
            chatNpc(
                neutral,
                "The Slayer Masters will take your combat level into account when choosing tasks for you, so you shouldn't get anything too hard.",
            )
            when (
                choice2(
                    "That's fine - I don't want anything too tough.",
                    1,
                    "Stop checking my combat level - I can take anything!",
                    2,
                )
            ) {
                1 -> {
                    chatPlayer(neutral, "That's fine - I don't want anything too tough.")
                    chatNpc(neutral, "Okay, we'll keep checking your combat level.")
                }
                2 -> {
                    chatPlayer(neutral, "Stop checking my combat level - I can take anything!")
                    chatNpc(
                        neutral,
                        "Okay, from now on, all the Slayer Masters will assign you anything from their lists, regardless of your combat level.",
                    )
                    mesbox("Slayer Masters will no longer take the player's combat level into account.")
                    SlayerTaskManager.setCombatCheckEnabled(access, false)
                }
            }
        } else {
            chatNpc(
                neutral,
                "The Slayer Masters may currently assign you any task in our lists, regardless of your combat level.",
            )
            when (
                choice2(
                    "That's fine - I can handle any task.",
                    1,
                    "In future, please don't give anything too tough.",
                    2,
                )
            ) {
                1 -> {
                    chatPlayer(neutral, "That's fine - I can handle any task.")
                    chatNpc(neutral, "That's the spirit.")
                }
                2 -> {
                    chatPlayer(neutral, "In future, please don't give anything too tough.")
                    chatNpc(
                        neutral,
                        "Okay, from now on, all the Slayer Masters will take your combat level into account when choosing tasks for you, so you shouldn't get anything too hard.",
                    )
                    mesbox("Slayer Masters will now take the player's combat level into account.")
                    SlayerTaskManager.setCombatCheckEnabled(access, true)
                }
            }
        }
    }
}
