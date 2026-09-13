package org.rsmod.content.bosses.zulrah

import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.spec.BossSpec

internal object ZulrahSpec {
    // Provisional impact geometry and timing; see EVIDENCE.md.
    val tail = ZulrahTailAttack(impactDelay = 4, stunTicks = 5, damage = 20..30)
    val boss: BossSpec by lazy { rotating() }

    fun rotating(enabledRotations: List<Int> = (1..4).toList()): BossSpec {
        require(enabledRotations.isNotEmpty() && enabledRotations.distinct() == enabledRotations)
        require(enabledRotations.all { it in 1..4 })
        return boss(*ZulrahEncounterController.BOSS_FORMS.toTypedArray()) {
            stats(attackRate = 1, aggressionRadius = 64, retaliateOnHit = false)
            timeline("opening", listOf(ZulrahRotations.opening), "select_rotation")
            for (rotation in enabledRotations) {
                timeline("rotation_$rotation", ZulrahRotations.rotations[rotation - 1].drop(1), "recurring_0")
            }
            timeline("recurring", listOf(ZulrahRotations.recurringOpening), "select_rotation")
            val select = ability("select_rotation") {
                include(choose(weightedRandom(noRepeatBias = 0.0) {
                    for (rotation in enabledRotations) +random("rotation_$rotation", weight = 1)
                }, enabledRotations.associate { rotation ->
                    "rotation_$rotation" to sequence(
                        TransitionTo("rotation_${rotation}_0"), Run("rotation_${rotation}_event_0"))
                }))
            }
            phase("select_rotation", lockMovement = true) { entry = select.name }
            triggers { on(Always) runs run("opening_event_0") }
        }
    }

    private fun BossSpecBuilder.timeline(prefix: String, phases: List<ZulrahPhase>, nextPhase: String) {
        var duration = 0
        val events = phases.flatMap { phase ->
            val offset = duration
            duration += phase.duration
            phase.events.map { it.copy(tick = it.tick + offset) }
        }
        val steps = events.groupBy { it.tick }.entries.toList()
        for ((index, step) in steps.withIndex()) {
            val action = ability("${prefix}_event_${step.key}") { eventEffects(step.value) }
            val next = steps.getOrNull(index + 1)?.key ?: duration
            phase("${prefix}_${step.key}", lockMovement = true, exitAfter = next - step.key,
                nextPhase = if (index == steps.lastIndex) nextPhase else "${prefix}_$next") {
                entry = action.name
            }
        }
    }

    private fun AbilityBuilder.eventEffects(events: List<ZulrahRoutineEvent>) {
        for (event in events) {
            when (event.kind) {
                "emerge" -> {
                    include(external("zulrah.emerge", event))
                    anim(if (event.initial) "seq.snakeboss_spawn" else "seq.snakeboss_emergefast")
                }
                "dive" -> {
                    include(external("zulrah.dive"))
                    anim("seq.snakeboss_sinkfast")
                }
                "tail" -> {
                    include(external("zulrah.tail_attack", tail))
                    anim(event.symbol)
                }
                "attack" -> {
                    anim("seq.snakeboss_attack_acidx1")
                    if (event.mixedAttack) {
                        val weights = ZulrahRotations.mixedWeights
                        include(choose(weightedRandom(noRepeatBias = 0.0) {
                            for ((symbol, weight) in weights) +random(symbol, weight)
                        }, weights.keys.associateWith { symbol ->
                            external("zulrah.attack", event.copy(symbol = symbol))
                        }))
                    } else include(external("zulrah.attack", event))
                }
                "gas", "egg" -> {
                    anim("seq.snakeboss_attack_acidx1")
                    include(external("zulrah.hazard", event))
                }
            }
        }
    }

    fun recorded(routine: ZulrahRoutine): BossSpec =
        boss(*ZulrahEncounterController.BOSS_FORMS.toTypedArray()) {
            stats(attackRate = 1, aggressionRadius = 64, retaliateOnHit = false)
            val steps = routine.events.groupBy { it.tick }.entries.toList()
            for ((index, step) in steps.withIndex()) {
                val action = ability("recorded_${step.key}") {
                    eventEffects(step.value)
                }
                val next = steps.getOrNull(index + 1)?.key ?: routine.endTick
                phase("step_${step.key}", lockMovement = true,
                    exitAfter = next - step.key,
                    nextPhase = if (index == steps.lastIndex) "evidence_limit" else "step_$next") {
                    entry = action.name
                }
                if (index == 0) triggers { on(Always) runs run(action) }
            }
            val stop = ability("stop_at_evidence_limit") { include(external("zulrah.evidence_limit")) }
            phase("evidence_limit", lockMovement = true) { entry = stop.name }
        }

    val snakelings: BossSpec =
        boss(ZulrahEncounterController.MELEE_SNAKE, ZulrahEncounterController.MAGIC_SNAKE) {
            stats(attackRate = 3, aggressionRadius = 64, retaliateOnHit = false)
            val attack = ability("attack_owner") {
                anim("seq.snakeboss_pet_attack")
                include(external("zulrah.snake_attack"))
            }
            phase("combat") { rotationSelector { +then(attack) } }
        }
}

internal data class ZulrahTailAttack(
    val impactDelay: Int,
    val stunTicks: Int,
    val damage: IntRange,
)
