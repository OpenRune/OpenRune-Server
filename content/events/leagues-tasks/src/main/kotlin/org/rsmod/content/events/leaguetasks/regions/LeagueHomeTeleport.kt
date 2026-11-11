package org.rsmod.content.events.leaguetasks.regions

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.player.hook.PlayerTeleportValidator
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.clearMapFlag
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.map.CoordGrid

/**
 * Eight stages three ticks apart, each a player seq plus a map anim on the caster's tile, then the
 * move one tick after the last stage. Walking cancels it.
 */
@Singleton
public class LeagueHomeTeleport
@Inject
constructor(
    private val validator: PlayerTeleportValidator,
    private val areaChecker: AreaChecker,
    private val world: WorldRepository,
) {
    public fun start(access: ProtectedAccess, dest: CoordGrid, landingRadius: Int) {
        with(access) {
            // UNVERIFIED: refusal causes other than moving and combat.
            if (player.routeDestination.isNotEmpty() || isInCombat()) {
                mes("You can't use that teleport at the moment.")
                return
            }
            if (!canTeleport(dest)) {
                return
            }
            player.clearMapFlag()
            resetAnim()
            resetSpotanim()
            walkTrigger(CANCEL_ANIM_WALK_TRIGGER)
            clearWeakQueue(QUEUE)
            val first = Pending(dest.packed, landingRadius, stage = 0)
            weakQueue(QUEUE, FIRST_STAGE_DELAY + SAME_TICK_QUEUE, first)
        }
    }

    internal suspend fun process(access: ProtectedAccess, pending: Pending) {
        with(access) {
            val (seq, spotanim) = STAGES[pending.stage]
            anim(seq)
            spotanimMap(world, spotanim, player.coords)
            if (pending.stage < STAGES.lastIndex) {
                val next = pending.copy(stage = pending.stage + 1)
                weakQueue(QUEUE, STAGE_INTERVAL + SAME_TICK_QUEUE, next)
                return
            }
            vars[BUSY] = 1
            delay(1)
            player.clearWalkTrigger()
            vars[BUSY] = 0
            val dest = CoordGrid(pending.dest)
            if (canTeleport(dest)) {
                val landing =
                    mapFindSquareLineOfWalk(dest, minRadius = 0, maxRadius = pending.landingRadius)
                telejump(landing ?: dest)
            }
            resetAnim()
            resetSpotanim()
        }
    }

    private fun ProtectedAccess.canTeleport(dest: CoordGrid): Boolean {
        val denial =
            validator.validate(player, dest, TeleportType.Standard, areaChecker) ?: return true
        player.clearMapFlag()
        player.mes(denial, ChatType.Engine)
        return false
    }

    internal data class Pending(val dest: Int, val landingRadius: Int, val stage: Int)

    internal companion object {
        const val QUEUE = "queue.league_home_teleport"
        const val FIRST_STAGE_DELAY = 1
        const val STAGE_INTERVAL = 3
        const val SAME_TICK_QUEUE = 1
        const val BUSY = "varbit.busy"
        const val CANCEL_ANIM_WALK_TRIGGER = "walktrigger.emote_cancelanim"

        const val STAGE_COUNT = 8

        val STAGES =
            (1..STAGE_COUNT).map {
                val stage = "%02d".format(it)
                "seq.leagues_6_home_teleport_player_$stage" to
                    "spotanim.leagues_6_home_teleport_spotanim_$stage"
            }
    }
}
