package org.rsmod.game.entity.worldentity

public interface WorldEntityInfoProtocol {
    public fun updateCoord(level: Int, fineX: Int, fineZ: Int, teleport: Boolean)

    public fun updateAngle(angle: Int)

    public fun setSequence(seq: Int, delay: Int)

    public fun setVisibleOps(ops: Byte)

    public fun setSpecific(specific: Boolean)
}

public data object NoopWorldEntityInfo : WorldEntityInfoProtocol {
    override fun updateCoord(level: Int, fineX: Int, fineZ: Int, teleport: Boolean) {}

    override fun updateAngle(angle: Int) {}

    override fun setSequence(seq: Int, delay: Int) {}

    override fun setVisibleOps(ops: Byte) {}

    override fun setSpecific(specific: Boolean) {}
}
