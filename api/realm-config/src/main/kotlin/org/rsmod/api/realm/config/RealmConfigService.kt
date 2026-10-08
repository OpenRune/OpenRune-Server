package org.rsmod.api.realm.config

import com.github.michaelbull.logging.InlineLogger
import dev.or2.sql.OpenRuneSql
import jakarta.inject.Inject
import org.rsmod.api.db.Database
import org.rsmod.api.realm.Realm
import org.rsmod.api.server.config.ServerConfig
import org.rsmod.game.world.WorldType
import org.rsmod.server.services.ListenerService

public class RealmConfigService
@Inject
constructor(
    private val realm: Realm,
    private val serverConfig: ServerConfig,
    private val loader: RealmConfigLoader,
    private val database: Database,
) : ListenerService {
    private val logger = InlineLogger()

    override suspend fun signalStartup() {
        val worldId = serverConfig.world
        logger.debug { "Loading world $worldId configuration from database..." }
        val config = loader.load(worldId)
        if (config == null) {
            throw IllegalStateException("World not found in database: world_id=$worldId")
        }
        realm.updateConfig(config)
        clearGhostOnlineSessions(worldId)
        logger.info { "Loaded world $worldId configuration: $config" }
    }

    /**
     * Clears stale `character_progress.online_*` rows for this JVM's world id after an unclean
     * shutdown. Runs here (not in [org.rsmod.api.db.jdbc.GameDatabaseService]) so [Realm.config] is
     * initialized and the configured [worldId][ServerConfig.world] is known.
     *
     * Once per mode served, since `character_progress` lives in a schema per world type.
     */
    private suspend fun clearGhostOnlineSessions(gameWorldId: Int) {
        val sql = OpenRuneSql.text("game/character/characters_clear_online_presence_on_world.sql")
        for (worldType in WorldType.supportedFrom(serverConfig.worldTypes)) {
            try {
                database.withSchemaTransaction(worldType.key) { connection ->
                    connection.prepareStatement(sql).use { ps ->
                        ps.setInt(1, gameWorldId)
                        ps.executeUpdate()
                    }
                }
                logger.debug {
                    "Cleared online-session markers for worldId=$gameWorldId, " +
                        "world type='${worldType.key}'."
                }
            } catch (e: Exception) {
                logger.warn(e) {
                    "Could not clear online-session markers for world type='${worldType.key}'."
                }
            }
        }
    }

    override suspend fun signalShutdown() {}

    override suspend fun startup() {}

    override suspend fun shutdown() {}
}
