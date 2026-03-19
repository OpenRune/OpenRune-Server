package dev.openrune.map.util

import io.netty.buffer.ByteBuf

public fun ByteBuf.writeIntSmart(value: Int): ByteBuf {
    require(value > Short.MAX_VALUE)
    require(value < (Int.MAX_VALUE - Short.MAX_VALUE))
    val diff = value - Short.MAX_VALUE
    writeShort(0xFFFF)
    writeUnsignedShortSmart(diff)
    return this
}

public fun ByteBuf.writeUnsignedSmartInt(value: Int): ByteBuf =
    if (value > Short.MAX_VALUE) {
        writeIntSmart(value)
    } else {
        writeUnsignedShortSmart(value)
    }

public fun ByteBuf.toInlineBuf(): InlineByteBuf =
    if (hasArray()) {
        val bytes = array().copyOf(writerIndex())
        InlineByteBuf(bytes)
    } else {
        val bytes = ByteArray(writerIndex())
        readBytes(bytes)
        InlineByteBuf(bytes)
    }

public fun ByteBuf.toReadableByteArray(): ByteArray {
    val bytes = ByteArray(readableBytes())
    getBytes(readerIndex(), bytes)
    return bytes
}

public fun ByteBuf.writeUnsignedShortSmart(v: Int): ByteBuf {
    when (v) {
        in 0..0x7F -> writeByte(v)
        in 0..0x7FFF -> writeShort(0x8000 or v)
        else -> throw IllegalArgumentException()
    }

    return this
}
