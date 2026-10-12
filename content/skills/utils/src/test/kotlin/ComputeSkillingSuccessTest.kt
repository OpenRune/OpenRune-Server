import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ComputeSkillingSuccessTest {
    @ParameterizedTest(name = "level {0} succeeds {1} in 256")
    @CsvSource("1, 101", "99, 201", "50, 151")
    fun `chance interpolates from the low value to the high value`(level: Int, chance: Int) {
        // when
        val success = computeSkillingSuccess(LOW, HIGH, level)

        // then
        assertEquals(chance / 256.0, success, DELTA)
    }

    @Test
    fun `levels below one are treated as level one`() {
        // when
        val success = computeSkillingSuccess(LOW, HIGH, 0)

        // then
        assertEquals(computeSkillingSuccess(LOW, HIGH, 1), success, DELTA)
    }

    @Test
    fun `levels above 99 are treated as level 99`() {
        // when
        val success = computeSkillingSuccess(LOW, HIGH, 150)

        // then
        assertEquals(computeSkillingSuccess(LOW, HIGH, 99), success, DELTA)
    }

    @Test
    fun `chance is capped at one`() {
        // when
        val success = computeSkillingSuccess(300, 300, 50)

        // then
        assertEquals(1.0, success, DELTA)
    }

    @Test
    fun `chance never drops below zero`() {
        // when
        val success = computeSkillingSuccess(-500, -500, 50)

        // then
        assertEquals(0.0, success, DELTA)
    }

    private companion object {
        const val LOW = 100
        const val HIGH = 200
        const val DELTA = 1e-9
    }
}
