package org.rsmod.api.bosses.runtime

import jakarta.inject.Singleton
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

fun interface BossExtensionHandler {
    /** [access] is null when the ability runs outside the npc's AI turn, e.g. from a hit reaction. */
    fun invoke(access: StandardNpcAccess?, npc: Npc, target: Player, params: Any?)
}

/**
 * What a context handler receives. [access] is null outside the npc's AI turn; [tile] is the
 * `external(at = …)` tile, resolved where the effect runs (so `CurrentTile`/`ImpactTile` work),
 * or null when the effect has none.
 */
class BossExtensionContext(
    val access: StandardNpcAccess?,
    val npc: Npc,
    val target: Player,
    val params: Any?,
    val tile: CoordGrid?,
)

@Singleton
class BossExtensionRegistry {
    private val handlers = mutableMapOf<String, (BossExtensionContext) -> Unit>()

    fun register(name: String, handler: BossExtensionHandler) {
        handlers[name] = { handler.invoke(it.access, it.npc, it.target, it.params) }
    }

    fun register(name: String, handler: (BossExtensionContext) -> Unit) {
        handlers[name] = handler
    }

    fun invoke(
        name: String,
        access: StandardNpcAccess?,
        npc: Npc,
        target: Player,
        params: Any?,
        tile: CoordGrid? = null,
    ) {
        val handler = handlers[name] ?: error("No boss extension registered: $name")
        handler(BossExtensionContext(access, npc, target, params, tile))
    }

    fun contains(name: String): Boolean = name in handlers
}
