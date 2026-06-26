package org.rsmod.api.bosses.validation

import org.rsmod.api.bosses.spec.*

data class ValidationError(val message: String)

object SpecValidator {

    fun validate(spec: BossSpec): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val phaseNames = spec.phases.keys

        if (spec.abilities.isEmpty()) {
            errors += ValidationError("Boss '${spec.npcType}' has no abilities defined.")
        }

        if (spec.phases.isEmpty()) {
            errors += ValidationError("Boss '${spec.npcType}' has no phases defined.")
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
    ) {
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
            is Effect.Sequence -> effect.effects.forEach { validateEffect(it, abilityNames, phaseNames, errors, context) }
            is Effect.Parallel -> effect.effects.forEach { validateEffect(it, abilityNames, phaseNames, errors, context) }
            is Effect.Repeat -> validateEffect(effect.effect, abilityNames, phaseNames, errors, context)
            is Effect.Whenever -> {
                validateEffect(effect.then, abilityNames, phaseNames, errors, context)
                validateEffect(effect.otherwise, abilityNames, phaseNames, errors, context)
            }
            is Effect.OnEach -> validateEffect(effect.effect, abilityNames, phaseNames, errors, context)
            is Effect.Choose -> {
                validateSelector(effect.selector, context, abilityNames, errors)
                effect.branches.values.forEach { validateEffect(it, abilityNames, phaseNames, errors, context) }
            }
            else -> {}
        }
    }

    private fun prefix(context: String): String = if (context.isNotEmpty()) "$context: " else ""
}
