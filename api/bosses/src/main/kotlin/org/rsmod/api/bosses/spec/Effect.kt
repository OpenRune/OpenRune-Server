package org.rsmod.api.bosses.spec

import dev.openrune.types.NpcMode

public data class StatDrainEntry(
    val stat: String,
    val amount: Int,
    val chance: Int = 1,
    val outOf: Int = 1,
)

sealed interface Effect {

    data class Anim(val seq: String, val delay: Int = 0) : Effect
    data class Say(val text: String) : Effect
    data class Sound(val synth: String, val radius: Int = 10) : Effect
    data class Spotanim(val spot: String, val height: Int = 0, val delay: Int = 0) : Effect
    data class MapSpotanim(val spot: String, val at: TargetExpr, val height: Int = 0, val delay: Int = 0) : Effect
    data class Broadcast(val text: String, val radius: Int = 15) : Effect

    data class CamShake(
        val axis: Int,
        val random: Int,
        val amplitude: Int = 0,
        val rate: Int = 0,
        val radius: Int = 15,
    ) : Effect

    data class Message(val text: String, val target: TargetExpr = TargetExpr.CurrentTarget) : Effect

    data class Delay(val ticks: Int) : Effect

    data class Wait(val ticks: Int) : Effect
    data object NoOp : Effect

    data class Hit(
        val target: TargetExpr = TargetExpr.CurrentTarget,
        val damage: DamageExpr,
        val type: HitType,
        val delay: Int = 0,
        val spotanim: String? = null,
        val spotanimHeight: Int = 0,
        /** Percentage (0-100) of a protection prayer's block this hit ignores. */
        val penetration: Int = 0,
    ) : Effect

    data class Projectile(
        val spotanim: String,
        val travel: String? = null,
        val config: ProjectileConfig? = null,
        val target: TargetExpr = TargetExpr.CurrentTarget,
        val launch: String? = null,
        val impact: String? = null,
        val hit: Hit? = null,
        val resolveOnImpact: Boolean = false,
        /**
         * Runs once the projectile lands, in addition to [impact]/[hit]. [TargetExpr.ImpactTile]
         * resolves to the tile it landed on for the duration of this effect — e.g.
         * `onImpact = summon("npc.ice_block", centeredOn = ImpactTile)`.
         */
        val onImpact: Effect? = null,
    ) : Effect

    data class TileAoE(
        val center: TargetExpr,
        val radius: Int,
        val telegraph: TelegraphSpec? = null,
        val damage: DamageExpr,
        val type: HitType,
    ) : Effect

    /**
     * A telegraphed, dodgeable area attack (e.g. Scurrius' falling rocks): a [telegraph] spotanim is
     * shown on a set of tiles, then after [windup] ticks any player standing on one of those tiles
     * takes [damage]. Tiles are every player within [targetRadius] of the boss (so each is targeted)
     * plus random scatter within [scatterRadius] of the boss, up to a total drawn from [count].
     */
    data class Debris(
        val telegraph: String,
        val damage: DamageExpr,
        val type: HitType = HitType.Typeless,
        val impact: String? = null,
        val windup: Int = 3,
        val targetRadius: Int = 15,
        val scatterRadius: Int = 5,
        val count: IntRange = 1..1,
        val center: TargetExpr = TargetExpr.Self,
    ) : Effect

    data class Summon(
        val npc: String,
        val count: Int = 1,
        val radius: Int = 3,
        val centeredOn: TargetExpr = TargetExpr.Self,
        val mode: NpcMode? = null,
        /** Ticks until the spawned npc auto-despawns if still idle; `Int.MAX_VALUE` for permanent. */
        val duration: Int = 100,
        /**
         * A [BossExtensionHandler][org.rsmod.api.bosses.runtime.BossExtensionHandler] name invoked
         * once per spawned npc, right after it's added to the world — the handler's `npc` parameter
         * is the newly spawned npc, not the caster. Use it for anything a plain spawn can't express:
         * tracking ownership, scheduling a per-instance timeout, etc.
         */
        val onSummon: String? = null,
        val onSummonParams: Any? = null,
    ) : Effect

    data class Transmog(val to: String, val durationTicks: Int) : Effect

    data class Teleport(val to: TargetExpr.Single) : Effect
    data object FaceTarget : Effect
    data class FaceTile(val at: TargetExpr.Single) : Effect

    data class Poison(val damage: Int, val chance: Int = 1, val outOf: Int = 1) : Effect
    data class Freeze(val ticks: Int, val chance: Int = 1, val outOf: Int = 1) : Effect
    data object DisablePrayers : Effect
    data class StatDrain(val entries: List<StatDrainEntry>) : Effect {
        init {
            require(entries.isNotEmpty()) { "StatDrain requires at least one entry." }
        }
    }

    data class Run(val ability: String) : Effect
    data class TransitionTo(val phase: String) : Effect
    data class External(val handler: String, val params: Any? = null) : Effect

    /**
     * A timeline of [effects] run in order, tick by tick. Include [Wait] to advance to a later
     * tick before continuing, e.g. `sequence(anim("seq.foo"), wait(2), parallel(spotanim("spot.bar"),
     * projectile(...)))` plays a seq, waits 2 ticks, then fires a spotanim and projectile together.
     */
    data class Sequence(val effects: List<Effect>) : Effect

    /** A bag of [effects] that all fire on the same tick, in no particular declared order. */
    data class Parallel(val effects: List<Effect>) : Effect
    data class Choose(val selector: Selector, val branches: Map<String, Effect>) : Effect

    /** Runs [effect] a number of times rolled once from [times] (a fixed count is `n..n`). */
    data class Repeat(val times: IntRange, val effect: Effect, val gap: Int = 0) : Effect
    data class Whenever(val condition: Condition, val then: Effect, val otherwise: Effect = NoOp) : Effect
    data class OnEach(val targets: TargetExpr, val effect: Effect) : Effect
}

enum class HitType {
    Melee,
    Ranged,
    Magic,
    Dragonfire,
    DragonfireMetal,
    WyvernIce,
    Typeless,
}
