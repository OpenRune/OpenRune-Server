package org.rsmod.api.net.central.netty

import com.github.michaelbull.logging.InlineLogger
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.timeout.ReadTimeoutException
import java.util.concurrent.BlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

internal class WorldLinkInboundQueueHandler(
    private val queue: BlockingQueue<ByteArray>,
) : SimpleChannelInboundHandler<WorldLinkInboundPacket>() {

    private val closeSignalled = AtomicBoolean(false)

    private fun signalClosed() {
        if (closeSignalled.compareAndSet(false, true)) {
            queue.offer(CLOSE_SENTINEL)
        }
    }

    override fun channelRead0(
        ctx: ChannelHandlerContext,
        msg: WorldLinkInboundPacket,
    ) {
        queue.put(msg.content)
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        signalClosed()
        super.channelInactive(ctx)
    }

    override fun exceptionCaught(
        ctx: ChannelHandlerContext,
        cause: Throwable,
    ) {
        when (cause) {
            is ReadTimeoutException ->
                logger.debug { "World link read timeout (${ctx.channel().remoteAddress()})" }

            else ->
                logger.debug {
                    "World link channel exception (${ctx.channel().remoteAddress()}): ${cause.message}"
                }
        }
        signalClosed()
        ctx.close()
    }

    private companion object {
        private val logger = InlineLogger()

        /** Empty payload is never a valid Central frame; used only as a close signal for blocking [poll]. */
        val CLOSE_SENTINEL: ByteArray = byteArrayOf()
    }
}
