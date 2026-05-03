package org.rsmod.api.script

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.AreaType
import org.rsmod.api.player.events.EngineQueueEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.events.EventBus
import org.rsmod.game.queue.EngineQueueType
import org.rsmod.plugin.scripts.ScriptContext

private fun ScriptContext.onEngineQueue(
    type: EngineQueueType,
    label: Int,
    action: suspend ProtectedAccess.(EngineQueueEvents.Labelled) -> Unit,
) {
    onProtectedEvent(EventBus.composeLongKey(label, type.id), action)
    engineQueueCache.addLabelled(type, label)
}

public fun ScriptContext.onArea(
    area: String,
    action: suspend ProtectedAccess.(EngineQueueEvents.Labelled) -> Unit,
): Unit = onEngineQueue(EngineQueueType.Area, area.asRSCM(RSCMType.AREA), action)

public fun ScriptContext.onAreaExit(
    area: String,
    action: suspend ProtectedAccess.(EngineQueueEvents.Labelled) -> Unit,
): Unit = onEngineQueue(EngineQueueType.AreaExit, area.asRSCM(RSCMType.AREA), action)
