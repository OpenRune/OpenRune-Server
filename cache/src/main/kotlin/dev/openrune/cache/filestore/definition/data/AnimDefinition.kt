package dev.openrune.cache.filestore.definition.data

import dev.openrune.cache.filestore.definition.Definition

data class AnimDefinition(
    override var id: Int = -1,
    var frameIds: IntArray? = null,
    var frameLengths: IntArray? = null,
    var priority: Int = -1
) : Definition {

    var lengthInCycles = 0

    val cycleLength: Int get() = lengthInCycles
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AnimDefinition

        if (id != other.id) return false
        if (frameIds != null) {
            if (other.frameIds == null) return false
            if (!frameIds.contentEquals(other.frameIds)) return false
        } else if (other.frameIds != null) return false
        if (frameLengths != null) {
            if (other.frameLengths == null) return false
            if (!frameLengths.contentEquals(other.frameLengths)) return false
        } else if (other.frameLengths != null) return false
        if (priority != other.priority) return false
        if (lengthInCycles != other.lengthInCycles) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + (frameIds?.contentHashCode() ?: 0)
        result = 31 * result + (frameLengths?.contentHashCode() ?: 0)
        result = 31 * result + priority
        result = 31 * result + lengthInCycles
        return result
    }

}