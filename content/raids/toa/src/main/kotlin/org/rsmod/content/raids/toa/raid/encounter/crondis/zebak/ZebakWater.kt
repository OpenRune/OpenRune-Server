package org.rsmod.content.raids.toa.raid.encounter.crondis.zebak

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.BasType
import org.rsmod.annotations.InternalApi
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.mes
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionPlayer
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.map.CoordGrid

internal class ZebakWater(private val room: ZebakEncounter) {
    private val deps = room.raid.deps
    private val swimmers = HashSet<Player>()
    private val crocodiles = ArrayList<Npc>()

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

    fun dropDeadSwimmers() {
        for (player in swimmers.toList()) {
            if (room.raid.isGhost(player) || room.raid.isDying(player)) stopSwimming(player)
        }
    }

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
    }

    fun crocodileTick(croc: Npc) {
        val prey = if (hunting()) prey(croc) else null
        val current = (croc.interaction as? InteractionPlayer)?.target
        if (prey == null) {
            if (current != null) croc.defaultMode()
            return
        }
        if (current !== prey) croc.opPlayer2(prey, deps.aiInteractions)
    }

    fun mayBite(target: Player): Boolean = hunting() && target in swimmers

    private fun hunting(): Boolean =
        room.stage == ToaStage.STARTED && (room.zebak?.hitpoints ?: 0) > 0

    private fun prey(croc: Npc): Player? =
        room.targets()
            .filter { it in swimmers }
            .map { it to it.coords.chebyshevDistance(croc.coords) }
            .filter { it.second < HUNT_RANGE }
            .minByOrNull { it.second }
            ?.first

    fun clear() {
        for (player in swimmers.toList()) stopSwimming(player)
    }

    private companion object {
        const val HUNT_RANGE = 16
    }
}
