package org.rsmod.api.bosses.spec

import dev.openrune.types.NpcMode
import org.rsmod.api.player.output.CamShakeAxis
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

public data class StatDrainEntry(
    val stat: String,
    val amount: Int,
    val chance: Int = 1,
    val outOf: Int = 1,
    val percent: Int = 0,
)

sealed interface Effect {

    data class Anim(val seq: String, val delay: Int = 0) : Effect

    /** Sets (or with a null [seq], clears) the anim the caster holds while idle. */
    data class IdleAnim(val seq: String?) : Effect

    data object ResetAnim : Effect

    /** Queues [ability] as the boss's next attack, once nothing else is holding it up. */
    data class ForceNext(val ability: String) : Effect

    /**
     * The next attack may start [ticks] from now, replacing the running ability's attack delay or
     * the attack rate. [BossEncounter.busyUntil][org.rsmod.api.bosses.runtime.BossEncounter.busyUntil]
     * still holds it back on top.
     */
    data class NextAttackIn(val ticks: Int) : Effect
    data class Say(val text: String) : Effect

    /** Plays [synth] to everyone within [radius] of [at] (the caster when null). */
    data class Sound(
        val synth: String,
        val radius: Int = 10,
        val at: TargetExpr.Single? = null,
        val delay: Int = 0,
    ) : Effect

    /** Plays [synth] to [target] only, rather than to the area around the caster like [Sound]. */
    data class SoundTo(
        val synth: String,
        val target: TargetExpr = TargetExpr.CurrentTarget,
        val loops: Int = 1,
        val delay: Int = 0,
    ) : Effect
    data class Spotanim(val spot: String, val height: Int = 0, val delay: Int = 0, val slot: Int = 0) : Effect
    data class MapSpotanim(val spot: String, val at: TargetExpr.Single, val height: Int = 0, val delay: Int = 0) : Effect
    data class Broadcast(val text: String, val radius: Int = 15) : Effect

    /**
     * Shakes the camera of [target] (everyone within [radius] of the caster when null). With
     * [randomMax], each player's shake strength is rolled from `random..randomMax`.
     */
    data class CamShake(
        val axis: CamShakeAxis,
        val random: Int,
        val amplitude: Int = 0,
        val rate: Int = 0,
        val radius: Int = 15,
        val target: TargetExpr? = null,
        val randomMax: Int? = null,
    ) : Effect

    data class CamReset(val target: TargetExpr = TargetExpr.CurrentTarget) : Effect

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
        val spotanimDelay: Int? = null,
        /** Percentage (0-100) of a protection prayer's block this hit ignores. */
        val penetration: Int = 0,
        val missSpotanim: String? = null,
        val onHit: Effect? = null,
        val onHitEvenOnMiss: Boolean = false,
        val lifesteal: Int = 0,
        /**
         * Show [spotanim] only when the target isn't praying against [type], even on a 0. Needs a
         * projectile with [Projectile.resolveOnImpact].
         */
        val spotanimUnlessPraying: Boolean = false,
        /**
         * When set, [penetration] only applies if this holds for the target on impact. Needs a
         * projectile with [Projectile.resolveOnImpact].
         */
        val penetrationWhen: Condition? = null,
        /** Environmental damage (falling rocks etc.): no retaliation and no defend anim. */
        val hazard: Boolean = false,
    ) : Effect

    data class Projectile(
        val spotanim: String,
        val travel: String? = null,
        val config: ProjectileConfig? = null,
        val target: TargetExpr = TargetExpr.CurrentTarget,
        val launch: String? = null,
        val impact: String? = null,
        val hit: Hit? = null,
        /**
         * Resolves [hit] when the projectile lands instead of at launch: retaliation, the defend
         * anim, prayer and [Hit.penetrationWhen] are all decided on the impact tick, and
         * [Hit.spotanimUnlessPraying] becomes available.
         */
        val resolveOnImpact: Boolean = false,
        /**
         * Runs once the projectile lands, in addition to [impact]/[hit]. [TargetExpr.ImpactTile]
         * resolves to the tile it landed on for the duration of this effect — e.g.
         * `onImpact = summon("npc.ice_block", centeredOn = ImpactTile)`.
         */
        val onImpact: Effect? = null,
        /** Launch tile, without an entity anchor; the caster itself when null. */
        val from: TargetExpr.Single? = null,
        val impactRounding: ImpactRounding = ImpactRounding.Down,
    ) : Effect

    data class TileAoE(
        val tiles: (Npc, Player) -> Collection<CoordGrid>,
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
        val center: TargetExpr.Single = TargetExpr.Self,
    ) : Effect

    data class Summon(
        val npc: String,
        val count: Int = 1,
        val radius: Int = 3,
        val centeredOn: TargetExpr.Single = TargetExpr.Self,
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
        /** Owned by the encounter: removed when the boss is deleted or respawns. */
        val owned: Boolean = false,
    ) : Effect

    data class Transmog(val to: String, val durationTicks: Int) : Effect

    /**
     * Shows [headbar] over the caster, filling from [fromPercent] to [toPercent] of the bar over
     * [cycles] client cycles (30 per tick, at most 1275).
     */
    data class Headbar(
        val headbar: String,
        val fromPercent: Int,
        val toPercent: Int,
        val cycles: Int,
    ) : Effect

    data class ClearHeadbar(val headbar: String) : Effect

    /** Shows sprite [index] of sprite group [graphic] in head icon [slot] (0..7) over the caster. */
    data class HeadIcon(val slot: Int, val graphic: Int, val index: Int) : Effect

    data class ClearHeadIcon(val slot: Int) : Effect

    /**
     * Locks the caster in place ([locked]) or hands movement back to the current phase's
     * [PhaseSpec.lockMovement]. Any phase transition also resets it to the new phase's setting.
     */
    data class LockMovement(val locked: Boolean) : Effect

    /** Heals the caster by [amount], capped at its max hp, with a heal hitsplat. */
    data class HealSelf(val amount: Int) : Effect

    data class Teleport(val to: TargetExpr.Single) : Effect
    data object FaceTarget : Effect
    data class FaceTile(val at: TargetExpr.Single) : Effect

    data class Poison(val damage: Int, val chance: Int = 1, val outOf: Int = 1) : Effect
    data class Freeze(val ticks: Int, val chance: Int = 1, val outOf: Int = 1) : Effect
    data class DisablePrayers(val overheadsOnly: Boolean = false) : Effect
    data class StatDrain(val entries: List<StatDrainEntry>) : Effect {
        init {
            require(entries.isNotEmpty()) { "StatDrain requires at least one entry." }
        }
    }

    data class Run(val ability: String) : Effect

    /** Scripted phase switch; does not run the phase's [PhaseSpec.entry]. */
    data class TransitionTo(val phase: String) : Effect

    /** Runs a Kotlin handler; [at] is resolved here and passed on as the handler's tile. */
    data class External(
        val handler: String,
        val params: Any? = null,
        val at: TargetExpr.Single? = null,
    ) : Effect

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

    data class SetVarn(val varn: String, val value: VarExpr) : Effect

    /** Runs the case matching [varn]'s current value, or [otherwise] if none does. */
    data class Switch(val varn: String, val cases: Map<Int, Effect>, val otherwise: Effect = NoOp) : Effect

    /**
     * Cancels every other ability still running on this boss: their pending [Wait]s and [Repeat]
     * gaps never resume, and the attack lockout they set is cleared. Projectiles and hits already
     * in flight still land. The ability running this effect carries on.
     */
    data object Interrupt : Effect

    /** Runs [effect] once for every tile in [tiles], with [TargetExpr.CurrentTile] bound to it. */
    data class OnTiles(val tiles: TileSet, val effect: Effect) : Effect

    /** Resolves [tile] once and binds it as [TargetExpr.Bound] [name] for everything in [effect]. */
    data class WithTile(val name: String, val tile: TargetExpr.Single, val effect: Effect) : Effect

    /** Resolves [tiles] once and binds them as [TileSet.Bound] [name] for everything in [effect]. */
    data class WithTiles(val name: String, val tiles: TileSet, val effect: Effect) : Effect

    /**
     * Schedules [effect] [ticks] from now without holding up the ability or the boss's next
     * attack, unlike [Wait]. Not cancelled by [Interrupt]; dropped if the boss has died unless
     * [requireAlive] is off (e.g. clean-up such as resetting a shaken camera).
     */
    data class After(val ticks: Int, val effect: Effect, val requireAlive: Boolean = true) : Effect

    /**
     * Spawns [loc] at [at], owned by the encounter: the tile stops counting as free for
     * [TileSet]s and the loc is removed when the encounter ends. Skipped if the encounter already
     * owns a loc there. [blockPlayersOnly] swaps the loc's collision for a player-only block.
     */
    data class SpawnLoc(
        val loc: String,
        val at: TargetExpr.Single,
        val angle: Int = 0,
        val blockPlayersOnly: Boolean = false,
    ) : Effect

    /** Shoves the target one tile to a random free neighbour inside [within], playing [anim]. */
    data class Knockback(val anim: String, val within: Area) : Effect
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
