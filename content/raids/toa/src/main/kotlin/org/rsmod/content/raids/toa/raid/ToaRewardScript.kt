package org.rsmod.content.raids.toa.raid

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.interf.IfButtonOp
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stopInvTransmit
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.raids.toa.raid.ToaRaidManager.currentRaid
import org.rsmod.content.raids.toa.raid.encounter.ToaRewardEncounter
import org.rsmod.content.raids.toa.raid.encounter.ToaRewardEncounter.Companion.rewardChest
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.map.Direction
import org.rsmod.game.movement.MoveSpeed
import org.rsmod.objtx.isOk
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ToaRewardScript : PluginScript() {
    override fun ScriptContext.startup() {
        for (chest in CHESTS) onOpLoc1(chest) { openChest() }
        onOpLoc1(SARCOPHAGUS_PARENT) { revealSarcophagus() }
        onOpLoc1(ToaRewardEncounter.SARCOPHAGUS_OPEN) { openChest() }
        onIfClose(INTERFACE) { player.stopInvTransmit(player.rewardChest) }
        onIfModalButton(BANK) { takeAll(toBank = true) }
        onIfModalButton(INVENTORY) { takeAll(toBank = false) }
        onIfModalButton(DISCARD) { discardAll() }
        onIfModalButton(ITEMS) { takeItem(it.comsub, it.op) }
    }

    private suspend fun ProtectedAccess.revealSarcophagus() {
        val room = player.currentRaid?.encounterOf(player) as? ToaRewardEncounter ?: return
        if (!room.isSarcophagusWinner(player)) return
        soundSynth(CLICK_SOUND, delay = CLICK_SOUND_DELAY)
        playerMove(room.sarcophagusFront(), MoveSpeed.Walk)
        delay(STEP_TO_ANIM_TICKS)
        faceDirection(Direction.South)
        anim(PICKUP_SEQ)
        room.revealSarcophagus()
    }

    private fun ProtectedAccess.openChest() {
        val room = player.currentRaid?.encounterOf(player) as? ToaRewardEncounter ?: return
        if (room.isSarcophagusWinner(player) && !room.sarcophagusReady) return
        val chest = player.rewardChest
        if (chest.isEmpty()) {
            mes("The chest is empty.")
            return
        }
        room.onFirstOpen(player)
        if (!room.isSarcophagusWinner(player)) soundSynth(CHEST_SOUND, delay = CHEST_SOUND_DELAY)
        soundSynth(INTERFACE_SOUND)
        invTransmit(chest)
        ifOpenMainModal(INTERFACE)
        player.runClientScript(
            INV_INIT.asRSCM(RSCMType.CLIENTSCRIPT),
            ITEMS.asRSCM(RSCMType.COMPONENT),
            CHEST_INV.asRSCM(RSCMType.INV),
            COLUMNS,
            ROWS,
            0,
            NO_COMPONENT,
            "Take",
            "Take-5",
            "Take-All",
            "Take-X",
            "",
        )
        ifSetEvents(
            ITEMS,
            0 until chest.size,
            IfEvent.Op1,
            IfEvent.Op2,
            IfEvent.Op3,
            IfEvent.Op4,
            IfEvent.Op10,
        )
    }

    private fun ProtectedAccess.takeAll(toBank: Boolean) {
        val chest = player.rewardChest
        val target = if (toBank) bank else inv
        for (slot in chest.indices) {
            val obj = chest[slot] ?: continue
            invMoveFromSlot(
                from = chest,
                into = target,
                fromSlot = slot,
                count = obj.count,
                strict = false,
                uncert = toBank,
            )
        }
        invCompress(chest)
        finishIfEmpty(chest)
    }

    private suspend fun ProtectedAccess.takeItem(slot: Int, op: IfButtonOp) {
        val chest = player.rewardChest
        val obj = chest[slot] ?: return
        if (op == IfButtonOp.Op10) {
            objExamine(chest, slot)
            return
        }
        val requested =
            when (op) {
                IfButtonOp.Op1 -> 1
                IfButtonOp.Op2 -> TAKE_FIVE
                IfButtonOp.Op3 -> obj.count
                IfButtonOp.Op4 -> countDialog()
                else -> return
            }
        val moved =
            invMoveFromSlot(
                from = chest,
                into = inv,
                fromSlot = slot,
                count = requested.coerceIn(1, obj.count),
                strict = false,
            )
        if (!moved[0].isOk()) {
            mes("You don't have enough inventory space.")
            return
        }
        invCompress(chest)
        finishIfEmpty(chest)
    }

    private suspend fun ProtectedAccess.discardAll() {
        ifClose()
        val discard =
            choice2(
                "Yes, discard them all.",
                true,
                "No, keep them.",
                false,
                title = "Are you sure you wish to discard all the items?",
            )
        if (!discard) {
            openChest()
            return
        }
        invClear(player.rewardChest)
        markChestEmpty(player)
    }

    private fun ProtectedAccess.finishIfEmpty(chest: Inventory) {
        if (chest.isNotEmpty()) return
        ifClose()
        markChestEmpty(player)
    }

    private fun markChestEmpty(player: Player) {
        val raid = player.currentRaid ?: return
        ToaRewardEncounter.markEmpty(player, raid.players.indexOf(player))
    }

    private companion object {
        val CHESTS = (0..7).map { "loc.toa_vault_chest_loc$it" }
        const val SARCOPHAGUS_PARENT = "loc.toa_vault_sarcophagus_parent"
        const val PICKUP_SEQ = "seq.human_pickuptable"
        const val CLICK_SOUND = "synth.toa_vault_sarcophagus_click"
        const val CLICK_SOUND_DELAY = 30
        const val STEP_TO_ANIM_TICKS = 2
        const val CHEST_SOUND = "synth.toa_vault_chest_open"
        const val CHEST_SOUND_DELAY = 20
        const val INTERFACE_SOUND = "synth.found_gem"
        const val INTERFACE = "interface.toa_chests"
        const val CHEST_INV = "inv.toa_chests"
        const val ITEMS = "component.toa_chests:items"
        const val BANK = "component.toa_chests:bank"
        const val INVENTORY = "component.toa_chests:inventory"
        const val DISCARD = "component.toa_chests:discard"
        const val INV_INIT = "clientscript.[clientscript,interface_inv_init]"
        const val COLUMNS = 2
        const val ROWS = 3
        const val NO_COMPONENT = -1
        const val TAKE_FIVE = 5
    }
}
