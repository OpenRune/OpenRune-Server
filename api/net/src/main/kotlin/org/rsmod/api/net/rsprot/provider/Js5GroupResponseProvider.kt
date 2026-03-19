package org.rsmod.api.net.rsprot.provider

import dev.openrune.net.CacheJs5GroupProvider
import io.netty.buffer.ByteBuf
import net.rsprot.protocol.api.js5.Js5GroupProvider

class Js5GroupResponseProvider() : Js5GroupProvider {
    override fun provide(archive: Int, group: Int): ByteBuf? =
        CacheJs5GroupProvider.provide(archive, group)
}
