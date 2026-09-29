package org.rsmod.api.player.protect

import jakarta.inject.Inject
import org.rsmod.annotations.InternalApi
import org.rsmod.api.player.output.mes
import org.rsmod.game.entity.Player

public class ProtectedAccessLauncher
@Inject
constructor(private val contextFactory: ProtectedAccessContextFactory) {
    public fun launch(
        player: Player,
        busyText: String? = null,
        block: suspend ProtectedAccess.() -> Unit,
    ): Boolean = withProtectedAccess(player, contextFactory.create(), busyText, block)

    @InternalApi(message = "Usage of this function should only be used internally, or sparingly.")
    public fun launchLenient(player: Player, block: suspend ProtectedAccess.() -> Unit) {
        player.launch {
            val protectedAccess = ProtectedAccess(player, this, contextFactory.create())
            block(protectedAccess)
        }
    }

    /**
     * Publishes [block] alongside a suspended script instead of replacing it.
     *
     * Used by the overlay input paths, which the real game answers on the cycle they arrive while a
     * paused script keeps its own schedule. Because the player's coroutine and delay are left
     * alone, `isAccessProtected` and `isModalButtonProtected` stay accurate inside [block], so a
     * handler that refuses to run while the player is busy still refuses.
     */
    @InternalApi(message = "Usage of this function should only be used internally, or sparingly.")
    public fun launchBeside(player: Player, block: suspend ProtectedAccess.() -> Unit) {
        if (player.activeCoroutine?.isSuspended != true) {
            launchLenient(player, block)
            return
        }
        player.launchBeside {
            val protectedAccess = ProtectedAccess(player, this, contextFactory.create())
            block(protectedAccess)
        }
    }

    public companion object {
        public fun withProtectedAccess(
            player: Player,
            context: ProtectedAccessContext,
            busyText: String? = null,
            block: suspend ProtectedAccess.() -> Unit,
        ): Boolean {
            if (player.isAccessProtected) {
                busyText?.let { player.mes(it) }
                return false
            }
            player.launch {
                val protectedAccess = ProtectedAccess(player, this, context)
                block(protectedAccess)
            }
            return true
        }
    }
}
