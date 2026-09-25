package org.rsmod.content.other.pets

import dev.openrune.types.NpcMode
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.npc.owner.assignSpawnOwner
import org.rsmod.api.npc.owner.clearSpawnOwner
import org.rsmod.api.npc.owner.isSpawnOwnedBy
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.content.other.pets.cats.Cats
import org.rsmod.content.other.pets.dogs.Dogs
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcUid
import org.rsmod.game.movement.MoveSpeed
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

@Singleton
class PetFollowers
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val npcList: NpcList,
    private val mapClock: MapClock,
    private val collision: CollisionFlagMap,
) {
    fun hasFollower(player: Player): Boolean = player.followerObj != 0

    fun followerForm(player: Player): PetForm? =
        Pets.formForObj(player.followerObj)
            ?: Cats.formForObj(player.followerObj)
            ?: Dogs.formForObj(player.followerObj)

    fun follower(player: Player): Npc? {
        val packed = player.followerNpc
        if (packed == NO_FOLLOWER) {
            return null
        }
        val npc = NpcUid(packed).resolve(npcList)
        if (npc == null || !npc.isSpawnOwnedBy(player)) {
            player.followerNpc = NO_FOLLOWER
            return null
        }
        return npc
    }

    fun isFollowerOf(npc: Npc, player: Player): Boolean = follower(player) === npc

    /** Guards an op against another player's follower, sending the refusal message when it is. */
    fun requireOwned(access: ProtectedAccess, npc: Npc): Boolean {
        if (isFollowerOf(npc, access.player)) {
            return true
        }
        access.mes("That's not your pet.")
        return false
    }

    fun spawn(player: Player, form: PetForm) {
        val previous = follower(player)
        val coords = previous?.coords ?: besideTile(player)
        val facedNpc = previous?.facingTarget(npcList)
        despawn(player)
        val npc = Npc(form.npc, coords)
        npc.mode = NpcMode.None
        npc.defaultMoveSpeed = if (form.runs) MoveSpeed.Run else MoveSpeed.Walk
        npcRepo.add(npc, Int.MAX_VALUE)
        npc.respawns = false
        npc.assignSpawnOwner(player, mapClock.cycle)
        // Metamorphosis replaces the npc, so the new one takes over the old one's tile and facing
        // rather than reappearing beside the owner pointing somewhere else.
        if (facedNpc != null) {
            npc.faceNpc(facedNpc)
        } else {
            npc.facePlayer(player)
        }
        // Replacing the npc drops whatever the owner was facing, so hand the face target over to
        // the new one; otherwise metamorphosing mid-interaction snaps the owner back to a default
        // angle.
        if (previous != null && player.isFacingNpc(previous)) {
            player.faceNpc(npc)
        }
        player.followerNpc = npc.uid.packed
        player.followerObj = form.objId
    }

    private fun Player.isFacingNpc(npc: Npc): Boolean = faceEntity.isNpc && faceEntity.npcSlot == npc.slotId

    fun dismiss(player: Player): PetForm? {
        val form = followerForm(player)
        despawn(player)
        player.followerObj = 0
        return form
    }

    fun call(player: Player): Boolean {
        val npc = follower(player) ?: return false
        npc.teleport(collision, besideTile(player))
        npc.facePlayer(player)
        return true
    }

    fun onLogout(player: Player) {
        despawn(player)
    }

    fun onPostTick(player: Player) {
        if (player.loggingOut || player.followerObj == 0) {
            return
        }
        val npc = follower(player)
        if (npc != null) {
            maintain(player, npc)
            return
        }
        val form = followerForm(player)
        if (form == null) {
            player.followerObj = 0
            return
        }
        spawn(player, form)
    }

    /** Parks the pet so a script can walk it somewhere; cleared once that route ends. */
    fun markBusy(player: Player) {
        player.followerBusy = true
    }

    fun clearBusy(player: Player) {
        player.followerBusy = false
    }

    /**
     * Pets are driven here rather than through [NpcMode.PlayerFollow]. That mode paths a pet the way
     * a monster approaches its target, which lets it cut corners, shuffle in place once adjacent,
     * and teleport onto the owner's own tile. The mode is pinned to [NpcMode.None] every cycle
     * because anything that resets it would otherwise fall back to the npc type's default of
     * wandering home to its spawn tile.
     */
    private fun maintain(player: Player, npc: Npc) {
        npc.mode = NpcMode.None
        if (player.followerBusy) {
            // Leave the face target alone: the script that parked the pet owns what it looks at.
            releaseWhenRouteEnds(player, npc)
            return
        }
        npc.facePlayer(player)
        follow(player, npc)
        unstick(player, npc)
    }

    private fun releaseWhenRouteEnds(player: Player, npc: Npc) {
        if (npc.routeDestination.isEmpty() && !npc.hasMovedThisCycle) {
            player.followerBusy = false
        }
    }

    /**
     * Trails the owner by heading for the tile they just vacated, which is what produces the natural
     * following look, instead of repeatedly re-pathing onto whichever tile happens to be adjacent.
     */
    private fun follow(player: Player, npc: Npc) {
        val sameLevel = npc.coords.level == player.coords.level
        val distance = if (sameLevel) npc.coords.chebyshevDistance(player.coords) else Int.MAX_VALUE
        if (distance > TELEPORT_DISTANCE) {
            warpBeside(player, npc)
            return
        }
        val settled = distance == 1 && !player.hasMovedThisCycle
        if (settled) {
            return
        }
        val trail = player.previousCoords
        val onOwnerTile = trail == player.coords || trail.level != player.coords.level
        val destination = if (onOwnerTile) besideTile(player) else trail
        if (destination == npc.coords || npc.routeDestination.lastOrNull() == destination) {
            return
        }
        npc.walk(destination)
    }

    private fun warpBeside(player: Player, npc: Npc) {
        npc.abortRoute()
        npc.teleport(collision, besideTile(player))
        npc.facePlayer(player)
        player.followerStuck = 0
    }

    /** Nearest free tile next to [player], falling back to their own tile when boxed in. */
    private fun besideTile(player: Player): CoordGrid {
        val centre = player.coords
        for ((x, z) in ADJACENT_OFFSETS) {
            val candidate = centre.translate(x, z)
            if (collision[candidate.x, candidate.z, candidate.level] and BLOCKED_FLAGS == 0) {
                return candidate
            }
        }
        return centre
    }

    private fun unstick(player: Player, npc: Npc) {
        val adjacent = npc.coords.chebyshevDistance(player.coords) <= 1
        if (adjacent || npc.hasMovedThisCycle) {
            player.followerStuck = 0
            return
        }
        val stuck = player.followerStuck + 1
        if (stuck < STUCK_TELEPORT_CYCLES) {
            player.followerStuck = stuck
            return
        }
        player.followerStuck = 0
        npc.teleport(collision, besideTile(player))
        npc.facePlayer(player)
    }

    private fun despawn(player: Player) {
        player.followerStuck = 0
        player.followerBusy = false
        val npc = follower(player) ?: return
        player.followerNpc = NO_FOLLOWER
        npc.clearSpawnOwner()
        npcRepo.del(npc, Int.MAX_VALUE)
    }

    private companion object {
        const val STUCK_TELEPORT_CYCLES = 5
        const val TELEPORT_DISTANCE = 12
        const val NO_FOLLOWER = 0
        const val BLOCKED_FLAGS = CollisionFlag.BLOCK_WALK or CollisionFlag.BLOCK_PLAYERS

        val ADJACENT_OFFSETS =
            listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1, -1 to -1, 1 to -1, -1 to 1, 1 to 1)
    }
}

/**
 * Official varp the client reads to render follower-only right-click options. Holds the npc uid,
 * which packs as `(type shl 16) or slot` — exactly what the client expects.
 */
private var Player.followerNpc: Int by intVarp("varp.follower_npc")
private var Player.followerStuck: Int by intVarBit("varbit.pet_follower_stuck")
private var Player.followerBusy: Boolean by boolVarBit("varbit.pet_follower_busy")
