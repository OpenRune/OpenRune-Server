package org.rsmod.api.net.rsprot

import dev.openrune.net.CacheJs5GroupProvider
import io.netty.buffer.Unpooled
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.nio.file.Paths
import net.rsprot.compression.HuffmanCodec
import net.rsprot.compression.provider.DefaultHuffmanCodecProvider
import net.rsprot.compression.provider.HuffmanCodecProvider
import net.rsprot.crypto.rsa.RsaKeyPair
import net.rsprot.protocol.api.AbstractNetworkServiceFactory
import net.rsprot.protocol.api.GameConnectionHandler
import net.rsprot.protocol.api.bootstrap.BootstrapBuilder
import net.rsprot.protocol.api.handlers.ExceptionHandlers
import net.rsprot.protocol.api.js5.Js5GroupProvider
import net.rsprot.protocol.api.suppliers.NpcInfoSupplier
import net.rsprot.protocol.api.suppliers.WorldEntityInfoSupplier
import net.rsprot.protocol.common.client.OldSchoolClientType
import net.rsprot.protocol.message.codec.incoming.provider.GameMessageConsumerRepositoryProvider
import org.rsmod.api.net.rsprot.provider.ExceptionHandlersProvider
import org.rsmod.api.net.rsprot.provider.Js5GroupResponseProvider
import org.rsmod.api.net.rsprot.provider.MessageConsumerProvider
import org.rsmod.api.net.rsprot.provider.NpcSupplier
import org.rsmod.api.net.rsprot.provider.RsaProvider
import org.rsmod.api.net.rsprot.provider.WorldEntityProvider
import org.rsmod.api.server.config.ServerConfig
import org.rsmod.game.entity.Player

@OptIn(ExperimentalUnsignedTypes::class)
@Singleton
class NetworkFactory
@Inject
constructor(
    private val messageConsumerProvider: MessageConsumerProvider,
    private val connectionHandler: ConnectionHandler,
    private val config: ServerConfig,
) : AbstractNetworkServiceFactory<Player>() {
    private val js5Groups = Js5GroupResponseProvider()
    private val npcSupplier = NpcSupplier.provide()

    override val ports: List<Int> = listOf(config.gamePort)

    override val supportedClientTypes: List<OldSchoolClientType> =
        listOf(OldSchoolClientType.DESKTOP)

    override fun getExceptionHandlers(): ExceptionHandlers<Player> {
        return ExceptionHandlersProvider.provide()
    }

    override fun getGameConnectionHandler(): GameConnectionHandler<Player> {
        return connectionHandler
    }

    override fun getGameMessageConsumerRepositoryProvider():
        GameMessageConsumerRepositoryProvider<Player> {
        return messageConsumerProvider.get()
    }

    override fun getHuffmanCodecProvider(): HuffmanCodecProvider {
        val huffman: HuffmanCodec =
            HuffmanCodec.create(Unpooled.wrappedBuffer(CacheJs5GroupProvider.huffmanData))
        return DefaultHuffmanCodecProvider(huffman)
    }

    override fun getJs5GroupProvider(): Js5GroupProvider {
        return js5Groups
    }

    override fun getNpcInfoSupplier(): NpcInfoSupplier {
        return npcSupplier
    }

    override fun getRsaKeyPair(): RsaKeyPair {
        return RsaProvider.from(Paths.get(".data", "game.key"))
    }

    override fun getWorldEntityInfoSupplier(): WorldEntityInfoSupplier {
        return WorldEntityProvider.provide()
    }

    // start adv: disable netty backends to use rsprot off central
    override fun getBootstrapBuilder() = BootstrapBuilder().eventLoopGroupTypes(BootstrapBuilder.EventLoopGroupType.NIO)
    // end adv
}
