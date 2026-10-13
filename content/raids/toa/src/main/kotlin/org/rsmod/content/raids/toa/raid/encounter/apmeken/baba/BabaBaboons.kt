package org.rsmod.content.raids.toa.raid.encounter.apmeken.baba

import dev.openrune.ServerCacheManager
import org.rsmod.annotations.InternalApi
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.stat.stat
import org.rsmod.content.raids.toa.party.ToaInvocationKey
import org.rsmod.content.raids.toa.raid.encounter.spotanim
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid

internal class BabaBaboons(private val room: BabaEncounter) {
    private val deps = room.raid.deps
    private val sarcophagi = ArrayList<Sarcophagus>()
    private val baboons = HashSet<Npc>()
    private val peels = HashMap<CoordGrid, LocInfo>()

    private class Sarcophagus(val npc: Npc, val tile: CoordGrid, val south: Boolean) {
        var openedAt = -1
        val open: Boolean
            get() = openedAt >= 0
    }

    val alive: Int
        get() = baboons.count { it.isSlotAssigned && it.hitpoints > 0 }

    fun coverNear(tile: CoordGrid): Npc? {
        val alive = baboons.filter { it.isSlotAssigned && it.hitpoints > 0 }
        val closed = sarcophagi.filter { !it.open && it.npc.isSlotAssigned }.map { it.npc }
        return (alive + closed).firstOrNull { dist(it.coords, tile) <= 1 }
    }

    fun spawnSarcophagi() {
        clearSarcophagi()
        val level = BabaCoords.BABA.level
        for (column in 0 until BabaApes.SARC_COLUMNS) {
            val x = BabaApes.SARC_XS_FIRST + column * BabaApes.SARC_X_STEP
            for (south in listOf(true, false)) {
                val z = if (south) BabaApes.SOUTH_Z else BabaApes.NORTH_Z
                val tile = room.coords(CoordGrid(x, z, level))
                val npc = room.spawn(BabaNpcs.SARCOPHAGUS, tile)
                npc.ignoreCombatInteractions = true
                npc.baseHitpointsLvl = BabaApes.SARCOPHAGUS_HP
                npc.hitpoints = BabaApes.SARCOPHAGUS_HP
                sarcophagi += Sarcophagus(npc, tile, south)
            }
        }
    }

    fun discharge(boss: Npc) {
        if (alive > 0) return
        boss.anim(BabaSeqs.DISCHARGE)
        val count = (room.teamSize + 1) / 2
        val tiles =
            room
                .freeTiles(
                    CoordGrid(BabaApes.SPAWN_MIN_X, BabaApes.SPAWN_MIN_Z, BabaCoords.BABA.level),
                    CoordGrid(BabaApes.SPAWN_MAX_X, BabaApes.SPAWN_MAX_Z, BabaCoords.BABA.level),
                    emptyList(),
                )
                .filter { !overlapsBoss(boss, it) }
        for (tile in tiles.take(count)) {
            val npc = room.spawn(BabaNpcs.BABOON, tile, Direction.South)
            npc.baseHitpointsLvl = BabaApes.BABOON_HP
            npc.hitpoints = BabaApes.BABOON_HP
            baboons += npc
        }
    }

    fun tick() {
        val now = deps.mapClock.cycle
        val players = room.targets()
        for (npc in baboons.toList()) {
            if (!npc.isSlotAssigned) {
                baboons.remove(npc)
                continue
            }
            act(npc, now, players)
        }
        for (sarc in sarcophagi) if (sarc.open) volley(sarc, now, players)
        stepPeels(players)
    }

    fun killed(npc: Npc) {
        if (!baboons.remove(npc)) return
        if (room.raid.isActive(ToaInvocationKey.JungleJapes)) dropPeels(npc.coords)
        room.schedule(1) { if (npc.isSlotAssigned) room.despawn(npc) }
    }

    fun crush(boulder: Npc) {
        val b = room.staticCoords(boulder.coords)
        for (npc in baboons.toList()) {
            if (!npc.isSlotAssigned) continue
            val p = room.staticCoords(npc.coords)
            if (p.x in b.x..b.x + 2 && p.z in b.z..b.z + 2) {
                npc.hitpoints = 0
                killed(npc)
            }
        }
    }

    fun clear() {
        for (npc in baboons.toList()) if (npc.isSlotAssigned) room.despawn(npc)
        baboons.clear()
        for ((tile, loc) in peels) deps.locRepo.del(loc, Int.MAX_VALUE)
        peels.clear()
    }

    fun clearSarcophagi() {
        for (sarc in sarcophagi) {
            if (sarc.open) swap(sarc.tile, BabaLocs.COFFIN_OPEN, BabaLocs.COFFIN_CLOSED)
            if (sarc.npc.isSlotAssigned) room.despawn(sarc.npc)
        }
        sarcophagi.clear()
    }

    private fun act(npc: Npc, now: Int, players: List<Player>) {
        val target = sarcophagi.filter { !it.open }.minByOrNull { dist(npc.coords, it.tile) }
        if (target != null) {
            if (dist(npc.coords, target.tile) > 1) {
                npc.walk(target.tile)
                return
            }
            npc.anim(BabaSeqs.BABOON_MELEE)
            target.npc.hitpoints -= BabaApes.SARC_HIT
            if (target.npc.hitpoints <= 0) open(target, now)
            return
        }
        val victim = players.minByOrNull { dist(npc.coords, it.coords) } ?: return
        npc.anim(BabaSeqs.BABOON_RANGED)
        room.hazardDamage(victim, BabaApes.RANGED_DAMAGE, 0)
    }

    private fun open(sarc: Sarcophagus, now: Int) {
        sarc.openedAt = now
        swap(sarc.tile, BabaLocs.COFFIN_CLOSED, BabaLocs.COFFIN_OPEN)
        if (sarc.npc.isSlotAssigned) room.despawn(sarc.npc)
    }

    private fun volley(sarc: Sarcophagus, now: Int, players: List<Player>) {
        if ((now - sarc.openedAt) % BabaApes.ENERGY_INTERVAL != 0) return
        val dz = if (sarc.south) 1 else -1
        val half = room.pathTier
        val gfx = spotanim(BabaSpots.ENERGY)
        for (step in 0 until BabaApes.ENERGY_LENGTH) {
            for (dx in -half..half) {
                val tile = sarc.tile.translate(dx, dz * (BabaApes.ENERGY_START + step))
                deps.worldRepo.spotanimMap(gfx, tile, delay = step * BabaApes.ENERGY_STEP_CYCLES)
                for (player in players) if (player.coords == tile) energyHit(player)
            }
        }
    }

    private fun energyHit(player: Player) {
        var base = BabaApes.ENERGY_DAMAGE
        if (room.raid.isActive(ToaInvocationKey.GottaHaveFaith)) {
            base += player.stat(BabaApes.PRAYER_STAT) / BabaApes.FAITH_PRAYER_DIVISOR
        }
        room.hazardDamage(player, base, BabaApes.ENERGY_SPREAD)
    }

    private fun dropPeels(tile: CoordGrid) {
        for (peelTile in listOf(tile, tile.translate(1, 0))) {
            if (peelTile in peels) continue
            peels[peelTile] =
                deps.locRepo.add(
                    peelTile,
                    BabaLocs.BANANA_PEEL,
                    Int.MAX_VALUE,
                    LocAngle.West,
                    LocShape.CentrepieceStraight,
                )
        }
    }

    @OptIn(InternalApi::class)
    private fun stepPeels(players: List<Player>) {
        if (peels.isEmpty()) return
        for (player in players) {
            val loc = peels.remove(player.coords) ?: continue
            deps.locRepo.del(loc, Int.MAX_VALUE)
            player.anim(BabaSeqs.KNOCKDOWN)
            room.hazardDamage(player, BabaApes.PEEL_DAMAGE, 0)
            deps.launcher.launchLenient(player) { delay(BabaRolling.PEEL_STUN_TICKS) }
        }
    }

    private fun swap(tile: CoordGrid, from: String, into: String) {
        val fromId = from.asRSCM(RSCMType.LOC)
        val loc = deps.locRepo.findAll(tile).firstOrNull { it.id == fromId } ?: return
        val type = ServerCacheManager.getObject(into.asRSCM(RSCMType.LOC)) ?: return
        deps.locRepo.change(loc, type, Int.MAX_VALUE)
    }

    private fun overlapsBoss(boss: Npc, tile: CoordGrid): Boolean {
        val dx = tile.x - boss.coords.x
        val dz = tile.z - boss.coords.z
        return dx in -1..BabaShock.BOSS_SIZE && dz in -1..BabaShock.BOSS_SIZE
    }

    private fun dist(a: CoordGrid, b: CoordGrid): Int = a.chebyshevDistance(b)
}
