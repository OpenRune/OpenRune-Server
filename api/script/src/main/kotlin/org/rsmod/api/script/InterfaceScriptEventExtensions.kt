package org.rsmod.api.script

import dev.openrune.cache.filestore.definition.InterfaceType
import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.IfCloseSub
import org.rsmod.api.player.ui.IfModalButton
import org.rsmod.api.player.ui.IfModalButtonT
import org.rsmod.api.player.ui.IfModalDrag
import org.rsmod.api.player.ui.IfModalSubOpMenu
import org.rsmod.api.player.ui.IfOpenSub
import org.rsmod.api.player.ui.IfOverlayButton
import org.rsmod.api.player.ui.IfOverlayButtonT
import org.rsmod.api.player.ui.IfOverlayDrag
import org.rsmod.api.player.ui.IfOverlaySubOpMenu
import org.rsmod.events.EventBus
import org.rsmod.plugin.scripts.ScriptContext

public fun ScriptContext.onIfOpen(type: String, action: IfOpenSub.() -> Unit): Unit =
    onEvent(type.asRSCM(RSCMType.INTERFACE), action)

public fun ScriptContext.onIfClose(type: String, action: IfCloseSub.() -> Unit): Unit =
    onEvent(type.asRSCM(RSCMType.INTERFACE), action)

public fun ScriptContext.onIfOverlayButton(
    button: String,
    action: suspend ProtectedAccess.(IfOverlayButton) -> Unit,
): Unit = onProtectedEvent(button.asRSCM(RSCMType.COMPONENT), action)

public fun ScriptContext.onIfOverlayButton(
    button: ComponentType,
    action: suspend ProtectedAccess.(IfOverlayButton) -> Unit,
): Unit = onProtectedEvent(button.packed, action)

public fun ScriptContext.onIfModalButton(
    button: String,
    action: suspend ProtectedAccess.(IfModalButton) -> Unit,
): Unit = onProtectedEvent(button.asRSCM(RSCMType.COMPONENT), action)

public fun ScriptContext.onIfOverlaySubOpMenu(
    button: String,
    action: suspend ProtectedAccess.(IfOverlaySubOpMenu) -> Unit,
): Unit = onProtectedEvent(button.asRSCM(RSCMType.COMPONENT), action)

public fun ScriptContext.onIfModalSubOpMenu(
    button: String,
    action: suspend ProtectedAccess.(IfModalSubOpMenu) -> Unit,
): Unit = onProtectedEvent(button.asRSCM(RSCMType.COMPONENT), action)

public fun ScriptContext.onIfOverlayButtonT(
    selectedComponent: String,
    targetComponent: String = selectedComponent,
    action: IfOverlayButtonT.() -> Unit,
) {
    val packed = EventBus.composeLongKey(selectedComponent.asRSCM(RSCMType.COMPONENT), targetComponent.asRSCM(RSCMType.COMPONENT))
    onEvent(packed, action)
}

public fun ScriptContext.onIfModalButtonT(
    selectedComponent: String,
    targetComponent: String = selectedComponent,
    action: suspend ProtectedAccess.(IfModalButtonT) -> Unit,
) {
    val packed = EventBus.composeLongKey(selectedComponent.asRSCM(RSCMType.COMPONENT), targetComponent.asRSCM(RSCMType.COMPONENT))
    onProtectedEvent(packed, action)
}

public fun ScriptContext.onIfOverlayDrag(
    selectedComponent: String,
    targetComponent: String = selectedComponent,
    action: IfOverlayDrag.() -> Unit,
) {
    val packed = EventBus.composeLongKey(selectedComponent.asRSCM(RSCMType.COMPONENT), targetComponent.asRSCM(RSCMType.COMPONENT))
    onEvent(packed, action)
}

public fun ScriptContext.onIfModalDrag(
    selectedComponent: String,
    targetComponent: String = selectedComponent,
    action: suspend ProtectedAccess.(IfModalDrag) -> Unit,
) {
    val packed = EventBus.composeLongKey(selectedComponent.asRSCM(RSCMType.COMPONENT), targetComponent.asRSCM(RSCMType.COMPONENT))
    onProtectedEvent(packed, action)
}
