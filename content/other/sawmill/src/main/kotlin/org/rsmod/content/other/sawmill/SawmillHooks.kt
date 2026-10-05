package org.rsmod.content.other.sawmill

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.concurrent.CopyOnWriteArrayList
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.game.entity.Player

interface SawmillTalkHook {
    fun option(player: Player, operator: SawmillOperator): String?

    suspend fun choose(dialogue: Dialogue, operator: SawmillOperator)
}

@Singleton
class SawmillHooks @Inject constructor() {
    private val hooks = CopyOnWriteArrayList<SawmillTalkHook>()

    fun register(hook: SawmillTalkHook) {
        hooks += hook
    }

    fun unregister(hook: SawmillTalkHook) {
        hooks -= hook
    }

    internal fun offered(
        player: Player,
        operator: SawmillOperator,
    ): List<Pair<String, SawmillTalkHook>> =
        hooks.mapNotNull { hook -> hook.option(player, operator)?.let { it to hook } }
}
