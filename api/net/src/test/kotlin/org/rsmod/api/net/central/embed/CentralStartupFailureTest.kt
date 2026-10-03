package org.rsmod.api.net.central.embed

import java.net.BindException
import java.sql.SQLException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class CentralStartupFailureTest {
    @ParameterizedTest
    @ValueSource(strings = ["42P01", "42703", "3F000", "08001"])
    fun `schema and connection errors stop startup once and preserve the original cause`(state: String) {
        val cause = SQLException("An existing database may need a migration", state)
        val failure = IllegalStateException("Central startup failed", cause)
        var starts = 0
        var stops = 0

        val thrown = assertThrows(IllegalStateException::class.java) {
            startCentralPreservingDatabase(
                start = { starts++; throw failure },
                stop = { stops++ },
            )
        }

        assertSame(failure, thrown.cause)
        assertEquals(1, starts)
        assertEquals(1, stops)
        assertTrue(thrown.message!!.contains("No automatic database reset was attempted"))
    }

    @Test
    fun `cleanup failure does not mask a startup port conflict`() {
        val failure = BindException("Address already in use")
        val cleanupFailure = IllegalStateException("Stop failed")

        val thrown = assertThrows(IllegalStateException::class.java) {
            startCentralPreservingDatabase(
                start = { throw failure },
                stop = { throw cleanupFailure },
            )
        }

        assertSame(failure, thrown.cause)
        assertSame(cleanupFailure, failure.suppressed.single())
    }

    @Test
    fun `successful startup leaves central running`() {
        var starts = 0
        var stops = 0

        startCentralPreservingDatabase(start = { starts++ }, stop = { stops++ })

        assertEquals(1, starts)
        assertEquals(0, stops)
    }
}
