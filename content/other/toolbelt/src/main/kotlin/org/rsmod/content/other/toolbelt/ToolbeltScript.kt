package org.rsmod.content.other.toolbelt

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.interf.IfButtonOp
import jakarta.inject.Inject
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.output.objExamine
import org.rsmod.api.player.vars.resyncVar
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onIfOpen
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onPlayerInit
import org.rsmod.api.table.ToolbeltSlotsRow
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.entity.Player
import org.rsmod.game.type.getInvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ToolbeltScript @Inject constructor(
    val eventBus: EventBus,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOpen("interface.wornitems") {
            ToolbeltWornItems.sync(player)
        }

        onPlayerInit {
            if (Toolbelt.isEnabled()) {
                Toolbelt.validateStorageOnLogin(player)
            }
            ToolbeltWornItems.sync(player)
        }

        onIfOverlayButton("component.wornitems:tb_toolbelt") {
            if (Toolbelt.isEnabled()) {
                openToolbelt()
            }
        }

        if (!Toolbelt.isEnabled()) {
            return
        }

        installAddOptions()
        onIfModalButton(ToolbeltUi.CONTENT) { contentOp(it.comsub, it.op) }
        onCommand("toolbelt") {
            desc = "Tool belt: list|add|grant|remove|unlock|lock"
            invalidArgs =
                "Usage: ::toolbelt list | add | grant <obj|slot> | remove <slot> | unlock|lock <slot>"
            cheat { handleToolbeltCommand() }
        }
    }

    private fun ProtectedAccess.openToolbelt() {
        player.syncToolbeltVarsToClient()
        ifOpenMainModal(ToolbeltUi.INTERFACE)
        ifSetEvents(
            ToolbeltUi.CONTENT,
            ToolbeltUi.ITEM_COM_BASE until (ToolbeltUi.ITEM_COM_BASE + ToolbeltUi.SLOT_COUNT),
            IfEvent.Op1,
            IfEvent.Op2,
            IfEvent.Op3,
            IfEvent.Op4,
            IfEvent.Op5,
            IfEvent.Op10,
        )
        ifSetEvents(
            ToolbeltUi.CONTENT,
            ToolbeltUi.LOCK_COM_BASE until (ToolbeltUi.LOCK_COM_BASE + ToolbeltUi.SLOT_COUNT),
            IfEvent.Op1,
        )
    }

    private fun Player.syncToolbeltVarsToClient() {
        resyncVar("varp.toolbelt_unlocks")
        for (slot in 0 until ToolbeltVars.SLOT_COUNT) {
            resyncVar(ToolbeltVars.varpName(slot))
        }
    }

    private suspend fun ProtectedAccess.contentOp(comsub: Int, op: IfButtonOp) {
        val slotIndex = ToolbeltUi.slotFromComsub(comsub) ?: return
        val slot =
            ToolbeltSlotsRow.all().firstOrNull { it.slotIndex == slotIndex } ?: return
        if (!ToolbeltUnlocks.isUnlocked(player, slot)) {
            when (op) {
                IfButtonOp.Op1 -> showSlotInfo(slot)
                IfButtonOp.Op10 -> examineSlot(slot)
                else -> Unit
            }
            return
        }
        when (op) {
            IfButtonOp.Op10 -> examineSlot(slot)
            IfButtonOp.Op1 -> primarySlotOp(slot)
            IfButtonOp.Op2,
            IfButtonOp.Op3,
            IfButtonOp.Op4,
            IfButtonOp.Op5,
            -> bonecrusherSlotOp(slot, op)
            else -> Unit
        }
    }

    private suspend fun ProtectedAccess.bonecrusherSlotOp(slot: ToolbeltSlotsRow, op: IfButtonOp) {
        if (slot.slotIndex != ToolbeltUi.BONE_CRUSHER_SLOT || !Toolbelt.has(player, slot)) {
            return
        }
        when (op) {
            IfButtonOp.Op2 -> ToolbeltBonecrusher.checkCharges(this)
            IfButtonOp.Op3 -> ToolbeltBonecrusher.charge(this)
            IfButtonOp.Op4 -> ToolbeltBonecrusher.toggleActivity(this)
            IfButtonOp.Op5 -> ToolbeltBonecrusher.uncharge(this)
            else -> Unit
        }
    }

    private fun ProtectedAccess.examineSlot(slot: ToolbeltSlotsRow) {
        val type = Toolbelt.displayType(player, slot)
        player.objExamine(type, Toolbelt.OWNED_COUNT, 0)
    }

    private fun ProtectedAccess.primarySlotOp(slot: ToolbeltSlotsRow) {
        if (Toolbelt.has(player, slot)) {
            removeFromSlot(slot)
        }
    }

    private suspend fun ProtectedAccess.showSlotInfo(slot: ToolbeltSlotsRow) {
        ToolbeltSlotInfo.show(this, slot, eventBus)
    }

    private fun ProtectedAccess.removeFromSlot(slot: ToolbeltSlotsRow) {
        val removed = Toolbelt.tryRemove(player, slot)
        if (removed == null) {
            mes("You can't remove that from your tool belt.")
            return
        }
        val given =
            invAdd(
                inv,
                removed.internalName,
                1,
                strict = false,
                ignoreVirtualStorage = true,
            )
        if (given.completed() <= 0) {
            Toolbelt.tryAdd(player, removed)
            mes("You need inventory space to remove that from your tool belt.")
            return
        }
        mes("You remove the ${removed.name} from your tool belt.")
    }

    private fun ScriptContext.installAddOptions() {
        for (type in Toolbelt.allEligibleItems()) {
            val slotIndex = type.interfaceOptions.indexOfFirst { it == Toolbelt.ADD_OPTION }
            if (slotIndex < 0) {
                continue
            }
            val internal = type.internalName
            when (slotIndex) {
                0 -> onOpHeld1(internal) { addFromInventory(it.slot) }
                1 -> onOpHeld2(internal) { addFromInventory(it.slot) }
                2 -> onOpHeld3(internal) { addFromInventory(it.slot) }
                3 -> onOpHeld4(internal) { addFromInventory(it.slot) }
            }
        }
    }

    private fun ProtectedAccess.addFromInventory(invSlot: Int) {
        val obj = inv[invSlot] ?: return
        val type = getInvObj(obj)
        val slot = Toolbelt.slotFor(type)
        if (slot == null) {
            mes("You can't add that to your tool belt.")
            return
        }

        when (val result = Toolbelt.tryAdd(player, type)) {
            is Toolbelt.AddResult.Failed -> mes(result.message)
            is Toolbelt.AddResult.Added -> {
                if (
                    invDel(inv, type.internalName, 1, slot = invSlot, ignoreVirtualStorage = true)
                        .failure
                ) {
                    Toolbelt.tryRemove(player, slot)
                    mes("You don't have enough inventory space.")
                    return
                }
                mes("You add the ${type.name} to your tool belt.")
                player.syncToolbeltVarsToClient()
            }
            is Toolbelt.AddResult.Replaced -> {
                if (
                    invDel(inv, type.internalName, 1, slot = invSlot, ignoreVirtualStorage = true)
                        .failure
                ) {
                    Toolbelt.tryAdd(player, result.returned)
                    mes("You don't have enough inventory space.")
                    return
                }
                val given =
                    invAdd(
                        inv,
                        result.returned.internalName,
                        1,
                        strict = false,
                        ignoreVirtualStorage = true,
                    )
                if (given.completed() <= 0) {
                    invAdd(inv, type.internalName, 1, strict = false, ignoreVirtualStorage = true)
                    Toolbelt.tryAdd(player, result.returned)
                    mes("You don't have enough inventory space to swap tools.")
                    return
                }
                mes(
                    "You add the ${result.added.name} to your tool belt and receive your ${result.returned.name} back.",
                )
                player.syncToolbeltVarsToClient()
            }
        }
    }

    private fun Cheat.handleToolbeltCommand() {
        when (args.getOrNull(0)?.lowercase()) {
            null,
            "list" -> listToolbelt()
            "add" -> addFromFirstSlot()
            "grant" -> grantToSlot()
            "remove",
            "take", -> removeSlot()
            "unlock" -> setSlotUnlock(locked = false)
            "lock" -> setSlotUnlock(locked = true)
            else ->
                player.mes(
                    "Usage: ::toolbelt list | add | grant <obj|slot> | remove <slot> | unlock|lock <slot>",
                )
        }
    }

    private fun Cheat.setSlotUnlock(locked: Boolean) {
        val key = args.getOrNull(1)
        if (key.isNullOrBlank()) {
            player.mes("Usage: ::toolbelt ${if (locked) "lock" else "unlock"} <slot>")
            listUnlockableSlots()
            return
        }
        val slot = slotArg(1)
        if (slot == null) {
            player.mes("Unknown slot '${args.getOrNull(1)}'.")
            return
        }
        if (slot.unlockBit == null) {
            player.mes("The ${slot.displayName} slot is not lockable.")
            return
        }
        ToolbeltUnlocks.setUnlocked(player, slot, unlocked = !locked)
        player.mes(
            if (locked) {
                "Locked the ${slot.displayName.lowercase()} tool belt slot."
            } else {
                "Unlocked the ${slot.displayName.lowercase()} tool belt slot."
            },
        )
    }

    private fun Cheat.listUnlockableSlots() {
        val unlockable = ToolbeltUnlocks.slotsWithUnlock()
        if (unlockable.isEmpty()) {
            return
        }
        player.mes("Unlockable slots: " + unlockable.joinToString(", ") { "${it.slotIndex} ${it.displayName}" })
    }

    private fun Cheat.listToolbelt() {
        val occupied = Toolbelt.occupiedSlots(player)
        if (occupied.isEmpty()) {
            player.mes("Your tool belt is empty.")
            return
        }
        player.mes("Tool belt contents:")
        for ((slot, type) in occupied) {
            player.mes("- ${slot.displayName}: ${type.name}")
        }
    }

    private fun Cheat.grantToSlot() {
        val arg = args.getOrNull(1)?.trim().orEmpty()
        if (arg.isEmpty()) {
            player.mes("Usage: ::toolbelt grant <slot index|obj name>")
            return
        }
        val slotIndex = arg.toIntOrNull()
        val slot = slotArg(1)
        val type =
            when {
                slotIndex != null && slot != null -> slot.defaultItem
                else -> {
                    val name = if (arg.startsWith("obj.")) arg else "obj.$arg"
                    ServerCacheManager.getItem(name.asRSCM(RSCMType.OBJ))
                }
            }
        if (type == null) {
            player.mes("Unknown item or slot '$arg'.")
            return
        }
        val resolvedSlot = Toolbelt.slotFor(type)
        if (resolvedSlot == null) {
            player.mes("That item cannot go on the tool belt.")
            return
        }
        when (val result = Toolbelt.grantOwned(player, type, bypassRequirements = true)) {
            is Toolbelt.AddResult.Failed -> player.mes(result.message)
            is Toolbelt.AddResult.Added -> {
                player.mes("Granted ${type.name} to your tool belt.")
                player.syncToolbeltVarsToClient()
            }
            is Toolbelt.AddResult.Replaced -> {
                player.mes("Granted ${type.name} (replaced ${result.returned.name}).")
                player.syncToolbeltVarsToClient()
            }
        }
    }

    private fun Cheat.addFromFirstSlot() {
        val invSlot =
            player.inv.indexOfFirst { obj ->
                obj != null && Toolbelt.slotFor(getInvObj(obj)) != null
            }
        if (invSlot < 0) {
            player.mes("You need a toolbelt-compatible tool in your inventory.")
            return
        }
        val obj = player.inv[invSlot] ?: return
        val type = getInvObj(obj)
        val slot = Toolbelt.slotFor(type) ?: return

        when (val result = Toolbelt.tryAdd(player, type)) {
            is Toolbelt.AddResult.Failed -> player.mes(result.message)
            is Toolbelt.AddResult.Added -> {
                if (
                    player
                        .invDel(
                            player.inv,
                            type.internalName,
                            1,
                            slot = invSlot,
                            ignoreVirtualStorage = true,
                        )
                        .failure
                ) {
                    Toolbelt.tryRemove(player, slot)
                    player.mes("Failed to remove the tool from your inventory.")
                    return
                }
                player.mes("You add the ${type.name} to your tool belt.")
            }
            is Toolbelt.AddResult.Replaced -> {
                if (
                    player
                        .invDel(
                            player.inv,
                            type.internalName,
                            1,
                            slot = invSlot,
                            ignoreVirtualStorage = true,
                        )
                        .failure
                ) {
                    Toolbelt.tryAdd(player, result.returned)
                    player.mes("Failed to remove the tool from your inventory.")
                    return
                }
                val given =
                    player.invAdd(
                        player.inv,
                        result.returned.internalName,
                        1,
                        strict = false,
                        ignoreVirtualStorage = true,
                    )
                if (given.completed() <= 0) {
                    player.invAdd(
                        player.inv,
                        type.internalName,
                        1,
                        strict = false,
                        ignoreVirtualStorage = true,
                    )
                    Toolbelt.tryAdd(player, result.returned)
                    player.mes("You don't have enough inventory space to swap tools.")
                    return
                }
                player.mes(
                    "You add the ${result.added.name} to your tool belt and receive your ${result.returned.name} back.",
                )
            }
        }
    }

    private fun Cheat.removeSlot() {
        val key = args.getOrNull(1)
        if (key.isNullOrBlank()) {
            player.mes("Usage: ::toolbelt remove <slot index|obj name>")
            player.mes(
                "Slots: " +
                    ToolbeltSlotsRow.all().sortedBy { it.slotIndex }.joinToString { "${it.slotIndex}=${it.displayName}" },
            )
            return
        }
        val slot = slotArg(1)
        if (slot == null) {
            player.mes("Unknown slot '${args.getOrNull(1)}'.")
            return
        }
        val removed = Toolbelt.tryRemove(player, slot)
        if (removed == null) {
            player.mes("Your tool belt does not have a ${slot.displayName.lowercase()}.")
            return
        }
        val given =
            player.invAdd(
                player.inv,
                removed.internalName,
                1,
                strict = false,
                ignoreVirtualStorage = true,
            )
        if (given.completed() <= 0) {
            Toolbelt.tryAdd(player, removed)
            player.mes("You need inventory space to remove that from your tool belt.")
            return
        }
        player.mes("You remove the ${removed.name} from your tool belt.")
    }

    private fun Cheat.slotArg(index: Int): ToolbeltSlotsRow? {
        val arg = args.getOrNull(index)?.trim().orEmpty()
        if (arg.isEmpty()) {
            return null
        }
        arg.toIntOrNull()?.let { slotIndex ->
            return ToolbeltSlotsRow.all().firstOrNull { it.slotIndex == slotIndex }
        }
        return Toolbelt.slotFor(arg)
    }
}
