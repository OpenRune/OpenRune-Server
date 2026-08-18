package org.rsmod.tools.wiki.dumping.dropfill

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * A wiki rarity ("1/181.1", "10/1,077") reduced to an exact integer fraction.
 *
 * Decimals are scaled by powers of ten and reduced by gcd; anything non-numeric returns null for
 * the caller to quarantine. The reduced fraction must reproduce the source value within 0.1% — a
 * defensive invariant that no realistic Dropsline input can trigger.
 */
data class RarityFraction(val numerator: Long, val denominator: Long) {
    val rate: Double
        get() = numerator.toDouble() / denominator.toDouble()

    val key: String
        get() = "$numerator/$denominator"

    companion object {
        private val PATTERN = Regex("""^(\d+(?:\.\d+)?)/(\d+(?:\.\d+)?)$""")

        fun parse(rarity: String): RarityFraction? {
            val match = PATTERN.matchEntire(rarity.trim().replace(",", "")) ?: return null
            val (numText, denText) = match.destructured
            val decimals =
                maxOf(
                    numText.substringAfter('.', "").length,
                    denText.substringAfter('.', "").length,
                )
            val scale = 10.0.pow(decimals)
            var num = Math.round(numText.toDouble() * scale)
            var den = Math.round(denText.toDouble() * scale)
            if (num <= 0 || den <= 0 || num > den) {
                return null
            }
            val g = gcd(num, den)
            num /= g
            den /= g
            val source = numText.toDouble() / denText.toDouble()
            if (abs(num.toDouble() / den.toDouble() - source) / source > 0.001) {
                return null
            }
            return RarityFraction(num, den)
        }

        private tailrec fun gcd(a: Long, b: Long): Long = if (b == 0L) a else gcd(b, a % b)
    }
}

/**
 * A minimal evaluator for the wiki's `1/{{#expr: … round N}}` drop-rate templates. Used ONLY to
 * compute an expected rate for disambiguating candidate Dropsline rows — emitted numbers always
 * come from Dropsline.
 *
 * Grammar: + - * / and parentheses over decimal literals, with `{{#var:NAME}}` placeholders
 * substituted before parsing, and a trailing `round N` rounding half-up to N decimal places.
 */
object WikiExpr {
    private val VAR_PATTERN = Regex("""\{\{#var:([a-z_]+)}}""")
    private val EXPR_PATTERN = Regex("""^1/\{\{#expr:(.+?)}}$""", RegexOption.DOT_MATCHES_ALL)
    private val ROUND_PATTERN = Regex("""\s+round\s+(\d+)\s*$""")

    /** Real anchors all use `round 1`; anything above this is treated as unparseable. */
    private const val MAX_ROUND_PLACES = 15

    fun variablesIn(template: String): List<String> =
        VAR_PATTERN.findAll(template).map { it.groupValues[1] }.distinct().toList()

    fun evaluate(template: String, vars: Map<String, Double> = emptyMap()): Double? {
        val match = EXPR_PATTERN.matchEntire(template) ?: return null
        var body = match.groupValues[1]
        var places: Int? = null
        val round = ROUND_PATTERN.find(body)
        if (round != null) {
            places = round.groupValues[1].toIntOrNull()
            if (places == null || places > MAX_ROUND_PLACES) {
                return null
            }
            body = body.substring(0, round.range.first)
        }
        for (name in variablesIn(template)) {
            val value = vars[name] ?: return null
            // Plain-decimal form: Double.toString uses scientific notation below
            // 1e-3, which the arithmetic grammar (digits and '.') cannot parse.
            body =
                body.replace("{{#var:$name}}", "(${BigDecimal(value.toString()).toPlainString()})")
        }
        if (body.contains("{{")) {
            return null // an unsubstituted construct we do not understand
        }
        val denominator = Arith(body.trim()).parse() ?: return null
        if (denominator <= 0) {
            return null
        }
        val rounded =
            if (places == null) denominator else roundHalfUp(denominator, places) ?: return null
        return if (rounded > 0) 1 / rounded else null
    }

    /** Round half-up on the decimal value, matching MediaWiki's `{{#expr: … round N}}`. */
    private fun roundHalfUp(value: Double, places: Int): Double? =
        runCatching {
                BigDecimal(value.toString())
                    .movePointRight(places)
                    .setScale(0, RoundingMode.HALF_UP)
                    .movePointLeft(places)
                    .toDouble()
            }
            .getOrNull()
            ?.takeIf { it.isFinite() }

    /** Recursive-descent over + - * / and parens. Returns null on any malformed input. */
    private class Arith(private val src: String) {
        private var pos = 0

        fun parse(): Double? {
            val value = expr()
            ws()
            return if (value != null && pos == src.length) value else null
        }

        private fun ws() {
            while (pos < src.length && src[pos].isWhitespace()) pos++
        }

        private fun expr(): Double? {
            var left = term() ?: return null
            while (true) {
                ws()
                val op = src.getOrNull(pos)
                if (op != '+' && op != '-') return left
                pos++
                val right = term() ?: return null
                left = if (op == '+') left + right else left - right
            }
        }

        private fun term(): Double? {
            var left = factor() ?: return null
            while (true) {
                ws()
                val op = src.getOrNull(pos)
                if (op != '*' && op != '/') return left
                pos++
                val right = factor() ?: return null
                if (op == '/' && right == 0.0) return null
                left = if (op == '*') left * right else left / right
            }
        }

        private fun factor(): Double? {
            ws()
            when (src.getOrNull(pos)) {
                '(' -> {
                    pos++
                    val value = expr() ?: return null
                    ws()
                    if (src.getOrNull(pos) != ')') return null
                    pos++
                    return value
                }
                '-' -> {
                    pos++
                    val value = factor() ?: return null
                    return -value
                }
            }
            val start = pos
            while (pos < src.length && (src[pos].isDigit() || src[pos] == '.')) pos++
            if (pos == start) return null
            val n = src.substring(start, pos).toDoubleOrNull() ?: return null
            return if (n.isFinite()) n else null
        }
    }
}

/**
 * Recovers a wiki template variable (herbbase, uncseed, …) from observed Dropsline rates for
 * anchors in the same file. Each template is monotonic in its unknown over the plausible range, so
 * a coarse log-scale scan plus a ternary refine converges. Used only for disambiguation.
 */
object VariableSolver {
    fun solve(observations: List<Observation>, name: String): Double? {
        val usable =
            observations.filter {
                val vars = WikiExpr.variablesIn(it.template)
                vars.size == 1 && vars[0] == name
            }
        if (usable.isEmpty()) {
            return null
        }
        fun error(candidate: Double): Double? {
            var sum = 0.0
            for (obs in usable) {
                val predicted = WikiExpr.evaluate(obs.template, mapOf(name to candidate))
                if (predicted == null || obs.rate <= 0) {
                    return null
                }
                sum += abs(ln(predicted / obs.rate))
            }
            return sum
        }

        // Candidate starting points: each observation inverted exactly (the
        // rounding steps inside the template make the error a staircase, so a
        // fixed-grid scan can settle on a neighbouring plateau), plus a coarse
        // log-scale grid as a fallback when inversion fails.
        val candidates = mutableListOf<Double>()
        for (obs in usable) {
            invert(obs.template, name, obs.rate)?.let { candidates += it }
        }
        var exp = -9.0
        while (exp <= 0.0) {
            candidates += 10.0.pow(exp)
            exp += 0.05
        }
        var best: Pair<Double, Double>? = null
        for (v in candidates) {
            val e = error(v)
            if (e != null && (best == null || e < best.second)) {
                best = v to e
            }
        }
        val anchor = best?.first ?: return null
        var lo = anchor / 1.2
        var hi = anchor * 1.2
        for (iteration in 0 until 200) {
            val m1 = lo + (hi - lo) / 3
            val m2 = hi - (hi - lo) / 3
            val e1 = error(m1) ?: break
            val e2 = error(m2) ?: break
            if (e1 < e2) hi = m2 else lo = m1
        }
        val solved = (lo + hi) / 2
        val err = error(solved)
        // reject a "solution" that does not actually reproduce the observations
        return if (err != null && err / usable.size < 0.02) solved else null
    }

    /**
     * Finds v with `evaluate(template, {name: v}) == rate` by log-space bisection — the template
     * denominator is monotonic in the variable over the plausible range. Lands inside the exact-fit
     * plateau where one exists; returns null when the rate is not bracketed.
     */
    private fun invert(template: String, name: String, rate: Double): Double? {
        fun predict(v: Double): Double? = WikiExpr.evaluate(template, mapOf(name to v))
        var lo = 1e-9
        var hi = 1.0
        var predLo = predict(lo)
        var predHi = predict(hi)
        var guard = 0
        while ((predLo == null || predHi == null) && guard++ < 20 && lo < hi) {
            if (predLo == null) {
                lo *= 3
                predLo = predict(lo)
            }
            if (predHi == null) {
                hi /= 3
                predHi = predict(hi)
            }
        }
        if (predLo == null || predHi == null || lo >= hi) return null
        if (predLo == rate) return lo
        if (predHi == rate) return hi
        if ((predLo < rate) == (predHi < rate)) return null
        val increasing = predHi > predLo
        repeat(200) {
            val mid = sqrt(lo * hi)
            val predMid = predict(mid) ?: return null
            when {
                predMid == rate -> return mid
                (predMid < rate) == increasing -> lo = mid
                else -> hi = mid
            }
        }
        return sqrt(lo * hi)
    }

    data class Observation(val template: String, val rate: Double)
}
