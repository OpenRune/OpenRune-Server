package org.rsmod.content.raids.toa.raid.encounter.scabaras.kephri

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign
import org.rsmod.annotations.InternalApi
import org.rsmod.api.bosses.runtime.clearOwnedLocs
import org.rsmod.api.bosses.runtime.removeLocs
import org.rsmod.api.bosses.runtime.spawnOwnedLoc
import org.rsmod.api.config.Constants
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.content.raids.toa.party.ToaInvocationKey
import org.rsmod.content.raids.toa.raid.encounter.hitTypeless
import org.rsmod.content.raids.toa.raid.encounter.spotanim
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

internal class KephriDung(private val room: KephriEncounter) {
    private val deps = room.raid.deps
    private var cursor = 0

    var targets: List<Player> = emptyList()
        private set

    var pending = false
        private set

    fun pick() {
        val players = room.targets()
        val count =
            if (room.raid.isActive(ToaInvocationKey.BlowingMud)) minOf(2, players.size) else 1
        targets =
            if (players.isEmpty()) {
                emptyList()
            } else {
                (0 until count).map { players[(cursor + it) % players.size] }
            }
        if (players.isNotEmpty()) cursor = (cursor + count) % players.size
        pending = targets.isNotEmpty()
    }

    fun cancel() {
        pending = false
    }

    fun hasPile(tile: CoordGrid): Boolean {
        val boss = room.kephri ?: return false
        return deps.bossDeps.encounterRegistry.of(boss).ownsLocAt(tile)
    }

    @OptIn(InternalApi::class)
    fun throwBack(boss: Npc, player: Player) {
        val centre = boss.coords.translate(KephriCoords.CENTRE_OFFSET, KephriCoords.CENTRE_OFFSET)
        val from = player.coords
        val dx = from.x - centre.x
        val dz = from.z - centre.z
        val adx = abs(dx)
        val adz = abs(dz)
        var sx = sign(dx.toDouble()).toInt()
        var sz = sign(dz.toDouble()).toInt()
        if (abs(adx - adz) > 1) {
            if (adx > adz) sz = 0 else sx = 0
        }
        if (sx == 0 && sz == 0) sz = -1

        var landing = from
        repeat(max(0, TRAVEL_RANGE - max(adx, adz))) {
            val next = landing.translate(sx, sz)
            if (!room.isOpenFloor(next) || !inArena(next)) return@repeat
            landing = next
        }

        player.anim(KephriSeqs.THROWN_PLAYER)
        player.spotanim(KephriSpots.THROWN_PLAYER, 0, THROWN_HEIGHT, 0)
        if (landing != from) {
            val flight = THROW_CYCLES_PER_TILE * from.chebyshevDistance(landing) + THROW_CYCLES_BASE
            deps.launcher.launchLenient(player) {
                exactMove(from, landing, THROW_HOLD_CYCLES, flight, Constants.em_face_west, TeleportType.Exempt)
            }
        }
        layLine(boss, from, landing, sx, sz)
    }

    private fun layLine(boss: Npc, start: CoordGrid, landing: CoordGrid, sx: Int, sz: Int) {
        val tiles = ArrayList<CoordGrid>()
        var tile = start
        while (tiles.size < MAX_LINE) {
            tiles += tile
            if (tile == landing) break
            tile = tile.translate(sx, sz)
        }
        for ((index, tile) in tiles.withIndex()) {
            val delay = DROP_GFX_START_CYCLES + DROP_GFX_STEP_CYCLES * index
            deps.worldRepo.spotanimMap(spotanim(KephriSpots.DUNG_DROP), tile, delay = delay)
            val ticks = if (index < FIRST_BATCH) PILE_DELAY else PILE_DELAY + 1
            room.schedule(ticks) {
                if (boss.isSlotAssigned && room.isOpenFloor(tile)) {
                    val angle = deps.random.of(0, MAX_ANGLE)
                    deps.bossDeps.spawnOwnedLoc(boss, tile, KephriLocs.DUNG, angle, false)
                }
            }
        }
    }

    fun tick() {
        if (room.kephri == null) return
        for (player in room.targets()) {
            if (!hasPile(player.coords)) continue
            player.say(HAZARD_SHOUT)
            player.hitTypeless(room.maxHit(HAZARD_DAMAGE).coerceAtLeast(1))
            shove(player)
        }
    }

    fun clear() {
        pending = false
        targets = emptyList()
        room.kephri?.let { if (it.isSlotAssigned) deps.bossDeps.clearOwnedLocs(it) }
    }

    fun replaceWithInvisible() {
        val boss = room.kephri ?: return
        val piles = deps.bossDeps.encounterRegistry.of(boss).releaseOwnedLocs()
        deps.bossDeps.removeLocs(piles)
        for (tile in piles.keys) {
            deps.locRepo.add(
                tile,
                KephriLocs.INVISIBLE,
                Int.MAX_VALUE,
                LocAngle.West,
                LocShape.CentrepieceStraight,
            )
        }
    }

    private fun inArena(tile: CoordGrid): Boolean =
        room.room.inChallengeArea(room.staticCoords(tile))

    @OptIn(InternalApi::class)
    private fun shove(player: Player) {
        val here = player.coords
        for (radius in 1..SHOVE_RADIUS) {
            val ring = ArrayList<CoordGrid>()
            for (x in -radius..radius) {
                for (z in -radius..radius) {
                    if (max(abs(x), abs(z)) != radius) continue
                    val tile = here.translate(x, z)
                    if (hasPile(tile) || !room.isOpenFloor(tile) || !inArena(tile)) continue
                    ring += tile
                }
            }
            if (ring.isEmpty()) continue
            val dest = ring[deps.random.of(0, ring.lastIndex)]
            deps.launcher.launchLenient(player) {
                exactMove(here, dest, 0, SHOVE_CYCLES, SHOVE_FACING, TeleportType.Exempt)
            }
            return
        }
    }

    private companion object {
        const val MAX_LINE = 12
        const val TRAVEL_RANGE = 8
        const val SHOVE_RADIUS = 10
        const val HAZARD_DAMAGE = 3
        const val HAZARD_SHOUT = "Ouch!"
        const val THROWN_HEIGHT = 124
        const val THROW_HOLD_CYCLES = 5
        const val THROW_CYCLES_BASE = 10
        const val THROW_CYCLES_PER_TILE = 10
        const val SHOVE_CYCLES = 29
        const val SHOVE_FACING = 511
        const val DROP_GFX_START_CYCLES = 60
        const val DROP_GFX_STEP_CYCLES = 10
        const val FIRST_BATCH = 3
        const val PILE_DELAY = 4
        const val MAX_ANGLE = 3
    }
}
