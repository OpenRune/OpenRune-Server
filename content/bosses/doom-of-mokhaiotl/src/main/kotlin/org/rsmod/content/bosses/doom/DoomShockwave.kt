package org.rsmod.content.bosses.doom

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.MoveRestrict
import dev.openrune.types.NpcMode
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.IdentityHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.repeatTick
import org.rsmod.api.bosses.runtime.spawnOwnedNpc
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.route.StepFactory
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onModifyNpcHit
import org.rsmod.api.script.onNpcQueue
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.entity.util.EntityTinting
import org.rsmod.game.hit.HitType
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.game.movement.MoveSpeed
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionStrategy

internal object DoomShockwave {
    const val VOLATILE_EARTH_EXT = "doom.volatile_earth"
    const val SLAM_EXT = "doom.shockwave_slam"
    const val END_EXT = "doom.shockwave_end"

    const val FIGHT_TICKS_TRIGGER = 79
    const val PRE_SHOCKWAVE_ROCK_THROW_DELAY = 5

    private const val CHARGE_DELAY = 15
    private const val CHARGE_LOOP_DELAY = 2
    private const val SLAM_WINDUP = 2
    private const val SLAM_GAP = 2
    private const val SLAM_HIT_DELAY = 2
    private const val END_DELAY = 1

    private const val CHARGE_SEQ = "seq.dom_area_charge"
    private const val CHARGE_LOOP_SEQ = "seq.dom_area_charge_loop"
    private const val SLAM_SEQ = "seq.doom_area_slam"
    private const val SLAM_SPOTANIM = "spotanim.vfx_doom_area_slam"
    private const val RETURN_IDLE_SEQ = "seq.doom_area_slam_return_idle"

    fun volatileEarth(delve: DoomDelve, scriptedRockThrow: AbilityRef): Effect =
        sequence(
            external(VOLATILE_EARTH_EXT, params = delve),
            if (delve.hasBurrow) Effect.NoOp else forceNext(scriptedRockThrow),
            after(CHARGE_DELAY, shockwave(delve, scriptedRockThrow)),
        )

    private fun shockwave(delve: DoomDelve, scriptedRockThrow: AbilityRef): Effect =
        sequence(
            anim(CHARGE_SEQ),
            wait(CHARGE_LOOP_DELAY),
            anim(CHARGE_LOOP_SEQ),
            wait(SLAM_WINDUP),
            repeat(
                delve.shockwaveSlams,
                effect =
                    sequence(
                        anim(SLAM_SEQ),
                        spotanim(SLAM_SPOTANIM),
                        after(SLAM_HIT_DELAY, external(SLAM_EXT)),
                        wait(SLAM_GAP),
                    ),
            ),
            anim(RETURN_IDLE_SEQ),
            after(END_DELAY, external(END_EXT), requireAlive = false),
            wait(END_DELAY),
            if (delve.hasBurrow) {
                sequence(
                    setVarn(DoomVarns.ROTATION_STEP, DoomCar.POST_SHOCKWAVE),
                    setVarn(DoomVarns.STEP_ATTACKS, 0),
                    forceNext(scriptedRockThrow),
                    nextAttackIn(delve.attackSpeed),
                )
            } else Effect.NoOp,
        )
}

@Singleton
internal class DoomShockwaves
@Inject
constructor(
    private val deps: BossDeps,
    private val stepFactory: StepFactory,
    private val acid: DoomAcid,
) {
    private val waves: MutableMap<Npc, Wave> = IdentityHashMap()
    private val nodeWaves: MutableMap<Npc, Wave> = IdentityHashMap()

    private val ripples by lazy { RIPPLE_SPOTANIMS.map { SpotanimType(it.asRSCM(RSCMType.SPOTANIM)) } }

    private class Wave(val boss: Npc, val player: Player, val delve: DoomDelve) {
        val nodes: MutableList<Npc> = mutableListOf()
        var destination: CoordGrid? = null
        var shield: Npc? = null
        var shieldSpawned = false
    }

    fun register(script: ScriptContext) {
        deps.extensionRegistry.register(DoomShockwave.VOLATILE_EARTH_EXT) { ext ->
            spawnNodes(ext.npc, ext.target, ext.params as DoomDelve)
        }
        deps.extensionRegistry.register(DoomShockwave.SLAM_EXT) { ext -> slam(ext.npc, ext.target) }
        deps.extensionRegistry.register(DoomShockwave.END_EXT) { ext -> waves.remove(ext.npc)?.let(::end) }

        val nodeType = ServerCacheManager.getNpc(NODE.asRSCM(RSCMType.NPC))!!
        script.onNpcQueue(nodeType, "queue.death") { onNodeDeath(this) }
        script.onModifyNpcHit(nodeType) {
            val attacker = hit.playerAttacker(deps.playerList) ?: return@onModifyNpcHit
            if (hit.isDemonbaneOrEyeOfAyak()) attacker.clearAttackDelay()
            hit.damage = max(attacker.vars[MAX_HIT_VARP], 1)
        }
        script.onEvent<NpcStateEvents.Delete> {
            waves.remove(npc)
            nodeWaves.remove(npc)
        }
    }

    private fun spawnNodes(boss: Npc, player: Player, delve: DoomDelve) {
        val wave = Wave(boss, player, delve)
        val spawn = boss.spawnCoords
        val candidates =
            NODE_OFFSETS.map { (dx, dz) -> spawn.translate(dx, dz) }
                .filter { !underBoss(boss, it) && !deps.collision.isWalkBlocked(it) }
                .toMutableList()
        repeat(deps.random.of(NODE_COUNT).coerceAtMost(candidates.size)) {
            val tile = candidates.removeAt(deps.random.of(candidates.size))
            val node = deps.spawnOwnedNpc(boss, NODE, tile) ?: return@repeat
            node.mode = NpcMode.None
            node.ignoreCombatInteractions = true
            node.vars[DoomVarns.GUARANTEED_HIT] = 1
            node.anim(NODE_SPAWN_SEQ)
            wave.nodes += node
            nodeWaves[node] = wave
        }
        waves[boss] = wave
    }

    private suspend fun onNodeDeath(access: StandardNpcAccess) {
        val node = access.npc
        access.noneMode()
        access.hideAllOps()
        val wave = nodeWaves[node]
        if (wave == null || wave.shieldSpawned) {
            removeNode(node)
            return
        }
        node.anim(NODE_POP_SEQ)
        val destination = wave.destination
        if (destination == null) {
            wave.destination = node.coords
        } else {
            spawnShield(wave, node.coords, destination)
            val others = wave.nodes.filter { it !== node && it.isSlotAssigned }
            deps.worldQueues.add(1) { others.forEach { it.anim(NODE_DEATH_SEQ) } }
            deps.worldQueues.add(NODE_REMOVE_DELAY) { others.forEach(::removeNode) }
        }
        access.delay(NODE_REMOVE_DELAY)
        removeNode(node)
    }

    private fun spawnShield(wave: Wave, centre: CoordGrid, destination: CoordGrid) {
        wave.shieldSpawned = true
        val shield = deps.spawnOwnedNpc(wave.boss, SHIELD, centre.translate(-SHIELD_HALF, -SHIELD_HALF)) ?: return
        shield.mode = NpcMode.None
        shield.ignoreCombatInteractions = true
        shield.moveRestrict = MoveRestrict.PassThru
        shield.defaultMoveSpeed = if (wave.delve.shieldCrawls) MoveSpeed.Crawl else MoveSpeed.Walk
        shield.anim(SHIELD_SPAWN_SEQ)
        wave.shield = shield
        val target = destination.translate(-SHIELD_HALF, -SHIELD_HALF)
        val stepTicks = if (wave.delve.shieldCrawls) CRAWL_STEP_TICKS else 1
        var ticks = 0
        deps.repeatTick(
            ticks = Int.MAX_VALUE,
            onTick = { _ ->
                if (wave.shield !== shield || !shield.isSlotAssigned) {
                    tintInside(wave.player, inside = false)
                    return@repeatTick false
                }
                acid.clear(wave.player, shield.coords.translate(SHIELD_HALF, SHIELD_HALF))
                tintInside(wave.player, insideShield(wave, wave.player))
                if (shield.coords == target) {
                    dissolve(wave)
                    tintInside(wave.player, inside = false)
                    return@repeatTick false
                }
                if (++ticks % stepTicks == 0) step(shield, target)
                true
            },
        )
    }

    private fun step(shield: Npc, target: CoordGrid) {
        val from = shield.coords
        val next = from.translate((target.x - from.x).sign, (target.z - from.z).sign)
        if (stepFactory.validated(shield, from, next, CollisionStrategy.Normal) == next) {
            shield.walk(next)
        } else {
            shield.teleport(deps.collision, next)
        }
    }

    private fun tintInside(player: Player, inside: Boolean) {
        if (player.isSlotAssigned) player.tint(if (inside) SHIELD_TINT else NO_TINT)
    }

    private fun slam(boss: Npc, target: Player) {
        val centre = boss.coords.translate(BOSS_HALF, BOSS_HALF)
        val spawn = boss.spawnCoords
        for (dx in DoomArena.FLOOR_MIN_X..DoomArena.FLOOR_MAX_X) {
            for (dz in DoomArena.FLOOR_MIN_Z..DoomArena.FLOOR_MAX_Z) {
                val tile = spawn.translate(dx, dz)
                if (deps.collision.isWalkBlocked(tile)) continue
                val distance = max(abs(tile.x - centre.x), abs(tile.z - centre.z))
                deps.worldRepo.spotanimMap(deps.random.pick(ripples), tile, delay = (distance - RIPPLE_LEAD).coerceAtLeast(0))
            }
        }
        if (!target.isValidTarget() || insideShield(waves[boss], target)) return
        target.queueHit(boss, 1, HitType.Typeless, deps.random.of(SLAM_DAMAGE), deps.playerHitModifier)
    }

    private fun insideShield(wave: Wave?, player: Player): Boolean {
        val shield = wave?.shield ?: return false
        if (!shield.isSlotAssigned) return false
        val sw = shield.coords
        return player.coords.x in sw.x until sw.x + SHIELD_SIZE && player.coords.z in sw.z until sw.z + SHIELD_SIZE
    }

    private fun end(wave: Wave) {
        val remaining = wave.nodes.filter { it.isSlotAssigned && it.hitpoints > 0 }
        remaining.forEach { it.anim(NODE_DEATH_SEQ) }
        deps.worldQueues.add(NODE_DESPAWN_DELAY) { remaining.forEach(::removeNode) }
        dissolve(wave)
    }

    private fun dissolve(wave: Wave) {
        val shield = wave.shield ?: return
        wave.shield = null
        if (!shield.isSlotAssigned) return
        shield.anim(SHIELD_DEATH_SEQ)
        deps.worldQueues.add(1) { if (shield.isSlotAssigned) deps.npcRepo.del(shield, Int.MAX_VALUE) }
    }

    private fun removeNode(node: Npc) {
        nodeWaves.remove(node)
        if (node.isSlotAssigned) deps.npcRepo.del(node, Int.MAX_VALUE)
    }

    private fun underBoss(boss: Npc, tile: CoordGrid): Boolean =
        tile.x - boss.coords.x in 0 until boss.size && tile.z - boss.coords.z in 0 until boss.size

    private companion object {
        private const val NODE = "npc.dom_shockwave_path_node"
        private const val SHIELD = "npc.dom_shockwave_shield"
        private const val NODE_SPAWN_SEQ = "seq.area_node_spawn"
        private const val NODE_POP_SEQ = "seq.area_node_pop"
        private const val NODE_DEATH_SEQ = "seq.area_node_death"
        private const val SHIELD_SPAWN_SEQ = "seq.shield_spawn"
        private const val SHIELD_DEATH_SEQ = "seq.shield_death"

        private val NODE_COUNT = 12..24
        private const val NODE_REMOVE_DELAY = 3
        private const val MAX_HIT_VARP = "varp.com_maxhit"
        private const val NODE_DESPAWN_DELAY = 2
        private const val SHIELD_SIZE = 3
        private const val SHIELD_HALF = SHIELD_SIZE / 2
        private const val CRAWL_STEP_TICKS = 2
        private val SHIELD_TINT = EntityTinting(startCycle = 0, endCycle = 30, hue = 0, saturation = 0, lightness = 106, weight = 112)
        private val NO_TINT = EntityTinting(startCycle = 0, endCycle = 0, hue = -1, saturation = -1, lightness = -1, weight = 0)
        private const val BOSS_HALF = 2

        private val SLAM_DAMAGE = 26..42
        private const val RIPPLE_LEAD = 2
        private val RIPPLE_SPOTANIMS =
            listOf("spotanim.vfx_area_slam_01", "spotanim.vfx_area_slam_02", "spotanim.vfx_area_slam_03")

        private val NODE_OFFSETS =
            listOf(
                -10 to -8, -10 to 7, -9 to 3, -8 to 12, -7 to -6, -7 to -1, -7 to 10, -6 to 5,
                -5 to 2, -4 to -3, -4 to 8, -4 to 12, -3 to -7, -2 to 0, -2 to 4, -1 to 10,
                0 to -4, 0 to 3, 0 to 7, 0 to 14, 1 to -8, 1 to 0, 2 to 5, 2 to 12,
                3 to -3, 3 to 9, 4 to -10, 4 to -6, 4 to 2, 5 to 0, 6 to -8, 6 to 6,
                6 to 13, 7 to -2, 7 to 3, 7 to 9, 8 to -10, 9 to -6, 9 to 1, 10 to -3,
                10 to 7, 10 to 13, 11 to -9, 11 to 0, 11 to 3, 11 to 10, 11 to 14,
            )
    }
}
