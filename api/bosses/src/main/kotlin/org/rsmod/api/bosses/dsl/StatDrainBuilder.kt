package org.rsmod.api.bosses.dsl

import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.Odds
import org.rsmod.api.bosses.spec.StatDrainEntry

@BossDsl
class StatDrainBuilder internal constructor() {
    private val lines = mutableListOf<Line>()
    private var defaultAmount: Int? = null
    private var defaultChance: Int = 1
    private var defaultOutOf: Int = 1

    fun amount(n: Int) {
        defaultAmount = n
    }

    fun odds(chance: Int, outOf: Int) {
        require(outOf > 0) { "outOf must be > 0" }
        require(chance in 0..outOf) { "chance must be in 0..$outOf" }
        defaultChance = chance
        defaultOutOf = outOf
    }

    fun odds(odds: Odds) {
        defaultChance = odds.chance
        defaultOutOf = odds.outOf
    }

    private data class Line(val stat: String, val amount: Int?, val chance: Int?, val outOf: Int?)

    /**
     * Named overrides, then `+stat("…")` to commit, e.g. `+stat("stat.attack", amount = 4, chance =
     * 1, outOf = 2)`.
     */
    fun stat(
        stat: String,
        amount: Int? = null,
        chance: Int? = null,
        outOf: Int? = null,
    ): PendingStatDrainStat = PendingStatDrainStat(this, stat, amount, chance, outOf)

    /** Per-stat overrides in a block, then `+stat("…") { … }` to commit. */
    fun stat(stat: String, configure: StatDrainPerStatScope.() -> Unit): PendingStatDrainStat {
        val scope = StatDrainPerStatScope().apply(configure)
        return PendingStatDrainStat(this, stat, scope.lineAmount, scope.lineChance, scope.lineOutOf)
    }

    @BossDsl
    class StatDrainPerStatScope internal constructor() {
        internal var lineAmount: Int? = null
        internal var lineChance: Int? = null
        internal var lineOutOf: Int? = null

        fun amount(n: Int) {
            lineAmount = n
        }

        fun odds(chance: Int, outOf: Int) {
            require(outOf > 0) { "outOf must be > 0" }
            require(chance in 0..outOf) { "chance must be in 0..$outOf" }
            lineChance = chance
            lineOutOf = outOf
        }

        fun odds(odds: Odds) {
            lineChance = odds.chance
            lineOutOf = odds.outOf
        }
    }

    @BossDsl
    class PendingStatDrainStat
    internal constructor(
        private val builder: StatDrainBuilder,
        private val stat: String,
        private val overrideAmount: Int?,
        private val overrideChance: Int?,
        private val overrideOutOf: Int?,
    ) {
        operator fun unaryPlus() {
            builder.lines += Line(stat, overrideAmount, overrideChance, overrideOutOf)
        }
    }

    operator fun String.unaryPlus() {
        lines += Line(this, null, null, null)
    }

    internal fun build(): Effect.StatDrain {
        require(lines.isNotEmpty()) {
            "statDrain { } must list at least one stat (e.g. +stat(\"stat.attack\") or +\"stat.attack\")."
        }
        val entries =
            lines.map { line ->
                StatDrainEntry(
                    stat = line.stat,
                    amount =
                        line.amount
                            ?: requireNotNull(defaultAmount) {
                                "statDrain { } requires amount(…) on the block or an amount on each stat line."
                            },
                    chance = line.chance ?: defaultChance,
                    outOf = line.outOf ?: defaultOutOf,
                )
            }
        return Effect.StatDrain(entries)
    }
}
