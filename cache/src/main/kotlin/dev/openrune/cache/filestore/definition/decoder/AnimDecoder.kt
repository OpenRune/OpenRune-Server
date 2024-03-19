package dev.openrune.cache.filestore.definition.decoder

import dev.openrune.cache.filestore.definition.DefinitionDecoder
import dev.openrune.cache.util.Index.ANIM
import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.definition.data.AnimDefinition
import kotlin.math.ceil

class AnimDecoder : DefinitionDecoder<AnimDefinition>(ANIM) {

    override fun create(size: Int) = Array(size) { AnimDefinition(it) }

    override fun getFile(id: Int) = id

    override fun AnimDefinition.read(opcode: Int, buffer: Reader) {
        when (opcode) {
            1 -> {
                val frameCount = buffer.readUnsignedShort()
                var totalFrameLength = 0
                frameIds = IntArray(frameCount)
                frameLengths = IntArray(frameCount)

                for (i in 0 until frameCount) {
                    frameLengths!![i] = buffer.readUnsignedShort()
                    if (i < frameCount - 1 || frameLengths!![i] < 200) {
                        totalFrameLength += frameLengths!![i]
                    }
                }

                for (i in 0 until frameCount) {
                    frameIds!![i] = buffer.readUnsignedShort()
                }

                for (i in 0 until frameCount) {
                    frameIds!![i] += buffer.readUnsignedShort() shl 16
                }
                lengthInCycles = ceil((totalFrameLength * 20.0) / 600.0).toInt()
            }
            2 -> buffer.readUnsignedShort()
            3 -> {
                val count = buffer.readUnsignedByte()
                for (i in 0 until count) {
                    buffer.readUnsignedByte()
                }
            }
            5 -> buffer.readUnsignedByte()
            6 -> buffer.readUnsignedShort()
            7 -> buffer.readUnsignedShort()
            8 -> buffer.readUnsignedByte()
            9 -> buffer.readUnsignedByte()
            10 -> priority = buffer.readUnsignedByte()
            11 -> buffer.readUnsignedByte()
            12 -> {
                val count = buffer.readUnsignedByte()
                for (i in 0 until count) {
                    buffer.readUnsignedShort()
                }
                for (i in 0 until count) {
                    buffer.readUnsignedShort()
                }
            }
            13 -> {
                val count = buffer.readUnsignedByte()
                for (i in 0 until count) {
                    buffer.readMedium()
                }
            }
            14 -> buffer.readInt()
            15 -> {
                val count = buffer.readUnsignedShort()
                for (i in 0 until count) {
                    buffer.readUnsignedShort()
                    buffer.readMedium()
                }
            }
            16 -> {
                buffer.readUnsignedShort()
                buffer.readUnsignedShort()
            }
            17 -> {
                val count = buffer.readUnsignedByte()
                for (i in 0 until count) {
                    buffer.readUnsignedByte()
                }
            }
        }
    }
}