package org.rsmod.api.net.central.logging

import com.github.michaelbull.logging.InlineLogger
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.or2.central.logs.CentralActivityLog
import dev.openrune.types.ItemServerType
import dev.or2.central.server.logging.CentralActivityLogRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.min
import org.rsmod.api.db.jdbc.EmbeddedSameInstancePostgres
import org.rsmod.api.server.config.ServerConfig
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj

@Singleton
public class CentralActivityLogWriter
@Inject
constructor(
    private val config: ServerConfig,
) {
    private val logger = InlineLogger()
    private val executor =
        Executors.newSingleThreadExecutor { r ->
            Thread(r, "central-activity-log-writer").apply { isDaemon = true }
        }

    @Volatile
    private var repository: CentralActivityLogRepository? = null

    private var dataSource: HikariDataSource? = null

    public fun start() {
        synchronized(this) {
            if (repository != null) {
                return
            }
            val (jdbc, user, password, poolSize) = resolveCentralJdbc(config) ?: return
            val ds =
                HikariDataSource(
                    HikariConfig().apply {
                        this.jdbcUrl = jdbc
                        username = user
                        this.password = password
                        maximumPoolSize = min(4, poolSize)
                        poolName = "openrune-central-activity-log"
                    },
                )
            dataSource = ds
            repository = CentralActivityLogRepository(ds)
            logger.info { "Central activity log JDBC enabled ($jdbc)" }
        }
    }

    public fun stop() {
        executor.shutdown()
        try {
            if (!executor.awaitTermination(15, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (_: InterruptedException) {
            executor.shutdownNow()
            Thread.currentThread().interrupt()
        }
        synchronized(this) {
            repository = null
            dataSource?.close()
            dataSource = null
        }
    }

    public fun logItemDrop(
        player: Player,
        type: ItemServerType,
        obj: InvObj,
    ) {
        logItem(
            player = player,
            itemId = type.internalName,
            quantity = obj.count,
            log =
                { worldId, now, charId, accountId, itemId, qty, x, z ->
                    CentralActivityLog.DroppedItem(
                        worldId = worldId,
                        occurredAtEpochMillis = now,
                        characterId = charId,
                        accountId = accountId,
                        itemId = itemId,
                        quantity = qty,
                        tileX = x,
                        tileZ = z,
                    )
                },
            failureLabel = "drop",
        )
    }

    /**
     * @param itemIdRscm RSCM object key from the destroy flow (same as [ItemServerType.internalName]).
     */
    public fun logItemDestroy(
        player: Player,
        itemIdRscm: String,
        obj: InvObj,
    ) {
        logItem(
            player = player,
            itemId = itemIdRscm,
            quantity = obj.count,
            log =
                { worldId, now, charId, accountId, itemId, qty, x, z ->
                    CentralActivityLog.DestroyItem(
                        worldId = worldId,
                        occurredAtEpochMillis = now,
                        characterId = charId,
                        accountId = accountId,
                        itemId = itemId,
                        quantity = qty,
                        tileX = x,
                        tileZ = z,
                    )
                },
            failureLabel = "destroy",
        )
    }

    public fun logCommand(
        player: Player,
        command: String,
        args: List<String>,
    ) {
        val repo = repository ?: return
        val worldId = config.world
        val charId = player.characterId
        val now = System.currentTimeMillis()
        val accountId = player.accountId.toLong()
        val argsCopy = args.toList()
        executor.execute {
            try {
                repo.insert(
                    CentralActivityLog.Command(
                        worldId = worldId,
                        occurredAtEpochMillis = now,
                        characterId = charId,
                        accountId = accountId,
                        command = command,
                        args = argsCopy,
                    ),
                )
            } catch (e: Exception) {
                logger.warn(e) {
                    "Central activity log command failed accountId=$accountId characterId=$charId cmd=$command"
                }
            }
        }
    }

    /**
     * Persists a `login` row once the player is in-world with [Player.characterId] / [Player.accountId] set.
     * (Central worlds-link no longer writes login logs.)
     */
    public fun logPlayerLogin(player: Player) {
        val repo = repository ?: return
        val worldId = config.world
        val now = System.currentTimeMillis()
        val charId = player.characterId
        val accountId = player.accountId.toLong()
        executor.execute {
            try {
                repo.insert(
                    CentralActivityLog.Login(
                        worldId = worldId,
                        occurredAtEpochMillis = now,
                        characterId = charId,
                        accountId = accountId,
                    ),
                )
            } catch (e: Exception) {
                logger.warn(e) {
                    "Central activity log login failed accountId=$accountId characterId=$charId"
                }
            }
        }
    }

    /**
     * Persists a `logout` row. [centralSessionId] is optional (Central DB session row id when known).
     */
    public fun logPlayerLogout(
        player: Player,
        centralSessionId: Long? = null,
    ) {
        val repo = repository ?: return
        val worldId = config.world
        val now = System.currentTimeMillis()
        val charId = player.characterId
        val accountId = player.accountId.toLong()
        executor.execute {
            try {
                repo.insert(
                    CentralActivityLog.Logout(
                        worldId = worldId,
                        occurredAtEpochMillis = now,
                        characterId = charId,
                        accountId = accountId,
                        sessionId = centralSessionId,
                    ),
                )
            } catch (e: Exception) {
                logger.warn(e) {
                    "Central activity log logout failed accountId=$accountId characterId=$charId"
                }
            }
        }
    }

    private fun logItem(
        player: Player,
        itemId: String,
        quantity: Int,
        log:
            (
                worldId: Int,
                now: Long,
                charId: Int,
                accountId: Long,
                itemId: String,
                qty: Int,
                x: Int,
                z: Int,
            ) -> CentralActivityLog,
        failureLabel: String,
    ) {
        val repo = repository ?: return
        val worldId = config.world
        val charId = player.characterId
        val accountId = player.accountId.toLong()
        val now = System.currentTimeMillis()
        val x = player.x
        val z = player.z
        executor.execute {
            try {
                repo.insert(log(worldId, now, charId, accountId, itemId, quantity, x, z))
            } catch (e: Exception) {
                logger.warn(e) {
                    "Central activity log $failureLabel failed accountId=$accountId characterId=$charId itemId=$itemId qty=$quantity"
                }
            }
        }
    }

    private companion object {
        fun resolveCentralJdbc(config: ServerConfig): JdbcParams? {
            val central = config.central ?: return null
            val pg = central.postgres ?: return null
            val jdbcFromYaml = pg.jdbcUrl.trim()
            val triple =
                if (jdbcFromYaml.isNotEmpty()) {
                    Triple(
                        jdbcFromYaml,
                        pg.user.trim().ifBlank { "openrune" },
                        pg.password,
                    )
                } else if (central.sameInstance) {
                    EmbeddedSameInstancePostgres.jdbcTripleIfEmbedded() ?: return null
                } else {
                    return null
                }
            return JdbcParams(triple.first, triple.second, triple.third, pg.poolSize)
        }
    }

    private data class JdbcParams(
        val jdbcUrl: String,
        val user: String,
        val password: String,
        val poolSize: Int,
    )
}
