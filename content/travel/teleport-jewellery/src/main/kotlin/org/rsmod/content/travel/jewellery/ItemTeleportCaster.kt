package org.rsmod.content.travel.jewellery

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.player.hook.PlayerTeleportValidator
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.clearMapFlag
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.map.CoordGrid

@Singleton
class ItemTeleportCaster
@Inject
constructor(
    private val teleportValidator: PlayerTeleportValidator,
    private val areaChecker: AreaChecker,
) {
    fun ProtectedAccess.canTeleport(type: TeleportType): Boolean {
        val denial = teleportValidator.validate(player, type, areaChecker)
        if (denial == null) {
            return true
        }
        player.clearMapFlag()
        mes(denial)
        return false
    }

    /**
     * Plays the standard teleport cast and lands on [target]. [commit] runs after the cast delay,
     * once the teleport is certain to happen, and returns false to cancel it.
     */
    suspend fun ProtectedAccess.cast(
        type: TeleportType,
        target: CoordGrid,
        commit: ProtectedAccess.() -> Boolean = { true },
    ) {
        if (!canTeleport(type)) {
            return
        }
        actionDelay = mapClock + ACTION_DELAY
        anim(RSCM.getReverseMapping(RSCMType.SEQ, START_ANIM))
        spotanim(RSCM.getReverseMapping(RSCMType.SPOTANIM, SPOTANIM), height = SPOTANIM_HEIGHT)
        soundSynth(TELEPORT_SOUND)
        delay(TELEPORT_DELAY)
        if (!canTeleport(type) || !commit()) {
            resetAnim()
            resetSpotanim()
            return
        }
        telejump(target)
        anim(RSCM.getReverseMapping(RSCMType.SEQ, END_ANIM))
    }

    private companion object {
        private const val START_ANIM = 714
        private const val END_ANIM = 715
        private const val SPOTANIM = 111
        private const val SPOTANIM_HEIGHT = 92
        private const val TELEPORT_SOUND = "synth.teleport_all"
        private const val TELEPORT_DELAY = 3
        private const val ACTION_DELAY = 5
    }
}
