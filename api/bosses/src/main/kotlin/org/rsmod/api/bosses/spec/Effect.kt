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
    data class Delay(val ticks: Int) : Effect
    data object NoOp : Effect

    data class Hit(
        val target: TargetExpr = TargetExpr.CurrentTarget,
        val damage: DamageExpr,
        val type: HitType,
        val delay: Int = 0,
        val spotanim: String? = null,
        val spotanimHeight: Int = 0,
    ) : Effect

    data class Projectile(
        val spotanim: String,
        val travel: String? = null,
        val config: ProjectileConfig? = null,
        val target: TargetExpr = TargetExpr.CurrentTarget,
        val launch: String? = null,
        val impact: String? = null,
        val hit: Hit? = null,
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
    ) : Effect

    data class Transmog(val to: String, val durationTicks: Int) : Effect

    data class Poison(val damage: Int, val chance: Int = 1, val outOf: Int = 1) : Effect
    data class Freeze(val ticks: Int, val chance: Int = 1, val outOf: Int = 1) : Effect
    data class StatDrain(val entries: List<StatDrainEntry>) : Effect {
        init {
            require(entries.isNotEmpty()) { "StatDrain requires at least one entry." }
        }
    }

    data class Run(val ability: String) : Effect
    data class TransitionTo(val phase: String) : Effect
    data class External(val handler: String, val params: Any? = null) : Effect

    data class Sequence(val effects: List<Effect>) : Effect
    data class Parallel(val effects: List<Effect>) : Effect
    data class Choose(val selector: Selector, val branches: Map<String, Effect>) : Effect
    data class Repeat(val times: Int, val effect: Effect, val gap: Int = 0) : Effect
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
