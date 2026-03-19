package org.rsmod.api.npc.interact

import dev.openrune.ServerCacheManager
import dev.openrune.types.ObjectServerType
import jakarta.inject.Inject
import org.rsmod.api.npc.events.interact.AiLocContentEvents
import org.rsmod.api.npc.events.interact.AiLocDefaultEvents
import org.rsmod.api.npc.events.interact.AiLocEvents
import org.rsmod.api.npc.events.interact.AiLocUnimplementedEvents
import org.rsmod.api.npc.events.interact.ApEvent
import org.rsmod.api.npc.events.interact.OpEvent
import org.rsmod.api.route.BoundValidator
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.interact.InteractionLocOp
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.hasOp

public class AiLocInteractions
@Inject
constructor(private val boundValidator: BoundValidator, private val eventBus: EventBus) {
    public fun interactOp(
        npc: Npc,
        loc: BoundLocInfo,
        op: InteractionOp,
        type: ObjectServerType = ServerCacheManager.getObject(loc.id)!!,
    ) {
        val opTrigger = hasOpTrigger(loc, op, type)
        val interaction =
            InteractionLocOp(target = loc, op = op, hasOpTrigger = opTrigger, hasApTrigger = false)
        npc.interaction = interaction
        if (!npc.isWithinOpRange(loc)) {
            npc.walk(loc.coords)
        }
    }

    public fun interactAp(npc: Npc, loc: BoundLocInfo, op: InteractionOp) {
        val apRange = npc.visType.attackRange
        val interaction =
            InteractionLocOp(
                target = loc,
                op = op,
                hasOpTrigger = false,
                hasApTrigger = true,
                startApRange = apRange,
            )
        npc.interaction = interaction
        if (!npc.isWithinOpRange(loc)) {
            npc.walk(loc.coords)
        }
    }

    private fun Npc.isWithinOpRange(loc: BoundLocInfo): Boolean =
        boundValidator.collides(avatar, loc) || boundValidator.touches(avatar, loc)

    public fun opTrigger(
        loc: BoundLocInfo,
        op: InteractionOp,
        type: ObjectServerType = ServerCacheManager.getObject(loc.id)!!,
    ): OpEvent? {
        val typeEvent = loc.toOp(type, op)
        if (eventBus.contains(typeEvent::class.java, typeEvent.id)) {
            return typeEvent
        }

        val contentEvent = loc.toContentOp(type, type.contentGroup, op)
        if (eventBus.contains(contentEvent::class.java, contentEvent.id)) {
            return contentEvent
        }

        val unimplEvent = loc.toUnimplementedOp(type, op)
        if (eventBus.contains(unimplEvent::class.java, unimplEvent.id)) {
            return unimplEvent
        }

        val defaultEvent = loc.toDefaultOp(type, op)
        if (eventBus.contains(defaultEvent::class.java, defaultEvent.id)) {
            return defaultEvent
        }
        return null
    }

    public fun hasOpTrigger(
        loc: BoundLocInfo,
        op: InteractionOp,
        type: ObjectServerType = ServerCacheManager.getObject(loc.id)!!,
    ): Boolean = opTrigger(loc, op, type) != null

    public fun apTrigger(
        loc: BoundLocInfo,
        op: InteractionOp,
        type: ObjectServerType = ServerCacheManager.getObject(loc.id)!!,
    ): ApEvent? {
        val typeEvent = loc.toAp(type, op)
        if (eventBus.contains(typeEvent::class.java, typeEvent.id)) {
            return typeEvent
        }

        val contentEvent = loc.toContentAp(type, type.contentGroup, op)
        if (eventBus.contains(contentEvent::class.java, contentEvent.id)) {
            return contentEvent
        }

        val defaultEvent = loc.toDefaultAp(type, op)
        if (eventBus.contains(defaultEvent::class.java, defaultEvent.id)) {
            return defaultEvent
        }

        return null
    }

    private fun BoundLocInfo.toOp(type: ObjectServerType, op: InteractionOp): AiLocEvents.Op =
        when (op) {
            InteractionOp.Op1 -> AiLocEvents.Op1(this, type)
            InteractionOp.Op2 -> AiLocEvents.Op2(this, type)
            InteractionOp.Op3 -> AiLocEvents.Op3(this, type)
            InteractionOp.Op4 -> AiLocEvents.Op4(this, type)
            InteractionOp.Op5 -> AiLocEvents.Op5(this, type)
        }

    private fun BoundLocInfo.toContentOp(
        type: ObjectServerType,
        contentGroup: Int,
        op: InteractionOp,
    ): AiLocContentEvents.Op =
        when (op) {
            InteractionOp.Op1 -> AiLocContentEvents.Op1(this, type, contentGroup)
            InteractionOp.Op2 -> AiLocContentEvents.Op2(this, type, contentGroup)
            InteractionOp.Op3 -> AiLocContentEvents.Op3(this, type, contentGroup)
            InteractionOp.Op4 -> AiLocContentEvents.Op4(this, type, contentGroup)
            InteractionOp.Op5 -> AiLocContentEvents.Op5(this, type, contentGroup)
        }

    private fun BoundLocInfo.toUnimplementedOp(
        type: ObjectServerType,
        op: InteractionOp,
    ): AiLocUnimplementedEvents.Op =
        when (op) {
            InteractionOp.Op1 -> AiLocUnimplementedEvents.Op1(this, type)
            InteractionOp.Op2 -> AiLocUnimplementedEvents.Op2(this, type)
            InteractionOp.Op3 -> AiLocUnimplementedEvents.Op3(this, type)
            InteractionOp.Op4 -> AiLocUnimplementedEvents.Op4(this, type)
            InteractionOp.Op5 -> AiLocUnimplementedEvents.Op5(this, type)
        }

    private fun BoundLocInfo.toDefaultOp(
        type: ObjectServerType,
        op: InteractionOp,
    ): AiLocDefaultEvents.Op =
        when (op) {
            InteractionOp.Op1 -> AiLocDefaultEvents.Op1(this, type)
            InteractionOp.Op2 -> AiLocDefaultEvents.Op2(this, type)
            InteractionOp.Op3 -> AiLocDefaultEvents.Op3(this, type)
            InteractionOp.Op4 -> AiLocDefaultEvents.Op4(this, type)
            InteractionOp.Op5 -> AiLocDefaultEvents.Op5(this, type)
        }

    private fun BoundLocInfo.toAp(type: ObjectServerType, op: InteractionOp): AiLocEvents.Ap =
        when (op) {
            InteractionOp.Op1 -> AiLocEvents.Ap1(this, type)
            InteractionOp.Op2 -> AiLocEvents.Ap2(this, type)
            InteractionOp.Op3 -> AiLocEvents.Ap3(this, type)
            InteractionOp.Op4 -> AiLocEvents.Ap4(this, type)
            InteractionOp.Op5 -> AiLocEvents.Ap5(this, type)
        }

    private fun BoundLocInfo.toContentAp(
        type: ObjectServerType,
        contentGroup: Int,
        op: InteractionOp,
    ): AiLocContentEvents.Ap =
        when (op) {
            InteractionOp.Op1 -> AiLocContentEvents.Ap1(this, type, contentGroup)
            InteractionOp.Op2 -> AiLocContentEvents.Ap2(this, type, contentGroup)
            InteractionOp.Op3 -> AiLocContentEvents.Ap3(this, type, contentGroup)
            InteractionOp.Op4 -> AiLocContentEvents.Ap4(this, type, contentGroup)
            InteractionOp.Op5 -> AiLocContentEvents.Ap5(this, type, contentGroup)
        }

    private fun BoundLocInfo.toDefaultAp(
        type: ObjectServerType,
        op: InteractionOp,
    ): AiLocDefaultEvents.Ap =
        when (op) {
            InteractionOp.Op1 -> AiLocDefaultEvents.Ap1(this, type)
            InteractionOp.Op2 -> AiLocDefaultEvents.Ap2(this, type)
            InteractionOp.Op3 -> AiLocDefaultEvents.Ap3(this, type)
            InteractionOp.Op4 -> AiLocDefaultEvents.Ap4(this, type)
            InteractionOp.Op5 -> AiLocDefaultEvents.Ap5(this, type)
        }

    public fun hasOp(type: ObjectServerType, op: InteractionOp): Boolean = type.hasOp(op)
}
