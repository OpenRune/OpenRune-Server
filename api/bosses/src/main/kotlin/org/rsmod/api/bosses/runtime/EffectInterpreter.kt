package org.rsmod.api.bosses.runtime

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcServerType
import dev.openrune.types.ProjAnimType
import dev.openrune.types.aconverted.SpotanimType
import kotlin.math.abs
import kotlin.math.sign
import org.rsmod.annotations.InternalApi
import org.rsmod.api.bosses.spec.*
import org.rsmod.api.bosses.spec.HitType as BossHitType
import org.rsmod.api.combat.commons.CombatEffects
import org.rsmod.api.combat.commons.DragonfireProtection
import org.rsmod.api.combat.commons.player.combatPlayDefendAnim
import org.rsmod.api.combat.commons.player.finishNpcHit
import org.rsmod.api.combat.commons.player.queueCombatRetaliate
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.heal
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.disablePrayers
import org.rsmod.api.player.hit.modifier.PlayerHitModifier
import org.rsmod.api.player.hit.modify
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.hit.queueImpactHit
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.output.Camera
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.statDrain
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.EntityExactMove
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.hit.HitType
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid
import org.rsmod.map.util.Bounds

/** The [BossEncounter.epoch] one ability run belongs to, shared with the interpreters it spawns. */
class AbilityRun internal constructor(internal var epoch: Int)

class EffectInterpreter(
    private val npc: Npc,
    private val target: Player,
    private val spec: BossSpec,
    private val encounter: BossEncounter,
    private val deps: BossDeps,
    private val abilityRun: AbilityRun = AbilityRun(encounter.epoch),
    boundTile: CoordGrid? = null,
) {
    private var impactTile: CoordGrid? = boundTile

    private val interrupted: Boolean
        get() = encounter.epoch != abilityRun.epoch

    fun run(access: StandardNpcAccess?, effect: Effect, onComplete: () -> Unit = {}) {
        when (effect) {
            is Effect.Anim -> npc.anim(effect.seq, effect.delay)
            is Effect.IdleAnim -> effect.seq?.let(npc::setIdleAnim) ?: npc.clearIdleAnim()
            is Effect.ResetAnim -> npc.resetAnim()
            is Effect.ForceNext -> encounter.forceNext(effect.ability)
            is Effect.Say -> npc.say(effect.text)
            is Effect.Sound -> {
                val at = effect.at?.let(::resolveTile) ?: npc.coords
                deps.worldRepo.soundArea(at, effect.synth, delay = effect.delay, radius = effect.radius)
            }
            is Effect.SoundTo -> {
                for (t in resolvePlayers(effect.target)) {
                    t.soundSynth(effect.synth, effect.loops, effect.delay)
                }
            }
            is Effect.Spotanim -> npc.spotanim(effect.spot, effect.delay, effect.height)
            is Effect.MapSpotanim -> {
                val coord = resolveTile(effect.at)
                val spot = SpotanimType(effect.spot.asRSCM(RSCMType.SPOTANIM))
                deps.worldRepo.spotanimMap(spot, coord, effect.height, effect.delay)
            }
            is Effect.Broadcast -> {
                val radius = effect.radius
                for (player in deps.playerList) {
                    if (player.coords.chebyshevDistance(npc.coords) <= radius) {
                        player.mes(effect.text)
                    }
                }
            }
            is Effect.CamShake -> {
                val players =
                    effect.target?.let(::resolvePlayers)
                        ?: deps.playerList.filter { it.coords.chebyshevDistance(npc.coords) <= effect.radius }
                for (player in players) {
                    val random = effect.randomMax?.let { deps.random.of(effect.random, it) } ?: effect.random
                    Camera.camShake(player, effect.axis, random, effect.amplitude, effect.rate)
                }
            }
            is Effect.CamReset -> resolvePlayers(effect.target).forEach(Camera::camReset)
            is Effect.Delay -> {
                scheduleWait(effect.ticks, onComplete)
                return
            }
            is Effect.Wait -> {
                scheduleWait(effect.ticks, onComplete)
                return
            }
            is Effect.NoOp -> {}
            is Effect.Message -> applyMessage(effect)

            is Effect.Hit -> applyHit(access, effect)
            is Effect.Projectile -> fireProjectile(access, effect)
            is Effect.TileAoE -> applyTileAoE(effect)
            is Effect.Debris -> applyDebris(effect)
            is Effect.Summon -> summon(access, effect)
            is Effect.Poison -> applyPoison(effect)
            is Effect.Freeze -> applyFreeze(effect)
            is Effect.DisablePrayers -> target.disablePrayers()
            is Effect.StatDrain -> applyStatDrain(effect)
            is Effect.Transmog -> {
                val npcType = ServerCacheManager.getNpc(effect.to.asRSCM(RSCMType.NPC))
                if (npcType != null) {
                    transmog(npcType, effect.durationTicks)
                }
            }

            is Effect.Teleport -> {
                if (npc.isValidTarget()) {
                    PathingEntityCommon.telejump(npc, deps.collision, resolveTile(effect.to))
                }
            }
            is Effect.FaceTarget -> {
                if (npc.isValidTarget()) {
                    npc.resetFaceEntity()
                    if (target.isValidTarget()) npc.facePlayer(target)
                }
            }
            is Effect.FaceTile -> {
                if (npc.isValidTarget()) {
                    npc.faceSquare(resolveTile(effect.at))
                    npc.resetFaceEntity()
                }
            }

            is Effect.Run -> {
                val ability = spec.abilities[effect.ability]
                if (ability != null) run(access, ability, onComplete) else onComplete()
                return
            }
            is Effect.TransitionTo -> {
                encounter.transitionTo(effect.phase, deps.mapClock.cycle)
            }
            is Effect.External -> {
                deps.extensionRegistry.invoke(effect.handler, access, npc, target, effect.params)
            }

            is Effect.Sequence -> {
                runSequence(access, effect.effects, 0, onComplete)
                return
            }
            is Effect.Parallel -> {
                runParallel(access, effect.effects, onComplete)
                return
            }
            is Effect.Choose -> {
                val abilityName = encounter.pick(effect.selector, deps.mapClock.cycle, target)
                val branch = abilityName?.let { effect.branches[it] }
                if (branch != null) run(access, branch, onComplete) else onComplete()
                return
            }
            is Effect.Repeat -> {
                runRepeat(access, effect.times, effect.effect, effect.gap, onComplete)
                return
            }
            is Effect.Whenever -> {
                val next = if (encounter.evaluate(effect.condition, target)) effect.then else effect.otherwise
                run(access, next, onComplete)
                return
            }
            is Effect.OnEach -> {
                val targets = resolveMulti(effect.targets)
                if (targets.isEmpty()) {
                    onComplete()
                    return
                }
                var remaining = targets.size
                for (t in targets) {
                    val subInterpreter = EffectInterpreter(npc, t, spec, encounter, deps, abilityRun, impactTile)
                    subInterpreter.run(access, effect.effect) {
                        remaining--
                        if (remaining == 0) onComplete()
                    }
                }
                return
            }

            is Effect.SetVarn -> npc.vars[effect.varn] = evaluateVar(effect.value)
            is Effect.Switch -> {
                val next = effect.cases[npc.vars[effect.varn]] ?: effect.otherwise
                run(access, next, onComplete)
                return
            }
            is Effect.Interrupt -> {
                encounter.interrupt(deps.mapClock.cycle)
                abilityRun.epoch = encounter.epoch
            }
            is Effect.OnTiles -> {
                val tiles = resolveTiles(effect.tiles)
                if (tiles.isEmpty()) {
                    onComplete()
                    return
                }
                var remaining = tiles.size
                for (tile in tiles) {
                    val sub = EffectInterpreter(npc, target, spec, encounter, deps, abilityRun, tile)
                    sub.run(access, effect.effect) {
                        remaining--
                        if (remaining == 0) onComplete()
                    }
                }
                return
            }
            is Effect.After -> {
                deps.worldQueues.add(effect.ticks) {
                    val alive = npc.isSlotAssigned && npc.hitpoints > 0
                    if (alive || !effect.requireAlive) run(access, effect.effect)
                }
            }
            is Effect.SpawnLoc -> {
                deps.spawnOwnedLoc(npc, resolveTile(effect.at), effect.loc, effect.angle, effect.blockPlayersOnly)
            }
            is Effect.Knockback -> knockback(effect)
        }
        onComplete()
    }

    @OptIn(InternalApi::class)
    private fun transmog(type: NpcServerType, duration: Int) {
        npc.transmog(type, duration)
        npc.assignUid()
    }

    private fun evaluateVar(expr: VarExpr): Int =
        when (expr) {
            is VarExpr.Const -> expr.value
            is VarExpr.Varn -> npc.vars[expr.varn]
            is VarExpr.Plus -> evaluateVar(expr.a) + evaluateVar(expr.b)
            is VarExpr.Min -> minOf(evaluateVar(expr.a), evaluateVar(expr.b))
            is VarExpr.Max -> maxOf(evaluateVar(expr.a), evaluateVar(expr.b))
            is VarExpr.BearingTo -> Angles.bearing(resolveTile(expr.from), resolveTile(expr.to))
        }

    private fun scheduleWait(ticks: Int, onComplete: () -> Unit) {
        require(ticks > 0) { "`ticks` must be greater than 0. (ticks=$ticks)" }
        deps.suppressAttacks(npc, ticks)
        deps.worldQueues.add(ticks) { if (npc.isValidTarget() && !interrupted) onComplete() }
    }

    private fun runSequence(
        access: StandardNpcAccess?,
        effects: List<Effect>,
        index: Int,
        onComplete: () -> Unit,
    ) {
        if (index >= effects.size) {
            onComplete()
            return
        }
        run(access, effects[index]) { runSequence(access, effects, index + 1, onComplete) }
    }

    private fun runParallel(access: StandardNpcAccess?, effects: List<Effect>, onComplete: () -> Unit) {
        if (effects.isEmpty()) {
            onComplete()
            return
        }
        var remaining = effects.size
        for (e in effects) {
            run(access, e) {
                remaining--
                if (remaining == 0) onComplete()
            }
        }
    }

    private fun runRepeat(
        access: StandardNpcAccess?,
        times: IntRange,
        effect: Effect,
        gap: Int,
        onComplete: () -> Unit,
    ) {
        fun step(remaining: Int) {
            if (remaining <= 0) {
                onComplete()
                return
            }
            run(access, effect) {
                if (remaining > 1 && gap > 0) {
                    deps.worldQueues.add(gap) { if (!interrupted) step(remaining - 1) }
                } else {
                    step(remaining - 1)
                }
            }
        }
        step(deps.random.of(times))
    }

    private fun applyHit(access: StandardNpcAccess?, hit: Effect.Hit) {
        val targets = when (val t = hit.target) {
            is TargetExpr.Single -> listOfNotNull(resolveSingle(t))
            is TargetExpr.Multi -> resolveMulti(t)
            else -> listOf(target)
        }
        val delay = hit.delay.coerceAtLeast(1)
        for (t in targets) {
            val damage = rollDamage(hit, t)
            if (damage > 0) {
                hit.spotanim?.let { t.spotanim(it, delay = hit.spotanimDelay ?: 0, height = hit.spotanimHeight) }
            }
            if (hit.hazard) {
                t.queueHit(npc, delay, hit.type.toEngine(), damage, deps.playerHitModifier)
                continue
            }
            val landed =
                t.finishNpcHit(npc, delay, hit.type.toEngine(), damage, deps.playerHitModifier, hit.penetration)
            scheduleLanding(access, hit, t, damage, landed.damage, delay, clientDelay = 0)
        }
    }

    private fun rollDamage(hit: Effect.Hit, t: Player): Int {
        val damage = evaluateDamage(hit.damage, hit.type, t)
        val dragonfire = dragonfireType(hit.type) ?: return damage
        val max = damageMax(hit.damage, hit.type, t)
        val cap = DragonfireProtection.resolveMaxHit(t, dragonfire, max)
        return if (cap <= 0) 0 else deps.random.of(cap + 1)
    }

    private fun impactTicks(anim: ProjAnim, rounding: ImpactRounding): Int =
        when (rounding) {
            ImpactRounding.Down -> anim.serverCycles
            ImpactRounding.Up -> ceilingImpactTicks(anim.endTime)
        }

    private fun projAnimType(proj: Effect.Projectile): ProjAnimType {
        proj.travel?.let {
            return ServerCacheManager.getProjectile(it.asRSCM(RSCMType.PROJANIM))
                ?: error("Projectile not found: $it")
        }
        val cfg = proj.config ?: ProjectileConfig()
        return ProjAnimType(
            startHeight = cfg.startHeight,
            endHeight = cfg.endHeight,
            delay = cfg.startDelay,
            angle = cfg.angle,
            lengthAdjustment = cfg.travelTime,
            progress = cfg.progress,
            stepMultiplier = cfg.stepMultiplier,
        )
    }

    private fun buildProjAnim(
        proj: Effect.Projectile,
        type: ProjAnimType,
        spotanim: Int,
        player: Player?,
        dest: CoordGrid,
    ): ProjAnim {
        val from = proj.from?.let(::resolveTile)
        if (from == null) {
            return if (player != null) {
                ProjAnim.fromNpcToPlayer(npc, player, spotanim, type)
            } else {
                ProjAnim.fromNpcToCoord(npc, dest, spotanim, type)
            }
        }
        return ProjAnim(
            spotanim = spotanim,
            startHeight = type.startHeight,
            endHeight = type.endHeight,
            startTime = type.delay,
            endTime = ProjAnim.calculateEndTime(type, Bounds(from).distanceTo(Bounds(dest))),
            angle = type.angle,
            progress = type.progress,
            sourceIndex = 0,
            targetIndex = player?.let { -(it.slotId + 1) } ?: 0,
            startCoord = from,
            endCoord = dest,
        )
    }

    private fun fireProjectile(access: StandardNpcAccess?, proj: Effect.Projectile) {
        val targetExpr = proj.target as? TargetExpr.Single ?: TargetExpr.CurrentTarget
        val player = resolveSingle(targetExpr)
        val destCoord = player?.coords ?: resolveTile(targetExpr)
        val type = projAnimType(proj)

        proj.launch?.let { npc.spotanim(it) }

        val spotanimId = proj.spotanim.asRSCM(RSCMType.SPOTANIM)
        val projAnim = buildProjAnim(proj, type, spotanimId, player, destCoord)
        deps.worldRepo.projAnim(projAnim)
        val ticks = impactTicks(projAnim, proj.impactRounding)

        proj.impact?.let { impactSpot ->
            val spot = SpotanimType(impactSpot.asRSCM(RSCMType.SPOTANIM))
            deps.worldQueues.add(ticks) { deps.worldRepo.spotanimMap(spot, destCoord) }
        }

        proj.onImpact?.let { onImpact ->
            deps.worldQueues.add(ticks) { runWithImpactTile(destCoord) { run(access, onImpact) } }
        }

        val hit = proj.hit ?: return
        if (player == null) return
        val damage = rollDamage(hit, player)
        if (damage > 0 && !hit.spotanimUnlessPraying) {
            val delay = hit.spotanimDelay ?: projAnim.clientCycles
            hit.spotanim?.let { player.spotanim(it, delay = delay, height = hit.spotanimHeight) }
        }
        if (proj.resolveOnImpact) {
            val conditionalPenetration = hit.penetrationWhen != null
            player.queueCombatRetaliate(npc, ticks)
            player.queueImpactHit(
                npc,
                ticks,
                hit.type.toEngine(),
                damage,
                impactModifier(access, hit, damage),
                penetration = if (conditionalPenetration) 0 else hit.penetration,
            )
            showMissSpotanim(hit, player, damage, projAnim.clientCycles)
            return
        }
        val hitType = hit.type.toEngine()
        val landed =
            player.finishNpcHit(npc, ticks, hitType, damage, deps.playerHitModifier, hit.penetration).damage
        scheduleLanding(access, hit, player, damage, landed, ticks, projAnim.clientCycles)
    }

    /** Resolves prayer, penetration, the defend anim and lifesteal/on-hit on the impact tick. */
    private fun impactModifier(
        access: StandardNpcAccess?,
        hit: Effect.Hit,
        rolled: Int,
    ): PlayerHitModifier {
        val onHit = landingEffect(hit, rolled)
        return PlayerHitModifier { t ->
            val whenever = hit.penetrationWhen
            if (whenever != null && encounter.evaluate(whenever, t)) penetration = hit.penetration
            val praying = encounter.evaluate(Condition.TargetPraying(hit.type), t)
            deps.playerHitModifier.modify(this, t)
            t.combatPlayDefendAnim()
            if (hit.spotanimUnlessPraying && !praying) {
                hit.spotanim?.let { t.spotanim(it, height = hit.spotanimHeight) }
            }
            if (!npc.isValidTarget()) return@PlayerHitModifier
            val heal = damage * hit.lifesteal / 100
            if (heal > 0) npc.heal(heal)
            onHit?.let { EffectInterpreter(npc, t, spec, encounter, deps, abilityRun).run(access, it) }
        }
    }

    /**
     * Schedules a hit's landing extras: the miss graphic (sent now with a client delay, like
     * [Effect.Hit.spotanim]), and [Effect.Hit.onHit] / [Effect.Hit.lifesteal] once the hit lands.
     */
    private fun scheduleLanding(
        access: StandardNpcAccess?,
        hit: Effect.Hit,
        t: Player,
        rolled: Int,
        landed: Int,
        serverDelay: Int,
        clientDelay: Int,
    ) {
        showMissSpotanim(hit, t, rolled, clientDelay)
        val onHit = landingEffect(hit, rolled)
        val heal = landed * hit.lifesteal / 100
        if (onHit == null && heal <= 0) return
        deps.worldQueues.add(serverDelay) {
            if (!npc.isValidTarget()) return@add
            if (heal > 0) npc.heal(heal)
            onHit?.let { run(access, it) }
        }
    }

    private fun landingEffect(hit: Effect.Hit, rolled: Int): Effect? =
        hit.onHit?.takeIf { hit.onHitEvenOnMiss || rolled > 0 }

    private fun showMissSpotanim(hit: Effect.Hit, t: Player, rolled: Int, clientDelay: Int) {
        if (rolled <= 0) {
            hit.missSpotanim?.let { t.spotanim(it, delay = clientDelay, height = hit.spotanimHeight) }
        }
    }

    private fun runWithImpactTile(coord: CoordGrid, block: () -> Unit) {
        val previous = impactTile
        impactTile = coord
        block()
        impactTile = previous
    }

    private fun applyTileAoE(aoe: Effect.TileAoE) {
        val tiles = aoe.tiles(npc, target).toSet()
        val windup = aoe.telegraph?.windup ?: 0
        aoe.telegraph?.let { telegraph ->
            val spot = SpotanimType(telegraph.spotanim.asRSCM(RSCMType.SPOTANIM))
            for (tile in tiles) deps.worldRepo.spotanimMap(spot, tile)
        }
        val strike = {
            val targets = deps.playerList.filter { it.coords in tiles }
            for (t in targets) {
                val damage = evaluateDamage(aoe.damage, aoe.type, t)
                t.finishNpcHit(npc, 1, aoe.type.toEngine(), damage, deps.playerHitModifier)
            }
        }
        if (windup > 0) deps.worldQueues.add(windup) { strike() } else strike()
    }

    private fun applyDebris(effect: Effect.Debris) {
        val center = resolveTile(effect.center)
        val telegraphSpot = SpotanimType(effect.telegraph.asRSCM(RSCMType.SPOTANIM))

        val playersInRange =
            deps.playerList.filter { it.coords.chebyshevDistance(center) <= effect.targetRadius }
        val total = effect.count.first + deps.random.of(effect.count.last - effect.count.first + 1)
        val scatterCount = (total - playersInRange.size).coerceAtLeast(0)
        val scatterDiameter = effect.scatterRadius * 2 + 1
        val tiles =
            playersInRange.map { it.coords } +
                List(scatterCount) {
                    center.translate(
                        deps.random.of(scatterDiameter) - effect.scatterRadius,
                        deps.random.of(scatterDiameter) - effect.scatterRadius,
                    )
                }
        val tileSet = tiles.toSet()

        tiles.forEach { deps.worldRepo.spotanimMap(telegraphSpot, it) }

        deps.worldQueues.add(effect.windup) {
            effect.impact?.let { impact ->
                val impactSpot = SpotanimType(impact.asRSCM(RSCMType.SPOTANIM))
                tileSet.forEach { deps.worldRepo.spotanimMap(impactSpot, it) }
            }
            val landing =
                deps.playerList.filter { it.coords.chebyshevDistance(center) <= effect.targetRadius }
            for (player in landing) {
                if (player.hitpoints > 0 && player.coords in tileSet) {
                    val damage = evaluateDamage(effect.damage, effect.type, player)
                    player.finishNpcHit(npc, 1, effect.type.toEngine(), damage, deps.playerHitModifier)
                }
            }
        }
    }

    private fun summon(access: StandardNpcAccess?, summon: Effect.Summon) {
        val npcTypeId = summon.npc.asRSCM(RSCMType.NPC)
        val npcType = ServerCacheManager.getNpc(npcTypeId) ?: return
        val center = resolveTile(summon.centeredOn)
        val radius = summon.radius
        val mode = summon.mode ?: npcType.defaultMode

        // Only consider tiles that are walkable
        val spawnTiles =
            buildList {
                    for (dx in -radius..radius) {
                        for (dz in -radius..radius) {
                            val origin = center.translate(dx, dz)
                            if (canStand(origin, npcType.size)) {
                                add(origin)
                            }
                        }
                    }
                }
                .toMutableList()

        repeat(summon.count) {
            val spawnCoord =
                if (spawnTiles.isNotEmpty()) {
                    spawnTiles.removeAt(deps.random.of(spawnTiles.size))
                } else {
                    center
                }
            val spawned = Npc(npcType, spawnCoord)
            spawned.mode = mode
            deps.npcRepo.add(spawned, summon.duration)
            if (summon.owned) encounter.addOwnedNpc(spawned)
            summon.onSummon?.let { handler ->
                deps.extensionRegistry.invoke(handler, access, spawned, target, summon.onSummonParams)
            }
        }
    }

    /** Whether an npc of [size] can stand at [origin]. */
    private fun canStand(origin: CoordGrid, size: Int): Boolean {
        for (dx in 0 until size) {
            for (dz in 0 until size) {
                if (deps.collision.isWalkBlocked(origin.translate(dx, dz))) {
                    return false
                }
            }
        }
        return true
    }

    private fun applyMessage(effect: Effect.Message) {
        for (t in resolvePlayers(effect.target)) {
            t.mes(effect.text)
        }
    }

    private fun resolvePlayers(expr: TargetExpr): List<Player> =
        when (expr) {
            is TargetExpr.Single -> listOfNotNull(resolveSingle(expr))
            is TargetExpr.Multi -> resolveMulti(expr)
        }

    private fun applyPoison(effect: Effect.Poison) {
        if (deps.random.of(effect.outOf) < effect.chance) {
            CombatEffects.poison(target, effect.damage)
        }
    }

    private fun applyFreeze(effect: Effect.Freeze) {
        if (deps.random.of(effect.outOf) < effect.chance) {
            CombatEffects.freeze(target, effect.ticks)
        }
    }

    private fun applyStatDrain(effect: Effect.StatDrain) {
        for (entry in effect.entries) {
            if (deps.random.of(entry.outOf) < entry.chance) {
                if (entry.percent == 0) {
                    CombatEffects.statDrain(target, listOf(entry.stat), entry.amount)
                } else {
                    target.statDrain(entry.stat, entry.amount, entry.percent)
                }
            }
        }
    }

    private class TileBox(val sw: CoordGrid, val ne: CoordGrid) {
        fun contains(tile: CoordGrid): Boolean =
            tile.level == sw.level && tile.x in sw.x..ne.x && tile.z in sw.z..ne.z
    }

    private fun resolveArea(area: Area): TileBox = TileBox(resolveTile(area.sw), resolveTile(area.ne))

    private fun isFree(box: TileBox, tile: CoordGrid): Boolean =
        box.contains(tile) && !deps.collision.isWalkBlocked(tile) && !encounter.ownsLocAt(tile)

    private fun resolveTiles(set: TileSet): List<CoordGrid> {
        val box = resolveArea(set.area)
        return when (set) {
            is TileSet.RandomFree -> {
                val free = buildList {
                    for (x in box.sw.x..box.ne.x) {
                        for (z in box.sw.z..box.ne.z) {
                            val tile = CoordGrid(x, z, box.sw.level)
                            if (isFree(box, tile)) add(tile)
                        }
                    }
                }
                free.shuffled().take(deps.random.of(set.count))
            }
            is TileSet.UnderPlayers ->
                deps.playerList
                    .filter { it.isValidTarget() && box.contains(it.coords) }
                    .map { it.coords }
                    .filter { isFree(box, it) }
            is TileSet.Nearest -> set.tiles.mapNotNull { nearestFree(box, resolveTile(it), set.searchRadius) }
        }
    }

    private fun nearestFree(box: TileBox, tile: CoordGrid, searchRadius: Int): CoordGrid? {
        if (isFree(box, tile)) return tile
        for (radius in 1..searchRadius) {
            for (dx in -radius..radius) {
                for (dz in -radius..radius) {
                    if (maxOf(abs(dx), abs(dz)) != radius) continue
                    val candidate = tile.translate(dx, dz)
                    if (isFree(box, candidate)) return candidate
                }
            }
        }
        return null
    }

    private fun knockback(effect: Effect.Knockback) {
        val box = resolveArea(effect.within)
        val from = target.coords
        val dest =
            KNOCKBACK_DIRECTIONS.shuffled()
                .map { (dx, dz) -> from.translate(dx, dz) }
                .firstOrNull { isFree(box, it) } ?: return
        target.anim(effect.anim)
        PathingEntityCommon.teleport(target, deps.collision, dest)
        target.pendingExactMove =
            EntityExactMove(
                deltaX1 = from.x - dest.x,
                deltaZ1 = from.z - dest.z,
                deltaX2 = 0,
                deltaZ2 = 0,
                clientDelay1 = 0,
                clientDelay2 = 30,
                direction = Angles.bearing(dest, from),
            )
    }

    private fun resolveSingle(expr: TargetExpr.Single): Player? {
        return when (expr) {
            is TargetExpr.CurrentTarget -> target
            is TargetExpr.Self -> null
            is TargetExpr.CurrentTargetTile -> null
            is TargetExpr.HighestDamageDealer -> target
            is TargetExpr.LowestPrayer -> target
            is TargetExpr.RandomNearby -> target
            is TargetExpr.RandomWalkableTile -> null
            is TargetExpr.ImpactTile -> null
            is TargetExpr.EachTile -> null
            is TargetExpr.SpawnTile -> null
            is TargetExpr.Centre -> null
            is TargetExpr.Toward -> null
        }
    }

    private fun resolveTile(expr: TargetExpr): org.rsmod.map.CoordGrid {
        return when (expr) {
            is TargetExpr.CurrentTarget -> target.coords
            is TargetExpr.CurrentTargetTile -> target.coords
            is TargetExpr.Self -> npc.coords
            is TargetExpr.RandomWalkableTile -> {
                val center = resolveTile(expr.of)
                randomWalkableTile(center, expr.radius) ?: center
            }
            is TargetExpr.ImpactTile,
            is TargetExpr.EachTile -> impactTile ?: npc.coords
            is TargetExpr.SpawnTile -> npc.spawnCoords.translate(expr.dx, expr.dz)
            is TargetExpr.Centre -> npc.centreTile
            is TargetExpr.Toward -> resolveToward(expr, ::resolveTile)
            else -> npc.coords
        }
    }

    private fun randomWalkableTile(center: CoordGrid, radius: Int): CoordGrid? {
        val candidates = mutableListOf<CoordGrid>()
        for (dx in -radius..radius) {
            for (dz in -radius..radius) {
                val coord = center.translate(dx, dz)
                if (!deps.collision.isWalkBlocked(coord)) candidates += coord
            }
        }
        if (candidates.isEmpty()) return null
        return candidates[deps.random.of(candidates.size)]
    }

    private fun resolveMulti(expr: TargetExpr): List<Player> {
        return when (expr) {
            is TargetExpr.AllInRadius -> {
                if (expr.of is TargetExpr.Self) {
                    deps.playerList.filter {
                        it.coords.level == npc.coords.level && npc.isWithinDistance(it, expr.radius)
                    }
                } else {
                    val center = resolveTile(expr.of)
                    deps.playerList.filter {
                        it.coords.level == center.level &&
                            it.coords.chebyshevDistance(center) <= expr.radius
                    }
                }
            }
            is TargetExpr.TopN -> listOf(target)
            is TargetExpr.FacingQuadrant -> playersInFacingQuadrant(expr.reach)
            is TargetExpr.PlayersIn -> {
                val area = resolveArea(expr.area)
                deps.playerList.filter { it.isValidTarget() && area.contains(it.coords) }
            }
            is TargetExpr.PlayersOn -> {
                val tile = resolveTile(expr.tile)
                deps.playerList.filter { it.isValidTarget() && it.coords == tile }
            }
            is TargetExpr.Single -> listOfNotNull(resolveSingle(expr))
            else -> listOf(target)
        }
    }

    private fun playersInFacingQuadrant(reach: Int): List<Player> {
        val half = npc.size / 2
        val centreX = npc.coords.x + half
        val centreZ = npc.coords.z + half
        val targetDx = target.coords.x - centreX
        val targetDz = target.coords.z - centreZ
        if (targetDx == 0 && targetDz == 0) return emptyList()

        val vertical = abs(targetDz) >= abs(targetDx)
        val sign = if (vertical) targetDz.sign else targetDx.sign
        val limit = half + reach
        return deps.playerList.filter { player ->
            if (player.coords.level != npc.coords.level) return@filter false
            val dx = player.coords.x - centreX
            val dz = player.coords.z - centreZ
            if (abs(dx) > limit || abs(dz) > limit) return@filter false
            val (along, across) = if (vertical) dz to dx else dx to dz
            along * sign > 0 && abs(across) <= abs(along)
        }
    }

    private fun damageMax(expr: DamageExpr, hitType: BossHitType, t: Player): Int =
        when (expr) {
            is DamageExpr.Roll -> if (expr.range.isEmpty()) 0 else expr.range.last
            is DamageExpr.Fixed -> expr.value
            is DamageExpr.NpcMaxHit -> npcFormulaMaxHit(expr, hitType, t)
            else -> evaluateDamage(expr, hitType, t)
        }

    private fun npcFormulaMaxHit(expr: DamageExpr.NpcMaxHit, hitType: BossHitType, t: Player): Int {
        val raw =
            when (hitType) {
                BossHitType.Ranged -> deps.maxHit.getRangedMaxHit(npc, t)
                BossHitType.Magic,
                BossHitType.Dragonfire,
                BossHitType.DragonfireMetal,
                BossHitType.WyvernIce -> deps.maxHit.getMagicMaxHit(npc, t)
                BossHitType.Melee,
                BossHitType.Typeless -> deps.maxHit.getMeleeMaxHit(npc, t, expr.meleeAttackType)
            }
        val scaled = if (expr.scale == 1.0) raw else (raw * expr.scale).toInt()
        return scaled.coerceAtLeast(0)
    }

    private fun dragonfireType(t: BossHitType): DragonfireProtection.DragonfireType? =
        when (t) {
            BossHitType.Dragonfire -> DragonfireProtection.DragonfireType.Chromatic
            BossHitType.DragonfireMetal -> DragonfireProtection.DragonfireType.Metal
            BossHitType.WyvernIce -> DragonfireProtection.DragonfireType.WyvernIce
            else -> null
        }

    private fun evaluateDamage(expr: DamageExpr, hitType: BossHitType, t: Player): Int {
        return when (expr) {
            is DamageExpr.Fixed -> expr.value
            is DamageExpr.Roll ->
                if (expr.range.isEmpty()) 0
                else {
                    expr.range.first + deps.random.of(expr.range.last - expr.range.first + 1)
                }
            is DamageExpr.Accuracy -> {
                val landed = rollAccuracy(hitType, t, expr.meleeAttackType)
                evaluateDamage(if (landed) expr.on else expr.miss, hitType, t)
            }
            is DamageExpr.NpcMaxHit -> {
                val max = npcFormulaMaxHit(expr, hitType, t)
                val lo = expr.minHit.coerceIn(0, max)
                if (max <= 0) 0 else lo + deps.random.of(max - lo + 1)
            }
            is DamageExpr.PercentOfTargetHp -> (t.hitpoints * expr.fraction).toInt()
            is DamageExpr.Custom -> expr.roll(npc, t)
            is DamageExpr.Min -> minOf(evaluateDamage(expr.a, hitType, t), evaluateDamage(expr.b, hitType, t))
            is DamageExpr.Max -> maxOf(evaluateDamage(expr.a, hitType, t), evaluateDamage(expr.b, hitType, t))
        }
    }

    private fun rollAccuracy(
        hitType: BossHitType,
        t: Player,
        meleeAttackType: MeleeAttackType? = null,
    ): Boolean =
        when (hitType) {
            BossHitType.Melee -> deps.accuracy.rollMeleeAccuracy(npc, t, meleeAttackType, deps.random)
            BossHitType.Ranged -> deps.accuracy.rollRangedAccuracy(npc, t, deps.random)
            BossHitType.Magic,
            BossHitType.Dragonfire,
            BossHitType.DragonfireMetal,
            BossHitType.WyvernIce -> deps.accuracy.rollMagicAccuracy(npc, t, deps.random)
            BossHitType.Typeless -> true
        }

    private companion object {
        private val KNOCKBACK_DIRECTIONS =
            listOf(0 to 1, 1 to 0, 0 to -1, -1 to 0, 1 to 1, 1 to -1, -1 to -1, -1 to 1)
    }

    private fun BossHitType.toEngine(): HitType = when (this) {
        BossHitType.Melee -> HitType.Melee
        BossHitType.Ranged -> HitType.Ranged
        BossHitType.Magic -> HitType.Magic
        BossHitType.Dragonfire -> HitType.Magic
        BossHitType.DragonfireMetal -> HitType.Magic
        BossHitType.WyvernIce -> HitType.Magic
        BossHitType.Typeless -> HitType.Typeless
    }
}
