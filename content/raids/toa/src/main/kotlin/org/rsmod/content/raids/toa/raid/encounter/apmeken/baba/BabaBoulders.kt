package org.rsmod.content.raids.toa.raid.encounter.apmeken.baba

import org.rsmod.annotations.InternalApi
import org.rsmod.api.config.Constants
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.content.raids.toa.party.ToaInvocationKey
import org.rsmod.content.raids.toa.raid.encounter.hitTypeless
import org.rsmod.content.raids.toa.raid.encounter.spotanim
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.EntityExactMove
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid

internal class BabaBoulders(private val room: BabaEncounter) {
    private val deps = room.raid.deps
    private val boulders = ArrayList<Boulder>()

    private class Boulder(val npc: Npc, val weak: Boolean) {
        val struck = HashSet<Player>()
    }

    fun spawnRow(weakLane: Int) {
        for (lane in 0 until BabaRolling.LANES) {
            val z = BabaRolling.LANE_BASE_Z + lane * BabaRolling.LANE_SIZE
            val tile = room.coords(CoordGrid(BabaRolling.ROW_X, z, BabaCoords.BABA.level))
            val weak = lane == weakLane
            val npc =
                room.spawn(
                    if (weak) BabaNpcs.BOULDER_WEAK else BabaNpcs.BOULDER,
                    tile,
                    Direction.West,
                )
            npc.ignoreCombatInteractions = !weak
            npc.anim(BabaSeqs.BOULDER_ROLL)
            boulders += Boulder(npc, weak)
        }
    }

    fun tick() {
        val iterator = boulders.iterator()
        val targets = room.targets()
        while (iterator.hasNext()) {
            val boulder = iterator.next()
            val npc = boulder.npc
            if (!npc.isSlotAssigned) {
                iterator.remove()
                continue
            }
            val origin = npc.coords
            var gone = false
            repeat(speed) {
                if (gone) return@repeat
                val from = npc.coords
                val next = from.translate(-1, 0)
                npc.teleport(deps.collision, next)
                npc.pendingExactMove =
                    EntityExactMove(
                        deltaX1 = origin.x - next.x,
                        deltaZ1 = origin.z - next.z,
                        deltaX2 = 0,
                        deltaZ2 = 0,
                        clientDelay1 = 0,
                        clientDelay2 = CYCLES_PER_TICK,
                        direction = Constants.em_face_west,
                    )
                room.baboons.crush(npc)
                for (player in targets) {
                    if (player !in boulder.struck && overlaps(npc, player)) {
                        boulder.struck += player
                        knockback(player)
                    }
                }
                if (room.staticCoords(next).x < BabaRolling.WALL_X) {
                    room.despawn(npc)
                    iterator.remove()
                    gone = true
                }
            }
        }
    }

    private val speed: Int
        get() = if (room.raid.isActive(ToaInvocationKey.Boulderdash)) 2 else 1

    private fun pitCheck(player: Player, cycles: Int) {
        if (!room.raid.isActive(ToaInvocationKey.MindTheGap)) return
        room.schedule(cycles / CYCLES_PER_TICK + 1) {
            val static = room.staticCoords(player.coords)
            val inPit = static.z in BabaApes.PIT_MIN_Z..BabaApes.PIT_MAX_Z
            if (inPit && static.x <= BabaRolling.WALL_X && player.hitpoints > 0) {
                player.hitTypeless(player.hitpoints)
            }
        }
    }

    fun killed(npc: Npc) {
        boulders.removeIf { it.npc === npc }
        room.schedule(1) { if (npc.isSlotAssigned) room.despawn(npc) }
    }

    fun clear() {
        for (boulder in boulders) if (boulder.npc.isSlotAssigned) room.despawn(boulder.npc)
        boulders.clear()
    }

    fun isWeak(npc: Npc): Boolean = boulders.any { it.npc === npc && it.weak }

    private fun overlaps(boulder: Npc, player: Player): Boolean {
        val b = room.staticCoords(boulder.coords)
        val p = room.staticCoords(player.coords)
        return p.x in b.x..b.x + BabaRolling.LANE_SIZE - 1 &&
            p.z in b.z..b.z + BabaRolling.LANE_SIZE - 1
    }

    @OptIn(InternalApi::class)
    private fun knockback(player: Player) {
        val static = room.staticCoords(player.coords)
        val destX = maxOf(BabaRolling.WALL_X, static.x - BabaRolling.FLYBACK_DISTANCE)
        player.anim(BabaSeqs.KNOCKDOWN, KNOCK_ANIM_DELAY)
        if (destX != static.x) {
            val dest = room.coords(CoordGrid(destX, static.z, static.level))
            deps.launcher.launchLenient(player) { telejump(dest, TeleportType.Exempt) }
        }
        pitCheck(player, 0)
        room.schedule(1) {
            room.hazardDamage(
                player,
                BabaRolling.BOULDER_BASE_DAMAGE,
                BabaRolling.BOULDER_DAMAGE_SPREAD,
            )
        }
    }

    @OptIn(InternalApi::class)
    fun flyback(player: Player) {
        val from = player.coords
        val static = room.staticCoords(from)
        player.anim(BabaSeqs.FLYBACK)
        player.spotanim(BabaSpots.STUNNED, 0, BabaRolling.FLYBACK_HEIGHT, 0)
        pitCheck(player, BabaRolling.FLYBACK_CYCLES)
        if (static.x == BabaRolling.WALL_X) return
        val dest = room.coords(CoordGrid(BabaRolling.WALL_X, static.z, static.level))
        deps.launcher.launchLenient(player) {
            exactMove(
                from,
                dest,
                0,
                BabaRolling.FLYBACK_CYCLES,
                Constants.em_face_west,
                TeleportType.Exempt,
            )
        }
    }

    private companion object {
        const val KNOCK_ANIM_DELAY = 20
        const val CYCLES_PER_TICK = 30
    }
}
