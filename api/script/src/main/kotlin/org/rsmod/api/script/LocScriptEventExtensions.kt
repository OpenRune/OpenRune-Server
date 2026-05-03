package org.rsmod.api.script

import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.ObjectServerType
import org.rsmod.api.player.events.interact.LocContentEvents
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.events.interact.LocTContentEvents
import org.rsmod.api.player.events.interact.LocTDefaultEvents
import org.rsmod.api.player.events.interact.LocTEvents
import org.rsmod.api.player.events.interact.LocUContentEvents
import org.rsmod.api.player.events.interact.LocUDefaultEvents
import org.rsmod.api.player.events.interact.LocUEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.events.EventBus
import org.rsmod.plugin.scripts.ScriptContext

/* Op functions */

public fun ScriptContext.onOpLoc1(
    internal: String,
    action: suspend ProtectedAccess.(LocEvents.Op1) -> Unit,
): Unit = onProtectedEvent(internal.asRSCM(RSCMType.LOC), action)

public fun ScriptContext.onOpLoc2(
    type: String,
    action: suspend ProtectedAccess.(LocEvents.Op2) -> Unit,
): Unit = onProtectedEvent(type.asRSCM(RSCMType.LOC), action)

public fun ScriptContext.onOpLoc3(
    type: String,
    action: suspend ProtectedAccess.(LocEvents.Op3) -> Unit,
): Unit = onProtectedEvent(type.asRSCM(RSCMType.LOC), action)

public fun ScriptContext.onOpLoc4(
    type: String,
    action: suspend ProtectedAccess.(LocEvents.Op4) -> Unit,
): Unit = onProtectedEvent(type.asRSCM(RSCMType.LOC), action)

public fun ScriptContext.onOpLoc5(
    type: String,
    action: suspend ProtectedAccess.(LocEvents.Op5) -> Unit,
): Unit = onProtectedEvent(type.asRSCM(RSCMType.LOC), action)

public fun ScriptContext.onOpContentLoc1(
    content: String,
    action: suspend ProtectedAccess.(LocContentEvents.Op1) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onOpContentLoc2(
    content: String,
    action: suspend ProtectedAccess.(LocContentEvents.Op2) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onOpContentLoc3(
    content: String,
    action: suspend ProtectedAccess.(LocContentEvents.Op3) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onOpContentLoc4(
    content: String,
    action: suspend ProtectedAccess.(LocContentEvents.Op4) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onOpContentLoc5(
    content: String,
    action: suspend ProtectedAccess.(LocContentEvents.Op5) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onOpLocT(
    component: String,
    action: suspend ProtectedAccess.(LocTDefaultEvents.Op) -> Unit,
): Unit = onProtectedEvent(component.asRSCM(RSCMType.COMPONENT), action)

public fun ScriptContext.onOpLocT(
    type: String,
    component: String,
    action: suspend ProtectedAccess.(LocTEvents.Op) -> Unit,
): Unit = onProtectedEvent(EventBus.composeLongKey(type.asRSCM(RSCMType.LOC), component.asRSCM(RSCMType.COMPONENT)), action)

public fun ScriptContext.onOpContentLocT(
    content: String,
    component: String,
    action: suspend ProtectedAccess.(LocTContentEvents.Op) -> Unit,
): Unit = onProtectedEvent(EventBus.composeLongKey(content.asRSCM(RSCMType.CONTENT), component.asRSCM(RSCMType.COMPONENT)), action)

public fun ScriptContext.onOpLocU(
    type: String,
    action: suspend ProtectedAccess.(LocUDefaultEvents.OpType) -> Unit,
): Unit = onProtectedEvent(type.asRSCM(RSCMType.LOC), action)

public fun ScriptContext.onOpContentLocU(
    content: String,
    action: suspend ProtectedAccess.(LocUDefaultEvents.OpContent) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onOpLocU(
    type: String,
    objType: String,
    action: suspend ProtectedAccess.(LocUEvents.Op) -> Unit,
): Unit = onProtectedEvent(EventBus.composeLongKey(type.asRSCM(RSCMType.LOC), objType.asRSCM(RSCMType.OBJ)), action)

public fun ScriptContext.onOpContentMixedLocU(
    loccontent: String,
    objcontent: String,
    action: suspend ProtectedAccess.(LocUContentEvents.OpContent) -> Unit,
): Unit = onProtectedEvent(EventBus.composeLongKey(loccontent.asRSCM(RSCMType.CONTENT), objcontent.asRSCM(RSCMType.OBJ)), action)


public fun ScriptContext.onOpContentU(
    loccontent: String,
    objcontent: String,
    action: suspend ProtectedAccess.(LocUContentEvents.OpContent) -> Unit,
): Unit = onProtectedEvent(EventBus.composeLongKey(loccontent.asRSCM(RSCMType.CONTENT), objcontent.asRSCM(RSCMType.CONTENT)), action)


public fun ScriptContext.onOpContentLocU(
    loccontent: String,
    objcontent: String,
    action: suspend ProtectedAccess.(LocUContentEvents.OpContent) -> Unit,
): Unit = onProtectedEvent(EventBus.composeLongKey(loccontent.asRSCM(RSCMType.CONTENT), objcontent.asRSCM(RSCMType.OBJ)), action)

/* Ap functions */
public fun ScriptContext.onApLoc1(
    type: String,
    action: suspend ProtectedAccess.(LocEvents.Ap1) -> Unit,
): Unit = onProtectedEvent(type.asRSCM(RSCMType.LOC), action)

public fun ScriptContext.onApLoc2(
    type: String,
    action: suspend ProtectedAccess.(LocEvents.Ap2) -> Unit,
): Unit = onProtectedEvent(type.asRSCM(RSCMType.LOC), action)

public fun ScriptContext.onApLoc3(
    type: String,
    action: suspend ProtectedAccess.(LocEvents.Ap3) -> Unit,
): Unit = onProtectedEvent(type.asRSCM(RSCMType.LOC), action)

public fun ScriptContext.onApLoc4(
    type: String,
    action: suspend ProtectedAccess.(LocEvents.Ap4) -> Unit,
): Unit = onProtectedEvent(type.asRSCM(RSCMType.LOC), action)

public fun ScriptContext.onApLoc5(
    type: String,
    action: suspend ProtectedAccess.(LocEvents.Ap5) -> Unit,
): Unit = onProtectedEvent(type.asRSCM(RSCMType.LOC), action)

public fun ScriptContext.onApContentLoc1(
    content: String,
    action: suspend ProtectedAccess.(LocContentEvents.Ap1) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onApContentLoc2(
    content: String,
    action: suspend ProtectedAccess.(LocContentEvents.Ap2) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onApContentLoc3(
    content: String,
    action: suspend ProtectedAccess.(LocContentEvents.Ap3) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onApContentLoc4(
    content: String,
    action: suspend ProtectedAccess.(LocContentEvents.Ap4) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onApContentLoc5(
    content: String,
    action: suspend ProtectedAccess.(LocContentEvents.Ap5) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onApLocT(
    component: ComponentType,
    action: suspend ProtectedAccess.(LocTDefaultEvents.Ap) -> Unit,
): Unit = onProtectedEvent(component.packed, action)

public fun ScriptContext.onApLocT(
    type: ObjectServerType,
    component: ComponentType,
    action: suspend ProtectedAccess.(LocTEvents.Ap) -> Unit,
): Unit = onProtectedEvent(EventBus.composeLongKey(type.id, component.packed), action)

public fun ScriptContext.onApLocT(
    content: String,
    component: ComponentType,
    action: suspend ProtectedAccess.(LocTContentEvents.Ap) -> Unit,
): Unit = onProtectedEvent(EventBus.composeLongKey(content.asRSCM(RSCMType.CONTENT), component.packed), action)

public fun ScriptContext.onApLocU(
    type: ObjectServerType,
    action: suspend ProtectedAccess.(LocUDefaultEvents.ApType) -> Unit,
): Unit = onProtectedEvent(type.id, action)

public fun ScriptContext.onApLocU(
    content: String,
    action: suspend ProtectedAccess.(LocUDefaultEvents.ApContent) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onApLocU(
    type: ObjectServerType,
    objType: ItemServerType,
    action: suspend ProtectedAccess.(LocUEvents.Ap) -> Unit,
): Unit = onProtectedEvent(EventBus.composeLongKey(type.id, objType.id), action)

public fun ScriptContext.onApLocU(
    content: String,
    objType: ItemServerType,
    action: suspend ProtectedAccess.(LocUContentEvents.ApType) -> Unit,
): Unit = onProtectedEvent(EventBus.composeLongKey(content.asRSCM(RSCMType.CONTENT), objType.id), action)

public fun ScriptContext.onApLocU(
    loccontent: String,
    objcontent: String,
    action: suspend ProtectedAccess.(LocUContentEvents.ApContent) -> Unit,
): Unit = onProtectedEvent(EventBus.composeLongKey(loccontent.asRSCM(RSCMType.CONTENT), objcontent.asRSCM(RSCMType.CONTENT)), action)
