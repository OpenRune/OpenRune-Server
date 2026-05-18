package org.rsmod.api.server.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.michaelbull.logging.InlineLogger
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.notExists
import kotlin.io.path.writeText

/**
 * When [OpenRuneCentralGameConfig.sameInstance] is enabled, upgrades a legacy `world: 1` in
 * `game.yml` to match [game.example.yml] before [ServerConfig] is loaded.
 */
internal object SameInstanceCentralWorldMigrator {
    private val logger = InlineLogger()

    fun migrateIfNeeded(
        gameYml: Path,
        exampleYml: Path,
        yamlMapper: ObjectMapper,
    ) {
        if (gameYml.notExists() || exampleYml.notExists()) {
            return
        }
        val game =
            runCatching { yamlMapper.readValue(gameYml.toFile(), ServerConfig::class.java) }
                .getOrElse { return }
        val central = game.central ?: return
        if (!central.sameInstance) {
            return
        }
        if (game.world != LEGACY_DEFAULT_WORLD) {
            return
        }
        val example =
            runCatching { yamlMapper.readValue(exampleYml.toFile(), ServerConfig::class.java) }
                .getOrElse { return }
        val targetWorld = example.world
        if (targetWorld == game.world) {
            return
        }
        val updated = game.copy(world = targetWorld)
        gameYml.writeText(yamlMapper.writeValueAsString(updated))
        logger.info {
            "Migrated game.yml `world` from $LEGACY_DEFAULT_WORLD to $targetWorld " +
                "(same-instance Central; aligned with ${exampleYml.fileName})."
        }
    }

    private const val LEGACY_DEFAULT_WORLD = 1
}
