package dev.openrune.tables.consumables.potion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PotionDataTest {
    @Test
    fun `run energy potions restore their wiki percentage per dose`() {
        val expected =
            mapOf(
                PotionData.ENERGY_POTION to 15,
                PotionData.ENERGY_MIX to 15,
                PotionData.SUPER_ENERGY to 20,
                PotionData.SUPER_ENERGY_MIX to 20,
                PotionData.STAMINA_POTION to 20,
                PotionData.STAMINA_MIX to 20,
                PotionData.EXTREME_ENERGY_POTION to 40,
                PotionData.EXTENDED_STAMINA_POTION to 40,
            )
        expected.forEach { (potion, percent) ->
            assertEquals(percent, runEnergyPercent(potion.effect), potion.name)
        }
    }

    @Test
    fun `stamina potions last their wiki duration`() {
        assertEquals(minutesInTicks(2), effect("dbrow.effect_stamina").duration)
        assertEquals(minutesInTicks(4), effect("dbrow.effect_extended_stamina").duration)
    }

    private fun runEnergyPercent(row: String): Int {
        val effect = effect(row)
        return when (effect.kind) {
            "run_energy" -> effect.amount
            "compound" -> effect.effects.sumOf(::runEnergyPercent)
            else -> 0
        }
    }

    private fun effect(row: String): PotionEffectData =
        PotionEffectData.entries.single { it.row == row }

    private fun minutesInTicks(minutes: Int): Int = minutes * 100
}
