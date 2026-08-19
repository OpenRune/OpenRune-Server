package org.rsmod.api.bosses.runtime

import jakarta.inject.Singleton
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

fun interface BossExtensionHandler {
    suspend fun invoke(access: StandardNpcAccess, npc: Npc, target: Player, params: Any?)
}

@Singleton
class BossExtensionRegistry {
    private val handlers = mutableMapOf<String, BossExtensionHandler>()

    fun register(name: String, handler: BossExtensionHandler) {
        handlers[name] = handler
    }

    suspend fun invoke(
        name: String,
        access: StandardNpcAccess,
        npc: Npc,
        target: Player,
        params: Any?,
    ) {
        val handler = handlers[name] ?: error("No boss extension registered: $name")
        handler.invoke(access, npc, target, params)
    }

    fun contains(name: String): Boolean = name in handlers
}
