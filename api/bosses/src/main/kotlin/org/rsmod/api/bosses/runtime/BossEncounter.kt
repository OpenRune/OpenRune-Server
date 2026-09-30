package org.rsmod.api.bosses.runtime

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcServerType
import kotlin.math.abs
import kotlin.random.Random
import org.rsmod.annotations.InternalApi
import org.rsmod.api.bosses.spec.*
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

class BossEncounter(
    val npc: Npc,
    val spec: BossSpec,
    private val mapClock: MapClock,
    private val npcType: (String) -> NpcServerType? = ::cacheNpcType,
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

    /** The player the latest combat tick ran against; timers fire against it. */
    var lastTarget: Player? = null

    /** Bumped on every [transitionTo], so phase timers can tell the phase was (re-)entered. */
    internal var phaseEpoch: Int = 0
        private set

    internal var timersStarted: Boolean = false

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

    /**
     * Tick the next attack may start, set by the running ability's attack delay or
     * [Effect.NextAttackIn]. While null, the next attack waits the attack rate after
     * [lastAbilityTick]. Cleared when the next attack starts.
     */
    var nextAttackTick: Int? = null

    internal fun attackReady(tick: Int): Boolean {
        val attackRate = attackRateOverride ?: currentPhase?.attackRate ?: spec.stats.attackRate
        return tick >= (nextAttackTick ?: (lastAbilityTick + attackRate))
    }

    /** Records [ability] as started by the attack loop on [tick]; call before running it. */
    internal fun startAttack(ability: String, tick: Int) {
        lastAbilityTick = tick
        lastAbilityName = ability
        usedAbilities += ability
        nextAttackTick = spec.abilityAttackDelays[ability]?.let { tick + it }
    }

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

    /** Switches phase state only; does not run the phase's [PhaseSpec.entry]. */
    fun transitionTo(phaseName: String, tick: Int) {
        val from = currentPhaseName
        currentPhaseName = phaseName
        queuedAbility = null
        phaseEnteredTick = tick
        phaseEpoch++
        rotationCursor = 0
        rotationStarted = false
        cooldowns.clear()
        forcedTickLastFired.clear()
        basicAttackCount = 0
        forceAttackThreshold = -1

        val phase = spec.phases[phaseName]

        phase?.transmog?.let(npcType)?.let { npc.bossTransmog(it, Int.MAX_VALUE) }

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

    /**
     * [tiles] is the effect this condition runs inside, if any, so conditions see the same bound
     * tiles and tile sets as the effects around them. [hit] is the player hit an incoming rule or
     * hit reaction is being checked for.
     */
    fun evaluate(
        condition: Condition,
        target: Player? = null,
        tiles: TileScope? = null,
        hit: HitContext? = null,
    ): Boolean {
        fun eval(inner: Condition) = evaluate(inner, target, tiles, hit)
        fun requireHit(): HitContext =
            checkNotNull(hit) { "$condition evaluated outside an incoming hit." }
        return when (condition) {
            is Condition.Always -> true
            is Condition.WithinMeleeRange -> {
                target != null && npc.isWithinDistance(target, 1)
            }
            is Condition.HpBelow ->
                if (condition.inclusive) hpFraction <= condition.fraction else hpFraction < condition.fraction
            is Condition.HpExact -> npc.hitpoints == condition.hp
            is Condition.InPhase -> currentPhaseName == condition.phase
            is Condition.AbilityUsed -> condition.ability in usedAbilities
            is Condition.VarnIn -> npc.vars[condition.varn] in condition.range
            is Condition.VarnExpired -> {
                val deadline = npc.vars[condition.varn]
                deadline > 0 && deadline <= mapClock.cycle
            }
            is Condition.LastAbility -> lastAbilityName == condition.ability
            is Condition.TargetWithin -> {
                if (target == null) return false
                val of = tiles?.tile(condition.of) ?: npc.resolveTile(condition.of, target)
                target.coords.chebyshevDistance(of) <= condition.distance
            }
            is Condition.TargetInArc -> {
                val wanted = Angles.normalise(npc.vars[condition.bearingVarn] + condition.offset)
                target != null &&
                    abs(Angles.delta(Angles.bearing(npc.centreTile, target.coords), wanted)) <= condition.halfArc
            }
            is Condition.TilesEmpty -> {
                val name = condition.name
                val scope = checkNotNull(tiles) { "tilesEmpty(\"$name\") evaluated outside an effect." }
                scope.set(name).isEmpty()
            }
            is Condition.Custom -> condition.test(npc, target)
            is Condition.Not -> !eval(condition.c)
            is Condition.And -> eval(condition.a) && eval(condition.b)
            is Condition.Or -> eval(condition.a) || eval(condition.b)
            is Condition.PhaseTicksAtLeast ->
                phaseEnteredTick >= 0 && mapClock.cycle - phaseEnteredTick >= condition.ticks
            is Condition.HitStyle -> requireHit().type == condition.type.toEngine()
            is Condition.HitDemonbane -> requireHit().demonbane
            is Condition.HitDamageAtLeast -> requireHit().damage >= condition.damage
            is Condition.TargetPraying -> target != null && target.isProtectingFrom(condition.type)
        }
    }

    private val hpFraction: Double
        get() = npc.hitpoints.toDouble() / npc.baseHitpointsLvl.coerceAtLeast(1)

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

internal fun cacheNpcType(name: String): NpcServerType? = ServerCacheManager.getNpc(name.asRSCM(RSCMType.NPC))

@OptIn(InternalApi::class)
internal fun Npc.bossTransmog(type: NpcServerType, duration: Int) {
    transmog(type, duration)
    assignUid()
}
