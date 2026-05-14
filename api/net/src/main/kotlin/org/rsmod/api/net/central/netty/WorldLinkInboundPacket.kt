package org.rsmod.api.net.central.netty


internal data class WorldLinkInboundPacket(
    val content: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as WorldLinkInboundPacket
        return content.contentEquals(other.content)
    }

    override fun hashCode(): Int = content.contentHashCode()
}
