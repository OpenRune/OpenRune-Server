package org.rsmod.api.bosses.dsl

import dev.openrune.types.NpcMode
import org.rsmod.api.bosses.spec.*
import org.rsmod.api.combat.commons.types.MeleeAttackType as EngineMeleeAttackType
import org.rsmod.api.player.output.CamShakeAxis
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

fun anim(seq: String, delay: Int = 0): Effect = Effect.Anim(seq, delay)

fun idleAnim(seq: String): Effect = Effect.IdleAnim(seq)

fun clearIdleAnim(): Effect = Effect.IdleAnim(null)

fun resetAnim(): Effect = Effect.ResetAnim

fun forceNext(ability: AbilityRef): Effect = Effect.ForceNext(ability.name)

/** Plays [spot] on the caster (the boss npc itself), not on the target. */
fun spotanim(spot: String, height: Int = 0, delay: Int = 0): Effect =
    Effect.Spotanim(spot, height, delay)
fun say(text: String): Effect = Effect.Say(text)
fun sound(synth: String, radius: Int = 10, at: TargetExpr.Single? = null, delay: Int = 0): Effect =
    Effect.Sound(synth, radius, at, delay)

fun soundTo(synth: String, target: TargetExpr = TargetExpr.CurrentTarget, loops: Int = 1, delay: Int = 0): Effect =
    Effect.SoundTo(synth, target, loops, delay)
fun delay(ticks: Int): Effect = Effect.Delay(ticks)

fun wait(ticks: Int): Effect = Effect.Wait(ticks)

fun camShake(axis: CamShakeAxis, random: Int, amplitude: Int = 0, rate: Int = 0, radius: Int = 15): Effect =
    Effect.CamShake(axis, random, amplitude, rate, radius)

fun camShake(axis: CamShakeAxis, random: IntRange, target: TargetExpr, amplitude: Int = 0, rate: Int = 0): Effect =
    Effect.CamShake(axis, random.first, amplitude, rate, target = target, randomMax = random.last)

fun camReset(target: TargetExpr = TargetExpr.CurrentTarget): Effect = Effect.CamReset(target)

fun mapSpotanim(spot: String, at: TargetExpr.Single, height: Int = 0, delay: Int = 0): Effect =
    Effect.MapSpotanim(spot, at, height, delay)

fun message(text: String, target: TargetExpr = TargetExpr.CurrentTarget): Effect =
    Effect.Message(text, target)
fun sequence(vararg e: Effect): Effect = Effect.Sequence(e.toList())
fun parallel(vararg e: Effect): Effect = Effect.Parallel(e.toList())
fun repeat(times: Int, gap: Int = 0, effect: Effect): Effect = Effect.Repeat(times..times, effect, gap)

fun repeat(times: IntRange, gap: Int = 0, effect: Effect): Effect = Effect.Repeat(times, effect, gap)
fun whenever(condition: Condition, then: Effect, otherwise: Effect = Effect.NoOp): Effect =
    Effect.Whenever(condition, then, otherwise)
fun onEach(targets: TargetExpr, effect: Effect): Effect = Effect.OnEach(targets, effect)

fun choose(selector: Selector, branches: Map<String, Effect>): Effect = Effect.Choose(selector, branches)

/** Runs one of [options], picked uniformly at random. */
fun oneOf(vararg options: Effect): Effect = oneOf(options.toList())

/** Runs [effect] with a 1 in [oneIn] chance. */
fun chance(oneIn: Int, effect: Effect): Effect = oneOf(listOf(effect) + List(oneIn - 1) { Effect.NoOp })

fun oneOf(options: List<Effect>): Effect {
    val keys = options.indices.map(Int::toString)
    val selector = Selector.WeightedRandom(keys.map { WeightedRef(it) })
    return Effect.Choose(selector, keys.zip(options).toMap())
}
fun run(ability: String): Effect = Effect.Run(ability)

fun run(ability: AbilityRef): Effect = Effect.Run(ability.name)

fun transitionTo(phase: String): Effect = Effect.TransitionTo(phase)

fun transitionTo(phase: PhaseRef): Effect = Effect.TransitionTo(phase.name)
fun external(handler: String, params: Any? = null): Effect = Effect.External(handler, params)

fun hit(
    damage: DamageExpr,
    type: HitType,
    target: TargetExpr = TargetExpr.CurrentTarget,
    delay: Int = 0,
): Effect.Hit = Effect.Hit(target, damage, type, delay)

fun hit(block: HitBuilder.() -> Unit): Effect.Hit = HitBuilder().apply(block).build()

/** Inclusive rolled damage; use in `hit { damage((0..25).roll()); … }` or with [HitBuilder.damage]. */
fun IntRange.roll(): DamageExpr.Roll = DamageExpr.Roll(this)

/** Alias of [roll] for boss hit specs. */
fun IntRange.randomRoll(): DamageExpr.Roll = DamageExpr.Roll(this)

fun npcMaxHit(
    meleeAttackType: MeleeAttackType? = null,
    scale: Double = 1.0,
    minHit: Int = 0,
): DamageExpr.NpcMaxHit = DamageExpr.NpcMaxHit(meleeAttackType, scale, minHit)

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
): Effect =
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

fun tileAoE(
    tiles: (Npc, Player) -> Collection<CoordGrid>,
    telegraph: TelegraphSpec? = null,
    damage: DamageExpr,
    type: HitType,
): Effect = Effect.TileAoE(tiles, telegraph, damage, type)

fun debris(
    telegraph: String,
    damage: DamageExpr,
    type: HitType = HitType.Typeless,
    impact: String? = null,
    windup: Int = 3,
    targetRadius: Int = 15,
    scatterRadius: Int = 5,
    count: IntRange = 1..1,
    center: TargetExpr = TargetExpr.Self,
): Effect =
    Effect.Debris(telegraph, damage, type, impact, windup, targetRadius, scatterRadius, count, center)

fun summon(
    npc: String,
    count: Int = 1,
    radius: Int = 3,
    centeredOn: TargetExpr = TargetExpr.Self,
    mode: NpcMode? = null,
    duration: Int = 100,
    onSummon: String? = null,
    onSummonParams: Any? = null,
    owned: Boolean = false,
): Effect = Effect.Summon(npc, count, radius, centeredOn, mode, duration, onSummon, onSummonParams, owned)

fun varn(name: String): VarExpr = VarExpr.Varn(name)

operator fun VarExpr.plus(other: VarExpr): VarExpr = VarExpr.Plus(this, other)

operator fun VarExpr.plus(delta: Int): VarExpr = VarExpr.Plus(this, VarExpr.Const(delta))

infix fun VarExpr.atMost(cap: VarExpr): VarExpr = VarExpr.Min(this, cap)

infix fun VarExpr.atMost(cap: Int): VarExpr = VarExpr.Min(this, VarExpr.Const(cap))

infix fun VarExpr.atLeast(floor: VarExpr): VarExpr = VarExpr.Max(this, floor)

infix fun VarExpr.atLeast(floor: Int): VarExpr = VarExpr.Max(this, VarExpr.Const(floor))

fun setVarn(varn: String, value: Int): Effect = Effect.SetVarn(varn, VarExpr.Const(value))

fun setVarn(varn: String, value: VarExpr): Effect = Effect.SetVarn(varn, value)

fun addVarn(varn: String, delta: Int, max: Int? = null): Effect {
    val sum = VarExpr.Varn(varn) + delta
    return Effect.SetVarn(varn, if (max != null) sum atMost max else sum)
}

fun switch(varn: String, vararg cases: Pair<Int, Effect>, otherwise: Effect = Effect.NoOp): Effect =
    Effect.Switch(varn, cases.toMap(), otherwise)

/** Runs `cases[value]` for [varn]'s current value; handy for ladders built from a list. */
fun switch(varn: String, cases: List<Effect>, otherwise: Effect = Effect.NoOp): Effect =
    Effect.Switch(varn, cases.withIndex().associate { (i, e) -> i to e }, otherwise)

fun interrupt(): Effect = Effect.Interrupt

fun area(sw: TargetExpr.Single, ne: TargetExpr.Single): Area = Area(sw, ne)

fun randomFreeTiles(area: Area, count: IntRange): TileSet = TileSet.RandomFree(area, count)

fun tilesUnderPlayers(area: Area): TileSet = TileSet.UnderPlayers(area)

fun nearestFreeTiles(tiles: List<TargetExpr.Single>, area: Area, searchRadius: Int): TileSet =
    TileSet.Nearest(tiles, area, searchRadius)

fun onTiles(tiles: TileSet, effect: Effect): Effect = Effect.OnTiles(tiles, effect)

fun after(ticks: Int, effect: Effect, requireAlive: Boolean = true): Effect =
    Effect.After(ticks, effect, requireAlive)

fun spawnLoc(loc: String, at: TargetExpr.Single, angle: Int = 0, blockPlayersOnly: Boolean = false): Effect =
    Effect.SpawnLoc(loc, at, angle, blockPlayersOnly)

fun knockback(anim: String, within: Area): Effect = Effect.Knockback(anim, within)

fun playersIn(area: Area): TargetExpr.Multi = TargetExpr.PlayersIn(area)

fun playersOn(tile: TargetExpr.Single): TargetExpr.Multi = TargetExpr.PlayersOn(tile)

fun varnIs(varn: String, value: Int): Condition = Condition.VarnIn(varn, value..value)

fun varnAtLeast(varn: String, value: Int): Condition = Condition.VarnIn(varn, value..Int.MAX_VALUE)

fun bearingTo(to: TargetExpr.Single, from: TargetExpr.Single = TargetExpr.Centre): VarExpr =
    VarExpr.BearingTo(to, from)

fun toward(from: TargetExpr.Single, to: TargetExpr.Single, distance: Double): TargetExpr.Single =
    TargetExpr.Toward(from, to, distance)

fun targetWithin(distance: Int, of: TargetExpr.Single = TargetExpr.Centre): Condition =
    Condition.TargetWithin(distance, of)

fun targetInArc(bearingVarn: String, offset: Int = 0, halfArc: Int): Condition =
    Condition.TargetInArc(bearingVarn, offset, halfArc)

fun lastAbility(ability: String): Condition = Condition.LastAbility(ability)

fun lastAbility(ability: AbilityRef): Condition = Condition.LastAbility(ability.name)

operator fun Condition.not(): Condition = Condition.Not(this)

fun transmog(to: String, durationTicks: Int): Effect = Effect.Transmog(to, durationTicks)
fun poison(damage: Int, chance: Int = 1, outOf: Int = 1): Effect = Effect.Poison(damage, chance, outOf)

fun poison(damage: Int, odds: Odds): Effect = Effect.Poison(damage, odds.chance, odds.outOf)

fun freeze(ticks: Int, chance: Int = 1, outOf: Int = 1): Effect = Effect.Freeze(ticks, chance, outOf)

fun freeze(ticks: Int, odds: Odds): Effect = Effect.Freeze(ticks, odds.chance, odds.outOf)

fun disablePrayers(): Effect = Effect.DisablePrayers
fun statDrain(block: StatDrainBuilder.() -> Unit): Effect = StatDrainBuilder().apply(block).build()

fun statDrain(vararg stats: String, amount: Int, chance: Int = 1, outOf: Int = 1): Effect =
    Effect.StatDrain(stats.map { StatDrainEntry(it, amount, chance, outOf) })

fun statDrain(stats: List<String>, amount: Int, chance: Int = 1, outOf: Int = 1): Effect =
    Effect.StatDrain(stats.map { StatDrainEntry(it, amount, chance, outOf) })

fun statDrain(stats: List<String>, amount: Int, odds: Odds): Effect =
    Effect.StatDrain(stats.map { StatDrainEntry(it, amount, odds.chance, odds.outOf) })

fun statDrainPercent(vararg stats: String, percent: Int): Effect =
    Effect.StatDrain(stats.map { StatDrainEntry(it, amount = 0, percent = percent) })

fun statDrain(vararg stats: String, amount: Int, odds: Odds): Effect =
    Effect.StatDrain(stats.map { StatDrainEntry(it, amount, odds.chance, odds.outOf) })

fun telegraph(spotanim: String, windup: Int): TelegraphSpec = TelegraphSpec(spotanim, windup)

fun spawnTile(dx: Int = 0, dz: Int = 0): TargetExpr.Single = TargetExpr.SpawnTile(dx, dz)

fun teleport(to: TargetExpr.Single): Effect = Effect.Teleport(to)

fun faceTarget(): Effect = Effect.FaceTarget

fun faceTile(at: TargetExpr.Single): Effect = Effect.FaceTile(at)

fun randomWalkableTile(radius: Int, of: TargetExpr.Single = TargetExpr.Self): TargetExpr =
    TargetExpr.RandomWalkableTile(radius, of)

fun weightedRandom(
    noRepeatBias: Double = 0.5,
    block: WeightedRandomBuilder.() -> Unit,
): Selector.WeightedRandom =
    WeightedRandomBuilder().apply {
        this.noRepeatBias = noRepeatBias
        block()
    }.build()

@BossDsl
class WeightedRandomBuilder internal constructor() {
    var noRepeatBias: Double = 0.5
    private val entries = mutableListOf<WeightedRef>()

    @BossDsl
    inner class RandomPending(
        private val abilityName: String,
        private val weight: Int,
        private val requires: Condition,
        private val cooldown: Int,
    ) {
        operator fun unaryPlus() {
            this@WeightedRandomBuilder.entries +=
                WeightedRef(ability = abilityName, weight = weight, cooldown = cooldown, requires = requires)
        }
    }

    fun random(
        ability: AbilityRef,
        weight: Int,
        requires: Condition = Condition.Always,
        cooldown: Int = 0,
    ): RandomPending = RandomPending(ability.name, weight, requires, cooldown)

    fun random(
        ability: String,
        weight: Int,
        requires: Condition = Condition.Always,
        cooldown: Int = 0,
    ): RandomPending = RandomPending(ability, weight, requires, cooldown)

    internal fun build(): Selector.WeightedRandom = Selector.WeightedRandom(entries, noRepeatBias)
}

fun rotation(block: RotationBuilder.() -> Unit): Selector.Rotation = RotationBuilder().apply(block).build()

@BossDsl
class RotationBuilder internal constructor() {
    private val sequence = mutableListOf<String>()

    @BossDsl
    inner class ThenPending(private val abilityName: String) {
        operator fun unaryPlus() {
            this@RotationBuilder.sequence += abilityName
        }
    }

    fun then(ability: AbilityRef): ThenPending = ThenPending(ability.name)

    fun then(ability: String): ThenPending = ThenPending(ability)

    internal fun build(): Selector.Rotation = Selector.Rotation(sequence)
}

// Re-exports for clean spec authoring
typealias Roll = DamageExpr.Roll
typealias Accuracy = DamageExpr.Accuracy
typealias Fixed = DamageExpr.Fixed
typealias NpcMaxHit = DamageExpr.NpcMaxHit
typealias HpBelow = Condition.HpBelow
typealias IncomingHitDamageAtLeast = Condition.IncomingHitDamageAtLeast
typealias PlayerEnterRange = Condition.PlayerEnterRange
typealias EveryNTicks = Condition.EveryNTicks
typealias TargetPraying = Condition.TargetPraying
typealias InPhase = Condition.InPhase
typealias WeightedRandom = Selector.WeightedRandom
typealias AbilityRef = org.rsmod.api.bosses.spec.AbilityRef
typealias PhaseRef = org.rsmod.api.bosses.spec.PhaseRef
typealias Rotation = Selector.Rotation
typealias Run = Effect.Run
typealias TransitionTo = Effect.TransitionTo
typealias AllInRadius = TargetExpr.AllInRadius
typealias FacingQuadrant = TargetExpr.FacingQuadrant
typealias MeleeAttackType = EngineMeleeAttackType

val OnDeath: Condition = Condition.OnDeath
val OnSpawn: Condition = Condition.OnSpawn
val Always: Condition = Condition.Always
val WithinMeleeRange: Condition = Condition.WithinMeleeRange
val CurrentTarget: TargetExpr.Single = TargetExpr.CurrentTarget
val CurrentTargetTile: TargetExpr.Single = TargetExpr.CurrentTargetTile
val Self: TargetExpr.Single = TargetExpr.Self
val Centre: TargetExpr.Single = TargetExpr.Centre
val ImpactTile: TargetExpr.Single = TargetExpr.ImpactTile
val EachTile: TargetExpr.Single = TargetExpr.EachTile
val Melee: HitType = HitType.Melee
val Ranged: HitType = HitType.Ranged
val Magic: HitType = HitType.Magic
val Dragonfire: HitType = HitType.Dragonfire
val DragonfireMetal: HitType = HitType.DragonfireMetal
val WyvernIce: HitType = HitType.WyvernIce
val Typeless: HitType = HitType.Typeless
