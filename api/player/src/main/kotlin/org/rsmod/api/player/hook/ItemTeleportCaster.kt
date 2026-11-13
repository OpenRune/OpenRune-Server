package org.rsmod.api.player.hook

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.player.output.clearMapFlag
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.map.CoordGrid

@Singleton
public class ItemTeleportCaster
@Inject
constructor(
    private val teleportValidator: PlayerTeleportValidator,
    private val areaChecker: AreaChecker,
) {
    public fun ProtectedAccess.canTeleport(type: TeleportType): Boolean {
        val denial = teleportValidator.validate(player, type, areaChecker)
        if (denial == null) {
            return true
        }
        player.clearMapFlag()
        mes(denial)
        return false
    }

    public suspend fun ProtectedAccess.cast(
        type: TeleportType,
        target: CoordGrid,
        style: TeleportStyle = TeleportStyle.Standard,
        commit: ProtectedAccess.() -> Boolean = { true },
    ) {
        if (!canTeleport(type)) {
            return
        }
        actionDelay = mapClock + ACTION_DELAY
        anim(style.startAnim)
        spotanim(style.spotanim, height = style.spotanimHeight)
        soundSynth(style.sound)
        delay(TELEPORT_DELAY)
        if (!canTeleport(type) || !commit()) {
            resetAnim()
            resetSpotanim()
            return
        }
        telejump(target, type)
        val end = style.endAnim
        if (end != null) {
            anim(end)
        } else {
            resetAnim()
        }
    }

    private companion object {
        private const val TELEPORT_DELAY = 3
        private const val ACTION_DELAY = 5
    }
}

public data class TeleportStyle(
    val startAnim: String,
    val endAnim: String?,
    val spotanim: String,
    val spotanimHeight: Int,
    val sound: String,
) {
    public companion object {
        public val Standard: TeleportStyle =
            TeleportStyle(
                startAnim = "seq.human_castteleport",
                endAnim = "seq.human_castteleport_reverse",
                spotanim = "spotanim.teleport_casting",
                spotanimHeight = 92,
                sound = "synth.teleport_all",
            )

        public val Xeric: TeleportStyle =
            TeleportStyle(
                startAnim = "seq.arceuus_necromancy_anim",
                endAnim = null,
                spotanim = "spotanim.xeric_teleport_spotanim",
                spotanimHeight = 0,
                sound = "synth.teleport_all",
            )
    }
}
