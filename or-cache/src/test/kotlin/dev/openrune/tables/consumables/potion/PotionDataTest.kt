package dev.openrune.tables.consumables.potion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

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

    @ParameterizedTest(name = "{0}")
    @CsvSource(
        "antifire, 1, 6, false",
        "extended antifire, 2, 12, false",
        "super antifire, 3, 3, true",
        "extended super antifire, 4, 6, true",
    )
    fun `each antifire item drinks as its own wiki potion`(
        name: String,
        family: Int,
        minutes: Int,
        fullProtection: Boolean,
    ) {
        val potion = PotionData.entries.single { "obj.4dose${family}antidragon" in it.items }
        val mix = PotionData.entries.single { "obj.brutal_2dose${family}antidragon" in it.items }
        val doses = (4 downTo 1).map { "obj.${it}dose${family}antidragon" }
        assertEquals(doses, potion.items)
        assertEquals(potion.effect, mix.effect)
        val effect = effect(potion.effect)
        assertEquals("dragonfire_protection", effect.kind)
        assertEquals(minutesInTicks(minutes), effect.duration)
        assertEquals(fullProtection, effect.fullProtection)
        assertEquals(name, potion.displayName.lowercase())
    }

    @Test
    fun `restore potions restore only the stats the wiki lists`() {
        val combat = listOf("stat.attack", "stat.strength", "stat.defence", "stat.ranged", "stat.magic")
        assertEquals(combat, effect(PotionData.RESTORE_POTION.effect).skills)
        assertEquals(PotionData.RESTORE_POTION.effect, PotionData.RESTORE_MIX.effect)

        listOf(PotionData.SUPER_RESTORE, PotionData.SUPER_RESTORE_MIX, PotionData.BLIGHTED_SUPER_RESTORE)
            .forEach { potion ->
                val restore = effect(potion.effect)
                assertEquals(emptyList<String>(), restore.skills, potion.name)
                assertEquals(listOf("stat.hitpoints"), restore.excludedSkills, potion.name)
                assertEquals(true, restore.restorePrayer, potion.name)
            }
    }

    @Test
    fun `barbarian mixes carry the food attack delay and potions none`() {
        PotionData.entries.forEach { potion ->
            assertEquals(potion.category == "barbarian_mix", potion.mix, potion.name)
            assertEquals(3, potion.drinkDelay, potion.name)
            assertEquals(if (potion.mix) 3 else 0, potion.combatDelay, potion.name)
        }
    }

    @Test
    fun `sanfew serum cures disease and gives fifteen minutes of disease immunity`() {
        val sanfew = effect(PotionData.SANFEW_SERUM.effect)
        assertEquals(true, sanfew.curesDisease)
        assertEquals(false, sanfew.stamina)
        assertEquals(minutesInTicks(15), sanfew.duration)
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
