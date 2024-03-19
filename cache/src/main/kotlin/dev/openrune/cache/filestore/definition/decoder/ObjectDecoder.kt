package dev.openrune.cache.filestore.definition.decoder

import dev.openrune.cache.filestore.definition.DefinitionDecoder
import dev.openrune.cache.util.Index.OBJECTS
import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.definition.data.ObjectDefinition
import java.util.stream.IntStream

class ObjectDecoder : DefinitionDecoder<ObjectDefinition>(OBJECTS) {
    override fun create(size: Int) = Array(size) { ObjectDefinition(it) }

    override fun getFile(id: Int) = id

    override fun ObjectDefinition.read(opcode: Int, buffer: Reader) {
        when (opcode) {
            1 -> {
                val length: Int = buffer.readUnsignedByte()
                when {
                    length > 0 -> {
                        objectTypes = IntArray(length)
                        objectModels = IntArray(length)
                        (0 until length).forEach {
                            objectModels!![it] = buffer.readUnsignedShort()
                            objectTypes!![it] = buffer.readUnsignedByte()
                        }
                    }
                }
            }
            2 -> name = buffer.readString()
            5 -> {
                val length: Int = buffer.readUnsignedByte()
                when {
                    length > 0 -> {
                        objectTypes = null
                        objectModels = IntStream.range(0, length).map {
                            buffer.readUnsignedShort()
                        }.toArray()
                    }
                }
            }
            14 -> width = buffer.readUnsignedByte()
            15 -> length = buffer.readUnsignedByte()
            17 -> {
                solid = 0
                impenetrable = false
            }
            18 -> impenetrable = false
            19 -> interactive = buffer.readUnsignedByte()
            21 -> clipType = 0
            22 -> nonFlatShading = true
            23 -> modelClipped = true
            24 -> {
                animationId = buffer.readUnsignedShort()
                if (animationId == 65535) {
                    animationId = -1
                }
            }
            27 -> solid = 1
            28 -> decorDisplacement = buffer.readUnsignedByte()
            29 -> ambient = buffer.readByte()
            39 -> contrast = buffer.readByte()
            in 30..34 -> {
                actions[opcode - 30] = buffer.readString()
            }
            40 -> readColours(buffer)
            41 -> readTextures(buffer)
            61 -> category = buffer.readUnsignedShort()
            62 -> isRotated = true
            64 -> clipped = false
            65 -> modelSizeX = buffer.readUnsignedShort()
            66 -> modelSizeZ = buffer.readUnsignedShort()
            67 -> modelSizeY = buffer.readUnsignedShort()
            68 -> mapSceneID = buffer.readUnsignedShort()
            69 -> clipMask = buffer.readByte()
            70 -> offsetX = buffer.readUnsignedShort()
            71 -> offsetZ = buffer.readUnsignedShort()
            72 -> offsetY = buffer.readUnsignedShort()
            73 -> obstructive = true
            74 -> isHollow = true
            75 -> supportsItems = buffer.readUnsignedByte()
            77, 92 -> readTransforms(buffer, opcode == 92)
            78 -> {
                ambientSoundId = buffer.readUnsignedShort()
                soundDistance = buffer.readUnsignedByte()
            }
            79 -> {
                soundMin = buffer.readUnsignedShort()
                soundMax = buffer.readUnsignedShort()
                soundDistance = buffer.readUnsignedByte()

                val length: Int = buffer.readUnsignedByte()
                ambientSoundIds = IntStream.range(0, length).map {
                    buffer.readUnsignedShort()
                }.toArray()
            }
            81 -> clipType = (buffer.readUnsignedByte()) * 256
            60,82 -> mapAreaId = buffer.readUnsignedShort()
            89 -> randomizeAnimStart = true
            249 -> readParameters(buffer)
            else -> logger.info { "Unable to decode Npcs [${opcode}]" }
        }
    }

}