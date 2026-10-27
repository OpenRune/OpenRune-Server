package org.rsmod.api.net.rsprot.websocket

import com.github.michaelbull.logging.InlineLogger
import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolConfig
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler
import io.netty.handler.ssl.SslContext
import io.netty.util.ReferenceCounted
import jakarta.inject.Inject
import org.rsmod.api.net.rsprot.websocket.tls.WebSocketTlsManager
import org.rsmod.api.server.config.ServerConfig
import org.rsmod.server.services.Service

/**
 * Local-testing-only bridge that lets a browser webclient (which only speaks WebSocket) connect to
 * this server's existing plain-TCP game port. It understands nothing about the OSRS protocol - it
 * just relays raw bytes both ways between a WebSocket connection and a loopback TCP connection to
 * [ServerConfig.gamePort], exactly as if the bytes had arrived from the real desktop client's TCP
 * socket. All login/js5 multiplexing, ISAAC ciphering, etc. still happens exactly as it does today,
 * inside the existing RSProt pipeline on the other end of that loopback connection - this bridge
 * never looks inside the protocol. Mirrors FluxRSv2's `WebSocketRelay` (a Ktor/coroutine version of
 * the same idea), written against plain Netty since that's what this project already depends on.
 *
 * Plain `ws://` unless `websocket.tls` is configured in game.yml: ClientLabs' hosted page only
 * requires `wss://` when it is itself served over `https://` (browsers block an HTTPS page from
 * opening an insecure `ws://` connection - "mixed content"). A local ClientLabs dev instance
 * serves the page over plain `http://`, where that restriction doesn't apply, so a local game
 * server never needs a cert. A *production* ClientLabs host does - see `WebSocketTlsConfig` for
 * the two ways to provide one (automatic via ACME, or your own cert files).
 */
class WebSocketBridgeService @Inject constructor(private val config: ServerConfig) : Service {
    private val logger = InlineLogger()
    private var bossGroup: NioEventLoopGroup? = null
    private var workerGroup: NioEventLoopGroup? = null
    private var channel: Channel? = null
    private var tlsManager: WebSocketTlsManager? = null

    override suspend fun startup() {
        val bridgeConfig = config.webSocket
        if (!bridgeConfig.enabled) {
            logger.info { "WebSocket bridge disabled (websocket.enabled=false in game.yml)." }
            return
        }

        val tlsConfig = bridgeConfig.tls
        var sslContextProvider: (() -> SslContext?)? = null
        if (tlsConfig != null && tlsConfig.enabled) {
            // Suspends until a certificate is actually ready - intentional. Binding a "TLS"
            // listener with nothing to serve would be worse than failing loudly at startup, same
            // as every other misconfiguration this project treats as a startup failure rather
            // than a silent fallback.
            val manager = WebSocketTlsManager(tlsConfig)
            manager.start()
            tlsManager = manager
            sslContextProvider = manager::current
        }

        val boss = NioEventLoopGroup(1)
        val worker = NioEventLoopGroup()
        bossGroup = boss
        workerGroup = worker

        val bootstrap =
            ServerBootstrap()
                .group(boss, worker)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(WebSocketBridgeInitializer(config.gamePort, worker, sslContextProvider))

        channel = bootstrap.bind(bridgeConfig.host, bridgeConfig.port).sync().channel()
        val scheme = if (sslContextProvider != null) "wss" else "ws"
        logger.info {
            "WebSocket bridge listening on $scheme://${bridgeConfig.host}:${bridgeConfig.port}, relaying to " +
                "loopback game port ${config.gamePort}."
        }
    }

    override suspend fun shutdown() {
        try {
            channel?.close()?.sync()
        } catch (t: Throwable) {
            logger.error(t) { "WebSocket bridge failed to close its listening channel." }
        }
        bossGroup?.shutdownGracefully()
        workerGroup?.shutdownGracefully()
        tlsManager?.stop()
    }
}

private class WebSocketBridgeInitializer(
    private val gamePort: Int,
    private val workerGroup: NioEventLoopGroup,
    /** Read fresh per connection, not captured once - so a background certificate renewal is picked up by the next connection with no restart. Null means TLS is off entirely. */
    private val sslContextProvider: (() -> SslContext?)?,
) : ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel) {
        val wsConfig = WebSocketServerProtocolConfig.newBuilder().websocketPath("/").checkStartsWith(true).build()
        val pipeline = ch.pipeline()
        val sslContext = sslContextProvider?.invoke()
        if (sslContext != null) {
            pipeline.addLast(sslContext.newHandler(ch.alloc()))
        }
        pipeline
            .addLast(HttpServerCodec())
            .addLast(HttpObjectAggregator(64 * 1024))
            .addLast(WebSocketServerProtocolHandler(wsConfig))
            .addLast(WebSocketRelayHandler(gamePort, workerGroup))
    }
}

/**
 * Bridges one browser WebSocket connection to its own loopback TCP connection against the real game
 * port, pumping raw bytes both ways.
 */
private class WebSocketRelayHandler(
    private val gamePort: Int,
    private val workerGroup: NioEventLoopGroup,
) : SimpleChannelInboundHandler<Any>() {
    private val logger = InlineLogger()
    private var gameChannel: Channel? = null

    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
        if (evt !is WebSocketServerProtocolHandler.HandshakeComplete) {
            super.userEventTriggered(ctx, evt)
            return
        }

        val browserChannel = ctx.channel()
        Bootstrap()
            .group(workerGroup)
            .channel(NioSocketChannel::class.java)
            .handler(
                object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(ch: SocketChannel) {
                        ch.pipeline().addLast(GameSocketRelayHandler(browserChannel))
                    }
                },
            )
            .connect("127.0.0.1", gamePort)
            .addListener(
                ChannelFutureListener { future ->
                    if (future.isSuccess) {
                        gameChannel = future.channel()
                    } else {
                        logger.warn(future.cause()) { "WebSocket bridge could not reach loopback game port $gamePort." }
                        browserChannel.close()
                    }
                },
            )
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: Any) {
        when (msg) {
            is BinaryWebSocketFrame -> gameChannel?.writeAndFlush(msg.content().retain())
            is CloseWebSocketFrame -> ctx.close()
            // Text/ping/pong frames carry no game-protocol meaning here; nothing to relay.
        }
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        gameChannel?.close()
        super.channelInactive(ctx)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        logger.warn(cause) { "WebSocket bridge connection error." }
        ctx.close()
    }
}

/** The other half of the relay: bytes read off the real loopback game socket become a binary WS frame sent to the browser. */
private class GameSocketRelayHandler(private val browserChannel: Channel) : ChannelInboundHandlerAdapter() {
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is ByteBuf) {
            browserChannel.writeAndFlush(BinaryWebSocketFrame(msg))
        } else {
            (msg as? ReferenceCounted)?.release()
        }
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        browserChannel.close()
        super.channelInactive(ctx)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        ctx.close()
    }
}
