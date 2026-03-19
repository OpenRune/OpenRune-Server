package org.rsmod.api.npc.interact

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.types.ItemServerType
import dev.openrune.types.ObjectServerType
import jakarta.inject.Inject
import org.rsmod.api.npc.events.interact.AiLocTContentEvents
import org.rsmod.api.npc.events.interact.AiLocTDefaultEvents
import org.rsmod.api.npc.events.interact.AiLocTEvents
import org.rsmod.api.npc.events.interact.ApEvent
import org.rsmod.api.npc.events.interact.OpEvent
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.interact.InteractionLocT
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.movement.RouteRequestLoc

public class AiLocTInteractions @Inject constructor(private val eventBus: EventBus) {
    public fun interact(
        npc: Npc,
        loc: BoundLocInfo,
        type: ObjectServerType,
        objType: ItemServerType?,
        component: ComponentType,
        comsub: Int,
    ) {
        val opTrigger = hasOpTrigger(loc, type, objType, component, comsub)
        val apTrigger = hasApTrigger(loc, type, objType, component, comsub)
        val interaction =
            InteractionLocT(
                target = loc,
                comsub = comsub,
                objType = objType,
                component = component,
                hasOpTrigger = opTrigger,
                hasApTrigger = apTrigger,
            )
        val routeRequest =
            RouteRequestLoc(
                destination = loc.coords,
                width = type.width,
                length = type.length,
                shape = loc.entity.shape,
                angle = loc.entity.angle,
                forceApproachFlags = type.forceApproachFlags,
            )
        npc.interaction = interaction
        npc.routeRequest = routeRequest
    }

    public fun opTrigger(
        loc: BoundLocInfo,
        objType: ItemServerType?,
        component: ComponentType,
        comsub: Int,
        type: ObjectServerType = ServerCacheManager.getObject(loc.id)!!,
    ): OpEvent? {
        val typeEvent = AiLocTEvents.Op(loc, type, objType, comsub, component)
        if (eventBus.contains(typeEvent::class.java, typeEvent.id)) {
            return typeEvent
        }

        val contentEvent = AiLocTContentEvents.Op(loc, type, objType, comsub, component)
        if (eventBus.contains(contentEvent::class.java, contentEvent.id)) {
            return contentEvent
        }

        val defaultEvent = AiLocTDefaultEvents.Op(loc, type, objType, comsub, component)
        if (eventBus.contains(defaultEvent::class.java, defaultEvent.id)) {
            return defaultEvent
        }

        return null
    }

    public fun hasOpTrigger(
        loc: BoundLocInfo,
        type: ObjectServerType,
        objType: ItemServerType?,
        component: ComponentType,
        comsub: Int,
    ): Boolean = opTrigger(loc, objType, component, comsub, type) != null

    public fun apTrigger(
        loc: BoundLocInfo,
        objType: ItemServerType?,
        component: ComponentType,
        comsub: Int,
        type: ObjectServerType = ServerCacheManager.getObject(loc.id)!!,
    ): ApEvent? {
        val typeEvent = AiLocTEvents.Ap(loc, type, objType, comsub, component)
        if (eventBus.contains(typeEvent::class.java, typeEvent.id)) {
            return typeEvent
        }

        val contentEvent = AiLocTContentEvents.Ap(loc, type, objType, comsub, component)
        if (eventBus.contains(contentEvent::class.java, contentEvent.id)) {
            return contentEvent
        }

        val defaultEvent = AiLocTDefaultEvents.Ap(loc, type, objType, comsub, component)
        if (eventBus.contains(defaultEvent::class.java, defaultEvent.id)) {
            return defaultEvent
        }

        return null
    }

    public fun hasApTrigger(
        loc: BoundLocInfo,
        type: ObjectServerType,
        objType: ItemServerType?,
        component: ComponentType,
        comsub: Int,
    ): Boolean = apTrigger(loc, objType, component, comsub, type) != null
}
