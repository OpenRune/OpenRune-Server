package org.rsmod.api.player.skilling

import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.events.skilling.SkillingActionCompleteEvent
import org.rsmod.api.player.events.skilling.SkillingActionContext
import org.rsmod.api.player.events.skilling.SkillingProduct
import org.rsmod.api.player.events.skilling.SkillingProductPrepareEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player

public enum class SkillingAwardResult {
    Success,
    InventoryFull,
    Cancelled,
}

public fun ProtectedAccess.awardSkillingProduct(product: SkillingProduct): SkillingAwardResult {
    if (!product.isBonus) {
        publish(SkillingProductPrepareEvent(product))
    }
    if (product.cancelled) {
        return SkillingAwardResult.Cancelled
    }

    val transaction = invAdd(inv, product.item, product.count)
    if (!transaction.success) {
        return SkillingAwardResult.InventoryFull
    }

    if (product.grantsExperience && product.experience > 0.0) {
        statAdvance(product.skill, product.experience)
    }

    player.recordSkillingProduct(product.skill, product.item, product.count)
    publish(product.toCompleteEvent())
    return SkillingAwardResult.Success
}

public fun Player.awardSkillingProduct(
    eventBus: EventBus,
    product: SkillingProduct,
): SkillingAwardResult {
    if (!product.isBonus) {
        eventBus.publish(SkillingProductPrepareEvent(product))
    }
    if (product.cancelled) {
        return SkillingAwardResult.Cancelled
    }

    val transaction = invAdd(inv, product.item, product.count)
    if (!transaction.success) {
        return SkillingAwardResult.InventoryFull
    }

    if (product.grantsExperience && product.experience > 0.0) {
        statAdvance(product.skill, product.experience)
    }

    recordSkillingProduct(product.skill, product.item, product.count)
    eventBus.publish(product.toCompleteEvent())
    return SkillingAwardResult.Success
}

private fun SkillingProduct.toCompleteEvent(): SkillingActionCompleteEvent =
    SkillingActionCompleteEvent(
        player = player,
        context =
            SkillingActionContext.Product(
                skill = skill,
                item = item,
                count = count,
                experienceGranted = if (grantsExperience) experience else 0.0,
                source = source,
                isBonus = isBonus,
            ),
    )
