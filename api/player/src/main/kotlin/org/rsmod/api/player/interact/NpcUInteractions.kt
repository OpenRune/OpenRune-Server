package org.rsmod.api.player.interact

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.ServerCacheManager
import dev.openrune.types.ItemServerType
import dev.openrune.types.NpcServerType
import dev.openrune.types.varp.baseVar
import dev.openrune.types.varp.bits
import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.player.events.interact.ApEvent
import org.rsmod.api.player.events.interact.NpcUContentEvents
import org.rsmod.api.player.events.interact.NpcUDefaultEvents
import org.rsmod.api.player.events.interact.NpcUEvents
import org.rsmod.api.player.events.interact.OpEvent
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.UpdateInventory.resendSlot
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.inv.isType
import org.rsmod.game.vars.VarPlayerIntMap
import org.rsmod.utils.bits.getBits

public class NpcUInteractions @Inject private constructor(private val eventBus: EventBus) {
    private val logger = InlineLogger()

    public suspend fun interactOp(
        access: ProtectedAccess,
        target: Npc,
        inv: Inventory,
        invSlot: Int,
        npcType: NpcServerType,
        objType: ItemServerType,
    ) {
        val obj = inv[invSlot]
        if (objectVerify(inv, obj, objType)) {
            access.opNpcU(target, invSlot, npcType, objType)
        }
    }

    private suspend fun ProtectedAccess.opNpcU(
        target: Npc,
        invSlot: Int,
        npcType: NpcServerType,
        objType: ItemServerType,
    ) {
        val script = opTrigger(target, invSlot, target.visType, objType)
        if (script != null) {
            eventBus.publish(this, script)
            return
        }
        mes(constants.dm_default, ChatType.Engine)
        logger.debug {
            "opNpcU for `${objType.name}` on `${npcType.name}` is not implemented: " +
                "npcType=$npcType, objType=$objType"
        }
    }

    private fun ProtectedAccess.opTrigger(
        target: Npc,
        invSlot: Int,
        npcType: NpcServerType,
        objType: ItemServerType,
    ): OpEvent? {
        val multiNpcType = multiNpc(npcType, player.vars)
        if (multiNpcType != null) {
            val multiNpcTrigger = opTrigger(target, invSlot, multiNpcType, objType)
            if (multiNpcTrigger != null) {
                return multiNpcTrigger
            }
        }

        val contentGroup = npcType.contentGroup

        val typeScript = NpcUEvents.Op(target, invSlot, objType, npcType)
        if (eventBus.contains(typeScript::class.java, typeScript.id)) {
            return typeScript
        }

        val groupScript = NpcUContentEvents.Op(target, invSlot, objType, contentGroup)
        if (eventBus.contains(groupScript::class.java, groupScript.id)) {
            return groupScript
        }

        val defaultTypeScript = NpcUDefaultEvents.OpType(target, invSlot, objType, npcType)
        if (eventBus.contains(defaultTypeScript::class.java, defaultTypeScript.id)) {
            return defaultTypeScript
        }

        val defGroupScript = NpcUDefaultEvents.OpContent(target, invSlot, objType, contentGroup)
        if (eventBus.contains(defGroupScript::class.java, defGroupScript.id)) {
            return defGroupScript
        }

        return null
    }

    public suspend fun interactAp(
        access: ProtectedAccess,
        target: Npc,
        inv: Inventory,
        invSlot: Int,
        objType: ItemServerType,
    ) {
        val obj = inv[invSlot]
        if (objectVerify(inv, obj, objType)) {
            access.apNpcU(target, invSlot, objType)
        }
    }

    private suspend fun ProtectedAccess.apNpcU(target: Npc, invSlot: Int, objType: ItemServerType) {
        val script = apTrigger(target, invSlot, target.visType, objType)
        if (script != null) {
            eventBus.publish(this, script)
            return
        }
        apRange(-1)
    }

    private fun ProtectedAccess.apTrigger(
        target: Npc,
        invSlot: Int,
        npcType: NpcServerType,
        objType: ItemServerType,
    ): ApEvent? {
        val multiNpcType = multiNpc(npcType, player.vars)
        if (multiNpcType != null) {
            val multiNpcTrigger = apTrigger(target, invSlot, multiNpcType, objType)
            if (multiNpcTrigger != null) {
                return multiNpcTrigger
            }
        }

        val contentGroup = npcType.contentGroup

        val typeScript = NpcUEvents.Ap(target, invSlot, objType, npcType)
        if (eventBus.contains(typeScript::class.java, typeScript.id)) {
            return typeScript
        }

        val groupScript = NpcUContentEvents.Ap(target, invSlot, objType, contentGroup)
        if (eventBus.contains(groupScript::class.java, groupScript.id)) {
            return groupScript
        }

        val defaultTypeScript = NpcUDefaultEvents.ApType(target, invSlot, objType, npcType)
        if (eventBus.contains(defaultTypeScript::class.java, defaultTypeScript.id)) {
            return defaultTypeScript
        }

        val defGroupScript = NpcUDefaultEvents.ApContent(target, invSlot, objType, contentGroup)
        if (eventBus.contains(defGroupScript::class.java, defGroupScript.id)) {
            return defGroupScript
        }

        return null
    }

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

    private fun objectVerify(inv: Inventory, obj: InvObj?, type: ItemServerType): Boolean {
        if (obj == null || !obj.isType(type)) {
            resendSlot(inv, 0)
            return false
        }
        return true
    }
}
