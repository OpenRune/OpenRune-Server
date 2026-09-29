package org.rsmod.api.bosses.runtime

import kotlin.math.abs
import kotlin.random.Random
import org.rsmod.api.bosses.spec.*
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

class BossEncounter(
    val npc: Npc,
    val spec: BossSpec,
) {
    var currentPhaseName: String = spec.phases.keys.firstOrNull() ?: ""

    var phaseEnteredTick: Int = -1
    var lastAbilityTick: Int = 0
    var lastAbilityName: String? = null
    var invulnerable: Boolean = false
    var damageScale: Double = 1.0
    var lethalHandled: Boolean = false

    /** Tick until which multi-tick effects are still running; nothing new may start before it. */
    var busyUntil: Int = 0

    private val ownedLocs = mutableMapOf<CoordGrid, OwnedLoc>()

    fun ownsLocAt(tile: CoordGrid): Boolean = tile in ownedLocs

    internal fun addOwnedLoc(tile: CoordGrid, loc: OwnedLoc) {
        ownedLocs[tile] = loc
    }

    private val ownedNpcs = mutableListOf<Npc>()

    internal fun addOwnedNpc(npc: Npc) {
        ownedNpcs += npc
    }

    fun releaseOwnedNpcs(): List<Npc> {
        val released = ownedNpcs.toList()
        ownedNpcs.clear()
        return released
    }

    /** Hands over every loc this encounter spawned, e.g. to remove them on a delay after death. */
    fun releaseOwnedLocs(): Map<CoordGrid, OwnedLoc> {
        val released = ownedLocs.toMap()
        ownedLocs.clear()
        return released
    }

    /** Bumped by [interrupt]; deferred ability steps started under an older epoch are dropped. */
    var epoch: Int = 0
        private set

    fun interrupt(tick: Int) {
        epoch++
        busyUntil = tick
    }
    internal val usedAbilities = mutableSetOf<String>()
    private var queuedAbility: String? = null

    /** Runs [ability] as the next priority ability once [busyUntil] has passed; replaces any earlier queue. */
    fun forceNext(ability: String) {
        require(ability in spec.abilities) { "Ability '$ability' does not exist in boss spec." }
        queuedAbility = ability
    }

    /**
     * Per-encounter attack-rate override (ticks between ability uses). Takes precedence over
     * [PhaseSpec.attackRate] and [BossStats.attackRate]. Null by default and recreated with the
     * encounter on respawn, so bosses that never set it are unaffected.
     */
    var attackRateOverride: Int? = null

    internal val firedTriggers = mutableSetOf<Int>()
    internal val firedPhaseEntries = mutableSetOf<String>()
    private val cooldowns = mutableMapOf<String, Int>()
    private val forcedTickLastFired = mutableMapOf<String, Int>()
    private var rotationCursor = 0
    private var rotationStarted = false
    private var basicAttackCount = 0
    private var forceAttackThreshold = -1

    init {
        npc.movementLocked = currentPhase?.lockMovement == true
    }

    val currentPhase: PhaseSpec?
        get() = spec.phases[currentPhaseName]

    fun transitionTo(phaseName: String, tick: Int) {
        val from = currentPhaseName
        currentPhaseName = phaseName
        queuedAbility = null
        phaseEnteredTick = tick
        rotationCursor = 0
        rotationStarted = false
        cooldowns.clear()
        forcedTickLastFired.clear()
        basicAttackCount = 0
        forceAttackThreshold = -1

        val phase = spec.phases[phaseName]

        val idle = phase?.idleAnim
        if (idle != null) npc.setIdleAnim(idle) else npc.clearIdleAnim()

        npc.movementLocked = phase?.lockMovement == true

        npc.clearFacingLock()
    }

    fun selectAbility(selector: Selector, tick: Int, target: Player? = null): String? {
        val phase = currentPhase ?: return null

        for (forced in phase.forceAbilities) {
            if (forced.condition != null || forced.attackMin != null) continue
            val lastFired = forcedTickLastFired[forced.ability] ?: phaseEnteredTick
            if (tick - lastFired >= forced.period) {
                forcedTickLastFired[forced.ability] = tick
                return forced.ability
            }
        }

        val attackForced =
            phase.forceAbilities.firstOrNull { it.condition == null && it.attackMin != null }
        if (attackForced != null) {
            if (forceAttackThreshold < 0) {
                forceAttackThreshold = randomThreshold(attackForced)
            }
            if (basicAttackCount >= forceAttackThreshold) {
                basicAttackCount = 0
                forceAttackThreshold = randomThreshold(attackForced)
                return attackForced.ability
            }
        }

        val selected = pick(selector, tick, target)
        if (selected != null && attackForced != null) {
            basicAttackCount++
        }
        return selected
    }

    /**
     * Picks a key from [selector] alone, ignoring the phase's forced abilities; used by
     * [Effect.Choose] so a nested branch pick can't consume a phase-level force.
     */
    fun pick(selector: Selector, tick: Int, target: Player? = null): String? =
        when (selector) {
            is Selector.WeightedRandom -> selectWeightedRandom(selector, tick, target)
            is Selector.Rotation -> selectRotation(selector)
            is Selector.Conditional -> null
        }

    fun selectPriorityAbility(tick: Int, target: Player?): String? {
        if (tick < busyUntil) return null
        queuedAbility?.let {
            queuedAbility = null
            return it
        }
        val phase = currentPhase ?: return null
        for (forced in phase.forceAbilities) {
            val condition = forced.condition ?: continue
            if (forced.once && forced.ability in usedAbilities) continue
            if (evaluate(condition, target)) return forced.ability
        }
        return null
    }

    private fun randomThreshold(forced: ForcedAbility): Int {
        val min = forced.attackMin ?: return Int.MAX_VALUE
        val max = (forced.attackMax ?: min).coerceAtLeast(min)
        return if (max == min) min else Random.nextInt(min, max + 1)
    }

    private fun selectWeightedRandom(selector: Selector.WeightedRandom, tick: Int, target: Player? = null): String? {
        val available = selector.entries.filter { ref ->
            val onCooldown = cooldowns[ref.ability]?.let { tick - it < ref.cooldown } ?: false
            !onCooldown && evaluate(ref.requires, target)
        }

        if (available.isEmpty()) return null

        val totalWeight = available.sumOf { it.weight }
        if (totalWeight <= 0) return null

        var roll = Random.nextInt(totalWeight)
        for (ref in available) {
            roll -= ref.weight
            if (roll < 0) {
                cooldowns[ref.ability] = tick
                return ref.ability
            }
        }
        return available.last().ability
    }

    private fun selectRotation(selector: Selector.Rotation): String? {
        if (selector.sequence.isEmpty()) return null
        if (!rotationStarted) {
            rotationStarted = true
            if (selector.randomStart) {
                rotationCursor = Random.nextInt(selector.sequence.size)
            }
        }
        val ability = selector.sequence[rotationCursor % selector.sequence.size]
        rotationCursor++
        return ability
    }

    fun evaluate(condition: Condition, target: Player? = null): Boolean {
        return when (condition) {
            is Condition.Always -> true
            is Condition.OnSpawn -> false
            is Condition.OnDeath -> false
            is Condition.WithinMeleeRange -> {
                target != null && npc.isWithinDistance(target, 1)
            }
            is Condition.HpBelow -> {
                val fraction = npc.hitpoints.toDouble() / npc.baseHitpointsLvl.coerceAtLeast(1)
                fraction < condition.fraction
            }
            is Condition.HpExact -> npc.hitpoints == condition.hp
            is Condition.InPhase -> currentPhaseName == condition.phase
            is Condition.AbilityUsed -> condition.ability in usedAbilities
            is Condition.VarnIn -> npc.vars[condition.varn] in condition.range
            is Condition.LastAbility -> lastAbilityName == condition.ability
            is Condition.TargetWithin -> {
                target != null && target.coords.chebyshevDistance(tileOf(condition.of, target)) <= condition.distance
            }
            is Condition.TargetInArc -> {
                val wanted = Angles.normalise(npc.vars[condition.bearingVarn] + condition.offset)
                target != null &&
                    abs(Angles.delta(Angles.bearing(npc.centreTile, target.coords), wanted)) <= condition.halfArc
            }
            is Condition.Custom -> condition.test(npc, target)
            is Condition.Not -> !evaluate(condition.c, target)
            is Condition.And -> evaluate(condition.a, target) && evaluate(condition.b, target)
            is Condition.Or -> evaluate(condition.a, target) || evaluate(condition.b, target)
            is Condition.EveryNTicks -> false
            is Condition.OnPhaseTick -> false
            is Condition.IncomingHitDamageAtLeast -> false
            is Condition.PlayerEnterRange -> false
            is Condition.TargetPraying -> target != null && target.isProtectingFrom(condition.type)
        }
    }

    private fun tileOf(expr: TargetExpr.Single, target: Player): CoordGrid =
        when (expr) {
            is TargetExpr.CurrentTarget,
            is TargetExpr.CurrentTargetTile -> target.coords
            is TargetExpr.Centre -> npc.centreTile
            is TargetExpr.SpawnTile -> npc.spawnCoords.translate(expr.dx, expr.dz)
            is TargetExpr.Toward -> resolveToward(expr) { tileOf(it, target) }
            else -> npc.coords
        }

    private fun Player.isProtectingFrom(type: HitType): Boolean =
        when (type) {
            HitType.Melee -> vars[PROTECT_FROM_MELEE] > 0
            HitType.Ranged -> vars[PROTECT_FROM_MISSILES] > 0
            HitType.Magic,
            HitType.Dragonfire,
            HitType.DragonfireMetal,
            HitType.WyvernIce -> vars[PROTECT_FROM_MAGIC] > 0
            HitType.Typeless -> false
        }

    private companion object {
        private const val PROTECT_FROM_MELEE = "varbit.prayer_protectfrommelee"
        private const val PROTECT_FROM_MISSILES = "varbit.prayer_protectfrommissiles"
        private const val PROTECT_FROM_MAGIC = "varbit.prayer_protectfrommagic"
    }
}
