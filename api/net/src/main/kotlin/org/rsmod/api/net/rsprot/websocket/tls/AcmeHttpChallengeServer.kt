package org.rsmod.api.net.rsprot.websocket.tls

import com.github.michaelbull.logging.InlineLogger
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.HttpVersion
import java.nio.charset.StandardCharsets

/**
 * The absolute minimum HTTP server needed to answer an ACME HTTP-01 challenge
 * (`GET /.well-known/acme-challenge/{token}` -> the expected key authorization, plain text).
 * Bound to port 80 only for the duration of [serveWhile]'s block, not held open permanently - an
 * operator may want that port for something else between renewal checks; Let's Encrypt only needs
 * to reach it for the few seconds validation actually takes.
 */
internal class AcmeHttpChallengeServer {
    private val logger = InlineLogger()

    suspend fun <T> serveWhile(token: String, keyAuthorization: String, host: String, block: suspend () -> T): T {
        val bossGroup = NioEventLoopGroup(1)
        val workerGroup = NioEventLoopGroup(1)
        var channel: Channel? = null
        try {
            val bootstrap =
                ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel::class.java)
                    .childHandler(
                        object : ChannelInitializer<SocketChannel>() {
                            override fun initChannel(ch: SocketChannel) {
                                ch.pipeline()
                                    .addLast(HttpServerCodec())
                                    .addLast(HttpObjectAggregator(8 * 1024))
                                    .addLast(ChallengeResponseHandler(token, keyAuthorization))
                            }
                        },
                    )
            channel = bootstrap.bind(host, 80).sync().channel()
            logger.info { "ACME HTTP-01 challenge responder listening on $host:80 for token '$token'." }
            return block()
        } finally {
            channel?.close()?.sync()
            bossGroup.shutdownGracefully()
            workerGroup.shutdownGracefully()
        }
    }
}

private class ChallengeResponseHandler(
    private val token: String,
    private val keyAuthorization: String,
) : SimpleChannelInboundHandler<FullHttpRequest>() {
    override fun channelRead0(ctx: ChannelHandlerContext, request: FullHttpRequest) {
        val expectedPath = "/.well-known/acme-challenge/$token"
        val response =
            if (request.method() == HttpMethod.GET && request.uri() == expectedPath) {
                val content = Unpooled.copiedBuffer(keyAuthorization, StandardCharsets.US_ASCII)
                DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content).apply {
                    headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain")
                    headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes())
                }
            } else {
                DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND, Unpooled.EMPTY_BUFFER)
            }
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
    }
}
