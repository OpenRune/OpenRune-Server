package org.rsmod.content.skills.magic.arceuus

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class ArceuusLogicTest {
    @ParameterizedTest
    @CsvSource(
        "0,10000,100,100",
        "0,10000,40,40",
        "9000,10000,100,10",
        "9950,10000,100,1",
        "9999,10000,100,1",
        "10000,10000,100,0",
        "0,10000,0,0",
    )
    fun `vile vigour spends one prayer point per percent of run energy`(
        runEnergy: Int,
        maxEnergy: Int,
        prayerPoints: Int,
        expected: Int,
    ) {
        assertEquals(expected, VileVigour.prayerToSpend(runEnergy, maxEnergy, prayerPoints))
    }
}
