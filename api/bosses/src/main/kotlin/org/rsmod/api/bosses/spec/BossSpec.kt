package org.rsmod.api.bosses.spec

data class BossSpec(
    val npcTypes: List<String>,
    val stats: BossStats,
    val abilities: Map<String, Effect>,
    val phases: Map<String, PhaseSpec>,
    val triggers: List<TriggerSpec>,
    val hitReactions: List<HitReaction> = emptyList(),
    val incomingRules: List<IncomingRule> = emptyList(),
    val abilityAttackDelays: Map<String, Int> = emptyMap(),
    val timers: List<TimerSpec> = emptyList(),
)

data class BossStats(val attackRate: Int = 4)

data class PhaseSpec(
    val name: String,
    val entryHp: Double? = null,
    val transmog: String? = null,
    val lockMovement: Boolean = false,
    /**
     * Per-phase attack-rate default (ticks between ability uses). When set, it overrides
     * [BossStats.attackRate] while this phase is active. A per-encounter
     * [org.rsmod.api.bosses.runtime.BossEncounter.attackRateOverride] takes precedence over both.
     */
    val attackRate: Int? = null,
    val exitAfter: Int? = null,
    val nextPhase: String? = null,
    val idleAnim: String? = null,
    /**
     * Ability run when this phase is entered by an automatic transition ([entryHp] or another
     * phase's [exitAfter]). A scripted [Effect.TransitionTo] or
     * [org.rsmod.api.bosses.runtime.BossEncounter.transitionTo] does not run it; the script doing
     * the transition orchestrates whatever the new phase needs.
     */
    val entry: String? = null,
    val selector: Selector = Selector.WeightedRandom(),
    val forceAbilities: List<ForcedAbility> = emptyList(),
    val timers: List<TimerSpec> = emptyList(),
)

/**
 * Runs [effect] every [ticks] ticks (re-rolled after each fire) against the encounter's last
 * target, alongside whatever ability is running. It never counts as an attack and ignores the
 * attack rate and attack delays; to respect those, have [effect] `forceNext` an ability instead. A
 * phase timer counts from the phase's entry and stops when the phase is left; a spec timer counts
 * from the first combat tick and stops when the encounter is removed. Interrupts don't stop a
 * timer, but drop the rest of an [effect] still waiting.
 *
 * Waits inside [effect] hold attacks, as they do in abilities. With [skipWhileBusy], a fire that
 * falls while [org.rsmod.api.bosses.runtime.BossEncounter.busyUntil] holds (an effect is mid-wait)
 * is skipped, and the timer waits its next interval.
 */
data class TimerSpec(val ticks: IntRange, val effect: Effect, val skipWhileBusy: Boolean = false)

data class ForcedAbility(
    val period: Int,
    val ability: String,
    val attackMin: Int? = null,
    val attackMax: Int? = null,
    val condition: Condition? = null,
    val once: Boolean = false,
)

data class TriggerSpec(val condition: Condition, val effect: Effect)

data class TelegraphSpec(val spotanim: String, val windup: Int)

data class ProjectileConfig(
    val startHeight: Int = 43,
    val endHeight: Int = 31,
    val startDelay: Int = 51,
    val travelTime: Int = 56,
    val angle: Int = 10,
    val progress: Int = 15,
    val stepMultiplier: Int = 5,
) {
    companion object {
        /** Timing that ignores distance: waits [delay] cycles, then flies for [travel]. */
        fun fixed(
            startHeight: Int,
            endHeight: Int,
            delay: Int,
            travel: Int,
            angle: Int,
            progress: Int = 0,
        ) = ProjectileConfig(startHeight, endHeight, delay, travel, angle, progress, stepMultiplier = 0)
    }
}

/**
 * Which tick a projectile's hit, [Effect.Projectile.impact] and [Effect.Projectile.onImpact]
 * resolve on when its flight ends part-way through a tick. They only differ when the flight isn't
 * a whole number of ticks (30 cycles): a 54-cycle flight resolves on tick 2 with [Down] and tick 3
 * with [Up] (never sooner than tick 2).
 */
enum class ImpactRounding {
    /** The tick the projectile finishes in, as `ProjAnim.serverCycles` has always done. */
    Down,

    /** The first whole tick after it finishes. */
    Up,
}
