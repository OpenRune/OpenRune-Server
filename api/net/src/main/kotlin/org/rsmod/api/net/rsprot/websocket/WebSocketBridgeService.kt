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
import io.netty.util.ReferenceCounted
import jakarta.inject.Inject
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
 * Plain `ws://`, no TLS: ClientLabs' hosted page only requires `wss://` when it is itself served
 * over `https://` (browsers block an HTTPS page from opening an insecure `ws://` connection -
 * "mixed content"). A local ClientLabs dev instance serves the page over plain `http://`, where that
 * restriction doesn't apply, so a local game server never needs a cert at all. If you're pointing a
 * *production* ClientLabs host at this for some reason, put a real TLS-terminating reverse proxy or
 * tunnel in front of this port - this bridge intentionally stays as simple as the thing it's testing.
 */
class WebSocketBridgeService @Inject constructor(private val config: ServerConfig) : Service {
    private val logger = InlineLogger()
    private var bossGroup: NioEventLoopGroup? = null
    private var workerGroup: NioEventLoopGroup? = null
    private var channel: Channel? = null

    override suspend fun startup() {
        val bridgeConfig = config.webSocket
        if (!bridgeConfig.enabled) {
            logger.info { "WebSocket bridge disabled (websocket.enabled=false in game.yml)." }
            return
        }

        val boss = NioEventLoopGroup(1)
        val worker = NioEventLoopGroup()
        bossGroup = boss
        workerGroup = worker

        val bootstrap =
            ServerBootstrap()
                .group(boss, worker)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(WebSocketBridgeInitializer(config.gamePort, worker))

        channel = bootstrap.bind(bridgeConfig.host, bridgeConfig.port).sync().channel()
        logger.info {
            "WebSocket bridge listening on ws://${bridgeConfig.host}:${bridgeConfig.port}, relaying to " +
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
    }
}

private class WebSocketBridgeInitializer(
    private val gamePort: Int,
    private val workerGroup: NioEventLoopGroup,
) : ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel) {
        val wsConfig = WebSocketServerProtocolConfig.newBuilder().websocketPath("/").checkStartsWith(true).build()
        ch.pipeline()
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
