package org.rsmod.content.slayer.core

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.table.slayer.SlayerMasterTaskRow
import org.rsmod.api.table.slayer.SlayerMastersRow
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

object MortimerAssignment {

    const val MASTER_ID = 10
    const val CANCEL_COST = 100
    const val NPC = "npc.slayer_master_mortimer_vis"

    private const val VAR_CHOOSE_TASK_1 = "varbit.slayer_choose_task_1"
    private const val VAR_CHOOSE_TASK_2 = "varbit.slayer_choose_task_2"
    private const val VAR_CHOOSE_TASK_3 = "varbit.slayer_choose_task_3"
    private const val VAR_CHOOSE_MOD_ID_1 = "varbit.slayer_choose_task_1_modifier_id"
    private const val VAR_CHOOSE_MOD_ID_2 = "varbit.slayer_choose_task_2_modifier_id"
    private const val VAR_CHOOSE_MOD_ID_3 = "varbit.slayer_choose_task_3_modifier_id"
    private const val VAR_CHOOSE_MOD_VAL_1 = "varbit.slayer_choose_task_1_modifier_value"
    private const val VAR_CHOOSE_MOD_VAL_2 = "varbit.slayer_choose_task_2_modifier_value"
    private const val VAR_CHOOSE_MOD_VAL_3 = "varbit.slayer_choose_task_3_modifier_value"
    private const val VAR_CHOOSE_MOD_NEG_1 = "varbit.slayer_choose_task_1_modifier_negative"
    private const val VAR_CHOOSE_MOD_NEG_2 = "varbit.slayer_choose_task_2_modifier_negative"
    private const val VAR_CHOOSE_MOD_NEG_3 = "varbit.slayer_choose_task_3_modifier_negative"
    private const val VAR_CHOOSE_BOSS_1 = "varbit.slayer_choose_task_1_boss_id"
    private const val VAR_CHOOSE_BOSS_2 = "varbit.slayer_choose_task_2_boss_id"
    private const val VAR_CHOOSE_BOSS_3 = "varbit.slayer_choose_task_3_boss_id"
    private const val VAR_CHOOSE_AMOUNT_1 = "varbit.slayer_choose_amount_1"
    private const val VAR_CHOOSE_AMOUNT_2 = "varbit.slayer_choose_amount_2"
    private const val VAR_CHOOSE_AMOUNT_3 = "varbit.slayer_choose_amount_3"
    private const val VAR_CHOOSE_MASTER = "varbit.slayer_choose_rolled_master"
    private const val VAR_MOD_ID = "varbit.slayer_modifier_id"
    private const val VAR_MOD_VALUE = "varbit.slayer_modifier_value"
    private const val VAR_MOD_NEGATIVE = "varbit.slayer_modifier_negative"
    private const val VAR_LAST_AMOUNT = "varbit.slayer_last_mortimer_amount"
    private const val VAR_LAST_MOD_ID = "varbit.slayer_last_mortimer_modifier_id"
    private const val VAR_LAST_MOD_VALUE = "varbit.slayer_last_mortimer_modifier_value"
    private const val VAR_LAST_MOD_NEGATIVE = "varbit.slayer_last_mortimer_modifier_negative"

    data class Offer(
        val masterTask: SlayerMasterTaskRow,
        val amount: Int,
        val modifier: MortimerModifiers.RolledModifier?,
    )

    fun isMortimer(master: SlayerMastersRow?): Boolean = master?.masterId == MASTER_ID

    fun hasPendingChoice(player: Player): Boolean = player.vars[VAR_CHOOSE_TASK_1] != 0

    fun hasPendingChoice(access: ProtectedAccess): Boolean = hasPendingChoice(access.player)

    fun tasksCompleted(player: Player): Int = player.vars["varbit.slayer_mortimer_tasks_done"]

    fun streak(player: Player): Int = player.vars["varbit.slayer_mortimer_streak"]

    fun rollOffers(access: ProtectedAccess, master: SlayerMastersRow): List<Offer>? {
        val tasksDone = tasksCompleted(access.player)
        val choiceCount = MortimerModifiers.choiceCount(tasksDone)
        val rolls =
            SlayerTaskManager.rollDistinctAssignments(
                protected = access,
                master = master,
                count = choiceCount,
            )
        if (rolls.isEmpty()) return null

        return rolls.map { roll ->
            val modifier = MortimerModifiers.rollModifier(roll.masterTask, tasksDone)
            val amount = MortimerModifiers.applyQuantityModifier(roll.amount, modifier)
            Offer(masterTask = roll.masterTask, amount = amount, modifier = modifier)
        }
    }

    fun storeOffers(access: ProtectedAccess, offers: List<Offer>) {
        clearOffers(access)
        VarPlayerIntMapSetter.set(access.player, VAR_CHOOSE_MASTER, MASTER_ID)
        offers.forEachIndexed { slot, offer -> writeOfferSlot(access, slot, offer) }
    }

    fun readStoredOffers(access: ProtectedAccess, master: SlayerMastersRow): List<Offer> {
        val masterTasks = SlayerTaskManager.tasks[master].orEmpty()
        return buildList {
            for (slot in 0 until 3) {
                val taskId = access.vars[chooseTaskVar(slot)]
                if (taskId == 0) continue
                val masterTask = masterTasks.find { it.task.id == taskId } ?: continue
                val amount = access.vars[chooseAmountVar(slot)].coerceAtLeast(1)
                add(
                    Offer(
                        masterTask = masterTask,
                        amount = amount,
                        modifier = readModifier(access, slot),
                    )
                )
            }
        }
    }

    fun acceptOffer(access: ProtectedAccess, master: SlayerMastersRow, offer: Offer) {
        SlayerTaskManager.applyRegularAssignment(
            protected = access,
            master = master,
            masterTask = offer.masterTask,
            amount = offer.amount,
        )
        writeActiveModifier(access, offer.modifier)
        recordLastAssignmentExtras(access.player, offer.amount, offer.modifier)
        clearOffers(access)
    }

    fun acceptSlot(access: ProtectedAccess, slot: Int): Offer? {
        val master = SlayerTaskManager.findMasterByNpc(NPC) ?: return null
        val offers = readStoredOffers(access, master)
        val offer = offers.getOrNull(slot) ?: return null
        acceptOffer(access, master, offer)
        return offer
    }

    fun clearOffers(access: ProtectedAccess) {
        val player = access.player
        for (slot in 0 until 3) {
            VarPlayerIntMapSetter.set(player, chooseTaskVar(slot), 0)
            VarPlayerIntMapSetter.set(player, chooseModIdVar(slot), 0)
            VarPlayerIntMapSetter.set(player, chooseModValueVar(slot), 0)
            VarPlayerIntMapSetter.set(player, chooseModNegVar(slot), 0)
            VarPlayerIntMapSetter.set(player, chooseBossVar(slot), 0)
            VarPlayerIntMapSetter.set(player, chooseAmountVar(slot), 0)
        }
        VarPlayerIntMapSetter.set(player, VAR_CHOOSE_MASTER, 0)
    }

    fun activeModifier(player: Player): MortimerModifiers.RolledModifier? {
        val id = player.vars[VAR_MOD_ID]
        if (id == 0) return null
        val value = player.vars[VAR_MOD_VALUE]
        val negative = player.vars[VAR_MOD_NEGATIVE] != 0
        return MortimerModifiers.RolledModifier(id = id, value = if (negative) -value else value)
    }

    fun lastAmount(player: Player): Int = player.vars[VAR_LAST_AMOUNT].coerceAtLeast(1)

    fun lastModifier(player: Player): MortimerModifiers.RolledModifier? {
        val id = player.vars[VAR_LAST_MOD_ID]
        if (id == 0) return null
        val value = player.vars[VAR_LAST_MOD_VALUE]
        val negative = player.vars[VAR_LAST_MOD_NEGATIVE] != 0
        return MortimerModifiers.RolledModifier(id = id, value = if (negative) -value else value)
    }

    fun restorePreviousAssignment(
        access: ProtectedAccess,
        master: SlayerMastersRow,
        masterTask: SlayerMasterTaskRow,
    ) {
        val amount = lastAmount(access.player)
        SlayerTaskManager.applyRegularAssignment(
            protected = access,
            master = master,
            masterTask = masterTask,
            amount = amount,
        )
        val modifier = lastModifier(access.player)
        writeActiveModifier(access, modifier)
        recordLastAssignmentExtras(access.player, amount, modifier)
    }

    fun clueDropRateBoostPercent(player: Player, npc: Npc?): Int {
        if (!isMortimer(SlayerTaskManager.getCurrentAssignedMaster(player))) return 0
        val modifier = activeModifier(player) ?: return 0
        if (modifier.id != MortimerModifiers.MODIFIER_CLUES) return 0
        val taskId = player.vars["varp.slayer_target"]
        if (taskId == 0) return 0
        if (npc != null && !SlayerTaskManager.countsAsTaskKill(npc, taskId)) return 0
        return modifier.absoluteValue
    }

    fun onTaskComplete(player: Player): Int {
        val tasksDone = tasksCompleted(player) + 1
        VarPlayerIntMapSetter.set(player, "varbit.slayer_mortimer_tasks_done", tasksDone)
        VarPlayerIntMapSetter.set(player, "varbit.slayer_mortimer_streak", streak(player) + 1)

        val modifier = activeModifier(player)
        val points =
            if (modifier?.id == MortimerModifiers.MODIFIER_POINTS) modifier.absoluteValue else 0
        clearActiveModifierVars(player)
        return points
    }

    fun resetTaskExtras(player: Player) {
        clearActiveModifierVars(player)
    }

    private fun recordLastAssignmentExtras(
        player: Player,
        amount: Int,
        modifier: MortimerModifiers.RolledModifier?,
    ) {
        VarPlayerIntMapSetter.set(player, VAR_LAST_AMOUNT, amount.coerceIn(1, 1023))
        if (modifier == null) {
            VarPlayerIntMapSetter.set(player, VAR_LAST_MOD_ID, 0)
            VarPlayerIntMapSetter.set(player, VAR_LAST_MOD_VALUE, 0)
            VarPlayerIntMapSetter.set(player, VAR_LAST_MOD_NEGATIVE, 0)
            return
        }
        VarPlayerIntMapSetter.set(player, VAR_LAST_MOD_ID, modifier.id)
        VarPlayerIntMapSetter.set(
            player,
            VAR_LAST_MOD_VALUE,
            modifier.absoluteValue.coerceAtMost(511),
        )
        VarPlayerIntMapSetter.set(player, VAR_LAST_MOD_NEGATIVE, if (modifier.negative) 1 else 0)
    }

    private fun clearActiveModifierVars(player: Player) {
        VarPlayerIntMapSetter.set(player, VAR_MOD_ID, 0)
        VarPlayerIntMapSetter.set(player, VAR_MOD_VALUE, 0)
        VarPlayerIntMapSetter.set(player, VAR_MOD_NEGATIVE, 0)
    }

    fun writeActiveModifier(access: ProtectedAccess, modifier: MortimerModifiers.RolledModifier?) {
        if (modifier == null) {
            clearActiveModifierVars(access.player)
            return
        }
        VarPlayerIntMapSetter.set(access.player, VAR_MOD_ID, modifier.id)
        VarPlayerIntMapSetter.set(access.player, VAR_MOD_VALUE, modifier.absoluteValue)
        VarPlayerIntMapSetter.set(access.player, VAR_MOD_NEGATIVE, if (modifier.negative) 1 else 0)
    }

    private fun writeOfferSlot(access: ProtectedAccess, slot: Int, offer: Offer) {
        VarPlayerIntMapSetter.set(access.player, chooseTaskVar(slot), offer.masterTask.task.id)
        VarPlayerIntMapSetter.set(access.player, chooseAmountVar(slot), offer.amount)
        VarPlayerIntMapSetter.set(access.player, chooseBossVar(slot), 0)
        val modifier = offer.modifier
        if (modifier == null) {
            VarPlayerIntMapSetter.set(access.player, chooseModIdVar(slot), 0)
            VarPlayerIntMapSetter.set(access.player, chooseModValueVar(slot), 0)
            VarPlayerIntMapSetter.set(access.player, chooseModNegVar(slot), 0)
            return
        }
        VarPlayerIntMapSetter.set(access.player, chooseModIdVar(slot), modifier.id)
        VarPlayerIntMapSetter.set(access.player, chooseModValueVar(slot), modifier.absoluteValue)
        VarPlayerIntMapSetter.set(
            access.player,
            chooseModNegVar(slot),
            if (modifier.negative) 1 else 0,
        )
    }

    private fun readModifier(
        access: ProtectedAccess,
        slot: Int,
    ): MortimerModifiers.RolledModifier? {
        val id = access.vars[chooseModIdVar(slot)]
        if (id == 0) return null
        val value = access.vars[chooseModValueVar(slot)]
        val negative = access.vars[chooseModNegVar(slot)] != 0
        return MortimerModifiers.RolledModifier(id = id, value = if (negative) -value else value)
    }

    private fun chooseTaskVar(slot: Int): String =
        when (slot) {
            0 -> VAR_CHOOSE_TASK_1
            1 -> VAR_CHOOSE_TASK_2
            else -> VAR_CHOOSE_TASK_3
        }

    private fun chooseModIdVar(slot: Int): String =
        when (slot) {
            0 -> VAR_CHOOSE_MOD_ID_1
            1 -> VAR_CHOOSE_MOD_ID_2
            else -> VAR_CHOOSE_MOD_ID_3
        }

    private fun chooseModValueVar(slot: Int): String =
        when (slot) {
            0 -> VAR_CHOOSE_MOD_VAL_1
            1 -> VAR_CHOOSE_MOD_VAL_2
            else -> VAR_CHOOSE_MOD_VAL_3
        }

    private fun chooseModNegVar(slot: Int): String =
        when (slot) {
            0 -> VAR_CHOOSE_MOD_NEG_1
            1 -> VAR_CHOOSE_MOD_NEG_2
            else -> VAR_CHOOSE_MOD_NEG_3
        }

    private fun chooseBossVar(slot: Int): String =
        when (slot) {
            0 -> VAR_CHOOSE_BOSS_1
            1 -> VAR_CHOOSE_BOSS_2
            else -> VAR_CHOOSE_BOSS_3
        }

    private fun chooseAmountVar(slot: Int): String =
        when (slot) {
            0 -> VAR_CHOOSE_AMOUNT_1
            1 -> VAR_CHOOSE_AMOUNT_2
            else -> VAR_CHOOSE_AMOUNT_3
        }
}
