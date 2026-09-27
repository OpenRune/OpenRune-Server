package org.rsmod.content.bosses.leviathan

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.IdentityHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import org.rsmod.api.bossbar.plugin.BossHpBarScript
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.bossProjectile
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.bosses.runtime.forceNext
import org.rsmod.api.bosses.runtime.repeatTick
import org.rsmod.api.bosses.runtime.suppressAttacks
import org.rsmod.api.combat.commons.player.combatPlayDefendAnim
import org.rsmod.api.combat.commons.player.queueCombatRetaliate
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.hit.modifier.PlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.hit.queueImpactHit
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.output.CamShakeAxis
import org.rsmod.api.player.output.Camera
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.route.RayCastValidator
import org.rsmod.api.route.RouteFactory
import org.rsmod.api.route.walkTo
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.EntityExactMove
import org.rsmod.game.entity.util.EntityTinting
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.map.CoordGrid

/**
 * Fight mechanics for the Leviathan. The npc is a normal AI combatant
 * (`npc.apPlayer2(player, aiPlayerInteractions)` in [spawn]) driven by the boss DSL's ability
 * rotation declared in [Leviathan]; this class supplies the bespoke, non-declarative parts of
 * that rotation (the volley ladder, rockfall tiles, arena setup) as
 * [org.rsmod.api.bosses.runtime.BossExtensionRegistry] handlers, the same way
 * `Muspah`/`Whisperer`/`Vardorvis` delegate their own barrages and sweeps.
 *
 * Stage 2 added the shadow-spell stun/weak-spot break and the lightning/smoke specials it
 * triggers - these are reactive events (not selector-chosen), so they run as direct calls from
 * [onShadowSpellImpact]/[modifyIncoming] rather than through `forceNext`, the same way
 * `TormentedDemon`'s `onDemonHit` reacts to cumulative damage directly.
 *
 * Stage 3 adds the enrage phase, declared in [Leviathan] as `phase("enraged", entryHp = ...)` so
 * the DSL's own hp-threshold auto-transition (`BossCombat.checkAutoTransitions`) replaces the old
 * hand-rolled poll in `onDamaged`. Its entry ability spawns the pathfinder and does the initial
 * rockfall; the recurring rockfall and orb barrage are `forceEvery`/the phase selector, both
 * delegating to bespoke Kotlin here exactly like the `fight` phase's `bite`/`volley`. Pathfinder
 * patrol, the tornado chase-and-hit loop, and the aura tint are each their own self-rescheduling
 * loop (`deps.repeatTick`/recursive `deps.worldQueues`), mirroring Muspah's homing spike.
 */
@Singleton
internal class LeviathanEncounters
@Inject
constructor(
    private val deps: BossDeps,
    private val instances: InstanceManager,
    private val locRepo: LocRepository,
    private val rayCast: RayCastValidator,
    private val routeFactory: RouteFactory,
    private val hpBar: BossHpBarScript,
    private val aiPlayerInteractions: AiPlayerInteractions,
) {
    private val fights: MutableMap<Npc, LeviathanFight> = IdentityHashMap()
    private val pendingSpawns: MutableSet<Long> = HashSet()
    private val sessionTails: MutableMap<Long, List<Npc>> = HashMap()

    private val now: Int
        get() = deps.mapClock.cycle

    fun registerExtensions() {
        deps.extensionRegistry.register(VOLLEY_EXT) { _, npc, target, _ -> runVolley(npc, target) }
        deps.extensionRegistry.register(ENRAGE_ENTRY_EXT) { _, npc, target, _ -> runEnrageEntry(npc, target) }
        deps.extensionRegistry.register(ENRAGED_ROCKFALL_EXT) { _, npc, _, _ -> runEnragedRockfall(npc) }
        deps.extensionRegistry.register(ENRAGED_ORB_EXT) { _, npc, target, _ -> runEnragedOrb(npc, target) }
    }

    fun hasBoss(session: InstanceSession): Boolean =
        session.id.value in pendingSpawns || bossOf(session) != null

    fun bossOf(session: InstanceSession): Npc? =
        fights.keys.firstOrNull { it.isSlotAssigned && instances.instanceForNpc(it) == session.id }

    fun scheduleSpawn(session: InstanceSession, player: Player, awakened: Boolean, delay: Int) {
        if (!pendingSpawns.add(session.id.value)) return
        deps.worldQueues.add(delay) {
            pendingSpawns.remove(session.id.value)
            if (instances.sessionForPlayer(player)?.id != session.id) return@add
            if (bossOf(session) != null) return@add
            spawn(session, player, awakened)
        }
    }

    private fun spawn(session: InstanceSession, player: Player, awakened: Boolean) {
        val coords = instances.resolveCoord(session, LeviathanArena.BOSS_SPAWN) ?: return
        val arena = Arena.forBoss(coords)
        if (!arena.inSearchBox(player.coords) || isOnIsland(arena, player.coords)) return

        val type = ServerCacheManager.getNpc(BOSS_NPC.asRSCM(RSCMType.NPC)) ?: return
        val npc = Npc(type, coords)
        npc.movementLocked = true
        npc.apRequiresLineOfSight = false
        if (awakened) markAwakened(npc)
        deps.npcRepo.add(npc, Int.MAX_VALUE)
        instances.registerSessionNpc(player, npc)

        val fight = LeviathanFight(npc, arena, awakened)
        fights[npc] = fight

        npc.faceSquare(arena.at(RISE_FACE))
        npc.anim(RISE_SEQ)
        player.soundSynth(RISE_SYNTH)
        hpBar.onOpen(player, npc)
        deps.suppressAttacks(npc, FIRST_VOLLEY_DELAY)
        deps.worldQueues.add(1) { if (npc.isSlotAssigned) npc.facePlayer(player) }
        npc.apPlayer2(player, aiPlayerInteractions)
        watchAbandonment(fight)
    }

    private fun markAwakened(npc: Npc) {
        npc.vars["varn.awakened_state"] = 1
        npc.vars["varn.skip_killcount"] = 1
        npc.baseHitpointsLvl = AWAKENED_HITPOINTS
        npc.hitpoints = AWAKENED_HITPOINTS
        npc.baseAttackLvl = AWAKENED_ATTACK
        npc.baseStrengthLvl = AWAKENED_STRENGTH
        npc.baseDefenceLvl = AWAKENED_DEFENCE
        npc.baseRangedLvl = AWAKENED_RANGED
        npc.baseMagicLvl = AWAKENED_MAGIC
        npc.attackLvl = AWAKENED_ATTACK
        npc.strengthLvl = AWAKENED_STRENGTH
        npc.defenceLvl = AWAKENED_DEFENCE
        npc.rangedLvl = AWAKENED_RANGED
        npc.magicLvl = AWAKENED_MAGIC
    }

    /** Ends the fight if no arena player has been present for [ABANDON_TICKS] straight ticks. */
    private fun watchAbandonment(fight: LeviathanFight) {
        deps.repeatTick(
            ticks = Int.MAX_VALUE,
            onTick = { _ ->
                if (fight.ended || !fight.npc.isSlotAssigned) return@repeatTick false
                if (arenaPlayers(fight).isEmpty()) {
                    fight.absentTicks++
                    if (fight.absentTicks >= ABANDON_TICKS) {
                        abandon(fight)
                        return@repeatTick false
                    }
                } else {
                    fight.absentTicks = 0
                }
                true
            },
        )
    }

    private fun arenaPlayers(fight: LeviathanFight): List<Player> {
        val instanceId = instances.instanceForNpc(fight.npc) ?: return emptyList()
        return deps.playerList.filter {
            it.isValidTarget() &&
                instances.sessionForPlayer(it)?.id == instanceId &&
                fight.arena.inSearchBox(it.coords) &&
                !isOnIsland(fight.arena, it.coords)
        }
    }

    private fun isOnIsland(arena: Arena, coords: CoordGrid): Boolean =
        arena.toStatic(coords).x < LeviathanArena.HANDHOLDS_INSIDE.x

    private fun inBiteRange(fight: LeviathanFight, target: Player): Boolean {
        val c = fight.arena.centre
        return abs(target.coords.x - c.x) <= BITE_RANGE && abs(target.coords.z - c.z) <= BITE_RANGE
    }

    /**
     * `requires` condition for the `bite` ability. Bite and volley share equal selector weight,
     * so when both are eligible this alone reproduces the original 50/50 chance to bite instead
     * of volleying; when out of range or just used, only volley is eligible. "Just used" is read
     * straight off [org.rsmod.api.bosses.runtime.BossEncounter.lastAbilityName] rather than a
     * separate flag, since the weighted selector already tracks it.
     */
    fun shouldBite(npc: Npc): Boolean {
        if (deps.encounter(npc).lastAbilityName == BITE_ABILITY) return false
        val fight = fights[npc] ?: return false
        val target = arenaPlayers(fight).firstOrNull() ?: return false
        return inBiteRange(fight, target)
    }

    fun biteDamage(npc: Npc): Int {
        val fight = fights[npc] ?: return 0
        val max = if (fight.awakened) BITE_MAX_HIT_AWAKENED else BITE_MAX_HIT
        return deps.random.of(0, max)
    }

    /** Fires the escalating orb ladder for the current [LeviathanFight.stage], then rockfalls. */
    private fun runVolley(npc: Npc, target: Player) {
        val fight = fights[npc] ?: return
        fight.shotsFired = 0
        fight.volleysStarted++
        npc.facePlayer(target)
        fireVolleyShots(fight, target)
    }

    private fun fireVolleyShots(fight: LeviathanFight, target: Player) {
        val npc = fight.npc
        if (!npc.isSlotAssigned || fight.stunned || fight.inSpecial || fight.enraged) return
        val liveTarget = if (target.isValidTarget()) target else arenaPlayers(fight).firstOrNull()
        if (liveTarget == null) return

        val stage = VOLLEY_STAGES[fight.stage]
        if (fight.shotsFired >= stage.shots) {
            deps.suppressAttacks(npc, ROCKFALL_RECOVERY)
            rockfall(fight, withHints = fight.nextSpecial == null, animated = true)
            fight.stage = min(fight.stage + 1, VOLLEY_STAGES.lastIndex)
            return
        }

        val styles = if (stage.allStyles) OrbStyle.ALL else OrbStyle.DISTANCED
        val fast = stage.interval == 1
        val first = fight.shotsFired == 0
        fireOrb(fight, liveTarget, deps.random.pick(styles), stage.orbDelay, stage.orbTravel, fast, first)
        fight.shotsFired++

        deps.suppressAttacks(npc, stage.interval)
        if (stage.interval > 1) {
            // Settles back from the cast pose almost immediately, matching the pre-DSL polling
            // loop's `now == fight.nextActionTick - stage.interval + 1` (fires 1 tick after the
            // shot) rather than right before the next one.
            deps.worldQueues.add(1) { if (npc.isSlotAssigned) npc.anim(ORB_SINGLE_SEQ) }
        }
        deps.worldQueues.add(stage.interval) { fireVolleyShots(fight, liveTarget) }
    }

    private fun fireOrb(
        fight: LeviathanFight,
        target: Player,
        style: OrbStyle,
        delay: Int,
        travel: Int,
        fast: Boolean,
        first: Boolean,
    ) {
        val npc = fight.npc
        npc.anim(if (fast && !first) ORB_LOOP_SEQ else ORB_START_SEQ)
        npc.spotanim(if (fast && !first) style.followSpotanim else style.launchSpotanim)

        val centre = fight.arena.centre
        val bearing = LeviathanArena.bearing(target.coords.x - centre.x, target.coords.z - centre.z)
        deps.bossProjectile(
            spotanim = style.projectile.asRSCM(RSCMType.SPOTANIM),
            src = LeviathanArena.step(centre, bearing, ORB_SOURCE_OFFSET),
            target = target.coords,
            startHeight = ORB_START_HEIGHT,
            endHeight = ORB_END_HEIGHT,
            delay = delay,
            travel = travel,
            curve = ORB_CURVE,
            progress = ORB_PROGRESS,
            homing = target,
        )
        target.soundSynth(style.synth)

        val max = if (fight.awakened) style.awakenedMaxHit else style.maxHit
        val flightCycles = delay + travel
        val flightTicks = max(1, (flightCycles + CYCLES_PER_TICK - 1) / CYCLES_PER_TICK)
        // Fight logic runs in the world phase, before player input and before this tick's player
        // queues, so a queue added now is already decremented once: +1 lands it on the impact tick.
        val impactDelay = flightTicks + 1
        target.queueCombatRetaliate(npc, impactDelay)
        target.queueImpactHit(npc, impactDelay, style.hitType, deps.random.of(0, max), orbImpact(fight, style))
    }

    /** Resolves prayer, enraged prayer-piercing and the impact graphic on the tick the orb lands. */
    private fun orbImpact(fight: LeviathanFight, style: OrbStyle) = PlayerHitModifier { target ->
        if (fight.enraged && !insideAura(fight, target)) penetration = ENRAGED_PENETRATION
        val unprotected = !target.isProtectedFrom(style.hitType)
        with(deps.playerHitModifier) { modify(target) }
        target.combatPlayDefendAnim()
        if (unprotected) target.spotanim(style.impactSpotanim, height = ORB_IMPACT_HEIGHT)
    }

    private fun Player.isProtectedFrom(type: HitType): Boolean =
        when (type) {
            HitType.Melee -> vars[PROTECT_FROM_MELEE] != 0
            HitType.Ranged -> vars[PROTECT_FROM_MISSILES] != 0
            HitType.Magic -> vars[PROTECT_FROM_MAGIC] != 0
            HitType.Typeless -> false
        }

    private fun rockfall(fight: LeviathanFight, withHints: Boolean, animated: Boolean) {
        val npc = fight.npc
        val arena = fight.arena
        val players = arenaPlayers(fight)
        if (animated) npc.anim(ROCKFALL_SEQ)

        if (withHints) {
            val special = deps.random.pick(Special.entries)
            fight.nextSpecial = special
            val hints = if (special == Special.Lightning) LeviathanArena.LIGHTNING_HINTS else LeviathanArena.SMOKE_HINTS
            hints.forEach { hint -> nearestFreeTile(fight, arena.at(hint))?.let { dropBoulder(fight, it, true, HINT_DELAY) } }
        }

        val candidates = freeArenaTiles(fight).shuffled()
        val count = deps.random.of(ROCKFALL_MIN, ROCKFALL_MAX)
        candidates.take(count).forEach { dropBoulder(fight, it, false, deps.random.pick(BREAK_DELAYS)) }

        for (player in players) {
            val targets =
                if (fight.awakened) listOf(player.coords) + CARDINALS.map { player.coords.translate(it.first, it.second) }
                else listOf(player.coords)
            targets.filter { isFreeTile(fight, it) }.forEach { dropBoulder(fight, it, true, HINT_DELAY) }

            if (!animated) continue
            player.soundSynth(ROCKFALL_SYNTH, delay = 5)
            player.soundSynth(ROCKFALL_RUMBLE_SYNTH, loops = 5)
            Camera.camShake(player, CamShakeAxis.LEFT_RIGHT, deps.random.of(5, 8), 0, 0)
            Camera.camShake(player, CamShakeAxis.UP_DOWN, deps.random.of(5, 8), 0, 0)
            Camera.camShake(player, CamShakeAxis.FORWARDS_BACKWARDS, deps.random.of(5, 8), 0, 0)
            player.queueHit(npc, 1, HitType.Typeless, deps.random.of(ROCKFALL_CHIP), deps.playerHitModifier)
            deps.worldQueues.add(ROCKFALL_RECOVERY) { if (player.isValidTarget()) Camera.camReset(player) }
        }
    }

    private fun dropBoulder(fight: LeviathanFight, tile: CoordGrid, permanent: Boolean, delay: Int) {
        val angle = if (permanent) deps.random.of(0, 3) else deps.random.of(0, 1)
        val spotanim = if (permanent) STAY_SPOTANIMS[angle] else BREAK_SPOTANIMS[angle]
        mapSpot(spotanim, tile, delay = delay)
        val landTicks = BOULDER_LAND_BASE + (delay - BREAK_DELAYS.first()) / CYCLES_PER_TICK
        deps.worldQueues.add(landTicks) { landBoulder(fight, tile, permanent, LocAngle[angle]) }
    }

    private fun landBoulder(fight: LeviathanFight, tile: CoordGrid, permanent: Boolean, angle: LocAngle) {
        if (fight.ended) return
        deps.worldRepo.soundArea(tile, DEBRIS_IMPACT_SYNTH, radius = 5)
        deps.worldRepo.soundArea(tile, RUBBLE_LAND_SYNTH, radius = 5)
        if (permanent && tile !in fight.rubble) {
            fight.rubble[tile] = locRepo.add(tile, RUBBLE_LOC, Int.MAX_VALUE, angle, LocShape.CentrepieceStraight)
        }
        for (player in arenaPlayers(fight)) {
            if (player.coords != tile) continue
            player.queueHit(fight.npc, 1, HitType.Typeless, deps.random.of(BOULDER_DAMAGE), deps.playerHitModifier)
            if (permanent) knockBack(fight, player)
        }
    }

    private fun knockBack(fight: LeviathanFight, player: Player) {
        val from = player.coords
        val dest = ALL_DIRECTIONS.shuffled().map { from.translate(it.first, it.second) }.firstOrNull { isFreeTile(fight, it) } ?: return
        player.anim(KNOCKBACK_SEQ)
        PathingEntityCommon.teleport(player, deps.collision, dest)
        player.pendingExactMove =
            EntityExactMove(
                deltaX1 = from.x - dest.x,
                deltaZ1 = from.z - dest.z,
                deltaX2 = 0,
                deltaZ2 = 0,
                clientDelay1 = 0,
                clientDelay2 = CYCLES_PER_TICK,
                direction = LeviathanArena.bearing(from.x - dest.x, from.z - dest.z),
            )
    }

    private fun freeArenaTiles(fight: LeviathanFight): List<CoordGrid> {
        val sw = fight.arena.at(LeviathanArena.SEARCH_SW)
        val ne = fight.arena.at(LeviathanArena.SEARCH_NE)
        return buildList {
            for (x in sw.x..ne.x) {
                for (z in sw.z..ne.z) {
                    val tile = CoordGrid(x, z, sw.level)
                    if (isFreeTile(fight, tile)) add(tile)
                }
            }
        }
    }

    private fun isFreeTile(fight: LeviathanFight, tile: CoordGrid): Boolean =
        fight.arena.inSearchBox(tile) &&
            !isOnIsland(fight.arena, tile) &&
            tile !in fight.rubble &&
            !deps.collision.isWalkBlocked(tile)

    private fun nearestFreeTile(fight: LeviathanFight, tile: CoordGrid): CoordGrid? {
        if (isFreeTile(fight, tile)) return tile
        for (radius in 1..HINT_SEARCH_RADIUS) {
            for (dx in -radius..radius) {
                for (dz in -radius..radius) {
                    if (max(abs(dx), abs(dz)) != radius) continue
                    val candidate = tile.translate(dx, dz)
                    if (isFreeTile(fight, candidate)) return candidate
                }
            }
        }
        return null
    }

    fun onShadowSpellImpact(npc: Npc, player: Player) {
        val fight = fights[npc] ?: return
        if (fight.ended || npc.hitpoints <= 0) return
        if (fight.stunned || fight.inSpecial) return
        if (fight.volleysStarted == 0) return

        val centre = fight.arena.centre
        fight.stunCount++
        fight.stage = min(fight.stage, fight.stunCount)
        fight.stunFacing = LeviathanArena.bearing(player.coords.x - centre.x, player.coords.z - centre.z)
        fight.stunned = true

        val duration = if (fight.awakened) STUN_TICKS_AWAKENED else STUN_TICKS
        deps.suppressAttacks(npc, duration)

        npc.facePlayer(player)
        npc.anim(STUN_SEQ)
        npc.setIdleAnim(STUN_IDLE_SEQ)
        animateTails(fight)
        arenaPlayers(fight).forEach { it.mes(STUN_MESSAGE) }

        scheduleStunEnd(fight, duration)
    }

    private fun scheduleStunEnd(fight: LeviathanFight, ticks: Int) {
        val token = ++fight.stunToken
        deps.worldQueues.add(ticks) {
            // A weak-spot hit during the stun bumps the token to cancel this natural end and jump
            // straight to the special instead.
            if (fight.stunToken != token) return@add
            fight.stunned = false
            val npc = fight.npc
            if (npc.isSlotAssigned) {
                npc.clearIdleAnim()
                npc.resetAnim()
                deps.forceNext(npc, VOLLEY_ABILITY)
            }
        }
    }

    /**
     * Applies the stun cap, weak spot and in-special damage reduction to a player hit landing on
     * the Leviathan. A hit on the weak spot while stunned breaks the stun early and queues the
     * next special attack.
     */
    fun modifyIncoming(npc: Npc, attacker: Player?, damage: Int, type: HitType): Int {
        val fight = fights[npc] ?: return damage
        if (fight.stunned && attacker != null) {
            val centre = fight.arena.centre
            val bearing = LeviathanArena.bearing(attacker.coords.x - centre.x, attacker.coords.z - centre.z)
            val back = (fight.stunFacing + LeviathanArena.ANGLE_STEPS / 2) % LeviathanArena.ANGLE_STEPS
            if (abs(LeviathanArena.angleDelta(bearing, back)) > WEAK_SPOT_ARC) {
                return min(damage, STUNNED_DAMAGE_CAP)
            }
            breakStunForSpecial(npc, attacker)
            return if (type == HitType.Ranged) weakSpotRanged(npc, attacker, damage) else damage
        }
        if (fight.inSpecial) return damage * SPECIAL_DAMAGE_PERCENT / 100
        if (fight.enraged && attacker != null) {
            if (!insideAura(fight, attacker)) return damage * OUTSIDE_AURA_DAMAGE_PERCENT / 100
            return when (type) {
                HitType.Magic -> damage * 2
                HitType.Ranged -> boostedRanged(npc, attacker, damage, AURA_MIN_PERCENT)
                else -> damage
            }
        }
        return damage
    }

    private fun breakStunForSpecial(npc: Npc, attacker: Player) {
        val fight = fights[npc] ?: return
        fight.stunToken++
        deps.suppressAttacks(npc, 1)
        deps.worldQueues.add(1) { triggerSpecial(fight, attacker) }
    }

    private fun weakSpotRanged(npc: Npc, attacker: Player, damage: Int): Int {
        attacker.mes(WEAK_SPOT_MESSAGE)
        return boostedRanged(npc, attacker, damage, WEAK_SPOT_MIN_PERCENT)
    }

    private fun boostedRanged(npc: Npc, attacker: Player, damage: Int, minPercent: Int): Int {
        val maxHit = deps.maxHit.getRangedMaxHit(attacker, npc, null, null, 1.0, 0)
        val floor = (maxHit * minPercent + 99) / 100
        return if (damage >= floor) damage else deps.random.of(floor, max(floor, maxHit))
    }

    private fun triggerSpecial(fight: LeviathanFight, target: Player) {
        val npc = fight.npc
        if (fight.ended || !npc.isSlotAssigned) return
        fight.stunned = false
        npc.clearIdleAnim()
        npc.anim(STUN_SEQ)
        animateTails(fight)
        arenaPlayers(fight).filter { it !== target }.forEach { it.mes(WEAK_SPOT_MESSAGE) }

        val special = fight.nextSpecial ?: deps.random.pick(Special.entries)
        fight.nextSpecial = special.other
        fight.inSpecial = true
        when (special) {
            Special.Lightning -> startLightning(fight, target)
            Special.Smoke -> startSmoke(fight, target)
        }
    }

    private fun animateTails(fight: LeviathanFight) {
        val session = sessionOf(fight.npc) ?: return
        sessionTails[session.id.value]
            ?.filter { it.isSlotAssigned }
            ?.forEach { it.anim(deps.random.pick(TAIL_STUN_SEQS)) }
    }

    private fun startLightning(fight: LeviathanFight, target: Player) {
        val npc = fight.npc
        val centre = fight.arena.centre
        fight.specialAngle = LeviathanArena.bearing(target.coords.x - centre.x, target.coords.z - centre.z)
        arenaPlayers(fight).forEach {
            it.mes(LIGHTNING_MESSAGE)
            it.soundSynth(LIGHTNING_CHARGE_SYNTH)
        }
        npc.resetFaceEntity()
        npc.anim(BEAM_START_SEQ)
        npc.spotanim(BEAM_SPOTANIM)
        deps.suppressAttacks(npc, LIGHTNING_END + SPECIAL_RECOVERY + 2)
        tickLightning(fight, target, elapsed = 0)
    }

    private fun tickLightning(fight: LeviathanFight, target: Player, elapsed: Int) {
        if (fight.ended || !fight.npc.isSlotAssigned || fight.enraged) return
        val npc = fight.npc
        val liveTarget = if (target.isValidTarget()) target else arenaPlayers(fight).firstOrNull()

        if (elapsed % LIGHTNING_ORB_INTERVAL == 0 && elapsed < LIGHTNING_END) launchLightningOrbs(fight)

        val centre = fight.arena.centre
        val wanted =
            liveTarget?.let { LeviathanArena.bearing(it.coords.x - centre.x, it.coords.z - centre.z) }
                ?: fight.specialAngle
        when {
            elapsed < LIGHTNING_BEAM_START -> {
                fight.specialAngle = turnTowards(fight.specialAngle, wanted, LIGHTNING_WINDUP_TURN)
                npc.infoProtocol.setFaceAngle(fight.specialAngle, instant = false)
            }
            elapsed < LIGHTNING_END -> {
                npc.anim(BEAM_LOOP_SEQ)
                npc.spotanim(BEAM_SPOTANIM)
                fireBeam(fight)
                fight.specialAngle = turnTowards(fight.specialAngle, wanted, LIGHTNING_BEAM_TURN)
                npc.infoProtocol.setFaceAngle(fight.specialAngle, instant = false)
            }
            elapsed == LIGHTNING_END -> npc.anim(BEAM_END_SEQ)
            else -> {
                finishSpecial(fight, liveTarget ?: target)
                return
            }
        }
        deps.worldQueues.add(1) { tickLightning(fight, target, elapsed + 1) }
    }

    private fun turnTowards(current: Int, wanted: Int, maxStep: Int): Int {
        val delta = LeviathanArena.angleDelta(wanted, current).coerceIn(-maxStep, maxStep)
        return (current + delta + LeviathanArena.ANGLE_STEPS) % LeviathanArena.ANGLE_STEPS
    }

    private fun fireBeam(fight: LeviathanFight) {
        val centre = fight.arena.centre
        val angle = fight.specialAngle
        val struck = HashSet<CoordGrid>()
        val groundSpot = SpotanimType(BEAM_GROUND_SPOTANIM.asRSCM(RSCMType.SPOTANIM))
        for (depth in 1..LIGHTNING_BEAM_LENGTH) {
            val width = 1 + (depth - 1) / LIGHTNING_WIDEN_EVERY
            val forward = LeviathanArena.step(centre, angle, depth.toDouble())
            val tiles = mutableListOf(forward)
            for (side in 1..(width - 1)) {
                val offset = (side + 1) / 2
                val perpendicular = angle + if (side % 2 == 1) PERPENDICULAR else -PERPENDICULAR
                tiles += LeviathanArena.step(forward, perpendicular, offset.toDouble())
            }
            for (tile in tiles) {
                if (width > 1 && !fight.arena.inSearchBox(tile)) continue
                if (!struck.add(tile)) continue
                deps.worldRepo.spotanimMap(groundSpot, tile, delay = BEAM_GROUND_BASE_DELAY + depth * BEAM_GROUND_STEP_DELAY)
            }
        }
        for (player in arenaPlayers(fight)) {
            player.soundSynth(LIGHTNING_SHOT_SYNTH)
            if (player.coords in struck) {
                player.queueHit(fight.npc, 1, HitType.Typeless, deps.random.of(LIGHTNING_DAMAGE), deps.playerHitModifier)
            }
        }
    }

    private fun launchLightningOrbs(fight: LeviathanFight) {
        val arena = fight.arena
        val targets = HashSet<CoordGrid>()
        val free = freeArenaTiles(fight)
        for (strip in LeviathanArena.LIGHTNING_STRIPS) {
            val inStrip = free.filter { arena.inStrip(it, strip) }.shuffled()
            targets += inStrip.take(deps.random.of(1, 3))
        }
        if (deps.random.randomBoolean(LIGHTNING_PLAYER_CHANCE)) {
            arenaPlayers(fight)
                .filter { p -> LeviathanArena.LIGHTNING_STRIPS.any { arena.inStrip(p.coords, it) } }
                .forEach { targets += it.coords }
        }
        targets.forEach { launchLightningOrb(fight, it) }
    }

    private fun launchLightningOrb(fight: LeviathanFight, tile: CoordGrid) {
        val flight = LIGHTNING_ORB_DELAY + LIGHTNING_ORB_TRAVEL
        deps.bossProjectile(
            spotanim = LIGHTNING_ORB_SPOTANIM.asRSCM(RSCMType.SPOTANIM),
            src = fight.arena.centre,
            target = tile,
            startHeight = LIGHTNING_ORB_START_HEIGHT,
            endHeight = 0,
            delay = LIGHTNING_ORB_DELAY,
            travel = LIGHTNING_ORB_TRAVEL,
            curve = LIGHTNING_ORB_CURVE,
        )
        mapSpot(SHADOW_SPOTANIM, tile, delay = flight - SHADOW_LEAD)
        mapSpot(LIGHTNING_STRIKE_SPOTANIM, tile, delay = flight)
        deps.worldRepo.soundArea(tile, LIGHTNING_STRIKE_SYNTH, delay = flight, radius = 3)
        deps.worldQueues.add(flight / CYCLES_PER_TICK) {
            if (fight.ended) return@add
            arenaPlayers(fight).filter { it.coords == tile }.forEach {
                it.queueHit(fight.npc, 1, HitType.Typeless, deps.random.of(LIGHTNING_DAMAGE), deps.playerHitModifier)
            }
        }
    }

    private fun startSmoke(fight: LeviathanFight, target: Player) {
        fight.npc.facePlayer(target)
        fight.npc.anim(ROCKFALL_SEQ)
        arenaPlayers(fight).forEach {
            it.mes(SMOKE_MESSAGE)
            it.soundSynth(ROCKFALL_SYNTH, delay = 5)
        }
        deps.suppressAttacks(fight.npc, SMOKE_END + SPECIAL_RECOVERY + 2)
        tickSmoke(fight, target, elapsed = 0)
    }

    private fun tickSmoke(fight: LeviathanFight, target: Player, elapsed: Int) {
        if (fight.ended || !fight.npc.isSlotAssigned || fight.enraged) return
        val liveTarget = if (target.isValidTarget()) target else arenaPlayers(fight).firstOrNull()
        when {
            elapsed == SMOKE_SPIT_START -> {
                fight.npc.anim(SPIT_START_SEQ)
                arenaPlayers(fight).forEach { it.soundSynth(SPIT_START_SYNTH) }
            }
            elapsed in SMOKE_FIRST_SPIT until SMOKE_FIRST_SPIT + SMOKE_SPITS -> {
                if (liveTarget != null) spitDebris(fight, liveTarget)
            }
            elapsed == SMOKE_BLAST -> smokeBlast(fight)
            elapsed >= SMOKE_END -> {
                finishSpecial(fight, liveTarget ?: target)
                return
            }
        }
        deps.worldQueues.add(1) { tickSmoke(fight, target, elapsed + 1) }
    }

    private fun spitDebris(fight: LeviathanFight, target: Player) {
        val npc = fight.npc
        npc.anim(SPIT_LOOP_SEQ)
        val tile = target.coords
        val centre = fight.arena.centre
        val bearing = LeviathanArena.bearing(tile.x - centre.x, tile.z - centre.z)
        deps.bossProjectile(
            spotanim = DEBRIS_PROJECTILE.asRSCM(RSCMType.SPOTANIM),
            src = LeviathanArena.step(centre, bearing, ORB_SOURCE_OFFSET),
            target = tile,
            startHeight = DEBRIS_START_HEIGHT,
            endHeight = DEBRIS_END_HEIGHT,
            delay = 0,
            travel = DEBRIS_TRAVEL,
            curve = DEBRIS_CURVE,
        )
        mapSpot(SHADOW_SPOTANIM, tile)
        arenaPlayers(fight).forEach { it.soundSynth(SPIT_LAUNCH_SYNTH) }
        deps.worldQueues.add(DEBRIS_LAND_TICKS) {
            if (fight.ended || !isFreeTile(fight, tile)) return@add
            mapSpot(DEBRIS_IMPACT_SPOTANIM, tile)
            landBoulder(fight, tile, permanent = true, LocAngle[deps.random.of(0, 3)])
        }
    }

    private fun smokeBlast(fight: LeviathanFight) {
        val npc = fight.npc
        val centre = fight.arena.centre
        npc.anim(EXPLOSION_SEQ)
        npc.spotanim(EXPLOSION_SPOTANIM)
        for (tile in freeArenaTiles(fight)) {
            if (!hasLineOfSight(fight, tile)) continue
            val spot = SpotanimType(LeviathanArena.smokeSpotanim(centre, tile).asRSCM(RSCMType.SPOTANIM))
            deps.worldRepo.spotanimMap(spot, tile, delay = LeviathanArena.smokeDelay(centre, tile))
        }
        val max = if (fight.awakened) SMOKE_MAX_HIT_AWAKENED else SMOKE_MAX_HIT
        for (player in arenaPlayers(fight)) {
            val arrival = LeviathanArena.smokeDelay(centre, player.coords) / CYCLES_PER_TICK
            deps.worldQueues.add(max(0, arrival)) {
                if (fight.ended || !player.isValidTarget() || !hasLineOfSight(fight, player.coords)) return@add
                player.queueHit(npc, 1, HitType.Typeless, deps.random.of(0, max), deps.playerHitModifier)
            }
        }
    }

    private fun hasLineOfSight(fight: LeviathanFight, tile: CoordGrid): Boolean =
        rayCast.hasLineOfSight(
            source = fight.npc.coords,
            destination = tile,
            srcWidth = LeviathanArena.BOSS_SIZE,
            srcLength = LeviathanArena.BOSS_SIZE,
        )

    private fun finishSpecial(fight: LeviathanFight, target: Player) {
        fight.inSpecial = false
        val npc = fight.npc
        if (npc.isSlotAssigned) {
            npc.facePlayer(target)
            deps.forceNext(npc, VOLLEY_ABILITY)
        }
        deps.suppressAttacks(npc, SPECIAL_RECOVERY)
    }

    /** Awakened-only: spawns a chasing tornado per arena player once hp drops to [TORNADO_HP_FRACTION]. */
    fun onDamaged(npc: Npc) {
        val fight = fights[npc] ?: return
        if (npc.hitpoints <= 0) return
        if (!fight.awakened || fight.tornadoesSpawned) return
        val fraction = npc.hitpoints.toDouble() / npc.baseHitpointsLvl.coerceAtLeast(1)
        if (fraction > TORNADO_HP_FRACTION) return
        fight.tornadoesSpawned = true
        arenaPlayers(fight).forEach { spawnTornado(fight, it) }
    }

    private fun runEnrageEntry(npc: Npc, target: Player) {
        val fight = fights[npc] ?: return
        if (fight.enraged) return
        fight.enraged = true
        fight.shotsFired = 0
        npc.clearIdleAnim()
        rockfall(fight, withHints = false, animated = true)
        deps.encounter(npc).attackRateOverride =
            if (fight.awakened) ENRAGED_INTERVAL_AWAKENED else ENRAGED_INTERVAL

        val corner = pathfinderStartCorner(fight)
        fight.pathfinderCorner = corner
        val sw = fight.arena.at(LeviathanArena.PATHFINDER_CORNERS[corner])
        val middle = sw.translate(1, 1)
        for (step in 0 until PATHFINDER_SPAWN_PULSES) {
            val delay = step * CYCLES_PER_TICK
            (listOf(0 to 0) + CARDINALS).forEach { (dx, dz) ->
                mapSpot(PATHFINDER_SPAWN_SPOTANIM, middle.translate(dx, dz), delay = delay, height = PATHFINDER_SPAWN_HEIGHT)
            }
        }
        deps.worldQueues.add(PATHFINDER_SPAWN_DELAY) { spawnPathfinder(fight, sw) }
    }

    private fun runEnragedRockfall(npc: Npc) {
        val fight = fights[npc] ?: return
        rockfall(fight, withHints = false, animated = false)
    }

    private fun runEnragedOrb(npc: Npc, target: Player) {
        val fight = fights[npc] ?: return
        val liveTarget = if (target.isValidTarget()) target else arenaPlayers(fight).firstOrNull() ?: return
        npc.facePlayer(liveTarget)
        val fast = fight.awakened
        val first = fight.shotsFired == 0
        fireOrb(fight, liveTarget, deps.random.pick(OrbStyle.DISTANCED), ENRAGED_ORB_DELAY, ENRAGED_ORB_TRAVEL, fast, first)
        fight.shotsFired++
        if (!fast) {
            deps.worldQueues.add(1) { if (npc.isSlotAssigned) npc.anim(ORB_SINGLE_SEQ) }
        }
    }

    private fun pathfinderStartCorner(fight: LeviathanFight): Int =
        LeviathanArena.PATHFINDER_CORNERS.indices.firstOrNull { index ->
            val sw = fight.arena.at(LeviathanArena.PATHFINDER_CORNERS[index])
            (0 until LeviathanArena.PATHFINDER_SIZE).all { dx ->
                (0 until LeviathanArena.PATHFINDER_SIZE).all { dz -> sw.translate(dx, dz) !in fight.rubble }
            }
        } ?: 0

    private fun spawnPathfinder(fight: LeviathanFight, sw: CoordGrid) {
        if (fight.ended || !fight.npc.isSlotAssigned) return
        val type = ServerCacheManager.getNpc(PATHFINDER_NPC.asRSCM(RSCMType.NPC)) ?: return
        val pathfinder = Npc(type, sw)
        pathfinder.mode = NpcMode.None
        deps.npcRepo.add(pathfinder, Int.MAX_VALUE)
        pathfinder.anim(PATHFINDER_SPAWN_SEQ)
        fight.pathfinder = pathfinder
        val next = fight.arena.at(LeviathanArena.PATHFINDER_CORNERS[(fight.pathfinderCorner + 1) % 4])
        pathfinder.faceSquare(next)
        for (player in arenaPlayers(fight)) {
            player.mes(ENRAGE_MESSAGE)
            player.soundSynth(PATHFINDER_SPAWN_SYNTH)
        }
        deps.worldQueues.add(PATHFINDER_WALK_DELAY) { patrol(fight) }
        watchAura(fight)
    }

    private fun patrol(fight: LeviathanFight) {
        val pathfinder = fight.pathfinder ?: return
        if (fight.ended || !pathfinder.isSlotAssigned) return
        fight.pathfinderCorner = (fight.pathfinderCorner + 1) % LeviathanArena.PATHFINDER_CORNERS.size
        val dest = fight.arena.at(LeviathanArena.PATHFINDER_CORNERS[fight.pathfinderCorner])
        pathfinder.walkTo(routeFactory, dest) { patrol(fight) }
    }

    /** Tints arena players standing inside the pathfinder's protective aura, tick after tick. */
    private fun watchAura(fight: LeviathanFight) {
        deps.repeatTick(
            ticks = Int.MAX_VALUE,
            onTick = { _ ->
                if (fight.ended || fight.pathfinder?.isSlotAssigned != true) return@repeatTick false
                arenaPlayers(fight).filter { insideAura(fight, it) }.forEach { it.tint(AURA_TINT) }
                true
            },
        )
    }

    fun insideAura(fight: LeviathanFight, player: Player): Boolean {
        val pathfinder = fight.pathfinder ?: return false
        if (!pathfinder.isSlotAssigned) return false
        val border = if (fight.awakened) AURA_BORDER_AWAKENED else AURA_BORDER
        val sw = pathfinder.coords
        val size = LeviathanArena.PATHFINDER_SIZE
        return player.coords.x in (sw.x - border)..(sw.x + size - 1 + border) &&
            player.coords.z in (sw.z - border)..(sw.z + size - 1 + border)
    }

    private fun spawnTornado(fight: LeviathanFight, player: Player) {
        val tile =
            freeArenaTiles(fight)
                .filter {
                    val d = max(abs(it.x - player.coords.x), abs(it.z - player.coords.z))
                    d in TORNADO_MIN_DISTANCE..TORNADO_MAX_DISTANCE
                }
                .randomOrNull() ?: return
        val tornado = LeviathanFight.Tornado(player, null, tile)
        fight.tornadoes += tornado
        tornado.npc = addTornadoNpc(tile)
        watchTornado(fight, tornado)
    }

    private fun addTornadoNpc(tile: CoordGrid): Npc? {
        val type = ServerCacheManager.getNpc(TORNADO_NPC.asRSCM(RSCMType.NPC)) ?: return null
        val npc = Npc(type, tile)
        npc.mode = NpcMode.None
        deps.npcRepo.add(npc, Int.MAX_VALUE)
        return npc
    }

    /** Chases [tornado]'s target down and hits it on contact, respawning a few ticks later. */
    private fun watchTornado(fight: LeviathanFight, tornado: LeviathanFight.Tornado) {
        deps.repeatTick(
            ticks = Int.MAX_VALUE,
            onTick = { _ ->
                if (fight.ended || !tornado.target.isValidTarget()) return@repeatTick false
                tickTornado(fight, tornado)
                true
            },
        )
    }

    private fun tickTornado(fight: LeviathanFight, tornado: LeviathanFight.Tornado) {
        val target = tornado.target
        val npc = tornado.npc
        if (npc == null || !npc.isSlotAssigned) {
            if (now >= tornado.respawnTick) tornado.npc = addTornadoNpc(tornado.home)
            return
        }
        if (npc.coords == target.coords) {
            target.queueHit(fight.npc, 1, HitType.Typeless, deps.random.of(TORNADO_DAMAGE), deps.playerHitModifier)
            target.spotanim(TORNADO_HIT_SPOTANIM)
            deps.worldRepo.soundArea(npc.coords, TORNADO_SYNTH, radius = 6)
            tornado.home = npc.coords
            tornado.respawnTick = now + TORNADO_RESPAWN_TICKS
            deps.npcRepo.del(npc, Int.MAX_VALUE)
            tornado.npc = null
            return
        }
        val step = stepTowards(npc.coords, target.coords)
        if (fight.arena.inSearchBox(step)) npc.walk(step)
    }

    private fun stepTowards(from: CoordGrid, to: CoordGrid): CoordGrid {
        val dx = (to.x - from.x).coerceIn(-1, 1)
        val dz = (to.z - from.z).coerceIn(-1, 1)
        return from.translate(dx, dz)
    }

    fun onDeath(npc: Npc) {
        val fight = fights[npc] ?: return
        deps.worldRepo.soundArea(fight.arena.centre, DEATH_SYNTH, delay = DEATH_SYNTH_DELAY, radius = 15)
        end(fight, clearRubble = false)
        deps.worldQueues.add(RUBBLE_CLEAR_DELAY) { clearRubble(fight) }
    }

    private fun abandon(fight: LeviathanFight) {
        end(fight, clearRubble = true)
        if (fight.npc.isSlotAssigned) deps.npcRepo.del(fight.npc, Int.MAX_VALUE)
    }

    fun onDeleted(npc: Npc) {
        val fight = fights.remove(npc) ?: return
        if (!fight.ended) end(fight, clearRubble = true)
    }

    private fun end(fight: LeviathanFight, clearRubble: Boolean) {
        fight.ended = true
        fight.pathfinder?.let { if (it.isSlotAssigned) deps.npcRepo.del(it, Int.MAX_VALUE) }
        fight.pathfinder = null
        fight.tornadoes.forEach { t -> t.npc?.let { if (it.isSlotAssigned) deps.npcRepo.del(it, Int.MAX_VALUE) } }
        fight.tornadoes.clear()
        if (clearRubble) clearRubble(fight)
    }

    private fun clearRubble(fight: LeviathanFight) {
        for ((tile, loc) in fight.rubble) {
            locRepo.del(loc, Int.MAX_VALUE)
            mapSpot(RUBBLE_BREAK_SPOTANIM, tile)
        }
        fight.rubble.clear()
    }

    fun sessionOf(npc: Npc): InstanceSession? = instances.instanceForNpc(npc)?.let(instances::sessionForId)

    fun dropCoords(npc: Npc): CoordGrid {
        val fight = fights[npc] ?: return npc.coords
        return arenaPlayers(fight).firstOrNull()?.coords ?: npc.coords
    }

    fun afterKill(session: InstanceSession) {
        setBoat(session, escape = false)
        setHandholds(session, HANDHOLDS_EXIT_LOC)
        deps.worldQueues.add(RESPAWN_TICKS) {
            if (bossOf(session) != null || session.id.value in pendingSpawns) return@add
            val coords = instances.resolveCoord(session, LeviathanArena.BOSS_SPAWN) ?: return@add
            val arena = Arena.forBoss(coords)
            val player =
                deps.playerList.firstOrNull {
                    it.isValidTarget() &&
                        instances.sessionForPlayer(it)?.id == session.id &&
                        arena.inSearchBox(it.coords) &&
                        !isOnIsland(arena, it.coords)
                } ?: return@add
            beginEncounter(session, player, awakened = false, delay = 0)
        }
    }

    fun beginEncounter(session: InstanceSession, player: Player, awakened: Boolean, delay: Int) {
        setHandholds(session, HANDHOLDS_SEALED_LOC)
        setBoat(session, escape = true)
        if (delay <= 0) {
            if (bossOf(session) == null) spawn(session, player, awakened)
        } else {
            scheduleSpawn(session, player, awakened, delay)
        }
    }

    fun placeArenaLocs(session: InstanceSession) {
        val active = hasBoss(session)
        setHandholds(session, if (active) HANDHOLDS_SEALED_LOC else HANDHOLDS_ENTER_LOC)
        setBoat(session, escape = active)
        instances.resolveCoord(session, LeviathanArena.HANDHOLDS_NOOP)?.let {
            locRepo.add(it, HANDHOLDS_NOOP_LOC, Int.MAX_VALUE, LocAngle.East, LocShape.CentrepieceStraight)
        }
    }

    fun setHandholds(session: InstanceSession, loc: String) {
        val coords = instances.resolveCoord(session, LeviathanArena.HANDHOLDS) ?: return
        locRepo.add(coords, loc, Int.MAX_VALUE, LocAngle.West, LocShape.CentrepieceStraight)
    }

    private fun setBoat(session: InstanceSession, escape: Boolean) {
        val coords = instances.resolveCoord(session, LeviathanArena.ISLAND_BOAT) ?: return
        val loc = if (escape) BOAT_ESCAPE_LOC else BOAT_LEAVE_LOC
        locRepo.add(coords, loc, Int.MAX_VALUE, LocAngle.South, LocShape.CentrepieceStraight)
    }

    fun ensureTails(session: InstanceSession) {
        if (sessionTails[session.id.value]?.all { it.isSlotAssigned } == true) return
        sessionTails[session.id.value]?.forEach { if (it.isSlotAssigned) deps.npcRepo.del(it, Int.MAX_VALUE) }
        sessionTails[session.id.value] =
            LeviathanArena.TAILS.mapNotNull { tail ->
                val coords = instances.resolveCoord(session, tail.coords) ?: return@mapNotNull null
                val faces = instances.resolveCoord(session, tail.faces) ?: return@mapNotNull null
                val type = ServerCacheManager.getNpc(tail.npc.asRSCM(RSCMType.NPC)) ?: return@mapNotNull null
                val npc = Npc(type, coords)
                npc.mode = NpcMode.None
                deps.npcRepo.add(npc, Int.MAX_VALUE)
                instances.attachNpc(session.id, npc)
                npc.faceSquare(faces)
                npc
            }
    }

    private fun mapSpot(spot: String, tile: CoordGrid, delay: Int = 0, height: Int = 0) {
        deps.worldRepo.spotanimMap(SpotanimType(spot.asRSCM(RSCMType.SPOTANIM)), tile, height, delay)
    }

    companion object {
        const val BOSS_NPC = "npc.leviathan"
        const val HANDHOLDS_ENTER_LOC = "loc.leviathan_wall_climb"
        const val HANDHOLDS_EXIT_LOC = "loc.leviathan_wall_climb_quest_exit"
        const val HANDHOLDS_SEALED_LOC = "loc.dt2_scar_wallkit01_short"
        const val HANDHOLDS_NOOP_LOC = "loc.leviathan_wall_climb_noop"
        const val BOAT_ESCAPE_LOC = "loc.dt2_scar_boat_island_escape"
        const val BOAT_LEAVE_LOC = "loc.dt2_scar_boat_island_leave"

        const val VOLLEY_EXT = "leviathan.volley"
        const val ENRAGE_ENTRY_EXT = "leviathan.enrage_entry"
        const val ENRAGED_ROCKFALL_EXT = "leviathan.enraged_rockfall"
        const val ENRAGED_ORB_EXT = "leviathan.enraged_orb"
        private const val BITE_ABILITY = "bite"
        private const val VOLLEY_ABILITY = "volley"

        private const val RESPAWN_TICKS = 30
        private const val CYCLES_PER_TICK = 30

        private const val AWAKENED_HITPOINTS = 2700
        private const val AWAKENED_ATTACK = 525
        private const val AWAKENED_STRENGTH = 630
        private const val AWAKENED_DEFENCE = 287
        private const val AWAKENED_RANGED = 280
        private const val AWAKENED_MAGIC = 280

        private val RISE_FACE = CoordGrid(2064, 6369, 0)
        private const val RISE_SEQ = "seq.npc_leviathan_01_spawn"
        private const val RISE_SYNTH = "synth.leviathan_rise"
        private const val FIRST_VOLLEY_DELAY = 4
        private const val ABANDON_TICKS = 5

        private const val ORB_START_SEQ = "seq.npc_leviathan_01_projectile_start_01"
        private const val ORB_LOOP_SEQ = "seq.npc_leviathan_01_projectile_loop_01"
        private const val ORB_SINGLE_SEQ = "seq.npc_leviathan_01_projectile_single_01"
        private const val ORB_START_HEIGHT = 548
        private const val ORB_END_HEIGHT = 100
        private const val ORB_CURVE = 30
        private const val ORB_PROGRESS = 124
        private const val ORB_SOURCE_OFFSET = 2.5
        private const val ORB_IMPACT_HEIGHT = 100

        // interval = ticks between shots; orbDelay/orbTravel = the projectile's own cycle timings
        // (delay before it starts moving, then cycles in flight). Only interval shortens the
        // ladder's pace - individual orbs still fly the same arc, just launched closer together.
        private val VOLLEY_STAGES =
            listOf(
                VolleyStage(interval = 3, shots = 6, allStyles = false, orbDelay = 30, orbTravel = 90),
                VolleyStage(interval = 2, shots = 8, allStyles = false, orbDelay = 30, orbTravel = 60),
                VolleyStage(interval = 1, shots = 8, allStyles = true, orbDelay = 30, orbTravel = 30),
                VolleyStage(interval = 1, shots = 12, allStyles = false, orbDelay = 30, orbTravel = 30),
                VolleyStage(interval = 1, shots = 10, allStyles = true, orbDelay = 30, orbTravel = 30),
                VolleyStage(interval = 1, shots = 10, allStyles = true, orbDelay = 0, orbTravel = 15),
                VolleyStage(interval = 1, shots = 12, allStyles = true, orbDelay = 0, orbTravel = 15),
            )

        private const val PROTECT_FROM_MELEE = "varbit.prayer_protectfrommelee"
        private const val PROTECT_FROM_MISSILES = "varbit.prayer_protectfrommissiles"
        private const val PROTECT_FROM_MAGIC = "varbit.prayer_protectfrommagic"

        private const val BITE_RANGE = 5
        private const val BITE_MAX_HIT = 50
        private const val BITE_MAX_HIT_AWAKENED = 86

        private const val ROCKFALL_SEQ = "seq.npc_leviathan_01_stun_01"
        private const val ROCKFALL_SYNTH = "synth.leviathan_rockfall"
        private const val ROCKFALL_RUMBLE_SYNTH = "synth.leviathan_rockfall_rumble"
        private const val ROCKFALL_RECOVERY = 5
        private const val ROCKFALL_MIN = 12
        private const val ROCKFALL_MAX = 30
        private val ROCKFALL_CHIP = 5..10
        private val BOULDER_DAMAGE = 10..30
        private const val HINT_DELAY = 50
        private const val HINT_SEARCH_RADIUS = 4
        private val BREAK_DELAYS = listOf(20, 50, 80)
        private const val BOULDER_LAND_BASE = 6
        private val BREAK_SPOTANIMS =
            listOf("spotanim.spotanim_brain_01_falling_break_01", "spotanim.spotanim_brain_01_falling_break_02")
        private val STAY_SPOTANIMS =
            listOf(
                "spotanim.spotanim_brain_01_falling_stay_01",
                "spotanim.spotanim_brain_01_falling_stay_02",
                "spotanim.spotanim_brain_01_falling_stay_03",
                "spotanim.spotanim_brain_01_falling_stay_04",
            )
        private const val RUBBLE_LOC = "loc.leviathan_rubble"
        private const val RUBBLE_BREAK_SPOTANIM = "spotanim.projanim_brain_01_impact_02"
        private const val RUBBLE_CLEAR_DELAY = 7
        private const val DEBRIS_IMPACT_SYNTH = "synth.leviathan_debris_impact"
        private const val RUBBLE_LAND_SYNTH = "synth.leviathan_rubble_land"
        private const val KNOCKBACK_SEQ = "seq.agilityarena_player_spikedback"

        private val CARDINALS = listOf(0 to 1, 1 to 0, 0 to -1, -1 to 0)
        private val ALL_DIRECTIONS = CARDINALS + listOf(1 to 1, 1 to -1, -1 to -1, -1 to 1)

        private const val STUN_SEQ = "seq.npc_leviathan_01_projectile_end_01"
        private const val STUN_IDLE_SEQ = "seq.npc_leviathan_01_stun_idle"
        private const val STUN_TICKS = 15
        private const val STUN_TICKS_AWAKENED = 8
        private const val STUNNED_DAMAGE_CAP = 10
        private const val WEAK_SPOT_ARC = 650
        private const val WEAK_SPOT_MIN_PERCENT = 65
        private val TAIL_STUN_SEQS =
            listOf(
                "seq.npc_leviathan_tail01_stun01",
                "seq.npc_leviathan_tail01_stunvariant01",
                "seq.npc_leviathan_tail01_stunvariant02",
            )
        private const val STUN_MESSAGE = "<col=06600c>Your spell stuns the Leviathan!</col>"
        private const val WEAK_SPOT_MESSAGE = "<col=06600c>You hit the Leviathan right in its weak spot!</col>"

        private const val SPECIAL_DAMAGE_PERCENT = 67
        private const val SPECIAL_RECOVERY = 2

        private const val LIGHTNING_MESSAGE = "<col=a53fff>The Leviathan charges up a lightning attack..."
        private const val LIGHTNING_CHARGE_SYNTH = "synth.leviathan_lightning_charge"
        private const val LIGHTNING_SHOT_SYNTH = "synth.leviathan_lightning_shot"
        private const val BEAM_START_SEQ = "seq.npc_leviathan_01_360_start"
        private const val BEAM_LOOP_SEQ = "seq.npc_leviathan_01_360_loop"
        private const val BEAM_END_SEQ = "seq.npc_leviathan_01_360_end"
        private const val BEAM_SPOTANIM = "spotanim.beam_360_attack_vfx_01"
        private const val BEAM_GROUND_SPOTANIM = "spotanim.vfx_leviathan_360_ground"
        private const val BEAM_GROUND_BASE_DELAY = 2
        private const val BEAM_GROUND_STEP_DELAY = 2
        private const val LIGHTNING_BEAM_START = 3
        private const val LIGHTNING_END = LIGHTNING_BEAM_START + 35
        private const val LIGHTNING_WINDUP_TURN = 300
        private const val LIGHTNING_BEAM_TURN = 100
        private const val LIGHTNING_BEAM_LENGTH = 15
        private const val LIGHTNING_WIDEN_EVERY = 3
        private const val PERPENDICULAR = 512
        private val LIGHTNING_DAMAGE = 20..30
        private const val LIGHTNING_ORB_INTERVAL = 4
        private const val LIGHTNING_PLAYER_CHANCE = 3
        private const val LIGHTNING_ORB_SPOTANIM = "spotanim.dt2_leviathan_bomb01"
        private const val LIGHTNING_STRIKE_SPOTANIM = "spotanim.vfx_leviathan_360_lightning"
        private const val LIGHTNING_STRIKE_SYNTH = "synth.leviathan_orb_magic"
        private const val LIGHTNING_ORB_START_HEIGHT = 800
        private const val LIGHTNING_ORB_DELAY = 40
        private const val LIGHTNING_ORB_TRAVEL = 110
        private const val LIGHTNING_ORB_CURVE = 3
        private const val SHADOW_SPOTANIM = "spotanim.gargboss_debris_shadow_90"
        private const val SHADOW_LEAD = 60

        private const val SMOKE_MESSAGE = "<col=a53fff>The Leviathan begins to spit out debris..."
        private const val SPIT_START_SEQ = "seq.npc_leviathan_01_spit_start"
        private const val SPIT_LOOP_SEQ = "seq.npc_leviathan_01_spit_loop"
        private const val EXPLOSION_SEQ = "seq.npc_leviathan_01_explosion_start"
        private const val EXPLOSION_SPOTANIM = "spotanim.spotanim_leviathan_explode_02"
        private const val SPIT_START_SYNTH = "synth.leviathan_spit_start"
        private const val SPIT_LAUNCH_SYNTH = "synth.leviathan_spit_launch"
        private const val SMOKE_SPIT_START = 4
        private const val SMOKE_FIRST_SPIT = 5
        private const val SMOKE_SPITS = 10
        private const val SMOKE_BLAST = SMOKE_FIRST_SPIT + SMOKE_SPITS + 2
        private const val SMOKE_END = SMOKE_BLAST + 3
        private const val SMOKE_MAX_HIT = 60
        private const val SMOKE_MAX_HIT_AWAKENED = 75
        private const val DEBRIS_PROJECTILE = "spotanim.projanim_brain_01"
        private const val DEBRIS_IMPACT_SPOTANIM = "spotanim.projanim_brain_01_impact_01"
        private const val DEBRIS_START_HEIGHT = 600
        private const val DEBRIS_END_HEIGHT = 0
        private const val DEBRIS_TRAVEL = 60
        private const val DEBRIS_CURVE = 40
        private const val DEBRIS_LAND_TICKS = 2

        private const val ENRAGED_ORB_DELAY = 30
        private const val ENRAGED_ORB_TRAVEL = 120

        const val ENRAGE_HP_FRACTION = 0.2
        private const val ENRAGE_MESSAGE = "<col=ff289d>The Leviathan focuses on you intensely...</col>"
        const val ENRAGED_ROCKFALL_INTERVAL = 8
        private const val ENRAGED_INTERVAL = 2
        private const val ENRAGED_INTERVAL_AWAKENED = 1
        private const val ENRAGED_PENETRATION = 25
        private const val OUTSIDE_AURA_DAMAGE_PERCENT = 50
        private const val AURA_MIN_PERCENT = 25
        private const val AURA_BORDER = 1
        private const val AURA_BORDER_AWAKENED = 2
        private val AURA_TINT =
            EntityTinting(startCycle = 0, endCycle = 30, hue = 9, saturation = 7, lightness = 70, weight = 70)

        private const val PATHFINDER_NPC = "npc.leviathan_buff_npc"
        private const val PATHFINDER_SPAWN_SEQ = "seq.npc_buff01_spawn01"
        private const val PATHFINDER_SPAWN_SPOTANIM = "spotanim.leviathan_buff_spawn"
        private const val PATHFINDER_SPAWN_SYNTH = "synth.leviathan_pathfinder_spawn"
        private const val PATHFINDER_SPAWN_PULSES = 4
        private const val PATHFINDER_SPAWN_HEIGHT = 124
        private const val PATHFINDER_SPAWN_DELAY = 3
        private const val PATHFINDER_WALK_DELAY = 3

        private const val TORNADO_HP_FRACTION = 0.5
        private const val TORNADO_NPC = "npc.leviathan_tornado"
        private const val TORNADO_HIT_SPOTANIM = "spotanim.leviathan_tornado_spotanim"
        private const val TORNADO_SYNTH = "synth.leviathan_tornado"
        private val TORNADO_DAMAGE = 40..50
        private const val TORNADO_RESPAWN_TICKS = 8
        private const val TORNADO_MIN_DISTANCE = 3
        private const val TORNADO_MAX_DISTANCE = 6

        private const val DEATH_SYNTH = "synth.leviathan_death"
        private const val DEATH_SYNTH_DELAY = 9
    }
}
