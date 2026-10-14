package org.rsmod.content.raids.toa.raid.encounter.apmeken.puzzle

import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statSub
import org.rsmod.content.raids.toa.raid.encounter.hitTypeless
import org.rsmod.content.raids.toa.raid.encounter.spotanim
import org.rsmod.content.raids.toa.raid.shuffled
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid

internal class ApmekenBaboons(private val room: ApmekenPuzzleEncounter) {
    private val deps = room.raid.deps
    private val live = ArrayList<Baboon>()
    private val pools = HashMap<CoordGrid, Pool>()
    private val carriers = ArrayList<Npc>()
    private var wave = 0

    private enum class Kind {
        BRAWLER,
        THROWER,
        MAGE,
        SHAMAN,
        VOLATILE,
        CURSED,
        THRALL,
    }

    private class Baboon(val npc: Npc, val kind: Kind, val large: Boolean, now: Int) {
        var activeAt = now + SPAWN_IDLE
        var target: Player? = null
        var owner: Baboon? = null
        var thralls = 0
        var thrallCap = 0
        var armedAt = -1
        var destination: CoordGrid? = null
        val trail = ArrayDeque<CoordGrid>()
        val dead: Boolean
            get() = !npc.isSlotAssigned || npc.hitpoints <= 0
    }

    private class Pool(val loc: LocInfo, val expiresAt: Int)

    private class Wave(val fixed: List<String>, val specials: Int = 0)

    val waveNumber: Int
        get() = wave

    fun clear() {
        for (baboon in live) if (baboon.npc.isSlotAssigned) room.despawn(baboon.npc)
        live.clear()
        for (carrier in carriers) if (carrier.isSlotAssigned) room.despawn(carrier)
        carriers.clear()
        clearPools()
        wave = 0
    }

    fun killAll() {
        for (baboon in live) if (!baboon.dead) baboon.npc.hitpoints = 0
    }

    fun skipTo(target: Int) {
        wave = (target - 1).coerceIn(0, WAVES.size - 1)
        spawnWave()
    }

    fun tick(now: Int, targets: List<Player>) {
        live.removeIf { !it.npc.isSlotAssigned }
        pollWaves(now)
        if (targets.isNotEmpty()) for (baboon in live.toList()) act(baboon, now, targets)
        tickPools(now, targets)
    }

    fun clearPools() {
        for (pool in pools.values) deps.locRepo.del(pool.loc, Int.MAX_VALUE)
        pools.clear()
    }

    private fun pollWaves(now: Int) {
        val elapsed = now - room.startCycle
        if (elapsed < FIRST_WAVE || elapsed % POLL != 0) return
        if (wave < WAVES.size) {
            val critical = live.count { it.kind != Kind.THRALL }
            val threshold = if (wave == WAVES.size - 1) LAST_THRESHOLD else THRESHOLD
            if (wave == 0 || critical <= threshold) spawnWave()
        } else if (live.isEmpty()) {
            room.complete()
        }
    }

    private fun spawnWave() {
        val now = deps.mapClock.cycle
        val definition = WAVES[wave++]
        val types = ArrayList(definition.fixed)
        repeat(definition.specials) {
            types += SPECIALS[deps.random.of(maxExclusive = SPECIALS.size)]
        }
        val tiles = deps.random.shuffled(ApmekenCoords.BABOON_SPAWNS)
        val targets = room.targets()
        for ((index, type) in types.withIndex()) {
            val tile = tiles[index % tiles.size]
            val npc = room.spawn(type, room.coords(tile))
            val baboon = Baboon(npc, kindOf(type), type in LARGE, now)
            npc.vars[FORCED_MAX_HIT] = WEAK_STYLES[baboon.kind] ?: 0
            if (baboon.kind == Kind.SHAMAN) {
                baboon.thrallCap = deps.random.of(THRALL_CAP_MIN, THRALL_CAP_MAX)
            }
            if (baboon.kind == Kind.CURSED) {
                npc.faceDirection(Direction.South)
            } else {
                pickTarget(targets)?.let {
                    baboon.target = it
                    npc.facePlayer(it)
                }
            }
            live += baboon
        }
    }

    private fun kindOf(type: String): Kind =
        when (type) {
            ApmekenNpcs.BRAWLER_SMALL,
            ApmekenNpcs.BRAWLER_LARGE -> Kind.BRAWLER
            ApmekenNpcs.THROWER_SMALL,
            ApmekenNpcs.THROWER_LARGE -> Kind.THROWER
            ApmekenNpcs.MAGE_SMALL,
            ApmekenNpcs.MAGE_LARGE -> Kind.MAGE
            ApmekenNpcs.SHAMAN -> Kind.SHAMAN
            ApmekenNpcs.VOLATILE -> Kind.VOLATILE
            ApmekenNpcs.CURSED -> Kind.CURSED
            else -> Kind.THRALL
        }

    private fun pickTarget(targets: List<Player>): Player? =
        if (targets.isEmpty()) null else targets[deps.random.of(maxExclusive = targets.size)]

    private fun pickTile(tiles: List<CoordGrid>): CoordGrid? =
        if (tiles.isEmpty()) null else tiles[deps.random.of(maxExclusive = tiles.size)]

    fun damageFactor(slot: Int): Double? {
        val baboon = live.firstOrNull { it.npc.slotId == slot } ?: return null
        return room.combatantOf(baboon.npc)?.damageFactor
    }

    fun onAttack(npc: Npc, target: Player) {
        val baboon = live.firstOrNull { it.npc === npc } ?: return
        if (baboon.kind == Kind.THRALL) drainPrayer(target, THRALL_DRAIN)
    }

    fun trySummon(npc: Npc, target: Player): Boolean {
        val baboon = live.firstOrNull { it.npc === npc } ?: return false
        if (baboon.kind != Kind.SHAMAN || baboon.thralls >= baboon.thrallCap) return false
        summonThrall(baboon)
        return true
    }

    fun onImpact(player: Player, hit: Hit) {
        if (!hit.isFromNpc) return
        val slot = hit.hitmark.npcSlot
        val baboon = live.firstOrNull { it.npc.slotId == slot } ?: return
        if (baboon.kind == Kind.SHAMAN) drainPrayer(player, hit.damage)
    }

    private fun act(baboon: Baboon, now: Int, targets: List<Player>) {
        if (baboon.dead) {
            baboon.owner?.let { it.thralls-- }
            baboon.owner = null
            return
        }
        when (baboon.kind) {
            Kind.CURSED -> if (now >= baboon.activeAt) wander(baboon, now)
            Kind.VOLATILE -> chase(baboon, now, targets)
            else -> {}
        }
    }

    private fun chase(baboon: Baboon, now: Int, targets: List<Player>) {
        if (now < baboon.activeAt) return
        var target = baboon.target
        if (target == null || target !in targets) {
            target = pickTarget(targets) ?: return
            baboon.target = target
        }
        val distance = baboon.npc.coords.chebyshevDistance(target.coords)
        volatile(baboon, target, distance, now)
    }

    private fun summonThrall(baboon: Baboon) {
        val npc = baboon.npc
        npc.anim(ApmekenSeqs.SUMMON)
        room.schedule(1) {
            if (baboon.dead) return@schedule
            val tiles = ArrayList<CoordGrid>()
            for (dx in -SUMMON_RADIUS..SUMMON_RADIUS) {
                for (dz in -SUMMON_RADIUS..SUMMON_RADIUS) {
                    val tile = npc.coords.translate(dx, dz)
                    if (tile != npc.coords && room.isOpenFloor(tile)) tiles += tile
                }
            }
            val tile = pickTile(tiles) ?: return@schedule
            val now = deps.mapClock.cycle
            val thrall = room.spawn(ApmekenNpcs.THRALL, tile)
            thrall.spotanim(ApmekenSpots.SUMMON)
            val entry = Baboon(thrall, Kind.THRALL, false, now)
            entry.activeAt = now
            entry.owner = baboon
            baboon.thralls++
            live += entry
        }
    }

    private fun drainPrayer(target: Player, amount: Int) {
        if (amount <= 0) return
        val drain = amount.coerceAtMost(target.stat(PRAYER_STAT))
        target.statSub(PRAYER_STAT, constant = drain, percent = 0)
    }

    private fun volatile(baboon: Baboon, target: Player, distance: Int, now: Int) {
        val npc = baboon.npc
        if (baboon.armedAt < 0) {
            if (distance > 1) return npc.walk(target.coords)
            baboon.armedAt = now
            npc.facePlayer(target)
            return
        }
        val since = now - baboon.armedAt
        if (since == EXPLODE_ANIM) npc.anim(ApmekenSeqs.EXPLODE)
        if (since >= EXPLODE_AT) explode(baboon)
    }

    private fun explode(baboon: Baboon) {
        val npc = baboon.npc
        val tile = npc.coords
        val health = npc.hitpoints.coerceAtLeast(1)
        val maxHealth = npc.baseHitpointsLvl.coerceAtLeast(1)
        live.remove(baboon)
        room.despawn(npc)
        for (other in live) {
            if (!other.dead && other.npc.coords.chebyshevDistance(tile) <= 1) {
                other.npc.hitpoints = 0
            }
        }
        val carrier = room.spawn(ApmekenNpcs.EXPLOSION, tile.translate(-1, -1))
        carrier.spotanim(ApmekenSpots.EXPLODE_CARRIER)
        carriers += carrier
        room.schedule(CARRIER_TICKS) {
            carriers.remove(carrier)
            if (carrier.isSlotAssigned) room.despawn(carrier)
        }
        deps.worldRepo.spotanimMap(spotanim(ApmekenSpots.EXPLODE_GROUND), tile, 0, 0)
        for (player in room.targets()) {
            if (player.coords.chebyshevDistance(tile) <= 1) {
                val roll = room.hazardRoll(EXPLOSION_DAMAGE, EXPLOSION_SPREAD)
                player.hitTypeless((roll * health + maxHealth - 1) / maxHealth)
            }
        }
    }

    private fun wander(baboon: Baboon, now: Int) {
        val npc = baboon.npc
        val here = npc.coords
        baboon.trail.addLast(here)
        if (baboon.trail.size > POOL_TRAIL) dropPool(baboon.trail.removeFirst(), now)
        if (now < baboon.activeAt + WANDER_DELAY) return
        val goal = baboon.destination
        if (goal == null || goal == here || deps.random.of(maxExclusive = PAUSE_ODDS) == 0) {
            baboon.destination = nextDestination(here)
        }
        baboon.destination?.let { npc.walk(it) }
    }

    private fun nextDestination(from: CoordGrid): CoordGrid? {
        val players = room.targets().map { it.coords }
        val tiles = ArrayList<CoordGrid>()
        for (dx in -WANDER_RANGE..WANDER_RANGE) {
            for (dz in -WANDER_RANGE..WANDER_RANGE) {
                val tile = from.translate(dx, dz)
                val static = room.staticCoords(tile)
                if (static == CoordGrid.NULL || !room.room.inChallengeArea(static)) continue
                if (static in ApmekenCoords.AVOID_TILES || tile in players) continue
                if (room.isOpenFloor(tile)) tiles += tile
            }
        }
        return pickTile(tiles)
    }

    private fun dropPool(tile: CoordGrid, now: Int) {
        val static = room.staticCoords(tile)
        if (static in ApmekenCoords.AVOID_TILES || tile in pools) return
        val angle = LocAngle.entries[deps.random.of(maxExclusive = LocAngle.entries.size)]
        val loc =
            deps.locRepo.add(
                tile,
                ApmekenLocs.VENOM_POOL,
                POOL_TICKS,
                angle,
                LocShape.CentrepieceStraight,
            )
        pools[tile] = Pool(loc, now + POOL_TICKS)
    }

    private fun tickPools(now: Int, targets: List<Player>) {
        pools.values.removeIf { it.expiresAt <= now }
        if (pools.isEmpty()) return
        for (player in targets) {
            if (player.coords !in pools) continue
            player.soundSynth(ApmekenSynths.VENOM_HIT)
            player.mes(ApmekenMessages.VENOMED)
            player.hitTypeless(room.hazardRoll(POOL_DAMAGE, POOL_SPREAD))
        }
    }

    private companion object {
        const val FIRST_WAVE = 16
        const val FORCED_MAX_HIT = "varn.forced_max_hit_styles"
        const val POLL = 2
        const val THRESHOLD = 2
        const val LAST_THRESHOLD = 3
        const val SPAWN_IDLE = 2
        const val THRALL_CAP_MIN = 4
        const val THRALL_CAP_MAX = 5
        const val SUMMON_RADIUS = 3
        const val THRALL_DRAIN = 2
        const val PRAYER_STAT = "stat.prayer"
        const val EXPLODE_ANIM = 2
        const val EXPLODE_AT = 4
        const val CARRIER_TICKS = 2
        const val EXPLOSION_DAMAGE = 14
        const val EXPLOSION_SPREAD = 2
        const val POOL_TICKS = 30
        const val POOL_TRAIL = 2
        const val POOL_DAMAGE = 6
        const val POOL_SPREAD = 2
        const val WANDER_DELAY = 3
        const val WANDER_RANGE = 8
        const val PAUSE_ODDS = 12

        const val WEAK_MELEE = 1
        const val WEAK_RANGED = 2
        const val WEAK_MAGIC = 4

        val WEAK_STYLES =
            mapOf(
                Kind.BRAWLER to WEAK_MAGIC,
                Kind.THROWER to WEAK_MELEE,
                Kind.MAGE to WEAK_RANGED,
                Kind.SHAMAN to WEAK_RANGED,
                Kind.CURSED to WEAK_RANGED,
            )

        val SPECIALS = listOf(ApmekenNpcs.SHAMAN, ApmekenNpcs.VOLATILE, ApmekenNpcs.CURSED)

        val LARGE =
            setOf(ApmekenNpcs.BRAWLER_LARGE, ApmekenNpcs.THROWER_LARGE, ApmekenNpcs.MAGE_LARGE)

        val WAVES =
            listOf(
                Wave(listOf(ApmekenNpcs.BRAWLER_SMALL, ApmekenNpcs.BRAWLER_SMALL, ApmekenNpcs.SHAMAN)),
                Wave(
                    listOf(ApmekenNpcs.THROWER_SMALL, ApmekenNpcs.THROWER_SMALL, ApmekenNpcs.VOLATILE)
                ),
                Wave(listOf(ApmekenNpcs.MAGE_SMALL, ApmekenNpcs.MAGE_SMALL, ApmekenNpcs.CURSED)),
                Wave(listOf(ApmekenNpcs.THROWER_LARGE, ApmekenNpcs.THROWER_LARGE), specials = 3),
                Wave(
                    listOf(ApmekenNpcs.MAGE_LARGE, ApmekenNpcs.MAGE_LARGE, ApmekenNpcs.SHAMAN),
                    specials = 2,
                ),
                Wave(
                    listOf(ApmekenNpcs.BRAWLER_LARGE, ApmekenNpcs.BRAWLER_LARGE, ApmekenNpcs.SHAMAN),
                    specials = 2,
                ),
                Wave(
                    listOf(
                        ApmekenNpcs.BRAWLER_LARGE,
                        ApmekenNpcs.THROWER_LARGE,
                        ApmekenNpcs.SHAMAN,
                        ApmekenNpcs.CURSED,
                        ApmekenNpcs.CURSED,
                    )
                ),
                Wave(
                    listOf(ApmekenNpcs.SHAMAN, ApmekenNpcs.VOLATILE, ApmekenNpcs.VOLATILE),
                    specials = 2,
                ),
            )
    }
}
