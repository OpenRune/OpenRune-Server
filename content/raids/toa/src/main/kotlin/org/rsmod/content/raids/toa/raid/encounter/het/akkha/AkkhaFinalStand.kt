package org.rsmod.content.raids.toa.raid.encounter.het.akkha

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import kotlin.math.floor
import kotlin.math.min
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.content.raids.toa.raid.encounter.hitTypeless
import org.rsmod.content.raids.toa.raid.encounter.npcType
import org.rsmod.content.raids.toa.raid.encounter.spotanim
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid

internal class AkkhaFinalStand(private val room: AkkhaEncounter) {
    private val deps = room.raid.deps
    private val fakes = ArrayList<Npc>()
    private val orbs = ArrayList<Orb>()

    private class Orb(val npc: Npc, val dx: Int, val dz: Int)

    enum class Phase {
        NONE,
        PENDING,
        ACTIVE,
    }

    var phase = Phase.NONE
        private set

    private var corner = Quadrant.NE
    private var hits = 0
    private var swapCycle = -1

    private val edgeStarts: List<Triple<CoordGrid, Int, Int>> by lazy { findEdgeStarts() }

    fun begin(boss: Npc) {
        if (phase != Phase.NONE) return
        phase = Phase.PENDING
        boss.ignoreCombatInteractions = true
        boss.noneMode()
        room.messageAll(FINAL_STAND_TEXT)
        boss.transmog(npcType(AkkhaNpcs.ENRAGE_SPAWN), Int.MAX_VALUE)
        boss.anim(AkkhaSeqs.FINAL_STAND)
        room.shadows.finalStand()
        room.schedule(INITIAL_DELAY) {
            boss.transmog(npcType(AkkhaNpcs.ENRAGE_INITIAL), Int.MAX_VALUE)
            boss.hitpoints = floor(boss.baseHitpointsLvl * HEAL_FRACTION).toInt()
            room.hpBar?.update()
        }
        room.schedule(DISAPPEAR_DELAY) { boss.anim(AkkhaSeqs.DISAPPEAR) }
        room.schedule(REAPPEAR_DELAY) { reappear(boss) }
    }

    fun modifyHit(npc: Npc, hit: org.rsmod.game.hit.HitBuilder) {
        val real = npc.visType.id == AkkhaNpcs.ENRAGE.asRSCM(RSCMType.NPC)
        if (!real || phase != Phase.ACTIVE || hit.type != HitType.Melee) hit.damage = 0
    }

    fun onRealHit(hit: Hit) {
        if (phase != Phase.ACTIVE || !hit.isFromPlayer || hit.damage <= 0) return
        val cycle = deps.mapClock.cycle
        if (cycle == swapCycle + 1) return
        if (++hits < swapThreshold()) return
        hits = 0
        swap(cycle)
    }

    fun complete() {
        val dead = fakes.toList()
        fakes.clear()
        for (fake in dead) {
            fake.anim(AkkhaSeqs.DEATH)
            room.schedule(DEATH_TICKS) { room.despawn(fake) }
        }
        clearOrbs()
    }

    fun clear() {
        phase = Phase.NONE
        hits = 0
        swapCycle = -1
        for (fake in fakes) room.despawn(fake)
        fakes.clear()
        clearOrbs()
    }

    private fun swapThreshold(): Int = BASE_SWAP_HITS + room.teamSize

    private fun cornerOf(npc: Npc): Quadrant? {
        val centre = room.staticCoords(npc.coords.translate(1, 1))
        return if (centre == CoordGrid.NULL) null else Quadrant.of(centre)
    }

    private fun reappear(boss: Npc) {
        if (room.stage != ToaStage.STARTED) return
        corner = Quadrant.entries[deps.random.of(0, Quadrant.entries.lastIndex)]
        boss.transmog(npcType(AkkhaNpcs.ENRAGE), Int.MAX_VALUE)
        boss.telejump(deps.collision, room.coords(AkkhaShadows.cornerTile(corner)))
        AkkhaShadows.faceCorner(boss, corner)
        boss.anim(AkkhaSeqs.APPEAR)
        boss.removeHeadbar(AkkhaHeadbars.AKKHA_HEALTH.asRSCM(RSCMType.HEADBAR))
        for (quadrant in Quadrant.entries) {
            if (quadrant == corner) continue
            val fake = AkkhaShadows.spawnClone(room, AkkhaNpcs.SHADOW_ENRAGE, quadrant)
            fake.anim(AkkhaSeqs.APPEAR)
            fakes += fake
        }
        phase = Phase.ACTIVE
        room.schedule(FIRST_WAVE) { wave(0) }
        stepLoop()
    }

    private fun swap(cycle: Int) {
        val boss = room.akkha ?: return
        val fake = fakes[deps.random.of(0, fakes.lastIndex)]
        boss.anim(AkkhaSeqs.DISAPPEAR)
        fake.anim(AkkhaSeqs.DISAPPEAR)
        swapCycle = cycle
        room.schedule(1) {
            if (phase != Phase.ACTIVE) return@schedule
            val realTile = boss.coords
            boss.telejump(deps.collision, fake.coords)
            fake.telejump(deps.collision, realTile)
            cornerOf(boss)?.let { AkkhaShadows.faceCorner(boss, it) }
            cornerOf(fake)?.let { AkkhaShadows.faceCorner(fake, it) }
            boss.anim(AkkhaSeqs.APPEAR)
            fake.anim(AkkhaSeqs.APPEAR)
        }
    }

    private fun wave(index: Int) {
        if (phase != Phase.ACTIVE || room.stage != ToaStage.STARTED) return
        val count = deps.random.of(MIN_ORBS, MAX_ORBS)
        repeat(count) { spawnOrb() }
        room.schedule(WAVE_GAP) { wave(index + 1) }
    }

    private fun spawnOrb() {
        if (edgeStarts.isEmpty()) return
        val (tile, dx, dz) = edgeStarts[deps.random.of(0, edgeStarts.lastIndex)]
        orbs += Orb(room.spawn(AkkhaNpcs.UNSTABLE_ORB, tile), dx, dz)
    }

    private fun stepLoop() {
        room.schedule(1) {
            if (phase != Phase.ACTIVE || room.stage != ToaStage.STARTED) return@schedule
            stepOrbs()
            stepLoop()
        }
    }

    private fun stepOrbs() {
        val targets = room.targets()
        val iterator = orbs.iterator()
        while (iterator.hasNext()) {
            val orb = iterator.next()
            val victim = targets.firstOrNull { it.coords == orb.npc.coords }
            if (victim != null) {
                hit(victim)
                burst(orb)
                iterator.remove()
                continue
            }
            val next = orb.npc.coords.translate(orb.dx, orb.dz)
            if (!open(next)) {
                burst(orb)
                iterator.remove()
                continue
            }
            orb.npc.teleport(deps.collision, next)
            targets.firstOrNull { it.coords == next }?.let {
                hit(it)
                burst(orb)
                iterator.remove()
            }
        }
    }

    private fun burst(orb: Orb) {
        deps.worldRepo.spotanimMap(spotanim(AkkhaSpots.ORB_IMPACT), orb.npc.coords)
        room.despawn(orb.npc)
    }

    private fun hit(player: Player) {
        val raidLevel = room.raid.settings.raidLevel
        val base = room.combatantOf(room.akkha ?: return)?.maxHit(ORB_DAMAGE) ?: ORB_DAMAGE
        var damage = base + deps.random.of(0, ORB_SPREAD)
        val praying = player.protectsFromMagic()
        if (praying && raidLevel < PRAYER_LEVEL_LIMIT) damage = damage * PRAYER_PERCENT / 100
        player.hitTypeless(min(ORB_CAP, damage))
    }

    private fun Player.protectsFromMagic(): Boolean = protectFromMagic != 0

    private fun clearOrbs() {
        for (orb in orbs) room.despawn(orb.npc)
        orbs.clear()
    }

    private fun open(tile: CoordGrid): Boolean {
        val static = room.staticCoords(tile)
        return static != CoordGrid.NULL && room.room.inChallengeArea(static) && room.isOpenFloor(tile)
    }

    private fun findEdgeStarts(): List<Triple<CoordGrid, Int, Int>> {
        val result = ArrayList<Triple<CoordGrid, Int, Int>>()
        for (tile in room.quadrants.openTiles()) {
            for ((dx, dz) in DIRECTIONS) {
                val back = tile.translate(-dx, -dz)
                val ahead = tile.translate(dx, dz)
                if (!open(back) && open(ahead)) result += Triple(tile, dx, dz)
            }
        }
        return result
    }

    private companion object {
        const val INITIAL_DELAY = 2
        const val DISAPPEAR_DELAY = 4
        const val REAPPEAR_DELAY = 5
        const val FIRST_WAVE = 2
        const val WAVE_GAP = 3
        const val DEATH_TICKS = 2
        const val HEAL_FRACTION = 0.2
        const val BASE_SWAP_HITS = 2
        const val MIN_ORBS = 3
        const val MAX_ORBS = 6
        const val ORB_DAMAGE = 13
        const val ORB_SPREAD = 2
        const val ORB_CAP = 25
        const val PRAYER_PERCENT = 75
        const val PRAYER_LEVEL_LIMIT = 300
        const val FINAL_STAND_TEXT = "Akkha makes his final stand!"
        val DIRECTIONS = listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)
    }
}

private val Player.protectFromMagic by intVarBit("varbit.prayer_protectfrommagic")
