package org.rsmod.content.bosses.barrows

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlin.random.Random
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.npc.owner.assignSpawnOwner
import org.rsmod.api.npc.owner.clearSpawnOwner
import org.rsmod.api.npc.owner.ownedBy
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.LineValidator
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

@Singleton
class BarrowsNpcSpawner
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val npcList: NpcList,
    private val collision: CollisionFlagMap,
    private val mapClock: MapClock,
    private val aiPlayerInteractions: AiPlayerInteractions,
) {
    private val lineValidator = LineValidator(collision)

    fun owned(player: Player): Sequence<Npc> = npcList.ownedBy(player).filter { it.isBarrowsNpc() }

    fun currentBrother(player: Player): Npc? =
        owned(player).firstOrNull { it.isBrother() && it.hitpoints > 0 }

    internal fun spawnBrother(player: Player, brother: BarrowsBrother, shout: String?) {
        val npc = spawn(player, brother.npc)
        if (shout != null) npc.say(shout)
        player.hintArrow(npc)
        when (brother) {
            BarrowsBrother.AHRIM,
            BarrowsBrother.KARIL -> npc.apPlayer2(player, aiPlayerInteractions)
            else -> npc.opPlayer2(player, aiPlayerInteractions)
        }
    }

    fun spawnMonster(player: Player, type: String) {
        spawn(player, type).opPlayer2(player, aiPlayerInteractions)
    }

    fun despawn(player: Player, npc: Npc) {
        if (npc.isBrother()) player.clearHintArrow()
        npc.clearSpawnOwner()
        npcRepo.del(npc, Int.MAX_VALUE)
    }

    /** Npcs mid-death would lose their kill credit when deleted, so it is granted here instead. */
    fun despawnAll(player: Player) {
        player.clearHintArrow()
        for (npc in owned(player).toList()) {
            if (npc.hitpoints <= 0) player.creditBarrowsKill(npc)
            npc.clearSpawnOwner()
            npcRepo.del(npc, Int.MAX_VALUE)
        }
    }

    private fun spawn(player: Player, type: String): Npc {
        val npc = Npc(type, spawnTile(player.coords))
        npc.respawns = false
        npcRepo.add(npc, LIFETIME_TICKS)
        npc.assignSpawnOwner(player, mapClock.cycle)
        return npc
    }

    private fun spawnTile(near: CoordGrid): CoordGrid {
        repeat(SPAWN_ATTEMPTS) {
            val dx = Random.nextInt(-SPAWN_RADIUS, SPAWN_RADIUS + 1)
            val dz = Random.nextInt(-SPAWN_RADIUS, SPAWN_RADIUS + 1)
            val candidate = near.translate(dx, dz)
            if (candidate != near && isWalkable(candidate) && canWalk(near, candidate)) {
                return candidate
            }
        }
        return near
    }

    fun walkableNear(centre: CoordGrid): CoordGrid {
        if (isWalkable(centre)) return centre
        for (radius in 1..SPAWN_RADIUS) {
            for (dx in -radius..radius) {
                for (dz in -radius..radius) {
                    val candidate = centre.translate(dx, dz)
                    if (isWalkable(candidate) && canWalk(centre, candidate)) return candidate
                }
            }
        }
        return centre
    }

    fun randomWalkableNear(centre: CoordGrid, radius: Int): CoordGrid {
        repeat(SPAWN_ATTEMPTS) {
            val candidate =
                centre.translate(
                    Random.nextInt(-radius, radius + 1),
                    Random.nextInt(-radius, radius + 1),
                )
            if (isWalkable(candidate) && canWalk(centre, candidate)) return candidate
        }
        return walkableNear(centre)
    }

    private fun isWalkable(coords: CoordGrid): Boolean =
        collision[coords.x, coords.z, coords.level] and BLOCKED_FLAGS == 0

    private fun canWalk(from: CoordGrid, to: CoordGrid): Boolean =
        lineValidator.hasLineOfWalk(from.level, from.x, from.z, to.x, to.z)

    private companion object {
        const val LIFETIME_TICKS = 500
        const val SPAWN_RADIUS = 3
        const val SPAWN_ATTEMPTS = 100
        const val BLOCKED_FLAGS =
            CollisionFlag.LOC or
                CollisionFlag.BLOCK_WALK or
                CollisionFlag.GROUND_DECOR or
                CollisionFlag.BLOCK_PLAYERS
    }
}
