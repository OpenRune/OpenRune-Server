package org.rsmod.api.type.refs.seq

import org.rsmod.api.type.refs.TypeReferences
import org.rsmod.game.type.seq.HashedSeqType
import org.rsmod.game.type.seq.SeqType

public abstract class SeqReferences : TypeReferences<SeqType>() {
    public fun seq(internal: String): SeqType {
        val type = HashedSeqType(null, internal)
        cache += type
        return type
    }
}
