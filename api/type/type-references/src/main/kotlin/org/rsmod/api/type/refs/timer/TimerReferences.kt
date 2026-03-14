package org.rsmod.api.type.refs.timer

import org.rsmod.api.type.refs.TypeReferences
import org.rsmod.game.type.timer.TimerType
import org.rsmod.game.type.timer.TimerTypeBuilder

public abstract class TimerReferences : TypeReferences<TimerType>(TimerType::class.java) {
    public fun timer(internal: String): TimerType {
        val type = TimerTypeBuilder(internalName = internal).build(id = -1)
        cache += type
        return type
    }
}
