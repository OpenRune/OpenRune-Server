package org.rsmod.api.type.refs.midi

import org.rsmod.api.type.refs.TypeReferences
import org.rsmod.game.type.midi.MidiType
import org.rsmod.game.type.midi.MidiTypeBuilder

public abstract class MidiReferences : TypeReferences<MidiType>(MidiType::class.java) {
    public fun midi(internal: String): MidiType {
        val type = MidiTypeBuilder(internal).build(id = -1)
        cache += type
        return type
    }
}
