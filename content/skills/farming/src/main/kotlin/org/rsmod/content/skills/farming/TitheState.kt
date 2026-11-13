package org.rsmod.content.skills.farming

internal data class TithePlant(
    val tier: Int,
    val stage: Int = 1,
    val watered: Boolean = false,
    val dead: Boolean = false,
    val deadline: Int,
) {
    fun advance(tick: Int, stageTicks: Int): TithePlant {
        var next = this
        while (!next.dead && next.stage < 4 && tick >= next.deadline) {
            next = if (!next.watered) next.copy(dead = true)
            else next.copy(stage = next.stage + 1, watered = false, deadline = next.deadline + stageTicks)
        }
        return next
    }
}

internal data class TitheDeposit(
    val count: Int,
    val score: Int,
    val points: Int,
    val doubled: Int,
    val bonusXp: Int,
)

internal fun titheDeposit(
    score: Int,
    available: Int,
    harvestXp: Int,
    sackSize: Int,
    bonusThreshold: Int,
): TitheDeposit {
    require(score in 0 until sackSize && available >= 0)
    val count = minOf(available, sackSize - score)
    val end = score + count
    val doubled = (end - maxOf(score, bonusThreshold - 1)).coerceAtLeast(0)
    val points = end / 3 - score / 3 + if (end == sackSize) 2 else 0
    val bonus = if (score < bonusThreshold && end >= bonusThreshold) harvestXp * 250 else 0
    return TitheDeposit(count, end % sackSize, points, doubled, bonus)
}
