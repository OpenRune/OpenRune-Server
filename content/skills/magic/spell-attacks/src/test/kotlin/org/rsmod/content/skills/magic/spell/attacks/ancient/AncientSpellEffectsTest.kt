package org.rsmod.content.skills.magic.spell.attacks.ancient

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.rsmod.annotations.InternalApi
import org.rsmod.api.combat.commons.CombatEffects
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AncientSpellEffectsTest {
    @BeforeAll
    fun loadCache() {
        TestCache.load()
    }

    private val landing = FixedRandom(0)
    private val missing = FixedRandom(Int.MAX_VALUE)

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Smoke {
        @ParameterizedTest
        @CsvSource(
            "SmokeRush, false, 10",
            "SmokeBurst, false, 10",
            "SmokeBlitz, false, 20",
            "SmokeBarrage, false, 20",
            "SmokeRush, true, 11",
            "SmokeBarrage, true, 21",
        )
        fun `poisons npc with wiki severity`(spell: AncientSpell, sceptre: Boolean, severity: Int) {
            val target = npc("npc.man")
            AncientSpellEffects(landing).apply(spell, player(), target, damage = 5, sceptre)
            // The first poison hit is queued immediately, consuming one point of severity.
            assertEquals(severity - 1, target.vars["varn.poison_severity"])
        }

        @Test
        fun `does not poison when the 1 in 8 roll fails`() {
            val target = npc("npc.man")
            AncientSpellEffects(missing).apply(AncientSpell.SmokeBarrage, player(), target, 5, false)
            assertEquals(0, target.vars["varn.poison_severity"])
        }

        @Test
        fun `rolls poison out of 8`() {
            var bound = -1
            val spy =
                object : org.rsmod.api.random.GameRandom by landing {
                    override fun of(maxExclusive: Int): Int {
                        bound = maxExclusive
                        return 1
                    }
                }
            AncientSpellEffects(spy).apply(AncientSpell.SmokeRush, player(), npc("npc.man"), 5, false)
            assertEquals(8, bound)
        }

        @Test
        fun `does not poison poison-immune npcs`() {
            val target = npc("npc.cow")
            AncientSpellEffects(landing).apply(AncientSpell.SmokeBarrage, player(), target, 5, false)
            assertEquals(0, target.vars["varn.poison_severity"])
        }

        @Test
        fun `poisons players`() {
            val target = player()
            AncientSpellEffects(landing).apply(AncientSpell.SmokeBlitz, player(), target, 5, false)
            assertEquals(19, target.vars["varp.poison_severity"])
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Shadow {
        @ParameterizedTest
        @CsvSource(
            "ShadowRush, false, 90",
            "ShadowBurst, false, 90",
            "ShadowBlitz, false, 85",
            "ShadowBarrage, false, 85",
            "ShadowRush, true, 89",
            "ShadowBarrage, true, 84",
        )
        fun `drains npc attack`(spell: AncientSpell, sceptre: Boolean, expected: Int) {
            val target = npcWithAttack(100)
            AncientSpellEffects(landing).apply(spell, player(), target, 5, sceptre)
            assertEquals(expected, target.attackLvl)
        }

        @Test
        fun `does not stack on an already drained npc`() {
            val target = npcWithAttack(100)
            val effects = AncientSpellEffects(landing)
            effects.apply(AncientSpell.ShadowBarrage, player(), target, 5, false)
            effects.apply(AncientSpell.ShadowBarrage, player(), target, 5, false)
            assertEquals(85, target.attackLvl)
        }

        @Test
        fun `drains player attack`() {
            val target = player().apply { setLevel("stat.attack", 99) }
            AncientSpellEffects(landing).apply(AncientSpell.ShadowRush, player(), target, 5, false)
            assertEquals(90, target.stat("stat.attack"))
        }

        @Test
        fun `does not drain an already drained player`() {
            val target = player().apply { setLevel("stat.attack", 99, current = 95) }
            AncientSpellEffects(landing).apply(AncientSpell.ShadowBlitz, player(), target, 5, false)
            assertEquals(95, target.stat("stat.attack"))
        }

        private fun npcWithAttack(level: Int): Npc =
            npc("npc.man").apply {
                baseAttackLvl = level
                attackLvl = level
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Blood {
        @ParameterizedTest
        @CsvSource(
            "BloodRush, false, 20, 55",
            "BloodBurst, false, 21, 55",
            "BloodBlitz, false, 25, 56",
            "BloodBarrage, false, 29, 57",
            "BloodBarrage, true, 40, 61",
            "BloodRush, false, 3, 50",
        )
        fun `heals caster a quarter of damage dealt`(
            spell: AncientSpell,
            sceptre: Boolean,
            damage: Int,
            expectedHp: Int,
        ) {
            val caster = player().apply { setLevel("stat.hitpoints", 99, current = 50) }
            AncientSpellEffects(landing).apply(spell, caster, npc("npc.man"), damage, sceptre)
            assertEquals(expectedHp, caster.stat("stat.hitpoints"))
        }

        @Test
        fun `does not heal above max hitpoints`() {
            val caster = player().apply { setLevel("stat.hitpoints", 99, current = 97) }
            AncientSpellEffects(landing).apply(AncientSpell.BloodBarrage, caster, npc("npc.man"), 29, false)
            assertEquals(99, caster.stat("stat.hitpoints"))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Ice {
        @ParameterizedTest
        @CsvSource(
            "IceRush, false, 8",
            "IceBurst, false, 16",
            "IceBlitz, false, 24",
            "IceBarrage, false, 32",
            "IceBarrage, true, 35",
        )
        fun `freezes npc for wiki duration`(spell: AncientSpell, sceptre: Boolean, ticks: Int) {
            val target = npc("npc.man", clock = 100)
            AncientSpellEffects(landing).apply(spell, player(), target, 5, sceptre)
            target.currentMapClock = 100 + ticks - 1
            assertTrue(CombatEffects.isFrozen(target), "frozen on last tick")
            target.currentMapClock = 100 + ticks
            assertFalse(CombatEffects.isFrozen(target), "thawed after duration")
        }

        @Test
        fun `npc is immune to refreezing for 5 ticks after thawing`() {
            val target = npc("npc.man", clock = 100)
            val effects = AncientSpellEffects(landing)
            effects.apply(AncientSpell.IceRush, player(), target, 5, false)

            target.currentMapClock = 110
            effects.apply(AncientSpell.IceRush, player(), target, 5, false)
            assertFalse(CombatEffects.isFrozen(target))

            target.currentMapClock = 113
            effects.apply(AncientSpell.IceRush, player(), target, 5, false)
            assertTrue(CombatEffects.isFrozen(target))
        }

        @Test
        fun `freeze-resistant npcs are not frozen`() {
            val target = npc("npc.gemstone_crab")
            AncientSpellEffects(landing).apply(AncientSpell.IceBarrage, player(), target, 5, false)
            assertFalse(CombatEffects.isFrozen(target))
        }

        @Test
        fun `guaranteed freeze bypasses resistance`() {
            val target = npc("npc.gemstone_crab")
            val effects = AncientSpellEffects(landing)
            effects.apply(AncientSpell.IceBarrage, player(), target, 5, false, guaranteedFreeze = true)
            assertTrue(CombatEffects.isFrozen(target))
        }

        @Test
        fun `guaranteed freeze bypasses post-thaw immunity`() {
            val target = npc("npc.man", clock = 100)
            val effects = AncientSpellEffects(landing)
            effects.apply(AncientSpell.IceRush, player(), target, 5, false)

            target.currentMapClock = 110
            effects.apply(AncientSpell.IceRush, player(), target, 5, false, guaranteedFreeze = true)
            assertTrue(CombatEffects.isFrozen(target))
        }

        @Test
        fun `guaranteed freeze is claimed once and only by ice spells`() {
            val target = npc("npc.man")
            target.vars["varn.freeze_guaranteed"] = 1
            val effects = AncientSpellEffects(landing)
            assertFalse(effects.claimGuaranteedFreeze(AncientSpell.SmokeBarrage, target))
            assertTrue(effects.claimGuaranteedFreeze(AncientSpell.IceRush, target))
            assertFalse(effects.claimGuaranteedFreeze(AncientSpell.IceRush, target))
        }

        @Test
        fun `freezes players`() {
            val target = player()
            AncientSpellEffects(landing).apply(AncientSpell.IceBarrage, player(), target, 5, false)
            assertTrue(target.isFrozen)
            assertEquals(32, target.freezeTimerInterval())
        }

        @Test
        fun `protect from magic halves player freeze`() {
            val target = player()
            VarPlayerIntMapSetter.set(target, "varbit.prayer_protectfrommagic", 1)
            AncientSpellEffects(landing).apply(AncientSpell.IceBarrage, player(), target, 5, false)
            assertEquals(16, target.freezeTimerInterval())
        }

        @OptIn(InternalApi::class)
        private fun Player.freezeTimerInterval(): Int {
            val timer = "timer.combat_freeze".asRSCM(RSCMType.TIMER).toShort()
            return timerMap.extractInterval(checkNotNull(timerMap[timer]))
        }
    }

    @Nested
    inner class AreaOfEffect {
        private val centre = TestCoords

        @ParameterizedTest
        @CsvSource("-1, -1", "0, 0", "1, 1", "1, -1", "-1, 0")
        fun `size 1 targets inside the 3x3 are hit`(dx: Int, dz: Int) {
            assertTrue(AncientSpellEffects.overlapsAoe(centre, centre.translate(dx, dz), 1))
        }

        @ParameterizedTest
        @CsvSource("-2, 0", "2, 0", "0, 2", "2, 2", "0, -2")
        fun `size 1 targets outside the 3x3 are missed`(dx: Int, dz: Int) {
            assertFalse(AncientSpellEffects.overlapsAoe(centre, centre.translate(dx, dz), 1))
        }

        @Test
        fun `large npc whose south-west tile is outside but body overlaps is hit`() {
            assertTrue(AncientSpellEffects.overlapsAoe(centre, centre.translate(-3, -3), 3))
            assertFalse(AncientSpellEffects.overlapsAoe(centre, centre.translate(-4, -3), 3))
        }
    }
}
