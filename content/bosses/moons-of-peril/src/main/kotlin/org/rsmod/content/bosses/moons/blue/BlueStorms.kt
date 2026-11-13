package org.rsmod.content.bosses.moons.blue

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.Collections
import java.util.IdentityHashMap
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.output.spam
import org.rsmod.api.player.vars.setActiveMoveSpeed
import org.rsmod.content.bosses.moons.Moon
import org.rsmod.content.bosses.moons.MoonsFights
import org.rsmod.content.bosses.moons.MoonsFights.Companion.set
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.game.movement.MoveSpeed
import org.rsmod.map.CoordGrid

@Singleton
class BlueStorms
@Inject
constructor(private val deps: BossDeps, private val fights: MoonsFights) {
    private val moon = Moon.Blue

    private class Storm(val npc: Npc, val path: List<CoordGrid>) {
        var removeAt = -1
        val struck: MutableSet<Player> = Collections.newSetFromMap(IdentityHashMap())
    }

    private val storms = mutableListOf<Storm>()
    private val fire = mutableListOf<LocInfo>()
    private val columns = HashMap<Int, List<CoordGrid>?>()

    fun fireOn() {
        fireOff()
        for (row in FIRE_ROWS) {
            for (dx in FIRE_WEST..FIRE_EAST) {
                val at = moon.southWest.translate(dx, row)
                val info =
                    deps.locRepo.add(at, FIRE_LOC, Int.MAX_VALUE, LocAngle.North, LocShape.CentrepieceStraight)
                val fromEnd = minOf(dx - FIRE_WEST, FIRE_EAST - dx).coerceAtMost(FIRE_ANIMS.lastIndex)
                deps.worldRepo.locAnim(info, FIRE_ANIMS[fromEnd])
                fire += info
            }
        }
    }

    fun fireOff() {
        for (info in fire) deps.locRepo.del(info, Int.MAX_VALUE)
        fire.clear()
    }

    fun cast() {
        val npc = fights.npc(moon) ?: return
        npc.anim(CAST_SEQ)
        npc.spotanim(CAST_SPOTANIM, delay = CAST_SPOTANIM_DELAY, height = CAST_SPOTANIM_HEIGHT)
        for (target in CAST_TARGETS) {
            deps.worldRepo.projAnim(
                org.rsmod.game.proj.ProjAnim(
                    spotanim = CAST_PROJECTILE.asRSCM(RSCMType.SPOTANIM),
                    startHeight = CAST_START_HEIGHT,
                    endHeight = CAST_END_HEIGHT,
                    startTime = CAST_START_TIME,
                    endTime = CAST_END_TIME,
                    angle = CAST_ANGLE,
                    progress = CAST_PROGRESS,
                    sourceIndex = 0,
                    targetIndex = 0,
                    startCoord = moon.centre,
                    endCoord = moon.southWest.translate(target.first, target.second),
                )
            )
        }
    }

    fun spawn() {
        if (deps.random.of(0, 99) >= FIRST_SPAWN_PERCENT) return
        spawnOne()
        if (deps.random.of(0, 99) < SECOND_SPAWN_PERCENT) spawnOne()
    }

    private fun spawnOne() {
        val dx = deps.random.of(COLUMN_WEST, COLUMN_EAST)
        val column = columns.getOrPut(dx) { column(dx) } ?: return
        val path =
            if (deps.random.of(0, 1) == 0) column
            else column.reversed()
        val npc = Npc(STORM_NPC, path.first())
        deps.npcRepo.add(npc, STORM_LIFESPAN)
        npc.noneMode()
        npc.ignoreCombatInteractions = true
        npc.faceSquare(path.last())
        npc.anim(STORM_SPAWN_SEQ)
        deps.worldRepo.soundArea(path.first(), STORM_SPAWN_SYNTH, radius = STORM_SOUND_RADIUS)
        npc.walk(path.last())
        storms += Storm(npc, path)
    }

    private fun column(dx: Int): List<CoordGrid>? {
        val tiles = (COLUMN_SOUTH..COLUMN_NORTH).map { moon.southWest.translate(dx, it) }
        val first = tiles.indexOfFirst { !deps.collision.isWalkBlocked(it) }
        val last = tiles.indexOfLast { !deps.collision.isWalkBlocked(it) }
        if (first < 0 || last - first + 1 < 2) return null
        return tiles.subList(first, last + 1)
    }

    fun step(clear: Boolean) {
        val now = deps.mapClock.cycle
        val iterator = storms.iterator()
        while (iterator.hasNext()) {
            val storm = iterator.next()
            val gone = !storm.npc.isValidTarget() && !storm.npc.isSlotAssigned
            if (clear || gone || (storm.removeAt in 0..now)) {
                if (storm.npc.isSlotAssigned) deps.npcRepo.del(storm.npc, Int.MAX_VALUE)
                iterator.remove()
                continue
            }
            if (storm.removeAt >= 0) continue
            val npc = storm.npc
            strike(storm)
            if (npc.coords == storm.path.last()) {
                npc.anim(STORM_DESPAWN_SEQ)
                storm.removeAt = now + 1
            }
        }
    }

    private fun strike(storm: Storm) {
        val at = storm.npc.coords
        val boss = fights.npc(moon) ?: return
        for (player in fights.playersIn(moon)) {
            val fight = fights.of(player) ?: continue
            if (fight.subdued || player.coords != at || !storm.struck.add(player)) continue
            player.queueHit(boss, 1, HitType.Typeless, deps.random.of(STORM_HIT.first, STORM_HIT.last), deps.playerHitModifier)
            player.spotanim(STRIKE_SPOTANIM, height = STRIKE_HEIGHT, slot = STRIKE_SLOT)
            fights.addCondition(fight, STORM_COLD)
            if (player.moveSpeed == MoveSpeed.Run) {
                player.setActiveMoveSpeed(MoveSpeed.Walk)
                deps.worldQueues.add(FORCED_WALK) {
                    if (fights.of(player) === fight) player.setActiveMoveSpeed(MoveSpeed.Run)
                }
            }
        }
    }

    fun heal(player: Player) {
        val fight = fights.of(player) ?: return
        if (fight.subdued) return
        val lit = player.vars[BlueFights.BRAZIER_LEFT] + player.vars[BlueFights.BRAZIER_RIGHT]
        val amount = lit * HEAL_PER_BRAZIER
        val boss = fights.npc(moon) ?: return
        if (amount > 0) fights.tracker(moon)?.heal(player, amount)
        fights.showHeal(fight, boss, amount)
    }

    fun light(player: Player, side: Int) {
        val fight = fights.of(player) ?: return
        val varbit = if (side == LEFT) BlueFights.BRAZIER_LEFT else BlueFights.BRAZIER_RIGHT
        if (player.vars[varbit] == 0) {
            player.mes(ALREADY_BURNING)
            return
        }
        if (fight.relightSide != -1) return
        player.anim(RELIGHT_SEQ)
        player.soundSynth(RELIGHT_SYNTH)
        fight.relightSide = side
        deps.worldQueues.add(1) { relight(player, fight, side, varbit) }
    }

    private fun relight(player: Player, fight: org.rsmod.content.bosses.moons.MoonsFight, side: Int, varbit: String) {
        if (fights.of(player) !== fight) return
        player.set(varbit, 0)
        player.spam(RELIGHT_MESSAGE)
        fight.relightSide = -1
        if (player.vars[BlueFights.BRAZIER_LEFT] == 0 && player.vars[BlueFights.BRAZIER_RIGHT] == 0) {
            player.set(MoonsFights.CONDITION, 0)
            player.mes(WARMTH_MESSAGE)
        }
    }

    companion object {
        const val LEFT = 0
        const val RIGHT = 1
        const val STORM_COLD = 6
        const val CLEAR = true
        private const val FIRST_SPAWN_PERCENT = 60
        private const val SECOND_SPAWN_PERCENT = 15
        private const val COLUMN_WEST = -10
        private const val COLUMN_EAST = 14
        private const val COLUMN_SOUTH = -9
        private const val COLUMN_NORTH = 13
        private const val FIRE_WEST = -5
        private const val FIRE_EAST = 9
        private val FIRE_ROWS = listOf(-6, 10)
        private const val FIRE_LOC = "loc.pmoon_boss_blue_fire"
        private val FIRE_ANIMS =
            listOf(
                "seq.fx_fire02_spawn_idle",
                "seq.fx_fire02_spawn_idle_delay_1",
                "seq.fx_fire02_spawn_idle_delay_2",
                "seq.fx_fire02_spawn_idle_delay_3",
                "seq.fx_fire02_spawn_idle_delay_4",
            )
        private const val STORM_NPC = "npc.pmoon_boss_winter_storm"
        private const val STORM_LIFESPAN = 70
        private const val STORM_SPAWN_SEQ = "seq.vfx_djinn_blue_tempest_spawn_01"
        private const val STORM_DESPAWN_SEQ = "seq.vfx_djinn_blue_tempest_despawn_01"
        private const val STORM_SPAWN_SYNTH = "synth.varl_pm_boss_blue_tornado_loop_01"
        private const val STORM_SOUND_RADIUS = 10
        private val STORM_HIT = 1..8
        private const val STRIKE_SPOTANIM = "spotanim.wyvern_skeleton_impact_breath"
        private const val STRIKE_HEIGHT = 60
        private const val STRIKE_SLOT = 2
        private const val FORCED_WALK = 2
        private const val CAST_SEQ = "seq.npc_djinn_03_magic"
        private const val CAST_SPOTANIM = "spotanim.vfx_djinn_blue_moon_spotanim_magic"
        private const val CAST_SPOTANIM_DELAY = 30
        private const val CAST_SPOTANIM_HEIGHT = 255
        private const val CAST_PROJECTILE = "spotanim.vfx_djinn_blue_moon_projanim_01"
        private const val CAST_START_HEIGHT = 830
        private const val CAST_END_HEIGHT = 50
        private const val CAST_START_TIME = 80
        private const val CAST_END_TIME = 150
        private const val CAST_ANGLE = 140
        private const val CAST_PROGRESS = 60
        private val CAST_TARGETS = listOf(-12 to 2, 16 to 2)
        private const val HEAL_PER_BRAZIER = 10
        private const val RELIGHT_SEQ = "seq.human_casting"
        private const val RELIGHT_SYNTH = "synth.varl_pm_boss_blue_relight_brazier"
        private const val RELIGHT_MESSAGE =
            "You dispel the blue moon's shroud over the brazier causing it to re-ignite!"
        private const val WARMTH_MESSAGE =
            "Lighting the final brazier fills you with warmth, removing some of your cold buildup."
        private const val ALREADY_BURNING = "The brazier is already burning."
    }
}
