package org.rsmod.api.type.refs.queue

import org.rsmod.api.type.refs.TypeReferences
import org.rsmod.game.type.queue.QueueType
import org.rsmod.game.type.queue.QueueTypeBuilder

public abstract class QueueReferences : TypeReferences<QueueType>() {
    public fun queue(internal: String): QueueType {
        val type = QueueTypeBuilder(internalName = internal).build(id = -1)
        cache += type
        return type
    }
}
