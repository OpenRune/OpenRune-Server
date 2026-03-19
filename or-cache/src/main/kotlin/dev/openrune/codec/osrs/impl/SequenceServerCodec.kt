package dev.openrune.codec.osrs.impl

import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.opcode.OpcodeDefinitionCodec
import dev.openrune.definition.opcode.OpcodeList
import dev.openrune.definition.opcode.OpcodeType
import dev.openrune.definition.type.SequenceType
import dev.openrune.types.SequenceServerType
import kotlin.math.ceil

class SequenceServerCodec(val sequences: Map<Int, SequenceType>? = null) :
    OpcodeDefinitionCodec<SequenceServerType>() {

    override val definitionCodec =
        OpcodeList<SequenceServerType>().apply {
            add(DefinitionOpcode(1, OpcodeType.USHORT, SequenceServerType::tickDuration))
            add(DefinitionOpcode(2, OpcodeType.USHORT, SequenceServerType::totalDelay))
            add(DefinitionOpcode(3, OpcodeType.BYTE, SequenceServerType::maxLoops))
            add(DefinitionOpcode(4, OpcodeType.BYTE, SequenceServerType::priority))
        }

    override fun SequenceServerType.createData() {
        if (sequences == null) return

        val seq = sequences[id] ?: return

        if (seq.skeletalId >= 0) {
            tickDuration = (seq.getSkeletalLength() / 30.0).toInt()
            totalDelay = seq.getSkeletalLength()
        } else {
            tickDuration = tickDuration(seq.frameDelays!!)
            totalDelay = seq.frameDelays!!.sum()
        }
        maxLoops = seq.maxLoops
    }

    private fun SequenceType.getSkeletalLength(): Int = rangeEnd - rangeBegin

    private fun tickDuration(shorts: MutableList<Int>): Int {
        val validDelays = shorts.dropLastWhile { it > 30 }
        val buffer = if (validDelays.size != shorts.size) 5 else 0
        val duration = (validDelays.sum() + buffer) * 20
        return ceil(duration / 600.0).toInt()
    }

    override fun createDefinition(): SequenceServerType = SequenceServerType()
}
