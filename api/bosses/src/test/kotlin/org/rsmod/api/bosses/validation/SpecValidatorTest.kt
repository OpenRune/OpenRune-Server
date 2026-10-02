package org.rsmod.api.bosses.validation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.BossStats
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.HitReaction
import org.rsmod.api.bosses.spec.IncomingAction
import org.rsmod.api.bosses.spec.IncomingRule
import org.rsmod.api.bosses.spec.PhaseSpec
import org.rsmod.api.bosses.spec.ProjectileConfig
import org.rsmod.api.bosses.spec.TimerSpec

class SpecValidatorTest {
    private val arena = area(spawnTile(-5, -5), spawnTile(5, 5))
    private val fixed = ProjectileConfig.fixed(startHeight = 50, endHeight = 30, delay = 30, travel = 60, angle = 10)

    @Test
    fun `leviathan-style tile hazard composition is valid`() {
        val landing =
            hit {
                target = playersOn(CurrentTile)
                damage(10..30).roll()
                type(Typeless)
                hazard()
            }
        val boulder =
            sequence(
                mapSpotanim("spotanim.boulder", CurrentTile, delay = 30),
                after(2, sequence(spawnLoc("loc.rubble", CurrentTile, angle = 3), landing)),
            )
        val effect =
            sequence(
                onTiles(randomFreeTiles(arena, 1..3), boulder),
                onTiles(tilesUnderPlayers(arena), boulder),
                onTiles(nearestFreeTiles(listOf(spawnTile(2, 2)), arena, searchRadius = 4), boulder),
            )
        assertEquals(emptyList<String>(), errorsFor(effect))
    }

    @Test
    fun `CurrentTile outside onTiles is reported`() {
        assertHasError(errorsFor(mapSpotanim("spotanim.x", CurrentTile)), "CurrentTile outside an OnTiles")
        val inImpact = projectile("spotanim.orb", config = fixed, onImpact = mapSpotanim("spotanim.x", CurrentTile))
        assertHasError(errorsFor(inImpact), "CurrentTile outside an OnTiles")
    }

    @Test
    fun `ImpactTile outside onImpact is reported`() {
        assertHasError(errorsFor(mapSpotanim("spotanim.x", ImpactTile)), "ImpactTile outside a Projectile.onImpact")
        assertHasError(
            errorsFor(onTiles(tilesUnderPlayers(arena), mapSpotanim("spotanim.x", ImpactTile))),
            "ImpactTile outside a Projectile.onImpact",
        )
    }

    @Test
    fun `ImpactTile and CurrentTile both resolve when onTiles and onImpact nest`() {
        val landing = sequence(wait(3), spawnLoc("loc.rubble", ImpactTile), mapSpotanim("spotanim.x", CurrentTile))
        val tilesInImpact =
            projectile("spotanim.orb", config = fixed, onImpact = onTiles(tilesUnderPlayers(arena), landing))
        val impactInTiles =
            onTiles(tilesUnderPlayers(arena), projectile("spotanim.orb", config = fixed, target = CurrentTile, onImpact = landing))
        assertEquals(emptyList<String>(), errorsFor(tilesInImpact))
        assertEquals(emptyList<String>(), errorsFor(impactInTiles))
    }

    @Test
    fun `an onTiles set is resolved in the enclosing scope`() {
        val nested = onTiles(nearestFreeTiles(listOf(CurrentTile), arena, 0), resetAnim())
        assertHasError(errorsFor(nested), "OnTiles tile set")
        assertEquals(emptyList<String>(), errorsFor(onTiles(tilesUnderPlayers(arena), nested)))
    }

    @Test
    fun `delayed interrupt is reported, immediate interrupt is not`() {
        assertEquals(emptyList<String>(), errorsFor(sequence(interrupt(), wait(2))))
        assertHasError(errorsFor(after(3, interrupt())), "Interrupt inside After")
    }

    @Test
    fun `conditional penetration needs an impact-resolved projectile`() {
        val conditional =
            hit {
                damage(0..10).roll()
                type(Magic)
                penetration(25, whenever = varnIs("varn.enraged", 1))
            }
        assertHasError(errorsFor(conditional), "resolveOnImpact = true")
        assertHasError(errorsFor(projectile("spotanim.orb", config = fixed, hit = conditional)), "resolveOnImpact")
        val resolved = projectile("spotanim.orb", config = fixed, resolveOnImpact = true, hit = conditional)
        assertEquals(emptyList<String>(), errorsFor(resolved))
    }

    @Test
    fun `prayer-aware spotanim needs an impact-resolved projectile`() {
        val praying =
            hit {
                damage(0..10).roll()
                type(Magic)
                spotanim("spotanim.impact", unlessPraying = true)
            }
        assertHasError(errorsFor(projectile("spotanim.orb", hit = praying)), "unlessPraying")
        assertEquals(
            emptyList<String>(),
            errorsFor(projectile("spotanim.orb", resolveOnImpact = true, hit = praying)),
        )
    }

    @Test
    fun `hazard on a projectile hit is reported`() {
        val hazard =
            hit {
                damage(0..10).roll()
                type(Typeless)
                hazard()
            }
        assertHasError(errorsFor(projectile("spotanim.orb", hit = hazard)), "hazard()")
    }

    @Test
    fun `switch needs cases and varn names`() {
        assertHasError(errorsFor(Effect.Switch("varn.stage", emptyMap())), "has no cases")
        assertHasError(errorsFor(switch("stage", 0 to resetAnim())), "is not a varn reference")
    }

    @Test
    fun `conditions are checked for unknown phases and abilities`() {
        assertHasError(errorsFor(whenever(InPhase("missing"), resetAnim())), "phase 'missing'")
        assertHasError(errorsFor(whenever(lastAbility("missing"), resetAnim())), "lastAbility 'missing'")
    }

    @Test
    fun `incoming rules and reactions are validated`() {
        val rules =
            listOf(
                IncomingRule(Always, emptyList()),
                IncomingRule(Always, listOf(IncomingAction.FloorPercentOfMaxHit(50, Magic))),
            )
        val reactions = listOf(HitReaction(run("missing")))
        val errors = SpecValidator.validate(spec(mapOf("a" to resetAnim()), reactions, rules)).map { it.message }
        assertHasError(errors, "rule has no actions")
        assertHasError(errors, "only supports Ranged and Melee")
        assertHasError(errors, "Run 'missing' does not exist")
    }

    @Test
    fun `phase transmog must be a registered boss npc type`() {
        val base = spec(mapOf("a" to resetAnim()))
        val registered = base.copy(phases = mapOf("main" to PhaseSpec("main", transmog = "npc.test")))
        val unregistered = base.copy(phases = mapOf("main" to PhaseSpec("main", transmog = "npc.other")))
        assertEquals(emptyList<String>(), SpecValidator.validate(registered).map { it.message })
        assertHasError(SpecValidator.validate(unregistered).map { it.message }, "transmog 'npc.other'")
    }

    @Test
    fun `hp condition fractions must be within 0 and 1`() {
        assertEquals(emptyList<String>(), errorsFor(whenever(HpBelow(0.75, inclusive = true), resetAnim())))
        assertHasError(errorsFor(whenever(HpBelow(75.0), resetAnim())), "HpBelow fraction '75.0'")
        assertHasError(errorsFor(whenever(HpBelow(-0.1), resetAnim())), "HpBelow fraction '-0.1'")
    }

    @Test
    fun `specs registered together are each validated and must share npc types`() {
        val valid = spec(mapOf("a" to resetAnim()))
        assertEquals(emptyList<String>(), SpecValidator.validateAll(listOf(valid, valid)).map { it.message })
        val broken = spec(mapOf("a" to run("missing")))
        val otherTypes = valid.copy(npcTypes = listOf("npc.other"))
        val errors = SpecValidator.validateAll(listOf(valid, broken, otherTypes)).map { it.message }
        assertHasError(errors, "spec 1: ")
        assertHasError(errors, "spec 2: npc types")
        assertHasError(SpecValidator.validateAll(emptyList()).map { it.message }, "No specs")
    }

    @Test
    fun `attack delays must be positive and name an ability, nextAttackIn must not be negative`() {
        val base = spec(mapOf("a" to nextAttackIn(0)))
        assertEquals(
            emptyList<String>(),
            SpecValidator.validate(base.copy(abilityAttackDelays = mapOf("a" to 4))).map { it.message },
        )
        assertHasError(
            SpecValidator.validate(base.copy(abilityAttackDelays = mapOf("a" to 0))).map { it.message },
            "attack delay '0'",
        )
        assertHasError(
            SpecValidator.validate(base.copy(abilityAttackDelays = mapOf("missing" to 4))).map { it.message },
            "attack delay 'missing' does not exist",
        )
        assertHasError(errorsFor(nextAttackIn(-1)), "NextAttackIn ticks '-1'")
    }

    @Test
    fun `healSelf amount must be positive`() {
        assertEquals(emptyList<String>(), errorsFor(healSelf(5)))
        assertHasError(errorsFor(healSelf(0)), "HealSelf amount '0'")
    }

    @Test
    fun `headbar and head icon effects are range checked`() {
        val valid =
            sequence(
                headbar("headbar.charge_80", 0, 100, 390),
                clearHeadbar("headbar.charge_80"),
                headIcon(6, 440, 1),
                clearHeadIcon(6),
            )
        assertEquals(emptyList<String>(), errorsFor(valid))
        assertHasError(errorsFor(headbar("charge_80", 0, 100, 390)), "not a headbar reference")
        assertHasError(errorsFor(headbar("headbar.charge_80", 0, 101, 390)), "fill '101'")
        assertHasError(errorsFor(headbar("headbar.charge_80", 0, 100, 1280)), "cycles '1280'")
        assertHasError(errorsFor(headIcon(8, 440, 1)), "slot '8'")
    }

    @Test
    fun `external tiles follow the bound tile scope rules`() {
        assertEquals(emptyList<String>(), errorsFor(external("doom.place_rock", at = CurrentTarget)))
        assertHasError(
            errorsFor(external("doom.place_rock", at = CurrentTile)),
            "CurrentTile outside an OnTiles",
        )
        val inTiles = onTiles(tilesUnderPlayers(arena), external("doom.clear_acid", at = CurrentTile))
        assertEquals(emptyList<String>(), errorsFor(inTiles))
    }

    @Test
    fun `offset, custom and union tile sets follow the bound tile scope rules`() {
        val debris = customTiles(arena) { _, t, _ -> listOf(t.coords) } + tilesUnderPlayers(arena)
        val spot = mapSpotanim("spotanim.x", CurrentTile)
        assertEquals(emptyList<String>(), errorsFor(onTiles(debris, spot)))
        val offsetSpot = mapSpotanim("spotanim.x", offset(CurrentTile, 1, 0))
        assertHasError(errorsFor(offsetSpot), "CurrentTile outside")
        val nestedUnion = tilesUnderPlayers(arena) + nearestFreeTiles(listOf(CurrentTile), arena, 0)
        assertHasError(errorsFor(onTiles(nestedUnion, resetAnim())), "OnTiles tile set")
    }

    @Test
    fun `deadline varns are checked like any varn`() {
        val restart =
            sequence(
                Effect.SetVarn("varn.charge_end", Now + 13),
                whenever(varnExpired("varn.charge_end"), resetAnim()),
            )
        assertEquals(emptyList<String>(), errorsFor(restart))
        assertHasError(errorsFor(whenever(varnExpired("charge_end"), resetAnim())), "not a varn reference")
    }

    @Test
    fun `named tiles and sets are lexically scoped`() {
        val debris = customTiles(arena) { _, t, _ -> listOf(t.coords) }
        val rockThrow =
            withTile(
                "split_player",
                CurrentTarget,
                withTiles(
                    "debris",
                    debris,
                    sequence(
                        onTiles(bound("debris"), mapSpotanim("spotanim.shadow", CurrentTile)),
                        wait(2),
                        external("doom.place_rock", at = tile("split_player")),
                        whenever(
                            !tilesEmpty("debris"),
                            projectile("spotanim.orb", config = fixed, from = randomOf("debris")),
                        ),
                    ),
                ),
            )
        assertEquals(emptyList<String>(), errorsFor(rockThrow))

        assertHasError(errorsFor(mapSpotanim("spotanim.x", tile("split"))), "tile(\"split\") outside")
        assertHasError(errorsFor(onTiles(bound("debris"), resetAnim())), "\"debris\" outside")
        assertHasError(errorsFor(mapSpotanim("spotanim.x", randomOf("debris"))), "\"debris\" outside")
        assertHasError(errorsFor(whenever(tilesEmpty("debris"), resetAnim())), "\"debris\" outside")
        assertHasError(
            errorsFor(withTiles("debris", debris, mapSpotanim("spotanim.x", tile("debris")))),
            "tile(\"debris\") outside",
        )
        val sibling = sequence(withTile("a", CurrentTarget, resetAnim()), mapSpotanim("spotanim.x", tile("a")))
        assertHasError(errorsFor(sibling), "tile(\"a\") outside")
    }

    @Test
    fun `hit conditions only gate incoming rules and hit reactions`() {
        val punish = HitReaction(Effect.Run("a"), requires = varnIs("varn.charge", 1) and hitStyle(Melee))
        val shield = IncomingRule(!hitDemonbane() and hitDamageAtLeast(1), listOf(IncomingAction.Cap(0)))
        val valid = spec(mapOf("a" to resetAnim()), reactions = listOf(punish), rules = listOf(shield))
        assertEquals(emptyList<String>(), SpecValidator.validate(valid).map { it.message })

        assertHasError(errorsFor(whenever(hitStyle(Melee), resetAnim())), "only works in an incoming rule")
        val inReactionEffect =
            HitReaction(whenever(hitDemonbane(), resetAnim()), requires = hitStyle(Melee))
        assertHasError(
            SpecValidator.validate(spec(mapOf("a" to resetAnim()), reactions = listOf(inReactionEffect)))
                .map { it.message },
            "only works in an incoming rule",
        )
    }

    @Test
    fun `timers need positive ticks and run their effect as deferred`() {
        val valid =
            spec(mapOf("a" to resetAnim())).copy(
                timers = listOf(TimerSpec(1..1, whenever(varnExpired("varn.charge_end"), run("a")))),
                phases = mapOf("main" to PhaseSpec("main", timers = listOf(TimerSpec(7..9, run("a"))))),
            )
        assertEquals(emptyList<String>(), SpecValidator.validate(valid).map { it.message })

        fun errors(timer: TimerSpec) =
            SpecValidator.validate(spec(mapOf("a" to resetAnim())).copy(timers = listOf(timer))).map { it.message }
        assertHasError(errors(TimerSpec(0..0, resetAnim())), "positive ticks")
        assertHasError(errors(TimerSpec(5..3, resetAnim())), "positive ticks")
        assertHasError(errors(TimerSpec(1..1, interrupt())), "Interrupt inside")
        assertHasError(errors(TimerSpec(1..1, whenever(hitStyle(Melee), resetAnim()))), "only works in an incoming rule")
        val phaseTimer = PhaseSpec("main", timers = listOf(TimerSpec(1..1, run("missing"))))
        assertHasError(
            SpecValidator.validate(spec(mapOf("a" to resetAnim())).copy(phases = mapOf("main" to phaseTimer)))
                .map { it.message },
            "phase 'main' timer: Run 'missing' does not exist",
        )
    }

    @Test
    fun `phaseTicksAtLeast must not be negative`() {
        assertEquals(emptyList<String>(), errorsFor(whenever(phaseTicksAtLeast(0), resetAnim())))
        assertHasError(errorsFor(whenever(phaseTicksAtLeast(-1), resetAnim())), "must not be negative")
    }

    private fun errorsFor(effect: Effect): List<String> =
        SpecValidator.validate(spec(mapOf("a" to effect))).map { it.message }

    private fun spec(
        abilities: Map<String, Effect>,
        reactions: List<HitReaction> = emptyList(),
        rules: List<IncomingRule> = emptyList(),
    ): BossSpec =
        BossSpec(
            npcTypes = listOf("npc.test"),
            stats = BossStats(),
            abilities = abilities,
            phases = mapOf("main" to PhaseSpec("main")),
            triggers = emptyList(),
            hitReactions = reactions,
            incomingRules = rules,
        )

    private fun assertHasError(errors: List<String>, fragment: String) {
        assertTrue(errors.any { fragment in it }, "expected an error containing '$fragment', got $errors")
    }
}
