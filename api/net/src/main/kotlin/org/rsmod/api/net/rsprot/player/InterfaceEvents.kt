package org.rsmod.api.net.rsprot.player

import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.definition.type.widget.IfEvent
import org.rsmod.game.ui.UserInterfaceMap

internal object InterfaceEvents {
    fun isEnabled(
        ui: UserInterfaceMap,
        component: ComponentType,
        comsub: Int,
        event: IfEvent,
    ): Boolean {
        val staticEnabled = comsub == -1 && component.hasEvent(event)
        return staticEnabled || ui.hasEvent(component, comsub, event)
    }
}
