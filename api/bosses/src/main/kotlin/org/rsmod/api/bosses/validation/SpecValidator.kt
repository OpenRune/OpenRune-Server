package org.rsmod.api.bosses.validation

import org.rsmod.api.bosses.spec.*

data class ValidationError(val message: String)

object SpecValidator {

    fun validate(spec: BossSpec): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val phaseNames = spec.phases.keys

        val bossName = spec.npcTypes.joinToString()

        if (spec.abilities.isEmpty()) {
            errors += ValidationError("Boss '$bossName' has no abilities defined.")
        }

        if (spec.phases.isEmpty()) {
            errors += ValidationError("Boss '$bossName' has no phases defined.")
        }

        val abilityNames = spec.abilities.keys

        for ((phaseName, phase) in spec.phases) {
            phase.entry?.let { entry ->
                if (entry !in abilityNames) {
                    errors += ValidationError("Phase '$phaseName' entry ability '$entry' does not exist.")
                }
            }

            phase.exit?.let { exit ->
                if (exit !in abilityNames) {
                    errors += ValidationError("Phase '$phaseName' exit ability '$exit' does not exist.")
                }
            }

            for (forced in phase.forceAbilities) {
                if (forced.ability !in abilityNames) {
                    errors += ValidationError("Phase '$phaseName' forced ability '${forced.ability}' does not exist.")
                }
            }

            validateSelector(phase.selector, phaseName, abilityNames, errors)
        }

        for (trigger in spec.triggers) {
            validateEffect(trigger.effect, abilityNames, phaseNames, errors)
        }

        for ((abilityName, effect) in spec.abilities) {
            validateEffect(effect, abilityNames, phaseNames, errors, context = "ability '$abilityName'")
        }

        val hpPhases = spec.phases.filter { it.value.entryHp != null }
        val hpValues = hpPhases.map { it.value.entryHp!! }
        if (hpValues.size != hpValues.distinct().size) {
            errors += ValidationError("Multiple phases share the same entryHp value — ambiguous transition order.")
        }

        return errors
    }

    private fun validateSelector(
        selector: Selector,
        context: String,
        abilityNames: Set<String>,
        errors: MutableList<ValidationError>,
    ) {
        when (selector) {
            is Selector.WeightedRandom -> {
                for (ref in selector.entries) {
                    if (ref.ability !in abilityNames) {
                        errors += ValidationError("Selector in '$context' references ability '${ref.ability}' which does not exist.")
                    }
                }
            }
            is Selector.Rotation -> {
                for (name in selector.sequence) {
                    if (name !in abilityNames) {
                        errors += ValidationError("Rotation in '$context' references ability '$name' which does not exist.")
                    }
                }
            }
            is Selector.Conditional -> {
                for ((_, name) in selector.branches) {
                    if (name !in abilityNames) {
                        errors += ValidationError("Conditional in '$context' references ability '$name' which does not exist.")
                    }
                }
                if (selector.fallback !in abilityNames) {
                    errors += ValidationError("Conditional fallback in '$context' references ability '${selector.fallback}' which does not exist.")
                }
            }
        }
    }

    private fun validateEffect(
        effect: Effect,
        abilityNames: Set<String>,
        phaseNames: Set<String>,
        errors: MutableList<ValidationError>,
        context: String = "",
        insideImpact: Boolean = false,
    ) {
        if (!insideImpact && targetExprsOf(effect).any { containsImpactTile(it) }) {
            errors +=
                ValidationError(
                    "${prefix(context)}${effect::class.simpleName} references ImpactTile outside a " +
                        "Projectile.onImpact — it silently falls back to the caster's tile there.",
                )
        }

        when (effect) {
            is Effect.Run -> {
                if (effect.ability !in abilityNames) {
                    errors += ValidationError("${prefix(context)}Run references ability '${effect.ability}' which does not exist.")
                }
            }
            is Effect.TransitionTo -> {
                if (effect.phase !in phaseNames) {
                    errors +=
                        ValidationError(
                            "${prefix(context)}TransitionTo references phase '${effect.phase}' which does not exist.",
                        )
                }
            }
            is Effect.Wait -> {
                if (effect.ticks <= 0) {
                    errors +=
                        ValidationError(
                            "${prefix(context)}Wait ticks '${effect.ticks}' must be greater than 0.",
                        )
                }
            }
            is Effect.Sequence ->
                effect.effects.forEach { validateEffect(it, abilityNames, phaseNames, errors, context, insideImpact) }
            is Effect.Parallel -> {
                for (child in effect.effects) {
                    if (child is Effect.Wait || child is Effect.Delay) {
                        errors +=
                            ValidationError(
                                "${prefix(context)}Parallel cannot contain a Wait/Delay directly — " +
                                    "its effects must all fire on the same tick. Give the timed " +
                                    "branch its own Sequence instead.",
                            )
                    }
                    validateEffect(child, abilityNames, phaseNames, errors, context, insideImpact)
                }
            }
            is Effect.Repeat -> {
                if (effect.times.isEmpty() || effect.times.first < 0) {
                    errors +=
                        ValidationError(
                            "${prefix(context)}Repeat times '${effect.times}' must be a non-empty, non-negative range.",
                        )
                }
                validateEffect(effect.effect, abilityNames, phaseNames, errors, context, insideImpact)
            }
            is Effect.Whenever -> {
                validateEffect(effect.then, abilityNames, phaseNames, errors, context, insideImpact)
                validateEffect(effect.otherwise, abilityNames, phaseNames, errors, context, insideImpact)
            }
            is Effect.OnEach -> validateEffect(effect.effect, abilityNames, phaseNames, errors, context, insideImpact)
            is Effect.Projectile -> {
                effect.onImpact?.let {
                    validateEffect(it, abilityNames, phaseNames, errors, context, insideImpact = true)
                }
            }
            is Effect.Choose -> {
                validateSelector(effect.selector, context, effect.branches.keys, errors)
                effect.branches.values.forEach {
                    validateEffect(it, abilityNames, phaseNames, errors, context, insideImpact)
                }
            }
            else -> {}
        }
    }

    private fun targetExprsOf(effect: Effect): List<TargetExpr> =
        when (effect) {
            is Effect.Message -> listOf(effect.target)
            is Effect.Hit -> listOf(effect.target)
            is Effect.Projectile -> listOf(effect.target)
            is Effect.MapSpotanim -> listOf(effect.at)
            is Effect.TileAoE -> listOf(effect.center)
            is Effect.Debris -> listOf(effect.center)
            is Effect.Summon -> listOf(effect.centeredOn)
            is Effect.OnEach -> listOf(effect.targets)
            else -> emptyList()
        }

    private fun containsImpactTile(expr: TargetExpr): Boolean =
        when (expr) {
            is TargetExpr.ImpactTile -> true
            is TargetExpr.RandomWalkableTile -> containsImpactTile(expr.of)
            is TargetExpr.AllInRadius -> containsImpactTile(expr.of)
            else -> false
        }

    private fun prefix(context: String): String = if (context.isNotEmpty()) "$context: " else ""
}
