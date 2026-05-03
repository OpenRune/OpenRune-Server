package org.rsmod.api.script

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import org.rsmod.api.player.events.interact.ObjContentEvents
import org.rsmod.api.player.events.interact.ObjEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.plugin.scripts.ScriptContext

public fun ScriptContext.onOpObj1(
    type: ItemServerType,
    action: suspend ProtectedAccess.(ObjEvents.Op1) -> Unit,
): Unit = onProtectedEvent(type.id, action)

public fun ScriptContext.onOpObj2(
    type: ItemServerType,
    action: suspend ProtectedAccess.(ObjEvents.Op2) -> Unit,
): Unit = onProtectedEvent(type.id, action)

public fun ScriptContext.onOpObj3(
    type: ItemServerType,
    action: suspend ProtectedAccess.(ObjEvents.Op3) -> Unit,
): Unit = onProtectedEvent(type.id, action)

public fun ScriptContext.onOpObj4(
    type: ItemServerType,
    action: suspend ProtectedAccess.(ObjEvents.Op4) -> Unit,
): Unit = onProtectedEvent(type.id, action)

public fun ScriptContext.onOpObj5(
    type: ItemServerType,
    action: suspend ProtectedAccess.(ObjEvents.Op5) -> Unit,
): Unit = onProtectedEvent(type.id, action)

public fun ScriptContext.onOpObj1(
    content: String,
    action: suspend ProtectedAccess.(ObjContentEvents.Op1) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onOpObj2(
    content: String,
    action: suspend ProtectedAccess.(ObjContentEvents.Op2) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onOpObj3(
    content: String,
    action: suspend ProtectedAccess.(ObjContentEvents.Op3) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onOpObj4(
    content: String,
    action: suspend ProtectedAccess.(ObjContentEvents.Op4) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onOpObj5(
    content: String,
    action: suspend ProtectedAccess.(ObjContentEvents.Op5) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)
