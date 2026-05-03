package org.rsmod.api.script

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import org.rsmod.api.player.events.interact.WornObjContentEvents
import org.rsmod.api.player.events.interact.WornObjEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.plugin.scripts.ScriptContext

/* Standard obj op functions */
/** **Important Note:** This replaces the default unequip op handling for obj [type]. */
public fun ScriptContext.onOpWorn1(
    type: String,
    action: suspend ProtectedAccess.(WornObjEvents.Op1) -> Unit,
): Unit = onProtectedEvent(type.asRSCM(RSCMType.OBJ), action)

public fun ScriptContext.onOpWorn2(
    type: String,
    action: suspend ProtectedAccess.(WornObjEvents.Op2) -> Unit,
): Unit = onProtectedEvent(type.asRSCM(RSCMType.OBJ), action)

public fun ScriptContext.onOpWorn3(
    type: String,
    action: suspend ProtectedAccess.(WornObjEvents.Op3) -> Unit,
): Unit = onProtectedEvent(type.asRSCM(RSCMType.OBJ), action)

public fun ScriptContext.onOpWorn4(
    type: String,
    action: suspend ProtectedAccess.(WornObjEvents.Op4) -> Unit,
): Unit = onProtectedEvent(type.asRSCM(RSCMType.OBJ), action)

public fun ScriptContext.onOpWorn5(
    type: String,
    action: suspend ProtectedAccess.(WornObjEvents.Op5) -> Unit,
): Unit = onProtectedEvent(type.asRSCM(RSCMType.OBJ), action)

public fun ScriptContext.onOpWorn6(
    type: String,
    action: suspend ProtectedAccess.(WornObjEvents.Op6) -> Unit,
): Unit = onProtectedEvent(type.asRSCM(RSCMType.OBJ), action)

public fun ScriptContext.onOpWorn7(
    type: String,
    action: suspend ProtectedAccess.(WornObjEvents.Op7) -> Unit,
): Unit = onProtectedEvent(type.asRSCM(RSCMType.OBJ), action)

public fun ScriptContext.onOpWorn8(
    type: ItemServerType,
    action: suspend ProtectedAccess.(WornObjEvents.Op8) -> Unit,
): Unit = onProtectedEvent(type.id, action)

public fun ScriptContext.onOpWorn9(
    type: ItemServerType,
    action: suspend ProtectedAccess.(WornObjEvents.Op9) -> Unit,
): Unit = onProtectedEvent(type.id, action)

/* Standard content op functions */
/**
 * **Important Note:** This replaces the default unequip op handling for content group [content].
 */
public fun ScriptContext.onOpContentWorn1(
    content: String,
    action: suspend ProtectedAccess.(WornObjContentEvents.Op1) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onOpContentWorn2(
    content: String,
    action: suspend ProtectedAccess.(WornObjContentEvents.Op2) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onOpContentWorn3(
    content: String,
    action: suspend ProtectedAccess.(WornObjContentEvents.Op3) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onOpContentWorn4(
    content: String,
    action: suspend ProtectedAccess.(WornObjContentEvents.Op3) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onOpContentWorn5(
    content: String,
    action: suspend ProtectedAccess.(WornObjContentEvents.Op5) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onOpContentWorn6(
    content: String,
    action: suspend ProtectedAccess.(WornObjContentEvents.Op6) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onOpContentWorn7(
    content: String,
    action: suspend ProtectedAccess.(WornObjContentEvents.Op7) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onOpContentWorn8(
    content: String,
    action: suspend ProtectedAccess.(WornObjContentEvents.Op8) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onOpContentWorn9(
    content: String,
    action: suspend ProtectedAccess.(WornObjContentEvents.Op9) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)
