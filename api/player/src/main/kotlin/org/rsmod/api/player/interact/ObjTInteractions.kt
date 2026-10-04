package org.rsmod.api.player.interact

import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.player.events.interact.ApEvent
import org.rsmod.api.player.events.interact.ObjTEvents
import org.rsmod.api.player.events.interact.OpEvent
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionObjT
import org.rsmod.game.movement.RouteRequestCoord
import org.rsmod.game.obj.Obj

public class ObjTInteractions @Inject constructor(private val eventBus: EventBus) {
    public fun interact(
        player: Player,
        obj: Obj,
        component: ComponentType,
        comsub: Int,
        objType: ItemServerType?,
    ) {
        val interaction =
            InteractionObjT(
                objType = objType,
                component = component,
                comsub = comsub,
                target = obj,
                hasOpTrigger = hasOpTrigger(obj, component, comsub, objType),
                hasApTrigger = hasApTrigger(obj, component, comsub, objType),
            )
        player.interaction = interaction
        player.routeRequest = RouteRequestCoord(obj.coords)
    }

    public fun opTrigger(
        obj: Obj,
        component: ComponentType,
        comsub: Int,
        objType: ItemServerType?,
    ): OpEvent? {
        val event = ObjTEvents.Op(obj, comsub, objType, component)
        return event.takeIf { eventBus.contains(it::class.java, it.id) }
    }

    public fun hasOpTrigger(
        obj: Obj,
        component: ComponentType,
        comsub: Int,
        objType: ItemServerType?,
    ): Boolean = opTrigger(obj, component, comsub, objType) != null

    public fun apTrigger(
        obj: Obj,
        component: ComponentType,
        comsub: Int,
        objType: ItemServerType?,
    ): ApEvent? {
        val event = ObjTEvents.Ap(obj, comsub, objType, component)
        return event.takeIf { eventBus.contains(it::class.java, it.id) }
    }

    public fun hasApTrigger(
        obj: Obj,
        component: ComponentType,
        comsub: Int,
        objType: ItemServerType?,
    ): Boolean = apTrigger(obj, component, comsub, objType) != null
}
