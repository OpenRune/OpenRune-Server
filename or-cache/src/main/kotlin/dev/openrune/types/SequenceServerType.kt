package dev.openrune.types

import dev.openrune.definition.Definition
import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.toml.rsconfig.RsTableHeaders

@RsTableHeaders("anims")
data class SequenceServerType(
    override var id: Int = -1,
    var priority: Int = 5,
    var tickDuration: Int = 0,
    var maxLoops: Int = 99,
    var totalDelay: Int = 0,
) : Definition {
    val internalName: String
        get() = ConstantProvider.getReverseMapping("seq", id)

    public fun isType(seq: SequenceServerType): Boolean {
        return this.id == seq.id
    }
}
