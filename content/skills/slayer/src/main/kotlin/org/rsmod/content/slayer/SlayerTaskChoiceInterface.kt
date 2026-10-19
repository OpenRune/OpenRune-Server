package org.rsmod.content.slayer

import dev.openrune.definition.type.widget.IfEvent
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.content.slayer.core.MortimerAssignment
import org.rsmod.content.slayer.core.SlayerTaskManager

object SlayerTaskChoiceInterface {

    const val INTERFACE = "interface.slayer_task_choice"
    const val CONTENT = "component.slayer_task_choice:content"
    const val CLOSE = "component.slayer_task_choice:close_button"

    private const val ROW_CHILDREN = 10

    suspend fun openAndAwait(access: ProtectedAccess) {
        access.ifOpenMainModal(INTERFACE)
        enableRowClicks(access)

        val input = access.pauseButton()
        handleInput(access, input)
    }

    fun onClose(access: ProtectedAccess) {
        access.ifClose()
    }

    internal fun resolveSlot(comsub: Int, offerCount: Int): Int? {
        if (comsub < 0 || comsub % ROW_CHILDREN != 0) return null
        return (comsub / ROW_CHILDREN).takeIf { it < offerCount }
    }

    private fun enableRowClicks(access: ProtectedAccess) {
        val master = SlayerTaskManager.findMasterByNpc(MortimerAssignment.NPC) ?: return
        val offerCount = MortimerAssignment.readStoredOffers(access, master).size
        for (slot in 0 until offerCount) {
            val row = slot * ROW_CHILDREN
            access.player.ifSetEvents(CONTENT, row..row, IfEvent.PauseButton)
        }
    }

    private fun handleInput(access: ProtectedAccess, input: ResumePauseButtonInput) {
        when {
            input.isComponentType(CLOSE) -> access.ifClose()
            input.isComponentType(CONTENT) -> acceptSlot(access, input.subcomponent)
        }
    }

    private fun acceptSlot(access: ProtectedAccess, comsub: Int) {
        if (!MortimerAssignment.hasPendingChoice(access)) {
            access.ifClose()
            return
        }
        val master = SlayerTaskManager.findMasterByNpc(MortimerAssignment.NPC) ?: return
        val offers = MortimerAssignment.readStoredOffers(access, master)
        if (offers.isEmpty()) {
            MortimerAssignment.clearOffers(access)
            access.ifClose()
            return
        }
        val slot = resolveSlot(comsub, offers.size) ?: return
        val offer = MortimerAssignment.acceptSlot(access, slot) ?: return
        access.ifClose()
        access.mes(
            "Your new task is to kill ${offer.amount} ${offer.masterTask.task.nameUppercase}.",
        )
    }
}
