package org.rsmod.api.type.refs.proj

import org.rsmod.api.type.refs.TypeReferences
import org.rsmod.game.type.proj.HashedProjAnimType
import org.rsmod.game.type.proj.ProjAnimType

public abstract class ProjAnimReferences : TypeReferences<ProjAnimType>() {
    public fun projAnim(internal: String): ProjAnimType {
        // For now, can't see a realistic situation where identity hash verification is required.
        // Though maybe at some point plugins may require support for this to ensure any base/core
        // projanims are not changed. Can reconsider this decision in the future.
        val type = HashedProjAnimType(null, internal)
        cache += type
        return type
    }
}
