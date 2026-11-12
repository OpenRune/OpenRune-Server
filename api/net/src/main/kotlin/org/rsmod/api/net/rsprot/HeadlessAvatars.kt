package org.rsmod.api.net.rsprot

import jakarta.inject.Inject
import net.rsprot.protocol.api.NetworkService
import net.rsprot.protocol.common.client.OldSchoolClientType
import net.rsprot.protocol.game.outgoing.info.Infos
import net.rsprot.protocol.game.outgoing.info.npcinfo.NpcInfoPacket
import net.rsprot.protocol.game.outgoing.info.util.BuildArea
import net.rsprot.protocol.game.outgoing.info.util.PacketResult
import net.rsprot.protocol.game.outgoing.info.util.getOrThrow
import net.rsprot.protocol.game.outgoing.info.util.isEmpty
import net.rsprot.protocol.game.outgoing.info.util.safeReleaseOrThrow
import net.rsprot.protocol.message.OutgoingGameMessage
import org.rsmod.game.client.ClientCycle
import org.rsmod.game.client.NoopClientCycle
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

/**
 * Gives a [Player] that has no network session a protocol avatar, so real clients render it while
 * the server never sends it anything. The player's `slotId` must be reserved and kept out of the
 * player list for as long as the avatar is attached.
 */
@OptIn(ExperimentalUnsignedTypes::class)
class HeadlessAvatars @Inject constructor(private val service: NetworkService<Player>) {
    fun attach(player: Player) {
        val infos = service.infoProtocols.alloc(player.slotId, OldSchoolClientType.DESKTOP)
        val cycle = HeadlessCycle(infos)
        player.clientCycle = cycle
        cycle.init(player)
    }

    fun detach(player: Player) {
        val cycle = player.clientCycle as? HeadlessCycle ?: return
        service.infoProtocols.dealloc(cycle.infos)
        player.clientCycle = NoopClientCycle
    }
}

private class HeadlessCycle(val infos: Infos) : ClientCycle {
    private val avatar = RspAvatarSync(infos)

    private var knownBuildArea: CoordGrid = CoordGrid.NULL

    fun init(player: Player) {
        avatar.updateCoords(player)
    }

    override fun update(player: Player) {
        avatar.updateMovement(player)
        syncBuildArea(player)
        avatar.updateExtendedInfo(player)
    }

    private fun syncBuildArea(player: Player) {
        val buildArea = player.buildArea
        if (buildArea == knownBuildArea || buildArea == CoordGrid.NULL) {
            return
        }
        val zone = ZoneKey.from(buildArea)
        infos.updateRootBuildArea(BuildArea(zone.x, zone.z))
        knownBuildArea = buildArea
    }

    override fun flush(player: Player) {
        release()
    }

    override fun release() {
        val infoPackets = infos.getPackets()
        val rootPackets = infoPackets.rootWorldInfoPackets
        releasePacketResult(rootPackets.worldEntityInfo)
        releasePacketResult(rootPackets.playerInfo)
        releaseNpcInfoPacket(rootPackets.npcInfo)
        for (world in infoPackets.activeWorlds) {
            releaseNpcInfoPacket(world.npcInfo)
        }
    }

    override fun forceSceneRebuild() {}

    private fun <T : OutgoingGameMessage> releasePacketResult(result: PacketResult<T>) {
        result.getOrThrow().safeRelease()
    }

    private fun releaseNpcInfoPacket(result: PacketResult<NpcInfoPacket>) {
        if (result.isEmpty()) {
            result.safeReleaseOrThrow()
            return
        }
        result.getOrThrow().safeRelease()
    }
}
