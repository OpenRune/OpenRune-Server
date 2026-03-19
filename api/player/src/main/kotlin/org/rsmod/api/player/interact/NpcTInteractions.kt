package org.rsmod.api.player.interact

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.types.ItemServerType
import dev.openrune.types.NpcServerType
import dev.openrune.types.varp.baseVar
import dev.openrune.types.varp.bits
import jakarta.inject.Inject
import org.rsmod.api.player.events.interact.ApEvent
import org.rsmod.api.player.events.interact.NpcTContentEvents
import org.rsmod.api.player.events.interact.NpcTDefaultEvents
import org.rsmod.api.player.events.interact.NpcTEvents
import org.rsmod.api.player.events.interact.OpEvent
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionNpcT
import org.rsmod.game.movement.RouteRequestPathingEntity
import org.rsmod.game.vars.VarPlayerIntMap
import org.rsmod.utils.bits.getBits

public class NpcTInteractions @Inject constructor(private val eventBus: EventBus) {
    public fun interact(
        player: Player,
        npc: Npc,
        component: ComponentType,
        comsub: Int,
        objType: ItemServerType?,
    ) {
        val opTrigger = hasOpTrigger(player, npc, component, comsub, objType)
        val apTrigger = hasApTrigger(player, npc, component, comsub, objType)
        val interaction =
            InteractionNpcT(
                target = npc,
                comsub = comsub,
                objType = objType,
                component = component,
                hasOpTrigger = opTrigger,
                hasApTrigger = apTrigger,
            )
        val routeRequest = RouteRequestPathingEntity(npc.avatar)
        player.faceNpc(npc)
        player.interaction = interaction
        player.routeRequest = routeRequest
    }

    public fun opTrigger(
        player: Player,
        npc: Npc,
        component: ComponentType,
        comsub: Int,
        objType: ItemServerType?,
        type: NpcServerType = npc.visType,
    ): OpEvent? {
        val multiNpcType = multiNpc(type, player.vars)
        if (multiNpcType != null) {
            val multiNpcTrigger = opTrigger(player, npc, component, comsub, objType, multiNpcType)
            if (multiNpcTrigger != null) {
                return multiNpcTrigger
            }
        }

        val typeEvent = NpcTEvents.Op(npc, comsub, objType, type, component)
        if (eventBus.contains(typeEvent::class.java, typeEvent.id)) {
            return typeEvent
        }

        val contentEvent = NpcTContentEvents.Op(npc, comsub, objType, component, type.contentGroup)
        if (eventBus.contains(contentEvent::class.java, contentEvent.id)) {
            return contentEvent
        }

        val defaultEvent = NpcTDefaultEvents.Op(npc, comsub, objType, type, component)
        if (eventBus.contains(defaultEvent::class.java, defaultEvent.id)) {
            return defaultEvent
        }

        return null
    }

    public fun hasOpTrigger(
        player: Player,
        npc: Npc,
        component: ComponentType,
        comsub: Int,
        objType: ItemServerType?,
    ): Boolean = opTrigger(player, npc, component, comsub, objType) != null

    public fun apTrigger(
        player: Player,
        npc: Npc,
        component: ComponentType,
        comsub: Int,
        objType: ItemServerType?,
        type: NpcServerType = npc.visType,
    ): ApEvent? {
        val multiNpcType = multiNpc(type, player.vars)
        if (multiNpcType != null) {
            val multiNpcTrigger = apTrigger(player, npc, component, comsub, objType, multiNpcType)
            if (multiNpcTrigger != null) {
                return multiNpcTrigger
            }
        }

        val typeEvent = NpcTEvents.Ap(npc, comsub, objType, type, component)
        if (eventBus.contains(typeEvent::class.java, typeEvent.id)) {
            return typeEvent
        }

        val contentEvent = NpcTContentEvents.Ap(npc, comsub, objType, component, type.contentGroup)
        if (eventBus.contains(contentEvent::class.java, contentEvent.id)) {
            return contentEvent
        }

        val defaultEvent = NpcTDefaultEvents.Ap(npc, comsub, objType, type, component)
        if (eventBus.contains(defaultEvent::class.java, defaultEvent.id)) {
            return defaultEvent
        }

        return null
    }

    public fun hasApTrigger(
        player: Player,
        npc: Npc,
        component: ComponentType,
        comsub: Int,
        objType: ItemServerType?,
    ): Boolean = apTrigger(player, npc, component, comsub, objType) != null

    public fun multiNpc(type: NpcServerType, vars: VarPlayerIntMap): NpcServerType? {
        if (type.multiNpc.isEmpty() && type.multiDefault <= 0) {
            return null
        }
        val varValue = type.multiVarValue(vars) ?: 0
        val multiNpc =
            if (varValue in type.multiNpc.indices) {
                type.multiNpc[varValue].toInt() and 0xFFFF
            } else {
                type.multiDefault
            }
        return if (!ServerCacheManager.getNpcs().containsKey(multiNpc)) {
            null
        } else {
            ServerCacheManager.getNpc(multiNpc)
        }
    }

    private fun NpcServerType.multiVarValue(vars: VarPlayerIntMap): Int? {
        if (multiVarp > 0) {
            val varp = ServerCacheManager.getVarp(multiVarp) ?: return null
            return vars[varp]
        } else if (multiVarBit > 0) {
            val varBit = ServerCacheManager.getVarbit(multiVarBit) ?: return null
            val packed = vars[varBit.baseVar]
            return packed.getBits(varBit.bits)
        }
        return null
    }
}
