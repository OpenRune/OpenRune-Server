package org.rsmod.content.bosses.whisperer

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
import dev.openrune.types.NpcServerType
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import java.util.IdentityHashMap
import kotlin.math.abs
import kotlin.math.sign
import org.rsmod.annotations.InternalApi
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.runtime.bossProjectile
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.bosses.runtime.forceNext
import org.rsmod.api.bosses.runtime.suppressAttacks
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.combat.commons.player.finishNpcHit
import org.rsmod.api.config.refs.params
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.npc.hit.modifier.StandardNpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onNpcHit
import org.rsmod.api.script.onNpcQueue
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.headbar.Headbar
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.hit.HitType as EngineHitType
import org.rsmod.game.hit.Hitmark
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

internal const val WHISPERER_ATTACK_RATE = 6

class Whisperer
@Inject
constructor(
    deps: BossDeps,
    private val locRepo: LocRepository,
    private val instances: InstanceManager,
    private val protectedAccess: ProtectedAccessLauncher,
    private val npcHitModifier: StandardNpcHitModifier,
    private val npcDeath: NpcDeath,
) : BossPluginScript(deps) {
    private val chargeTimers: MutableMap<Npc, ChargeTimer> = IdentityHashMap()
    private val specialCleanups: MutableMap<Npc, () -> Unit> = IdentityHashMap()

    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps, onModifyHit = { onBossHit(npc, hit) })
        registerEnrageGuard()
        registerDeath()

        val spawnId = SPAWN_NPC.asRSCM(RSCMType.NPC)
        onEvent<NpcStateEvents.Create> { if (npc.type.id == spawnId) npc.anim(SPAWN_IDLE_SEQ) }
        onEvent<NpcStateEvents.Respawn> {
            if (npc.type.id == spawnId) npc.anim(SPAWN_IDLE_SEQ)
        }
        onEvent<NpcStateEvents.Delete> {
            specialCleanups.remove(npc)
            chargeTimers.remove(npc)
        }
        deps.extensionRegistry.register(ENRAGED_TENTACLE) { _, npc, target, _ ->
            if (target.isValidTarget()) spawnTentacles(npc, target, listOf(X_FORMATION[deps.random.of(X_FORMATION.size)]))
        }
        deps.extensionRegistry.register(MELEE) { _, npc, target, _ ->
            if (target.isValidTarget()) fireMelee(npc, target)
        }
        deps.extensionRegistry.register(BIND) { _, npc, target, _ ->
            if (target.isValidTarget()) fireBind(npc, target)
        }

        deps.extensionRegistry.register(SHOT) { _, npc, target, params ->
            if (target.isValidTarget()) fireShot(npc, target, params as Style)
        }
        deps.extensionRegistry.register(TENTACLES) { _, npc, target, _ ->
            if (target.isValidTarget()) spawnTentacles(npc, target)
        }
        deps.extensionRegistry.register(SEEDS) { _, npc, target, _ ->
            if (target.isValidTarget() && !enraging(npc)) startCorruptedSeeds(npc, target)
        }
        deps.extensionRegistry.register(SOULS) { _, npc, target, _ ->
            if (target.isValidTarget() && !enraging(npc)) startSoulSiphon(npc, target)
        }
        deps.extensionRegistry.register(SCREECH) { _, npc, target, _ ->
            if (target.isValidTarget() && !enraging(npc)) startScreech(npc, target)
        }
    }

    private fun fireShot(npc: Npc, target: Player, style: Style) {
        deps.bossProjectile(
            spotanim = style.projectile.asRSCM(RSCMType.SPOTANIM),
            src = npc.coords.translate(1, 1),
            target = target.coords,
            startHeight = SHOT_START_HEIGHT,
            endHeight = SHOT_END_HEIGHT,
            delay = SHOT_START_DELAY,
            travel = SHOT_TRAVEL,
            curve = SHOT_ANGLE,
            progress = SHOT_PROGRESS,
            homing = target,
        )
        style.impact?.let {
            target.spotanim(it, delay = SHOT_CLIENT_CYCLES, height = SHOT_IMPACT_HEIGHT, slot = 2)
        }
        val damage = deps.random.of(style.maxHit + 1)
        target.finishNpcHit(npc, SHOT_FLIGHT_TICKS, style.hitType, damage, deps.playerHitModifier)
    }

    private fun spawnTentacles(boss: Npc, target: Player, formation: List<Pair<Int, Int>> = X_FORMATION) {
        val type = ServerCacheManager.getNpc(TENTACLE_NPC.asRSCM(RSCMType.NPC))!!
        val anchor = target.coords
        val lines = mutableListOf<List<CoordGrid>>()

        for ((dx, dz) in formation) {
            val centre = anchor.translate(dx, dz)
            val sw = centre.translate(-1, -1)
            if (!footprintClear(sw)) continue

            val tentacle = Npc(type, sw)
            tentacle.mode = NpcMode.None
            deps.npcRepo.add(tentacle, TENTACLE_LIFETIME)
            tentacle.faceSquare(anchor)
            tentacle.anim(TENTACLE_SPAWN_SEQ)
            tentacle.spotanim(TENTACLE_SPAWN_SPOTANIM)

            deps.worldQueues.add(TENTACLE_ATTACK_DELAY) {
                if (tentacle.isSlotAssigned) {
                    tentacle.anim(TENTACLE_ATTACK_SEQ)
                    tentacle.spotanim(TENTACLE_ATTACK_SPOTANIM)
                }
            }
            lines += splashLine(centre, anchor)
        }

        deps.worldQueues.add(TENTACLE_STRIKE_DELAY) { splash(lines) }
        deps.worldQueues.add(TENTACLE_DAMAGE_DELAY) { strike(boss, lines) }
    }

    private fun splashLine(centre: CoordGrid, anchor: CoordGrid): List<CoordGrid> {
        val stepX = (anchor.x - centre.x).sign
        val stepZ = (anchor.z - centre.z).sign
        return (1..SPLASH_STEPS).map { centre.translate(stepX * it, stepZ * it) }
    }

    private fun splash(lines: List<List<CoordGrid>>) {
        lines.forEach { line ->
            line.forEachIndexed { index, tile ->
                val spot = SpotanimType(SPLASH_SPOTANIMS[index].asRSCM(RSCMType.SPOTANIM))
                deps.worldRepo.spotanimMap(spot, tile, delay = SPLASH_CYCLES_PER_STEP * (index + 1))
            }
        }
    }

    private fun strike(boss: Npc, lines: List<List<CoordGrid>>) {
        val struck = lines.flatten().toSet()
        for (player in deps.playerList) {
            if (player.hitpoints <= 0 || player.coords !in struck) continue
            player.finishNpcHit(
                boss,
                TENTACLE_HIT_DELAY,
                EngineHitType.Typeless,
                TENTACLE_DAMAGE,
                deps.playerHitModifier,
            )
            player.changeSanity(-TENTACLE_SANITY_DRAIN)
        }
    }

    private fun footprintClear(sw: CoordGrid): Boolean {
        for (fx in 0 until TENTACLE_SIZE) {
            for (fz in 0 until TENTACLE_SIZE) {
                if (deps.collision.isWalkBlocked(sw.translate(fx, fz))) return false
            }
        }
        return true
    }

    /**
     * Seeds exist in both realms: every tile shows the generic [SEED_LOC] in the real realm, while
     * the Shadow Realm copy (offset by [REALM_OFFSET_X]) reveals safe vs hazard. The player can
     * step on either copy, so each tile is tracked as a real/shadow pair.
     */
    @OptIn(InternalApi::class)
    private fun startCorruptedSeeds(boss: Npc, target: Player) {
        val layout = SEED_LAYOUTS[deps.random.of(SEED_LAYOUTS.size)]
        val session = instances.sessionForPlayer(target)
        val resolve = { coords: CoordGrid ->
            session?.let { instances.resolveCoord(it, coords) } ?: coords
        }
        val toTile = { real: CoordGrid ->
            SeedTile(resolve(real), resolve(real.translate(-REALM_OFFSET_X, 0)))
        }
        val safeTiles = layout.safe.map(toTile)
        val dangerTiles = layout.danger.map(toTile)

        (safeTiles + dangerTiles).forEach {
            spawnSeedLoc(it.real, SEED_LOC)
            mapSpot(SEED_SPAWN_SPOTANIM, it.real)
        }
        safeTiles.forEach { spawnSeedLoc(it.shadow, SEED_SHADOW_SAFE_LOC) }
        dangerTiles.forEach { spawnSeedLoc(it.shadow, SEED_SHADOW_DANGER_LOC) }

        holdBoss(boss, SEED_TIMER_TICKS)
        boss.anim(SCREECH_START_SEQ)
        deps.worldQueues.add(SEED_IDLE_DELAY) {
            if (boss.isSlotAssigned && !enraging(boss)) boss.anim(SCREECH_IDLE_SEQ)
        }
        energizeFragment(target)
        startChargeTimer(boss, SEED_TIMER_TICKS)
        beginSpecial(boss, target) { (safeTiles + dangerTiles).forEach(::clearSeedLocs) }
        pollSeeds(boss, target, safeTiles.toMutableSet(), dangerTiles.toMutableSet(), SEED_TIMER_TICKS)
    }

    private fun spawnSeedLoc(coords: CoordGrid, loc: String) {
        val duration = SEED_TIMER_TICKS + SEED_LOC_DESPAWN_BUFFER
        locRepo.add(coords, loc, duration, LocAngle.West, LocShape.CentrepieceStraight)
    }

    private fun clearSeedLocs(tile: SeedTile) {
        val seedIds = SEED_LOC_IDS.map { it.asRSCM(RSCMType.LOC) }
        for (coords in listOf(tile.real, tile.shadow)) {
            locRepo.findAll(coords).filter { it.id in seedIds }.toList().forEach {
                locRepo.del(it, Int.MAX_VALUE)
            }
        }
    }

    private fun pollSeeds(
        boss: Npc,
        target: Player,
        remainingSafe: MutableSet<SeedTile>,
        dangerTiles: MutableSet<SeedTile>,
        ticksLeft: Int,
    ) {
        deps.worldQueues.add(1) {
            if (enraging(boss)) return@add
            if (!boss.isSlotAssigned || !target.isValidTarget()) {
                specialCleanups.remove(boss)
                boss.movementLocked = false
                stopChargeTimer(boss)
                (remainingSafe + dangerTiles).forEach(::clearSeedLocs)
                endFragmentEnergy(target)
                return@add
            }

            val coords = target.coords
            val inShadow = isInShadow(target)
            val safeStep = remainingSafe.firstOrNull { it.contains(coords) }
            val dangerStep = dangerTiles.firstOrNull { it.contains(coords) }
            if (safeStep != null) {
                remainingSafe.remove(safeStep)
                clearSeedLocs(safeStep)
                mapSpot(if (inShadow) SEED_SQUASH_SHADOW_SPOTANIM else SEED_SQUASH_SPOTANIM, coords)
            } else if (dangerStep != null) {
                dangerTiles.remove(dangerStep)
                clearSeedLocs(dangerStep)
                mapSpot(SEED_VANISH_SPOTANIM, coords)
                punishSeeds(boss, target, rollDangerDamage())
            }

            val ticksRemaining = ticksLeft - 1
            when {
                remainingSafe.isEmpty() -> {
                    dangerTiles.forEach(::clearSeedLocs)
                    resolveSeedsSuccess(boss, target)
                }
                ticksRemaining <= 0 -> resolveSeedsTimeout(boss, target, remainingSafe, dangerTiles)
                else -> pollSeeds(boss, target, remainingSafe, dangerTiles, ticksRemaining)
            }
        }
    }

    private fun rollDangerDamage(): Int {
        val span = SEED_DANGER_DAMAGE.last - SEED_DANGER_DAMAGE.first + 1
        return SEED_DANGER_DAMAGE.first + deps.random.of(span)
    }

    private fun punishSeeds(boss: Npc, target: Player, damage: Int) {
        target.changeSanity(-damage)
        target.finishNpcHit(boss, SEED_HIT_DELAY, EngineHitType.Typeless, damage, deps.playerHitModifier)
    }

    private fun isInShadow(player: Player): Boolean {
        val session = instances.sessionForPlayer(player) ?: return false
        return instances.isShadowRealm(session, player.coords)
    }

    @OptIn(InternalApi::class)
    private fun returnToRealRealm(boss: Npc, target: Player) {
        val session = instances.sessionForPlayer(target)
        if (session == null) return
        val shadow = instances.isShadowRealm(session, target.coords)
        if (!shadow) return
        target.inShadowRealm = false
        target.clearSoftTimer(INSANITY_TIMER)
        val dest = target.coords.translate(REALM_OFFSET_X, 0)
        boss.resetFaceEntity()
        protectedAccess.launchLenient(target) {
            fadeTeleport(dest) {
                if (boss.isSlotAssigned && instances.isShadowRealm(session, boss.coords)) {
                    boss.telejumpRealm(deps.collision, REALM_OFFSET_X)
                }
                refaceNextTick(boss, player)
            }
        }
    }

    private fun refaceNextTick(boss: Npc, target: Player) {
        deps.worldQueues.add(1) {
            if (boss.isSlotAssigned && target.isValidTarget()) boss.facePlayer(target)
        }
    }

    private fun energizeFragment(target: Player) {
        target.canSwitchRealms = true
        target.setFragmentActive(true)
        target.mes(FRAGMENT_PULSE_MESSAGE)
    }

    private fun endFragmentEnergy(target: Player) {
        target.canSwitchRealms = false
        target.mes(SEED_ENERGY_LOST_MESSAGE)
        if (!target.inShadowRealm) target.setFragmentActive(false)
    }

    private fun startChargeTimer(boss: Npc, ticks: Int) {
        val timer = ChargeTimer(ticks)
        chargeTimers[boss] = timer
        tickChargeTimer(boss, timer, 1)
    }

    private fun tickChargeTimer(boss: Npc, timer: ChargeTimer, step: Int) {
        deps.worldQueues.add(1) {
            if (chargeTimers[boss] !== timer) return@add
            if (!boss.isSlotAssigned) {
                chargeTimers.remove(boss)
                return@add
            }
            val bar = ServerCacheManager.getHealthBar(CHARGE_HEADBAR.asRSCM(RSCMType.HEADBAR))!!
            val fill = bar.segments * (timer.ticks - step) / timer.ticks
            boss.removeHeadbar(boss.visHeadbar(params.headbar).id)
            boss.showHeadbar(Headbar.fromNoSource(bar.id, bar.id, fill, fill, 0, 0))
            if (step < timer.ticks) {
                tickChargeTimer(boss, timer, step + 1)
            } else {
                stopChargeTimer(boss)
            }
        }
    }

    private fun stopChargeTimer(boss: Npc) {
        if (chargeTimers.remove(boss) == null) return
        boss.removeHeadbar(CHARGE_HEADBAR.asRSCM(RSCMType.HEADBAR))
    }

    private fun holdBoss(boss: Npc, ticks: Int) {
        leaveMeleeForm(boss)
        boss.movementLocked = true
        boss.resetMovement()
        deps.suppressAttacks(boss, ticks)
    }

    private fun resumeAfterSpecial(boss: Npc, nextActionIn: Int) {
        specialCleanups.remove(boss)
        deps.encounter(boss).busyUntil = deps.mapClock.cycle + nextActionIn
        deps.worldQueues.add(nextActionIn) { boss.movementLocked = false }
    }

    private fun enraging(boss: Npc): Boolean = deps.encounter(boss).lethalHandled

    private fun beginSpecial(boss: Npc, target: Player, despawn: () -> Unit) {
        specialCleanups[boss] = {
            stopChargeTimer(boss)
            if (target.canSwitchRealms) endFragmentEnergy(target)
            despawn()
            boss.resetAnim()
        }
    }

    private fun cancelSpecial(boss: Npc) {
        specialCleanups.remove(boss)?.invoke()
    }

    private fun resolveSeedsSuccess(boss: Npc, target: Player) {
        resumeAfterSpecial(boss, SEED_RETURN_DELAY + BIND_AFTER_RETURN_DELAY)
        stopChargeTimer(boss)
        boss.anim(SCREECH_END_SEQ)
        endFragmentEnergy(target)
        target.mes(SEED_SUCCESS_MESSAGE)
        returnThenBind(boss, target, SPECIAL_HP_SECOND, SEED_RETURN_DELAY, BIND_AFTER_RETURN_DELAY)
    }

    private fun resolveSeedsTimeout(
        boss: Npc,
        target: Player,
        unstepped: Set<SeedTile>,
        dangerTiles: Collection<SeedTile>,
    ) {
        resumeAfterSpecial(boss, SEED_RETURN_DELAY + BIND_AFTER_RETURN_DELAY)
        stopChargeTimer(boss)
        val inShadow = isInShadow(target)
        unstepped.forEach { mapSpot(SEED_HATCH_SPOTANIM, it.inRealm(inShadow)) }
        dangerTiles.forEach { mapSpot(SEED_VANISH_SHADOW_SPOTANIM, it.inRealm(inShadow)) }
        (unstepped + dangerTiles).forEach(::clearSeedLocs)
        boss.anim(SCREECH_END_SEQ)
        endFragmentEnergy(target)
        target.mes(SEED_TIMEOUT_HATCH_MESSAGE)
        punishSeeds(boss, target, SEED_TIMEOUT_DAMAGE)
        returnThenBind(boss, target, SPECIAL_HP_SECOND, SEED_RETURN_DELAY, BIND_AFTER_RETURN_DELAY)
    }

    private fun returnThenBind(
        boss: Npc,
        target: Player,
        nextThreshold: Double,
        returnIn: Int,
        bindAfterReturn: Int,
    ) {
        deps.worldQueues.add(returnIn) {
            if (enraging(boss)) return@add
            if (target.isValidTarget()) {
                returnToRealRealm(boss, target)
                target.setFragmentActive(false)
            }
            deps.worldQueues.add(bindAfterReturn) {
                if (!enraging(boss)) postSpecialBind(boss, target, nextThreshold)
            }
        }
    }

    private fun mapSpot(spot: String, coords: CoordGrid) {
        deps.worldRepo.spotanimMap(SpotanimType(spot.asRSCM(RSCMType.SPOTANIM)), coords)
    }

    private fun startSoulSiphon(boss: Npc, target: Player) {
        target.mes(SOUL_TRIGGER_MESSAGE)
        energizeFragment(target)

        val session = instances.sessionForPlayer(target)
        val resolve = { coords: CoordGrid ->
            session?.let { instances.resolveCoord(it, coords) } ?: coords
        }
        val realType = ServerCacheManager.getNpc(SOUL_NPC.asRSCM(RSCMType.NPC))!!
        val shadowType = ServerCacheManager.getNpc(SOUL_SHADOW_NPC.asRSCM(RSCMType.NPC))!!
        val phrases = SoulPhrase.entries.flatMap { phrase -> List(phrase.count) { phrase } }.shuffled()

        val souls =
            SOUL_SPAWNS.mapIndexed { index, spawn ->
                val real = resolve(spawn)
                val phrase = phrases[index]
                val shadow = spawnSoul(shadowType, real.translate(-REALM_OFFSET_X, 0))
                if (phrase.recolours.isNotEmpty()) shadow.setBodyRecolours(phrase.recolours)
                SoulPair(phrase, spawnSoul(realType, real), shadow)
            }

        holdBoss(boss, SOUL_CHANT_TICKS + SPECIAL_HOLD_MARGIN)
        startChargeTimer(boss, SOUL_CHANT_TICKS)
        beginSpecial(boss, target) {
            souls.forEach { pair ->
                pair.dead = true
                listOf(pair.real, pair.shadow).filterNot(::isDown).forEach {
                    killSoul(it, SOUL_CHANT_END_DESPAWN_TICKS)
                }
            }
        }
        pollSouls(boss, target, souls, SOUL_CHANT_TICKS)
    }

    private fun spawnSoul(type: NpcServerType, coords: CoordGrid): Npc {
        val soul = Npc(type, coords)
        soul.mode = NpcMode.None
        soul.movementLocked = true
        deps.npcRepo.add(soul, SOUL_LIFETIME)
        return soul
    }

    private fun isDown(soul: Npc): Boolean = !soul.isSlotAssigned || soul.hitpoints <= 0

    /** Each real-realm soul mirrors a Shadow Realm one; killing either copy takes down both. */
    private fun pollSouls(boss: Npc, target: Player, souls: List<SoulPair>, ticksLeft: Int) {
        deps.worldQueues.add(1) {
            if (!boss.isSlotAssigned || enraging(boss)) return@add

            for (pair in souls.filter { !it.dead && (isDown(it.shadow) || isDown(it.real)) }) {
                pair.dead = true
                listOf(pair.real, pair.shadow).filterNot(::isDown).forEach {
                    killSoul(it, SOUL_KILLED_DESPAWN_TICKS)
                }
            }

            val ticksRemaining = ticksLeft - 1
            if (souls.any { !it.dead } && ticksRemaining > 0) {
                souls.filter { !it.dead }.forEach { it.shadow.say(it.phrase.chant) }
                pollSouls(boss, target, souls, ticksRemaining)
                return@add
            }
            finishSoulSiphon(boss, target, souls)
        }
    }

    private fun killSoul(soul: Npc, despawnIn: Int) {
        soul.anim(SOUL_DEATH_SEQ)
        deps.worldQueues.add(despawnIn) {
            if (soul.isSlotAssigned) deps.npcRepo.del(soul, Int.MAX_VALUE)
        }
    }

    private fun finishSoulSiphon(boss: Npc, target: Player, souls: List<SoulPair>) {
        stopChargeTimer(boss)
        val alive = souls.filter { !it.dead }
        alive.forEach { pair ->
            pair.dead = true
            listOf(pair.real, pair.shadow).filterNot(::isDown).forEach {
                killSoul(it, SOUL_CHANT_END_DESPAWN_TICKS)
            }
        }
        val aliveColours = alive.map { it.phrase }.toSet()
        endFragmentEnergy(target)
        val landed = aliveColours.size == SoulPhrase.entries.size
        resumeAfterSpecial(boss, SOUL_RESULT_DELAY + SOUL_RETURN_DELAY + BIND_AFTER_RETURN_DELAY)
        boss.anim(SOUL_TELEPORT_END_SEQ)
        if (landed) boss.anim(SOUL_TELEPORT_ATTACK_SEQ)
        deps.worldQueues.add(SOUL_RESULT_DELAY) {
            if (!boss.isSlotAssigned || enraging(boss)) return@add
            if (landed) {
                landSoulSiphon(boss, target)
            } else {
                SoulPhrase.entries
                    .filter { it !in aliveColours }
                    .forEach { resolveSoulSet(boss, target, it, aliveColours.isEmpty()) }
            }
            returnThenBind(boss, target, SPECIAL_HP_THIRD, SOUL_RETURN_DELAY, BIND_AFTER_RETURN_DELAY)
        }
    }

    private fun resolveSoulSet(boss: Npc, target: Player, phrase: SoulPhrase, allSoulsDead: Boolean) {
        when (phrase) {
            SoulPhrase.Vita -> {
                target.statHeal("stat.hitpoints", constant = 0, percent = SOUL_HEAL_RESTORE_PERCENT)
                target.mes(SOUL_VITA_MESSAGE)
            }
            SoulPhrase.Oratio -> {
                target.statHeal("stat.prayer", constant = 0, percent = SOUL_HEAL_RESTORE_PERCENT)
                target.mes(SOUL_ORATIO_MESSAGE)
            }
            SoulPhrase.Sanitas -> target.mes(SOUL_SANITAS_MESSAGE)
            SoulPhrase.Mors -> {
                val damage = if (allSoulsDead) SOUL_MORS_DAMAGE_ALL else SOUL_MORS_DAMAGE
                boss.queueHit(SOUL_HIT_DELAY, EngineHitType.Typeless, damage, npcHitModifier)
                target.mes(SOUL_MORS_MESSAGE)
            }
        }
    }

    private fun landSoulSiphon(boss: Npc, target: Player) {
        target.mes(SOUL_LANDED_MESSAGE)
        target.finishNpcHit(
            boss,
            SOUL_HIT_DELAY,
            EngineHitType.Typeless,
            SOUL_LANDED_DAMAGE,
            deps.playerHitModifier,
        )
        boss.hitpoints = (boss.hitpoints + SOUL_LANDED_HEAL).coerceAtMost(boss.baseHitpointsLvl)
    }

    private fun startScreech(boss: Npc, target: Player) {
        holdBoss(boss, SCREECH_FIRST_SCREECH + (SCREECH_COUNT - 1) * SCREECH_INTERVAL + SPECIAL_HOLD_MARGIN)
        deps.worldQueues.add(SCREECH_SETUP_DELAY) {
            if (!boss.isSlotAssigned || enraging(boss)) return@add
            boss.anim(SCREECH_IDLE_SEQ)
            energizeFragment(target)
            val pillars = spawnPillars(target)
            startChargeTimer(boss, SCREECH_CHARGE_TICKS)
            beginSpecial(boss, target) { pillars.forEach(::destroyPillar) }
            refreshPillarHealth(boss, pillars)
            deps.worldQueues.add(SCREECH_FRAGMENT_TICKS) {
                if (!enraging(boss)) endFragmentEnergy(target)
            }
            pollScreech(boss, target, pillars, SCREECH_COUNT)
        }
    }

    private fun instanceResolver(player: Player): (CoordGrid) -> CoordGrid {
        val session = instances.sessionForPlayer(player)
        return { coords -> session?.let { instances.resolveCoord(it, coords) } ?: coords }
    }

    private fun spawnPillars(target: Player): List<ScreechPillar> {
        val resolve = instanceResolver(target)
        val hitpoints = PILLAR_HITPOINTS.shuffled()
        return PILLAR_COORDS.mapIndexed { index, coords ->
            val real = resolve(coords)
            val shadow = real.translate(-REALM_OFFSET_X, 0)
            val hp = hitpoints[index]
            val realNpc = spawnPillarNpc(PILLAR_NPC, real, PILLAR_REAL_MODELS.random())
            val shadowNpc = spawnPillarNpc(PILLAR_SHADOW_NPC, shadow, PILLAR_SHADOW_MODELS.getValue(hp))
            spawnPillarLoc(real)
            spawnPillarLoc(shadow)
            splashPillarFootprint(real)
            splashPillarFootprint(shadow)
            ScreechPillar(realNpc, shadowNpc, coverTiles(real) + coverTiles(shadow), hp)
        }
    }

    private fun splashPillarFootprint(pillar: CoordGrid) {
        for (dx in 0..1) {
            for (dz in 0..1) mapSpot(SCREECH_PILLAR_SPOTANIM, pillar.translate(dx, dz))
        }
    }

    private fun screechWave(boss: Npc, target: Player, cover: Set<CoordGrid>) {
        val resolve = instanceResolver(target)
        val session = instances.sessionForPlayer(target)
        val bossInShadow = session != null && instances.isShadowRealm(session, boss.coords)
        val centre = boss.coords.translate(if (bossInShadow) REALM_OFFSET_X + 1 else 1, 1)
        val spot = SpotanimType(SCREECH_WAVE_SPOTANIM.asRSCM(RSCMType.SPOTANIM))
        for ((z, xs) in SCREECH_WAVE_ROWS) {
            for (x in xs) {
                val real = resolve(CoordGrid(x, z, 0))
                val distance = abs(real.x - centre.x) + abs(real.z - centre.z)
                val delay = SCREECH_WAVE_BASE_DELAY + SCREECH_WAVE_STEP_DELAY * maxOf(0, distance - SCREECH_WAVE_CORE)
                for (tile in listOf(real, real.translate(-REALM_OFFSET_X, 0))) {
                    if (tile !in cover) deps.worldRepo.spotanimMap(spot, tile, delay = delay)
                }
            }
        }
    }

    private fun spawnPillarNpc(npc: String, coords: CoordGrid, model: Int): Npc {
        val type = ServerCacheManager.getNpc(npc.asRSCM(RSCMType.NPC))!!
        val pillar = Npc(type, coords)
        pillar.mode = NpcMode.None
        deps.npcRepo.add(pillar, SCREECH_PILLAR_DURATION)
        pillar.setBodyModel(model)
        return pillar
    }

    private fun refreshPillarHealth(boss: Npc, pillars: List<ScreechPillar>) {
        deps.worldQueues.add(1) {
            val standing = pillars.filter { it.shownHp > 0 && it.shadow.isSlotAssigned }
            if (!boss.isSlotAssigned || standing.isEmpty()) return@add
            standing.forEach(::showPillarHealth)
            refreshPillarHealth(boss, pillars)
        }
    }

    private fun showPillarHealth(pillar: ScreechPillar) {
        val headbar = ServerCacheManager.getHealthBar(PILLAR_HEADBAR.asRSCM(RSCMType.HEADBAR))!!
        val fill = (pillar.shownHp * headbar.segments + PILLAR_MAX_HP - 1) / PILLAR_MAX_HP
        pillar.shadow.showHeadbar(
            Headbar.fromNoSource(
                self = headbar.id,
                public = headbar.id,
                startFill = fill,
                endFill = fill,
                startTime = 0,
                endTime = 0,
            )
        )
    }

    private fun spawnPillarLoc(coords: CoordGrid) {
        locRepo.add(coords, PILLAR_LOC, SCREECH_PILLAR_DURATION, LocAngle.West, LocShape.CentrepieceStraight)
    }

    private fun coverTiles(pillar: CoordGrid): Set<CoordGrid> =
        buildSet {
            for (dx in 0..1) {
                for (dz in 0..PILLAR_COVER_DEPTH) add(pillar.translate(dx, dz))
            }
        }

    private fun pollScreech(
        boss: Npc,
        target: Player,
        pillars: List<ScreechPillar>,
        screechesLeft: Int,
    ) {
        val wait = if (screechesLeft == SCREECH_COUNT) SCREECH_FIRST_SCREECH - SCREECH_SETUP_DELAY else SCREECH_INTERVAL
        deps.worldQueues.add(wait) {
            if (!boss.isSlotAssigned || enraging(boss)) return@add

            boss.anim(SCREECH_END_SEQ)
            boss.spotanim(SCREECH_BOSS_SPOTANIM, height = SCREECH_BOSS_SPOTANIM_HEIGHT)

            val alive = pillars.filter { it.hp > 0 }
            screechWave(boss, target, alive.flatMapTo(mutableSetOf()) { it.cover })
            val shelter = if (target.isValidTarget()) alive.filter { target.coords in it.cover } else alive
            if (target.isValidTarget() && shelter.isEmpty()) {
                val modifier = deps.playerHitModifier
                target.finishNpcHit(boss, SCREECH_HIT_DELAY, EngineHitType.Typeless, SCREECH_DAMAGE, modifier)
                target.changeSanity(-SCREECH_DAMAGE)
            }
            damagePillars(alive, shelter.toSet())

            val remaining = screechesLeft - 1
            if (remaining > 0) {
                deps.worldQueues.add(SCREECH_RESTART_DELAY) {
                    if (boss.isSlotAssigned && !enraging(boss)) boss.anim(SCREECH_START_SEQ)
                }
                pollScreech(boss, target, pillars, remaining)
            } else {
                resumeAfterSpecial(boss, SCREECH_BIND_IN)
                deps.worldQueues.add(SCREECH_NAMASTE_DELAY) {
                    if (boss.isSlotAssigned && !enraging(boss)) boss.anim(BIND_SEQ)
                }
                returnThenBind(boss, target, NO_NEXT_SPECIAL, SCREECH_NAMASTE_DELAY, SCREECH_BIND_IN - SCREECH_NAMASTE_DELAY)
            }
        }
    }

    /** Every pillar takes [SCREECH_PILLAR_DAMAGE]; the ones the player hid behind are destroyed. */
    private fun damagePillars(alive: List<ScreechPillar>, shelter: Set<ScreechPillar>) {
        val damage = alive.associateWith { if (it in shelter) it.hp else minOf(it.hp, SCREECH_PILLAR_DAMAGE) }
        alive.forEach { it.hp -= damage.getValue(it) }
        deps.worldQueues.add(SCREECH_PILLAR_HIT_DELAY) {
            for ((pillar, dealt) in damage) {
                pillar.shownHp = pillar.hp
                if (!pillar.shadow.isSlotAssigned) continue
                pillar.shadow.showHitmark(pillarHitmark(dealt))
                if (pillar.hp > 0) pillar.shadow.setBodyModel(PILLAR_SHADOW_MODELS.getValue(pillar.hp))
                showPillarHealth(pillar)
            }
        }
        deps.worldQueues.add(SCREECH_PILLAR_HIT_DELAY + 1) {
            damage.keys.filter { it.hp <= 0 }.forEach(::destroyPillar)
        }
    }

    private fun pillarHitmark(damage: Int): Hitmark {
        val id = PILLAR_HITMARK.asRSCM(RSCMType.HITMARK)
        return Hitmark.fromNoSource(self = id, source = id, public = id, damage = damage, delay = 0)
    }

    private fun destroyPillar(pillar: ScreechPillar) {
        val locId = PILLAR_LOC.asRSCM(RSCMType.LOC)
        for (npc in listOf(pillar.real, pillar.shadow)) {
            locRepo.findAll(npc.coords).filter { it.id == locId }.toList().forEach {
                locRepo.del(it, Int.MAX_VALUE)
            }
            if (npc.isSlotAssigned) deps.npcRepo.del(npc, Int.MAX_VALUE)
        }
    }

    private fun postSpecialBind(boss: Npc, target: Player, nextThreshold: Double) {
        if (!boss.isSlotAssigned || !target.isValidTarget()) return
        val skipBelow = nextThreshold * boss.baseHitpointsLvl + BIND_SKIP_HP_MARGIN
        if (boss.hitpoints <= skipBelow) return
        deps.forceNext(boss, "bind")
    }

    @OptIn(InternalApi::class)
    private fun enterMeleeForm(boss: Npc) {
        val type = ServerCacheManager.getNpc(MELEE_NPC.asRSCM(RSCMType.NPC)) ?: return
        boss.transmog(type, Int.MAX_VALUE)
        boss.assignUid()
        boss.apRangeOverride = MELEE_AP_RANGE
    }

    @OptIn(InternalApi::class)
    private fun leaveMeleeForm(boss: Npc) {
        if (!boss.isSlotAssigned || boss.visType.id != MELEE_NPC.asRSCM(RSCMType.NPC)) return
        val type = ServerCacheManager.getNpc(FORM_NPC.asRSCM(RSCMType.NPC)) ?: return
        boss.transmog(type, Int.MAX_VALUE)
        boss.assignUid()
        boss.apRangeOverride = null
    }

    private fun fireMelee(boss: Npc, target: Player) {
        val damage = MELEE_MIN + deps.random.of(MELEE_MAX - MELEE_MIN + 1)
        repeat(MELEE_HITS) { index ->
            target.finishNpcHit(
                boss,
                index + 1,
                EngineHitType.Melee,
                damage,
                deps.playerHitModifier,
                MELEE_PRAYER_PENETRATION,
            )
        }
    }

    private fun melee(): Effect = sequence(anim(MELEE_SEQ), external(MELEE))

    private fun fireBind(boss: Npc, target: Player) {
        deps.suppressAttacks(boss, ATTACK_RATE)
        deps.bossProjectile(
            spotanim = BIND_TRAVEL_SPOTANIM.asRSCM(RSCMType.SPOTANIM),
            src = boss.coords.translate(1, 1),
            target = target.coords,
            startHeight = BIND_START_HEIGHT,
            endHeight = BIND_END_HEIGHT,
            delay = BIND_START_DELAY,
            travel = BIND_TRAVEL,
            curve = BIND_ANGLE,
            progress = BIND_PROGRESS,
            homing = target,
        )
        enterMeleeForm(boss)
        deps.worldQueues.add(BIND_LAND_TICKS + MELEE_FORM_AFTER_LAND) { leaveMeleeForm(boss) }
        deps.worldQueues.add(BIND_LAND_TICKS) {
            if (!target.isValidTarget()) return@add
            target.mes(BIND_MESSAGE)
            target.spotanim(BIND_IMPACT_SPOTANIM, height = BIND_IMPACT_HEIGHT)
            target.frozen = true
            target.routeDestination.clear()
            target.timer("timer.combat_freeze", BIND_DURATION_TICKS)
        }
    }

    private enum class Style(
        val projectile: String,
        val impact: String?,
        val hitType: EngineHitType,
        val maxHit: Int,
    ) {
        Ranged("spotanim.proj_whisperer_01_ranged_01", null, EngineHitType.Ranged, 40),
        Magic(
            "spotanim.proj_whisperer_01_magic_01",
            "spotanim.whisperer_magic_impact",
            EngineHitType.Magic,
            36,
        ),
    }

    private fun shot(style: Style, variant: Int): Effect =
        sequence(
            anim("seq.npc_whisperer_01_attack_${style.name.lowercase()}_0$variant"),
            external(SHOT, style),
        )

    private fun volleyOf(first: Style, second: Style, third: Style): Effect =
        sequence(
            shot(first, 3),
            wait(1),
            shot(second, 4),
            wait(1),
            shot(third, 3),
            wait(TENTACLE_SPAWN_WAIT),
            external(TENTACLES),
        )

    private fun volley(style: Style): Effect {
        val other = if (style == Style.Ranged) Style.Magic else Style.Ranged
        val allSpecials = specialsUsed(SPECIAL_ABILITIES.map { Condition.AbilityUsed(it) }, Condition::And)
        val anySpecial = specialsUsed(SPECIAL_ABILITIES.map { Condition.AbilityUsed(it) }, Condition::Or)
        return whenever(
            allSpecials,
            volleyOf(style, other, style),
            whenever(anySpecial, volleyOf(style, style, other), volleyOf(style, style, style)),
        )
    }

    private fun specialsUsed(
        used: List<Condition>,
        combine: (Condition, Condition) -> Condition,
    ): Condition = used.reduce(combine)

    private fun corruptedSeeds(): Effect =
        sequence(
            anim(SEED_TELEPORT_SEQ_1),
            teleport(spawnTile()),
            wait(1),
            anim(SEED_TELEPORT_SEQ_2),
            message(SEED_TRIGGER_MESSAGE),
            wait(SEED_SUMMON_WAIT),
            anim(SEED_SUMMON_SEQ),
            external(SEEDS),
        )

    private fun onBossHit(boss: Npc, hit: HitBuilder) {
        val encounter = deps.encounter(boss)
        if (encounter.currentPhaseName == ENRAGED_PHASE || encounter.invulnerable) return
        if (boss.hitpoints - hit.damage > 0) return
        hit.damage = boss.hitpoints - 1
        beginEnrage(boss)
    }

    private fun preventPreEnrageDeath(boss: Npc) {
        if (boss.hitpoints > 0 || deps.encounter(boss).currentPhaseName == ENRAGED_PHASE) return
        boss.clearQueue("queue.death")
        boss.hitpoints = 1
        beginEnrage(boss)
    }

    private fun beginEnrage(boss: Npc) {
        val encounter = deps.encounter(boss)
        if (encounter.lethalHandled) return
        encounter.lethalHandled = true
        encounter.invulnerable = true
        cancelSpecial(boss)
        holdBoss(boss, ENRAGE_ATTACK_DELAY + SPECIAL_HOLD_MARGIN)
        val players = instancePlayers(boss)
        deps.worldQueues.add(1) {
            if (boss.isSlotAssigned) boss.hitpoints = ENRAGE_HITPOINTS.coerceAtMost(boss.baseHitpointsLvl)
        }
        deps.worldQueues.add(ENRAGE_PULL_DELAY) { pullIntoShadowRealm(boss, players) }
        deps.worldQueues.add(ENRAGE_ATTACK_DELAY) {
            if (!boss.isSlotAssigned) return@add
            encounter.invulnerable = false
            encounter.busyUntil = 0
            encounter.transitionTo(ENRAGED_PHASE, deps.mapClock.cycle)
        }
    }

    private fun ScriptContext.registerEnrageGuard() {
        for (name in listOf(SPAWN_NPC, FORM_NPC, MELEE_NPC)) {
            val type = ServerCacheManager.getNpc(name.asRSCM(RSCMType.NPC))!!
            onNpcHit(type) {
                preventPreEnrageDeath(npc)
                val attacker = hit.resolvePlayerSource(deps.playerList)
                val boss = npc
                deps.worldQueues.add(ESCAPE_CHECK_DELAY) {
                    if (boss.isSlotAssigned && boss.mode == NpcMode.PlayerEscape) {
                    }
                }
            }
        }
    }

    private fun ScriptContext.registerDeath() {
        for (name in listOf(FORM_NPC, MELEE_NPC)) {
            val type = ServerCacheManager.getNpc(name.asRSCM(RSCMType.NPC))!!
            onNpcQueue(type, "queue.death") {
                val dropCoords = releaseFromShadowRealm(npc)
                npcDeath.deathWithDrops(this, dropCoords)
            }
        }
    }

    private fun releaseFromShadowRealm(boss: Npc): CoordGrid {
        val session = instances.instanceForNpc(boss)?.let(instances::sessionForId)
        val dropCoords =
            if (session != null && instances.isShadowRealm(session, boss.coords)) {
                boss.coords.translate(REALM_OFFSET_X, 0)
            } else {
                boss.coords
            }
        for (player in instancePlayers(boss)) {
            player.sanity = SANITY_MAX
            returnToRealRealm(boss, player)
        }
        return dropCoords
    }

    private fun instancePlayers(boss: Npc): List<Player> {
        val instanceId = instances.instanceForNpc(boss) ?: return emptyList()
        return deps.playerList.filter { instances.sessionForPlayer(it)?.id == instanceId }
    }

    @OptIn(InternalApi::class)
    private fun pullIntoShadowRealm(boss: Npc, players: List<Player>) {
        if (!boss.isSlotAssigned) return
        boss.anim(SEED_TELEPORT_SEQ_2)
        for (player in players) {
            val session = instances.sessionForPlayer(player) ?: continue
            if (!player.isValidTarget() || instances.isShadowRealm(session, player.coords)) continue
            player.mes(ENRAGE_MESSAGE)
            player.inShadowRealm = true
            player.canSwitchRealms = false
            val dest = player.coords.translate(-REALM_OFFSET_X, 0)
            boss.resetFaceEntity()
            protectedAccess.launchLenient(player) {
                fadeTeleport(dest) {
                    if (boss.isSlotAssigned && !instances.isShadowRealm(session, boss.coords)) {
                        boss.telejumpRealm(deps.collision, -REALM_OFFSET_X)
                    }
                    refaceNextTick(boss, player)
                }
            }
        }
    }

    private fun enragedAttack(style: Style, variant: Int): Effect =
        sequence(shot(style, variant), external(ENRAGED_TENTACLE))

    private fun bind(): Effect =
        sequence(
            anim(BIND_SEQ),
            spotanim(BIND_CAST_SPOTANIM, height = BIND_CAST_HEIGHT),
            external(BIND),
        )

    private fun soulSiphon(): Effect =
        sequence(
            anim(SOUL_TELEPORT_START_SEQ),
            teleport(spawnTile()),
            faceTarget(),
            wait(1),
            anim(SOUL_TELEPORT_IDLE_SEQ),
            external(SOULS),
        )

    private fun screech(): Effect =
        sequence(
            anim(SCREECH_TELEPORT_SEQ_1),
            teleport(spawnTile(dz = SCREECH_SOUTH_OFFSET)),
            faceTarget(),
            wait(1),
            anim(SCREECH_TELEPORT_SEQ_2),
            message(SCREECH_TRIGGER_MESSAGE),
            wait(SCREECH_CHARGE_WAIT),
            anim(SCREECH_START_SEQ),
            external(SCREECH),
        )

    override val spec: BossSpec by lazy {
        boss(SPAWN_NPC, FORM_NPC, MELEE_NPC) {
            stats(attackRate = ATTACK_RATE, aggressionRadius = AGGRESSION_RADIUS)

            val ranged = ability("ranged_volley", volley(Style.Ranged))
            val magic = ability("magic_volley", volley(Style.Magic))
            val seeds = ability("corrupted_seeds", corruptedSeeds())
            val siphon = ability("soul_siphon", soulSiphon())
            val screech = ability("screech", screech())
            val bind = ability("bind", bind())
            val melee = ability("melee", melee())
            val enragedRanged3 = ability("enraged_ranged_3", enragedAttack(Style.Ranged, 3))
            val enragedRanged4 = ability("enraged_ranged_4", enragedAttack(Style.Ranged, 4))
            val enragedMagic3 = ability("enraged_magic_3", enragedAttack(Style.Magic, 3))
            val enragedMagic4 = ability("enraged_magic_4", enragedAttack(Style.Magic, 4))

            phase("main") {
                weightedSelectorRandom {
                    +random(ranged, weight = 1)
                    +random(magic, weight = 1)
                }
                forceWhen(HpBelow(SPECIAL_HP_FIRST), seeds, once = true)
                forceWhen(HpBelow(SPECIAL_HP_SECOND), siphon, once = true)
                forceWhen(HpBelow(SPECIAL_HP_THIRD), screech, once = true)
                forceWhen(WithinMeleeRange, melee)
            }

            phase(ENRAGED_PHASE, attackRate = ENRAGED_ATTACK_RATE) {
                forceWhen(WithinMeleeRange, melee)
                rotationSelector {
                    +then(enragedRanged3)
                    +then(enragedRanged4)
                    +then(enragedMagic3)
                    +then(enragedMagic4)
                }
            }
        }
    }

    private companion object {
        private const val SHOT = "whisperer.shot"
        private val SPECIAL_ABILITIES = listOf("corrupted_seeds", "soul_siphon", "screech")
        private const val TENTACLES = "whisperer.tentacles"
        private const val BIND = "whisperer.bind"
        private const val MELEE = "whisperer.melee"
        private const val FORM_NPC = "npc.whisperer"
        private const val MELEE_NPC = "npc.whisperer_melee"
        private const val MELEE_SEQ = "seq.npc_whisperer_01_attack_melee_01"
        private const val MELEE_AP_RANGE = 1
        private const val MELEE_FORM_AFTER_LAND = 5
        private const val MELEE_MIN = 15
        private const val MELEE_MAX = 42
        private const val MELEE_HITS = 2
        private const val MELEE_PRAYER_PENETRATION = 50
        private const val ENRAGED_TENTACLE = "whisperer.enraged_tentacle"
        private const val ENRAGED_PHASE = "enraged"
        private const val ESCAPE_CHECK_DELAY = 2
        private const val NEAR_LETHAL_MARGIN = 100
        private const val ENRAGED_ATTACK_RATE = 2
        private const val ENRAGE_HITPOINTS = 140
        private const val ENRAGE_PULL_DELAY = 4
        private const val ENRAGE_ATTACK_DELAY = 7
        private const val ENRAGE_MESSAGE = "<col=ff289d>The Whisperer pulls you into the Shadow Realm..."

        private const val SPAWN_NPC = "npc.whisperer_spawn"
        private const val SPAWN_IDLE_SEQ = "seq.npc_whisperer_01_spawn_02"

        private const val ATTACK_RATE = WHISPERER_ATTACK_RATE
        private const val AGGRESSION_RADIUS = 15
        private const val TENTACLE_SPAWN_WAIT = 2

        private const val SHOT_START_DELAY = 30
        private const val SHOT_TRAVEL = 60
        private const val SHOT_CLIENT_CYCLES = SHOT_START_DELAY + SHOT_TRAVEL
        private const val SHOT_FLIGHT_TICKS = 3
        private const val SHOT_START_HEIGHT = 350
        private const val SHOT_END_HEIGHT = 100
        private const val SHOT_ANGLE = 254
        private const val SHOT_PROGRESS = 128
        private const val SHOT_IMPACT_HEIGHT = 100

        private const val TENTACLE_NPC = "npc.whisperer_tentacle"
        private const val TENTACLE_SPAWN_SEQ = "seq.npc_whisperer_tentacle_01_spawn_01"
        private const val TENTACLE_ATTACK_SEQ = "seq.npc_whisperer_tentacle_01_attack_01"
        private const val TENTACLE_SPAWN_SPOTANIM = "spotanim.spotanim_whisperer_tentacle_01_spawn_01"
        private const val TENTACLE_ATTACK_SPOTANIM = "spotanim.spotanim_whisperer_tentacle_01_attack_02"
        private const val TENTACLE_SIZE = 3
        private const val TENTACLE_LIFETIME = 4
        private const val TENTACLE_ATTACK_DELAY = 1
        private const val TENTACLE_STRIKE_DELAY = 2
        private const val TENTACLE_DAMAGE_DELAY = TENTACLE_STRIKE_DELAY + 1
        private const val TENTACLE_HIT_DELAY = 1
        private const val TENTACLE_DAMAGE = 20
        private const val TENTACLE_SANITY_DRAIN = 20

        private const val SPLASH_STEPS = 4
        private const val SPLASH_CYCLES_PER_STEP = 5
        private val SPLASH_SPOTANIMS =
            listOf(
                "spotanim.spotanim_water_splash_01",
                "spotanim.spotanim_water_splash_02",
                "spotanim.spotanim_water_splash_03",
                "spotanim.spotanim_water_splash_04",
            )

        private val X_FORMATION = listOf(-4 to -4, -4 to 4, 4 to -4, 4 to 4)

        private const val SEEDS = "whisperer.seeds"
        private const val SPECIAL_HP_FIRST = 0.8

        private const val SEED_TELEPORT_SEQ_1 = "seq.npc_whisperer_01_teleport_01"
        private const val SEED_TELEPORT_SEQ_2 = "seq.npc_whisperer_01_teleport_02"
        private const val SEED_SUMMON_SEQ = "seq.npc_whisperer_01_summon_leeches_01"
        private const val SEED_SUMMON_WAIT = 5

        private const val SEED_TRIGGER_MESSAGE =
            "<col=a53fff>Leeches begin to grow from the shadows...</col>"
        private const val FRAGMENT_PULSE_MESSAGE =
            "<col=ff289d>Your blackstone fragment pulses with dark energy..."
        private const val SEED_SUCCESS_MESSAGE =
            "<col=229628>You manage to deter all of the shadow leeches.</col>"
        private const val SEED_ENERGY_LOST_MESSAGE =
            "<col=ff3045>Your blackstone fragment loses all its energy."
        private const val SEED_TIMEOUT_HATCH_MESSAGE =
            "<col=ff3045>Leeches from the shadows hatch, draining your health and mind!"

        private const val SEED_RETURN_DELAY = 2
        private const val BIND_AFTER_RETURN_DELAY = 3
        private const val SPECIAL_HOLD_MARGIN = 10
        private const val SOUL_RESULT_DELAY = 3
        private const val SOUL_RETURN_DELAY = 2
        private const val SCREECH_NAMASTE_DELAY = 4
        private const val SCREECH_BIND_IN = 10
        private const val NO_NEXT_SPECIAL = 0.0
        private const val BIND_SKIP_HP_MARGIN = 50
        private const val SEED_LOC = "loc.whisperer_seed_regular_realm_weak"
        private const val SEED_SHADOW_SAFE_LOC = "loc.whisperer_seed_shadow_realm_weak"
        private const val SEED_SHADOW_DANGER_LOC = "loc.whisperer_seed_shadow_realm_danger"
        private const val SEED_SPAWN_SPOTANIM = "spotanim.npc_whisperer_egg_spawn"
        private const val SEED_SQUASH_SPOTANIM = "spotanim.npc_whisperer_egg_squash_normal"
        private const val SEED_SQUASH_SHADOW_SPOTANIM = "spotanim.npc_whisperer_egg_squash_shadow"
        private const val SEED_VANISH_SPOTANIM = "spotanim.npc_whisperer_egg_vanish_normal"
        private const val SEED_VANISH_SHADOW_SPOTANIM = "spotanim.npc_whisperer_egg_vanish_shadow"
        private const val SEED_HATCH_SPOTANIM = "spotanim.npc_whisperer_egg_hatch_shadow"

        private val SEED_LOC_IDS = listOf(SEED_LOC, SEED_SHADOW_SAFE_LOC, SEED_SHADOW_DANGER_LOC)

        private data class SeedTile(val real: CoordGrid, val shadow: CoordGrid) {
            fun contains(coords: CoordGrid) = coords == real || coords == shadow

            fun inRealm(shadowRealm: Boolean) = if (shadowRealm) shadow else real
        }

        private const val SEED_TIMER_TICKS = 40
        private const val SEED_LOC_DESPAWN_BUFFER = 5
        private const val SEED_IDLE_DELAY = 2

        private val SEED_DANGER_DAMAGE = 15..26

        private const val SEED_TIMEOUT_DAMAGE = 75
        private const val SEED_HIT_DELAY = 1

        private data class SeedLayout(val safe: List<CoordGrid>, val danger: List<CoordGrid>)

        private val SEED_LAYOUT_A =
            SeedLayout(
                safe =
                    listOf(
                        CoordGrid(2654, 6369, 0),
                        CoordGrid(2652, 6369, 0),
                        CoordGrid(2652, 6371, 0),
                        CoordGrid(2654, 6373, 0),
                        CoordGrid(2656, 6371, 0),
                        CoordGrid(2658, 6369, 0),
                        CoordGrid(2660, 6369, 0),
                        CoordGrid(2660, 6371, 0),
                        CoordGrid(2656, 6373, 0),
                        CoordGrid(2658, 6373, 0),
                        CoordGrid(2652, 6367, 0),
                        CoordGrid(2654, 6365, 0),
                        CoordGrid(2656, 6367, 0),
                        CoordGrid(2660, 6367, 0),
                        CoordGrid(2656, 6365, 0),
                        CoordGrid(2658, 6365, 0),
                    ),
                danger =
                    listOf(
                        CoordGrid(2653, 6368, 0),
                        CoordGrid(2658, 6371, 0),
                        CoordGrid(2659, 6370, 0),
                        CoordGrid(2659, 6372, 0),
                        CoordGrid(2657, 6372, 0),
                        CoordGrid(2654, 6367, 0),
                        CoordGrid(2653, 6366, 0),
                        CoordGrid(2655, 6366, 0),
                    ),
            )

        private val SEED_LAYOUT_B =
            SeedLayout(
                safe =
                    listOf(
                        CoordGrid(2651, 6369, 0),
                        CoordGrid(2651, 6371, 0),
                        CoordGrid(2655, 6373, 0),
                        CoordGrid(2653, 6371, 0),
                        CoordGrid(2661, 6369, 0),
                        CoordGrid(2661, 6371, 0),
                        CoordGrid(2657, 6373, 0),
                        CoordGrid(2659, 6371, 0),
                        CoordGrid(2651, 6367, 0),
                        CoordGrid(2653, 6367, 0),
                        CoordGrid(2655, 6365, 0),
                        CoordGrid(2661, 6367, 0),
                        CoordGrid(2657, 6365, 0),
                        CoordGrid(2659, 6367, 0),
                    ),
                danger =
                    listOf(
                        CoordGrid(2653, 6369, 0),
                        CoordGrid(2652, 6369, 0),
                        CoordGrid(2650, 6369, 0),
                        CoordGrid(2655, 6375, 0),
                        CoordGrid(2656, 6375, 0),
                        CoordGrid(2656, 6373, 0),
                        CoordGrid(2659, 6369, 0),
                        CoordGrid(2660, 6369, 0),
                        CoordGrid(2662, 6369, 0),
                        CoordGrid(2657, 6375, 0),
                        CoordGrid(2655, 6363, 0),
                        CoordGrid(2656, 6363, 0),
                        CoordGrid(2656, 6365, 0),
                        CoordGrid(2657, 6363, 0),
                    ),
            )

        private val SEED_LAYOUTS = listOf(SEED_LAYOUT_A, SEED_LAYOUT_B)

        private const val SOULS = "whisperer.souls"
        private const val SPECIAL_HP_SECOND = 0.55

        private const val SOUL_NPC = "npc.whisperer_soul"
        private const val SOUL_SHADOW_NPC = "npc.whisperer_soul_vulnerable"
        private const val SOUL_TELEPORT_START_SEQ =
            "seq.npc_whisperer_01_teleport_summon_souls_01_start"
        private const val SOUL_TELEPORT_IDLE_SEQ =
            "seq.npc_whisperer_01_teleport_summon_souls_01_idle"
        private const val SOUL_TELEPORT_END_SEQ = "seq.npc_whisperer_01_teleport_summon_souls_01_end"
        private const val SOUL_TELEPORT_ATTACK_SEQ =
            "seq.npc_whisperer_01_teleport_summon_souls_01_attack"

        private const val SOUL_CHANT_TICKS = 20
        private const val SOUL_LIFETIME = SOUL_CHANT_TICKS + 5
        private const val SOUL_HIT_DELAY = 1
        private const val SOUL_DEATH_SEQ = "seq.ghost_update_tendrill_death"
        private const val SOUL_KILLED_DESPAWN_TICKS = 4
        private const val SOUL_CHANT_END_DESPAWN_TICKS = 3

        private const val SOUL_HEAL_RESTORE_PERCENT = 20
        private const val SOUL_MORS_DAMAGE = 50
        private const val SOUL_MORS_DAMAGE_ALL = 75
        private const val SOUL_LANDED_DAMAGE = 50
        private const val SOUL_LANDED_HEAL = 100

        private const val SOUL_TRIGGER_MESSAGE =
            "<col=a53fff>The Whisperer summons twelve lost souls and begins to chant...</col>"
        private const val SOUL_VITA_MESSAGE =
            "<col=229628>Vita! The lost souls restore some of your health.</col>"
        private const val SOUL_ORATIO_MESSAGE =
            "<col=229628>Oratio! The lost souls restore some of your prayer.</col>"
        private const val SOUL_SANITAS_MESSAGE = "<col=229628>Sanitas! You feel your mind steady.</col>"
        private const val SOUL_MORS_MESSAGE =
            "<col=229628>Mors! The lost souls tear into the Whisperer.</col>"
        private const val SOUL_LANDED_MESSAGE =
            "<col=ff0000>The Whisperer completes her chant, siphoning your soul!</col>"

        private class SoulPair(val phrase: SoulPhrase, val real: Npc, val shadow: Npc) {
            var dead: Boolean = false
        }

        private enum class SoulPhrase(val count: Int, val chant: String, val recolours: List<Int>) {
            Vita(2, "Vita!", emptyList()),
            Sanitas(3, "Sanitas!", listOf(33602, 33591, 33581, 33552)),
            Mors(4, "Mors!", listOf(26434, 26423, 26413, 26386)),
            Oratio(3, "Oratio!", listOf(39746, 39735, 40749, 40720)),
        }

        private val SOUL_SPAWNS =
            listOf(
                CoordGrid(2650, 6375, 0),
                CoordGrid(2650, 6373, 0),
                CoordGrid(2648, 6373, 0),
                CoordGrid(2648, 6366, 0),
                CoordGrid(2650, 6364, 0),
                CoordGrid(2652, 6362, 0),
                CoordGrid(2662, 6362, 0),
                CoordGrid(2662, 6364, 0),
                CoordGrid(2664, 6364, 0),
                CoordGrid(2660, 6375, 0),
                CoordGrid(2662, 6373, 0),
                CoordGrid(2664, 6371, 0),
            )

        private const val SCREECH = "whisperer.screech"
        private const val SPECIAL_HP_THIRD = 0.3

        private const val SCREECH_TELEPORT_SEQ_1 = "seq.npc_whisperer_01_teleport_01"
        private const val SCREECH_TELEPORT_SEQ_2 = "seq.npc_whisperer_01_teleport_02"
        private const val SCREECH_START_SEQ = "seq.npc_whisperer_01_attack_screech_01_start"
        private const val SCREECH_IDLE_SEQ = "seq.npc_whisperer_01_attack_screech_01_idle"
        private const val SCREECH_END_SEQ = "seq.npc_whisperer_01_attack_screech_01_end"
        private const val SCREECH_BOSS_SPOTANIM = "spotanim.vfx_whisperer_01_attack_screech_01_end"
        private const val SCREECH_BOSS_SPOTANIM_HEIGHT = 125
        private const val SCREECH_PILLAR_SPOTANIM = "spotanim.spotanim_water_splash_04"
        private const val SCREECH_WAVE_SPOTANIM = "spotanim.spotanim_water_splash_05"
        private const val SCREECH_WAVE_BASE_DELAY = 5
        private const val SCREECH_WAVE_STEP_DELAY = 3
        private const val SCREECH_WAVE_CORE = 2
        private const val SCREECH_SETUP_DELAY = 2
        private const val SCREECH_FIRST_SCREECH = 14
        private const val SCREECH_RESTART_DELAY = 4
        private const val SCREECH_FRAGMENT_TICKS = 9

        private const val SCREECH_SOUTH_OFFSET = -10
        private const val SCREECH_CHARGE_WAIT = 3

        private const val SCREECH_COUNT = 3
        private const val SCREECH_INTERVAL = 6
        private const val SCREECH_HIT_DELAY = 1
        private const val SCREECH_DAMAGE = 45

        private const val PILLAR_LOC = "loc.whisperer_screech_safespot"
        private const val PILLAR_NPC = "npc.whisperer_screech_safespot"
        private const val PILLAR_SHADOW_NPC = "npc.whisperer_screech_safespot_shadow"
        private const val PILLAR_HEADBAR = "headbar.health_40"
        private const val PILLAR_MAX_HP = 60
        private val PILLAR_REAL_MODELS = listOf(49310, 49312, 49313)
        private val PILLAR_SHADOW_MODELS = mapOf(20 to 49309, 40 to 49308, 60 to 49311)
        private const val SCREECH_PILLAR_DAMAGE = 20
        private const val SCREECH_PILLAR_HIT_DELAY = 2
        private const val PILLAR_HITMARK = "hitmark.regular_damage_tint"
        private const val SCREECH_CHARGE_TICKS = SCREECH_FIRST_SCREECH - SCREECH_SETUP_DELAY - 1
        private const val SCREECH_PILLAR_DURATION = SCREECH_FIRST_SCREECH + (SCREECH_COUNT - 1) * SCREECH_INTERVAL + 5

        private const val SCREECH_TRIGGER_MESSAGE =
            "<col=a53fff>The Whisperer lets out a piercing screech as pillars rise from " +
                "the water...</col>"

        private class ChargeTimer(val ticks: Int)

        private const val CHARGE_HEADBAR = "headbar.charge_80"

        private class ScreechPillar(
            val real: Npc,
            val shadow: Npc,
            val cover: Set<CoordGrid>,
            var hp: Int,
        ) {
            var shownHp: Int = hp
        }

        private const val PILLAR_COVER_DEPTH = 3
        private val PILLAR_HITPOINTS = listOf(20, 20, 20, 20, 20, 20, 40, 60)
        private val PILLAR_COORDS =
            listOf(
                CoordGrid(2646, 6365, 0),
                CoordGrid(2648, 6361, 0),
                CoordGrid(2651, 6363, 0),
                CoordGrid(2654, 6364, 0),
                CoordGrid(2657, 6364, 0),
                CoordGrid(2660, 6363, 0),
                CoordGrid(2663, 6361, 0),
                CoordGrid(2665, 6365, 0),
            )

        private const val BIND_TRAVEL_SPOTANIM = "spotanim.whisperer_entangle_travel"
        private const val BIND_IMPACT_SPOTANIM = "spotanim.whisperer_entangle_impact"
        private const val BIND_SEQ = "seq.npc_whisperer_01_summon_leeches_01"
        private const val BIND_CAST_SPOTANIM = "spotanim.entangle_casting"
        private const val BIND_CAST_HEIGHT = 120
        private const val BIND_MESSAGE = "<col=ff3045>The Whisperer binds you in place!"
        private const val BIND_START_DELAY = 80
        private const val BIND_TRAVEL = 100
        private const val BIND_START_HEIGHT = 580
        private const val BIND_END_HEIGHT = 0
        private const val BIND_ANGLE = 16
        private const val BIND_PROGRESS = 150
        private const val BIND_IMPACT_HEIGHT = 100
        private const val BIND_LAND_TICKS = 6

        private const val BIND_DURATION_TICKS = 13
    }
}
