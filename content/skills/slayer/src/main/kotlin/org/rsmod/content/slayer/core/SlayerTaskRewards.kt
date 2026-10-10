package org.rsmod.content.slayer.core

import java.util.Locale
import org.rsmod.api.table.slayer.SlayerStreakBonusesRow

object SlayerTaskRewards {

    const val FIRST_REWARDED_STREAK = 5
    const val MAX_POINTS = 131_071
    const val MAX_DISPLAYED_STREAK = 16_000

    private const val TEXT = "<col=ef1020>"
    private const val VALUE = "</col>"

    private val streakBonuses: List<SlayerStreakBonusesRow> by lazy {
        SlayerStreakBonusesRow.all().sortedByDescending { it.interval }
    }

    fun streakMultiplier(streak: Int): Int =
        streakBonuses.firstOrNull { streak % it.interval == 0 }?.multiplier ?: 1

    fun pointsForTask(basePoints: Int, streak: Int): Int {
        if (basePoints <= 0 || streak < FIRST_REWARDED_STREAK) return 0
        return basePoints * streakMultiplier(streak)
    }

    fun needMoreTasksMessage(streak: Int, wilderness: Boolean): String =
        "${TEXT}You've completed $VALUE${taskCount(streak, wilderness)}$TEXT and will need $VALUE" +
            "${FIRST_REWARDED_STREAK - streak} more$TEXT before you start receiving " +
            "Slayer points; return to a Slayer master."

    fun noPointsMessage(streak: Int, wilderness: Boolean): String =
        "${TEXT}You've completed $VALUE${taskCount(streak, wilderness)} $TEXT. You'll be " +
            "eligible to earn reward points if you complete tasks from a more advanced " +
            "Slayer Master."

    fun pointsMessage(
        streak: Int,
        wilderness: Boolean,
        awarded: Int,
        total: Int,
        maxed: Boolean,
    ): String {
        val tasks = taskCount(streak, wilderness)
        if (maxed) {
            return "${TEXT}You've completed $VALUE$tasks ${TEXT}and reached the maximum amount " +
                "of Slayer points $VALUE(${format(total)})$TEXT; return to a Slayer master."
        }
        return "${TEXT}You've completed $VALUE$tasks ${TEXT}and received $VALUE" +
            "${format(awarded)} points$TEXT, giving you a total of $VALUE${format(total)}$TEXT" +
            "; return to a Slayer master."
    }

    private fun taskCount(streak: Int, wilderness: Boolean): String {
        val prefix = if (streak > MAX_DISPLAYED_STREAK) "at least " else ""
        val shown = streak.coerceAtMost(MAX_DISPLAYED_STREAK)
        val kind = if (wilderness) "Wilderness task" else "task"
        return "$prefix${format(shown)} $kind${if (shown == 1) "" else "s"}"
    }

    private fun format(value: Int): String = String.format(Locale.US, "%,d", value)
}
