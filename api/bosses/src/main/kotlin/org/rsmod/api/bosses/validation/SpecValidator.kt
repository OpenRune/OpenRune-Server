package org.rsmod.api.bosses.validation

import org.rsmod.api.bosses.spec.*

data class ValidationError(val message: String)

object SpecValidator {
    /** Largest end time a [org.rsmod.game.headbar.Headbar] can carry: 8 bits in steps of 5. */
    private const val MAX_HEADBAR_CYCLES = 1275

    fun validate(spec: BossSpec): List<ValidationError> = SpecCheck(spec).run()

    /** Validates each spec, plus what specs sharing npc types must agree on: those npc types. */
    fun validateAll(specs: Collection<BossSpec>): List<ValidationError> {
        val first = specs.firstOrNull() ?: return listOf(ValidationError("No specs given."))
        val errors = mutableListOf<ValidationError>()
        for ((index, spec) in specs.withIndex()) {
            if (spec.npcTypes.toSet() != first.npcTypes.toSet()) {
                val message = "spec $index: npc types ${spec.npcTypes} differ from ${first.npcTypes}."
                errors += ValidationError(message)
            }
            errors += validate(spec).map { ValidationError("spec $index: ${it.message}") }
        }
        return errors
    }

    /**
     * Where an effect sits: [impactTileBound] inside a [Effect.Projectile.onImpact] (so
     * `ImpactTile` resolves), [currentTileBound] inside an [Effect.OnTiles] (so `CurrentTile`
     * resolves), [tileNames]/[setNames] bound by enclosing [Effect.WithTile]/[Effect.WithTiles],
     * [deferred] when it runs on a later tick outside the ability's own timeline ([Effect.After],
     * impacts, on-hit effects).
     */
    private data class Scope(
        val label: String,
        val impactTileBound: Boolean = false,
        val currentTileBound: Boolean = false,
        val tileNames: Set<String> = emptySet(),
        val setNames: Set<String> = emptySet(),
        val deferred: Boolean = false,
        val hitAware: Boolean = false,
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
                phase.transmog?.let {
                    if (it !in spec.npcTypes) {
                        error("${scope.prefix}transmog '$it' is not one of the boss npc types ${spec.npcTypes}.")
                    }
                }
                for (forced in phase.forceAbilities) {
                    requireAbility(forced.ability, scope, "forced ability")
                    forced.condition?.let { condition(it, scope) }
                }
                selector(phase.selector, scope, abilityNames)
                phase.timers.forEach { timer(it, Scope("phase '$phaseName' timer", deferred = true)) }
            }

            spec.timers.forEach { timer(it, Scope("timer", deferred = true)) }

            for ((ability, delay) in spec.abilityAttackDelays) {
                val scope = Scope("ability '$ability'")
                requireAbility(ability, scope, "attack delay")
                if (delay <= 0) error("${scope.prefix}attack delay '$delay' must be greater than 0.")
            }

            spec.triggers.forEach { effect(it.effect, Scope("trigger")) }
            spec.triggers.forEach { condition(it.condition, Scope("trigger")) }

            for (reaction in spec.hitReactions) {
                val scope = Scope("hit reaction")
                condition(reaction.requires, scope.copy(hitAware = true))
                effect(reaction.effect, scope)
            }

            for (rule in spec.incomingRules) {
                val scope = Scope("incoming rule")
                condition(rule.condition, scope.copy(hitAware = true))
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

        private fun timer(timer: TimerSpec, scope: Scope) {
            if (timer.ticks.isEmpty() || timer.ticks.first <= 0) {
                error("${scope.prefix}ticks '${timer.ticks}' must be a non-empty range of positive ticks.")
            }
            effect(timer.effect, scope)
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
            }
        }

        private fun condition(condition: Condition, scope: Scope) {
            when (condition) {
                is Condition.InPhase ->
                    if (condition.phase !in phaseNames) {
                        error("${scope.prefix}InPhase references phase '${condition.phase}' which does not exist.")
                    }
                is Condition.HpBelow ->
                    if (condition.fraction !in 0.0..1.0) {
                        error("${scope.prefix}HpBelow fraction '${condition.fraction}' must be within 0.0..1.0.")
                    }
                is Condition.LastAbility -> requireAbility(condition.ability, scope, "lastAbility")
                is Condition.AbilityUsed -> requireAbility(condition.ability, scope, "AbilityUsed")
                is Condition.VarnIn -> varn(condition.varn, scope)
                is Condition.VarnExpired -> varn(condition.varn, scope)
                is Condition.TilesEmpty -> boundSet(condition.name, scope, "tilesEmpty")
                is Condition.PhaseTicksAtLeast ->
                    if (condition.ticks < 0) {
                        error("${scope.prefix}phaseTicksAtLeast ticks '${condition.ticks}' must not be negative.")
                    }
                is Condition.HitStyle,
                is Condition.HitDemonbane,
                is Condition.HitDamageAtLeast ->
                    if (!scope.hitAware) {
                        error(
                            "${scope.prefix}$condition only works in an incoming rule's condition " +
                                "or a hit reaction's requires, where there is a hit."
                        )
                    }
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

        private fun headbar(effect: Effect.Headbar, scope: Scope) {
            headbarRef(effect.headbar, scope)
            for (fill in listOf(effect.fromPercent, effect.toPercent)) {
                if (fill !in 0..100) error("${scope.prefix}Headbar fill '$fill' must be within 0..100.")
            }
            if (effect.cycles !in 0..MAX_HEADBAR_CYCLES) {
                val cycles = effect.cycles
                error("${scope.prefix}Headbar cycles '$cycles' must be within 0..$MAX_HEADBAR_CYCLES.")
            }
        }

        private fun headbarRef(name: String, scope: Scope) {
            if (!name.startsWith("headbar.")) {
                error("${scope.prefix}'$name' is not a headbar reference (expected headbar.<name>).")
            }
        }

        private fun headIconSlot(slot: Int, scope: Scope) {
            if (slot !in 0..7) error("${scope.prefix}head icon slot '$slot' must be within 0..7.")
        }

        private fun varn(name: String, scope: Scope) {
            if (!name.startsWith("varn.")) {
                error("${scope.prefix}'$name' is not a varn reference (expected \"varn.<name>\").")
            }
        }

        private fun varExpr(expr: VarExpr, scope: Scope) {
            when (expr) {
                is VarExpr.Const,
                is VarExpr.Now -> {}
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
            for (tile in tilesIn(expr)) {
                when (tile) {
                    is TargetExpr.ImpactTile ->
                        if (!scope.impactTileBound) {
                            error(
                                "${scope.prefix}$what references ImpactTile outside a " +
                                    "Projectile.onImpact, where there is no impact tile."
                            )
                        }
                    is TargetExpr.CurrentTile ->
                        if (!scope.currentTileBound) {
                            error(
                                "${scope.prefix}$what references CurrentTile outside an OnTiles, " +
                                    "where there is no current tile."
                            )
                        }
                    is TargetExpr.Bound -> boundTile(tile.name, scope, what)
                    is TargetExpr.RandomOfBound -> boundSet(tile.name, scope, "$what randomOf")
                    else -> {}
                }
            }
        }

        private fun boundTile(name: String, scope: Scope, what: String) {
            if (name !in scope.tileNames) {
                val where = "outside a withTile(\"$name\")"
                error("${scope.prefix}$what references tile(\"$name\") $where.")
            }
        }

        private fun boundSet(name: String, scope: Scope, what: String) {
            if (name !in scope.setNames) {
                val where = "outside a withTiles(\"$name\")"
                error("${scope.prefix}$what references tile set \"$name\" $where.")
            }
        }

        /** Every single tile [expr] resolves, including the anchors it is built from. */
        private fun tilesIn(expr: TargetExpr): List<TargetExpr.Single> =
            when (expr) {
                is TargetExpr.PlayersOn -> tilesIn(expr.tile)
                is TargetExpr.PlayersIn -> tilesIn(expr.area.sw) + tilesIn(expr.area.ne)
                is TargetExpr.AllInRadius -> tilesIn(expr.of)
                is TargetExpr.TopN -> tilesIn(expr.by)
                is TargetExpr.RandomWalkableTile -> listOf(expr) + tilesIn(expr.of)
                is TargetExpr.Offset -> listOf(expr) + tilesIn(expr.of)
                is TargetExpr.Toward -> listOf(expr) + tilesIn(expr.from) + tilesIn(expr.to)
                is TargetExpr.Single -> listOf(expr)
                is TargetExpr.FacingQuadrant -> emptyList()
            }

        private fun area(area: Area, scope: Scope, what: String) {
            target(area.sw, scope, what)
            target(area.ne, scope, what)
        }

        private fun tileSet(tiles: TileSet, scope: Scope) {
            val what = "OnTiles tile set"
            when (tiles) {
                is TileSet.RandomFree -> area(tiles.area, scope, what)
                is TileSet.UnderPlayers -> area(tiles.area, scope, what)
                is TileSet.Nearest -> {
                    area(tiles.area, scope, what)
                    tiles.tiles.forEach { target(it, scope, what) }
                }
                is TileSet.Custom -> area(tiles.area, scope, what)
                is TileSet.Plus -> {
                    tileSet(tiles.a, scope)
                    tileSet(tiles.b, scope)
                }
                is TileSet.Bound -> boundSet(tiles.name, scope, what)
            }
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
                is Effect.NextAttackIn ->
                    if (effect.ticks < 0) {
                        error("${scope.prefix}NextAttackIn ticks '${effect.ticks}' must not be negative.")
                    }
                is Effect.HealSelf ->
                    if (effect.amount <= 0) {
                        error("${scope.prefix}HealSelf amount '${effect.amount}' must be greater than 0.")
                    }
                is Effect.Headbar -> headbar(effect, scope)
                is Effect.ClearHeadbar -> headbarRef(effect.headbar, scope)
                is Effect.HeadIcon -> headIconSlot(effect.slot, scope)
                is Effect.ClearHeadIcon -> headIconSlot(effect.slot, scope)
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
                    effect(effect.effect, scope.copy(currentTileBound = true))
                }
                is Effect.WithTile -> {
                    target(effect.tile, scope, "withTile(\"${effect.name}\")")
                    effect(effect.effect, scope.copy(tileNames = scope.tileNames + effect.name))
                }
                is Effect.WithTiles -> {
                    tileSet(effect.tiles, scope)
                    effect(effect.effect, scope.copy(setNames = scope.setNames + effect.name))
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
                is Effect.External ->
                    effect.at?.let { target(it, scope, "External '${effect.handler}'") }
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
            proj.onImpact?.let { effect(it, scope.copy(impactTileBound = true, deferred = true)) }
        }
    }
}
