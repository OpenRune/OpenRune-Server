package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.random.GameRandom
import org.rsmod.api.table.agility.AgilityCoursesRow
import org.rsmod.api.table.agility.AgilityDispenserLootRow
import org.rsmod.content.skills.agility.WildernessDispenser.Proceed
import org.rsmod.content.skills.agility.WildernessDispenser.feePaid
import org.rsmod.content.skills.agility.WildernessDispenser.rewardAvailable
import org.rsmod.content.skills.agility.WildernessDispenser.startWarningOff
import org.rsmod.content.skills.agility.WildernessDispenser.streak
import org.rsmod.content.skills.agility.WildernessDispenser.ticketAvailable
import org.rsmod.game.entity.Player

object WildernessDispenser {
    const val FEE = 150_000
    const val MAX_STREAK = 131_071

    fun exitCourse(player: Player) {
        player.streak = 0
        player.feePaid = false
        player.rewardAvailable = false
    }

    fun loggedOut(player: Player) {
        player.streak = 0
    }

    fun bracket(rows: List<AgilityDispenserLootRow>, streak: Int): AgilityDispenserLootRow =
        rows.filter { it.firstLap <= streak }.maxByOrNull { it.firstLap }
            ?: rows.minBy { it.firstLap }

    fun <T> weighted(random: GameRandom, entries: List<Pair<T, Int>>): T {
        var roll = random.of(entries.sumOf { it.second })
        for ((entry, weight) in entries) {
            if (roll < weight) {
                return entry
            }
            roll -= weight
        }
        error("Weighted roll out of range: $entries")
    }

    fun ticketXpTenths(tickets: Int, rates: Map<Int, Int>): Int =
        rates.filterKeys { it <= tickets }.maxByOrNull { it.key }?.value ?: 0

    enum class Proceed {
        Yes,
        YesAlways,
        No,
    }

    var Player.feePaid by boolVarBit("varbit.wildy_agility_deposit")
    var Player.rewardAvailable by boolVarBit("varbit.wildy_agility_reward_available")
    var Player.ticketAvailable by boolVarBit("varbit.wildy_agility_token_available")
    var Player.streak by intVarBit("varbit.wildy_agility_reward_lap_new")
    var Player.startWarningOff by boolVarBit("varbit.wildy_agility_start_warning")
    var Player.exitWarningOff by boolVarBit("varbit.wildy_agility_exit_warning")
    var Player.paymentWarningOff by boolVarBit("varbit.wildy_agility_payment_warning")
}

/**
 * The Wilderness course's laps: each one leaves a ticket, and with the fee paid a reward and a
 * longer streak. A lap that would replace a ticket or loot not yet tagged asks before it starts.
 */
internal class WildernessLaps @Inject constructor() : AgilityCourseHook {
    private val wildernessCourse = AgilityCoursesRow.getRow(COURSE).rowId

    override fun lapDone(access: ProtectedAccess, course: AgilityCoursesRow) {
        if (course.rowId != wildernessCourse) {
            return
        }
        val player = access.player
        player.ticketAvailable = true
        if (!player.feePaid) {
            return
        }
        player.rewardAvailable = true
        player.streak = (player.streak + 1).coerceAtMost(WildernessDispenser.MAX_STREAK)
        access.mes("Your Wilderness Agility reward streak is: <col=ff0000>${player.streak}</col>")
    }

    override suspend fun mayStart(access: ProtectedAccess, obstacle: AgilityObstacle): Boolean {
        val wilderness = !obstacle.isShortcut && obstacle.course.rowId == wildernessCourse
        if (!wilderness || obstacle.step != 0) {
            return true
        }
        val player = access.player
        // UNVERIFIED: that an untagged ticket alone asks too; captures only show both untagged.
        if (!player.ticketAvailable && !player.rewardAvailable || player.startWarningOff) {
            return true
        }
        access.arriveDelay()
        val proceed =
            access.choice3(
                "Yes.",
                Proceed.Yes,
                "Yes, and don't ask again.",
                Proceed.YesAlways,
                "No.",
                Proceed.No,
                title = START_WARNING,
            )
        when (proceed) {
            Proceed.No -> return false
            Proceed.YesAlways -> player.startWarningOff = true
            Proceed.Yes -> Unit
        }
        access.delay(1)
        return true
    }

    private companion object {
        const val COURSE = "dbrow.agility_course_wilderness"
        const val START_WARNING =
            "You will lose any unclaimed rewards and tickets if you<br>complete another lap. Do " +
                "you still want to proceed?"
    }
}
