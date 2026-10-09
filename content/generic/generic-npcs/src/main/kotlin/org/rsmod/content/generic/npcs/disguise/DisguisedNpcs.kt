package org.rsmod.content.generic.npcs.disguise

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.hunt.HuntVis
import jakarta.inject.Inject
import org.rsmod.annotations.InternalApi
import org.rsmod.api.config.constants
import org.rsmod.api.hunt.Hunt
import org.rsmod.api.npc.aggression.AggressionTolerance
import org.rsmod.api.npc.isInCombat
import org.rsmod.api.script.onAiTimer
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PlayerList
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

/**
 * The npc's base type never changes, only its transmog does, so stats, params, drops and respawns
 * come from [dormant] and both types need the same combat params.
 */
data class Disguise(
    val dormant: String,
    val awake: String,
    val reveal: String = "seq.horror_crab_reveal",
    val hide: String = "seq.horror_crab_hide",
    val huntMode: Int = AGGRESSIVE_MELEE,
    val wakeRange: Int = 1,
    val restTicks: Int = 20,
    val tolerant: Boolean = true,
)

private const val AGGRESSIVE_MELEE = 6

class DisguisedNpcs
@Inject
constructor(
    private val hunt: Hunt,
    private val tolerance: AggressionTolerance,
    private val players: PlayerList,
    private val collision: CollisionFlagMap,
) {
    fun ScriptContext.bind(disguise: Disguise) {
        val dormant = disguise.dormant.asRSCM(RSCMType.NPC)
        if (disguise.tolerant) {
            tolerance.enroll(disguise.dormant)
            tolerance.enroll(disguise.awake)
        }
        onAiTimer(disguise.dormant) { dormantTick(npc, disguise) }
        onAiTimer(disguise.awake) {
            if (npc.type.id == dormant) {
                awakeTick(npc, disguise)
            }
        }
    }

    @OptIn(InternalApi::class)
    internal fun dormantTick(npc: Npc, disguise: Disguise) {
        if (!npc.isAnyoneNear()) {
            return
        }
        val reach = disguise.wakeRange + npc.size - 1
        val nearby = hunt.findPlayers(npc.coords, reach, HuntVis.LineOfSight)
        val wakers = nearby.filter { npc.isWithinDistance(it, disguise.wakeRange) }
        if (wakers.any { !tolerance.isTolerant(npc, it) }) {
            wake(npc, disguise)
        }
    }

    @OptIn(InternalApi::class)
    internal fun wake(npc: Npc, disguise: Disguise) {
        val awake = checkNotNull(ServerCacheManager.getNpc(disguise.awake.asRSCM(RSCMType.NPC)))
        npc.transmog(awake, Int.MAX_VALUE)
        npc.assignUid()
        npc.vars[IDLE] = 0
        npc.anim(disguise.reveal)
        npc.delay(disguise.reveal.ticks())
        val mode = checkNotNull(ServerCacheManager.getHunt(disguise.huntMode))
        npc.setHuntMode(mode)
        npc.setHunt(npc.type.huntRange)
    }

    @OptIn(InternalApi::class)
    internal fun awakeTick(npc: Npc, disguise: Disguise) {
        val idle = npc.vars[IDLE]
        if (idle == BURROWING) {
            surface(npc)
            return
        }

        if (npc.isEngaged()) {
            npc.facingTarget(players)?.let { tolerance.isTolerant(it) }
            npc.vars[IDLE] = 0
            return
        }

        val ticks = idle + 1
        npc.vars[IDLE] = ticks
        if (ticks < disguise.restTicks) {
            return
        }

        val home = npc.coords == npc.spawnCoords
        if (!home && ticks < disguise.restTicks + RETURN_GRACE) {
            if (npc.routeDestination.size == 0) {
                npc.walk(npc.spawnCoords)
            }
            return
        }
        burrow(npc, disguise)
    }

    private fun burrow(npc: Npc, disguise: Disguise) {
        npc.abortRoute()
        npc.resetFaceEntity()
        npc.vars[IDLE] = BURROWING
        npc.anim(disguise.hide)
        npc.delay(disguise.hide.ticks())
    }

    @OptIn(InternalApi::class)
    private fun surface(npc: Npc) {
        npc.resetTransmog()
        npc.assignUid()
        npc.resetHunt()
        npc.copyCurrentStats(npc.type)
        npc.vars[IDLE] = 0
        if (npc.coords != npc.spawnCoords) {
            npc.telejump(collision, npc.spawnCoords)
        }
    }

    private fun Npc.isEngaged(): Boolean =
        isInCombat() || vars["varn.lastcombat"] + constants.combat_activecombat_delay >= currentMapClock

    private fun String.ticks(): Int {
        val seq = checkNotNull(ServerCacheManager.getAnim(asRSCM(RSCMType.SEQ)))
        return seq.tickDuration.coerceAtLeast(1)
    }

    companion object {
        const val IDLE = "varn.disguise_idle"
        const val BURROWING = -1
        const val RETURN_GRACE = 30
    }
}
