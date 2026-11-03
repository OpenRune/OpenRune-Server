package org.rsmod.api.combat.formulas

import dev.openrune.ServerCacheManager
import dev.openrune.types.NpcServerType
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import java.nio.file.Files
import java.nio.file.Path
import java.util.EnumSet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.combat.formulas.accuracy.AccuracyOperations
import org.rsmod.api.combat.formulas.accuracy.magic.PvNMagicAccuracy
import org.rsmod.api.combat.formulas.accuracy.magic.PvPMagicAccuracy
import org.rsmod.api.combat.formulas.attributes.CombatNpcAttributes
import org.rsmod.api.combat.formulas.attributes.CombatSpellAttributes
import org.rsmod.api.combat.formulas.attributes.collector.CombatMagicAttributeCollector
import org.rsmod.api.combat.formulas.attributes.collector.CombatNpcAttributeCollector
import org.rsmod.game.entity.Player

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
internal class SpellAccuracyMultiplierTest {
    private val spellAttributes =
        mockk<CombatMagicAttributeCollector> {
            every { spellCollect(any(), any(), any(), any(), any()) } returns
                EnumSet.noneOf(CombatSpellAttributes::class.java)
        }
    private val npcAttributes =
        mockk<CombatNpcAttributeCollector> {
            every { collect(any(), any(), any(), any(), any()) } returns
                EnumSet.noneOf(CombatNpcAttributes::class.java)
        }

    private val pvp =
        spyk(PvPMagicAccuracy(mockk(), mockk(), mockk(), spellAttributes)) {
            every { computeSpellAttackRoll(any(), any()) } returns ATTACK_ROLL
            every { computeDefenceRoll(any<Player>()) } returns DEFENCE_ROLL
        }

    private val pvn =
        spyk(PvNMagicAccuracy(mockk(), mockk(), spellAttributes, npcAttributes)) {
            every { computeSpellAttackRoll(any(), any(), any(), any()) } returns ATTACK_ROLL
            every { computeDefenceRoll(any(), any(), any(), any(), any()) } returns DEFENCE_ROLL
        }

    private fun expected(attackRoll: Int): Int =
        AccuracyOperations.calculateHitChance(attackRoll, DEFENCE_ROLL)

    private fun pvpChance(multiplier: Double): Int =
        pvp.computeSpellHitChance(Player(), Player(), mockk(), null, false, multiplier)

    private fun pvnChance(multiplier: Double): Int {
        val target = mockk<NpcServerType>(relaxed = true)
        mockkStatic(INTERNAL_HELPERS)
        every { target.isSlayerTask(any()) } returns false
        return pvn.computeSpellHitChance(
            source = Player(),
            target = target,
            spell = mockk(),
            targetDefence = 1,
            targetCurrHp = 1,
            targetMaxHp = 1,
            targetMagic = 1,
            targetWeaknessPercent = 0,
            spellbook = null,
            usedSunfireRune = false,
            specialMultiplier = multiplier,
        )
    }

    @Test
    fun `a pvp multiplier of one leaves the hit chance alone`() {
        assertEquals(expected(ATTACK_ROLL), pvpChance(1.0))
    }

    @Test
    fun `a pvp multiplier scales the attack roll`() {
        assertEquals(expected(ATTACK_ROLL * 2), pvpChance(2.0))
        assertTrue(pvpChance(2.0) > pvpChance(1.0))
    }

    @Test
    fun `a pvn multiplier of one leaves the hit chance alone`() {
        assertEquals(expected(ATTACK_ROLL), pvnChance(1.0))
    }

    @Test
    fun `a pvn multiplier scales the attack roll`() {
        assertEquals(expected(ATTACK_ROLL * 2), pvnChance(2.0))
        assertTrue(pvnChance(2.0) > pvnChance(1.0))
    }

    private companion object {
        const val ATTACK_ROLL = 5_000
        const val DEFENCE_ROLL = 8_000
        const val INTERNAL_HELPERS = "org.rsmod.api.combat.formulas.InternalHelpersKt"

        @OptIn(InternalApi::class)
        @JvmStatic
        @BeforeAll
        fun cache() {
            val root =
                generateSequence(Path.of("").toAbsolutePath()) { it.parent }
                    .first { Files.isRegularFile(it.resolve("game.yml")) }
            val revision =
                Files.readAllLines(root.resolve("game.yml"))
                    .first { it.startsWith("revision:") }
                    .substringAfter(':')
                    .trim()
                    .substringBefore('.')
                    .toInt()
            ServerCacheManager.init(revision).close()
        }
    }
}
