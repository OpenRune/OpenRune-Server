package org.rsmod.content.bosses.giantmole

import jakarta.inject.Inject
import org.rsmod.api.player.hook.SpadeDigHook
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.Player

internal class MoleHillDigHook @Inject constructor(private val environment: MoleHoleEnvironment) :
    SpadeDigHook {
    override fun claims(player: Player): Boolean = GiantMoleRules.onMoleHill(player.coords)

    override suspend fun ProtectedAccess.beforeDig(): Boolean {
        if (!environment.hasLightSource(player) && !environment.hasPermanentLight(player)) {
            playDig()
            mes(NO_LIGHT_MESSAGE)
            return false
        }
        return true
    }

    override suspend fun ProtectedAccess.dig() {
        if (!environment.hasTinderbox(player) && !environment.hasPermanentLight(player)) {
            openMoleWarning()
            return
        }
        enterMoleHole(environment)
    }
}
