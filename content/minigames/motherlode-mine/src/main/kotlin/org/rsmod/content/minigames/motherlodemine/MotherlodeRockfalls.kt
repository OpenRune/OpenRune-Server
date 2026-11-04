package org.rsmod.content.minigames.motherlodemine

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.npc.clearInteractionRoute
import org.rsmod.api.player.clearInteractionRoute
import org.rsmod.api.player.hit.modifier.PlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.route.StepFactory
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeConstants
import org.rsmod.content.minigames.motherlodemine.scripts.MotherlodeMinerScript
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.map.Direction
import org.rsmod.game.movement.MoveSpeed
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.flag.CollisionFlag

@Singleton
class MotherlodeRockfalls @Inject constructor(
    private val clock: MapClock,
    private val locs: LocRepository,
    private val world: WorldRepository,
    private val players: PlayerList,
    private val hitModifier: PlayerHitModifier,
    private val npcs: NpcList,
    private val steps: StepFactory,
    private val events: EventBus,
) {
    private val respawns = mutableListOf<Respawn>()
    private val minerIds by lazy { MotherlodeMinerScript.Miners.map { it.asRSCM() }.toSet() }

    fun clear(rockfall: BoundLocInfo): Boolean {
        if (locs.findExact(rockfall.coords, rockfall.shape)?.id != rockfall.id) return false
        if (!locs.del(rockfall, Int.MAX_VALUE)) return false
        respawns += Respawn(rockfall, clock.cycle + MotherlodeConstants.ROCKFALL_RESPAWN_TICKS)
        return true
    }

    fun tick() {
        if (respawns.isEmpty()) return
        val reservedSteps = respawns.mapTo(mutableSetOf()) { it.loc.coords }
        val iterator = respawns.iterator()
        while (iterator.hasNext()) {
            val respawn = iterator.next()
            if (!respawn.falling && clock.cycle >= respawn.cycle - 1) {
                respawn.falling = true
                fall(respawn.loc.coords)
                crushOccupants(respawn.loc.coords, reservedSteps)
            }
            if (clock.cycle < respawn.cycle) continue
            iterator.remove()
            val rockfall = respawn.loc
            val loc = LocInfo(rockfall.layer, rockfall.coords, rockfall.entity)
            locs.add(loc, Int.MAX_VALUE)
        }
    }

    private fun crushOccupants(coords: CoordGrid, reservedSteps: MutableSet<CoordGrid>) {
        for (player in players) {
            if (!player.canProcess || player.coords != coords) continue
            val damage =
                player.hitpoints * MotherlodeConstants.ROCKFALL_DAMAGE_PERCENT / 100 +
                    MotherlodeConstants.ROCKFALL_DAMAGE_FLAT
            player.queueHit(delay = 1, type = HitType.Typeless, damage = damage, modifier = hitModifier)
            stepAside(player, reservedSteps)
        }
        for (npc in npcs) {
            if (npc.isVisible && npc.coords == coords && npc.id in minerIds) {
                stepAside(npc, reservedSteps)
            }
        }
    }

    private fun stepAside(entity: PathingEntity, reserved: MutableSet<CoordGrid>) {
        val destination = (Direction.CARDINAL + Direction.ORDINAL).firstNotNullOfOrNull { direction ->
            val tile = entity.coords.translate(direction.xOff, direction.zOff)
            if (tile in reserved) return@firstNotNullOfOrNull null
            val validated = when (entity) {
                is Player -> steps.validated(entity.coords, tile, entity.size, CollisionFlag.BLOCK_PLAYERS)
                is Npc -> entity.collisionStrategy?.let { steps.validated(entity, entity.coords, tile, it) }
            }
            tile.takeIf { validated == tile }
        } ?: return
        reserved += destination
        if (entity is Player) entity.ifClose(events)
        entity.cancelActiveCoroutine()
        entity.resetAnim()
        entity.resetPendingFaceSquare()
        when (entity) {
            is Player -> {
                entity.clearInteractionRoute()
                entity.resetFaceEntity()
                entity.walk(destination)
                // LateCycle has advanced the clock; protect the next input until normal movement.
                entity.delay = maxOf(entity.delay, clock.cycle)
            }
            is Npc -> {
                entity.clearInteractionRoute()
                entity.resetFaceEntity()
                entity.walk(destination)
                // Skip one mining decision without changing the recurring AI timer.
                entity.aiTimer = maxOf(entity.aiTimer, 2)
            }
        }
        entity.moveSpeed = MoveSpeed.Walk
        entity.tempMoveSpeed = MoveSpeed.Walk
    }

    private fun fall(coords: CoordGrid) {
        val projectile = ProjAnim(
            spotanim = "spotanim.motherlode_rockfall".asRSCM(),
            startHeight = 1000,
            endHeight = 25,
            startTime = 0,
            endTime = 30,
            angle = 0,
            progress = 0,
            sourceIndex = 0,
            targetIndex = 0,
            startCoord = coords.translate(-1, -2),
            endCoord = coords,
        )
        world.projAnim(projectile)
        world.projAnim(projectile.copy(startHeight = 900, startTime = 10, startCoord = coords.translate(1, 1)))
        world.spotanimMap(SpotanimType("spotanim.castlewars_catapult_splash".asRSCM()), coords, delay = 30)
        world.soundArea(coords, "synth.chinchompa_explode", radius = 2, delay = 30)
    }

    private class Respawn(val loc: BoundLocInfo, val cycle: Int, var falling: Boolean = false)
}
