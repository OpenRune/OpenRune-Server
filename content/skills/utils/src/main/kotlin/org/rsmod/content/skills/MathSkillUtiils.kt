import kotlin.math.floor
import kotlin.random.Random
import org.rsmod.api.player.stat.resolveSkillRoll
import org.rsmod.game.entity.Player

private val STATIC_RANDOM = Random.Default // shared instance

fun Player.skillSuccess(
    stat: String,
    low: Int,
    high: Int,
    level: Int,
    subject: String? = null,
): Boolean {
    val rate = computeSkillingSuccess(low, high, level)
    return resolveSkillRoll(stat, rate, rate > STATIC_RANDOM.nextDouble(), subject)
}

/**
 * Computes the skilling success probability using the Old School RuneScape skilling success formula.
 *
 * Docs on this formula can be found here: https://oldschool.runescape.wiki/w/Skilling_success_rate
 *
 * @param low the "low" value representing the odds at level 1
 * @param high the "high" value representing the odds at level 99
 * @param level the player's skill level; values below 1 or above 99 are clamped
 * @return the success probability as a Double in the range 0.0..1.0
 */
fun computeSkillingSuccess(low: Int, high: Int, level: Int): Double {
    val lvl = level.coerceIn(1, 99)
    val lowScaled = low * (99 - lvl) / 98.0
    val highScaled = high * (lvl - 1) / 98.0

    return ((1.0 + floor(lowScaled + highScaled + 0.5)) / 256.0).coerceIn(0.0, 1.0)
}
