package dev.openrune.tools

import dev.openrune.codec.osrs.SequenceDecoder
import dev.openrune.filesystem.Cache
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.SequenceServerType

/**
 * Prints how long an animation runs. An obstacle that carries the player has to take as long as its
 * animation or the player glides across ahead of their own feet, and the cache is the only source
 * for that duration.
 */
public object SeqDumper {
    public fun dump(serverCache: Cache, wanted: List<String>) {
        val seqs = HashMap<Int, SequenceServerType>()
        SequenceDecoder().load(serverCache, seqs)

        println("seq\tid\tticks\tcycles\tloops")
        for (name in wanted) {
            val id = runCatching { name.toIntOrNull() ?: name.asRSCM(RSCMType.SEQ) }.getOrNull()
            val seq = id?.let { seqs[it] }
            if (seq == null) {
                println("$name\t?\t-\t-\t-")
                continue
            }
            println("$name\t$id\t${seq.tickDuration}\t${seq.totalDelay}\t${seq.maxLoops}")
        }
    }
}
