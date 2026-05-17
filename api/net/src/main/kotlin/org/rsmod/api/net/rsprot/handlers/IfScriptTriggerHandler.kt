package org.rsmod.api.net.rsprot.handlers

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.ServerCacheManager
import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.buttons.IfScriptTrigger
import org.rsmod.annotations.InternalApi
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.IfOverlayScriptTrigger
import org.rsmod.api.player.ui.IfScriptParameterRegistry
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.ui.Component

class IfScriptTriggerHandler
@Inject
constructor(
    private val eventBus: EventBus,
    private val protectedAccess: ProtectedAccessLauncher,
) : MessageHandler<IfScriptTrigger> {
    private val logger = InlineLogger()

    private val IfScriptTrigger.asComponent: Component
        get() = Component(interfaceId, componentId)

    @OptIn(InternalApi::class)
    override fun handle(player: Player, message: IfScriptTrigger) {
        val componentType = ServerCacheManager.fromComponent(message.asComponent.packed)

        val parameterTypes = IfScriptParameterRegistry[componentType.packed]
        val args =
            try {
                message.decode(parameterTypes).args
            } catch (e: Exception) {
                logger.warn(e) {
                    "Failed to decode IfScriptTrigger for component=${componentType.internalName} " +
                        "(crc=${message.crc}, types=${parameterTypes.chars.contentToString()})"
                }
                return
            }

        val event =
            IfOverlayScriptTrigger(
                component = componentType,
                comsub = message.sub,
                obj = ServerCacheManager.getItem(message.obj),
                crc = message.crc,
                args = args,
            )

        protectedAccess.launchLenient(player) { eventBus.publish(this, event) }
    }
}
