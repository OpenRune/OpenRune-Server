package dev.openrune.types

import dev.openrune.definition.Definition
import dev.openrune.toml.rsconfig.RsTableHeaders

@RsTableHeaders("bas")
data class BasType(
    override var id: Int = -1,
    public var readyAnim: Int = -1,
    public var turnOnSpot: Int = -1,
    public var walkForward: Int = -1,
    public var walkBack: Int = -1,
    public var walkLeft: Int = -1,
    public var walkRight: Int = -1,
    public var running: Int = -1,
) : Definition {
    fun isType(type: BasType) = type.id == id
}
