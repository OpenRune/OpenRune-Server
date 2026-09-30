package org.rsmod.api.bosses.runtime

import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.TimerSpec
import org.rsmod.api.player.isValidTarget

internal class TimerSchedule(
    private val encounter: BossEncounter,
    startTick: Int,
    private val roll: (IntRange) -> Int,
) {
    private val specDue = IntArray(encounter.spec.timers.size) { startTick + roll(encounter.spec.timers[it].ticks) }
    private var phaseEpoch = -1
    private var phaseTimers = emptyList<TimerSpec>()
    private var phaseDue = IntArray(0)

    /** The effects of every timer due on [tick], rescheduling each one that fires. */
    fun due(tick: Int): List<Effect> {
        if (phaseEpoch != encounter.phaseEpoch) {
            phaseEpoch = encounter.phaseEpoch
            phaseTimers = encounter.currentPhase?.timers.orEmpty()
            phaseDue = IntArray(phaseTimers.size) { encounter.phaseEnteredTick + roll(phaseTimers[it].ticks) }
        }
        return fire(encounter.spec.timers, specDue, tick) + fire(phaseTimers, phaseDue, tick)
    }

    private fun fire(timers: List<TimerSpec>, due: IntArray, tick: Int): List<Effect> {
        val fired = mutableListOf<Effect>()
        for ((index, timer) in timers.withIndex()) {
            if (tick < due[index]) continue
            due[index] = tick + roll(timer.ticks)
            if (timer.skipWhileBusy && tick < encounter.busyUntil) continue
            fired += timer.effect
        }
        return fired
    }
}

internal fun BossDeps.startTimers(encounter: BossEncounter, tick: Int) {
    if (encounter.timersStarted) return
    encounter.timersStarted = true
    val hasTimers = encounter.spec.timers.isNotEmpty() || encounter.spec.phases.values.any { it.timers.isNotEmpty() }
    if (!hasTimers) return
    val schedule = TimerSchedule(encounter, tick) { random.of(it) }
    fun step() {
        worldQueues.add(1) {
            if (!encounterRegistry.isActive(encounter)) return@add
            val effects = schedule.due(mapClock.cycle)
            val npc = encounter.npc
            val target = encounter.lastTarget?.takeIf { it.isValidTarget() }
            if (target != null && npc.isSlotAssigned && npc.hitpoints > 0) {
                for (effect in effects) {
                    EffectInterpreter(npc, target, encounter.spec, encounter, this).run(null, effect)
                }
            }
            step()
        }
    }
    step()
}
