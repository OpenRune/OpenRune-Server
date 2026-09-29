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

class SpecValidatorTest {
    private val arena = area(spawnTile(-5, -5), spawnTile(5, 5))
    private val fixed = ProjectileConfig.fixed(startHeight = 50, endHeight = 30, delay = 30, travel = 60, angle = 10)

    @Test
    fun `leviathan-style tile hazard composition is valid`() {
        val landing =
            hit {
                target = playersOn(EachTile)
                damage(10..30).roll()
                type(Typeless)
                hazard()
            }
        val boulder =
            sequence(
                mapSpotanim("spotanim.boulder", EachTile, delay = 30),
                after(2, sequence(spawnLoc("loc.rubble", EachTile, angle = 3), landing)),
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
    fun `EachTile outside onTiles is reported`() {
        assertHasError(errorsFor(mapSpotanim("spotanim.x", EachTile)), "outside a Projectile.onImpact or OnTiles")
    }

    @Test
    fun `an onTiles set is resolved in the enclosing scope`() {
        val nested = onTiles(nearestFreeTiles(listOf(EachTile), arena, 0), resetAnim())
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
