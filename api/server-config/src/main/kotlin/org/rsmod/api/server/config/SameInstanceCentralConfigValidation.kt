package org.rsmod.api.server.config

import java.nio.file.Path

public object SameInstanceCentralConfigValidation {

    public fun validateAfterLoad(configFile: Path, config: ServerConfig) {
        val central = config.central ?: return
        if (!central.sameInstance) {
            return
        }
        if (central.postgres != null) {
            return
        }
        throw IllegalArgumentException(missingPostgresMessage(configFile))
    }

    public fun missingPostgresMessage(configFile: Path? = null): String {
        val header =
            if (configFile != null) {
                "Invalid game.yml (${configFile.toAbsolutePath().normalize()})\n\n"
            } else {
                "Invalid game.yml\n\n"
            }
        return header +
            buildString {
                appendLine(
                    "You enabled in-process OpenRune Central (`central.same-instance: true`) but the " +
                        "`central.postgres` block is missing.",
                )
                appendLine()
                appendLine(
                    "Central always needs a `postgres` section for its JDBC pool settings — at minimum " +
                        "`pool-size` — even when you use embedded PostgreSQL (blank `jdbc-url`).",
                )
                appendLine()
                appendLine("Add this under `central:` (see game.example.yml):")
                appendLine()
                appendLine("  postgres:")
                appendLine("    pool-size: 10")
                appendLine()
                appendLine(
                    "Omit `jdbc-url` or leave it blank to use embedded PostgreSQL (data defaults to `./.data/pgdata`). " +
                        "Set `jdbc-url` (and `user` / `password` if needed) to use an existing PostgreSQL server.",
                )
            }.trimEnd()
    }
}
