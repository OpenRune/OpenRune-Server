package org.rsmod.api.net.rsprot.handlers

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.types.aconverted.interf.IfButtonOp
import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.buttons.If1Button
import org.rsmod.annotations.InternalApi
import org.rsmod.api.net.rsprot.player.InterfaceEvents
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.IfModalButton
import org.rsmod.api.player.ui.IfOverlayButton
import org.rsmod.api.player.ui.ifCloseInputDialog
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.ui.Component

/**
 * Handles clicks on legacy if1-format components (e.g. the tanner interface).
 *
 * Unlike [If3Button][net.rsprot.protocol.game.incoming.buttons.If1Button]'s if3 sibling,
 * the if1 packet carries only the component (no op number, no comsub, and no obj).
 *
 * To account for that, incoming clicks are normalized to [IfButtonOp.Op1] with comsub -1 and
 * published as the same [IfModalButton]/[IfOverlayButton] events the if3 handler produces,
 * so content scripts handle both formats through the same `onIfModalButton`/`onIfOverlayButton` registration.
 */
class If1ButtonHandler
@Inject
constructor(private val eventBus: EventBus, private val protectedAccess: ProtectedAccessLauncher) :
    MessageHandler<If1Button> {
    private val logger = InlineLogger()

    private val If1Button.asComponent: Component
        get() = Component(interfaceId, componentId)

    @OptIn(InternalApi::class)
    override fun handle(player: Player, message: If1Button) {
        val componentType = ServerCacheManager.fromComponent(message.asComponent.packed)
        val interfaceType = ServerCacheManager.fromInterface(message.asComponent.packed)

        val opEnabled =
            componentType.hasEvent(IfEvent.Op1) ||
                InterfaceEvents.isEnabled(player.ui, componentType, 0, IfEvent.Op1)
        if (!opEnabled) {
            logger.debug { "[Gated] If1Button: $message" }
            return
        }

        if (player.ui.containsOverlay(interfaceType) || player.ui.containsTopLevel(interfaceType)) {
            val event = IfOverlayButton(componentType, comsub = -1, obj = null, op = IfButtonOp.Op1)
            logger.debug { "[Overlay] If1Button: $message (event=$event)" }
            protectedAccess.launchLenient(player) { eventBus.publish(this, event) }
            return
        }

        if (player.ui.containsModal(interfaceType)) {
            val event = IfModalButton(componentType, comsub = -1, obj = null, op = IfButtonOp.Op1)
            player.ifCloseInputDialog()
            if (player.isModalButtonProtected) {
                logger.debug { "[Modal][BLOCKED] If1Button: $message (event=$event)" }
                return
            }
            logger.debug { "[Modal] If1Button: $message (event=$event)" }
            protectedAccess.launchLenient(player) { eventBus.publish(this, event) }
        }
    }
}
