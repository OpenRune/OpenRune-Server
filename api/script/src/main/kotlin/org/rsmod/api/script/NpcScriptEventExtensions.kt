package org.rsmod.api.script

import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.NpcServerType
import dev.openrune.types.WalkTriggerType
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.npc.events.NpcMovementEvent
import org.rsmod.api.npc.events.NpcQueueEvents
import org.rsmod.api.npc.events.NpcTimerEvents
import org.rsmod.api.player.events.interact.NpcContentEvents
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.events.interact.NpcTContentEvents
import org.rsmod.api.player.events.interact.NpcTDefaultEvents
import org.rsmod.api.player.events.interact.NpcTEvents
import org.rsmod.api.player.events.interact.NpcUContentEvents
import org.rsmod.api.player.events.interact.NpcUDefaultEvents
import org.rsmod.api.player.events.interact.NpcUEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.events.EventBus
import org.rsmod.plugin.scripts.ScriptContext

/* Op functions */
public fun ScriptContext.onOpNpc1(
    type: NpcServerType,
    action: suspend ProtectedAccess.(NpcEvents.Op1) -> Unit,
): Unit = onProtectedEvent(type.id, action)

public fun ScriptContext.onOpNpc1(
    internal: String,
    action: suspend ProtectedAccess.(NpcEvents.Op1) -> Unit,
): Unit = onProtectedEvent(internal.asRSCM(RSCMType.NPC), action)

public fun ScriptContext.onOpNpc2(
    type: String,
    action: suspend ProtectedAccess.(NpcEvents.Op2) -> Unit,
): Unit = onProtectedEvent(type.asRSCM(RSCMType.NPC), action)

public fun ScriptContext.onOpNpc3(
    type: String,
    action: suspend ProtectedAccess.(NpcEvents.Op3) -> Unit,
): Unit = onProtectedEvent(type.asRSCM(RSCMType.NPC), action)

public fun ScriptContext.onOpNpc4(
    type: String,
    action: suspend ProtectedAccess.(NpcEvents.Op4) -> Unit,
): Unit = onProtectedEvent(type.asRSCM(RSCMType.NPC), action)

public fun ScriptContext.onOpNpc5(
    type: String,
    action: suspend ProtectedAccess.(NpcEvents.Op5) -> Unit,
): Unit = onProtectedEvent(type.asRSCM(RSCMType.NPC), action)

public fun ScriptContext.onOpContentNpc1(
    content: String,
    action: suspend ProtectedAccess.(NpcContentEvents.Op1) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onOpContentNpc2(
    content: String,
    action: suspend ProtectedAccess.(NpcContentEvents.Op2) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onOpContentNpc3(
    content: String,
    action: suspend ProtectedAccess.(NpcContentEvents.Op3) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onOpContentNpc4(
    content: String,
    action: suspend ProtectedAccess.(NpcContentEvents.Op4) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onOpContentNpc5(
    content: String,
    action: suspend ProtectedAccess.(NpcContentEvents.Op5) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onOpNpcT(
    component: String,
    action: suspend ProtectedAccess.(NpcTDefaultEvents.Op) -> Unit,
): Unit = onProtectedEvent(component.asRSCM(RSCMType.COMPONENT), action)

public fun ScriptContext.onOpNpcT(
    type: NpcServerType,
    component: String,
    action: suspend ProtectedAccess.(NpcTEvents.Op) -> Unit,
): Unit = onProtectedEvent(EventBus.composeLongKey(type.id, component.asRSCM(RSCMType.COMPONENT)), action)

public fun ScriptContext.onOpNpcT(
    content: String,
    component: String,
    action: suspend ProtectedAccess.(NpcTContentEvents.Op) -> Unit,
): Unit = onProtectedEvent(EventBus.composeLongKey(content.asRSCM(RSCMType.CONTENT), component.asRSCM(RSCMType.COMPONENT)), action)

public fun ScriptContext.onOpNpcU(
    npcType: NpcServerType,
    action: suspend ProtectedAccess.(NpcUDefaultEvents.OpType) -> Unit,
): Unit = onProtectedEvent(npcType.id, action)

public fun ScriptContext.onOpNpcU(
    npcType: String,
    action: suspend ProtectedAccess.(NpcUDefaultEvents.OpType) -> Unit,
): Unit = onProtectedEvent(npcType.asRSCM(RSCMType.NPC), action)


public fun ScriptContext.onOpContentNpcU(
    content: String,
    action: suspend ProtectedAccess.(NpcUDefaultEvents.OpContent) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onOpNpcU(
    npcType: NpcServerType,
    objType: ItemServerType,
    action: suspend ProtectedAccess.(NpcUEvents.Op) -> Unit,
): Unit = onProtectedEvent(EventBus.composeLongKey(npcType.id, objType.id), action)

public fun ScriptContext.onOpContentNpcU(
    content: String,
    objType: String,
    action: suspend ProtectedAccess.(NpcUContentEvents.Op) -> Unit,
): Unit = onProtectedEvent(EventBus.composeLongKey(content.asRSCM(RSCMType.CONTENT), objType.asRSCM(RSCMType.OBJ)), action)

/* Ap functions */
public fun ScriptContext.onApNpc1(
    type: String,
    action: suspend ProtectedAccess.(NpcEvents.Ap1) -> Unit,
): Unit = onProtectedEvent(type.asRSCM(RSCMType.NPC), action)

public fun ScriptContext.onApNpc2(
    type: String,
    action: suspend ProtectedAccess.(NpcEvents.Ap2) -> Unit,
): Unit = onProtectedEvent(type.asRSCM(RSCMType.NPC), action)

public fun ScriptContext.onApNpc3(
    type: String,
    action: suspend ProtectedAccess.(NpcEvents.Ap3) -> Unit,
): Unit = onProtectedEvent(type.asRSCM(RSCMType.NPC), action)

public fun ScriptContext.onApNpc4(
    type: String,
    action: suspend ProtectedAccess.(NpcEvents.Ap4) -> Unit,
): Unit = onProtectedEvent(type.asRSCM(RSCMType.NPC), action)

public fun ScriptContext.onApNpc5(
    type: NpcServerType,
    action: suspend ProtectedAccess.(NpcEvents.Ap5) -> Unit,
): Unit = onProtectedEvent(type.id, action)

public fun ScriptContext.onApContentNpc1(
    content: String,
    action: suspend ProtectedAccess.(NpcContentEvents.Ap1) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onApContentNpc2(
    content: String,
    action: suspend ProtectedAccess.(NpcContentEvents.Ap2) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onApContentNpc3(
    content: String,
    action: suspend ProtectedAccess.(NpcContentEvents.Ap3) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onApContentNpc4(
    content: String,
    action: suspend ProtectedAccess.(NpcContentEvents.Ap4) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onApContentNpc5(
    content: String,
    action: suspend ProtectedAccess.(NpcContentEvents.Ap5) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onApNpcT(
    component: ComponentType,
    action: suspend ProtectedAccess.(NpcTDefaultEvents.Ap) -> Unit,
): Unit = onProtectedEvent(component.packed, action)

public fun ScriptContext.onApNpcT(
    type: NpcServerType,
    component: ComponentType,
    action: suspend ProtectedAccess.(NpcTEvents.Ap) -> Unit,
): Unit = onProtectedEvent(EventBus.composeLongKey(type.id, component.packed), action)

public fun ScriptContext.onApNpcT(
    content: String,
    component: ComponentType,
    action: suspend ProtectedAccess.(NpcTContentEvents.Ap) -> Unit,
): Unit = onProtectedEvent(EventBus.composeLongKey(content.asRSCM(RSCMType.CONTENT), component.packed), action)

public fun ScriptContext.onApNpcU(
    npcType: NpcServerType,
    action: suspend ProtectedAccess.(NpcUDefaultEvents.ApType) -> Unit,
): Unit = onProtectedEvent(npcType.id, action)

public fun ScriptContext.onApNpcU(
    content: String,
    action: suspend ProtectedAccess.(NpcUDefaultEvents.ApContent) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onApNpcU(
    npcType: NpcServerType,
    objType: ItemServerType,
    action: suspend ProtectedAccess.(NpcUEvents.Ap) -> Unit,
): Unit = onProtectedEvent(EventBus.composeLongKey(npcType.id, objType.id), action)

public fun ScriptContext.onApNpcU(
    content: String,
    objType: ItemServerType,
    action: suspend ProtectedAccess.(NpcUContentEvents.Ap) -> Unit,
): Unit = onProtectedEvent(EventBus.composeLongKey(content.asRSCM(RSCMType.CONTENT), objType.id), action)

/* Timer functions */
public fun ScriptContext.onNpcTimer(
    timer: String,
    action: suspend StandardNpcAccess.(NpcTimerEvents.Default) -> Unit,
): Unit = onNpcAccessEvent(timer.asRSCM(RSCMType.TIMER), action)

public fun ScriptContext.onNpcTimer(
    npc: NpcServerType,
    timer: String,
    action: suspend StandardNpcAccess.(NpcTimerEvents.Type) -> Unit,
): Unit = onNpcAccessEvent(EventBus.composeLongKey(npc.id, timer.asRSCM(RSCMType.TIMER)), action)

public fun ScriptContext.onNpcTimer(
    content: String,
    timer: String,
    action: suspend StandardNpcAccess.(NpcTimerEvents.Content) -> Unit,
): Unit = onNpcAccessEvent(EventBus.composeLongKey(content.asRSCM(RSCMType.CONTENT), timer.asRSCM(RSCMType.TIMER)), action)

/* Queue functions */
public fun ScriptContext.onNpcQueue(
    type: String,
    action: suspend StandardNpcAccess.(NpcQueueEvents.Default<Nothing>) -> Unit,
): Unit = onNpcAccessEvent(type.asRSCM(RSCMType.QUEUE), action)

public fun <T> ScriptContext.onNpcQueueWithArgs(
    type: String,
    action: suspend StandardNpcAccess.(NpcQueueEvents.Default<T>) -> Unit,
): Unit = onNpcAccessEvent(type.asRSCM(RSCMType.QUEUE), action)

public fun ScriptContext.onNpcQueue(
    type: NpcServerType,
    queue: String,
    action: suspend StandardNpcAccess.(NpcQueueEvents.Type<Nothing>) -> Unit,
): Unit = onNpcAccessEvent(EventBus.composeLongKey(type.id, queue.asRSCM(RSCMType.QUEUE)), action)

public fun <T> ScriptContext.onNpcQueueWithArgs(
    type: NpcServerType,
    queue: String,
    action: suspend StandardNpcAccess.(NpcQueueEvents.Type<T>) -> Unit,
): Unit = onNpcAccessEvent(EventBus.composeLongKey(type.id, queue.asRSCM(RSCMType.QUEUE)), action)

public fun ScriptContext.onNpcQueue(
    content: String,
    queue: String,
    action: suspend StandardNpcAccess.(NpcQueueEvents.Content<Nothing>) -> Unit,
): Unit = onNpcAccessEvent(EventBus.composeLongKey(content.asRSCM(RSCMType.CONTENT), queue.asRSCM(RSCMType.QUEUE)), action)

public fun <T> ScriptContext.onNpcQueueWithArgs(
    content: String,
    queue: String,
    action: suspend StandardNpcAccess.(NpcQueueEvents.Content<T>) -> Unit,
): Unit = onNpcAccessEvent(EventBus.composeLongKey(content.asRSCM(RSCMType.CONTENT), queue.asRSCM(RSCMType.QUEUE)), action)

/* Walk trigger functions */
public fun ScriptContext.onNpcWalkTrigger(
    trigger: WalkTriggerType,
    action: NpcMovementEvent.WalkTrigger.() -> Unit,
): Unit = onEvent(trigger.id, action)

/* Hit functions */
/**
 * Registers a script to modify any **incoming** hit **before** it is applied to the associated npc.
 */
public fun ScriptContext.onModifyNpcHit(
    type: NpcServerType,
    action: NpcHitEvents.Modify.() -> Unit,
): Unit = onEvent(type.id, action)

/**
 * Registers a script that triggers when the associated npc receives a hit (when the hitsplat is
 * displayed).
 */
public fun ScriptContext.onNpcHit(
    type: NpcServerType,
    action: NpcHitEvents.Impact.() -> Unit,
): Unit = onEvent(type.id, action)
