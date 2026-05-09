package org.rsmod.api.script

import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.WalkTriggerType
import org.rsmod.api.player.events.PlayerHitpointsChangedEvent
import org.rsmod.api.player.events.PlayerMovementEvent
import org.rsmod.api.player.events.PlayerQueueEvents
import org.rsmod.api.player.events.PlayerTimerEvent
import org.rsmod.api.player.events.interact.PlayerTEvents
import org.rsmod.api.player.events.interact.PlayerUContentEvents
import org.rsmod.api.player.events.interact.PlayerUEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.WorldMapClick
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.plugin.scripts.ScriptContext

public fun ScriptContext.onPlayerInit(action: SessionStateEvent.Initialize.() -> Unit): Unit =
    onEvent(action)

public fun ScriptContext.onPlayerLogin(action: SessionStateEvent.Login.() -> Unit): Unit =
    onEvent(action)

public fun ScriptContext.onPlayerLogout(action: SessionStateEvent.Logout.() -> Unit): Unit =
    onEvent(action)

/* Op functions */
public fun ScriptContext.onOpPlayerT(
    component: String,
    action: suspend ProtectedAccess.(PlayerTEvents.Op) -> Unit,
): Unit = onProtectedEvent(component.asRSCM(RSCMType.COMPONENT), action)

public fun ScriptContext.onOpPlayerU(
    content: String,
    action: suspend ProtectedAccess.(PlayerUContentEvents.Op) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onOpPlayerU(
    type: ItemServerType,
    action: suspend ProtectedAccess.(PlayerUEvents.Op) -> Unit,
): Unit = onProtectedEvent(type.id, action)

/* Ap functions */
public fun ScriptContext.onApPlayerT(
    component: ComponentType,
    action: suspend ProtectedAccess.(PlayerTEvents.Ap) -> Unit,
): Unit = onProtectedEvent(component.packed, action)

public fun ScriptContext.onApPlayerU(
    content: String,
    action: suspend ProtectedAccess.(PlayerUContentEvents.Ap) -> Unit,
): Unit = onProtectedEvent(content.asRSCM(RSCMType.CONTENT), action)

public fun ScriptContext.onApPlayerU(
    type: ItemServerType,
    action: suspend ProtectedAccess.(PlayerUEvents.Ap) -> Unit,
): Unit = onProtectedEvent(type.id, action)

/* Timer functions */
public fun ScriptContext.onPlayerTimer(
    timer: String,
    action: suspend ProtectedAccess.(PlayerTimerEvent.Normal) -> Unit,
): Unit = onProtectedEvent(timer.asRSCM(RSCMType.TIMER), action)

public fun ScriptContext.onPlayerSoftTimer(
    timer: String,
    action: PlayerTimerEvent.Soft.() -> Unit,
): Unit = onEvent(timer.asRSCM(RSCMType.TIMER), action)

/* Queue functions */
public fun ScriptContext.onPlayerQueue(
    queue: String,
    action: suspend ProtectedAccess.(PlayerQueueEvents.Protected<Nothing>) -> Unit,
): Unit = onProtectedEvent(queue.asRSCM(RSCMType.QUEUE), action)

public fun <T> ScriptContext.onPlayerQueueWithArgs(
    queue: String,
    action: suspend ProtectedAccess.(PlayerQueueEvents.Protected<T>) -> Unit,
): Unit = onProtectedEvent(queue.asRSCM(RSCMType.QUEUE), action)

public fun ScriptContext.onPlayerSoftQueue(
    queue: String,
    action: PlayerQueueEvents.Soft<Nothing>.() -> Unit,
): Unit = onEvent(queue.asRSCM(RSCMType.QUEUE), action)

public fun <T> ScriptContext.onPlayerSoftQueueWithArgs(
    queue: String,
    action: PlayerQueueEvents.Soft<T>.() -> Unit,
): Unit = onEvent(queue.asRSCM(RSCMType.QUEUE), action)

/* Walk trigger functions */
public fun ScriptContext.onPlayerWalkTrigger(
    trigger: String,
    action: PlayerMovementEvent.WalkTrigger.() -> Unit,
): Unit = onEvent(trigger.asRSCM(RSCMType.WALKTRIGGER), action)

public fun ScriptContext.onPlayerCoordsChanged(action: PlayerMovementEvent.CoordsMovedEvent.() -> Unit): Unit =
    onEvent(action)

public fun ScriptContext.onPlayerHitpointsChanged(action: PlayerHitpointsChangedEvent.() -> Unit): Unit =
    onEvent(action)

public fun ScriptContext.onWorldMapClick(
    internal: String,
    action: suspend ProtectedAccess.(WorldMapClick) -> Unit,
): Unit = onProtectedEvent(WorldMapClick.BUS_ID) { event: WorldMapClick ->
    if (!player.modLevel.hasAccessTo(internal)) {
        return@onProtectedEvent
    }
    action(event)
}
