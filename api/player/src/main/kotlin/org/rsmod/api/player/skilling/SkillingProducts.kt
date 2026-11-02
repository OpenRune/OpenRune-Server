package org.rsmod.api.player.skilling

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.invtx.InvCost
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invConsumeAndAdd
import org.rsmod.api.player.events.skilling.SkillingActionCompleteEvent
import org.rsmod.api.player.events.skilling.SkillingActionContext
import org.rsmod.api.player.events.skilling.SkillingProduct
import org.rsmod.api.player.events.skilling.SkillingProductPrepareEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.objtx.TransactionResultList

/**
 * - [Success]: the product was added and its inputs, if any, were consumed.
 * - [InventoryFull]: no room for the product. Nothing was consumed.
 * - [Cancelled]: a subscriber cancelled the product, or its inputs were no longer in the
 *   inventory or virtual storage. Nothing was consumed.
 */
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

    val transaction = player.addProduct(product)
    if (!transaction.success) {
        return transaction.failureResult(product)
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

    val transaction = addProduct(product)
    if (!transaction.success) {
        return transaction.failureResult(product)
    }

    if (product.grantsExperience && product.experience > 0.0) {
        statAdvance(product.skill, product.experience)
    }

    recordSkillingProduct(product.skill, product.item, product.count)
    eventBus.publish(product.toCompleteEvent())
    return SkillingAwardResult.Success
}

/** Inputs, when there are any, are consumed in the same transaction that adds the product. */
private fun Player.addProduct(product: SkillingProduct): TransactionResultList<InvObj> {
    if (product.inputs.isEmpty()) {
        return invAdd(inv, product.item, product.count)
    }
    val costs = product.inputs.map { InvCost(it.item.asRSCM(RSCMType.OBJ), it.count, it.item) }
    return invConsumeAndAdd(inv, costs, product.item.asRSCM(RSCMType.OBJ), product.count)
}

private fun TransactionResultList<InvObj>.failureResult(product: SkillingProduct) =
    if (product.inputs.isNotEmpty() && err.isMissingInput()) {
        SkillingAwardResult.Cancelled
    } else {
        SkillingAwardResult.InventoryFull
    }

private fun org.rsmod.objtx.TransactionResult.Err?.isMissingInput(): Boolean =
    this == org.rsmod.objtx.TransactionResult.ObjNotFound ||
        this == org.rsmod.objtx.TransactionResult.NotEnoughObjCount

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
