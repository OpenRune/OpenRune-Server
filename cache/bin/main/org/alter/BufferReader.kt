package org.alter

import java.nio.ByteBuffer

class BufferReader(
    val buffer: ByteBuffer
) {

    constructor(array: ByteArray) : this(buffer = ByteBuffer.wrap(array))

    val length: Int = buffer.remaining()
    val remaining: Int
        get() = buffer.remaining()
    private var bitIndex = 0

    fun readByte(): Int {
        return buffer.get().toInt()
    }

    fun readByteAdd(): Int {
        return (readByte() - 128).toByte().toInt()
    }

    fun readByteInverse(): Int {
        return -readByte()
    }

    fun readByteSubtract(): Int {
        return (readByteInverse() + 128).toByte().toInt()
    }

    fun readUnsignedByte(): Int {
        return readByte() and 0xff
    }

    fun readShort(): Int {
        return (readByte() shl 8) or readUnsignedByte()
    }

    fun readShortAdd(): Int {
        return (readByte() shl 8) or readUnsignedByteAdd()
    }

    fun readUnsignedShortAdd(): Int {
        return (readByte() shl 8) or ((readByte() - 128) and 0xff)
    }

    fun readShortLittle(): Int {
        return readUnsignedByte() or (readByte() shl 8)
    }

    fun readShortAddLittle(): Int {
        return readUnsignedByteAdd() or (readByte() shl 8)
    }

    fun readShortSmart() : Int {
        val peek = readUnsignedByte()
        return if (peek < 128) peek - 64 else (peek shl 8 or readUnsignedByte()) - 49152
    }

    fun readUnsignedByteAdd(): Int {
        return (readByte() - 128).toByte().toInt()
    }

    fun readUnsignedShort(): Int {
        return (readUnsignedByte() shl 8) or readUnsignedByte()
    }

    fun readUnsignedShortLittle(): Int {
        return readUnsignedByte() or (readUnsignedByte() shl 8)
    }

    fun readMedium(): Int {
        return (readByte() shl 16) or (readByte() shl 8) or readUnsignedByte()
    }

    fun readUnsignedMedium(): Int {
        return (readUnsignedByte() shl 16) or (readUnsignedByte() shl 8) or readUnsignedByte()
    }

    fun readInt(): Int {
        return (readUnsignedByte() shl 24) or (readUnsignedByte() shl 16) or (readUnsignedByte() shl 8) or readUnsignedByte()
    }

    fun readIntInverseMiddle(): Int {
        return (readByte() shl 16) or (readByte() shl 24) or readUnsignedByte() or (readByte() shl 8)
    }

    fun readIntLittle(): Int {
        return readUnsignedByte() or (readByte() shl 8) or (readByte() shl 16) or (readByte() shl 24)
    }

    fun readUnsignedIntMiddle(): Int {
        return (readUnsignedByte() shl 8) or readUnsignedByte() or (readUnsignedByte() shl 24) or (readUnsignedByte() shl 16)
    }

    fun readSmart(): Int {
        val peek = readUnsignedByte()
        return if (peek < 128) {
            peek and 0xFF
        } else {
            (peek shl 8 or readUnsignedByte()) - 32768
        }
    }

    fun readBigSmart(): Int {
        val peek = readByte()
        return if (peek < 0) {
            ((peek shl 24) or (readUnsignedByte() shl 16) or (readUnsignedByte() shl 8) or readUnsignedByte()) and 0x7fffffff
        } else {
            val value = (peek shl 8) or readUnsignedByte()
            if (value == 32767) -1 else value
        }
    }

    fun readLargeSmart(): Int {
        var baseValue = 0
        var lastValue = readSmart()
        while (lastValue == 32767) {
            lastValue = readSmart()
            baseValue += 32767
        }
        return baseValue + lastValue
    }

    fun readLong(): Long {
        val first = readInt().toLong() and 0xffffffffL
        val second = readInt().toLong() and 0xffffffffL
        return second + (first shl 32)
    }

    fun readString(): String {
        val sb = StringBuilder()
        var b: Int
        while (buffer.hasRemaining()) {
            b = readUnsignedByte()
            if (b == 0) {
                break
            }
            sb.append(b.toChar())
        }
        return sb.toString()
    }

    fun readBytes(value: ByteArray) {
        buffer.get(value)
    }

    fun readBytes(array: ByteArray, offset: Int, length: Int) {
        buffer.get(array, offset, length)
    }

    fun skip(amount: Int) {
        buffer.position(buffer.position() + amount)
    }

    fun position(): Int {
        return buffer.position()
    }

    fun position(index: Int) {
        buffer.position(index)
    }

    fun array(): ByteArray {
        return buffer.array()
    }

    fun readableBytes(): Int {
        return buffer.remaining()
    }


}