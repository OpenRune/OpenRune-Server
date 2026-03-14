package org.rsmod.api.type.refs.synth

import org.rsmod.api.type.refs.TypeReferences
import org.rsmod.game.type.synth.SynthType
import org.rsmod.game.type.synth.SynthTypeBuilder

public abstract class SynthReferences : TypeReferences<SynthType>(SynthType::class.java) {
    public fun synth(internal: String): SynthType {
        val type = SynthTypeBuilder(internal).build(id = -1)
        cache += type
        return type
    }
}
