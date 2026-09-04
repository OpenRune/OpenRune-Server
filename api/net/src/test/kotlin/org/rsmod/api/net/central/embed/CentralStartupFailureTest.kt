package org.rsmod.api.net.central.embed

import java.net.BindException
import java.sql.SQLException
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Verifies [CentralStartupFailure.indicatesUninitializedSchema] only recognizes SQL errors that
 * mean Central's schema hasn't been created yet, not unrelated startup failures such as a port
 * already being bound - the bug the real fix in [CentralEmbeddedLifecycle] addresses (the
 * embedded database used to get wiped on any startup exception whatsoever).
 */
class CentralStartupFailureTest {
    @Test
    fun `port bind conflict does not indicate an uninitialized schema`() {
        val bindFailure = IllegalStateException("boom", BindException("Address already in use"))
        assertFalse(CentralStartupFailure.indicatesUninitializedSchema(bindFailure))
    }

    @Test
    fun `unrelated runtime exception does not indicate an uninitialized schema`() {
        assertFalse(
            CentralStartupFailure.indicatesUninitializedSchema(RuntimeException("something else broke")),
        )
    }

    @Test
    fun `undefined table sql error indicates an uninitialized schema`() {
        val sqlEx = SQLException("relation \"world\" does not exist", "42P01")
        val wrapped = RuntimeException("startup failed", sqlEx)
        assertTrue(CentralStartupFailure.indicatesUninitializedSchema(wrapped))
    }

    @Test
    fun `undefined column sql error indicates an uninitialized schema`() {
        val sqlEx = SQLException("column \"foo\" does not exist", "42703")
        assertTrue(CentralStartupFailure.indicatesUninitializedSchema(sqlEx))
    }

    @Test
    fun `invalid schema name sql error indicates an uninitialized schema`() {
        val sqlEx = SQLException("schema \"public\" does not exist", "3F000")
        assertTrue(CentralStartupFailure.indicatesUninitializedSchema(sqlEx))
    }

    @Test
    fun `unrelated sql error does not indicate an uninitialized schema`() {
        val sqlEx = SQLException("connection refused", "08001")
        assertFalse(CentralStartupFailure.indicatesUninitializedSchema(sqlEx))
    }
}
