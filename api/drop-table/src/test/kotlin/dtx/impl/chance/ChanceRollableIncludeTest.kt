package dtx.impl.chance

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.Single
import dtx.core.flatten
import dtx.core.singleRollable
import dtx.rs.rsPrerollTable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChanceRollableIncludeTest {
    private val excluded =
        singleRollable<String, String> {
            shouldInclude { _, _ -> false }
            selectResult { _, _ -> RollResult.Single("excluded") }
        }

    @Test
    fun `a chance entry is left out when its rollable is not included`() {
        val table =
            rsPrerollTable<String, String> {
                100 outOf 100 rolls excluded
                100 outOf 100 rolls "always"
            }

        assertEquals(RollResult.Single("always"), table.roll("player", ArgMap.Empty).flatten())
    }

    @Test
    fun `chance rollables ask the rollable they wrap`() {
        val plain = ChanceRollableImpl(100.0, excluded)
        val boosted = RateBoostChanceRollable(1, 1, excluded)

        assertFalse(plain.includeInRoll("player", ArgMap.Empty))
        assertFalse(boosted.includeInRoll("player", ArgMap.Empty))
        assertTrue(ChanceRollableImpl(100.0, Single<String, String>("x")).includeInRoll("player", ArgMap.Empty))
    }
}
