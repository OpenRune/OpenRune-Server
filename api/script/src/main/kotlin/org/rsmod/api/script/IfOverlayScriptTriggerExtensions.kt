package org.rsmod.api.script

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.IfOverlayScriptTrigger
import org.rsmod.api.player.ui.IfScriptArgsCodec
import org.rsmod.plugin.scripts.ScriptContext

public inline fun <reified T : Any> ScriptContext.onIfScriptTrigger(
    component: String,
    noinline action: suspend ProtectedAccess.(args: T) -> Unit,
) {
    val packed = component.asRSCM(RSCMType.COMPONENT)
    IfScriptArgsCodec.registerParameterTypes(packed, T::class)
    onProtectedEvent<IfOverlayScriptTrigger>(packed.toLong()) {
        action(IfScriptArgsCodec.decode(T::class, it.args))
    }
}
