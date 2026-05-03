package org.rsmod.api.script.advanced

import dev.openrune.cache.filestore.definition.InterfaceType
import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.ui.IfMoveSub
import org.rsmod.api.player.ui.IfMoveTop
import org.rsmod.api.script.onEvent
import org.rsmod.plugin.scripts.ScriptContext

public fun ScriptContext.onIfMoveTop(type: InterfaceType, action: IfMoveTop.() -> Unit): Unit =
    onEvent<IfMoveTop>(type.id, action)

public fun ScriptContext.onIfMoveSub(dest: String, action: IfMoveSub.() -> Unit): Unit =
    onEvent<IfMoveSub>(dest.asRSCM(RSCMType.COMPONENT), action)
