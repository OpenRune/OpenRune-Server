package org.alter.game.ui

import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.definition.type.widget.IfEvent


internal object InterfaceEvents {
    fun isEnabled(
        ui: UserInterfaceMap,
        component: ComponentType,
        comsub: Int,
        event: IfEvent,
    ): Boolean {
        val verifyStaticEvents = comsub == -1
        return if (verifyStaticEvents) {
            component.hasEvent(event)
        } else {
            ui.hasEvent(component, comsub, event)
        }
    }

}
