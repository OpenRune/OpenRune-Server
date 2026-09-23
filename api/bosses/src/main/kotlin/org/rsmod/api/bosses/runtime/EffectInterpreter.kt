package org.rsmod.api.bosses.runtime

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ProjAnimType
import dev.openrune.types.aconverted.SpotanimType
import org.rsmod.api.bosses.spec.*
import org.rsmod.api.bosses.spec.HitType as BossHitType
import org.rsmod.api.combat.commons.CombatEffects
import org.rsmod.api.combat.commons.DragonfireProtection
import org.rsmod.api.combat.commons.player.combatPlayDefendAnim
import org.rsmod.api.combat.commons.player.finishNpcHit
import org.rsmod.api.combat.commons.player.queueCombatRetaliate
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.disablePrayers
import org.rsmod.api.player.hit.modifier.PlayerHitModifier
import org.rsmod.api.player.hit.queueImpactHit
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.output.Camera
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.hit.HitType
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid

class EffectInterpreter(
    private val npc: Npc,
    private val target: Player,
    private val spec: BossSpec,
    private val encounter: BossEncounter,
    private val deps: BossDeps,
) {
    private var impactTile: CoordGrid? = null

    fun run(access: StandardNpcAccess, effect: Effect, onComplete: () -> Unit = {}) {
        when (effect) {
            is Effect.Anim -> access.anim(effect.seq)
            is Effect.Say -> access.say(effect.text)
            is Effect.Sound -> {
                deps.worldRepo.soundArea(npc.coords, effect.synth, radius = effect.radius)
            }
            is Effect.Spotanim -> access.spotanim(effect.spot, effect.delay, effect.height)
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
                for (player in deps.playerList) {
                    if (player.coords.chebyshevDistance(npc.coords) <= effect.radius) {
                        Camera.camShake(player, effect.axis, effect.random, effect.amplitude, effect.rate)
                    }
                }
            }
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

            is Effect.Hit -> applyHit(effect)
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
                    access.changeType(npcType, effect.durationTicks)
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
                val abilityName = encounter.selectAbility(effect.selector, deps.mapClock.cycle)
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
                    val subInterpreter = EffectInterpreter(npc, t, spec, encounter, deps)
                    subInterpreter.run(access, effect.effect) {
                        remaining--
                        if (remaining == 0) onComplete()
                    }
                }
                return
            }
        }
        onComplete()
    }

    private fun scheduleWait(ticks: Int, onComplete: () -> Unit) {
        require(ticks > 0) { "`ticks` must be greater than 0. (ticks=$ticks)" }
        deps.suppressAttacks(npc, ticks)
        deps.worldQueues.add(ticks) { if (npc.isValidTarget()) onComplete() }
    }

    private fun runSequence(
        access: StandardNpcAccess,
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

    private fun runParallel(access: StandardNpcAccess, effects: List<Effect>, onComplete: () -> Unit) {
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
        access: StandardNpcAccess,
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
                    deps.worldQueues.add(gap) { step(remaining - 1) }
                } else {
                    step(remaining - 1)
                }
            }
        }
        step(deps.random.of(times))
    }

    private fun applyHit(hit: Effect.Hit) {
        val targets = when (val t = hit.target) {
            is TargetExpr.Single -> listOfNotNull(resolveSingle(t))
            is TargetExpr.Multi -> resolveMulti(t)
            else -> listOf(target)
        }
        val delay = hit.delay.coerceAtLeast(1)
        for (t in targets) {
            var damage = evaluateDamage(hit.damage, hit.type, t)
            dragonfireType(hit.type)?.let { dfType ->
                val cap = DragonfireProtection.resolveMaxHit(t, dfType, damageMax(hit.damage, hit.type, t))
                damage = if (cap <= 0) 0 else deps.random.of(cap + 1)
            }
            if (damage > 0) {
                hit.spotanim?.let { t.spotanim(it, height = hit.spotanimHeight) }
            }
            t.finishNpcHit(npc, delay, hit.type.toEngine(), damage, deps.playerHitModifier, hit.penetration)
        }
    }

    private fun fireProjectile(access: StandardNpcAccess, proj: Effect.Projectile) {
        val targetExpr = proj.target as? TargetExpr.Single ?: TargetExpr.CurrentTarget
        val player = resolveSingle(targetExpr)
        val destCoord = player?.coords ?: resolveTile(targetExpr)
        val spotId = proj.spotanim.asRSCM(RSCMType.SPOTANIM)

        val type = if (proj.travel != null) {
            ServerCacheManager.getProjectile(proj.travel.asRSCM(RSCMType.PROJANIM))
                ?: error("Projectile not found: ${proj.travel}")
        } else {
            val cfg = proj.config ?: ProjectileConfig()
            ProjAnimType(
                startHeight = cfg.startHeight,
                endHeight = cfg.endHeight,
                delay = cfg.startDelay,
                angle = cfg.angle,
                lengthAdjustment = cfg.travelTime,
                progress = cfg.progress,
                stepMultiplier = cfg.stepMultiplier,
            )
        }

        proj.launch?.let { access.spotanim(it) }

        val projAnim =
            if (player != null) {
                ProjAnim.fromNpcToPlayer(npc, player, spotId, type)
            } else {
                ProjAnim.fromNpcToCoord(npc, destCoord, spotId, type)
            }
        deps.worldRepo.projAnim(projAnim)

        proj.impact?.let { impactSpot ->
            val spot = SpotanimType(impactSpot.asRSCM(RSCMType.SPOTANIM))
            deps.worldQueues.add(projAnim.serverCycles) { deps.worldRepo.spotanimMap(spot, destCoord) }
        }

        proj.onImpact?.let { onImpact ->
            deps.worldQueues.add(projAnim.serverCycles) {
                runWithImpactTile(destCoord) { run(access, onImpact) }
            }
        }

        if (player == null) return

        proj.hit?.let { hit ->
            var damage = evaluateDamage(hit.damage, hit.type, player)
            dragonfireType(hit.type)?.let { dfType ->
                val cap = DragonfireProtection.resolveMaxHit(player, dfType, damageMax(hit.damage, hit.type, player))
                damage = if (cap <= 0) 0 else deps.random.of(cap + 1)
            }
            if (damage > 0) {
                hit.spotanim?.let { player.spotanim(it, delay = projAnim.clientCycles, height = hit.spotanimHeight) }
            }
            if (proj.resolveOnImpact) {
                player.finishNpcImpactHit(
                    npc,
                    projAnim.serverCycles,
                    hit.type.toEngine(),
                    damage,
                    deps.playerHitModifier,
                    hit.penetration,
                )
            } else {
                player.finishNpcHit(
                    npc,
                    projAnim.serverCycles,
                    hit.type.toEngine(),
                    damage,
                    deps.playerHitModifier,
                    hit.penetration,
                )
            }
        }
    }

    private fun runWithImpactTile(coord: CoordGrid, block: () -> Unit) {
        val previous = impactTile
        impactTile = coord
        block()
        impactTile = previous
    }

    private fun Player.finishNpcImpactHit(
        source: Npc,
        delay: Int,
        type: HitType,
        damage: Int,
        modifier: PlayerHitModifier,
        penetration: Int = 0,
    ) {
        queueCombatRetaliate(source)
        queueImpactHit(source, delay, type, damage, modifier, penetration = penetration)
        combatPlayDefendAnim()
    }

    private fun applyTileAoE(aoe: Effect.TileAoE) {
        val center = resolveTile(aoe.center)
        val radius = aoe.radius
        val targets = deps.playerList.filter {
            it.coords.chebyshevDistance(center) <= radius
        }
        for (t in targets) {
            val damage = evaluateDamage(aoe.damage, aoe.type, t)
            t.finishNpcHit(npc, 0, aoe.type.toEngine(), damage, deps.playerHitModifier)
        }
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

    private fun summon(access: StandardNpcAccess, summon: Effect.Summon) {
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
        val targets = when (val t = effect.target) {
            is TargetExpr.Single -> listOfNotNull(resolveSingle(t))
            is TargetExpr.Multi -> resolveMulti(t)
            else -> listOf(target)
        }
        for (t in targets) {
            t.mes(effect.text)
        }
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
                CombatEffects.statDrain(target, listOf(entry.stat), entry.amount)
            }
        }
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
            is TargetExpr.SpawnTile -> null
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
            is TargetExpr.ImpactTile -> impactTile ?: npc.coords
            is TargetExpr.SpawnTile -> npc.spawnCoords.translate(expr.dx, expr.dz)
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
            is TargetExpr.Single -> listOfNotNull(resolveSingle(expr))
            else -> listOf(target)
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
