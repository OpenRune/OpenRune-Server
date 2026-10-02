package org.rsmod.api.combat.formulas

import java.util.EnumSet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.rsmod.api.combat.formulas.accuracy.magic.MagicAccuracyOperations
import org.rsmod.api.combat.formulas.attributes.CombatNpcAttributes
import org.rsmod.api.combat.formulas.attributes.CombatSpellAttributes
import org.rsmod.api.combat.formulas.attributes.DamageReductionAttributes
import org.rsmod.api.combat.formulas.maxhit.MaxHitOperations
import org.rsmod.api.combat.formulas.maxhit.magic.MagicMaxHitOperations

private fun spell(vararg attrs: CombatSpellAttributes): EnumSet<CombatSpellAttributes> =
    EnumSet.noneOf(CombatSpellAttributes::class.java).apply { addAll(attrs) }

private fun npc(vararg attrs: CombatNpcAttributes): EnumSet<CombatNpcAttributes> =
    EnumSet.noneOf(CombatNpcAttributes::class.java).apply { addAll(attrs) }

private fun reductions(vararg attrs: DamageReductionAttributes) =
    EnumSet.noneOf(DamageReductionAttributes::class.java).apply { addAll(attrs) }

internal class ArceuusFormulaTest {
    @Nested
    inner class DemonbaneAccuracy {
        private val roll = 1000

        private fun attackRoll(
            spell: EnumSet<CombatSpellAttributes>,
            npc: EnumSet<CombatNpcAttributes>,
        ): Int = MagicAccuracyOperations.modifySpellAttackRoll(roll, 0, spell, npc)

        @Test
        fun `demonbane gives twenty percent against demons`() {
            val spell = spell(CombatSpellAttributes.Demonbane)
            assertEquals(1200, attackRoll(spell, npc(CombatNpcAttributes.Demon)))
        }

        @Test
        fun `mark of darkness doubles the demonbane bonus`() {
            val spell = spell(CombatSpellAttributes.Demonbane, CombatSpellAttributes.MarkOfDarkness)
            assertEquals(1400, attackRoll(spell, npc(CombatNpcAttributes.Demon)))
        }

        @Test
        fun `demonbane resistant demons get the reduced bonus`() {
            val demon = npc(CombatNpcAttributes.Demon, CombatNpcAttributes.DemonbaneResistance)
            val plain = spell(CombatSpellAttributes.Demonbane)
            val marked = spell(CombatSpellAttributes.Demonbane, CombatSpellAttributes.MarkOfDarkness)
            assertEquals(1140, attackRoll(plain, demon))
            assertEquals(1280, attackRoll(marked, demon))
        }

        @Test
        fun `purging staff doubles the marked bonus`() {
            val spell =
                spell(
                    CombatSpellAttributes.Demonbane,
                    CombatSpellAttributes.MarkOfDarkness,
                    CombatSpellAttributes.PurgingStaff,
                )
            assertEquals(1800, attackRoll(spell, npc(CombatNpcAttributes.Demon)))
        }

        @Test
        fun `non demons get no bonus`() {
            val spell = spell(CombatSpellAttributes.Demonbane, CombatSpellAttributes.MarkOfDarkness)
            assertEquals(roll, attackRoll(spell, npc()))
        }

        @Test
        fun `mark of darkness alone gives no bonus without demonbane`() {
            val spell = spell(CombatSpellAttributes.MarkOfDarkness)
            assertEquals(roll, attackRoll(spell, npc(CombatNpcAttributes.Demon)))
        }
    }

    @Nested
    inner class DemonbaneDamage {
        private fun range(
            spell: EnumSet<CombatSpellAttributes>,
            npc: EnumSet<CombatNpcAttributes>,
        ): IntRange =
            MagicMaxHitOperations.modifySpellDamageRange(
                modifiedDamage = 100,
                baseDamage = 100,
                attackRate = 5,
                targetWeaknessPercent = 0,
                spellAttributes = spell,
                npcAttributes = npc,
            )

        private val demon = npc(CombatNpcAttributes.Demon)

        @Test
        fun `demonbane without the mark adds no damage`() {
            assertEquals(0..100, range(spell(CombatSpellAttributes.Demonbane), demon))
        }

        @Test
        fun `mark of darkness adds twenty five percent damage against demons`() {
            val spell = spell(CombatSpellAttributes.Demonbane, CombatSpellAttributes.MarkOfDarkness)
            assertEquals(0..125, range(spell, demon))
        }

        @Test
        fun `purging staff doubles the marked damage bonus`() {
            val spell =
                spell(
                    CombatSpellAttributes.Demonbane,
                    CombatSpellAttributes.MarkOfDarkness,
                    CombatSpellAttributes.PurgingStaff,
                )
            assertEquals(0..150, range(spell, demon))
        }

        @Test
        fun `mark of darkness adds no damage against non demons`() {
            val spell = spell(CombatSpellAttributes.Demonbane, CombatSpellAttributes.MarkOfDarkness)
            assertEquals(0..100, range(spell, npc()))
        }

        @Test
        fun `mark of darkness adds no damage to non demonbane spells`() {
            assertEquals(0..100, range(spell(CombatSpellAttributes.MarkOfDarkness), demon))
        }
    }

    @Nested
    inner class WardOfArceuus {
        private fun reduced(damage: Int, vararg attrs: DamageReductionAttributes): Int =
            MaxHitOperations.applyDamageReductions(damage, null, reductions(*attrs))

        @ParameterizedTest
        @CsvSource("0,0", "1,1", "9,9", "10,9", "19,18", "20,18", "29,27", "30,27", "100,90")
        fun `ward removes a tenth of the hit rounded down`(damage: Int, expected: Int) {
            assertEquals(expected, reduced(damage, DamageReductionAttributes.WardOfArceuus))
        }

        @Test
        fun `hits are unchanged without the ward`() {
            assertEquals(100, reduced(100))
        }

        @Test
        fun `ward stacks with other reductions`() {
            val elysian = reduced(100, DamageReductionAttributes.ElysianProc)
            val both =
                reduced(
                    100,
                    DamageReductionAttributes.ElysianProc,
                    DamageReductionAttributes.WardOfArceuus,
                )
            assertEquals(75, elysian)
            assertEquals(68, both)
        }
    }
}
