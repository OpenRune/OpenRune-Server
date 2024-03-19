package dev.openrune.cache.filestore.definition.decoder

import dev.openrune.cache.filestore.definition.DefinitionDecoder
import dev.openrune.cache.util.Index.NPCS
import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.definition.data.NPCDefinition

class NPCDecoder : DefinitionDecoder<NPCDefinition>(NPCS) {
    override fun create(size: Int) = Array(size) { NPCDefinition(it) }

    override fun getFile(id: Int) = id

    override fun NPCDefinition.read(opcode: Int, buffer: Reader) {
        when (opcode) {
            1 -> {
                val length = buffer.readUnsignedByte()
                modelIds = IntArray(length)
                for (count in 0 until length) {
                    modelIds!![count] = buffer.readUnsignedShort()
                    if (modelIds!![count] == 65535) {
                        modelIds!![count] = -1
                    }
                }
            }
            2 -> name = buffer.readString()
            12 -> size = buffer.readUnsignedByte()
            13 -> standAnim = buffer.readUnsignedShort()
            14 -> walkAnim = buffer.readUnsignedShort()
            15 -> render3 = buffer.readUnsignedShort()
            16 -> render4 = buffer.readUnsignedShort()
            17 -> {
                walkAnim = buffer.readUnsignedShort()
                render5 = buffer.readUnsignedShort()
                render6 = buffer.readUnsignedShort()
                render7 = buffer.readUnsignedShort()
            }
            18 -> category = buffer.readUnsignedShort()
            in 30..34 -> {
                actions[opcode - 30] = buffer.readString()
                if (actions[opcode - 30].equals("Hidden", true)) {
                    actions[opcode - 30] = null
                }
            }
            40 -> readColours(buffer)
            41 -> readTextures(buffer)
            60 -> {
                val length: Int = buffer.readUnsignedByte()
                chatheadModels = IntArray(length)
                (0 until length).forEach {
                    chatheadModels!![it] = buffer.readUnsignedShort()
                }
            }
            93 -> isMinimapVisible = false
            95 -> combatLevel = buffer.readUnsignedShort()
            97 -> widthScale = buffer.readUnsignedShort()
            98 -> heightScale = buffer.readUnsignedShort()
            99 -> hasRenderPriority = true
            100 -> ambient = buffer.readByte()
            101 -> contrast = buffer.readByte()
            102 -> {
                headIconArchiveIds = intArrayOf(-1)
                headIconSpriteIndex = IntArray(buffer.readUnsignedShort())
            }
            111 -> isFollower = true
            103 -> rotation = buffer.readUnsignedShort()
            106, 118 -> readTransforms(buffer, opcode == 118)
            107 -> isInteractable = false
            109 -> isClickable = false
            114 -> runSequence = buffer.readUnsignedShort()
            115 -> {
                runSequence = buffer.readUnsignedShort()
                runBackSequence = buffer.readUnsignedShort()
                runRightSequence = buffer.readUnsignedShort()
                runLeftSequence = buffer.readUnsignedShort()
            }
            116 -> crawlSequence = buffer.readUnsignedShort()
            117 -> {
                crawlSequence = buffer.readUnsignedShort()
                crawlBackSequence = buffer.readUnsignedShort()
                crawlRightSequence = buffer.readUnsignedShort()
                crawlLeftSequence = buffer.readUnsignedShort()
            }
            122 -> lowPriorityFollowerOps = true
            123 -> isFollower = true
            249 -> readParameters(buffer)
            else -> logger.info { "Unable to decode Npcs [${opcode}]" }
        }
    }

}