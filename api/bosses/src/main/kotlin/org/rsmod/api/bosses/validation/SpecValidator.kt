package org.rsmod.api.bosses.validation

import org.rsmod.api.bosses.spec.*

data class ValidationError(val message: String)

object SpecValidator {

    fun validate(spec: BossSpec): List<ValidationError> = SpecCheck(spec).run()

    /**
     * Where an effect sits: [tileBound] inside a [Effect.Projectile.onImpact] or [Effect.OnTiles]
     * (so `ImpactTile`/`EachTile` resolve), [deferred] when it runs on a later tick outside the
     * ability's own timeline ([Effect.After], impacts, on-hit effects).
     */
    private data class Scope(
        val label: String,
        val tileBound: Boolean = false,
        val deferred: Boolean = false,
    ) {
        val prefix: String
            get() = if (label.isNotEmpty()) "$label: " else ""
    }

    private class SpecCheck(private val spec: BossSpec) {
        private val errors = mutableListOf<ValidationError>()
        private val abilityNames = spec.abilities.keys
        private val phaseNames = spec.phases.keys

        fun run(): List<ValidationError> {
            val bossName = spec.npcTypes.joinToString()
            if (spec.abilities.isEmpty()) error("Boss '$bossName' has no abilities defined.")
            if (spec.phases.isEmpty()) error("Boss '$bossName' has no phases defined.")

            for ((phaseName, phase) in spec.phases) {
                val scope = Scope("phase '$phaseName'")
                phase.entry?.let { requireAbility(it, scope, "entry ability") }
                phase.exit?.let { requireAbility(it, scope, "exit ability") }
                for (forced in phase.forceAbilities) {
                    requireAbility(forced.ability, scope, "forced ability")
                    forced.condition?.let { condition(it, scope) }
                }
                selector(phase.selector, scope, abilityNames)
            }

            spec.triggers.forEach { effect(it.effect, Scope("trigger")) }
            spec.triggers.forEach { condition(it.condition, Scope("trigger")) }

            for (reaction in spec.hitReactions) {
                val scope = Scope("hit reaction")
                condition(reaction.requires, scope)
                effect(reaction.effect, scope)
            }

            for (rule in spec.incomingRules) {
                val scope = Scope("incoming rule")
                condition(rule.condition, scope)
                if (rule.actions.isEmpty()) error("${scope.prefix}rule has no actions.")
                for (action in rule.actions) {
                    when (action) {
                        is IncomingAction.Run -> effect(action.effect, scope)
                        is IncomingAction.FloorPercentOfMaxHit ->
                            if (action.style != HitType.Ranged && action.style != HitType.Melee) {
                                error(
                                    "${scope.prefix}floorPercentOfMaxHit only supports Ranged and Melee, " +
                                        "not ${action.style}."
                                )
                            }
                        is IncomingAction.Cap,
                        is IncomingAction.ScalePercent -> {}
                    }
                }
            }

            for ((name, ability) in spec.abilities) {
                effect(ability, Scope("ability '$name'"))
            }

            val hpValues = spec.phases.values.mapNotNull { it.entryHp }
            if (hpValues.size != hpValues.distinct().size) {
                error("Multiple phases share the same entryHp value — ambiguous transition order.")
            }
            return errors
        }

        private fun error(message: String) {
            errors += ValidationError(message)
        }

        private fun requireAbility(name: String, scope: Scope, what: String) {
            if (name !in abilityNames) error("${scope.prefix}$what '$name' does not exist.")
        }

        private fun selector(selector: Selector, scope: Scope, names: Set<String>) {
            when (selector) {
                is Selector.WeightedRandom ->
                    for (ref in selector.entries) {
                        if (ref.ability !in names) {
                            error("${scope.prefix}selector references '${ref.ability}' which does not exist.")
                        }
                        condition(ref.requires, scope)
                    }
                is Selector.Rotation ->
                    for (name in selector.sequence) {
                        if (name !in names) error("${scope.prefix}rotation references '$name' which does not exist.")
                    }
                is Selector.Conditional -> {
                    for ((cond, name) in selector.branches) {
                        if (name !in names) error("${scope.prefix}conditional references '$name' which does not exist.")
                        condition(cond, scope)
                    }
                    if (selector.fallback !in names) {
                        error("${scope.prefix}conditional fallback '${selector.fallback}' does not exist.")
                    }
                }
            }
        }

        private fun condition(condition: Condition, scope: Scope) {
            when (condition) {
                is Condition.InPhase ->
                    if (condition.phase !in phaseNames) {
                        error("${scope.prefix}InPhase references phase '${condition.phase}' which does not exist.")
                    }
                is Condition.LastAbility -> requireAbility(condition.ability, scope, "lastAbility")
                is Condition.AbilityUsed -> requireAbility(condition.ability, scope, "AbilityUsed")
                is Condition.VarnIn -> varn(condition.varn, scope)
                is Condition.TargetInArc -> varn(condition.bearingVarn, scope)
                is Condition.TargetWithin -> target(condition.of, scope, "TargetWithin")
                is Condition.Not -> condition(condition.c, scope)
                is Condition.And -> {
                    condition(condition.a, scope)
                    condition(condition.b, scope)
                }
                is Condition.Or -> {
                    condition(condition.a, scope)
                    condition(condition.b, scope)
                }
                else -> {}
            }
        }

        private fun varn(name: String, scope: Scope) {
            if (!name.startsWith("varn.")) {
                error("${scope.prefix}'$name' is not a varn reference (expected \"varn.<name>\").")
            }
        }

        private fun varExpr(expr: VarExpr, scope: Scope) {
            when (expr) {
                is VarExpr.Const -> {}
                is VarExpr.Varn -> varn(expr.varn, scope)
                is VarExpr.Plus -> {
                    varExpr(expr.a, scope)
                    varExpr(expr.b, scope)
                }
                is VarExpr.Min -> {
                    varExpr(expr.a, scope)
                    varExpr(expr.b, scope)
                }
                is VarExpr.Max -> {
                    varExpr(expr.a, scope)
                    varExpr(expr.b, scope)
                }
                is VarExpr.BearingTo -> {
                    target(expr.to, scope, "bearingTo")
                    target(expr.from, scope, "bearingTo")
                }
            }
        }

        private fun target(expr: TargetExpr, scope: Scope, what: String) {
            if (!scope.tileBound && usesBoundTile(expr)) {
                error(
                    "${scope.prefix}$what references ImpactTile/EachTile outside a Projectile.onImpact or " +
                        "OnTiles — it silently falls back to the caster's tile there."
                )
            }
        }

        private fun area(area: Area, scope: Scope, what: String) {
            target(area.sw, scope, what)
            target(area.ne, scope, what)
        }

        private fun tileSet(tiles: TileSet, scope: Scope) {
            area(tiles.area, scope, "OnTiles tile set")
            if (tiles is TileSet.Nearest) tiles.tiles.forEach { target(it, scope, "OnTiles tile set") }
        }

        private fun usesBoundTile(expr: TargetExpr): Boolean =
            when (expr) {
                is TargetExpr.ImpactTile,
                is TargetExpr.EachTile -> true
                is TargetExpr.PlayersOn -> usesBoundTile(expr.tile)
                is TargetExpr.PlayersIn -> usesBoundTile(expr.area.sw) || usesBoundTile(expr.area.ne)
                is TargetExpr.RandomWalkableTile -> usesBoundTile(expr.of)
                is TargetExpr.AllInRadius -> usesBoundTile(expr.of)
                is TargetExpr.Toward -> usesBoundTile(expr.from) || usesBoundTile(expr.to)
                else -> false
            }

        private fun effect(effect: Effect, scope: Scope) {
            val name = effect::class.simpleName ?: "Effect"
            when (effect) {
                is Effect.Run -> requireAbility(effect.ability, scope, "Run")
                is Effect.ForceNext -> requireAbility(effect.ability, scope, "ForceNext")
                is Effect.TransitionTo ->
                    if (effect.phase !in phaseNames) {
                        error("${scope.prefix}TransitionTo references phase '${effect.phase}' which does not exist.")
                    }
                is Effect.Wait ->
                    if (effect.ticks <= 0) error("${scope.prefix}Wait ticks '${effect.ticks}' must be greater than 0.")
                is Effect.Interrupt ->
                    if (scope.deferred) {
                        error(
                            "${scope.prefix}Interrupt inside After/onImpact/onHit fires on a later tick and " +
                                "would cancel whichever ability happens to be running then."
                        )
                    }
                is Effect.Sequence -> effect.effects.forEach { effect(it, scope) }
                is Effect.Parallel ->
                    for (child in effect.effects) {
                        if (child is Effect.Wait || child is Effect.Delay) {
                            error(
                                "${scope.prefix}Parallel cannot contain a Wait/Delay directly — its effects " +
                                    "must all fire on the same tick. Give the timed branch its own Sequence instead."
                            )
                        }
                        effect(child, scope)
                    }
                is Effect.Repeat -> {
                    if (effect.times.isEmpty() || effect.times.first < 0) {
                        error("${scope.prefix}Repeat times '${effect.times}' must be a non-empty, non-negative range.")
                    }
                    effect(effect.effect, scope)
                }
                is Effect.Whenever -> {
                    condition(effect.condition, scope)
                    effect(effect.then, scope)
                    effect(effect.otherwise, scope)
                }
                is Effect.Choose -> {
                    selector(effect.selector, scope, effect.branches.keys)
                    effect.branches.values.forEach { effect(it, scope) }
                }
                is Effect.OnEach -> {
                    target(effect.targets, scope, name)
                    effect(effect.effect, scope)
                }
                is Effect.OnTiles -> {
                    tileSet(effect.tiles, scope)
                    effect(effect.effect, scope.copy(tileBound = true))
                }
                is Effect.After -> {
                    if (effect.ticks <= 0) error("${scope.prefix}After ticks '${effect.ticks}' must be greater than 0.")
                    effect(effect.effect, scope.copy(deferred = true))
                }
                is Effect.SetVarn -> {
                    varn(effect.varn, scope)
                    varExpr(effect.value, scope)
                }
                is Effect.Switch -> {
                    varn(effect.varn, scope)
                    if (effect.cases.isEmpty()) error("${scope.prefix}Switch on '${effect.varn}' has no cases.")
                    (effect.cases.values + effect.otherwise).forEach { effect(it, scope) }
                }
                is Effect.Hit -> hit(effect, scope, projectile = null)
                is Effect.Projectile -> projectile(effect, scope)
                is Effect.SpawnLoc -> {
                    target(effect.at, scope, name)
                    if (effect.angle !in 0..3) error("${scope.prefix}SpawnLoc angle '${effect.angle}' must be 0..3.")
                }
                is Effect.Knockback -> area(effect.within, scope, name)
                is Effect.Message -> target(effect.target, scope, name)
                is Effect.SoundTo -> target(effect.target, scope, name)
                is Effect.Sound -> effect.at?.let { target(it, scope, name) }
                is Effect.MapSpotanim -> target(effect.at, scope, name)
                is Effect.CamShake -> effect.target?.let { target(it, scope, name) }
                is Effect.CamReset -> target(effect.target, scope, name)
                is Effect.Debris -> target(effect.center, scope, name)
                is Effect.Summon -> target(effect.centeredOn, scope, name)
                is Effect.Teleport -> target(effect.to, scope, name)
                is Effect.FaceTile -> target(effect.at, scope, name)
                else -> {}
            }
        }

        private fun hit(hit: Effect.Hit, scope: Scope, projectile: Effect.Projectile?) {
            target(hit.target, scope, "Hit")
            hit.penetrationWhen?.let { condition(it, scope) }
            val resolvedOnImpact = projectile?.resolveOnImpact == true
            if (!resolvedOnImpact && hit.penetrationWhen != null) {
                error(
                    "${scope.prefix}penetration(whenever = …) only works on a projectile with " +
                        "resolveOnImpact = true; " +
                        "elsewhere the penetration would apply unconditionally."
                )
            }
            if (!resolvedOnImpact && hit.spotanimUnlessPraying) {
                error(
                    "${scope.prefix}spotanim(unlessPraying = true) only works on a projectile with " +
                        "resolveOnImpact = true."
                )
            }
            if (projectile != null && hit.hazard) {
                error("${scope.prefix}hazard() has no effect on a projectile hit, which always resolves as combat.")
            }
            hit.onHit?.let { effect(it, scope.copy(deferred = true)) }
        }

        private fun projectile(proj: Effect.Projectile, scope: Scope) {
            target(proj.target, scope, "Projectile")
            proj.from?.let { target(it, scope, "Projectile") }
            proj.hit?.let { hit(it, scope, projectile = proj) }
            proj.onImpact?.let { effect(it, scope.copy(tileBound = true, deferred = true)) }
        }
    }
}
