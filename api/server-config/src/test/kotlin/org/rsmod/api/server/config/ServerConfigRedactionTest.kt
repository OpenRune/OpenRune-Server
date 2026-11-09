package org.rsmod.api.server.config

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ServerConfigRedactionTest {
    @Test
    fun `central config toString hides the postgres password and world key`() {
        val central =
            OpenRuneCentralGameConfig(
                worldKey = "world-key-secret",
                postgres =
                    CentralPostgresYaml(
                        jdbcUrl = "jdbc:postgresql://127.0.0.1:5432/openrune_central",
                        password = "central-pg-secret",
                    ),
            )

        val text = central.toString()

        assertFalse("world-key-secret" in text, text)
        assertFalse("central-pg-secret" in text, text)
        assertTrue("password=***" in text, text)
        assertTrue("worldKey=***" in text, text)
        assertTrue("jdbcUrl=jdbc:postgresql://127.0.0.1:5432/openrune_central" in text, text)
    }

    @Test
    fun `game database config toString hides the postgres password`() {
        val text = PostgresDbYaml(password = "game-pg-secret").toString()

        assertFalse("game-pg-secret" in text, text)
        assertTrue("password=***" in text, text)
    }

    @Test
    fun `a password embedded in the jdbc url is hidden`() {
        val text =
            PostgresDbYaml(jdbcUrl = "jdbc:postgresql://db:5432/game?user=game&password=url-secret&ssl=false")
                .toString()

        assertFalse("url-secret" in text, text)
        assertTrue("jdbc:postgresql://db:5432/game?user=game&password=***&ssl=false" in text, text)
    }

    @Test
    fun `unset secrets stay visibly empty`() {
        assertTrue("worldKey=," in OpenRuneCentralGameConfig().toString())
    }
}
