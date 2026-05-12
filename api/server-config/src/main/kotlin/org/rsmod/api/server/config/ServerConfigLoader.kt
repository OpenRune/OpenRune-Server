package org.rsmod.api.server.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.michaelbull.logging.InlineLogger
import com.google.inject.Provides
import com.google.inject.Singleton
import com.google.inject.name.Named
import jakarta.inject.Inject
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.notExists
import kotlin.io.path.writeText
import org.rsmod.api.parsers.toml.Toml

public class ServerConfigLoader {

    private val yamlMapper: ObjectMapper =
        ObjectMapper(YAMLFactory())
            .registerKotlinModule()

    private val logger = InlineLogger()

    public fun loadOrCreate(file: Path): ServerConfig =
        if (file.exists()) load(file) else create(file)

    public fun load(file: Path): ServerConfig {
        val config = yamlMapper.readValue(file.toFile(), ServerConfig::class.java)
        SameInstanceCentralConfigValidation.validateAfterLoad(file, config)
        return config
    }

    public fun create(file: Path): ServerConfig {
        require(file.notExists()) { "File already exists: ${file.toAbsolutePath()}" }

        val config = createDefault()
        val contents = yamlMapper.writeValueAsString(config)

        file.writeText(contents)

        logger.info { "Created default server config in file: $file" }
        return config
    }

    private fun createDefault(): ServerConfig =
        ServerConfig(
            name = "OpenRune",
            gamePort = 43594,
            revision = 233,
            environment = "LIVE",
            world = DEFAULT_WORLD,
            database =
                GameDatabaseYaml(
                    postgres =
                        PostgresDbYaml(
                            jdbcUrl = "jdbc:postgresql://127.0.0.1:5432/openrune_game",
                            user = "openrune",
                            password = "openrune",
                        ),
                ),
            central = null,
        )

    private companion object {
        private const val DEFAULT_WORLD = 1
    }
}
