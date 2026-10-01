package org.rsmod.api.bosses.dsl

import dev.openrune.types.NpcMode
import org.rsmod.api.bosses.spec.*
import org.rsmod.api.bosses.validation.SpecValidator
import org.rsmod.api.player.output.CamShakeAxis
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

@DslMarker annotation class BossDsl

fun boss(vararg npcTypes: String, block: BossSpecBuilder.() -> Unit): BossSpec =
    BossSpecBuilder(npcTypes.toList()).apply(block).build()

@BossDsl
class BossSpecBuilder(private val npcTypes: List<String>) {
    init {
        require(npcTypes.isNotEmpty()) { "A boss spec must declare at least one npc type." }
    }

    private var stats = BossStats()
    private val abilities = mutableMapOf<String, Effect>()
    private val abilityAttackDelays = mutableMapOf<String, Int>()
    private val phases = mutableMapOf<String, PhaseSpec>()
    private val triggers = mutableListOf<TriggerSpec>()
    private val hitReactions = mutableListOf<HitReaction>()
    private val incomingRules = mutableListOf<IncomingRule>()
    private val timers = mutableListOf<TimerSpec>()

    fun stats(attackRate: Int = 4) {
        stats = BossStats(attackRate)
    }

    fun ability(name: String, block: AbilityBuilder.() -> Unit): AbilityRef {
        val builder = AbilityBuilder().apply(block)
        return ability(name, builder.build(), builder.attackDelay)
    }

    fun ability(name: String, effect: Effect, attackDelay: Int? = null): AbilityRef {
        abilities[name] = effect
        if (attackDelay != null) abilityAttackDelays[name] = attackDelay else abilityAttackDelays -= name
        return AbilityRef(name)
    }

    fun phase(
        name: String,
        entryHp: Double? = null,
        transmog: String? = null,
        lockMovement: Boolean = false,
        exitAfter: Int? = null,
        nextPhase: String? = null,
        idleAnim: String? = null,
        attackRate: Int? = null,
        block: PhaseBuilder.() -> Unit,
    ): PhaseRef {
        val builder = PhaseBuilder(name).apply(block)
        phases[name] =
            PhaseSpec(
                name = name,
                entryHp = entryHp,
                transmog = transmog,
                lockMovement = lockMovement,
                exitAfter = exitAfter,
                nextPhase = nextPhase,
                idleAnim = idleAnim,
                attackRate = attackRate ?: builder.attackRate,
                entry = builder.entry,
                selector = builder.selector,
                forceAbilities = builder.forceAbilities,
                timers = builder.timers,
            )
        return PhaseRef(name)
    }

    fun every(ticks: Int, effect: Effect, skipWhileBusy: Boolean = false) {
        timers += TimerSpec(ticks..ticks, effect, skipWhileBusy)
    }

    fun every(ticks: IntRange, effect: Effect, skipWhileBusy: Boolean = false) {
        timers += TimerSpec(ticks, effect, skipWhileBusy)
    }

    fun triggers(block: TriggerBuilder.() -> Unit) {
        TriggerBuilder(triggers).apply(block)
    }

    /** See [HitReaction]. */
    fun onIncomingHit(
        ability: AbilityRef,
        withObj: List<String> = emptyList(),
        requires: Condition = Condition.Always,
    ) {
        hitReactions += HitReaction(Effect.Run(ability.name), withObj, requires)
    }

    /** Ordered, first-match rules for player hits landing on the boss; see [IncomingRule]. */
    fun incoming(block: IncomingRulesBuilder.() -> Unit) {
        IncomingRulesBuilder(incomingRules).apply(block)
    }

    fun build(): BossSpec {
        val spec =
            BossSpec(
                npcTypes,
                stats,
                abilities,
                phases,
                triggers,
                hitReactions,
                incomingRules,
                abilityAttackDelays,
                timers,
            )
        val errors = SpecValidator.validate(spec)
        if (errors.isNotEmpty()) {
            throw IllegalStateException(
                "Boss spec invalid for '${npcTypes.joinToString()}':\n" +
                    errors.joinToString("\n") { " - ${it.message}" }
            )
        }
        return spec
    }
}

/**
 * Call-style hit DSL: `hit { damage(0..25).roll(); type(Melee) }` (also on [ProjectileBuilder]).
 */
@BossDsl
class HitBuilder internal constructor() {
    private var damageExpr: DamageExpr? = null
    private var hitType: HitType? = null
    var target: TargetExpr = TargetExpr.CurrentTarget
    var delay: Int = 0
    private var spotanimSpot: String? = null
    private var spotanimHeight: Int = 0
    private var spotanimDelay: Int? = null
    private var spotanimUnlessPraying: Boolean = false
    private var penetrationPercent: Int = 0
    private var penetrationWhen: Condition? = null
    private var hazardHit: Boolean = false
    private var missSpot: String? = null
    private var onHitEffect: Effect? = null
    private var onHitEvenOnMiss: Boolean = false
    private var lifestealPercent: Int = 0

    fun damage(expr: DamageExpr) {
        damageExpr = expr
    }

    /** Inclusive roll range; finish with [DamageRangeStep.roll] or [DamageRangeStep.random]. */
    fun damage(range: IntRange): DamageRangeStep = DamageRangeStep(this, range)

    fun type(t: HitType) {
        hitType = t
    }

    /**
     * Plays [spot] on the resolved target(s) when the hit lands, e.g. a magic impact graphic. By
     * default only on a non-zero hit; with [unlessPraying], whenever the target isn't praying
     * against this hit's type.
     */
    fun spotanim(spot: String, height: Int = 0, delay: Int? = null, unlessPraying: Boolean = false) {
        spotanimSpot = spot
        spotanimHeight = height
        spotanimDelay = delay
        spotanimUnlessPraying = unlessPraying
    }

    /** Percentage (0-100) of a protection prayer's block this hit ignores, optionally only [whenever]. */
    fun penetration(percent: Int, whenever: Condition? = null) {
        penetrationPercent = percent
        penetrationWhen = whenever
    }

    /** Environmental damage: no retaliation and no defend anim. */
    fun hazard() {
        hazardHit = true
    }

    fun missSpotanim(spot: String) {
        missSpot = spot
    }

    fun onHit(effect: Effect, evenOnMiss: Boolean = false) {
        onHitEffect = effect
        onHitEvenOnMiss = evenOnMiss
    }

    fun lifesteal(percent: Int) {
        lifestealPercent = percent
    }

    internal fun commitDamage(expr: DamageExpr) {
        damageExpr = expr
    }

    internal fun build(): Effect.Hit =
        Effect.Hit(
            target = target,
            damage =
                requireNotNull(damageExpr) {
                    "hit { } requires damage(…), e.g. damage(0..25).roll() or damage((0..25).roll())"
                },
            type = requireNotNull(hitType) { "hit { } requires type(…)" },
            delay = delay,
            spotanim = spotanimSpot,
            spotanimHeight = spotanimHeight,
            spotanimDelay = spotanimDelay,
            penetration = penetrationPercent,
            missSpotanim = missSpot,
            onHit = onHitEffect,
            onHitEvenOnMiss = onHitEvenOnMiss,
            lifesteal = lifestealPercent,
            spotanimUnlessPraying = spotanimUnlessPraying,
            penetrationWhen = penetrationWhen,
            hazard = hazardHit,
        )
}

@BossDsl
class DamageRangeStep
internal constructor(private val builder: HitBuilder, private val range: IntRange) {
    fun roll() {
        builder.commitDamage(DamageExpr.Roll(range))
    }

    /** Same as [roll] (random value in [range] when the hit resolves). */
    fun random() {
        builder.commitDamage(DamageExpr.Roll(range))
    }
}

@BossDsl
class AbilityBuilder {
    private val effects = mutableListOf<Effect>()

    /**
     * Ticks from this ability's start to the next attack, replacing the attack rate for that gap;
     * null keeps the attack rate. Only applies when the attack loop starts the ability, not when
     * another ability `run`s it.
     */
    var attackDelay: Int? = null

    fun anim(seq: String, delay: Int = 0) {
        effects += Effect.Anim(seq, delay)
    }

    fun idleAnim(seq: String) {
        effects += Effect.IdleAnim(seq)
    }

    fun clearIdleAnim() {
        effects += Effect.IdleAnim(null)
    }

    fun resetAnim() {
        effects += Effect.ResetAnim
    }

    fun forceNext(ability: AbilityRef) {
        effects += Effect.ForceNext(ability.name)
    }

    /** Plays [spot] on the caster (the boss npc itself), not on the target. */
    fun spotanim(spot: String, height: Int = 0, delay: Int = 0, slot: Int = 0) {
        effects += Effect.Spotanim(spot, height, delay, slot)
    }

    fun say(text: String) {
        effects += Effect.Say(text)
    }

    fun sound(synth: String, radius: Int = 10) {
        effects += Effect.Sound(synth, radius)
    }

    fun soundTo(synth: String, target: TargetExpr = TargetExpr.CurrentTarget, loops: Int = 1, delay: Int = 0) {
        effects += Effect.SoundTo(synth, target, loops, delay)
    }

    fun delay(ticks: Int) {
        effects += Effect.Delay(ticks)
    }

    fun wait(ticks: Int) {
        effects += Effect.Wait(ticks)
    }

    fun faceTarget() {
        effects += Effect.FaceTarget
    }

    fun faceTile(at: TargetExpr.Single) {
        effects += Effect.FaceTile(at)
    }

    fun broadcastInArea(text: String, radius: Int = 15) {
        effects += Effect.Broadcast(text, radius)
    }

    fun camShake(axis: CamShakeAxis, random: Int, amplitude: Int = 0, rate: Int = 0, radius: Int = 15) {
        effects += Effect.CamShake(axis, random, amplitude, rate, radius)
    }

    fun message(text: String, target: TargetExpr = TargetExpr.CurrentTarget) {
        effects += Effect.Message(text, target)
    }

    fun run(ability: String) {
        effects += Effect.Run(ability)
    }

    fun run(ability: AbilityRef) {
        effects += Effect.Run(ability.name)
    }

    fun setVarn(varn: String, value: Int) {
        effects += org.rsmod.api.bosses.dsl.setVarn(varn, value)
    }

    fun setVarn(varn: String, value: VarExpr) {
        effects += org.rsmod.api.bosses.dsl.setVarn(varn, value)
    }

    fun addVarn(varn: String, delta: Int, max: Int? = null) {
        effects += org.rsmod.api.bosses.dsl.addVarn(varn, delta, max)
    }

    fun interrupt() {
        effects += Effect.Interrupt
    }

    fun nextAttackIn(ticks: Int) {
        effects += Effect.NextAttackIn(ticks)
    }

    fun headbar(headbar: String, fromPercent: Int, toPercent: Int, cycles: Int) {
        effects += Effect.Headbar(headbar, fromPercent, toPercent, cycles)
    }

    fun clearHeadbar(headbar: String) {
        effects += Effect.ClearHeadbar(headbar)
    }

    fun headIcon(slot: Int, graphic: Int, index: Int) {
        effects += Effect.HeadIcon(slot, graphic, index)
    }

    fun clearHeadIcon(slot: Int) {
        effects += Effect.ClearHeadIcon(slot)
    }

    fun lockMovement() {
        effects += Effect.LockMovement(true)
    }

    fun unlockMovement() {
        effects += Effect.LockMovement(false)
    }

    fun healSelf(amount: Int) {
        effects += Effect.HealSelf(amount)
    }

    fun transitionTo(phase: String) {
        effects += Effect.TransitionTo(phase)
    }

    fun transitionTo(phase: PhaseRef) {
        effects += Effect.TransitionTo(phase.name)
    }

    fun poison(damage: Int, chance: Int = 1, outOf: Int = 1) {
        effects += Effect.Poison(damage, chance, outOf)
    }

    fun poison(damage: Int, odds: Odds) {
        effects += Effect.Poison(damage, odds.chance, odds.outOf)
    }

    fun freeze(ticks: Int, chance: Int = 1, outOf: Int = 1) {
        effects += Effect.Freeze(ticks, chance, outOf)
    }

    fun freeze(ticks: Int, odds: Odds) {
        effects += Effect.Freeze(ticks, odds.chance, odds.outOf)
    }

    fun disablePrayers(overheadsOnly: Boolean = false) {
        effects += Effect.DisablePrayers(overheadsOnly)
    }

    fun statDrain(block: StatDrainBuilder.() -> Unit) {
        effects += StatDrainBuilder().apply(block).build()
    }

    fun statDrain(vararg stats: String, amount: Int, chance: Int = 1, outOf: Int = 1) {
        effects += Effect.StatDrain(stats.map { StatDrainEntry(it, amount, chance, outOf) })
    }

    fun statDrain(stats: List<String>, amount: Int, chance: Int = 1, outOf: Int = 1) {
        effects += Effect.StatDrain(stats.map { StatDrainEntry(it, amount, chance, outOf) })
    }

    fun statDrain(vararg stats: String, amount: Int, odds: Odds) {
        effects +=
            Effect.StatDrain(stats.map { StatDrainEntry(it, amount, odds.chance, odds.outOf) })
    }

    fun statDrain(stats: List<String>, amount: Int, odds: Odds) {
        effects +=
            Effect.StatDrain(stats.map { StatDrainEntry(it, amount, odds.chance, odds.outOf) })
    }

    fun include(effect: Effect) {
        effects += effect
    }

    fun hit(
        damage: DamageExpr,
        type: HitType,
        target: TargetExpr = TargetExpr.CurrentTarget,
        delay: Int = 0,
    ) {
        effects += Effect.Hit(target, damage, type, delay)
    }

    fun hit(block: HitBuilder.() -> Unit) {
        effects += HitBuilder().apply(block).build()
    }

    fun projectile(
        spotanim: String,
        travel: String? = null,
        config: ProjectileConfig? = null,
        target: TargetExpr = TargetExpr.CurrentTarget,
        launch: String? = null,
        impact: String? = null,
        hit: Effect.Hit? = null,
        resolveOnImpact: Boolean = false,
        onImpact: Effect? = null,
        from: TargetExpr.Single? = null,
        impactRounding: ImpactRounding = ImpactRounding.Down,
    ) {
        effects +=
            Effect.Projectile(
                spotanim,
                travel,
                config,
                target,
                launch,
                impact,
                hit,
                resolveOnImpact,
                onImpact,
                from,
                impactRounding,
            )
    }

    /**
     * Block form of [projectile]; set [ProjectileBuilder.spotanim] and optional fields, then
     * [ProjectileBuilder.hit].
     */
    fun projectile(block: ProjectileBuilder.() -> Unit) {
        val built = ProjectileBuilder().apply(block).build()
        effects += built
    }

    @BossDsl
    class ProjectileBuilder internal constructor() {
        var spotanim: String? = null
        var travel: String? = null
        var config: ProjectileConfig? = null
        var target: TargetExpr = TargetExpr.CurrentTarget
        var launch: String? = null
        var impact: String? = null
        var resolveOnImpact: Boolean = false
        var onImpact: Effect? = null
        var from: TargetExpr.Single? = null
        var impactRounding: ImpactRounding = ImpactRounding.Down
        private var hitPayload: Effect.Hit? = null

        fun hit(
            damage: DamageExpr,
            type: HitType,
            hitTarget: TargetExpr = TargetExpr.CurrentTarget,
            delay: Int = 0,
        ) {
            hitPayload = Effect.Hit(hitTarget, damage, type, delay)
        }

        fun hit(block: HitBuilder.() -> Unit) {
            hitPayload = HitBuilder().apply(block).build()
        }

        internal fun build(): Effect.Projectile =
            Effect.Projectile(
                spotanim = requireNotNull(spotanim) { "projectile { } requires spotanim = \"…\"" },
                travel = travel,
                config = config,
                target = target,
                launch = launch,
                impact = impact,
                hit = hitPayload,
                resolveOnImpact = resolveOnImpact,
                onImpact = onImpact,
                from = from,
                impactRounding = impactRounding,
            )
    }

    fun tileAoE(
        tiles: (Npc, Player) -> Collection<CoordGrid>,
        telegraph: TelegraphSpec? = null,
        damage: DamageExpr,
        type: HitType,
    ) {
        effects += Effect.TileAoE(tiles, telegraph, damage, type)
    }

    fun summon(
        npc: String,
        count: Int = 1,
        radius: Int = 3,
        centeredOn: TargetExpr.Single = TargetExpr.Self,
        mode: NpcMode? = null,
        duration: Int = 100,
        onSummon: String? = null,
        onSummonParams: Any? = null,
        owned: Boolean = false,
    ) {
        effects +=
            Effect.Summon(
                npc = npc,
                count = count,
                radius = radius,
                centeredOn = centeredOn,
                mode = mode,
                duration = duration,
                onSummon = onSummon,
                onSummonParams = onSummonParams,
                owned = owned,
            )
    }

    /**
     * Telegraphed, dodgeable area attack (falling rocks/debris). See [Effect.Debris]. A [telegraph]
     * spotanim marks each tile, then after [windup] ticks players still standing on a marked tile take
     * [damage]. Tiles target every player within [targetRadius] of the boss plus random scatter within
     * [scatterRadius], totaling a value drawn from [count].
     */
    fun debris(
        telegraph: String,
        damage: DamageExpr,
        type: HitType = HitType.Typeless,
        impact: String? = null,
        windup: Int = 3,
        targetRadius: Int = 15,
        scatterRadius: Int = 5,
        count: IntRange = 1..1,
        center: TargetExpr.Single = TargetExpr.Self,
    ) {
        effects +=
            Effect.Debris(
                telegraph,
                damage,
                type,
                impact,
                windup,
                targetRadius,
                scatterRadius,
                count,
                center,
            )
    }

    fun build(): Effect = if (effects.size == 1) effects.first() else Effect.Sequence(effects)
}

@BossDsl
class PhaseBuilder(private val name: String) {
    var entry: String? = null

    var attackRate: Int? = null
    var selector: Selector = Selector.WeightedRandom()
    internal val forceAbilities = mutableListOf<ForcedAbility>()
    internal val timers = mutableListOf<TimerSpec>()

    fun every(ticks: Int, effect: Effect, skipWhileBusy: Boolean = false) {
        timers += TimerSpec(ticks..ticks, effect, skipWhileBusy)
    }

    fun every(ticks: IntRange, effect: Effect, skipWhileBusy: Boolean = false) {
        timers += TimerSpec(ticks, effect, skipWhileBusy)
    }

    fun forceEvery(period: Int, ability: String) {
        forceAbilities += ForcedAbility(period, ability)
    }

    fun forceEvery(period: Int, ability: AbilityRef) {
        forceAbilities += ForcedAbility(period, ability.name)
    }

    fun forceEveryAttacks(min: Int, max: Int, ability: String) {
        forceAbilities += ForcedAbility(period = 0, ability = ability, attackMin = min, attackMax = max)
    }

    fun forceEveryAttacks(min: Int, max: Int, ability: AbilityRef) {
        forceAbilities += ForcedAbility(period = 0, ability = ability.name, attackMin = min, attackMax = max)
    }

    fun forceWhen(condition: Condition, ability: String, once: Boolean = false) {
        forceAbilities += ForcedAbility(period = 0, ability = ability, condition = condition, once = once)
    }

    fun forceWhen(condition: Condition, ability: AbilityRef, once: Boolean = false) {
        forceWhen(condition, ability.name, once)
    }

    fun weightedSelectorRandom(block: WeightedRandomBuilder.() -> Unit) {
        selector = weightedRandom(block)
    }

    fun rotationSelector(block: RotationBuilder.() -> Unit) {
        selector = rotation(block)
    }
}

@BossDsl
class IncomingRulesBuilder internal constructor(private val rules: MutableList<IncomingRule>) {
    fun rule(condition: Condition, block: IncomingActionsBuilder.() -> Unit) {
        rules += IncomingRule(condition, IncomingActionsBuilder().apply(block).actions)
    }
}

@BossDsl
class IncomingActionsBuilder internal constructor() {
    internal val actions = mutableListOf<IncomingAction>()

    fun cap(max: Int, style: HitType? = null) {
        actions += IncomingAction.Cap(max, style)
    }

    fun scalePercent(percent: Int, style: HitType? = null) {
        actions += IncomingAction.ScalePercent(percent, style)
    }

    fun floorPercentOfMaxHit(percent: Int, style: HitType) {
        actions += IncomingAction.FloorPercentOfMaxHit(percent, style)
    }

    fun run(ability: AbilityRef) {
        actions += IncomingAction.Run(Effect.Run(ability.name))
    }
}

@BossDsl
class TriggerBuilder(private val triggers: MutableList<TriggerSpec>) {
    fun on(c: Condition): Condition = c

    infix fun Condition.runs(effect: Effect) {
        triggers += TriggerSpec(this, effect)
    }
}
