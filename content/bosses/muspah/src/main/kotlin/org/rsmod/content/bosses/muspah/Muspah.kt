package org.rsmod.content.bosses.muspah

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
import dev.openrune.types.NpcServerType
import dev.openrune.types.ProjAnimType
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.bosses.runtime.suppressAttacks
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.bosses.spec.ProjectileConfig
import org.rsmod.api.combat.commons.player.combatPlayDefendAnim
import org.rsmod.api.combat.commons.player.finishNpcHit
import org.rsmod.api.combat.commons.player.queueCombatRetaliate
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.heal
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.events.PlayerHitEvents
import org.rsmod.api.player.hit.queueImpactHit
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.output.Camera
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onEvent
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.entity.npc.NpcUid
import org.rsmod.game.entity.player.PlayerUid
import org.rsmod.game.entity.util.EntityExactMove
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid
import org.rsmod.map.util.Translation
import org.rsmod.plugin.scripts.ScriptContext
import kotlin.math.PI
import kotlin.math.atan2

class Muspah
@Inject
constructor(deps: BossDeps, private val locRepo: LocRepository, private val npcList: NpcList) :
    BossPluginScript(deps) {

    private val logger = InlineLogger()

    private val rangedId by lazy { "npc.muspah".asRSCM(RSCMType.NPC) }
    private val meleeId by lazy { "npc.muspah_melee".asRSCM(RSCMType.NPC) }
    private val teleportId by lazy { "npc.muspah_teleport".asRSCM(RSCMType.NPC) }
    private val soulsplitId by lazy { "npc.muspah_soulsplit".asRSCM(RSCMType.NPC) }
    private val finalId by lazy { "npc.muspah_final".asRSCM(RSCMType.NPC) }
    private val projectileHazardId by lazy { "npc.muspah_projectile".asRSCM(RSCMType.NPC) }

    private val liveFormIds by lazy {
        setOf(rangedId, meleeId, teleportId, soulsplitId, finalId)
    }

    private val fights = mutableMapOf<NpcUid, MuspahFight>()

    private fun fightFor(npc: Npc): MuspahFight = fights.getOrPut(npc.uid) { MuspahFight() }

    override fun ScriptContext.startup() {
        BossCombat.register(
            this,
            spec,
            deps,
            onModifyHit = { onMuspahHit(npc, hit) },
            onCombatTick = { target -> checkPendingTransition(this, target) },
        )

        onEvent<NpcStateEvents.Respawn> {
            if (npc.visType.id in liveFormIds) fights.remove(npc.uid)?.let(::clearSpikes)
        }
        onEvent<NpcStateEvents.Delete> {
            if (npc.visType.id in liveFormIds) fights.remove(npc.uid)?.let(::clearSpikes)
        }

        deps.extensionRegistry.register("muspah_magic_projectile") { _, npc, target, _ ->
            deps.worldQueues.add(MAGIC_PROJECTILE_WINDUP) {
                if (npc.isValidTarget() && target.isValidTarget()) fireMagicProjectile(npc, target)
            }
        }
        onEvent<PlayerHitEvents.Impact> { logOutgoingHit(this) }
    }

    private fun logOutgoingHit(event: PlayerHitEvents.Impact) {
        val hit = event.hit
        if (!hit.isFromNpc) return
        val npc = hit.resolveNpcSource(npcList) ?: return
        if (npc.visType.id !in liveFormIds) return
        logger.info {
            "[muspah] outgoing hit: form=${formName(npc.visType.id)} type=${hit.type} damage=${hit.damage}"
        }
    }

    private fun formName(visId: Int): String =
        when (visId) {
            rangedId -> "ranged"
            meleeId -> "melee"
            teleportId -> "teleport"
            soulsplitId -> "soulsplit"
            finalId -> "final"
            else -> "unknown($visId)"
        }

    private fun clearSpikes(fight: MuspahFight) {
        fight.activeSpikes.forEach { locRepo.del(it, Int.MAX_VALUE) }
        fight.activeSpikes.clear()
    }

    override val spec =
        boss("npc.muspah", "npc.muspah_melee", "npc.muspah_teleport", "npc.muspah_soulsplit", "npc.muspah_final") {
            stats(attackRate = ATTACK_RATE, aggressionRadius = AGGRO_RANGE)

            val rangedAttack =
                ability("ranged_attack") {
                    anim("seq.npc_muspah_attack_ranged_01")
                    projectile {
                        spotanim = "spotanim.projectile_muspah_attack_ranged_01"
                        config = RANGED_PROJECTILE_CONFIG
                        hit {
                            delay = RANGED_HIT_DELAY
                            damage(0..RANGED_MAX_HIT).roll()
                            type(Ranged)
                        }
                    }
                }

            val meleeHit =
                ability("melee_hit") {
                    anim("seq.npc_muspah_attack_melee_01")
                    hit {
                        damage(0..MELEE_MAX_HIT).roll()
                        type(Melee)
                    }
                }

            val meleeRanged =
                ability("melee_ranged") {
                    anim("seq.npc_muspah_attack_melee_01")
                    projectile {
                        spotanim = "spotanim.projectile_muspah_attack_ranged_01"
                        hit {
                            delay = RANGED_HIT_DELAY
                            damage(0..MELEE_MAX_HIT).roll()
                            type(Ranged)
                        }
                    }
                }

            val magicAttack =
                ability("magic_attack") {
                    anim("seq.npc_muspah_attack_magic_01")
                    spotanim("spotanim.vfx_muspah_attack_magic_01")
                    include(external("muspah_magic_projectile"))
                }

            val transformSoulsplit =
                ability("transform_soulsplit") { include(transmog("npc.muspah_soulsplit", Int.MAX_VALUE)) }

            val transformFinal =
                ability("transform_final") { include(transmog("npc.muspah_final", Int.MAX_VALUE)) }

            phase(PHASE_RANGED) {
                weightedSelectorRandom {
                    +random(rangedAttack, weight = RANGED_ATTACK_WEIGHT)
                    +random(magicAttack, weight = RANGED_MAGIC_WEIGHT)
                }
            }

            phase(PHASE_MELEE) {
                weightedSelectorRandom {
                    +random(meleeHit, weight = 1, requires = WithinMeleeRange)
                    +random(meleeRanged, weight = 1, requires = Condition.Not(WithinMeleeRange))
                }
            }

            phase(PHASE_SOULSPLIT, entryHp = SOULSPLIT_HP_FRACTION) {
                entry = transformSoulsplit.name
                weightedSelectorRandom { +random(magicAttack, weight = 1) }
            }

        }

    private suspend fun checkPendingTransition(access: StandardNpcAccess, target: Player) {
        val npc = access.npc
        val fight = fightFor(npc)
        when (fight.pendingTransition) {
            Transition.TO_MELEE -> {
                fight.pendingTransition = null
                beginFormTransform(access, npc, target, meleeType(), PHASE_MELEE)
            }
            Transition.TO_RANGED -> {
                fight.pendingTransition = null
                beginFormTransform(access, npc, target, rangedType(), PHASE_RANGED)
            }
            Transition.TELEPORT_SPECIAL -> {
                fight.pendingTransition = null
                logger.info { "[muspah] transition applied: -> teleport special" }
                beginTeleportSpecial(access, npc, target)
            }
            null -> {}
        }
    }

    private fun beginFormTransform(
        access: StandardNpcAccess,
        npc: Npc,
        target: Player,
        type: NpcServerType,
        phase: String,
    ) {
        npc.anim(TRANSFORM_DISAPPEAR_SEQ)
        deps.worldQueues.add(TRANSFORM_ANIM_DELAY) {
            if (!npc.isValidTarget()) return@add
            access.changeType(type, Int.MAX_VALUE)
            access.spotanim(TRANSFORM_APPEAR_SPOTANIM)
            npc.anim(TRANSFORM_APPEAR_SEQ)
            deps.encounter(npc).transitionTo(phase, deps.mapClock.cycle)
            logger.info { "[muspah] transition applied: -> $phase" }

            if (target.isValidTarget()) beginSpikeSlam(npc, target)
        }
    }

    private fun onMuspahHit(npc: Npc, hit: HitBuilder) {
        val visId = npc.visType.id
        if (visId !in liveFormIds || !hit.isFromPlayer) return
        val attacker = hit.sourceUid?.let { PlayerUid(it).resolve(deps.playerList) }

        if (visId == soulsplitId || visId == finalId) {
            val protected = attacker?.vars?.get(PROTECT_FROM_MAGIC) ?: 0
            if (protected <= 0) {
                hit.damage = (hit.damage * SOULSPLIT_MITIGATION).toInt()
                attacker?.mes(SOULSPLIT_MESSAGE)
            }
            if (hit.damage > 0) npc.heal((hit.damage * SOULSPLIT_HEAL_FRACTION).toInt(), showHitsplat = true)
            return
        }

        val fight = fightFor(npc)
        fight.damageSinceSwitch += hit.damage
        fight.hitsSinceSwitch++
        logger.info {
            "[muspah] incoming hit: form=${formName(visId)} damage=${hit.damage} " +
                "damageSinceSwitch=${fight.damageSinceSwitch} hitsSinceSwitch=${fight.hitsSinceSwitch}"
        }

        val switchDue = fight.hitsSinceSwitch >= SWITCH_MIN_HITS
        when (visId) {
            rangedId ->
                if (switchDue && fight.damageSinceSwitch >= RANGED_SWITCH_DAMAGE) {
                    fight.damageSinceSwitch = 0
                    fight.hitsSinceSwitch = 0
                    fight.pendingTransition = Transition.TO_MELEE
                    logger.info { "[muspah] transition queued: ranged -> melee" }
                }
            meleeId ->
                if (switchDue && fight.damageSinceSwitch >= MELEE_SWITCH_DAMAGE) {
                    fight.damageSinceSwitch = 0
                    fight.hitsSinceSwitch = 0
                    fight.pendingTransition =
                        if (!fight.teleportSpecialUsed) {
                            fight.teleportSpecialUsed = true
                            Transition.TELEPORT_SPECIAL
                        } else {
                            Transition.TO_RANGED
                        }
                    logger.info { "[muspah] transition queued: melee -> ${fight.pendingTransition}" }
                }
        }
    }

    private fun beginTeleportSpecial(access: StandardNpcAccess, npc: Npc, target: Player) {
        access.changeType(teleportType(), Int.MAX_VALUE)
        deps.suppressAttacks(npc, TELEPORT_STEP_TICKS * (TELEPORT_LOOP.size + 1) + 2)

        val faceAnchor = npc.spawnCoords.translate(1, 0)

        TELEPORT_LOOP.forEachIndexed { index, offset ->
            deps.worldQueues.add(TELEPORT_STEP_TICKS * (index + 1)) {
                if (!npc.isValidTarget()) return@add
                val dest = npc.spawnCoords.translate(offset.first, offset.second)
                PathingEntityCommon.telejump(npc, deps.collision, dest)
                npc.faceSquare(faceAnchor)
                npc.resetFaceEntity()
                npc.anim("seq.npc_muspah_teleport_attack_01")
                access.spotanim("spotanim.vfx_muspah_teleport_attack_01")
                fireTeleportHit(npc, target)
                spawnHazardCloudBatch(npc.spawnCoords)
            }
        }

        deps.worldQueues.add(TELEPORT_STEP_TICKS * (TELEPORT_LOOP.size + 1)) {
            if (!npc.isValidTarget()) return@add
            val dest = npc.spawnCoords.translate(TELEPORT_RETURN_OFFSET.first, TELEPORT_RETURN_OFFSET.second)
            PathingEntityCommon.telejump(npc, deps.collision, dest)
            npc.faceSquare(faceAnchor)
            npc.resetFaceEntity()
            npc.anim("seq.npc_muspah_teleport_appear_01")
            access.spotanim("spotanim.vfx_muspah_teleport_appear_01")
            access.changeType(meleeType(), Int.MAX_VALUE)
            deps.encounter(npc).transitionTo(PHASE_MELEE, deps.mapClock.cycle)
        }
    }

    private fun fireTeleportHit(npc: Npc, target: Player) {
        if (!target.isValidTarget()) return
        val damage = deps.random.of(TELEPORT_MAX_HIT + 1)
        target.finishNpcHit(npc, RANGED_HIT_DELAY, HitType.Ranged, damage, deps.playerHitModifier)
    }

    private fun fireMagicProjectile(npc: Npc, target: Player) {
        val spotId = MAGIC_PROJECTILE_SPOTANIM.asRSCM(RSCMType.SPOTANIM)
        val type =
            ProjAnimType(
                startHeight = MAGIC_PROJECTILE_CONFIG.startHeight,
                endHeight = MAGIC_PROJECTILE_CONFIG.endHeight,
                delay = MAGIC_PROJECTILE_CONFIG.startDelay,
                angle = MAGIC_PROJECTILE_CONFIG.angle,
                lengthAdjustment = MAGIC_PROJECTILE_CONFIG.travelTime,
                progress = MAGIC_PROJECTILE_CONFIG.progress,
                stepMultiplier = MAGIC_PROJECTILE_CONFIG.stepMultiplier,
            )
        val projAnim = ProjAnim.fromNpcToPlayer(npc, target, spotId, type)
        deps.worldRepo.projAnim(projAnim)

        val damage = deps.random.of(MAGIC_MAX_HIT + 1)
        if (damage > 0) {
            target.spotanim(MAGIC_IMPACT_SPOTANIM, delay = projAnim.clientCycles)
        }
        target.queueCombatRetaliate(npc)
        target.queueImpactHit(npc, projAnim.serverCycles, HitType.Magic, damage, deps.playerHitModifier)
        target.combatPlayDefendAnim()
    }

    private fun spawnHazardCloudBatch(anchor: CoordGrid) {
        val type = ServerCacheManager.getNpc(projectileHazardId) ?: return
        val count = HAZARD_BATCH_MIN + deps.random.of(HAZARD_BATCH_MAX - HAZARD_BATCH_MIN + 1)
        repeat(count) {
            val coord =
                anchor.translate(
                    deps.random.of(ARENA_RADIUS * 2) - ARENA_RADIUS,
                    deps.random.of(ARENA_RADIUS * 2) - ARENA_RADIUS,
                )
            val cloud = Npc(type, coord)
            cloud.mode = NpcMode.None
            val lifetime = HAZARD_DRIFT_MIN + deps.random.of(HAZARD_DRIFT_MAX - HAZARD_DRIFT_MIN + 1)
            deps.npcRepo.add(cloud, lifetime + 2)
            hitHazardCloudOccupant(cloud)
            val direction = HAZARD_DIRECTIONS[deps.random.of(HAZARD_DIRECTIONS.size)]
            driftHazardCloud(cloud, direction, lifetime)
        }
    }

    private fun hitHazardCloudOccupant(cloud: Npc) {
        val occupant = deps.playerList.firstOrNull { it.coords == cloud.coords && it.hitpoints > 0 } ?: return
        val damage = deps.random.of(HAZARD_MAX_HIT + 1)
        occupant.finishNpcHit(cloud, 1, HitType.Typeless, damage, deps.playerHitModifier)
    }

    private fun driftHazardCloud(cloud: Npc, direction: Pair<Int, Int>, ticksLeft: Int) {
        if (ticksLeft <= 0) {
            deps.worldQueues.add(1) {
                if (cloud.isSlotAssigned) deps.npcRepo.del(cloud, Int.MAX_VALUE)
            }
            return
        }
        deps.worldQueues.add(1) {
            if (!cloud.isSlotAssigned) return@add
            val dest = cloud.coords.translate(direction.first, direction.second)
            PathingEntityCommon.teleport(cloud, deps.collision, dest)
            hitHazardCloudOccupant(cloud)
            driftHazardCloud(cloud, direction, ticksLeft - 1)
        }
    }

    private fun beginSpikeSlam(npc: Npc, target: Player) {
        val tiles = mutableSetOf(target.coords)
        val scatterCount = SPIKE_SCATTER_MIN + deps.random.of(SPIKE_SCATTER_MAX - SPIKE_SCATTER_MIN + 1)
        repeat(scatterCount) {
            val coord = randomWalkableTile(npc.spawnCoords, ARENA_RADIUS) ?: return@repeat
            tiles += coord
        }

        val warningSpot = SpotanimType(SPIKE_WARNING_SPOTANIM.asRSCM(RSCMType.SPOTANIM))
        tiles.forEach { deps.worldRepo.spotanimMap(warningSpot, it) }
        logger.info { "[muspah] spike slam: tiles=${tiles.size} guaranteed=${target.coords}" }

        deps.worldQueues.add(SPIKE_PRE_DELAY) {
            if (!npc.isValidTarget()) return@add
            val spawnSpot = SpotanimType(SPIKE_SPAWN_SPOTANIM.asRSCM(RSCMType.SPOTANIM))
            tiles.forEach {
                locRepo.add(it, SPIKE_TELEGRAPH_LOC, Int.MAX_VALUE, LocAngle.West, LocShape.CentrepieceStraight)
                deps.worldRepo.spotanimMap(spawnSpot, it)
            }

            deps.worldQueues.add(SPIKE_SOLIDIFY_DELAY) {
                if (!npc.isValidTarget()) return@add
                tiles.forEach { spawnSpike(npc, it) }
                shakeCameraNear(npc)
            }
        }
    }

    private fun shakeCameraNear(npc: Npc) {
        val nearby = deps.playerList.filter { it.coords.chebyshevDistance(npc.coords) <= CAM_SHAKE_RADIUS }
        for (player in nearby) {
            Camera.camShake(player, axis = 0, random = CAM_SHAKE_RANDOM_X, amplitude = 0, rate = 0)
            Camera.camShake(player, axis = 1, random = CAM_SHAKE_RANDOM_Y, amplitude = 0, rate = 0)
            Camera.camShake(player, axis = 2, random = CAM_SHAKE_RANDOM_Z, amplitude = 0, rate = 0)
        }
        deps.worldQueues.add(CAM_SHAKE_DURATION) {
            for (player in nearby) {
                if (player.isValidTarget()) Camera.camReset(player)
            }
        }
    }

    private fun spawnSpike(npc: Npc, coord: CoordGrid) {
        if (locRepo.findLoc(coord, SPIKE_LOC)) return
        val fight = fightFor(npc)
        val loc = locRepo.add(coord, SPIKE_LOC, Int.MAX_VALUE, LocAngle.West, LocShape.CentrepieceStraight)
        fight.activeSpikes += loc
        logger.info { "[muspah] spike spawned: coord=$coord active=${fight.activeSpikes.size}" }

        deps.worldQueues.add(1) {
            if (!locRepo.findLoc(coord, SPIKE_LOC)) return@add
            val occupant = deps.playerList.firstOrNull { it.coords == coord && it.hitpoints > 0 }
            if (occupant != null) {
                triggerSpikeHit(npc, occupant, coord)
            }
        }
    }

    private fun triggerSpikeHit(npc: Npc, player: Player, coord: CoordGrid) {
        val damage = SPIKE_DAMAGE_MIN + deps.random.of(SPIKE_DAMAGE_MAX - SPIKE_DAMAGE_MIN + 1)
        player.finishNpcHit(npc, 1, HitType.Typeless, damage, deps.playerHitModifier)
        npc.heal((damage * SPIKE_HEAL_FRACTION).toInt(), showHitsplat = true)

        val safeTile = randomAdjacentClearTile(coord) ?: coord
        logger.info {
            "[muspah] spike hit: coord=$coord damage=$damage heal=${(damage * SPIKE_HEAL_FRACTION).toInt()} " +
                "knockbackTo=$safeTile"
        }
        val lead = Translation.between(coord, safeTile)
        PathingEntityCommon.teleport(player, deps.collision, safeTile)
        player.pendingExactMove =
            EntityExactMove(
                deltaX1 = lead.x,
                deltaZ1 = lead.z,
                deltaX2 = 0,
                deltaZ2 = 0,
                clientDelay1 = 0,
                clientDelay2 = CLIENT_CYCLES_PER_TICK,
                direction = bearing(lead.x, lead.z),
            )
    }

    private fun bearing(dx: Int, dz: Int): Int {
        if (dx == 0 && dz == 0) return 0
        val turns = (atan2(-dx.toDouble(), -dz.toDouble()) / (2 * PI) * ANGLE_STEPS).toInt()
        return ((turns % ANGLE_STEPS) + ANGLE_STEPS) % ANGLE_STEPS
    }

    private fun randomWalkableTile(center: CoordGrid, radius: Int): CoordGrid? {
        val span = radius * 2 + 1
        repeat(RANDOM_TILE_ATTEMPTS) {
            val coord = center.translate(deps.random.of(span) - radius, deps.random.of(span) - radius)
            if (!deps.collision.isWalkBlocked(coord)) return coord
        }
        return null
    }

    private fun randomAdjacentClearTile(center: CoordGrid): CoordGrid? {
        val candidates =
            HAZARD_DIRECTIONS.map { center.translate(it.first, it.second) }.filter {
                !deps.collision.isWalkBlocked(it) && !locRepo.findLoc(it, SPIKE_LOC)
            }
        if (candidates.isEmpty()) return null
        return candidates[deps.random.of(candidates.size)]
    }

    private fun meleeType() = ServerCacheManager.getNpc(meleeId)!!

    private fun rangedType() = ServerCacheManager.getNpc(rangedId)!!

    private fun teleportType() = ServerCacheManager.getNpc(teleportId)!!

    private enum class Transition {
        TO_MELEE,
        TO_RANGED,
        TELEPORT_SPECIAL,
    }

    private class MuspahFight {
        var damageSinceSwitch: Int = 0
        var hitsSinceSwitch: Int = 0
        var teleportSpecialUsed: Boolean = false
        var pendingTransition: Transition? = null
        val activeSpikes: MutableList<LocInfo> = mutableListOf()
    }

    private companion object {
        private const val PHASE_RANGED = "ranged"
        private const val PHASE_MELEE = "melee"
        private const val PHASE_SOULSPLIT = "soulsplit"
        private const val PHASE_FINAL = "final"

        private const val ATTACK_RATE = 6
        private const val AGGRO_RANGE = 15

        private const val TRANSFORM_DISAPPEAR_SEQ = "seq.npc_muspah_transform_disappear_02"
        private const val TRANSFORM_APPEAR_SEQ = "seq.npc_muspah_transform_appear_02"
        private const val TRANSFORM_APPEAR_SPOTANIM = "spotanim.vfx_muspah_teleport_appear_01"
        private const val TRANSFORM_ANIM_DELAY = 1

        private const val RANGED_SWITCH_DAMAGE = 100
        private const val MELEE_SWITCH_DAMAGE = 80
        private const val SWITCH_MIN_HITS = 4

        private const val SOULSPLIT_HP_FRACTION = 75.0 / 850.0
        private const val FINAL_HP_FRACTION = 0.025

        private const val SOULSPLIT_MITIGATION = 0.2
        private const val SOULSPLIT_HEAL_FRACTION = 0.5
        private const val SOULSPLIT_MESSAGE =
            "<col=ff3045>The Phantom Muspah's</col> <col=00e6e6>prayer shield</col> " +
                "<col=ff3045>mitigates regular damage.</col>"

        private const val RANGED_MAX_HIT = 61
        private const val MELEE_MAX_HIT = 34
        private const val MAGIC_MAX_HIT = 72
        private const val TELEPORT_MAX_HIT = 20

        private const val TELEPORT_STEP_TICKS = 1
        private const val ARENA_RADIUS = 8
        private const val RANDOM_TILE_ATTEMPTS = 5

        private const val SPIKE_LOC = "loc.muspah_spike"
        private const val SPIKE_TELEGRAPH_LOC = "loc.muspah_spike_pre"

        private const val SPIKE_WARNING_SPOTANIM = "spotanim.vfx_muspah_spike_warning_01"
        private const val SPIKE_SPAWN_SPOTANIM = "spotanim.vfx_muspah_spike_spawn_01"

        private const val SPIKE_PRE_DELAY = 2
        private const val SPIKE_SOLIDIFY_DELAY = 1

        private const val CAM_SHAKE_RADIUS = 15
        private const val CAM_SHAKE_RANDOM_X = 5
        private const val CAM_SHAKE_RANDOM_Y = 8
        private const val CAM_SHAKE_RANDOM_Z = 8
        private const val CAM_SHAKE_DURATION = 2

        private const val CLIENT_CYCLES_PER_TICK = 30
        private const val ANGLE_STEPS = 2048

        private const val SPIKE_SCATTER_MIN = 2
        private const val SPIKE_SCATTER_MAX = 4
        private const val SPIKE_DAMAGE_MIN = 15
        private const val SPIKE_DAMAGE_MAX = 25
        private const val SPIKE_HEAL_FRACTION = 0.75

        private val RANGED_PROJECTILE_CONFIG =
            ProjectileConfig(
                startHeight = 32,
                endHeight = 25,
                startDelay = 26,
                travelTime = 34,
                angle = 5,
                progress = 244,
                stepMultiplier = 0,
            )

        private const val RANGED_ATTACK_WEIGHT = 5
        private const val RANGED_MAGIC_WEIGHT = 1

        private const val MAGIC_PROJECTILE_WINDUP = 2
        private const val MAGIC_PROJECTILE_SPOTANIM = "spotanim.projectile_muspah_attack_magic_01"
        private const val MAGIC_IMPACT_SPOTANIM = "spotanim.impact_muspah_attack_magic_01"

        private val MAGIC_PROJECTILE_CONFIG =
            ProjectileConfig(
                startHeight = 100,
                endHeight = 24,
                startDelay = 24,
                travelTime = 30,
                angle = 5,
                progress = 0,
                stepMultiplier = 0,
            )

        private const val HAZARD_BATCH_MIN = 2
        private const val HAZARD_BATCH_MAX = 4
        private const val HAZARD_DRIFT_MIN = 3
        private const val HAZARD_DRIFT_MAX = 6
        private const val HAZARD_MAX_HIT = 20

        private val HAZARD_DIRECTIONS =
            listOf(
                1 to 0,
                -1 to 0,
                0 to 1,
                0 to -1,
                1 to 1,
                1 to -1,
                -1 to 1,
                -1 to -1,
            )

        private const val RANGED_HIT_DELAY = 3

        private const val PROTECT_FROM_MAGIC = "varbit.prayer_protectfrommagic"

        // Fixed 8-tile loop confirmed identical across multiple kills/captures, offsets relative
        // to the boss's arena spawn tile.
        private val TELEPORT_LOOP =
            listOf(
                10 to -1,
                -4 to 5,
                1 to -9,
                8 to 5,
                -6 to -1,
                7 to -7,
                2 to 4,
                -5 to -8,
            )

        // The 9th, non-attacking recovery jump that follows the loop - identical destination
        // confirmed across multiple captures.
        private val TELEPORT_RETURN_OFFSET = 6 to 5
    }
}
