package org.rsmod.api.net.central.netty

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageDecoder

internal class WorldLinkInboundPacketDecoder : MessageToMessageDecoder<ByteBuf>() {
    override fun decode(
        ctx: ChannelHandlerContext,
        msg: ByteBuf,
        out: MutableList<Any>,
    ) {
        val n = msg.readableBytes()
        val bytes = ByteArray(n)
        msg.readBytes(bytes)
        out.add(WorldLinkInboundPacket(bytes))
    }
}
