package org.rsmod.api.bosses.runtime

import dev.openrune.types.NpcServerType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.phaseTicksAtLeast
import org.rsmod.api.bosses.dsl.say
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid

class BossTimersTest {
    private val specTimer = say("spec")
    private val fightTimer = say("fight")
    private val shieldTimer = say("shield")

    private val spec =
        boss("npc.boss") {
            ability("a", say("a"))
            every(3, specTimer)
            phase("fight") { every(2, fightTimer) }
            phase("shielded") { every(5..7, shieldTimer) }
        }

    @Test
    fun `spec timers count from the start tick and repeat`() {
        val (encounter, _) = encounter()
        encounter.phaseEnteredTick = 10
        val schedule = TimerSchedule(encounter, startTick = 10) { it.first }
        assertEquals(listOf(13, 16, 19), firedOn(schedule, 11..19, specTimer))
    }

    @Test
    fun `phase timers count from the phase entry and stop when it is left`() {
        val (encounter, _) = encounter()
        encounter.phaseEnteredTick = 10
        val schedule = TimerSchedule(encounter, startTick = 10) { it.first }
        assertEquals(listOf(12, 14), firedOn(schedule, 11..15, fightTimer))

        encounter.transitionTo("shielded", 15)
        val fired = (16..30).associateWith { schedule.due(it) }
        assertTrue(fired.values.none { fightTimer in it })
        assertEquals(listOf(20, 25, 30), fired.filterValues { shieldTimer in it }.keys.toList())
    }

    @Test
    fun `re-entering a phase restarts its timers`() {
        val (encounter, _) = encounter()
        encounter.phaseEnteredTick = 10
        val schedule = TimerSchedule(encounter, startTick = 10) { it.first }
        assertEquals(listOf(12), firedOn(schedule, 11..12, fightTimer))
        encounter.transitionTo("fight", 13)
        assertEquals(listOf(15, 17), firedOn(schedule, 13..17, fightTimer))
    }

    @Test
    fun `ranged timers re-roll their interval after each fire`() {
        val (encounter, _) = encounter()
        encounter.transitionTo("shielded", 0)
        val shieldRolls = ArrayDeque(listOf(5, 7, 6, 5))
        val schedule =
            TimerSchedule(encounter, startTick = 0) { if (it == 5..7) shieldRolls.removeFirst() else it.first }
        assertEquals(listOf(5, 12, 18), firedOn(schedule, 1..18, shieldTimer))
    }

    @Test
    fun `skipWhileBusy skips a fire while an effect is mid-wait and waits the next interval`() {
        val busyTimer = say("busy")
        val freeTimer = say("free")
        val spec =
            boss("npc.boss") {
                ability("a", say("a"))
                every(2, busyTimer, skipWhileBusy = true)
                every(2, freeTimer)
                phase("fight") {}
            }
        val encounter = BossEncounter(Npc(type, CoordGrid(0, 1, 1, 0, 0)), spec, MapClock()) { null }
        encounter.phaseEnteredTick = 0
        encounter.busyUntil = 5
        val schedule = TimerSchedule(encounter, startTick = 0) { it.first }
        val fired = (1..8).associateWith { schedule.due(it) }
        assertEquals(listOf(6, 8), fired.filterValues { busyTimer in it }.keys.toList())
        assertEquals(listOf(2, 4, 6, 8), fired.filterValues { freeTimer in it }.keys.toList())
    }

    @Test
    fun `phaseTicksAtLeast counts from the phase entry`() {
        val (encounter, clock) = encounter()
        assertFalse(encounter.evaluate(phaseTicksAtLeast(0)))
        encounter.transitionTo("shielded", 20)
        clock.cycle = 99
        assertTrue(encounter.evaluate(phaseTicksAtLeast(79)))
        assertFalse(encounter.evaluate(phaseTicksAtLeast(80)))
        clock.cycle = 100
        assertTrue(encounter.evaluate(phaseTicksAtLeast(80)))
    }

    private fun firedOn(schedule: TimerSchedule, ticks: IntRange, timer: Effect): List<Int> =
        ticks.filter { timer in schedule.due(it) }

    private val type = NpcServerType(id = 1, name = "Boss", size = 1, hitpoints = 100)

    private fun encounter(): Pair<BossEncounter, MapClock> {
        val clock = MapClock()
        return BossEncounter(Npc(type, CoordGrid(0, 1, 1, 0, 0)), spec, clock) { null } to clock
    }
}
