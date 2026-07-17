package org.rsmod.api.net.rsprot

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Inject
import net.rsprot.protocol.api.NetworkService
import net.rsprot.protocol.common.RSProtConstants
import net.rsprot.protocol.common.client.OldSchoolClientType
import org.rsmod.api.account.character.main.CharacterAccountRepository
import org.rsmod.api.db.jdbc.GameDatabase
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.net.central.OpenRuneCentralWorldLink
import org.rsmod.api.net.central.logging.CentralActivityLogWriter
import org.rsmod.api.net.rsprot.player.SessionStart
import org.rsmod.api.player.events.interact.HeldDropEvents
import org.rsmod.api.registry.player.PlayerRegistry
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.script.onEvent
import org.rsmod.api.server.config.ServerConfig
import org.rsmod.game.GameUpdate
import org.rsmod.game.MapClock
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

@OptIn(ExperimentalUnsignedTypes::class, ExperimentalStdlibApi::class)
class NetworkScript
@Inject
constructor(
    private val mapClock: MapClock,
    private val service: NetworkService<Player>,
    private val regionReg: RegionRegistry,
    private val config: ServerConfig,
    private val openRuneCentral: OpenRuneCentralWorldLink,
    private val gameUpdate: GameUpdate,
    private val database: GameDatabase,
    private val characterRepository: CharacterAccountRepository,
    private val playerRegistry: PlayerRegistry,
    private val centralActivityLogWriter: CentralActivityLogWriter,
) : PluginScript() {
    private val logger = InlineLogger()

    private companion object {
        private const val DB_ONLINE_SESSION_TIMING_INFO_MS = 100L
        private const val CENTRAL_HEARTBEAT_INTERVAL_CYCLES = 50L
    }

    override fun ScriptContext.startup() {
        check(RSProtConstants.REVISION == config.revision) {
            "RSProt and ${config.name} have mismatching revision builds! " +
                "(${config.name}=${config.revision}, rsprot=${RSProtConstants.REVISION})"
        }
        onEvent<GameLifecycle.Startup> {
            centralActivityLogWriter.start()
            initService()
            if (openRuneCentral.isEnabled) {
                openRuneCentral.startInboundWatch()
            }
        }
        onEvent<GameLifecycle.Shutdown> {
            openRuneCentral.stopInboundWatch()
            centralActivityLogWriter.stop()
        }
        onEvent<HeldDropEvents.Drop> {
            centralActivityLogWriter.logItemDrop(player, type, obj)
        }
        onEvent<HeldDropEvents.Destroy> {
            centralActivityLogWriter.logItemDestroy(player, type, obj)
        }
        onEvent<GameLifecycle.UpdateInfo> {
            updateService()
            maybeHeartbeatCentralSessions()
        }
        onEvent<SessionStart> { startSession() }
        onEvent<SessionStateEvent.Delete> {
            closeSession()
            finalizeCentralSessionIfNeeded()
        }
        onEvent<SessionStateEvent.Login> { markDbOnlineSession() }
        onEvent<SessionStateEvent.Logout> { notifyCentralLogout() }
        onEvent<NpcStateEvents.Create> { createNpcAvatar(npc) }
        onEvent<NpcStateEvents.Delete> { deleteNpcAvatar(npc) }
    }

    private fun initService() {
        service.setCommunicationThread(Thread.currentThread())
    }

    private fun updateService() {
        service.infoProtocols.update()
        if (openRuneCentral.isEnabled) {
            openRuneCentral.drainInboundRevokesOnGameThread(playerRegistry, gameUpdate)
            openRuneCentral.drainInboundSocialOnGameThread(playerRegistry)
        }
    }

    private fun maybeHeartbeatCentralSessions() {
        if (!openRuneCentral.isEnabled) {
            return
        }
        if (mapClock.cycle % CENTRAL_HEARTBEAT_INTERVAL_CYCLES != 0L) {
            return
        }
        val tokens = ArrayList<ByteArray>()
        playerRegistry.forEachOnline { player ->
            player.openRuneCentralSessionToken?.let { tokens.add(it) }
        }
        openRuneCentral.heartbeatOnlineSessions(tokens)
        touchOnlineSessionHeartbeats()
    }

    private fun touchOnlineSessionHeartbeats() {
        try {
            database.withTransactionBlocking { connection ->
                playerRegistry.forEachOnline { player ->
                    if (player.characterId > 0) {
                        characterRepository.setOnlineSession(
                            connection,
                            player.characterId,
                            config.world,
                        )
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn(e) { "Could not refresh online-session heartbeats" }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun SessionStart.startSession() {
        val slot = player.slotId

        val infos = service.infoProtocols.alloc(slot, OldSchoolClientType.DESKTOP)

        val client = RspClient(session, infos) as Client<Any, Any>
        val cycle = RspCycle(session, infos, regionReg)

        player.client = client
        player.clientCycle = cycle

        cycle.init(player)
    }

    private fun SessionStateEvent.Delete.closeSession() {
        val client = player.client as? RspClient ?: return
        client.unregister(service, player)
    }

    private fun SessionStateEvent.Delete.finalizeCentralSessionIfNeeded() {
        finalizeCentralSession(player)
    }

    private fun SessionStateEvent.Login.markDbOnlineSession() {
        val startedAt = System.nanoTime()
        try {
            database.withTransactionBlocking { connection ->
                characterRepository.setOnlineSession(
                    connection,
                    player.characterId,
                    config.world,
                )
            }
        } catch (e: Exception) {
            logger.warn(e) { "Could not set DB online-session for characterId=${player.characterId}" }
        }
        val elapsedMs = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
        if (config.loginTimingLogs && elapsedMs >= DB_ONLINE_SESSION_TIMING_INFO_MS) {
            logger.info {
                "Login set-online-session user='${player.username}' characterId=${player.characterId} elapsed=${elapsedMs}ms"
            }
        }
        centralActivityLogWriter.logPlayerLogin(player)
    }

    private fun SessionStateEvent.Logout.notifyCentralLogout() {
        centralActivityLogWriter.logPlayerLogout(player)
        finalizeCentralSession(player)
    }

    private fun finalizeCentralSession(player: Player) {
        try {
            database.withTransactionBlocking { connection ->
                characterRepository.clearOnlineSession(connection, player.characterId)
            }
        } catch (e: Exception) {
            logger.warn(e) { "Could not clear DB online-session for characterId=${player.characterId}" }
        }
        val token = player.openRuneCentralSessionToken ?: return
        openRuneCentral.notifyLogout(token)
        token.fill(0)
        player.openRuneCentralSessionToken = null
    }

    private fun createNpcAvatar(npc: Npc) {
        val rspAvatar =
            service.npcAvatarFactory.alloc(
                index = npc.slotId,
                id = npc.id,
                level = npc.level,
                x = npc.x,
                z = npc.z,
                spawnCycle = mapClock.cycle,
                direction = npc.respawnDir.id,
            )
        npc.infoProtocol = RspNpcInfo(rspAvatar)
    }

    private fun deleteNpcAvatar(npc: Npc) {
        val infoProtocol = npc.avatar.infoProtocol
        if (infoProtocol is RspNpcInfo) {
            service.npcAvatarFactory.release(infoProtocol.rspAvatar)
        }
    }
}
