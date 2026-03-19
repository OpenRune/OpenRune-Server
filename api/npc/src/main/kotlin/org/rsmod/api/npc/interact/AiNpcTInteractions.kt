package org.rsmod.api.npc.interact

import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.types.ItemServerType
import dev.openrune.types.NpcServerType
import jakarta.inject.Inject
import org.rsmod.api.npc.events.interact.AiNpcTContentEvents
import org.rsmod.api.npc.events.interact.AiNpcTDefaultEvents
import org.rsmod.api.npc.events.interact.AiNpcTEvents
import org.rsmod.api.npc.events.interact.ApEvent
import org.rsmod.api.npc.events.interact.OpEvent
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.interact.InteractionNpcT
import org.rsmod.game.movement.RouteRequestPathingEntity

public class AiNpcTInteractions @Inject constructor(private val eventBus: EventBus) {
    public fun interact(
        npc: Npc,
        target: Npc,
        component: ComponentType,
        comsub: Int,
        objType: ItemServerType?,
    ) {
        val opTrigger = hasOpTrigger(target, component, comsub, objType)
        val apTrigger = hasApTrigger(target, component, comsub, objType)
        val interaction =
            InteractionNpcT(
                target = target,
                comsub = comsub,
                objType = objType,
                component = component,
                hasOpTrigger = opTrigger,
                hasApTrigger = apTrigger,
            )
        val routeRequest = RouteRequestPathingEntity(target.avatar)
        npc.interaction = interaction
        npc.routeRequest = routeRequest
    }

    public fun opTrigger(
        target: Npc,
        component: ComponentType,
        comsub: Int,
        objType: ItemServerType?,
        type: NpcServerType = target.visType,
    ): OpEvent? {
        val typeEvent = AiNpcTEvents.Op(target, comsub, objType, type, component)
        if (eventBus.contains(typeEvent::class.java, typeEvent.id)) {
            return typeEvent
        }

        val contentEvent =
            AiNpcTContentEvents.Op(target, comsub, objType, component, type.contentGroup)
        if (eventBus.contains(contentEvent::class.java, contentEvent.id)) {
            return contentEvent
        }

        val defaultEvent = AiNpcTDefaultEvents.Op(target, comsub, objType, type, component)
        if (eventBus.contains(defaultEvent::class.java, defaultEvent.id)) {
            return defaultEvent
        }

        return null
    }

    public fun hasOpTrigger(
        target: Npc,
        component: ComponentType,
        comsub: Int,
        objType: ItemServerType?,
    ): Boolean = opTrigger(target, component, comsub, objType) != null

    public fun apTrigger(
        target: Npc,
        component: ComponentType,
        comsub: Int,
        objType: ItemServerType?,
        type: NpcServerType = target.visType,
    ): ApEvent? {
        val typeEvent = AiNpcTEvents.Ap(target, comsub, objType, type, component)
        if (eventBus.contains(typeEvent::class.java, typeEvent.id)) {
            return typeEvent
        }

        val contentEvent =
            AiNpcTContentEvents.Ap(target, comsub, objType, component, type.contentGroup)
        if (eventBus.contains(contentEvent::class.java, contentEvent.id)) {
            return contentEvent
        }

        val defaultEvent = AiNpcTDefaultEvents.Ap(target, comsub, objType, type, component)
        if (eventBus.contains(defaultEvent::class.java, defaultEvent.id)) {
            return defaultEvent
        }

        return null
    }

    public fun hasApTrigger(
        target: Npc,
        component: ComponentType,
        comsub: Int,
        objType: ItemServerType?,
    ): Boolean = apTrigger(target, component, comsub, objType) != null
}
