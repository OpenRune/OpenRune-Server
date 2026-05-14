package org.rsmod.api.net.central.netty

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.PooledByteBufAllocator
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.LengthFieldPrepender
import io.netty.handler.timeout.ReadTimeoutHandler
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.rsmod.api.net.central.WorldLinkFrameSpecs
import org.rsmod.api.net.central.validateGameToCentralFrameOrThrow

internal object WorldLinkNettyIOLoop {
    private val threadSeq = AtomicInteger()

    val group: EventLoopGroup by lazy {
        NioEventLoopGroup(
            1,
            ThreadFactory { r ->
                Thread(r, "openrune-worldlink-netty-${threadSeq.incrementAndGet()}").apply {
                    isDaemon = true
                }
            },
        )
    }
}

internal object WorldLinkNettyChannelSupport {
    fun initChildPipeline(
        ch: SocketChannel,
        inboundQueue: BlockingQueue<ByteArray>,
        readIdleSeconds: Int?,
    ) {
        val p = ch.pipeline()
        if (readIdleSeconds != null && readIdleSeconds > 0) {
            p.addLast(
                WorldLinkHandlerNames.READ_TIMEOUT,
                ReadTimeoutHandler(readIdleSeconds.toLong(), TimeUnit.SECONDS),
            )
        }
        p.addLast(
            WorldLinkHandlerNames.LENGTH_FRAME_DECODER,
            LengthFieldBasedFrameDecoder(
                WorldLinkFrameSpecs.MAX_FRAMED_BODY,
                LENGTH_FIELD_OFFSET,
                LENGTH_FIELD_LENGTH,
                LENGTH_ADJUSTMENT,
                INITIAL_BYTES_TO_STRIP,
            ),
        )
        p.addLast(
            WorldLinkHandlerNames.LENGTH_FRAME_ENCODER,
            LengthFieldPrepender(LENGTH_FIELD_LENGTH),
        )
        p.addLast(
            WorldLinkHandlerNames.INBOUND_PACKET_DECODER,
            WorldLinkInboundPacketDecoder(),
        )
        p.addLast(
            WorldLinkHandlerNames.INBOUND_QUEUE,
            WorldLinkInboundQueueHandler(inboundQueue),
        )
    }

    private const val LENGTH_FIELD_OFFSET = 0
    private const val LENGTH_FIELD_LENGTH = 4
    private const val LENGTH_ADJUSTMENT = 0
    private const val INITIAL_BYTES_TO_STRIP = 4
}

/**
 * Blocking façade over one world-link Netty channel (caller thread performs send/recv; I/O on [WorldLinkNettyIOLoop.group]).
 */
internal class WorldLinkNettyBlockingSession(
    val channel: Channel,
    private val inbound: BlockingQueue<ByteArray>,
) {
    fun send(body: ByteArray) {
        validateGameToCentralFrameOrThrow(body)
        channel.writeAndFlush(Unpooled.wrappedBuffer(body)).sync()
    }

    /**
     * Blocks up to [timeoutMs] for the next inbound packet.
     * @return `null` on timeout, empty array if the channel closed or failed, otherwise the frame bytes.
     */
    fun pollInbound(timeoutMs: Long): ByteArray? {
        val f = inbound.poll(timeoutMs, TimeUnit.MILLISECONDS) ?: return null
        return f
    }

    /** Blocks for one frame or throws [SocketTimeoutException] / [java.io.IOException] on close. */
    fun recvInbound(timeoutMs: Long): ByteArray {
        val f = pollInbound(timeoutMs) ?: throw SocketTimeoutException("Timed out waiting for Central world-link frame")
        if (f.isEmpty()) {
            throw java.io.IOException("Central world-link channel closed")
        }
        return f
    }

    fun close() {
        channel.close().syncUninterruptibly()
    }
}

internal object WorldLinkNettyBlockingClient {
    private const val CONNECT_TIMEOUT_MS = 10_000

    fun connect(
        address: InetSocketAddress,
        readIdleSeconds: Int?,
    ): WorldLinkNettyBlockingSession {
        val queue = LinkedBlockingQueue<ByteArray>()
        val bootstrap =
            Bootstrap()
                .group(WorldLinkNettyIOLoop.group)
                .channel(NioSocketChannel::class.java)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS)
                .handler(
                    object : ChannelInitializer<SocketChannel>() {
                        override fun initChannel(ch: SocketChannel) {
                            WorldLinkNettyChannelSupport.initChildPipeline(ch, queue, readIdleSeconds)
                        }
                    },
                )
        val future = bootstrap.connect(address)
        future.sync()
        return WorldLinkNettyBlockingSession(future.channel(), queue)
    }
}
