package org.rsmod.content.bosses.brutus

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.db.DatabaseConnection
import org.rsmod.api.db.gateway.GameDbManager
import org.rsmod.api.db.gateway.model.GameDbResult
import org.rsmod.api.db.gateway.model.isOk

/**
 * The scoreboard's server-wide kills, deaths and best time. They are kept in the database so they
 * survive restarts; each write returns the stored totals, which replace the cached copy.
 */
@Singleton
class BrutusWorldStats @Inject constructor(private val db: GameDbManager) {
    private val logger = InlineLogger()

    var kills: Long = 0
        private set

    var deaths: Long = 0
        private set

    /** The fastest kill in ticks, or 0 before any timed kill. */
    var bestTicks: Int = 0
        private set

    fun load() = add()

    fun addKill() = add(kills = 1)

    fun addDeath() = add(deaths = 1)

    fun addKillTime(ticks: Int) = add(bestTicks = ticks)

    private fun add(kills: Int = 0, deaths: Int = 0, bestTicks: Int = 0) {
        db.request(
            request = { connection ->
                GameDbResult.Ok(upsert(connection, kills, deaths, bestTicks))
            },
            response = { result ->
                if (result.isOk()) {
                    val (storedKills, storedDeaths, storedBest) = result.value
                    this.kills = storedKills
                    this.deaths = storedDeaths
                    this.bestTicks = storedBest
                } else {
                    logger.warn { "Could not update Brutus' scoreboard: $result" }
                }
            },
        )
    }

    private fun upsert(
        connection: DatabaseConnection,
        kills: Int,
        deaths: Int,
        bestTicks: Int,
    ): Triple<Long, Long, Int> {
        connection.prepareStatement(CREATE_TABLE).use { it.executeUpdate() }
        return connection.prepareStatement(UPSERT).use { statement ->
            statement.setString(1, KEY)
            statement.setLong(2, kills.toLong())
            statement.setLong(3, deaths.toLong())
            statement.setInt(4, bestTicks)
            statement.executeQuery().use { rs ->
                rs.next()
                Triple(rs.getLong(1), rs.getLong(2), rs.getInt(3))
            }
        }
    }

    private companion object {
        private const val KEY = "cowboss"

        private const val CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS boss_world_stats (" +
                "boss TEXT PRIMARY KEY, " +
                "kills BIGINT NOT NULL DEFAULT 0, " +
                "deaths BIGINT NOT NULL DEFAULT 0, " +
                "best_ticks INT NOT NULL DEFAULT 0)"

        private const val UPSERT =
            "INSERT INTO boss_world_stats AS s (boss, kills, deaths, best_ticks) " +
                "VALUES (?, ?, ?, ?) ON CONFLICT (boss) DO UPDATE SET " +
                "kills = s.kills + EXCLUDED.kills, " +
                "deaths = s.deaths + EXCLUDED.deaths, " +
                "best_ticks = CASE WHEN EXCLUDED.best_ticks > 0 AND " +
                "(s.best_ticks = 0 OR EXCLUDED.best_ticks < s.best_ticks) " +
                "THEN EXCLUDED.best_ticks ELSE s.best_ticks END " +
                "RETURNING kills, deaths, best_ticks"
    }
}
