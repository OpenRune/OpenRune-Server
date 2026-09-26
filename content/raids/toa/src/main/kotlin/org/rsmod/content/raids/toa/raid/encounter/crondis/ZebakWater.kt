package org.rsmod.content.raids.toa.raid.encounter.crondis.zebak

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.BasType
import org.rsmod.annotations.InternalApi
import org.rsmod.api.player.hit.modifier.NoopPlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.mes
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.map.CoordGrid

/**
 * The water around the arena: players washed in by Tidal Waves swim (swim render anims; no
 * starting combat, see ZebakSwimAttackHook) until they climb the rock steps, while the water
 * crocodiles hunt them (Offline_Scape WaterCrocodile, RockStepsAction).
 */
internal class ZebakWater(private val room: ZebakEncounter) {
    private val deps = room.raid.deps
    private val swimmers = HashSet<Player>()
    private val crocodiles = ArrayList<Npc>()
    private val biteCountdown = HashMap<Npc, Int>()

    private val swimBas: BasType by lazy {
        val swim = ZebakSeqs.SWIM.asRSCM(RSCMType.SEQ)
        BasType(
            readyAnim = ZebakSeqs.SWIM_READY.asRSCM(RSCMType.SEQ),
            turnOnSpot = swim,
            walkForward = swim,
            walkBack = swim,
            walkLeft = swim,
            walkRight = swim,
            running = swim,
        )
    }

    fun isSwimming(player: Player): Boolean = player in swimmers

    /** Assumes the water tiles are walkable with the edge blocking the way in (Offline_Scape). */
    fun canSwimTo(tile: CoordGrid): Boolean = !deps.collision.isWalkBlocked(tile)

    fun startSwimming(player: Player) {
        if (!swimmers.add(player)) return
        player.bas = swimBas
        player.rebuildAppearance()
    }

    fun stopSwimming(player: Player) {
        if (!swimmers.remove(player)) return
        player.bas = null
        player.rebuildAppearance()
    }

    /** Dead and ghost players leave the water (Offline_Scape reset the render on death). */
    fun dropDeadSwimmers() {
        for (player in swimmers.toList()) {
            if (room.raid.isGhost(player) || room.raid.isDying(player)) stopSwimming(player)
        }
    }

    /** Onto the tile beside the steps: north for angle 0, else south. */
    @OptIn(InternalApi::class)
    fun climbOut(player: Player, rock: CoordGrid, angleId: Int) {
        if (!isSwimming(player)) {
            player.mes(
                "The eyes looking at you from below the surface make you reconsider going down there."
            )
            return
        }
        val dest = rock.translate(0, if (angleId == 0) 1 else -1)
        stopSwimming(player)
        deps.launcher.launchLenient(player) { telejump(dest, TeleportType.Exempt) }
        player.mes("You use the steps to get yourself back onto the island.")
    }

    fun spawnCrocodiles() {
        removeCrocodiles()
        for (tile in ZebakCoords.WATER_CROCS) {
            crocodiles += room.spawn(ZebakNpcs.WATER_CROC, room.coords(tile))
        }
    }

    fun removeCrocodiles() {
        for (croc in crocodiles) room.despawn(croc)
        crocodiles.clear()
        biteCountdown.clear()
    }

    /** Once a tick (config `timer = 1`): chase the nearest swimmer within 16; bite 0-3 per 2 ticks. */
    fun crocodileTick(croc: Npc) {
        if (room.stage != ToaStage.STARTED || (room.zebak?.hitpoints ?: 0) <= 0) return
        val target =
            room.targets()
                .filter { it in swimmers }
                .map { it to chebyshev(it.coords, croc.coords) }
                .filter { it.second < HUNT_RANGE }
                .minByOrNull { it.second }
                ?.first
        if (target != null && !croc.isBeside(target)) croc.walk(target.coords)

        val countdown = (biteCountdown[croc] ?: BITE_RATE) - 1
        if (countdown > 0) {
            biteCountdown[croc] = countdown
            return
        }
        biteCountdown[croc] = BITE_RATE
        if (target != null && croc.isBeside(target)) {
            croc.facePlayer(target)
            val damage = deps.random.of(0, MAX_BITE)
            target.queueHit(croc, 1, HitType.Typeless, damage, NoopPlayerHitModifier)
        }
    }

    fun clear() {
        for (player in swimmers.toList()) stopSwimming(player)
        biteCountdown.clear()
    }

    private companion object {
        const val HUNT_RANGE = 16
        const val BITE_RATE = 2
        const val MAX_BITE = 3
    }
}
