package org.rsmod.content.raids.toa.raid.encounter.apmeken.baba

import org.rsmod.annotations.InternalApi
import org.rsmod.api.config.Constants
import org.rsmod.api.player.clearInteractionRoute
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
    private val immuneUntil = HashMap<Player, Int>()

    private class Boulder(val npc: Npc, val spawnCycle: Int) {
        var disarmed = false
    }

    private val mindTheGap: Boolean
        get() = room.raid.isActive(ToaInvocationKey.MindTheGap)

    fun spawnRow(crackedLane: Int) {
        val hp = hitpoints()
        val now = deps.mapClock.cycle
        for (lane in 0 until BabaRolling.LANES) {
            val z = BabaRolling.LANE_BASE_Z + lane * BabaRolling.LANE_SIZE
            val tile = room.coords(CoordGrid(BabaRolling.ROW_X, z, BabaCoords.BABA.level))
            val type = if (lane == crackedLane) BabaNpcs.BOULDER_WEAK else BabaNpcs.BOULDER
            val npc = room.spawn(type, tile, Direction.West)
            npc.baseHitpointsLvl = hp
            npc.hitpoints = hp
            npc.anim(BabaSeqs.BOULDER_ROLL)
            npc.pendingExactMove = ROLL
            boulders += Boulder(npc, now)
        }
    }

    /**
     * A boulder first rolls the tick after it spawns. Each tick it flattens whatever is under its
     * footprint, and only then steps one tile west, so a player is struck by the tile the boulder
     * is leaving rather than the one it is entering.
     */
    fun tick() {
        val now = deps.mapClock.cycle
        val targets = room.targets()
        for (boulder in boulders.toList()) {
            val npc = boulder.npc
            if (!npc.isSlotAssigned) {
                boulders -= boulder
                continue
            }
            if (boulder.spawnCycle == now) continue
            flatten(boulder, targets, now)
            if (room.staticCoords(npc.coords).x <= BabaRolling.WALL_X) {
                boulders -= boulder
                room.despawn(npc)
                continue
            }
            npc.teleport(deps.collision, npc.coords.translate(-1, 0))
            npc.anim(BabaSeqs.BOULDER_ROLL)
            npc.pendingExactMove = ROLL
        }
    }

    fun disarm(npc: Npc) {
        boulders.firstOrNull { it.npc === npc }?.disarmed = true
    }

    fun died(npc: Npc) {
        boulders.removeIf { it.npc === npc }
        if (npc.isSlotAssigned) room.despawn(npc)
    }

    fun clear() {
        for (boulder in boulders) if (boulder.npc.isSlotAssigned) room.despawn(boulder.npc)
        boulders.clear()
        immuneUntil.clear()
    }

    @OptIn(InternalApi::class)
    fun flyback(player: Player) {
        val from = player.coords
        val static = room.staticCoords(from)
        player.clearInteractionRoute()
        player.anim(BabaSeqs.FLYBACK)
        player.spotanim(BabaSpots.STUNNED, 0, BabaRolling.FLYBACK_HEIGHT, 0)
        if (mindTheGap && inPitRows(static)) {
            room.schedule(BabaRolling.FLYBACK_CYCLES / CYCLES_PER_TICK + 1) { dropInPit(player) }
        }
        if (static.x == BabaRolling.WALL_X) return
        val dest = room.coords(CoordGrid(BabaRolling.WALL_X, static.z, static.level))
        deps.launcher.launchLenient(player) {
            exactMove(
                from,
                dest,
                0,
                BabaRolling.FLYBACK_CYCLES,
                Constants.em_face_east,
                TeleportType.Exempt,
            )
            delay(BabaRolling.FLYBACK_CYCLES / CYCLES_PER_TICK)
        }
    }

    private fun flatten(boulder: Boulder, targets: List<Player>, now: Int) {
        val npc = boulder.npc
        room.baboons.crush(npc)
        room.rockfall.crush(npc)
        if (boulder.disarmed) return
        for (player in targets) {
            if (now <= (immuneUntil[player] ?: -1) || !overlaps(npc, player)) continue
            immuneUntil[player] = now + BabaRolling.KNOCKBACK_IMMUNITY
            knockback(player)
        }
    }

    @OptIn(InternalApi::class)
    private fun knockback(player: Player) {
        val static = room.staticCoords(player.coords)
        val landingX = static.x - BabaRolling.KNOCKBACK_TILES
        val destX = maxOf(BabaRolling.WALL_X, landingX)
        player.anim(BabaSeqs.KNOCKDOWN, KNOCKDOWN_DELAY)
        if (destX != static.x) {
            val dest = room.coords(CoordGrid(destX, static.z, static.level))
            deps.launcher.launchLenient(player) { telejump(dest, TeleportType.Exempt) }
        }
        if (mindTheGap && landingX < BabaRolling.WALL_X && inPitRows(static)) {
            dropInPit(player)
            return
        }
        room.schedule(1) {
            if (player !in room.targets()) return@schedule
            room.hazardDamage(
                player,
                BabaRolling.BOULDER_BASE_DAMAGE,
                BabaRolling.BOULDER_DAMAGE_SPREAD,
            )
        }
    }

    private fun dropInPit(player: Player) {
        if (player.hitpoints > 0 && room.inChallengeArea(player)) {
            player.hitTypeless(player.hitpoints)
        }
    }

    private fun inPitRows(static: CoordGrid): Boolean =
        static.z in BabaApes.PIT_MIN_Z..BabaApes.PIT_MAX_Z

    private fun overlaps(boulder: Npc, player: Player): Boolean {
        val dx = player.coords.x - boulder.coords.x
        val dz = player.coords.z - boulder.coords.z
        return dx in 0 until BabaRolling.LANE_SIZE && dz in 0 until BabaRolling.LANE_SIZE
    }

    private fun hitpoints(): Int {
        val party = (room.teamSize - 1).coerceIn(0, BabaRolling.BOULDER_HP_BY_PARTY.lastIndex)
        val percent = BabaRolling.BOULDER_HP_PATH_PERCENT[room.pathTier]
        return BabaRolling.BOULDER_HP_BY_PARTY[party] * percent / 100
    }

    private companion object {
        const val KNOCKDOWN_DELAY = 20
        const val CYCLES_PER_TICK = 30

        val ROLL =
            EntityExactMove(
                deltaX1 = 1,
                deltaZ1 = 0,
                deltaX2 = 0,
                deltaZ2 = 0,
                clientDelay1 = 0,
                clientDelay2 = CYCLES_PER_TICK,
                direction = Constants.em_face_west,
            )
    }
}
