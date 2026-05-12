package org.rsmod.api.realm.config

import com.github.michaelbull.logging.InlineLogger
import dev.or2.sql.OpenRuneSql
import jakarta.inject.Inject
import org.rsmod.api.db.Database
import org.rsmod.api.realm.Realm
import org.rsmod.api.server.config.ServerConfig
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
        clearGhostOnlineSessions(config.id, worldId)
        logger.info { "Loaded world $worldId configuration: $config" }
    }

    /**
     * Clears stale `account_characters.online_*` rows for this JVM's world id after an unclean shutdown.
     * Runs here (not in [org.rsmod.api.db.jdbc.GameDatabaseService]) so [Realm.config] is initialized
     * and [realm_id] is known from the loaded row.
     */
    private suspend fun clearGhostOnlineSessions(realmId: Int, gameWorldId: Int) {
        try {
            database.withTransaction { connection ->
                val sql = OpenRuneSql.text("game/character/characters_clear_online_presence_on_world.sql")
                connection.prepareStatement(sql).use { ps ->
                    ps.setInt(1, realmId)
                    ps.setInt(2, gameWorldId)
                    ps.executeUpdate()
                }
            }
            logger.info {
                "Cleared character online-session markers for worldId=$gameWorldId (realmId=$realmId)."
            }
        } catch (e: Exception) {
            logger.warn(e) { "Could not clear online-session markers after realm load." }
        }
    }

    override suspend fun signalShutdown() {}

    override suspend fun startup() {}

    override suspend fun shutdown() {}
}
