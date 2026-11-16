package org.rsmod.content.bosses.moons

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape

/** The ring of moon-phase floor sigils shown around an arena while the jaguars are out. */
@Singleton
class MoonsOverlays @Inject constructor(private val deps: BossDeps) {

    fun show(moon: Moon) {
        val placed =
            OVERLAY_LOCS.mapIndexed { focus, loc ->
                val (dx, dz) = Moon.JAGUAR_OFFSETS[focus]
                val info =
                    deps.locRepo.add(
                        moon.southWest.translate(dx, dz),
                        loc,
                        Int.MAX_VALUE,
                        LocAngle.West,
                        LocShape.CentrepieceDiagonal,
                    )
                deps.worldRepo.locAnim(info, SPAWN_SEQ)
                info
            }
        deps.worldQueues.add(1) { placed.forEach { deps.worldRepo.locAnim(it, IDLE_SEQ) } }
        deps.worldQueues.add(SHOWN_TICKS) {
            placed.forEach {
                deps.worldRepo.locAnim(it, DESPAWN_SEQ)
                deps.locRepo.del(it, Int.MAX_VALUE)
            }
        }
    }

    private companion object {
        const val SHOWN_TICKS = 41
        const val SPAWN_SEQ = "seq.vfx_djinn_ueyatlalli_protection_spawn_01"
        const val IDLE_SEQ = "seq.vfx_djinn_ueyatlalli_protection_idle_01"
        const val DESPAWN_SEQ = "seq.vfx_djinn_ueyatlalli_protection_despawn_01"

        val OVERLAY_LOCS =
            listOf(
                "loc.pmoon_boss_overlay_new",
                "loc.pmoon_boss_overlay_quarter_waxing",
                "loc.pmoon_boss_overlay_half_waxing",
                "loc.pmoon_boss_overlay_threequarters_waxing",
                "loc.pmoon_boss_overlay_full",
                "loc.pmoon_boss_overlay_threequarter_waning",
                "loc.pmoon_boss_overlay_half_waning",
                "loc.pmoon_boss_overlay_quarter_waning",
            )
    }
}
