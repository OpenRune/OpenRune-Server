package org.rsmod.api.script

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ControllerType
import org.rsmod.api.controller.access.StandardConAccess
import org.rsmod.api.controller.events.ControllerAIEvents
import org.rsmod.api.controller.events.ControllerQueueEvents
import org.rsmod.api.controller.events.ControllerTimerEvents
import org.rsmod.events.EventBus
import org.rsmod.plugin.scripts.ScriptContext

/* Timer functions */
public fun ScriptContext.onAiConTimer(
    type: String,
    action: ControllerAIEvents.Timer.() -> Unit,
): Unit = onEvent(type.asRSCM(RSCMType.CONTROLLER), action)

public fun ScriptContext.onConTimer(
    type: String,
    action: suspend StandardConAccess.(ControllerTimerEvents.Type) -> Unit,
): Unit = onConAccessEvent(type.asRSCM(RSCMType.CONTROLLER), action)

public fun ScriptContext.onConTimer(
    type: String,
    timer: String,
    action: suspend StandardConAccess.(ControllerTimerEvents.Type) -> Unit,
): Unit = onConAccessEvent(EventBus.composeLongKey(type.asRSCM(RSCMType.CONTROLLER), timer.asRSCM(RSCMType.TIMER)), action)

/* Queue functions */
public fun ScriptContext.onAiConQueue(
    type: String,
    action: ControllerAIEvents.Queue<Nothing>.() -> Unit,
): Unit = onEvent(type.asRSCM(RSCMType.CONTROLLER), action)

public fun <T> ScriptContext.onAiConQueueWithArgs(
    type: String,
    action: ControllerAIEvents.Queue<T>.() -> Unit,
): Unit = onEvent(type.asRSCM(RSCMType.CONTROLLER), action)

public fun ScriptContext.onConQueue(
    type: String,
    action: suspend StandardConAccess.(ControllerQueueEvents.Default<Nothing>) -> Unit,
): Unit = onConAccessEvent(type.asRSCM(RSCMType.QUEUE), action)

public fun <T> ScriptContext.onConQueueWithArgs(
    type: String,
    action: suspend StandardConAccess.(ControllerQueueEvents.Default<T>) -> Unit,
): Unit = onConAccessEvent(type.asRSCM(RSCMType.QUEUE), action)

public fun ScriptContext.onConQueue(
    type: ControllerType,
    queue: String,
    action: suspend StandardConAccess.(ControllerQueueEvents.Type<Nothing>) -> Unit,
): Unit = onConAccessEvent(EventBus.composeLongKey(type.id, queue.asRSCM(RSCMType.QUEUE)), action)

public fun <T> ScriptContext.onConQueueWithArgs(
    type: ControllerType,
    queue: String,
    action: suspend StandardConAccess.(ControllerQueueEvents.Type<T>) -> Unit,
): Unit = onConAccessEvent(EventBus.composeLongKey(type.id, queue.asRSCM(RSCMType.QUEUE)), action)
