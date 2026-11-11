package org.rsmod.api.player.interact

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.player.events.interact.HeldUContentEvents
import org.rsmod.api.player.events.interact.HeldUDefaultEvents
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.player.events.interact.ObservedAction
import org.rsmod.api.player.events.interact.ObservedTarget
import org.rsmod.api.player.events.interact.PlayerInteractionEvent
import org.rsmod.api.player.events.interact.heldCount
import org.rsmod.api.player.events.interact.observeInteraction
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.UpdateInventory.resendSlot
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.events.EventBus
import org.rsmod.events.SuspendEvent
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.inv.isType

public class HeldUInteractions @Inject constructor(private val eventBus: EventBus) {
    private val logger = InlineLogger()

    private suspend fun observed(
        access: ProtectedAccess,
        used: ItemServerType,
        usedSlot: Int,
        target: ItemServerType,
        targetSlot: Int,
        block: suspend () -> Unit,
    ) {
        eventBus.observeInteraction(
            {
                PlayerInteractionEvent(
                    access.player,
                    ObservedTarget.Held(target, targetSlot),
                    ObservedAction.Use(
                        used,
                        component = null,
                        comsub = usedSlot,
                        count = access.player.inv.heldCount(used, usedSlot),
                    ),
                )
            },
            block,
        )
    }

    public suspend fun interact(
        access: ProtectedAccess,
        inventory: Inventory,
        selectedItemServerType: ItemServerType,
        selectedSlot: Int,
        targetItemServerType: ItemServerType,
        targetSlot: Int,
    ) {
        if (selectedSlot == targetSlot) {
            resendSlot(inventory, 0)
            return
        }

        val selectedObj = inventory[selectedSlot]
        if (!objectVerify(inventory, selectedObj, selectedItemServerType)) {
            return
        }

        val targetObj = inventory[targetSlot]
        if (!objectVerify(inventory, targetObj, targetItemServerType)) {
            return
        }

        access.opHeldU(selectedItemServerType, selectedSlot, targetItemServerType, targetSlot)
    }

    private suspend fun ProtectedAccess.opHeldU(
        selectedItemServerType: ItemServerType,
        selectedSlot: Int,
        targetItemServerType: ItemServerType,
        targetSlot: Int,
    ) {
        val firstCombination =
            opTrigger(selectedItemServerType, selectedSlot, targetItemServerType, targetSlot)
        if (firstCombination != null) {
            observed(
                this,
                selectedItemServerType,
                selectedSlot,
                targetItemServerType,
                targetSlot,
            ) {
                eventBus.publish(this, firstCombination)
            }
            return
        }

        val secondCombination =
            opTrigger(targetItemServerType, targetSlot, selectedItemServerType, selectedSlot)
        if (secondCombination != null) {
            observed(
                this,
                targetItemServerType,
                targetSlot,
                selectedItemServerType,
                selectedSlot,
            ) {
                eventBus.publish(this, secondCombination)
            }
            return
        }

        mes(constants.dm_default, ChatType.GameMessage)
        logger.debug {
            "opHeldU for `${selectedItemServerType.name}` on `${targetItemServerType.name}` is not " +
                "implemented: selected=$selectedItemServerType, target=$targetItemServerType"
        }
    }

    private fun opTrigger(
        first: ItemServerType,
        firstSlot: Int,
        second: ItemServerType,
        secondSlot: Int,
    ): SuspendEvent<ProtectedAccess>? {
        val typeScript = HeldUEvents.Type(first, firstSlot, second, secondSlot)
        if (eventBus.contains(typeScript::class.java, typeScript.id)) {
            return typeScript
        }

        val contentTypeScript = HeldUContentEvents.Type(first, firstSlot, second, secondSlot)
        if (eventBus.contains(contentTypeScript::class.java, contentTypeScript.id)) {
            return contentTypeScript
        }

        val contentScript = HeldUContentEvents.Content(first, firstSlot, second, secondSlot)
        if (eventBus.contains(contentScript::class.java, contentScript.id)) {
            return contentScript
        }

        val defaultTypeScript = HeldUDefaultEvents.Type(first, firstSlot, second, secondSlot)
        if (eventBus.contains(defaultTypeScript::class.java, defaultTypeScript.id)) {
            return defaultTypeScript
        }

        val defaultContentScript = HeldUDefaultEvents.Content(first, firstSlot, second, secondSlot)
        if (eventBus.contains(defaultContentScript::class.java, defaultContentScript.id)) {
            return defaultContentScript
        }

        return null
    }

    private fun objectVerify(inv: Inventory, obj: InvObj?, type: ItemServerType): Boolean {
        if (obj == null || !obj.isType(type)) {
            resendSlot(inv, 0)
            return false
        }
        return true
    }
}
