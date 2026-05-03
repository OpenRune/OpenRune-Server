package org.rsmod.content.interfaces.gameframe

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.interf.IfSubType
import org.rsmod.api.player.ui.ifMoveSub
import org.rsmod.api.player.ui.ifOpenSub
import org.rsmod.api.player.ui.ifOpenTop
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.ui.Component
import org.rsmod.game.ui.UserInterfaceMap

internal fun Player.openGameframe(gameframe: Gameframe, eventBus: EventBus) {
    ui.setGameframe(gameframe.mappings)
    for (overlay in gameframe.overlays) {
        ifOpenSub(overlay.interf, overlay.target, IfSubType.Overlay, eventBus)
    }
}

internal fun Player.moveGameframe(from: Gameframe, dest: Gameframe, eventBus: EventBus) {
    ifOpenTop(dest.topLevel)
    ui.setGameframe(dest.mappings)
    val moveComponents = StandardOverlays.move
    for (moveComponent in moveComponents) {
        val target = Component(moveComponent.asRSCM(RSCMType.COMPONENT))
        val sourceComponent =
            from.mappings[target] ?: error("Expected move target in source mapping: '${moveComponent}'")
        val destComponent =
            dest.mappings[target] ?: error("Expected move target in dest mapping: '${moveComponent}'")
        ifMoveSub(sourceComponent, destComponent, target, eventBus)
    }
}

private fun UserInterfaceMap.setGameframe(mappings: Map<Component, Component>) {
    gameframe.clear()
    for ((original, translated) in mappings) {
        gameframe[original] = translated
    }
}
